'use client';

import { useState, FormEvent } from 'react';
import useSWRMutation from 'swr/mutation';
import api from '../lib/api';
import type { Product } from '../types/product';

type ImageMode = 'url' | 'file';

interface Props {
  onCreated?: (product: Product) => void;
}

async function createByUrl(_key: string, { arg }: { arg: any }) {
  const res = await api.post<Product>('/api/products', arg);
  return res.data;
}

async function createByFile(_key: string, { arg }: { arg: FormData }) {
  const res = await api.post<Product>('/api/products/upload', arg, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return res.data;
}

export default function CustomProductForm({ onCreated }: Props) {
  const [name, setName] = useState('');
  const [priceCurrent, setPriceCurrent] = useState('');
  const [priceOriginal, setPriceOriginal] = useState('');
  const [discountPct, setDiscountPct] = useState('');
  const [imageMode, setImageMode] = useState<ImageMode>('url');
  const [imageUrl, setImageUrl] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const urlMutation = useSWRMutation('/api/products', createByUrl);
  const fileMutation = useSWRMutation('/api/products/upload', createByFile);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setMessage(null);

    try {
      if (!name || !priceCurrent) {
        setMessage('İsim ve güncel fiyat zorunludur.');
        return;
      }

      if (imageMode === 'url') {
        const payload: any = {
          name,
          priceCurrent: Number(priceCurrent),
          priceOriginal: priceOriginal ? Number(priceOriginal) : undefined,
          discountPct: discountPct ? Number(discountPct) : undefined,
          imageUrl: imageUrl || undefined
        };
        const product = await urlMutation.trigger(payload);
        setMessage('Ürün eklendi.');
        onCreated?.(product);
      } else {
        if (!file) {
          setMessage('Lütfen bir görsel dosyası seçin.');
          return;
        }
        const fd = new FormData();
        fd.append('name', name);
        fd.append('priceCurrent', priceCurrent);
        if (priceOriginal) fd.append('priceOriginal', priceOriginal);
        if (discountPct) fd.append('discountPct', discountPct);
        fd.append('file', file);

        const product = await fileMutation.trigger(fd);
        setMessage('Ürün eklendi.');
        onCreated?.(product);
      }

      setName('');
      setPriceCurrent('');
      setPriceOriginal('');
      setDiscountPct('');
      setImageUrl('');
      setFile(null);
    } catch (err) {
      console.error(err);
      setMessage('Ürün eklenirken bir hata oluştu.');
    }
  };

  return (
    <form onSubmit={onSubmit}>
      <div className="form-row">
        <label>
          İsim *
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Güncel Fiyat *
          <input
            type="number"
            step="0.01"
            value={priceCurrent}
            onChange={(e) => setPriceCurrent(e.target.value)}
            required
          />
        </label>
        <label>
          Eski Fiyat
          <input
            type="number"
            step="0.01"
            value={priceOriginal}
            onChange={(e) => setPriceOriginal(e.target.value)}
          />
        </label>
        <label>
          İndirim %
          <input
            type="number"
            step="1"
            value={discountPct}
            onChange={(e) => setDiscountPct(e.target.value)}
          />
        </label>
      </div>

      <div className="radio-row">
        <label>
          <input
            type="radio"
            name="imageMode"
            value="url"
            checked={imageMode === 'url'}
            onChange={() => setImageMode('url')}
          />{' '}
          Görsel URL
        </label>
        <label>
          <input
            type="radio"
            name="imageMode"
            value="file"
            checked={imageMode === 'file'}
            onChange={() => setImageMode('file')}
          />{' '}
          Dosya Yükle
        </label>
      </div>

      {imageMode === 'url' ? (
        <div className="form-row">
          <label>
            Görsel URL
            <input
              type="text"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="https://..."
            />
          </label>
        </div>
      ) : (
        <div className="form-row">
          <label>
            Görsel Dosya
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </label>
        </div>
      )}

      <button className="btn-primary" type="submit" disabled={urlMutation.isMutating || fileMutation.isMutating}>
        Custom Ürün Ekle
      </button>

      {message && (
        <div style={{ marginTop: 8, fontSize: 13 }}>
          {message}
        </div>
      )}
    </form>
  );
}


