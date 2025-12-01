import express from 'express';
import cors from 'cors';
import { chromium } from 'playwright';

const app = express();
const PORT = process.env.PORT || 4000;

const BASE = 'https://www.mega-gastro.at';

app.use(cors());

function parsePrice(text) {
  if (!text) return null;
  const cleaned = text
    .replace(/[^\d,.\s]/g, '')
    .replace(/\s/g, '')
    .replace(/\.(?=\d{3}(\D|$))/g, '')
    .replace(',', '.');
  const value = parseFloat(cleaned);
  return isNaN(value) ? null : value;
}

async function scrapeCategory(page, url) {
  await page.goto(url, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);

  const products = await page.$$eval(
    'ul.products li.product, .product-grid .product, .product-card, .products .product',
    (nodes) =>
      nodes.map((el) => {
        const nameEl =
          el.querySelector('.woocommerce-loop-product__title') ||
          el.querySelector('.title') ||
          el.querySelector('h2') ||
          el.querySelector('h3');

        const linkEl = el.querySelector('a') || el.closest('a');
        const imgEl = el.querySelector('img');
        const priceWrapper = el.querySelector('.price') || el;
        const insEl = priceWrapper.querySelector('ins');
        const delEl = priceWrapper.querySelector('del');

        const priceCurrentText = insEl?.textContent || priceWrapper.textContent || '';
        const priceOriginalText = delEl?.textContent || '';

        return {
          name: nameEl?.textContent?.trim() || 'Unnamed product',
          url: linkEl?.href || '',
          image_url: imgEl?.src || '',
          price_current_raw: priceCurrentText,
          price_original_raw: priceOriginalText
        };
      })
  );

  return products;
}

app.get('/products', async (req, res) => {
  const categoryPaths = [
    '/produkt-kategorie/gastro-kuechengeraete/',
    '/produkt-kategorie/sale/',
  ];

  let browser;
  try {
    browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();

    const all = [];
    for (const path of categoryPaths) {
      try {
        const items = await scrapeCategory(page, `${BASE}${path}`);
        all.push(...items);
      } catch (e) {
        console.error('Error scraping category', path, e);
      }
    }

    const normalized = all.map((p) => {
      const current = parsePrice(p.price_current_raw);
      const originalRaw = p.price_original_raw || p.price_current_raw;
      const original = parsePrice(originalRaw);

      let discountPct = null;
      if (original && current && original > current) {
        discountPct = Math.round(((original - current) / original) * 100);
      }

      return {
        name: p.name,
        url: p.url || '',
        image_url: p.image_url || '',
        price_current: current ?? 0,
        price_original: original ?? current ?? 0,
        discount_pct: discountPct ?? 0,
      };
    });

    res.json(normalized);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Failed to scrape products' });
  } finally {
    if (browser) {
      await browser.close();
    }
  }
});

app.listen(PORT, () => {
  console.log(`Crawler service listening on http://localhost:${PORT}`);
});


