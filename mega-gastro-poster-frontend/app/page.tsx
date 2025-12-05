"use client";

import api from "../lib/api";
import type { Product } from "../types/product";
import CustomProductForm from "./CustomProductForm";
import { useMemo, useState, useEffect } from "react";

export default function HomePage() {
  const [selected, setSelected] = useState<string[]>([]);
  const [title, setTitle] = useState<string>("Angebote der Woche");
  const [search, setSearch] = useState<string>("");
  const [sortField, setSortField] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc");
  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Sayfa açılınca tüm ürünleri yükle
  useEffect(() => {
    const loadProducts = async () => {
      setIsLoading(true);
      try {
        const response = await api.get<Product[]>("/api/products");
        setAllProducts(response.data || []);
      } catch (error) {
        console.error("Ürünler yüklenirken hata:", error);
        setAllProducts([]);
      } finally {
        setIsLoading(false);
      }
    };

    loadProducts();
  }, []);

  const toggle = (id: string) => {
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  };

  const handleSort = (field: string) => {
    if (sortField === field) {
      // Aynı alana tıklandıysa yönü değiştir
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      // Farklı alana tıklandıysa yeni alanı seç ve varsayılan olarak artan sırala
      setSortField(field);
      setSortDirection("asc");
    }
  };

  const createPoster = async () => {
    if (selected.length === 0) return;
    try {
      // Maksimum 9 ürün al
      const productsForPoster = selected.slice(0, 9);

      const res = await api.post(
        "/api/poster",
        {
          productIds: productsForPoster,
          title,
        },
        { responseType: "blob" }
      );

      const blob = new Blob([res.data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "poster.pdf";
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);

      // İlk 9 ürünü seçili listeden çıkar
      if (selected.length > 9) {
        setSelected((prev) => prev.slice(9));
      } else {
        // 9 veya daha az ürün varsa hepsini temizle
        setSelected([]);
      }
    } catch (err) {
      console.error(err);
      alert("Poster oluşturulurken hata oluştu.");
    }
  };

  // Client-side arama ve sıralama
  const filteredProducts = useMemo(() => {
    // En az 3 karakter girilmedikçe hiçbir şey gösterme
    if (!search.trim() || search.trim().length < 3) {
      return [];
    }

    if (allProducts.length === 0) return [];

    // Arama terimine göre filtreleme
    const term = search.toLowerCase();
    let filtered = allProducts.filter(
      (p) => p.name && p.name.toLowerCase().includes(term)
    );

    // Sıralama (eğer sortField varsa)
    if (sortField) {
      filtered = [...filtered].sort((a, b) => {
        if (sortField === "name") {
          const result = a.name.localeCompare(b.name, "tr", {
            sensitivity: "base",
          });
          return sortDirection === "asc" ? result : -result;
        }

        if (sortField === "discount") {
          const discountA = a.discountPct ?? 0;
          const discountB = b.discountPct ?? 0;
          const result = discountA - discountB;
          return sortDirection === "asc" ? result : -result;
        }

        return 0;
      });
    }

    return filtered;
  }, [allProducts, search, sortField, sortDirection]);

  // Seçili ürünleri bul (tüm ürünlerden)
  const selectedProducts = useMemo(() => {
    if (allProducts.length === 0) return [];
    const setIds = new Set(selected);
    return allProducts.filter((p) => setIds.has(p.id));
  }, [allProducts, selected]);

  return (
    <div className="container">
      <h1>Mega-Gastro Poster Oluşturucu</h1>

      <div className="layout-grid">
        {/* Sol sütun: Sitedeki ürünlerden seçim */}
        <div>
          <div className="section">
            <div className="section-header">
              <div>
                <h2 style={{ margin: 0 }}>1. Siteden Ürün Seç</h2>
                <div style={{ fontSize: 13, color: "#6b7280" }}>
                  Listeden istediğin ürünleri işaretle; isme göre arama
                  yapabilirsin.
                </div>
              </div>
              <input
                type="text"
                className="search-input"
                placeholder="En az 3 karakter yazarak ara..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            {isLoading && (
              <div
                style={{ fontSize: 13, color: "#6b7280", padding: "16px 0" }}
              >
                Ürünler yükleniyor...
              </div>
            )}

            {!isLoading && (!search.trim() || search.trim().length < 3) && (
              <div
                style={{ fontSize: 13, color: "#6b7280", padding: "16px 0" }}
              >
                {search.trim().length === 0
                  ? "Ürünleri görmek için yukarıdaki arama kutusuna en az 3 karakter yazın."
                  : `Arama yapmak için en az 3 karakter yazmanız gerekiyor. (${
                      search.trim().length
                    }/3)`}
              </div>
            )}

            {!isLoading &&
              search.trim().length >= 3 &&
              filteredProducts.length === 0 && (
                <div
                  style={{ fontSize: 13, color: "#6b7280", padding: "16px 0" }}
                >
                  Arama kriterine uyan ürün bulunamadı.
                </div>
              )}

            {filteredProducts && filteredProducts.length > 0 && (
              <table>
                <thead>
                  <tr>
                    <th></th>
                    <th>Görsel</th>
                    <th
                      className="sortable-header"
                      onClick={() => handleSort("name")}
                      style={{ cursor: "pointer", userSelect: "none" }}
                    >
                      İsim
                      {sortField === "name" && (
                        <span className="sort-indicator">
                          {sortDirection === "asc" ? "↑" : "↓"}
                        </span>
                      )}
                    </th>
                    <th>Fiyat</th>
                    <th>Eski Fiyat</th>
                    <th
                      className="sortable-header"
                      onClick={() => handleSort("discount")}
                      style={{ cursor: "pointer", userSelect: "none" }}
                    >
                      İndirim
                      {sortField === "discount" && (
                        <span className="sort-indicator">
                          {sortDirection === "asc" ? "↑" : "↓"}
                        </span>
                      )}
                    </th>
                    <th>Kaynak</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredProducts.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <input
                          type="checkbox"
                          checked={selected.includes(p.id)}
                          onChange={() => toggle(p.id)}
                        />
                      </td>
                      <td>
                        {p.imageUrl && (
                          <img
                            src={p.imageUrl}
                            alt={p.name}
                            className="thumb"
                          />
                        )}
                      </td>
                      <td>{p.name}</td>
                      <td>{p.priceCurrent?.toFixed(2)}</td>
                      <td>
                        {p.priceOriginal != null
                          ? p.priceOriginal.toFixed(2)
                          : ""}
                      </td>
                      <td>
                        {p.discountPct != null ? `${p.discountPct}%` : ""}
                      </td>
                      <td>
                        {p.source === "REMOTE" ? (
                          <span className="badge-remote">REMOTE</span>
                        ) : (
                          <span className="badge-custom">CUSTOM</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Sağ sütun: Kullanıcının kendi ürünleri ve seçili ürünler */}
        <div>
          <div className="section" style={{ marginBottom: 16 }}>
            <h2 style={{ marginTop: 0 }}>2. Kendi Ürünlerini Ekle</h2>
            <div style={{ fontSize: 13, color: "#6b7280", marginBottom: 8 }}>
              Sitede olmayan ürünleri ismi, fiyatı ve isteğe bağlı görsel (URL
              veya dosya) ile ekleyebilirsin. Bu ürünler de listede görünecek ve
              seçilebilecek.
            </div>
            <CustomProductForm
              onCreated={async (newProduct) => {
                // Yeni ürünü allProducts listesine ekle
                if (newProduct) {
                  setAllProducts((prev) => {
                    const exists = prev.some((p) => p.id === newProduct.id);
                    if (!exists) {
                      return [...prev, newProduct];
                    }
                    return prev;
                  });
                }

                // Yeni eklenen ürünü otomatik olarak seçili listeye ekle
                if (newProduct && newProduct.id) {
                  setSelected((prev) => {
                    if (!prev.includes(newProduct.id)) {
                      return [...prev, newProduct.id];
                    }
                    return prev;
                  });
                }
              }}
            />
          </div>

          <div className="section">
            <h2 style={{ marginTop: 0 }}>
              3. Poster Ayarları ve Seçili Ürünler
            </h2>
            <label style={{ display: "block", fontSize: 13, marginBottom: 6 }}>
              Poster Başlığı
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={{ width: "100%", marginBottom: 12 }}
            />

            <button
              className="btn-primary"
              onClick={createPoster}
              disabled={selected.length === 0}
            >
              {selected.length > 9
                ? `Poster Oluştur (ilk 9 ürün, ${selected.length} seçili)`
                : `Poster Oluştur (${selected.length} ürün)`}
            </button>

            <div style={{ marginTop: 12, fontSize: 13, color: "#6b7280" }}>
              Aşağıda şu an postere eklenecek ürünleri görüyorsun:
            </div>
            <ul className="selected-list">
              {selectedProducts.map((p) => (
                <li key={p.id}>
                  <span className="selected-name">{p.name}</span>
                  <span className="selected-meta">
                    {p.priceCurrent.toFixed(2)} € · {p.source}
                  </span>
                </li>
              ))}
              {selectedProducts.length === 0 && (
                <li>
                  <span style={{ fontSize: 13, color: "#9ca3af" }}>
                    Henüz ürün seçmedin. Sol taraftan ürünleri işaretle veya
                    üstte custom ürün ekle.
                  </span>
                </li>
              )}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
