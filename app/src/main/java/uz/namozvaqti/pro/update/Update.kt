package uz.namozvaqti.pro.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONObject
import uz.namozvaqti.core.Versions
import uz.namozvaqti.pro.BuildConfig
import uz.namozvaqti.pro.R
import uz.namozvaqti.pro.ServiceLocator
import uz.namozvaqti.pro.alarm.Notifications
import uz.namozvaqti.pro.alarm.UzText
import uz.namozvaqti.pro.data.Http
import java.io.File
import java.security.MessageDigest

data class ReleaseInfo(val version: String, val notes: String, val apkUrl: String, val sizeBytes: Long)

sealed interface CheckResult {
    data class Available(val release: ReleaseInfo) : CheckResult
    data object UpToDate : CheckResult
    data class Failed(val reason: String) : CheckResult
}

/**
 * GitHub Releases orqali yangilanish. Xavfsizlik:
 *  - APK faqat foydalanuvchi roziligi bilan, Android'ning o'z o'rnatish oynasi orqali o'rnatiladi;
 *  - yuklangan APK paket nomi va imzosi o'rnatilgan ilovaniki bilan bir xil bo'lishi shart.
 */
object UpdateManager {
    private const val TAG = "UpdateManager"

    fun repoConfigured(): Boolean = !BuildConfig.GITHUB_REPO.startsWith("REPLACE_OWNER")

    fun check(): CheckResult {
        if (!repoConfigured()) return CheckResult.Failed("GitHub repository sozlanmagan")
        return try {
            val body = Http.get("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest", accept = "application/vnd.github+json")
            val o = JSONObject(body)
            val tag = o.optString("tag_name")
            if (!Versions.isNewer(tag, BuildConfig.VERSION_NAME)) return CheckResult.UpToDate
            val assets = o.optJSONArray("assets")
            var apk: JSONObject? = null
            for (i in 0 until (assets?.length() ?: 0)) {
                val a = assets!!.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) { apk = a; break }
            }
            if (apk == null) return CheckResult.Failed("Reliz ichida APK topilmadi")
            CheckResult.Available(
                ReleaseInfo(
                    version = tag.removePrefix("v"),
                    notes = o.optString("body").ifBlank { "O'zgarishlar ko'rsatilmagan." },
                    apkUrl = apk.getString("browser_download_url"),
                    sizeBytes = apk.optLong("size"),
                ),
            )
        } catch (e: Exception) {
            Log.w(TAG, "tekshirib bo'lmadi: ${e.message}")
            CheckResult.Failed(e.message ?: "Tarmoq xatosi")
        }
    }

    fun apkFile(ctx: Context, version: String) = File(File(ctx.cacheDir, "updates"), "namoz-vaqti-$version.apk")

    fun download(ctx: Context, release: ReleaseInfo, onProgress: (Float) -> Unit = {}): File {
        val file = apkFile(ctx, release.version)
        if (file.exists() && release.sizeBytes > 0 && file.length() == release.sizeBytes) return file
        File(ctx.cacheDir, "updates").listFiles()?.forEach { it.delete() }
        Http.download(release.apkUrl, file, onProgress)
        return file
    }

    /** Yuklangan APK shu ilovaning haqiqiy yangilanishi ekanini tekshiradi (nom + imzo). */
    fun verify(ctx: Context, apk: File): String? {
        val pm = ctx.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(apk.path, flags) ?: return "APK fayli buzuq"
        if (archive.packageName != ctx.packageName) return "APK boshqa ilovaga tegishli"
        val installed = pm.getPackageInfo(ctx.packageName, flags)
        return if (fingerprints(archive) == fingerprints(installed)) null
        else "APK imzosi o'rnatilgan ilova imzosiga mos emas (yangilanish o'rnatilmaydi)"
    }

    private fun fingerprints(info: android.content.pm.PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners
        else @Suppress("DEPRECATION") info.signatures
        val md = MessageDigest.getInstance("SHA-256")
        return sigs.orEmpty().map { md.digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) } }.toSet()
    }

    fun canInstall(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** O'rnatish: PackageInstaller sessiyasi; foydalanuvchi tasdig'i tizim oynasida so'raladi. */
    fun install(ctx: Context, apk: File): String? {
        verify(ctx, apk)?.let { return it }
        return try {
            val installer = ctx.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            if (Build.VERSION.SDK_INT >= 31) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("app.apk", 0, apk.length()).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }
                val pi = PendingIntent.getBroadcast(
                    ctx, id, Intent(ctx, InstallResultReceiver::class.java).setPackage(ctx.packageName),
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0),
                )
                session.commit(pi.intentSender)
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "o'rnatish xatosi", e)
            installViaViewIntent(ctx, apk)
        }
    }

    private fun installViaViewIntent(ctx: Context, apk: File): String? = try {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        null
    } catch (e: Exception) {
        "O'rnatish oynasini ochib bo'lmadi: ${e.message}"
    }
}

/** PackageInstaller natijasi: tasdiq kerak bo'lsa, tizim oynasini ochadi. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { ctx.startActivity(it) }
            }
            PackageInstaller.STATUS_SUCCESS -> Log.i("Update", "o'rnatildi")
            else -> Log.w("Update", "o'rnatish muvaffaqiyatsiz: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
        }
    }
}

/**
 * Fon tekshiruvi (12 soatda bir). Yangi versiya bo'lsa xabar beradi; Wi-Fi'da bo'lsa APKni oldindan
 * yuklab qo'yadi. O'rnatish — faqat foydalanuvchi bildirishnomani bosib, tasdiqlagandan keyin.
 */
class UpdateWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val s = ServiceLocator.get(ctx).settings.value
        if (!s.autoUpdateCheck || !s.onboardingDone) return Result.success()
        val result = UpdateManager.check()
        if (result !is CheckResult.Available) return Result.success()
        val prefs = ctx.getSharedPreferences("update_state", Context.MODE_PRIVATE)
        val r = result.release
        var ready = false
        if (s.autoDownloadUpdate && isUnmetered()) {
            ready = runCatching { UpdateManager.verify(ctx, UpdateManager.download(ctx, r)) == null }.getOrDefault(false)
        }
        if (prefs.getString("notified", null) != r.version || ready) {
            prefs.edit().putString("notified", r.version).apply()
            val text = if (ready) "Versiya ${r.version} yuklab olindi. O'rnatish uchun bosing." else "Versiya ${r.version} mavjud. Yuklab olish uchun bosing."
            val n = NotificationCompat.Builder(ctx, Notifications.CH_UPDATE)
                .setSmallIcon(R.drawable.ic_stat_namoz)
                .setContentTitle(UzText.of(ctx, "Namoz Vaqtlarim — yangilanish"))
                .setContentText(UzText.of(ctx, text))
                .setAutoCancel(true)
                .setContentIntent(Notifications.openAppIntent(ctx, 77))
                .build()
            Notifications.notify(ctx, Notifications.ID_UPDATE, n)
        }
        return Result.success()
    }

    private fun isUnmetered(): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return !cm.isActiveNetworkMetered
    }
}
