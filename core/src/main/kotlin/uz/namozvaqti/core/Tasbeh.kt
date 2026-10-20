package uz.namozvaqti.core

/** Zikr turi: nomi, arabcha matni, ma'nosi va odatiy maqsad (0 = cheksiz). */
data class Dhikr(val key: String, val title: String, val arabic: String, val meaning: String, val defaultTarget: Int)

/**
 * Tasbeh hisoblagichi (o'zgarmas): [count] — joriy davradagi son, [rounds] — to'liq tugagan davralar,
 * [total] — hamma vaqtdagi jami. Maqsad [target] > 0 bo'lsa, shunga yetganda davra yopiladi;
 * keyingi bosish yangi davrani 1 dan boshlaydi. Maqsad 0 bo'lsa — cheksiz sanash.
 */
data class TasbehCounter(val count: Int = 0, val rounds: Int = 0, val total: Int = 0) {
    /** Bosish: yangi holat. */
    fun tap(target: Int): TasbehCounter {
        val first = if (target > 0 && count >= target) 1 else count + 1
        val closed = target > 0 && first == target
        return TasbehCounter(first, rounds + if (closed) 1 else 0, total + 1)
    }

    /** Bitta orqaga (xato bosilganda). Davra yopilgan bo'lsa, uni ham ortga qaytaradi. */
    fun undo(target: Int): TasbehCounter {
        if (count <= 0) return this
        val reopen = target > 0 && count == target && rounds > 0
        return TasbehCounter(count - 1, if (reopen) rounds - 1 else rounds, (total - 1).coerceAtLeast(0))
    }

    /** Joriy davra va davralarni nolga tushiradi; hamma vaqtdagi jami saqlanadi. */
    fun reset() = TasbehCounter(0, 0, total)

    /** Davra tugadimi (count maqsadga teng). */
    fun isClosed(target: Int) = target > 0 && count == target

    /** Doira to'lish ulushi 0..1 (cheksiz rejimda har 33 tada aylanadi). */
    fun progress(target: Int): Float {
        val t = if (target > 0) target else 33
        if (count <= 0) return 0f
        val r = count % t
        return if (r == 0) 1f else r.toFloat() / t
    }
}

object Tasbeh {
    val dhikrs: List<Dhikr> = listOf(
        Dhikr("subhanalloh", "Subhanalloh", "سُبْحَانَ ٱللَّهِ", "Alloh (har qanday nuqsondan) pokdir", 33),
        Dhikr("alhamdulillah", "Alhamdulillah", "ٱلْحَمْدُ لِلَّهِ", "Hamd Allohgadir", 33),
        Dhikr("allohuakbar", "Allohu akbar", "ٱللَّهُ أَكْبَرُ", "Alloh buyukdir", 33),
        Dhikr("lailaha", "La ilaha illalloh", "لَا إِلَٰهَ إِلَّا ٱللَّهُ", "Allohdan o'zga iloh yo'q", 100),
        Dhikr("astagfirulloh", "Astag'firulloh", "أَسْتَغْفِرُ ٱللَّهَ", "Allohdan mag'firat so'rayman", 100),
        Dhikr("salavot", "Salovot", "ٱللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ", "Ey Alloh, Muhammadga salovot yo'lla", 100),
        Dhikr("lahavla", "La havla va la quvvata", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِٱللَّهِ", "Kuch-quvvat faqat Alloh bilandir", 100),
        Dhikr("free", "Erkin sanash", "", "O'zingiz tanlagan zikr uchun", 0),
    )

    /** Tanlanadigan maqsadlar; 0 = cheksiz. */
    val targets: List<Int> = listOf(33, 99, 100, 0)

    fun byKey(key: String?): Dhikr = dhikrs.firstOrNull { it.key == key } ?: dhikrs.first()

    fun targetLabel(target: Int) = if (target > 0) target.toString() else "∞"
}
