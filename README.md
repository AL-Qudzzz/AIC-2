# RouteWise AI

> Platform AI Fleet Copilot untuk Optimasi Logistik Last-Mile Delivery

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2024%2B%20%7C%20Target%2036-green.svg?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4.svg?style=flat-square)](https://developer.android.com/jetpack/compose)
[![ONNX Runtime](https://img.shields.io/badge/AI%20Inference-ONNX%20Runtime%20Android-005CED.svg?style=flat-square)](https://onnxruntime.ai)
[![Google Gemini](https://img.shields.io/badge/Generative%20AI-Gemini%202.5%20Flash-8E75B2.svg?style=flat-square)](https://ai.google.dev)
[![OpenStreetMap](https://img.shields.io/badge/Maps-osmdroid%20%2B%20OSRM-7EBC6F.svg?style=flat-square)](https://www.openstreetmap.org)
[![Room Database](https://img.shields.io/badge/Local%20DB-Room%20Persistence-orange.svg?style=flat-square)](https://developer.android.com/training/data-storage/room)

---

## Ringkasan Eksekutif

RouteWise AI adalah solusi kecerdasan armada berbasis Android yang dirancang khusus untuk mengatasi tantangan operasional *last-mile delivery* di Indonesia. Berbeda dengan aplikasi navigasi umum yang hanya berfokus pada pencarian jalan tercepat, RouteWise AI mengintegrasikan variabel bisnis logistik nyata seperti kapasitas kendaraan, prioritas paket, profil konsumsi BBM, prediksi kemacetan proaktif, serta probabilitas risiko kegagalan pengiriman ke dalam satu ekosistem terpadu.

Aplikasi ini bertindak sebagai **AI Fleet Copilot**, mendampingi kurir di lapangan dengan panduan navigasi cerdas dan memberikan analitik komprehensif bagi manajer armada untuk menekan biaya operasional distribusi.

---

## Perbandingan: Navigasi Konvensional vs RouteWise AI

| Parameter Evaluasi | Navigasi Konvensional (Maps / Waze) | RouteWise AI Fleet Copilot |
|---|---|---|
| Fokus Utama | Titik A ke Titik B (Perjalanan tunggal) | Optimasi multi-stop Vehicle Routing Problem (VRP) |
| Pertimbangan Biaya | Tidak memperhitungkan konsumsi BBM | Estimasi biaya bahan bakar riil per rute dan per paket |
| Prediksi Kemacetan | Reaktif terhadap titik macet yang sudah terjadi | Proaktif dengan pemodelan rute alternatif hemat waktu |
| Mitigasi Risiko | Tidak memiliki konteks penerima paket | Deteksi dini risiko gagal kirim berbasis alamat & waktu |
| Asisten Percakapan | Perintah suara dasar | Integrasi LLM Gemini untuk konsultasi logistik interaktif |
| Pemrosesan AI | Bergantung penuh pada koneksi cloud | On-device ML Inference (ONNX Runtime) offline-first |

---

## Fitur Utama

### 1. Smart Route Optimization (VRP Engine)
- Menghitung urutan pengiriman multi-titik paling optimal secara otomatis.
- Menggabungkan model Machine Learning berbasis ONNX Runtime dengan algoritma heuristik Vehicle Routing Problem.
- Memperhitungkan batas waktu pengiriman (*time slot*), prioritas paket (Reguler vs Ekspres), dan geolokasi aktual.

### 2. AI Delivery Failure & Risk Prediction
- Penilaian risiko kegagalan pengiriman (*failure risk assessment*) secara otomatis untuk setiap paket.
- Analisis pola berbasis teks alamat (gang sempit, ruko, pasar), rentang waktu istirahat pelanggan, dan catatan penerima.
- Memberikan klasifikasi tingkat risiko (*LOW*, *MEDIUM*, *HIGH*) beserta rekomendasi tindakan konkret (misal: draf konfirmasi pesan atau opsi titip tetangga/satpam).

### 3. AI Traffic Management & Proactive Re-routing
- Pemantauan kondisi lalu lintas pada rute aktif dengan deteksi ruas rawan macet atau proyek perbaikan jalan.
- Notifikasi peringatan macet disertai rekomendasi rute alternatif beserta estimasi waktu yang dapat dihemat.
- Penerapan rute alternatif ke antrean pengiriman hanya dengan satu sentuhan (*one-tap apply*).

### 4. Fuel Cost Analytics & Vehicle Profiling
- Perhitungan estimasi konsumsi liter bahan bakar dan biaya operasional (Rupiah) secara langsung.
- Kustomisasi profil kendaraan: Motor Matik, Motor Bebek, hingga Mobil Van/Box.
- Fleksibilitas pemilihan jenis bahan bakar (Pertalite, Pertamax, Solar) dan penyesuaian tarif per liter.

### 5. In-App Map & Turn-by-Turn Navigation
- Tampilan peta interaktif berbasis OpenStreetMap (`osmdroid`) tanpa ketergantungan API berbayar.
- Mesin rute jalan raya terintegrasi menggunakan OSRM (Open Source Routing Machine) untuk kalkulasi polyline presisi.
- Fitur pencarian alamat dan geocoding otomatis dengan integrasi Nominatim API.
- Mode navigasi visual berkendara dengan informasi jarak real-time dan status tujuan berikutnya.

### 6. AI Fleet Copilot (Gemini Assistant)
- Asisten virtual bertenaga Generative AI Google Gemini (v1beta `gemini-2.5-flash`).
- Menyediakan konsultasi strategi rute, efisiensi bahan bakar, ringkasan kinerja shift, hingga pembuatan draf pesan pengantaran ke pelanggan secara otomatis.
- Terkoneksi secara kontekstual dengan data operasional paket yang sedang aktif.

### 7. Status Management & Daily Summary Dashboard
- Pembaruan status paket secara langsung (*Belum Dimulai*, *Dalam Perjalanan*, *Terkirim*, *Gagal Kirim*).
- Input bukti pengiriman berupa catatan lokasi, nama penerima, atau alasan kegagalan.
- Dashboard rekapitulasi harian yang memuat total paket terkirim, rasio keberhasilan, total jarak tempuh, dan efisiensi biaya.

---

## Arsitektur Sistem

```
+-------------------------------------------------------------------------+
|                              PRESENTATION                               |
|   Jetpack Compose UI  |  Material 3 Components  |  osmdroid Map Layer   |
|   (RouteScreen, MapScreen, FuelProfileScreen, DailySummaryScreen, etc.) |
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                            STATE & VIEWMODEL                            |
|             RouteWiseViewModel    <--->    SharedRouteViewModel         |
|              (UI State Flow)              (Map & Routing Sync)          |
+-------------------------------------------------------------------------+
                                    |
            +-----------------------+-----------------------+
            |                                               |
            v                                               v
+-----------------------+                       +-----------------------+
|     LOCAL STORAGE     |                       |    AI & ML SERVICES   |
|  Room SQLite Database |                       |  - ONNX Runtime Engine|
|  - PackageDao         |                       |    (vrp_model.onnx)   |
|  - ChatDao            |                       |  - Failure Risk Engine|
+-----------------------+                       |  - Gemini 2.5 API     |
            |                                   +-----------------------+
            |                                               |
            +-----------------------+-----------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|                            EXTERNAL SERVICES                            |
|     OSRM Routing API     |    Nominatim Geocoding    |   OSM Tile Server|
+-------------------------------------------------------------------------+
```

---

## Struktur Direktori Proyek

```
routewise-ai/
|-- app/
|   |-- src/
|   |   |-- main/
|   |   |   |-- assets/
|   |   |   |   `-- models/
|   |   |   |       |-- vrp_model.onnx          # Model inferensi ONNX untuk optimasi rute
|   |   |   |       `-- vrp_model.joblib        # Model scikit-learn backup
|   |   |   |-- java/com/example/
|   |   |   |   |-- MainActivity.kt             # Entry point & navigasi tab aplikasi
|   |   |   |   |-- data/
|   |   |   |   |   |-- api/                    # Integrasi Retrofit & Google Gemini Client
|   |   |   |   |   |-- local/                  # Room Database, DAOs, & Entities
|   |   |   |   |   |-- ml/                     # On-device Inference (ONNX & Failure ML)
|   |   |   |   |   |-- model/                  # Data classes (DeliveryPackage, Profile, dsb)
|   |   |   |   |   |-- repository/             # Single source of truth repository
|   |   |   |   |   `-- service/                # OSRM & Geocoding network services
|   |   |   |   `-- ui/
|   |   |   |       |-- RouteWiseViewModel.kt   # ViewModel utama untuk data operasional
|   |   |   |       |-- SharedRouteViewModel.kt # Scoped ViewModel untuk peta & navigasi
|   |   |   |       |-- components/             # Reusable Compose UI components
|   |   |   |       |-- screens/                # Layar utama aplikasi (Rute, Peta, BBM, Summary)
|   |   |   |       `-- theme/                  # Konfigurasi tema Material 3
|   |   |   `-- AndroidManifest.xml
|   |   `-- res/                                # Drawable, layout, dan resource XML
|   `-- build.gradle.kts
|-- .env.example                                # Contoh konfigurasi variabel lingkungan
|-- build.gradle.kts                            # Konfigurasi build root
|-- settings.gradle.kts                         # Konfigurasi module & plugin Gradle
`-- README.md
```

---

## Teknologi & Dependensi

### Mobile & Antarmuka
- **Bahasa:** Kotlin (Java 17 compatibility)
- **Framework UI:** Jetpack Compose dengan Material Design 3
- **Komponen Arsitektur:** AndroidX Lifecycle, ViewModel Compose, Coroutines, StateFlow

### Machine Learning & Kecerdasan Buatan
- **On-Device Inference:** Microsoft ONNX Runtime Android (`ai.onnxruntime:onnxruntime-android`)
- **LLM / Generative AI:** Google Gemini API (`gemini-2.5-flash`) via Retrofit 2 & Moshi
- **Algoritma Optimasi:** Vehicle Routing Problem (VRP) & Ensemble ML Classifiers (Logistic Regression, Random Forest, XGBoost)

### Pemetaan & Navigasi
- **Map Renderer:** osmdroid (`org.osmdroid:osmdroid-android`)
- **Routing Engine:** Open Source Routing Machine (OSRM) HTTP API
- **Geocoding:** OpenStreetMap Nominatim Service
- **Location Services:** Google Play Services Location

### Penyimpanan Data & Jaringan
- **Database Lokal:** Android Room SQLite Persistence dengan Kotlin KSP
- **HTTP Client:** Square OkHttp 3 & Retrofit 2
- **JSON Serialization:** Square Moshi Kotlin

---

## Panduan Instalasi & Menjalankan Aplikasi

### Prasyarat
1. **Android Studio** versi Ladybug / Koala atau yang lebih baru.
2. **JDK 17** terpasang dan dikonfigurasi sebagai Gradle JDK.
3. Perangkat fisik Android atau Emulator (Android 7.0 / API 24 ke atas).
4. Google Gemini API Key (bisa didapatkan melalui [Google AI Studio](https://aistudio.google.com/)).

### Langkah Instalasi

1. **Clone Repositori**
   ```bash
   git clone https://github.com/AL-Qudzzz/AIC-2.git
   cd AIC-2
   ```

2. **Konfigurasi Environment Variable**
   Salin berkas `.env.example` menjadi `.env` pada direktori root proyek:
   ```bash
   cp .env.example .env
   ```
   Buka berkas `.env` dan masukkan API Key Gemini Anda:
   ```env
   GEMINI_API_KEY=masukkan_api_key_gemini_anda_di_sini
   ```

3. **Buka Proyek di Android Studio**
   - Buka Android Studio.
   - Pilih menu **File** > **Open**, lalu pilih direktori proyek `routewise-ai`.
   - Tunggu proses Gradle Sync hingga selesai.

4. **Kompilasi dan Jalankan**
   - Pilih target perangkat fisik atau Android Virtual Device (AVD).
   - Klik tombol **Run 'app'** (`Shift + F10`) pada Android Studio.

---

## Konfigurasi Kunci & Izin Akses

Aplikasi memerlukan beberapa izin akses pada `AndroidManifest.xml` untuk fungsionalitas pemetaan dan pelacakan rute:

- `android.permission.INTERNET`: Akses jaringan untuk memuat peta, OSRM routing, dan Gemini API.
- `android.permission.ACCESS_NETWORK_STATE`: Pengecekan status konektivitas perangkat.
- `android.permission.ACCESS_FINE_LOCATION`: Penentuan posisi GPS kurir secara akurat di atas peta.
- `android.permission.ACCESS_COARSE_LOCATION`: Penentuan lokasi berbasis jaringan/BTS.

---

## Roadmap Pengembangan

### Fase 1: Minimum Viable Product (Status Saat Ini)
- Optimasi rute multi-stop berbasis ONNX Runtime.
- Estimasi biaya bahan bakar berdasarkan parameter profil kendaraan.
- Pemantauan status paket dan pelacakan visual pada peta OSM.
- Rekapitulasi laporan dan statistik harian pengiriman.
- Asisten interaktif Gemini AI Copilot untuk panduan operasional.

### Fase 2: Peningkatan Kapabilitas AI
- Integrasi Graph Neural Network (GNN) untuk prediksi propagasi kemacetan jalan.
- Prediksi estimasi waktu tiba (ETA) adaptif dengan integrasi data cuaca real-time.
- Sinkronisasi otomatis data armada berbasis Cloud Firestore.

### Fase 3: Skalabilitas Platform
- Dispatcher Web Console untuk manajemen multi-kurir secara tersentralisasi.
- Mode navigasi *offline-first* dengan cache peta regional mandiri.
- Integrasi barcode/QR scanner untuk verifikasi penerimaan paket kilat.

---

## Kontribusi & Lisensi

Proyek ini dikembangkan dalam rangka inovasi bidang *Smart Logistics* untuk menekan biaya distribusi nasional dan meningkatkan efisiensi kerja kurir di lapangan.

Seluruh kode sumber dilindungi di bawah lisensi terbuka proyek. Untuk informasi lisensi dan kerja sama lebih lanjut, silakan hubungi tim pengembang melalui repositori ini.
