package uz.namozvaqti.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.PrayerTexts
import uz.namozvaqti.pro.AppViewModel
import uz.namozvaqti.pro.data.PrayerLog
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun HomeScreen(vm: AppViewModel, go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val now by vm.now.collectAsState()
    val times by vm.todayTimes.collectAsState()
    val tomorrow by vm.tomorrowTimes.collectAsState()
    val info by vm.timesInfo.collectAsState()
    val today = now.toLocalDate()
    val logs by remember(today) { vm.logsInRange(today, today) }.collectAsState(emptyList())
    val doneMap: Map<Prayer, PrayerLog> = logs.mapNotNull { l -> Prayer.fromKey(l.prayer)?.let { it to l } }.toMap()
    var confirmUndo by remember { mutableStateOf<Prayer?>(null) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.height(4.dp))
        // ---- sarlavha
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (s.name.isBlank()) "Assalomu alaykum" else "Assalomu alaykum, ${s.name}",
                    style = Type.H1,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = Glass.Purple, modifier = Modifier.size(18.dp))
                    Text(" ${s.locationLabel}", style = Type.Caption.copy(fontSize = 15.sp))
                }
                Text(Fmt.gregorianFull(today), style = Type.Caption.copy(fontSize = 15.sp), modifier = Modifier.padding(top = 2.dp))
                Text(Fmt.hijri(today, s.hijriAdjust), style = Type.Caption.copy(fontSize = 15.sp, color = Glass.PurpleDeep))
            }
            CrescentMosqueArt(Modifier.size(width = 96.dp, height = 84.dp))
        }

        // ---- ruxsat ogohlantirishlari (eslatmalar ishlashi uchun)
        Warnings(go)

        // ---- keyingi namoz
        val t = times
        if (t != null) {
            val candidates = Prayer.entries.map { it to LocalDateTime.of(today, t.of(it)) } +
                listOfNotNull(tomorrow?.let { Prayer.FAJR to LocalDateTime.of(today.plusDays(1), it.fajr) })
            val next = candidates.firstOrNull { it.second.isAfter(now) }
            GlassCard(Modifier.fillMaxWidth(), tint = Glass.PinkSoft) {
                Text("Keyingi namoz", style = Type.Caption)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(next?.first?.title ?: "—", style = Type.H2.copy(fontSize = 24.sp), modifier = Modifier.weight(1f))
                    next?.let { Text(Fmt.time(it.second.toLocalTime()), style = Type.H2) }
                }
                Text(
                    next?.let { Fmt.countdown(Duration.between(now, it.second)) } ?: "--:--:--",
                    style = Type.Big.copy(color = Glass.PurpleDeep),
                )
                Text("qoldi", style = Type.Caption)
            }

            // ---- besh vaqt namoz
            val doneCount = Prayer.entries.count { it in doneMap }
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bugungi namozlar", style = Type.H2, modifier = Modifier.weight(1f))
                    Text("$doneCount / 5 bajarildi", style = Type.BodyBold.copy(color = Glass.PurpleDeep))
                }
                Box(Modifier.height(8.dp))
                ProgressBar(doneCount / 5f)
                Box(Modifier.height(8.dp))
                Prayer.entries.forEachIndexed { i, p ->
                    if (i == 1) {
                        // Quyosh chiqishi alohida: namoz emas, shuning uchun belgilash tugmasi yo'q.
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Quyosh chiqishi", style = Type.Caption.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                            Text(Fmt.time(t.sunrise), style = Type.Caption.copy(fontSize = 14.sp))
                        }
                        Divider()
                    }
                    PrayerRow(
                        prayer = p,
                        label = if (PrayerTexts.isFridayMale(p, today, s.gender)) "Peshin (Juma)" else p.title,
                        time = Fmt.time(t.of(p)),
                        isNext = next?.first == p && next.second.toLocalDate() == today,
                        log = doneMap[p],
                        onMark = { vm.setDone(today, p, true) },
                        onUndo = { confirmUndo = p },
                    )
                    if (i < Prayer.entries.lastIndex) Divider()
                }
            }
        } else {
            GlassCard(Modifier.fillMaxWidth()) { Text("Vaqtlar hisoblanmoqda…", style = Type.Body) }
        }

        // ---- manba
        info?.let {
            val updated = it.fetchedAt?.let { ms ->
                val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
                " · yangilandi ${Fmt.time(dt.toLocalTime())}, ${dt.dayOfMonth}.${"%02d".format(dt.monthValue)}"
            } ?: ""
            Text("Manba: ${it.source}$updated", style = Type.Caption, modifier = Modifier.padding(horizontal = 4.dp))
        }

        // ---- tezkor kirish
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("Tahajjud", { go(Screen.TAHAJJUD) }, Modifier.weight(1f), filled = false, icon = Icons.Default.Star)
            GlassButton("Taqvim", { go(Screen.CALENDAR) }, Modifier.weight(1f), filled = false, icon = Icons.Default.DateRange)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("Tarix", { go(Screen.HISTORY) }, Modifier.weight(1f), filled = false, icon = Icons.AutoMirrored.Filled.List)
            GlassButton("Sozlamalar", { go(Screen.SETTINGS) }, Modifier.weight(1f), filled = false, icon = Icons.Default.Settings)
        }
        Box(Modifier.height(96.dp))
    }

    confirmUndo?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmUndo = null },
            title = { Text("${p.title} belgisini olib tashlaysizmi?") },
            text = { Text("Namoz “belgilanmagan” holatiga qaytadi, qolgan eslatmalar qayta yoqiladi.") },
            confirmButton = { TextButton(onClick = { vm.setDone(today, p, false); confirmUndo = null }) { Text("Ha, olib tashlash") } },
            dismissButton = { TextButton(onClick = { confirmUndo = null }) { Text("Yo'q") } },
        )
    }
}

