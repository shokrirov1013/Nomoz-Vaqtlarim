package uz.namozvaqti.pro.data

import android.content.Context
import uz.namozvaqti.core.Tasbeh
import uz.namozvaqti.core.TasbehCounter
import java.time.LocalDate

/** Tasbeh holati: har bir zikr uchun alohida hisoblagich va maqsad, tanlangan zikr, tebranish va bugungi jami. */
data class TasbehData(
    val dhikr: String = Tasbeh.dhikrs.first().key,
    val counters: Map<String, TasbehCounter> = emptyMap(),
    val targets: Map<String, Int> = emptyMap(),
    val haptic: Boolean = true,
    val todayDate: String = "",
    val today: Int = 0,
) {
    fun counter(key: String = dhikr): TasbehCounter = counters[key] ?: TasbehCounter()
    fun target(key: String = dhikr): Int = targets[key] ?: Tasbeh.byKey(key).defaultTarget

    /** Bugungi jami (kun o'zgargan bo'lsa 0). */
    fun todayCount(now: LocalDate = LocalDate.now()): Int = if (todayDate == now.toString()) today else 0
}

/** Tasbeh holatini telefon xotirasida saqlaydi (ilova yopilsa ham yo'qolmaydi). */
class TasbehStore(context: Context) {
    private val prefs = context.getSharedPreferences("tasbeh", Context.MODE_PRIVATE)

    fun load(): TasbehData {
        val p = prefs
        val counters = Tasbeh.dhikrs.associate { d ->
            d.key to TasbehCounter(p.getInt("c_${d.key}", 0), p.getInt("r_${d.key}", 0), p.getInt("t_${d.key}", 0))
        }
        val targets = Tasbeh.dhikrs.associate { d -> d.key to p.getInt("g_${d.key}", d.defaultTarget) }
        return TasbehData(
            dhikr = Tasbeh.byKey(p.getString("dhikr", null)).key,
            counters = counters,
            targets = targets,
            haptic = p.getBoolean("haptic", true),
            todayDate = p.getString("todayDate", "") ?: "",
            today = p.getInt("today", 0),
        )
    }

    fun save(d: TasbehData) {
        prefs.edit().apply {
            putString("dhikr", d.dhikr)
            putBoolean("haptic", d.haptic)
            putString("todayDate", d.todayDate)
            putInt("today", d.today)
            d.counters.forEach { (k, c) ->
                putInt("c_$k", c.count); putInt("r_$k", c.rounds); putInt("t_$k", c.total)
            }
            d.targets.forEach { (k, t) -> putInt("g_$k", t) }
        }.apply()
    }
}
