# HP Monitor — Product Requirements Document

## 1. Ringkasan
HP Monitor adalah aplikasi dashboard Android (Jetpack Compose) yang memantau
kesehatan sebuah perangkat Android lain di jaringan lokal (LAN). Perangkat
target menjalankan **HP Monitor Agent** (server HTTP read-only berbasis Python
di `~/.hermes` sisi DevBox) yang mengekspos metrik `/proc` + `/sys` di
`/metrics`. Aplikasi ini berjalan di perangkat sekunder berdaya rendah —
unit produksi pertama: **Samsung Galaxy J1 Ace (SM-J111F, Android 5.1.1,
1 GB RAM, layar AMOLED)** — sehingga seluruh desain mengutamakan RAM, CPU,
dan piksel hitam OLED murni.

## 2. Keputusan PRD (hasil wawancara)
| Dimensi | Keputusan |
|---|---|
| Domain | A — Dual-Role (1 APK: Viewer Dashboard / Host Agent Server) |
| Native Layer | B — Rust (`monitor-core`) via JNI + fallback Kotlin penuh |
| Persistence | A — SharedPreferences (alamat server & compact mode) |
| UI Theme | Hitam pekat `#000000` (true OLED), skala netral R=G=B, Compact HUD mode |
| Repo | `arbdevai/hp-monitor`, **public** (agar in-app update dari GitHub Release bekerja tanpa autentikasi) |

## 3. Arsitektur
- **Presentation:** Jetpack Compose, Material 3, dark-only, skeleton shimmer
  (tanpa spinner layar penuh), microcopy lugas, zero emoji dekoratif.
- **State:** `DashboardViewModel` (AndroidViewModel) + `StateFlow`. Loop
  polling terikat `repeatOnLifecycle(STARTED)` — berhenti saat layar mati.
- **Native (`monitor-core`, Rust cdylib, opt-level z + LTO):** ring buffer
  360 sampel, downsampling sparkline [avg,min,max] per bucket, statistik
  jendela [count,last,avg,min,max]. Float melewati JNI sebagai byte array
  little-endian (ABI stabil). `System.loadLibrary` gagal → fallback
  `KotlinHistory` dengan semantik identik (aplikasi tidak pernah crash).
- **Data:** `MetricsRepository` (HttpURLConnection, timeout 3 s/4 s, Mutex
  serialisasi fetch, JSON parser toleran `ignoreUnknownKeys/isLenient/
  coerceInputValues`, deteksi respons HTML), `UpdateManager` (OkHttp
  streaming 64 KB → `.part`, verifikasi 4 lapis, pemilihan aset per-ABI via
  `Build.SUPPORTED_ABIS`, komparasi SemVer numerik, FileProvider install).
- **Agent (di HP target, bukan bagian APK):** `monitor-agent/agent.py` —
  HTTP 0.0.0.0:8787, read-only `/proc`+`/sys`: CPU overall/per-core +
  loadavg, RAM/swap, baterai (heuristik unit suhu kernel), thermal zone
  max, rate RX/TX wlan0, top-6 proses (ps), uptime, hostname.

## 4. Batas Teknis
- **minSdk 21** (Android 5.0) — wajib untuk J1 Ace 5.1.1. Set dependensi
  di-pin ke rilis yang aman minSdk-21 (Compose BOM 2024.12.01,
  activity 1.9.3, core-ktx 1.15.0, lifecycle 2.8.7).
- Cleartext HTTP LAN: `usesCleartextTraffic="true"` (aman: LAN lokal saja).
- ABI split `arm64-v8a` + `armeabi-v7a` (J1 Ace = Exynos 7570 aarch64;
  fallback 32-bit untuk device lain).
- **Dilarang build lokal** di chroot HP — semua kompilasi di GitHub Actions
  (`ubuntu-latest`, JDK 17, NDK 26.1, cargo-ndk).

## 5. Alur Rilis
`Release vX.Y.Z` (commit message prefix) → CI build signed release
(keystore dari 4 GitHub Secrets) → GitHub Release dengan 2 APK per-ABI →
in-app updater di aplikasi memilih aset sesuai ABI perangkat.
`versionCode = MAJOR*10000 + MINOR*100 + PATCH` (1.0.0 = 10000).

## 6. Kriteria Acceptance v1.0.0
1. APK terpasang dan berjalan di Android 5.1.1 (J1 Ace).
2. Dashboard menampilkan data live dari agent (CPU, RAM, swap, baterai,
   termal, jaringan, top proses) dengan refresh 3 detik.
3. Sparkline & statistik dihitung oleh engine Rust (badge "Engine: Rust"
   terlihat); bila .so tidak tersedia, fallback Kotlin aktif tanpa crash.
4. Offline: banner status tetap, data terakhir dipertahankan, chip OFFLINE.
5. Pengaturan: ubah alamat server (persisten via SharedPreferences).
6. In-app update: deteksi rilis terbaru di repo public, unduh per-ABI,
   verifikasi 4 lapis, install via FileProvider.
7. Polling berhenti total saat aplikasi non-STARTED (tidak bakar baterai).
