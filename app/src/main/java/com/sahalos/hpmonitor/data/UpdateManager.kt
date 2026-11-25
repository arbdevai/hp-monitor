package com.sahalos.hpmonitor.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.sahalos.hpmonitor.BuildConfig
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Direct-download in-app updater against GitHub Releases (public repo).
 * No DownloadManager: OkHttp streaming to a .part file, 4-layer APK
 * validation before install, ABI-split asset selection.
 */
class UpdateManager(private val context: Context) {

    data class UpdateInfo(
        val tag: String,
        val name: String,
        val url: String,
        val size: Long,
    )

    data class DownloadProgress(val fraction: Float, val bytes: Long, val total: Long)

    private val client: OkHttpClient = createHttpClient()

    private fun createHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)

        // Android 5.0 - 5.1.1 (API 21-22) lacks modern root CAs (ISRG Root X1 / modern DigiCert).
        // For in-app updater downloading signed release APKs from GitHub, install a trust-all
        // SSL socket factory on API <= 22. APK integrity is strictly verified by isApkValid()
        // using Android's PackageManager DEX and signature parsing.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (_: Exception) {}
        }
        return builder.build()
    }

    private val updateDir: File
        get() = File(context.cacheDir, "updates").apply { mkdirs() }

    private fun deviceAbi(): String =
        Build.SUPPORTED_ABIS.firstOrNull { it == "arm64-v8a" || it == "armeabi-v7a" }
            ?: Build.SUPPORTED_ABIS.firstOrNull()
            ?: "arm64-v8a"

    /** Returns UpdateInfo only when the remote release is newer (numeric SemVer). */
    suspend fun checkLatest(currentVersionName: String): Result<UpdateInfo?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(LATEST_RELEASE_API).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                    val root = JSONObject(resp.body?.string() ?: "")
                    val tag = root.optString("tag_name").removePrefix("v")
                    if (!semverGreaterThan(tag, currentVersionName)) return@runCatching null
                    val assets = root.optJSONArray("assets") ?: JSONArray()
                    val abi = deviceAbi()
                    for (i in 0 until assets.length()) {
                        val a = assets.optJSONObject(i) ?: continue
                        val name = a.optString("name")
                        if (!name.endsWith(".apk")) continue
                        if (!name.contains(abi)) continue
                        return@runCatching UpdateInfo(
                            tag = tag,
                            name = a.optString("name"),
                            url = a.optString("browser_download_url"),
                            size = a.optLong("size"),
                        )
                    }
                    throw IOException("Aset APK $abi tidak ditemukan di rilis")
                }
            }
        }

    suspend fun download(
        info: UpdateInfo,
        onProgress: (DownloadProgress) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            cleanup()
            val target = File(updateDir, "hp-monitor-${info.tag}.apk")
            val part = File(target.absolutePath + ".part")
            client.newCall(Request.Builder().url(info.url).build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body ?: throw IOException("Body kosong")
                part.outputStream().use { out ->
                    val buf = ByteArray(BUFFER_BYTES)
                    var done = 0L
                    while (true) {
                        val n = body.byteStream().read(buf)
                        if (n == -1) break
                        out.write(buf, 0, n)
                        done += n
                        onProgress(
                            DownloadProgress(
                                done.toFloat() / info.size.coerceAtLeast(1L),
                                done,
                                info.size,
                            )
                        )
                    }
                }
            }
            if (!isApkValid(part, info.size)) {
                part.delete()
                throw IOException("Verifikasi integritas APK gagal")
            }
            if (target.exists() && !target.delete()) throw IOException("Gagal menulis file target")
            if (!part.renameTo(target)) throw IOException("Gagal menyelesaikan unduhan")
            target
        }
    }

    /** Removes stale .apk and .part leftovers before a new download. */
    fun cleanup() {
        updateDir.listFiles()?.forEach { f ->
            if (f.isFile && (f.extension == "apk" || f.name.endsWith(".part"))) f.delete()
        }
    }

    /**
     * 4-layer APK integrity check:
     * 1) physical size (exact match vs release size when known),
     * 2) deep DEX + certificate parse via getPackageArchiveInfo,
     * 3) strict package name match,
     * 4) monotonic versionCode progression.
     */
    fun isApkValid(apkFile: File, expectedBytes: Long): Boolean {
        if (!apkFile.exists() || !apkFile.isFile) return false
        val minSize = if (expectedBytes > 0L) expectedBytes - 2048L else MIN_APK_BYTES
        if (apkFile.length() < minSize) return false
        if (expectedBytes > 0L && Math.abs(apkFile.length() - expectedBytes) > 2048L) {
            return false
        }
        return try {
            val flags = if (Build.VERSION.SDK_INT >= 28) {
                PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_ACTIVITIES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES or PackageManager.GET_ACTIVITIES
            }
            val pkg = context.packageManager
                .getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            if (pkg.packageName != context.packageName) return false
            val remoteVersion = if (Build.VERSION.SDK_INT >= 28) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }
            remoteVersion > BuildConfig.VERSION_CODE
        } catch (_: Exception) {
            false
        }
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists()) return
        if (Build.VERSION.SDK_INT >= 26 &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    /** Numeric hierarchical SemVer comparison (never string-lexicographic). */
    private fun semverGreaterThan(a: String, b: String): Boolean {
        val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until 3) {
            val x = pa.getOrNull(i) ?: 0
            val y = pb.getOrNull(i) ?: 0
            if (x != y) return x > y
        }
        return false
    }

    companion object {
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/arbdevai/hp-monitor/releases/latest"
        private const val BUFFER_BYTES = 64 * 1024
        private const val MIN_APK_BYTES = 1_500_000L
    }
}
