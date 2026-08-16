# Product Requirements Document (PRD)
# Project Name: RouteWise AI — AI Fleet Copilot untuk Last Mile Delivery Optimization

**Kategori Kompetisi:** Smart Logistics — AIC
**Versi Dokumen:** 1.0
**Status:** Draft untuk Proposal Kompetisi
**Tipe Dokumen:** Product Requirements Document
**Nama Produk (Alternatif):** CourierMind AI, FleetPilot AI

---

## 1. Ringkasan Eksekutif

RouteWise AI adalah sebuah **AI Fleet Intelligence Platform** yang dirancang untuk mengoptimalkan proses pengiriman *last-mile delivery* pada perusahaan logistik. Berbeda dari aplikasi navigasi konvensional seperti Google Maps atau Waze, RouteWise AI tidak hanya mencari rute tercepat, tetapi juga mempertimbangkan variabel operasional bisnis secara menyeluruh — biaya bahan bakar, kapasitas kendaraan, prioritas paket, jendela waktu pelayanan pelanggan, serta probabilitas kegagalan pengiriman.

Produk ini diposisikan sebagai **AI Fleet Copilot**: asisten cerdas yang membantu kurir di lapangan dan manajer armada di kantor mengambil keputusan berbasis prediksi AI, bukan sekadar alat navigasi tambahan.

---

## 2. Latar Belakang & Problem Statement

### 2.1 Masalah yang Dihadapi Kurir dan Perusahaan Logistik
- Rute pengiriman yang ditentukan secara manual atau statis sering kali tidak optimal, menyebabkan pemborosan waktu dan bahan bakar.
- Kurir sering terjebak kemacetan yang sebenarnya bisa diprediksi dan dihindari sebelumnya.
- Paket gagal dikirim (pelanggan tidak di tempat, alamat sulit ditemukan) menyebabkan pengiriman ulang yang membebani biaya operasional.
- Biaya BBM sulit diprediksi dan dikendalikan karena bergantung pada banyak variabel (jenis kendaraan, jarak, kondisi jalan, perilaku mengemudi).
- Manajer armada tidak memiliki visibilitas real-time dan analitik yang memadai untuk mengevaluasi efisiensi operasional per kendaraan/kurir.

### 2.2 Konteks Nasional
Pemerintah menilai integrasi *smart logistics* sebagai kebutuhan mendesak untuk menekan biaya logistik nasional yang masih tergolong tinggi dibandingkan negara-negara lain di kawasan. Hal ini membuka peluang bagi solusi berbasis AI yang dapat memberikan dampak langsung terhadap efisiensi rantai distribusi domestik.

### 2.3 Kesenjangan pada Solusi Eksisting
Aplikasi navigasi umum (Google Maps, Waze) hanya berfokus pada estimasi rute dan waktu tempuh, tanpa mempertimbangkan konteks bisnis logistik seperti biaya operasional per pengiriman, prioritas multi-titik, maupun prediksi kegagalan pengiriman. Inilah celah yang diisi oleh RouteWise AI.

---

## 3. Tujuan Produk (Goals)

### 3.1 Tujuan Utama
- Mengoptimalkan urutan dan rute pengiriman multi-titik secara otomatis menggunakan AI.
- Memprediksi kondisi lalu lintas secara proaktif untuk menghindari kemacetan sebelum terjadi.
- Mengestimasi biaya BBM secara akurat berdasarkan profil kendaraan dan rute aktual.
- Menurunkan tingkat kegagalan pengiriman melalui prediksi perilaku pelanggan.
- Menyediakan dashboard analitik armada untuk pengambilan keputusan manajerial.

### 3.2 Non-Goals (Di Luar Cakupan)
- RouteWise AI tidak menggantikan sistem manajemen gudang (*warehouse management system*).
- Versi awal (MVP) tidak mencakup integrasi pembayaran atau sistem akuntansi perusahaan.
- Tidak mencakup pengembangan perangkat keras (GPS tracker khusus) — mengasumsikan penggunaan perangkat mobile kurir yang sudah ada.

---

## 4. Target Pengguna & Persona

| Persona | Peran | Kebutuhan Utama |
|---|---|---|
| Kurir Lapangan | Menjalankan pengiriman harian | Rute jelas, estimasi waktu akurat, notifikasi macet, panduan alamat sulit |
| Manajer Armada / Operasional | Mengawasi kinerja armada | Dashboard real-time, laporan biaya, analitik efisiensi |
| Perencana Logistik (Dispatcher) | Menyusun jadwal & alokasi pengiriman | Optimasi otomatis, prediksi kegagalan, alokasi kendaraan |

---

## 5. Positioning & Value Proposition

> **"AI Fleet Copilot — asisten cerdas untuk perusahaan logistik yang membantu kurir dan manajer armada mengambil keputusan terbaik berdasarkan prediksi AI."**

