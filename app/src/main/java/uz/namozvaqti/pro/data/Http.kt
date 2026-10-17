package uz.namozvaqti.pro.data

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object Http {
    private const val UA = "NamozVaqtlarim/1 (Android)"

    fun get(url: String, timeoutMs: Int = 15_000, accept: String = "application/json"): String {
        val c = open(url, timeoutMs, accept)
        try {
            if (c.responseCode != 200) throw java.io.IOException("HTTP ${c.responseCode} — $url")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** Faylni yuklab oladi; [onProgress] 0f..1f. */
    fun download(url: String, target: File, onProgress: (Float) -> Unit = {}) {
        val c = open(url, 30_000, "application/octet-stream")
        try {
            if (c.responseCode != 200) throw java.io.IOException("HTTP ${c.responseCode}")
            val total = c.contentLengthLong
            target.parentFile?.mkdirs()
            val tmp = File(target.path + ".part")
            c.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(32 * 1024)
                    var read: Long = 0
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress(read.toFloat() / total)
                    }
                }
            }
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) throw java.io.IOException("Faylni saqlab bo'lmadi")
            onProgress(1f)
        } finally {
            c.disconnect()
        }
    }

    private fun open(url: String, timeoutMs: Int, accept: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = timeoutMs
        c.readTimeout = timeoutMs
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Accept", accept)
        return c
    }
}
