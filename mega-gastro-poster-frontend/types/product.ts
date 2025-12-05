export type ProductSource = 'REMOTE' | 'CUSTOM';

export interface Product {
  id: string;
  name: string;
  url?: string | null;
  imageUrl?: string | null;
  priceCurrent: number;
  priceOriginal?: number | null;
  discountPct?: number | null;
  source: ProductSource;
  category?: string | null;
}