Produk ini secara sengaja tidak diposisikan sebagai "Google Maps untuk kurir", karena positioning tersebut akan langsung dibandingkan head-to-head dengan pemain besar seperti Google Maps atau Waze. Sebaliknya, RouteWise AI menonjolkan AI sebagai inti pengambilan keputusan operasional, bukan sekadar fitur tambahan di atas peta.

---

## 6. Ruang Lingkup Fitur (Functional Requirements)

### FR-1: Smart Route Planning (Optimasi Rute Multi-Titik)
- **Deskripsi:** Sistem menghitung urutan pengiriman optimal berdasarkan beberapa kendala sekaligus.
- **Input:** daftar alamat pelanggan, prioritas paket, jam operasional pelanggan, jenis & kapasitas kendaraan, jumlah paket.
- **Output:** urutan delivery optimal, estimasi waktu tempuh total, estimasi biaya, total jarak.
- **Pendekatan teknis:** Vehicle Routing Problem (VRP) berbasis Reinforcement Learning, dikombinasikan dengan solver optimasi kombinatorial (mis. OR-Tools) sebagai baseline/pembanding.
- **User Story:** Sebagai dispatcher, saya ingin sistem menyusun urutan pengiriman terbaik secara otomatis agar kurir tidak perlu menyusunnya secara manual.

### FR-2: AI Traffic Prediction (Prediksi Kemacetan Proaktif)
- **Deskripsi:** Sistem tidak hanya membaca kondisi lalu lintas saat ini, tetapi memprediksi kondisi 15–30 menit ke depan pada ruas jalan yang akan dilalui, lalu menyesuaikan rute sebelum kemacetan terjadi.
- **Pendekatan teknis:** Graph Neural Network (GNN) untuk memodelkan jaringan jalan sebagai graf (node = persimpangan, edge = ruas jalan) dan mempelajari pola propagasi kemacetan antar-ruas dari data historis.
- **Sumber data:** data lalu lintas historis dan real-time (mis. API peta pihak ketiga, OpenStreetMap).
- **User Story:** Sebagai kurir, saya ingin diberi peringatan dan rute alternatif sebelum saya terjebak macet, bukan setelahnya.

### FR-3: Fuel Cost Prediction (Estimasi Biaya BBM)
- **Deskripsi:** Sistem menghitung estimasi biaya bahan bakar per pengiriman berdasarkan profil kendaraan.
- **Input:** jenis kendaraan, tahun kendaraan, jenis BBM, konsumsi BBM (km/liter), jarak tempuh.
- **Proses:** Total jarak → estimasi liter BBM terpakai → biaya BBM → biaya per pengiriman (*cost per delivery*).
- **Contoh perhitungan:** kendaraan dengan konsumsi 13 km/liter menempuh jarak 74 km menggunakan BBM seharga Rp10.000/liter menghasilkan estimasi biaya sekitar Rp56.900.
- **User Story:** Sebagai manajer armada, saya ingin mengetahui estimasi biaya BBM setiap rute sebelum pengiriman dijalankan agar dapat mengontrol anggaran operasional.

### FR-4: Delivery Progress Tracking
- **Deskripsi:** Peta visual real-time dengan kode warna status pengiriman:
  - 🟢 Delivered (terkirim)
  - 🟡 On Delivery (dalam perjalanan)
  - 🔴 Failed Delivery (gagal terkirim)
- **User Story:** Sebagai manajer operasional, saya ingin memantau status seluruh pengiriman hari ini dalam satu tampilan peta.

### FR-5: AI ETA Prediction
- **Deskripsi:** Prediksi estimasi waktu tiba yang lebih akurat dibanding aplikasi peta standar, dengan mempertimbangkan faktor tambahan: kondisi cuaca (hujan), jam sibuk, waktu bongkar-muat, dan rata-rata waktu penerimaan paket oleh pelanggan di lokasi tersebut.
- **User Story:** Sebagai pelanggan/dispatcher, saya ingin estimasi waktu tiba yang realistis, bukan hanya berdasarkan jarak dan kecepatan rata-rata.

### FR-6: Delivery Report & Dashboard
- **Deskripsi:** Ringkasan performa pengiriman harian dalam bentuk dashboard, contoh: total paket dikirim, jumlah berhasil, jumlah gagal.
- **User Story:** Sebagai manajer, saya ingin melihat ringkasan performa pengiriman harian tanpa perlu merekap manual.

### FR-7: AI Failed Delivery Prediction
- **Deskripsi:** Sistem memprediksi kemungkinan kegagalan pengiriman untuk pelanggan tertentu (kemungkinan tidak di rumah, alamat sulit ditemukan, riwayat sering gagal menerima paket), lalu memberikan rekomendasi tindakan, misalnya menyarankan waktu pengiriman ulang yang lebih optimal.
- **User Story:** Sebagai dispatcher, saya ingin sistem memperingatkan saya jika suatu pengiriman berisiko gagal, beserta saran waktu pengiriman terbaik.

