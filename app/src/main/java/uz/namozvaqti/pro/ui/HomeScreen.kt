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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.PrayerTexts
import uz.namozvaqti.core.PrayerTimes
import uz.namozvaqti.pro.AppViewModel
import uz.namozvaqti.pro.data.PrayerLog
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private data class NextPrayer(val prayer: Prayer, val at: LocalDateTime)

private fun nextPrayerOf(now: LocalDateTime, today: LocalDate, t: PrayerTimes?, tomorrow: PrayerTimes?): NextPrayer? {
    if (t == null) return null
    Prayer.entries.forEach { p ->
        val at = LocalDateTime.of(today, t.of(p))
        if (at.isAfter(now)) return NextPrayer(p, at)
    }
    return tomorrow?.let { NextPrayer(Prayer.FAJR, LocalDateTime.of(today.plusDays(1), it.fajr)) }
}

@Composable
fun HomeScreen(vm: AppViewModel, go: (Screen) -> Unit) {
    val s by vm.settings.collectAsState()
    val today by vm.today.collectAsState()
    val times by vm.todayTimes.collectAsState()
    val tomorrow by vm.tomorrowTimes.collectAsState()
    val info by vm.timesInfo.collectAsState()
    val logs by remember(today) { vm.logsInRange(today, today) }.collectAsState(emptyList())
    val doneMap: Map<Prayer, PrayerLog> = remember(logs) {
        logs.mapNotNull { l -> Prayer.fromKey(l.prayer)?.let { it to l } }.toMap()
    }
    var confirmUndo by remember { mutableStateOf<Prayer?>(null) }

    // Soniyalik "now" shu yerda o'qilmaydi: faqat keyingi namoz o'zgarganda qayta chiziladi.
    val nowState = vm.now.collectAsState()
    val next by remember(times, tomorrow, today) {
        derivedStateOf { nextPrayerOf(nowState.value, today, times, tomorrow) }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState(), flingBehavior = SoftFling).padding(horizontal = 16.dp),
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
                    Icon(Icons.Default.LocationOn, null, tint = Glass.Gold, modifier = Modifier.size(18.dp))
                    Text(" ${s.locationLabel}", style = Type.Caption.copy(fontSize = 15.sp))
                }
                Text(Fmt.gregorianFull(today), style = Type.Caption.copy(fontSize = 15.sp), modifier = Modifier.padding(top = 2.dp))
                Text(Fmt.hijri(today, s.hijriAdjust), style = Type.Caption.copy(fontSize = 15.sp, color = Glass.GoldBright))
            }
            CrescentMosqueArt(Modifier.size(width = 96.dp, height = 84.dp))
        }

        Warnings(go)

        // ---- keyingi namoz (faqat shu karta har soniya yangilanadi)
        NextPrayerCard(vm, next)

        val t = times
        if (t != null) {
            val doneCount = Prayer.entries.count { it in doneMap }
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bugungi namozlar", style = Type.H2, modifier = Modifier.weight(1f))
                    Text("$doneCount / 5 bajarildi", style = Type.BodyBold.copy(color = Glass.GoldBright))
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
                        label = if (PrayerTexts.isFridayMale(p, today, s.gender)) "Peshin (Juma)" else p.title,
                        time = Fmt.time(t.of(p)),
                        isNext = next?.prayer == p && next?.at?.toLocalDate() == today,
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
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Manba: ${it.source}$updated", style = Type.Caption, modifier = Modifier.weight(1f))
                val refreshing by vm.refreshing.collectAsState()
                GlassButton(if (refreshing) "Yangilanmoqda…" else "Yangilash", { vm.refreshTimes() }, small = true, filled = false, enabled = !refreshing)
            }
        }

        // ---- tezkor kirish
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("Suralar", { go(Screen.QURAN) }, Modifier.weight(1f), filled = false, icon = TabIcons.Book)
            GlassButton("Tahajjud", { go(Screen.TAHAJJUD) }, Modifier.weight(1f), filled = false, icon = TabIcons.Crescent)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("Taqvim", { go(Screen.CALENDAR) }, Modifier.weight(1f), filled = false, icon = Icons.Default.DateRange)
            GlassButton("Tarix", { go(Screen.HISTORY) }, Modifier.weight(1f), filled = false, icon = Icons.AutoMirrored.Filled.List)
        }
        Box(Modifier.height(100.dp))
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
private fun NextPrayerCard(vm: AppViewModel, next: NextPrayer?) {
    val now by vm.now.collectAsState()
    GlassCard(Modifier.fillMaxWidth(), tint = Glass.Tint) {
        Text("Keyingi namoz", style = Type.Caption)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(next?.prayer?.title ?: "—", style = Type.H2.copy(fontSize = 24.sp), modifier = Modifier.weight(1f))
            next?.let { Text(Fmt.time(it.at.toLocalTime()), style = Type.H2.copy(color = Glass.GoldBright)) }
        }
        Text(
            next?.let { Fmt.countdown(Duration.between(now, it.at)) } ?: "--:--:--",
            style = Type.Big.copy(color = Glass.GoldBright),
        )
        Text("qoldi", style = Type.Caption)
    }
}

