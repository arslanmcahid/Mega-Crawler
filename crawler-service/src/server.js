import express from "express";
import cors from "cors";
import { chromium } from "playwright";

const app = express();
const PORT = process.env.PORT || 4000;
const BASE = "https://www.mega-gastro.at";

app.use(cors());

// Ürün datası cache
const PRODUCTS_TTL_MS = 5 * 60 * 1000; // 5 dk
let productsCache = { ts: 0, data: null };

// Kategori listesi cache (daha uzun)
const CATEGORIES_TTL_MS = 24 * 60 * 60 * 1000; // 24 saat
let categoriesCache = { ts: 0, data: null };

// Her kategori için en son yapılan scrape'de kaç ürün bulunduğu
let categoryCountsCache = {}; // { [key: string]: number }

// Menüde görünmesini istediğin ana kategoriler (PATH'LERİ SİTEDEN KONTROL ET!)
const MANUAL_MENU_CATEGORIES = [
  {
    key: "alle-produkte",
    name: "Alle Produkte",
    path: "/produkt-kategorie/alle-produkte/", // URL'yi menüden doğrula
  },
  {
    key: "verpackung",
    name: "Verpackung",
    path: "/produkt-kategorie/verpackung/",
  },
  {
    key: "lebensmittel",
    name: "Lebensmittel",
    path: "/produkt-kategorie/lebensmittel/",
  },
  {
    key: "getraenke",
    name: "Getränke",
    path: "/produkt-kategorie/getraenke/",
  },
  {
    key: "reinigung-hygiene",
    name: "Reinigung & Hygiene",
    path: "/produkt-kategorie/reinigung-hygiene/",
  },
  {
    key: "beauty-kosmetik",
    name: "Beauty & Kosmetik",
    path: "/produkt-kategorie/beauty-kosmetik/",
  },
  {
    key: "transporthilfsmaterial",
    name: "Transporthilfsmaterial",
    path: "/produkt-kategorie/transporthilfsmaterial/",
  },
  {
    key: "woechentliche-angebote",
    name: "Wöchentliche Angebote",
    path: "/produkt-kategorie/woechentliche-angebote/", // SALE bölümü
  },
];

// Çok acil fallback (hiç kategori bulunamazsa)
const FALLBACK_CATEGORIES = [
  {
    key: "sale",
    path: "/produkt-kategorie/sale/",
    name: "Wöchentliche Angebote",
  },
  {
    key: "gastro-kuechengeraete",
    path: "/produkt-kategorie/gastro-kuechengeraete/",
    name: "Gastro Küchengeräte",
  },
];

function parsePrice(text) {
  if (!text) return null;
  const cleaned = text
      .replace(/[^\d,.\s]/g, "")
      .replace(/\s/g, "")
      .replace(/\.(?=\d{3}(\D|$))/g, "")
      .replace(",", ".");
  const value = parseFloat(cleaned);
  return Number.isNaN(value) ? null : value;
}

function computeDiscount(original, current) {
  if (original && current && original > current) {
    return Math.round(((original - current) / original) * 100);
  }
  return 0;
}

