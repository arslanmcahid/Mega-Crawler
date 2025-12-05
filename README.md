# Mega-Gastro Poster Oluşturucu

Mega-Gastro e-ticaret sitesindeki ürünleri kullanarak indirim/poster broşürü hazırlayan full-stack bir uygulamadır.

## 📋 Proje Yapısı

Bu proje üç ana bileşenden oluşmaktadır:

1. **Crawler Service** (Node.js) - Mega-Gastro sitesinden ürünleri çeken servis
2. **Backend** (Spring Boot) - Ürün yönetimi ve PDF poster oluşturma API'si
3. **Frontend** (Next.js) - Kullanıcı arayüzü

## 🚀 Gereksinimler

- **Node.js** 20+ (Crawler ve Frontend için)
- **Java** 17+ (Backend için)
- **Maven** 3.6+ (Backend için)
- **npm** veya **yarn** (Node.js paket yöneticisi)

## 📦 Kurulum

### 1. Crawler Service

```bash
cd crawler-service
npm install
```

**Not:** İlk kurulumda Playwright tarayıcılarını indirmek için:

```bash
npx playwright install
```

### 2. Backend (Spring Boot)

```bash
cd mega-gastro-poster-backend
mvn clean install
```

### 3. Frontend (Next.js)

```bash
cd mega-gastro-poster-frontend
npm install
```

Frontend için `.env.local` dosyası oluşturun:

```bash
echo "NEXT_PUBLIC_BACKEND_URL=http://localhost:8080" > .env.local
```

## ▶️ Çalıştırma

Üç servisi de ayrı terminal pencerelerinde çalıştırmanız gerekmektedir.

### 1. Crawler Service'i Başlat

```bash
cd crawler-service
npm run start
```

Servis `http://localhost:4000` adresinde çalışacaktır.

**Test:**

```bash
curl http://localhost:4000/products
```

### 2. Backend'i Başlat

```bash
cd mega-gastro-poster-backend
mvn spring-boot:run
```

Backend `http://localhost:8080` adresinde çalışacaktır.

**API Endpoint'leri:**

- `GET /api/products` - Tüm ürünleri listele
- `POST /api/products` - Custom ürün ekle (JSON)
- `POST /api/products/upload` - Custom ürün ekle (Multipart dosya ile)
- `POST /api/poster` - PDF poster oluştur

### 3. Frontend'i Başlat

```bash
cd mega-gastro-poster-frontend
npm run dev
```

Frontend `http://localhost:3000` adresinde çalışacaktır.

## 🎯 Kullanım

### Ana Sayfa

1. Tarayıcıda `http://localhost:3000` adresine gidin.

### 1. Siteden Ürün Seçme

- Sol tarafta **"1. Siteden Ürün Seç"** bölümünde:
  - Üstteki arama kutusuna ürün adı yazarak filtreleme yapabilirsiniz
  - Tabloda görünen ürünlerin yanındaki checkbox'ları işaretleyerek postere eklenecek ürünleri seçebilirsiniz
  - Ürünler **REMOTE** (siteden çekilen) veya **CUSTOM** (manuel eklenen) olarak işaretlenir

### 2. Kendi Ürünlerini Ekleme

- Sağ tarafta **"2. Kendi Ürünlerini Ekle"** bölümünde:
  - **Ürün Adı** (zorunlu)
  - **Güncel Fiyat** (zorunlu)
  - **Eski Fiyat** (opsiyonel)
  - **İndirim Yüzdesi** (opsiyonel)
  - **Görsel** için iki seçenek:
    - **Görsel URL**: Bir URL girerek görsel ekleyebilirsiniz
    - **Dosya Yükle**: Bilgisayarınızdan bir görsel dosyası yükleyebilirsiniz
  - **"Custom Ürün Ekle"** butonuna tıklayarak ürünü kaydedin
  - Eklenen ürünler otomatik olarak sol taraftaki listede görünecektir

### 3. Poster Oluşturma

- Sağ tarafta **"3. Poster Ayarları ve Seçili Ürünler"** bölümünde:
  - **Poster Başlığı** alanına istediğiniz başlığı girin (varsayılan: "Angebote der Woche")
  - Seçili ürünlerin listesini görebilirsiniz
  - **"Poster Oluştur (X ürün)"** butonuna tıklayın
  - PDF dosyası otomatik olarak indirilecektir (`poster.pdf`)

