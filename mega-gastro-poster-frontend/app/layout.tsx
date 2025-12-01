import type { ReactNode } from 'react';
import './globals.css';

export const metadata = {
  title: 'Mega Gastro Poster',
  description: 'Mega-Gastro ürünlerinden poster oluşturma aracı'
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="tr">
      <body>{children}</body>
    </html>
  );
}


