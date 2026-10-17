package uz.namozvaqti.pro.alarm

object Extra {
    const val DATE = "date"
    const val PRAYER = "prayer"
    const val STAGE = "stage"
    const val AT = "at"
    const val KIND = "kind"
    const val TEXT = "text"
}

object Act {
    const val REMINDER = "uz.namozvaqti.pro.REMINDER"
    const val RING = "uz.namozvaqti.pro.RING"
    const val DAILY = "uz.namozvaqti.pro.DAILY"
    const val MARK_DONE = "uz.namozvaqti.pro.MARK_DONE"
    const val STOP_RING = "uz.namozvaqti.pro.STOP_RING"
    const val SNOOZE_RING = "uz.namozvaqti.pro.SNOOZE_RING"
}

/** Budilnik turi: Bomdod yoki Tahajjud. */
enum class RingKind(val key: String, val title: String) {
    FAJR("fajr", "Bomdod budilnigi"),
    TAHAJJUD("tahajjud", "Tahajjud");

    companion object {
        fun fromKey(key: String?): RingKind = entries.firstOrNull { it.key == key } ?: FAJR
    }
}
