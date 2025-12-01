'use client';

import useSWR from 'swr';
import api from '../lib/api';
import type { Product } from '../types/product';
import CustomProductForm from './CustomProductForm';
import { useMemo, useState } from 'react';

const fetcher = (url: string) => api.get<Product[]>(url).then((res) => res.data);

export default function HomePage() {
  const { data: products, isLoading, mutate } = useSWR<Product[]>('/api/products', fetcher);
  const [selected, setSelected] = useState<string[]>([]);
  const [title, setTitle] = useState<string>('Haftanın Fırsatları');
  const [search, setSearch] = useState<string>('');

  const toggle = (id: string) => {
    setSelected((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  };

  const createPoster = async () => {
    if (selected.length === 0) return;
    try {
      const res = await api.post(
        '/api/poster',
        {
          productIds: selected,
          title
        },
        { responseType: 'blob' }
      );

      const blob = new Blob([res.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'poster.pdf';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      alert('Poster oluşturulurken hata oluştu.');
    }
  };

  const filteredProducts = useMemo(() => {
    if (!products) return [];
    if (!search.trim()) return products;
    const term = search.toLowerCase();
    return products.filter((p) => p.name.toLowerCase().includes(term));
  }, [products, search]);

  const selectedProducts = useMemo(() => {
    if (!products) return [];
    const setIds = new Set(selected);
    return products.filter((p) => setIds.has(p.id));
  }, [products, selected]);

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
                <div style={{ fontSize: 13, color: '#6b7280' }}>
                  Listeden istediğin ürünleri işaretle; isme göre arama yapabilirsin.
                </div>
              </div>
              <input
                type="text"
                className="search-input"
                placeholder="İsme göre ara..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            {isLoading && <div>Yükleniyor...</div>}

            {filteredProducts && filteredProducts.length > 0 && (
              <table>
                <thead>
                  <tr>
                    <th></th>
                    <th>Görsel</th>
                    <th>İsim</th>
                    <th>Fiyat</th>
                    <th>Eski Fiyat</th>
                    <th>İndirim</th>
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
                        {p.imageUrl && <img src={p.imageUrl} alt={p.name} className="thumb" />}
                      </td>
                      <td>{p.name}</td>
                      <td>{p.priceCurrent?.toFixed(2)}</td>
                      <td>{p.priceOriginal != null ? p.priceOriginal.toFixed(2) : ''}</td>
                      <td>{p.discountPct != null ? `${p.discountPct}%` : ''}</td>
                      <td>
                        {p.source === 'REMOTE' ? (
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

            {filteredProducts && filteredProducts.length === 0 && !isLoading && (
              <div style={{ fontSize: 13, color: '#6b7280' }}>
                Arama kriterine uyan ürün bulunamadı.
              </div>
            )}
          </div>
        </div>

        {/* Sağ sütun: Kullanıcının kendi ürünleri ve seçili ürünler */}
        <div>
          <div className="section" style={{ marginBottom: 16 }}>
            <h2 style={{ marginTop: 0 }}>2. Kendi Ürünlerini Ekle</h2>
            <div style={{ fontSize: 13, color: '#6b7280', marginBottom: 8 }}>
              Sitede olmayan ürünleri ismi, fiyatı ve isteğe bağlı görsel (URL veya dosya) ile
              ekleyebilirsin. Bu ürünler de listede görünecek ve seçilebilecek.
            </div>
            <CustomProductForm
              onCreated={() => {
                mutate();
              }}
            />
          </div>

          <div className="section">
            <h2 style={{ marginTop: 0 }}>3. Poster Ayarları ve Seçili Ürünler</h2>
            <label style={{ display: 'block', fontSize: 13, marginBottom: 6 }}>
              Poster Başlığı
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              style={{ width: '100%', marginBottom: 12 }}
            />

            <button
              className="btn-primary"
              onClick={createPoster}
              disabled={selected.length === 0}
            >
              Poster Oluştur ({selected.length} ürün)
            </button>

            <div style={{ marginTop: 12, fontSize: 13, color: '#6b7280' }}>
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
                  <span style={{ fontSize: 13, color: '#9ca3af' }}>
                    Henüz ürün seçmedin. Sol taraftan ürünleri işaretle veya üstte custom ürün ekle.
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


