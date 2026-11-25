# HP Monitor — Project State & Evolution Log

## Current Status (v1.0.0)
- **Active Version:** `1.0.0` (`versionCode = 10000`)
- **Git Branch:** `main`
- **Repo:** `arbdevai/hp-monitor` (public — dibutuhkan in-app update)
- **minSdk:** 21 (J1 Ace = Android 5.1.1)
- **NDK Architectures:** `arm64-v8a`, `armeabi-v7a`
- **Target Android SDK:** 36 (Min SDK: 21)

## Logika & Modul yang Telah Beroperasi Penuh
1. **Engine Rust `monitor-core` (JNI, opt-level z + LTO):**
   - Ring buffer 360 sampel global (Mutex-guarded), sparkline [avg,min,max]
     per bucket, statistik jendela [count,last,avg,min,max].
   - Float crossing JNI sebagai byte array little-endian — tidak ada
     konstruksi typed-array yang rentan. `panic = "unwind"` agar panic
     tidak pernah abort proses Android.
   - Fallback `KotlinHistory` semantik identik bila `System.loadLibrary`
     gagal (bad ABI). Badge di dashboard menampilkan engine aktif.
2. **Agent metrik di HP target (di luar APK):**
   - `monitor-agent/agent.py` di sisi DevBox HP utama, `0.0.0.0:8787`,
     read-only. Sudah terverifikasi live: CPU per-core, loadavg, RAM/swap,
     baterai (heuristik unit suhu: raw<150 → °C, selain itu 0.1°C),
     thermal zone max, rate RX/TX wlan0, top-6 proses (filter artefak `ps`).
3. **UI Dashboard Compose (dark OLED #000000):**
   - Kartu CPU (overall + load + bar per-core + sparkline), Memori (RAM +
     swap + sparkline), Baterai & Termal (suhu chip + tegangan), Jaringan
     (rate down/up + sparkline), Proses Teratas.
   - Skeleton shimmer saat first-load (bukan spinner). Banner OFFLINE solid
     saat server tak terjangkau, data terakhir dipertahankan.
4. **In-app updater (OkHttp streaming, verifikasi 4 lapis, ABI split,
   SemVer numerik, FileProvider).**
5. **Polling lifecycle-bound** (`repeatOnLifecycle(STARTED)`): tidak ada
   polling saat layar mati/app background.

## Known CI Failure Patterns
(Belum ada — diisi saat pola kegagalan pertama muncul.)

## Catatan Keputusan
- Native dipilih manual JNI (bukan UniFFI): surface area lebih kecil,
  mengikuti pola `NativeCoreBridge` sboard yang terbukti di CI ini.
- HTTP polling memakai HttpURLConnection (bukan OkHttp) — polling 3 detik
  tidak butuh dependency ekstra; OkHttp hanya untuk updater.
- Warna: skala netral murni (R=G=B), aksen `#4C8DFF`. Tanpa kartu tinted
  transparan, tanpa emoji dekoratif.
