# HP Monitor — Project State & Evolution Log

## Current Status (v1.3.0)
- **Active Version:** `1.3.0` (`versionCode = 10300`)
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
2. **MediaTek Android 14 (ARM64) CPU Reading Multi-Strategy:**
   - Mengatasi blokade SELinux `/proc/stat` pada Android 8-14 dengan membaca
     frekuensi cpufreq langsung dari sysfs (`scaling_cur_freq`, `policy*`, MediaTek DVFSRC).
   - Dynamic process elapsed CPU delta fallback: memastikan metrik CPU % dan per-core
     tidak pernah kosong (`--`) di HP modern.
3. **Multi-Device Switcher Cepat:**
   - Dropdown chip switcher langsung di status bar / header telemetri.
   - Menyimpan daftar host target (PC, HP 1, HP 2, dsb) di SharedPreferences,
     dapat berganti target instan dalam 1 ketukan tanpa membuka Pengaturan.
4. **Real-time Network Ping & Latency (ms):**
   - Mengukur round-trip time (RTT) aktual ke server target setiap siklus polling.
   - Indikator visual real-time dengan kode warna hijau (<50ms), kuning (<120ms), merah (>=120ms).
5. **Remote Music & Volume Control (/api/control):**
   - Mengontrol volume host target (Volume Up, Volume Down, Toggle Mute).
   - Mengontrol media player host (Play/Pause, Next Track, Previous Track) via key event injection.
   - Terintegrasi langsung di `EmbeddedAgentServer` dan UI Remote Control popup.
6. **Pure OLED Black (#000000) Theme & Surface Tint Elimination:**
   - Menghilangkan degradasi warna merah/kecokelatan akibat Material 3 `surfaceTint`.
   - Palet warna OLED pekat (`MonitorBlack = #000000`, `MonitorCard = #080808`, border `#222222`).
   - Aksen neon cyber cyan (`#00D1FF`) dan green (`#00E676`) berdaya visual tinggi di AMOLED.
7. **Desk HUD Mode (Zero-Scroll Mode):**
   - Mode telemetri khusus pajangan meja ("hp nganggur") dengan grid 3-kolom landscape.
   - 100% informasi CPU, RAM/Swap, Net, Baterai, dan Top Procs muat tanpa scroll vertikal.
   - Jam digital monospace real-time di header telemetri.
8. **Collapsible TopAppBar & Fullscreen View:**
   - Tombol toggle untuk menyembunyikan TopAppBar tebal 56dp menjadi status bar telemetri 28dp.
9. **Keep Screen On Flag:**
   - Opsi `keepScreenOn` agar layar HP tidak mati otomatis saat diletakkan di meja sebagai monitor.
10. **Dual-Role 2-in-1 (Embedded Agent Server & Host Mode):**
   - APK dapat bertindak sebagai Viewer (Dashboard) ATAU Host (Yang Dimonitor) tanpa Termux/Python.
11. **SSL/TLS Root CA Trust Workaround (Android 5.1.1):**
   - Workaround trust store kadaluarsa Android 5.0-5.1.1 untuk unduhan GitHub Release.

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