@Composable
private fun PrayerRow(
    label: String,
    time: String,
    isNext: Boolean,
    log: PrayerLog?,
    onMark: () -> Unit,
    onUndo: () -> Unit,
) {
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
                        Modifier.background(Glass.Gold.copy(alpha = 0.18f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
                    ) { Text("keyingi", style = Type.Caption.copy(fontSize = 11.sp, color = Glass.GoldBright)) }
                }
            }
            Text(
                if (log != null) "Belgilangan: ${Fmt.time(LocalDateTime.ofInstant(Instant.ofEpochMilli(log.markedAt), ZoneId.systemDefault()).toLocalTime())}"
                else "Belgilanmagan",
                style = Type.Caption.copy(fontSize = 12.sp),
            )
        }
        Text(time, style = Type.H2.copy(fontSize = 20.sp), modifier = Modifier.padding(end = 12.dp))
        if (log != null) DoneChip(onUndo) else GlassButton("Bajarildi", onMark, small = true)
    }
}

private val DoneBrush = Brush.linearGradient(listOf(Glass.Emerald.copy(alpha = 0.32f), Glass.Mint.copy(alpha = 0.18f)))

@Composable
fun DoneChip(onClick: () -> Unit) {
    Row(
        Modifier
            .background(DoneBrush, RoundedCornerShape(14.dp))
            .clickableNoRipple(onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassTick(true, size = 20.dp)
        Text("  Bajarildi", style = Type.BodyBold.copy(fontSize = 14.sp, color = Glass.GoldBright))
    }
}

private val TrackColor = Glass.Mint.copy(alpha = 0.14f)
private val FillBrush = Brush.horizontalGradient(listOf(Glass.Emerald, Glass.Gold))

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(10.dp).background(TrackColor, RoundedCornerShape(50))) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(10.dp)
                .background(FillBrush, RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun Warnings(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    rememberResumeTick()
    val problems = buildList {
        if (!Perms.notifications(ctx)) add("Bildirishnomalar o'chiq — eslatmalar ko'rinmaydi")
        if (!Perms.exactAlarms(ctx)) add("Aniq budilnik ruxsati yo'q — eslatmalar kechikishi mumkin")
    }
    if (problems.isEmpty()) return
    GlassCard(Modifier.fillMaxWidth(), tint = Glass.Danger, onClick = { go(Screen.SETTINGS) }) {
        problems.forEach { Text("• $it", style = Type.Caption.copy(color = Glass.Danger, fontSize = 14.sp)) }
        Text("Sozlamalar → Ruxsatlar", style = Type.BodyBold.copy(color = Glass.GoldBright, fontSize = 14.sp))
    }
}
