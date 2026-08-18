# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Android Kotlin (Jetpack Compose, Material Design 3, OsmDroid / OpenStreetMap, Kotlin Coroutines, StateFlow)

## Users

Kurir Lapangan (Motor & Mobil) yang sedang bertugas mengantarkan puluhan paket dalam jadwal harian yang padat di jalanan kota. Mereka membutuhkan antarmuka yang sangat *glanceable*, minim teks distraksi, responsif saat berkendara, dengan visual map yang intuitif dan tombol aksi cepat (*quick delivery action*).

## Product Purpose

RouteWise AI adalah platform AI Fleet Intelligence & Copilot untuk optimasi pengiriman *last-mile delivery*. Mengurangi beban kognitif kurir dan meminimalkan biaya operasional melalui optimasi rute Vehicle Routing Problem (VRP), prediksi kemacetan lalu lintas berbasis AI, estimasi konsumsi bahan bakar, dan pencegahan kegagalan pengiriman.

## Positioning

AI Fleet Copilot — bukan sekadar peta navigasi biasa seperti Google Maps/Waze, melainkan asisten cerdas yang memberikan keputusan operasional langsung: urutan stop optimal, mitigasi macet proaktif, dan kontrol efisiensi pengiriman harian.

## Operating Context

Kurir mengoperasikan perangkat smartphone di atas dashboard motor/mobil atau dengan satu tangan saat berhenti. Pencahayaan bervariasi (terik siang hari hingga malam hari), membutuhkan kontras tinggi, elemen sentuh minimal 48dp, tampilan peta full-bleed imersif dengan HUD mengambang (floating pills/cards) yang ringkas, serta status pengiriman berbasis kode visual/ikonik yang instan dikenali.

## Capabilities and Constraints

- Optimasi rute multi-stop VRP on-device / backend.
- Prediksi lalu lintas real-time & rute alternatif.
- Manajemen paket: status pengiriman (Delivered, In Transit, Failed, Pending), bukti pengiriman, update cepat.
- Pemantauan efisiensi bahan bakar dan ringkasan performa harian kurir.
- Interaksi peta berbasis OpenStreetMap / OsmDroid dengan overlay polyline, custom waypoint pins, dan live location marker.
- Desain minimalis: menghilangkan teks berulang/clutter, memprioritaskan indikator visual, typography hierarchy tegas, micro-interactions halus.

## Brand Commitments

- Nama: RouteWise AI
- Karakter Visual: Minimalist Modern Logistics, Clean, High-Contrast Glanceability, Premium Dark/Light Surface Tones, Fast & Fluid.
- Color Tone: Deep Navy/Charcoal Slate, Electric Cyan / Precision Emerald accents, Traffic Amber/Red warnings.

## Product Principles

1. **Glanceable Over Verbose:** Informasi utama (next stop, ETA, status) harus bisa dipahami dalam waktu < 1 detik tanpa membaca paragraf teks.
2. **Action-First Simplicity:** Tombol aksi pengiriman (Selesai Antar, Hubungi Pelanggan, Gagal) mudah dijangkau dan berukuran minimal 48dp.
3. **Immersive Map-Centric Experience:** Peta adalah kanvas utama yang dinamis, bersih dari panel kaku, menggunakan floating HUD dan expandable bottom sheet elegan.
4. **Resilience & Clarity:** Status paket dan instruksi rute selalu jelas dalam segala kondisi pencahayaan dan konektivitas.

## Accessibility & Inclusion

- Material 3 accessibility standards, minimum 48x48 dp touch target.
- High contrast ratio (>4.5:1 untuk teks biasa, >3:1 untuk display/komponen besar).
- Desain ramah getaran dan responsif terhadap skala font sistem.
