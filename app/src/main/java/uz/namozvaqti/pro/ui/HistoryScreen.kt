package uz.namozvaqti.pro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.namozvaqti.core.PeriodStats
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.StatsCalculator
import uz.namozvaqti.pro.AppViewModel
import uz.namozvaqti.pro.data.PrayerLog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    val today = vm.now.collectAsState().value.toLocalDate()
    var selectedStr by rememberSaveable { mutableStateOf(today.toString()) }
    val selected = LocalDate.parse(selectedStr)
    var period by rememberSaveable { mutableStateOf("week") }

    val dayLogs by remember(selected) { vm.logsInRange(selected, selected) }.collectAsState(emptyList())
    val logMap: Map<Prayer, PrayerLog> = dayLogs.mapNotNull { l -> Prayer.fromKey(l.prayer)?.let { it to l } }.toMap()
    var times by remember { mutableStateOf<uz.namozvaqti.core.PrayerTimes?>(null) }
    LaunchedEffect(selected, s.lat, s.lon, s.method, s.hanafiAsr) { times = vm.timesFor(selected) }

    val (from, to) = if (period == "week") selected.minusDays(6) to selected
    else YearMonth.from(selected).atDay(1) to minOf(YearMonth.from(selected).atEndOfMonth(), today)
    val doneSet by remember(from, to) { vm.doneSet(from, to) }.collectAsState(emptySet())
    val stats = StatsCalculator.compute(from, to, doneSet)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Namozlar tarixi", style = Type.H1, modifier = Modifier.padding(top = 4.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clickableNoRipple { selectedStr = selected.minusDays(1).toString() }.padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Oldingi kun", tint = Glass.PurpleDeep)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Fmt.weekday(selected.dayOfWeek) + if (selected == today) " (bugun)" else "", style = Type.BodyBold)
                    Text(Fmt.gregorian(selected), style = Type.Caption)
                    Text(Fmt.hijri(selected, s.hijriAdjust), style = Type.Caption.copy(color = Glass.PurpleDeep))
                }
                Box(
                    Modifier.clickableNoRipple { if (selected.isBefore(today)) selectedStr = selected.plusDays(1).toString() }.padding(8.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, "Keyingi kun",
                        tint = if (selected.isBefore(today)) Glass.PurpleDeep else Glass.Lavender,
                    )
                }
            }
            Box(Modifier.height(8.dp))
            Prayer.entries.forEachIndexed { i, p ->
                val log = logMap[p]
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = Type.H2.copy(fontSize = 17.sp))
                        Text(
                            if (log != null) "Bajarildi · belgilangan ${markedAt(log)}" else "Belgilanmagan",
                            style = Type.Caption.copy(fontSize = 12.sp),
                        )
                    }
                    times?.let { Text(Fmt.time(it.of(p)), style = Type.Body, modifier = Modifier.padding(end = 10.dp)) }
                    if (log != null) DoneChip { vm.setDone(selected, p, false) }
                    else GlassButton("Bajarildi", { vm.setDone(selected, p, true) }, small = true, filled = false)
                }
                if (i < 4) Divider()
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassChip("Haftalik", period == "week", { period = "week" })
            GlassChip("Oylik", period == "month", { period = "month" })
        }
        StatsCard(
            title = if (period == "week") "So'nggi 7 kun (${Fmt.gregorian(from)} — ${Fmt.gregorian(to)})"
            else "${Fmt.months[selected.monthValue - 1]} ${selected.year}",
            stats = stats,
        )
        Text(
            "Statistika faqat siz o'zingiz “Bajarildi” deb belgilagan ma'lumotlarga asoslanadi. " +
                "Belgilanmagan namoz o'qilmagan degani emas — shunchaki belgilanmagan.",
            style = Type.Caption, modifier = Modifier.padding(horizontal = 4.dp),
        )
        Box(Modifier.height(96.dp))
    }
}

private fun markedAt(l: PrayerLog): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(l.markedAt), ZoneId.systemDefault())
    return "${Fmt.time(dt.toLocalTime())}, ${dt.dayOfMonth}.${"%02d".format(dt.monthValue)}"
}

@Composable
private fun StatsCard(title: String, stats: PeriodStats) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(title, style = Type.Caption)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Text("${stats.done}", style = Type.Big.copy(color = Glass.PurpleDeep))
            Text(" / ${stats.slots} bajarildi", style = Type.Body, modifier = Modifier.padding(bottom = 8.dp))
            Box(Modifier.weight(1f))
            Text("${stats.percentDone}%", style = Type.H2.copy(color = Glass.PurpleDeep))
        }
        ProgressBar(stats.percentDone / 100f)
        Text("Belgilanmagan: ${stats.unmarked}", style = Type.Caption, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
        Prayer.entries.forEach { p ->
            val n = stats.perPrayer[p] ?: 0
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.title, style = Type.Body.copy(fontSize = 15.sp), modifier = Modifier.width(72.dp))
                ProgressBar(if (stats.days == 0) 0f else n.toFloat() / stats.days, Modifier.weight(1f))
                Text("$n/${stats.days}", style = Type.Caption, modifier = Modifier.width(52.dp).padding(start = 8.dp))
            }
        }
    }
}