### FR-8: AI Fleet Analytics
- **Deskripsi:** Analitik per kendaraan/kurir mencakup biaya BBM, efisiensi rute, rata-rata waktu pengiriman, dan idle time.
- **User Story:** Sebagai manajer armada, saya ingin membandingkan efisiensi antar kendaraan/kurir untuk mengidentifikasi peluang perbaikan operasional.

### FR-9: AI Fleet Copilot (Asisten Percakapan Berbasis LLM)
- **Deskripsi:** Antarmuka percakapan berbasis LLM yang memungkinkan kurir maupun manajer bertanya secara natural language, misalnya *"Rute mana yang paling hemat BBM hari ini?"* atau *"Kenapa pengiriman ke pelanggan A sering gagal?"*, dan mendapat jawaban berbasis data operasional real-time.
- **User Story:** Sebagai manajer, saya ingin bertanya langsung kepada sistem tentang kondisi operasional tanpa perlu menganalisis data secara manual.

---

## 7. Arsitektur Sistem (High-Level)

```text
Customer Orders
       │
       ▼
AI Route Optimizer (RL + VRP Solver)
       │
       ├──► Traffic Prediction (Graph Neural Network)
       │
       ├──► Fuel Cost Prediction
       │
       ├──► ETA Prediction
       │
       ├──► Failed Delivery Prediction
       │
       ▼
Navigation App (Kurir)
       │
       ▼
Delivery Execution & Status Update
       │
       ▼
Delivery Report ──► Fleet Analytics Dashboard ──► AI Fleet Copilot (LLM)
```

### 7.1 Komponen Teknologi Inti

| Komponen | Teknologi | Fungsi |
|---|---|---|
| Route Optimization | Reinforcement Learning + OR-Tools (VRP solver) | Menentukan urutan pengiriman optimal |
| Traffic Prediction | Graph Neural Network (GNN) | Memodelkan dan memprediksi propagasi kemacetan pada graf jalan |
| ETA & Failure Prediction | Model prediktif (regresi/klasifikasi berbasis fitur historis) | Memprediksi waktu tiba dan risiko gagal kirim |
| Conversational Layer | Large Language Model (LLM) | Antarmuka tanya-jawab berbasis data operasional |
| Peta & Navigasi Dasar | API peta pihak ketiga (mis. OpenStreetMap/OSRM) | Data jalan, jarak, dan navigasi turn-by-turn |

---

## 8. Kebutuhan Data (Data Requirements)

| Kebutuhan Data | Kegunaan | Sumber yang Direkomendasikan |
|---|---|---|
| Data pengiriman last-mile skala besar (lokasi paket & kurir, waktu pickup/delivery) | Melatih model ETA & optimasi rute | Dataset publik last-mile delivery skala besar |
| Data deviasi rute (rencana vs aktual) | Mempelajari pola penyimpangan kurir dari rute optimal | Dataset publik rute terencana vs rute aktual |
| Matriks jarak & waktu tempuh pada berbagai skenario lalu lintas | Model prediksi ETA & traffic-aware routing | Dataset publik distribusi dengan matriks lalu lintas |
| Data operasional & risiko (konsumsi BBM, level lalu lintas, keterlambatan, kelelahan pengemudi) | Dashboard operasional & prediksi keterlambatan | Dataset publik operasi logistik |
| Data Vehicle Routing Problem (kapasitas, demand, jarak) | Benchmark algoritma optimasi rute | Dataset publik VRP |
| Konsumsi BBM kendaraan | Perhitungan estimasi biaya BBM | Spesifikasi resmi pabrikan kendaraan |
| Harga BBM | Perhitungan biaya BBM real-time | Data harga resmi bahan bakar |
| Jaringan jalan & jarak antar titik | Routing dasar | OpenStreetMap / OSRM |
| Data lalu lintas real-time & historis | Traffic prediction (GNN) | API penyedia peta pihak ketiga |

> **Catatan:** nama dan tautan dataset spesifik dapat dilampirkan pada bagian referensi proposal terpisah sesuai kebutuhan sitasi akademik/kompetisi.

---

## 9. Kebutuhan Non-Fungsional

