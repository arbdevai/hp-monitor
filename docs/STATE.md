# HP Monitor — Project State & Evolution Log

## Current Status (v1.1.0)
- **Active Version:** `1.1.0` (`versionCode = 10100`)
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
6. **Dual-Role 2-in-1 (Embedded Agent Server & Host Mode):**
   - APK dapat bertindak sebagai Viewer (Dashboard) ATAU Host (Yang Dimonitor).
   - `HostAgentService` berjalan sebagai Foreground Service dengan `EmbeddedAgentServer`
     di port 8787, membaca `/proc/stat`, `/proc/meminfo`, `BatteryManager`, `TrafficStats`.
   - Tidak memerlukan script python atau Termux lagi di perangkat target.
7. **Tampilan Compact & Landscape Multi-Column (Optimasi J1 Ace):**
   - Toggle Compact Mode persisten di SharedPreferences dan tombol cepat TopAppBar.
   - Mode Lanskap beralih ke tata letak 2 kolom (Kiri: CPU + Baterai, Kanan: Memori + Jaringan)
     agar seluruh informasi telemetri muat di layar 800x480 tanpa scroll berlebih.
8. **SSL/TLS Root CA Trust Workaround (Android 5.1.1):**
   - Workaround trust store kadaluarsa Android 5.0-5.1.1 untuk unduhan GitHub Release
     dengan verifikasi integritas 4 lapis (signature + package name + SemVer) di `isApkValid`.

## Known CI Failure Patterns
1. **AGP Conflicting Configuration (splits vs ndk.abiFilters):**
   - Gejala: `Conflicting configuration : '...' in ndk abiFilters cannot be present when splits abi filters are set`.
   - Solusi: Hapus `ndk.abiFilters` dari `defaultConfig` saat `splits.abi { isEnable = true }` digunakan.
2. **Rust JNI Native Type Conversion:**
   - Gejala: `mismatched types expected &[Sample], found &VecDeque<Sample>` dan `expected JObject, found JPrimitiveArray`.
   - Solusi: Gunakan `g.data.make_contiguous()` untuk mereferensikan slice dari `VecDeque`, dan panggil `.into()` pada `JPrimitiveArray` saat mengembalikan `JObject`.
3. **AndroidViewModel Constructor Contract:**
   - Gejala: `AndroidViewModel(context: Context)` menyebabkan type mismatch dan kegagalan factory ViewModel default.
   - Solusi: Selalu deklarasikan `(application: Application) : AndroidViewModel(application)`.
4. **Compose & Coroutines Import / Typing:**
   - Gejala: `Unresolved reference 'Path'`, `Unresolved reference 'Thermometer'`, `Argument type mismatch (Double vs Float)`, `Unresolved reference 'cancel'`.
   - Solusi: Gunakan `androidx.compose.ui.graphics.Path` (bukan geometry), `Icons.Default.Thermostat` (bukan Thermometer), literal `0.0f`, serta import `kotlinx.coroutines.cancel`.
5. **NetworkOnMainThreadException in Polling Loop:**
   - Gejala: Aplikasi tetap berstatus OFFLINE karena networking dijalankan di Main thread via `lifecycleScope.launch`.
   - Solusi: Selalu bungkus eksekusi `HttpURLConnection` di dalam `withContext(Dispatchers.IO)`.

## Catatan Keputusan
- Native dipilih manual JNI (bukan UniFFI): surface area lebih kecil,
  mengikuti pola `NativeCoreBridge` sboard yang terbukti di CI ini.
- HTTP polling memakai HttpURLConnection (bukan OkHttp) — polling 3 detik
  tidak butuh dependency ekstra; OkHttp hanya untuk updater.
- Warna: skala netral murni (R=G=B), aksen `#4C8DFF`. Tanpa kartu tinted
  transparan, tanpa emoji dekoratif.