### Poster Özellikleri

- Poster A4 boyutunda oluşturulur
- Ürünler 3 sütunlu grid düzeninde gösterilir
- Her ürün kartında:
  - Ürün görseli
  - Ürün adı
  - İndirimli fiyat (kırmızı, kalın)
  - Eski fiyat (üstü çizili, gri - varsa)
  - İndirim rozeti (sol üst köşede, varsa)
- Alt kısımda oluşturulma tarihi gösterilir

## 🏗️ Teknik Detaylar

### Crawler Service

- **Teknoloji:** Node.js, Playwright, Express
- **Port:** 4000
- **Endpoint:** `GET /products`
- Mega-Gastro sitesinden ürün bilgilerini çeker:
  - Ürün adı
  - URL
  - Görsel URL
  - Güncel fiyat
  - Eski fiyat
  - İndirim yüzdesi

### Backend

- **Teknoloji:** Spring Boot 3.3.5, Java 17
- **Port:** 8080
- **PDF Kütüphanesi:** openhtmltopdf
- **Özellikler:**
  - Remote ürünleri crawler servisinden alır
  - Custom ürünleri in-memory repository'de saklar
  - HTML'den PDF oluşturur
  - Dosya upload desteği (`uploads/` klasörü)

### Frontend

- **Teknoloji:** Next.js 14 (App Router), React 18, TypeScript
- **Port:** 3000
- **Data Fetching:** SWR + Axios
- **Özellikler:**
  - Gerçek zamanlı ürün listesi
  - Ürün arama/filtreleme
  - Custom ürün ekleme (URL veya dosya upload)
  - PDF poster indirme

## 📁 Proje Yapısı

```
mega-poster/
├── crawler-service/
│   ├── src/
│   │   └── server.js
│   └── package.json
├── mega-gastro-poster-backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/megagastro/poster/
│   │       │   ├── controller/
│   │       │   ├── service/
│   │       │   ├── repository/
│   │       │   ├── model/
│   │       │   └── dto/
│   │       └── resources/
│   │           └── application.yml
│   └── pom.xml
├── mega-gastro-poster-frontend/
│   ├── app/
│   │   ├── page.tsx
│   │   ├── layout.tsx
│   │   ├── globals.css
│   │   └── CustomProductForm.tsx
│   ├── lib/
│   │   └── api.ts
│   ├── types/
│   │   └── product.ts
│   └── package.json
└── README.md
```

## 🔧 Konfigürasyon

### Backend Konfigürasyonu

`mega-gastro-poster-backend/src/main/resources/application.yml` dosyasında:

```yaml
server:
  port: 8080

crawler:
  base-url: http://localhost:4000

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB
```

### Frontend Konfigürasyonu

`mega-gastro-poster-frontend/.env.local` dosyasında:

```env
NEXT_PUBLIC_BACKEND_URL=http://localhost:8080
```

## 🐛 Sorun Giderme

### Crawler çalışmıyor

- Playwright tarayıcılarının yüklü olduğundan emin olun: `npx playwright install`
- Mega-Gastro sitesinin erişilebilir olduğunu kontrol edin

### Backend başlamıyor

- Java 17+ yüklü olduğundan emin olun: `java -version`
- Maven bağımlılıklarının indirildiğini kontrol edin: `mvn clean install`
- Port 8080'in kullanılabilir olduğunu kontrol edin

### Frontend başlamıyor

- Node.js 20+ yüklü olduğundan emin olun: `node -version`
- `.env.local` dosyasının oluşturulduğunu kontrol edin
- Port 3000'in kullanılabilir olduğunu kontrol edin

### PDF oluşturulmuyor

- Backend'in çalıştığından emin olun
- Seçili ürünlerin olduğundan emin olun
- Tarayıcı konsolunda hata mesajlarını kontrol edin

## 📝 Notlar

- **node_modules** klasörleri Git'e commit edilmez (`.gitignore` ile)
- Backend'deki `uploads/` klasörü yüklenen görselleri saklar
- Custom ürünler in-memory saklanır (uygulama yeniden başlatıldığında kaybolur)
- Remote ürünler her istekte crawler servisinden çekilir

## 📄 Lisans

Bu proje eğitim/öğrenme amaçlıdır.

## 👤 Geliştirici

Proje geliştirici tarafından oluşturulmuştur.

---

**İyi kullanımlar! 🎉**
