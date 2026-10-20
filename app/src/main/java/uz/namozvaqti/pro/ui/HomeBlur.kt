package uz.namozvaqti.pro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.PrayerTexts
import uz.namozvaqti.core.PrayerTimes
import uz.namozvaqti.pro.AppViewModel
import uz.namozvaqti.pro.data.PrayerLog
import uz.namozvaqti.pro.data.Settings
import uz.namozvaqti.pro.data.TimesInfo
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Sodda blur glass rejimidagi bosh sahifa (v1.1.0 tartibi): sarlavha + hilol, "Keyingi namoz" kartasi,
 * "Bugungi namozlar" kartasi (progress + shisha belgilash sharchalari), manba qatori va tezkor tugmalar.
 * Soniyalik vaqt faqat hisoblagich va keyingi namoz qatori ichida o'qiladi — qolgani qayta chizilmaydi.
 */
@Composable
internal fun HomeBlur(
    vm: AppViewModel,
    s: Settings,
    today: LocalDate,
    times: PrayerTimes?,
    next: NextPrayer?,
    nowState: State<LocalDateTime>,
    doneMap: Map<Prayer, PrayerLog>,
    info: TimesInfo?,
    go: (Screen) -> Unit,
    onUndo: (Prayer) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), flingBehavior = SoftFling)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 14.dp, top = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                val hello = Type.H1.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Assalomu alaykum,", style = hello)
                Text(s.name.ifBlank { "Do'stim" }, style = hello, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Default.LocationOn, null, tint = Glass.Pink, modifier = Modifier.size(16.dp))
                    Text(" ${s.locationLabel}", style = Type.Caption.copy(fontSize = 13.sp))
                }
                Text(Fmt.gregorianFull(today), style = Type.Caption.copy(fontSize = 13.sp), modifier = Modifier.padding(top = 2.dp))
                Text(Fmt.hijri(today, s.hijriAdjust), style = Type.Caption.copy(fontSize = 13.sp, color = Glass.PinkSoft, fontWeight = FontWeight.SemiBold))
            }
            CrescentMosqueArt(Modifier.size(width = 98.dp, height = 86.dp))
        }

        Column(Modifier.padding(horizontal = 16.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Warnings(go)
            NextBlurCard(next, nowState)
            PrayersBlurCard(s, today, times, next, nowState, doneMap, onUndo, vm)

            info?.let {
                val updated = it.fetchedAt?.let { ms ->
                    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
                    " · yangilandi ${Fmt.time(dt.toLocalTime())}"
                } ?: ""
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Manba: ${it.source.substringBefore(" bilan")}$updated", style = Type.Caption.copy(fontSize = 11.sp), modifier = Modifier.weight(1f))
                    val refreshing by vm.refreshing.collectAsState()
                    GlassButton(if (refreshing) "Yangilanmoqda…" else "Yangilash", { vm.refreshTimes() }, small = true, filled = false, enabled = !refreshing)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickButton("Suralar", TabIcons.Book, Modifier.weight(1f)) { go(Screen.QURAN) }
                QuickButton("Tahajjud", TabIcons.Crescent, Modifier.weight(1f)) { go(Screen.TAHAJJUD) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickButton("Taqvim", Icons.Default.DateRange, Modifier.weight(1f)) { go(Screen.CALENDAR) }
                QuickButton("Tarix", Icons.AutoMirrored.Filled.List, Modifier.weight(1f)) { go(Screen.HISTORY) }
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun QuickButton(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    GlassButton(text, onClick, modifier, filled = false, icon = icon)
}

@Composable
private fun NextBlurCard(next: NextPrayer?, nowState: State<LocalDateTime>) {
    val now by nowState
    GlassCard(Modifier.fillMaxWidth(), corner = 26.dp, tint = Glass.Tint) {
        Text("Keyingi namoz", style = Type.Caption)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(next?.prayer?.title ?: "—", style = Type.H1.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            next?.let { Text(Fmt.time(it.at.toLocalTime()), style = Type.H2.copy(color = Glass.PinkSoft, fontWeight = FontWeight.Bold)) }
        }
        Text(
            next?.let { Fmt.countdown(Duration.between(now, it.at)) } ?: "--:--:--",
            style = Type.Big.copy(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Glass.PinkSoft),
        )
        Text("qoldi", style = Type.Caption)
    }
}

@Composable
private fun PrayersBlurCard(
    s: Settings,
    today: LocalDate,
    times: PrayerTimes?,
    next: NextPrayer?,
    nowState: State<LocalDateTime>,
    doneMap: Map<Prayer, PrayerLog>,
    onUndo: (Prayer) -> Unit,
    vm: AppViewModel,
) {
    val total = Prayer.entries.size
    GlassCard(Modifier.fillMaxWidth(), corner = 26.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bugungi namozlar", style = Type.H2.copy(fontWeight = FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            Text("${doneMap.size} / $total bajarildi", style = Type.Caption.copy(color = Glass.PinkSoft, fontWeight = FontWeight.Bold))
        }
        ProgressBar(doneMap.size.toFloat() / total, Modifier.padding(top = 10.dp, bottom = 4.dp))
        if (times == null) {
            Text("Vaqtlar hisoblanmoqda…", style = Type.Body, modifier = Modifier.padding(top = 8.dp))
            return@GlassCard
        }
        Prayer.entries.forEachIndexed { i, p ->
            val isNext = next?.prayer == p && next?.at?.toLocalDate() == today
            val log = doneMap[p]
            PrayerBlurRow(
                label = if (PrayerTexts.isFridayMale(p, today, s.gender)) "Peshin (Juma)" else p.title,
                time = Fmt.time(times.of(p)),
                caption = when {
                    log != null -> "Bajarildi · " + Fmt.time(LocalDateTime.ofInstant(Instant.ofEpochMilli(log.markedAt), ZoneId.systemDefault()).toLocalTime())
                    isNext -> "Keyingi"
                    else -> ""
                },
                isNext = isNext,
                done = log != null,
                level = { if (isNext && log == null) elapsedFraction(nowState.value, today, times, next) else 0f },
                onMark = { vm.setDone(today, p, true) },
                onUndo = { onUndo(p) },
            )
            if (i == 0) SunriseRow(times.sunrise)
            if (i < total - 1) Divider()
        }
    }
}

@Composable
private fun PrayerBlurRow(
    label: String,
    time: String,
    caption: String,
    isNext: Boolean,
    done: Boolean,
    level: () -> Float,
    onMark: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Type.H2.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold))
            if (caption.isNotEmpty()) {
                Text(caption, style = Type.Caption.copy(fontSize = 11.sp, color = if (done) Glass.PinkSoft else Glass.InkSoft))
            }
        }
        Text(time, style = Type.H2.copy(fontSize = 17.sp, color = Glass.PinkSoft, fontWeight = FontWeight.Bold), modifier = Modifier.padding(end = 12.dp))
        // level lambda faqat keyingi namoz qatorida soniyalik vaqtni o'qiydi
        GlassOrb(done, size = 34.dp, level = level(), active = isNext && !done, onClick = if (done) onUndo else onMark)
    }
}

@Composable
private fun SunriseRow(t: LocalTime) {
    Column {
        Divider()
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Quyosh chiqishi", style = Type.Caption.copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
            Text(Fmt.time(t), style = Type.Caption.copy(fontSize = 13.sp))
        }
    }
}