| Kategori | Kebutuhan |
|---|---|
| Performa | Rekalkulasi rute akibat perubahan lalu lintas harus selesai dalam hitungan detik agar tetap relevan bagi kurir di lapangan |
| Skalabilitas | Sistem harus mampu menangani ratusan hingga ribuan titik pengiriman per hari untuk satu armada |
| Ketersediaan | Navigasi dan status pengiriman harus tetap berfungsi meski koneksi kurir tidak stabil (mode offline-first untuk data rute yang sudah diunduh) |
| Akurasi Model | Model ETA dan prediksi kegagalan pengiriman perlu dievaluasi berkala dengan metrik yang jelas (lihat Bagian 10) dan diperbarui seiring data baru |
| Keamanan Data | Data lokasi pelanggan dan kurir bersifat sensitif dan harus dienkripsi serta dibatasi aksesnya sesuai peran pengguna |
| Usability | Antarmuka kurir harus sederhana dan minim distraksi karena digunakan sambil mengemudi |

---

## 10. Metrik Keberhasilan (Success Metrics / KPI)

| Metrik | Definisi | Target Indikatif |
|---|---|---|
| Efisiensi Rute | Pengurangan total jarak/waktu tempuh dibanding rute manual/baseline | Penurunan signifikan dibanding baseline non-AI |
| Akurasi ETA | Selisih rata-rata antara ETA prediksi dan waktu tiba aktual | Selisih sekecil mungkin (mendekati real-time) |
| Tingkat Keberhasilan Pengiriman | Persentase paket terkirim sukses pada percobaan pertama | Meningkat dibanding kondisi sebelum implementasi |
| Penghematan Biaya BBM | Selisih estimasi biaya BBM sebelum dan sesudah optimasi rute | Penurunan biaya operasional per pengiriman |
| Akurasi Prediksi Kegagalan Pengiriman | Precision/recall model failed-delivery prediction | Nilai precision/recall yang layak untuk aksi operasional |
| Adopsi Pengguna | Jumlah kurir/manajer aktif menggunakan sistem harian | Tingkat penggunaan konsisten oleh armada uji coba |

---

## 11. Cakupan MVP & Roadmap

### Fase 1 — MVP (Fokus Kompetisi/Prototipe)
- [ ] FR-1 (Smart Route Planning) — versi dasar dengan solver VRP
- [ ] FR-3 (Fuel Cost Prediction) — kalkulasi berbasis input manual kendaraan
- [ ] FR-4 (Delivery Progress Tracking) — peta status sederhana
- [ ] FR-6 (Delivery Report) — ringkasan harian dasar

### Fase 2 — Pengembangan Lanjutan
- [ ] FR-2 (AI Traffic Prediction dengan GNN)
- [ ] FR-5 (AI ETA Prediction dengan faktor cuaca & histori)
- [ ] FR-7 (AI Failed Delivery Prediction)

### Fase 3 — Platform Matang
- [ ] FR-8 (AI Fleet Analytics penuh)
- [ ] FR-9 (AI Fleet Copilot berbasis LLM)
- [ ] Integrasi API pihak ketiga skala penuh & mode offline-first

---

## 12. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Ketersediaan data lalu lintas real-time terbatas/berbayar | Menghambat akurasi traffic prediction | Gunakan dataset historis publik untuk prototipe, integrasi API berbayar pada tahap produksi |
| Model prediksi kegagalan pengiriman bias terhadap data historis tertentu | Rekomendasi kurang akurat di wilayah baru | Validasi model pada beberapa segmen wilayah, evaluasi berkala |
| Ketergantungan pada API peta pihak ketiga | Biaya operasional meningkat seiring skala | Evaluasi kombinasi OpenStreetMap/OSRM (open-source) dengan API berbayar untuk fitur premium |
| Adopsi pengguna (kurir) rendah karena perubahan kebiasaan kerja | Manfaat AI tidak tercapai di lapangan | Desain antarmuka sederhana, pelatihan onboarding, umpan balik iteratif dari kurir |

---

## 13. Asumsi & Ketergantungan
- Kurir memiliki perangkat mobile dengan konektivitas internet yang memadai untuk sebagian besar waktu operasional.
- Perusahaan logistik mitra bersedia membagikan data historis pengiriman untuk pelatihan model (atau digunakan dataset publik sebagai pengganti pada tahap prototipe).
- Tersedia akses ke API peta/lalu lintas pihak ketiga, minimal pada tingkat free-tier untuk keperluan prototipe.

---

## 14. Lampiran

### 14.1 Ringkasan Positioning

| Aspek | Deskripsi |
|---|---|
| Kategori | Smart Logistics |
| Diferensiasi Utama | Optimasi berbasis biaya operasional riil (BBM, kapasitas, prioritas), bukan sekadar rute tercepat |
| Teknologi Inti | Reinforcement Learning, Graph Neural Network, Route Optimization, LLM |
| Positioning | AI Fleet Copilot, bukan aplikasi navigasi biasa |

---

*Dokumen ini merupakan draf PRD awal yang dapat disesuaikan lebih lanjut sesuai format dan ketentuan pengumpulan proposal kompetisi AIC.*