@Composable
private fun PrayerRow(
    prayer: Prayer,
    label: String,
    time: String,
    isNext: Boolean,
    log: PrayerLog?,
    onMark: () -> Unit,
    onUndo: () -> Unit,
) {
    val done = log != null
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = Type.H2.copy(fontSize = 18.sp))
                if (isNext) {
                    Box(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(Glass.PinkSoft).padding(horizontal = 8.dp, vertical = 2.dp),
                    ) { Text("keyingi", style = Type.Caption.copy(fontSize = 11.sp, color = Glass.PurpleDeep)) }
                }
            }
            Text(
                if (log != null) "Belgilangan: ${Fmt.time(LocalDateTime.ofInstant(Instant.ofEpochMilli(log.markedAt), ZoneId.systemDefault()).toLocalTime())}"
                else "Belgilanmagan",
                style = Type.Caption.copy(fontSize = 12.sp),
            )
        }
        Text(time, style = Type.H2.copy(fontSize = 20.sp), modifier = Modifier.padding(end = 12.dp))
        if (done) {
            DoneChip(onUndo)
        } else {
            GlassButton("Bajarildi", onMark, small = true)
        }
    }
}

@Composable
fun DoneChip(onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Glass.Lavender.copy(alpha = 0.55f), Glass.Pink.copy(alpha = 0.55f))))
            .clickableNoRipple(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Check, null, tint = Glass.PurpleDeep, modifier = Modifier.size(18.dp))
        Text(" Bajarildi", style = Type.BodyBold.copy(fontSize = 14.sp, color = Glass.PurpleDeep))
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Glass.Lavender.copy(alpha = 0.3f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Glass.Pink, Glass.Purple))),
        )
    }
}

@Composable
private fun Warnings(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val problems = buildList {
        if (!Perms.notifications(ctx)) add("Bildirishnomalar o'chiq — eslatmalar ko'rinmaydi")
        if (!Perms.exactAlarms(ctx)) add("Aniq budilnik ruxsati yo'q — eslatmalar kechikishi mumkin")
    }
    if (problems.isEmpty()) return
    GlassCard(Modifier.fillMaxWidth(), tint = Glass.PinkSoft, onClick = { go(Screen.SETTINGS) }) {
        problems.forEach { Text("• $it", style = Type.Caption.copy(color = Glass.Danger, fontSize = 14.sp)) }
        Text("Sozlamalar → Ruxsatlar", style = Type.BodyBold.copy(color = Glass.PurpleDeep, fontSize = 14.sp))
    }
}