// /produkt-kategorie/abc-def/ -> abc-def
function categoryKeyFromPath(path) {
  const m = path.toLowerCase().match(/\/produkt-kategorie\/([^/]+)\//);
  return m?.[1] || "other";
}

/**
 * 1) Dinamik kategori keşfi
 * - Homepage / olası shop sayfalarından
 * - /produkt-kategorie/ linklerini topla
 */
async function discoverCategories(page) {
  const candidates = [`${BASE}/`, `${BASE}/shop/`];

  const found = new Map(); // path -> { key, path, name }

  for (const url of candidates) {
    try {
      await page.goto(url, { waitUntil: "domcontentloaded" });
      await page.waitForTimeout(800);

      // Daha agresif selector: hem küçük hem büyük harf
      const links = await page.$$eval(
          'a[href*="produkt-kategorie"], a[href*="Produkt-Kategorie"]',
          (as) =>
              as.map((a) => ({
                href: a.getAttribute("href") || "",
                text: (a.textContent || "").trim(),
              }))
      );

      for (const l of links) {
        const href = l.href.startsWith("http") ? l.href : `${BASE}${l.href}`;
        const u = new URL(href);

        let path = u.pathname.toLowerCase();
        if (!path.endsWith("/")) path += "/";

        const key = categoryKeyFromPath(path);
        const name = l.text || key.replace(/-/g, " ");

        if (!found.has(path)) {
          found.set(path, { key, path, name });
        }
      }
    } catch (e) {
      console.error("Error discovering categories from", url, e);
    }
  }

  const discovered = Array.from(found.values());

  // Eğer hiç bulunamadıysa, en azından fallback dön
  let baseList =
      discovered.length > 0
          ? discovered
          : FALLBACK_CATEGORIES.map((c) => ({ ...c }));

  // MANUAL_MENU_CATEGORIES ile merge et:
  // - Aynı key varsa MANUAL olan kazanır (menüdeki isim/path öncelikli)
  const finalMap = new Map();

  // Dinamikleri ekle
  for (const c of baseList) {
    finalMap.set(c.key, {
      key: c.key,
      name:
          c.name && c.name.length > 1
              ? c.name
              : c.key.replace(/-/g, " "),
      path: c.path,
    });
  }

  // Manuel kategorileri üstüne yaz
  for (const m of MANUAL_MENU_CATEGORIES) {
    const key = m.key;
    const name = m.name;
    const path = m.path.toLowerCase();
    finalMap.set(key, { key, name, path });
  }

  // Son liste
  const merged = Array.from(finalMap.values());

  return merged;
}

/**
 * Cache’li kategori get
 */
async function getCategories(page) {
  const now = Date.now();
  if (categoriesCache.data && now - categoriesCache.ts < CATEGORIES_TTL_MS) {
    return categoriesCache.data;
  }

  const cats = await discoverCategories(page);
  categoriesCache = { ts: now, data: cats };
  return cats;
}

/**
 * Kategori bazlı ürün scrape
 */
async function scrapeCategory(page, url, categoryName) {
  await page.goto(url, { waitUntil: "networkidle" });
  await page.waitForTimeout(1200);

  const selector =
      "ul.products li.product, .product-grid .product, .product-card, .products .product";

  const products = await page.$$eval(
      selector,
      (nodes, cat) =>
          nodes.map((el) => {
            const nameEl =
                el.querySelector(".woocommerce-loop-product__title") ||
                el.querySelector(".title") ||
                el.querySelector("h2") ||
                el.querySelector("h3");

            const linkEl = el.querySelector("a") || el.closest("a");
            const imgEl = el.querySelector("img");

            const img =
                imgEl?.getAttribute("src") ||
                imgEl?.getAttribute("data-src") ||
                imgEl?.getAttribute("data-lazy-src") ||
                "";

            const priceWrapper = el.querySelector(".price") || el;
            const insEl = priceWrapper.querySelector("ins");
            const delEl = priceWrapper.querySelector("del");

            const priceCurrentText =
                insEl?.textContent || priceWrapper.textContent || "";
            const priceOriginalText = delEl?.textContent || "";

            return {
              name: nameEl?.textContent?.trim() || "Unnamed product",
              url: linkEl?.href || "",
              image_url: img,
              price_current_raw: priceCurrentText,
              price_original_raw: priceOriginalText,
              category: cat || "Diğer",
            };
          }),
      categoryName
  );

  return products;
}

function normalizeProducts(raw) {
  return raw.map((p) => {
    const current = parsePrice(p.price_current_raw);
    const originalRaw = p.price_original_raw || p.price_current_raw;
    const original = parsePrice(originalRaw);

    return {
      name: p.name,
      url: p.url || "",
      image_url: p.image_url || "",
      price_current: current ?? 0,
      price_original: original ?? current ?? 0,
      discount_pct: computeDiscount(original, current),
      category: p.category || "Diğer",
    };
  });
}

function dedupeByUrl(products) {
  const map = new Map();
  for (const p of products) {
    const key = p.url || `${p.name}-${p.image_url}`;
    if (!map.has(key)) {
      map.set(key, p);
    } else {
      const existing = map.get(key);
      if (existing.category !== p.category) {
        const set = new Set(
            `${existing.category}|${p.category}`
                .split("|")
                .map((s) => s.trim())
                .filter(Boolean)
        );
        existing.category = Array.from(set).join(" | ");
        map.set(key, existing);
      }
    }
  }
  return Array.from(map.values());
}

/**
 * /products?categories=sale,xyz
 * - key slug'ına göre filtre çalışır
 */
function filterCategories(all, query) {
  const q = (query.categories || "").toString().trim();
  if (!q) return all;

  const keys = new Set(q.split(",").map((s) => s.trim()).filter(Boolean));
  const filtered = all.filter((c) => keys.has(c.key));
  return filtered.length ? filtered : all;
}

/**
 * Kategorileri görmek için ayrı endpoint
 * + son scrape'den gelen product_count bilgisini de ekler.
 */
app.get("/categories", async (req, res) => {
  let browser;
  try {
    browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();
    const cats = await getCategories(page);

    const result = cats.map((c) => ({
      ...c,
      product_count: categoryCountsCache[c.key] ?? 0,
    }));

    res.json(result);
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "Failed to discover categories" });
  } finally {
    if (browser) await browser.close();
  }
});

app.get("/products", async (req, res) => {
  const now = Date.now();

  if (productsCache.data && now - productsCache.ts < PRODUCTS_TTL_MS) {
    return res.json(productsCache.data);
  }

  let browser;
  try {
    browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();

    const allCategories = await getCategories(page);
    const requested = filterCategories(allCategories, req.query);

    const allRaw = [];
    const perCategoryCount = new Map();

    for (const c of requested) {
      try {
        const items = await scrapeCategory(page, `${BASE}${c.path}`, c.name);
        allRaw.push(...items);

        // Bu kategoride bulunan ürün sayısını kaydet
        const prev = perCategoryCount.get(c.key) || 0;
        perCategoryCount.set(c.key, prev + items.length);
      } catch (e) {
        console.error("Error scraping category", c.path, e);
      }
    }

    // Son scrape sonuçlarını global cache'e yaz
    categoryCountsCache = Object.fromEntries(perCategoryCount);

    const normalized = normalizeProducts(allRaw);
    const deduped = dedupeByUrl(normalized);

    productsCache = { ts: Date.now(), data: deduped };
    res.json(deduped);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to scrape products" });
  } finally {
    if (browser) await browser.close();
  }
});

app.listen(PORT, () => {
  console.log(`Crawler service listening on http://localhost:${PORT}`);
});
