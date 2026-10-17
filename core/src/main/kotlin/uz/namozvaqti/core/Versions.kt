package uz.namozvaqti.core

object Versions {
    /** "v1.2.3", "1.2", "1.2.3-beta" -> [1,2,3]. Tushunilmasa null. */
    fun parse(raw: String): List<Int>? {
        val core = raw.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
        if (core.isEmpty()) return null
        val parts = core.split('.').map { it.toIntOrNull() ?: return null }
        return parts
    }

    fun isNewer(remote: String, local: String): Boolean {
        val r = parse(remote) ?: return false
        val l = parse(local) ?: return false
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
