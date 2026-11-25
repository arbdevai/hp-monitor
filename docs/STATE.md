# HP Monitor — Project State & Evolution Log

## Current Status (v1.2.0)
- **Active Version:** `1.2.0` (`versionCode = 10200`)
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
2. **Pure OLED Black (#000000) Theme & Surface Tint Elimination:**
   - Menghilangkan degradasi warna merah/kecokelatan akibat Material 3 `surfaceTint`.
   - Palet warna OLED pekat (`MonitorBlack = #000000`, `MonitorCard = #080808`, border `#222222`).
   - Aksen neon cyber cyan (`#00D1FF`) dan green (`#00E676`) berdaya visual tinggi di AMOLED.
3. **Desk HUD Mode (Zero-Scroll Mode):**
   - Mode telemetri khusus pajangan meja ("hp nganggur") dengan grid 3-kolom landscape.
   - 100% informasi CPU, RAM/Swap, Net, Baterai, dan Top Procs muat tanpa scroll vertikal.
   - Jam digital monospace real-time di header telemetri.
4. **Collapsible TopAppBar & Fullscreen View:**
   - Tombol toggle untuk menyembunyikan TopAppBar tebal 56dp menjadi status bar telemetri 28dp,
     menghemat ruang vertikal secara drastis pada resolusi kecil (800x480).
5. **Keep Screen On Flag:**
   - Opsi `keepScreenOn` agar layar HP tidak mati otomatis saat diletakkan di meja sebagai monitor.
6. **Dual-Role 2-in-1 (Embedded Agent Server & Host Mode):**
   - APK dapat bertindak sebagai Viewer (Dashboard) ATAU Host (Yang Dimonitor).
   - `HostAgentService` berjalan sebagai Foreground Service dengan `EmbeddedAgentServer`
     di port 8787, membaca `/proc/stat`, `/proc/meminfo`, `BatteryManager`, `TrafficStats`.
7. **SSL/TLS Root CA Trust Workaround (Android 5.1.1):**
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
