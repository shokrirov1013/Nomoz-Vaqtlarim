package uz.namozvaqti.pro.ui

import android.content.Context
import android.media.RingtoneManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.namozvaqti.pro.data.SOUND_BUILTIN
import uz.namozvaqti.pro.data.SOUND_DEFAULT_ALARM

data class SoundChoice(val id: String, val title: String)

object Sounds {
    fun choices(ctx: Context): List<SoundChoice> {
        val list = mutableListOf(SoundChoice(SOUND_BUILTIN, "Ilova ovozi (ting)"), SoundChoice(SOUND_DEFAULT_ALARM, "Standart budilnik"))
        runCatching {
            val rm = RingtoneManager(ctx).apply { setType(RingtoneManager.TYPE_ALARM) }
            rm.cursor.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = rm.getRingtoneUri(c.position).toString()
                    list += SoundChoice(uri, title)
                }
            }
        }
        return list
    }

    /** Tez: faqat bitta ovoz nomini o'qiydi (butun ro'yxatni emas). */
    fun titleOf(ctx: Context, id: String): String = when (id) {
        SOUND_BUILTIN -> "Ilova ovozi (ting)"
        SOUND_DEFAULT_ALARM -> "Standart budilnik"
        else -> runCatching {
            RingtoneManager.getRingtone(ctx, android.net.Uri.parse(id)).getTitle(ctx)
        }.getOrNull() ?: "Ilova ovozi (ting)"
    }
}

@Composable
fun SoundPickerDialog(ctx: Context, current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val items = Sounds.choices(ctx)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ovozni tanlang") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                items.forEach {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().clickableNoRipple { onPick(it.id) }.padding(vertical = 10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        GlassTick(it.id == current, size = 22.dp)
                        Text("  " + it.title, style = if (it.id == current) Type.BodyBold else Type.Body)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Yopish") } },
    )
}
