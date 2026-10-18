package uz.namozvaqti.pro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private data class NextPrayer(val prayer: Prayer, val at: LocalDateTime)

private fun nextPrayerOf(now: LocalDateTime, today: LocalDate, t: PrayerTimes?, tomorrow: PrayerTimes?): NextPrayer? {
    if (t == null) return null
    Prayer.entries.forEach { p ->
        val at = LocalDateTime.of(today, t.of(p))
        if (at.isAfter(now)) return NextPrayer(p, at)
    }
    return tomorrow?.let { NextPrayer(Prayer.FAJR, LocalDateTime.of(today.plusDays(1), it.fajr)) }
}

/** Oldingi namoz vaqtidan keyingisigacha o'tgan ulush (0..1): suyuq shar va chiziq shu bilan to'lib boradi. */
private fun elapsedFraction(now: LocalDateTime, today: LocalDate, t: PrayerTimes?, next: NextPrayer?): Float {
    if (t == null || next == null) return 0f
    val prev = Prayer.entries.map { LocalDateTime.of(today, t.of(it)) }.lastOrNull { !it.isAfter(now) } ?: today.atStartOfDay()
    val total = Duration.between(prev, next.at).seconds.coerceAtLeast(1)
    return (Duration.between(prev, now).seconds.toFloat() / total).coerceIn(0f, 1f)
}

/**
 * Bosh sahifa. Orqa fonda (SkyBackdrop) haqiqiy quyosh/oy holati, foydalanuvchi jinsiga mos namoz silueti;
 * ustida shaffof "suyuq shisha" kartalar. Fon qimirlamaydi — faqat kartalar siljiydi.
 */
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
    val minute by remember { derivedStateOf { nowState.value.truncatedTo(ChronoUnit.MINUTES) } }
    val phase = remember(minute, s.lat, s.lon) { skyPhase(minute, s.lat, s.lon) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val sceneH = screenW * (SKY_SCENE_HEIGHT / 360f)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState(), flingBehavior = SoftFling)) {
            // ---- sahna: sarlavha va tezkor tugmalar (orqada osmon)
            Box(Modifier.fillMaxWidth().height(sceneH)) {
                Column(
                    Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 20.dp, top = 6.dp).widthIn(max = screenW * 0.62f),
                ) {
                    Text("Assalomu alaykum,", style = Type.Caption.copy(fontSize = 13.sp, color = Color(0xFFD9CCF2)))
                    Text(
                        s.name.ifBlank { "Do'stim" },
                        style = Type.H1.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = Glass.Pink, modifier = Modifier.size(15.dp))
                        Text(" ${s.locationLabel}", style = Type.Caption.copy(fontSize = 12.sp, color = Color(0xFFD9CCF2)))
                    }
                    Text(Fmt.gregorianFull(today), style = Type.Caption.copy(fontSize = 12.sp, color = Color(0xFFD9CCF2)), modifier = Modifier.padding(top = 1.dp))
                    Text(Fmt.hijri(today, s.hijriAdjust), style = Type.Caption.copy(fontSize = 12.sp, color = Color(0xFFEADFFF), fontWeight = FontWeight.Bold))
                }
                Column(
                    Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 18.dp, top = 6.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PhaseChip(phase.first, phase.second)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(TabIcons.Book, "Suralar", { go(Screen.QURAN) })
                        GlassIconButton(TabIcons.Crescent, "Tahajjud", { go(Screen.TAHAJJUD) }, tint = Glass.Lilac)
                    }
                }
            }

            // ---- shisha kartalar
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Warnings(go)
                NextPrayerCard(vm, next, times, today)

                val t = times
                if (t != null) {
                    Prayer.entries.forEachIndexed { i, p ->
                        val isNext = next?.prayer == p && next?.at?.toLocalDate() == today
                        val log = doneMap[p]
                        PrayerCapsule(
                            label = if (PrayerTexts.isFridayMale(p, today, s.gender)) "Peshin (Juma)" else p.title,
                            time = Fmt.time(t.of(p)),
                            caption = when {
                                log != null -> "Bajarildi · " + Fmt.time(LocalDateTime.ofInstant(Instant.ofEpochMilli(log.markedAt), ZoneId.systemDefault()).toLocalTime())
                                isNext -> "Keyingi"
                                else -> ""
                            },
                            isNext = isNext,
                            done = log != null,
                            level = if (isNext && log == null) elapsedFraction(nowState.value, today, t, next) else 0f,
                            delayMs = 60 + i * 55,
                            onMark = { vm.setDone(today, p, true) },
                            onUndo = { confirmUndo = p },
                        )
                        if (i == 0) SunriseLine(t.sunrise)
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
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Manba: ${it.source}$updated", style = Type.Caption.copy(fontSize = 11.sp), modifier = Modifier.weight(1f))
                        val refreshing by vm.refreshing.collectAsState()
                        GlassButton(if (refreshing) "Yangilanmoqda…" else "Yangilash", { vm.refreshTimes() }, small = true, filled = false, enabled = !refreshing)
                    }
                }
                Spacer(Modifier.height(100.dp))
            }
        }
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

/** "Tun", "Quyosh chiqdi", "Kunduz"... — osmon holati yozuvi. */
@Composable
private fun PhaseChip(label: String, day: Boolean) {
    Row(
        Modifier.glassSurface(RoundedCornerShape(50), Glass.Tint, 1.3f).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (day) Box(Modifier.size(9.dp).background(Color(0xFFFFD9A0), CircleShape))
        else Icon(TabIcons.Crescent, null, tint = Color(0xFFFFD0EA), modifier = Modifier.size(12.dp))
        Text("  $label", style = Type.BodyBold.copy(fontSize = 12.sp, color = Color.White))
    }
}

@Composable
private fun NextPrayerCard(vm: AppViewModel, next: NextPrayer?, times: PrayerTimes?, today: LocalDate) {
    val now by vm.now.collectAsState()
    val frac by animateFloatAsState(elapsedFraction(now, today, times, next), tween(900), label = "next-progress")
    GlassCard(Modifier.fillMaxWidth(), corner = 28.dp, tint = Glass.Tint, padding = 0.dp) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("KEYINGI NAMOZ", style = Type.Caption.copy(fontSize = 10.sp, color = Glass.Pink, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(next?.prayer?.title ?: "—", style = Type.H2.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold))
                    next?.let { Text("  ${Fmt.time(it.at.toLocalTime())}", style = Type.H2.copy(fontSize = 16.sp, color = Glass.PinkSoft)) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    next?.let { Fmt.countdown(Duration.between(now, it.at)) } ?: "--:--:--",
                    style = Type.Big.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, brush = CountBrush),
                )
                Text("qoldi", style = Type.Caption.copy(fontSize = 10.sp))
            }
        }
        // vaqt chizig'i: oldingi namozdan keyingisigacha
        Box(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp).fillMaxWidth().height(3.dp)
                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50)),
        ) {
            Box(
                Modifier.fillMaxWidth(frac.coerceIn(0.02f, 1f)).height(3.dp)
                    .background(Brush.horizontalGradient(listOf(Glass.Violet, Glass.Pink)), RoundedCornerShape(50)),
            )
        }
    }
}

private val CountBrush = Brush.horizontalGradient(listOf(Color(0xFFFFD6EC), Color(0xFFC9B6FF)))

/** Bitta namoz: shaffof kapsula, o'ngda belgilash sharchasi (bosilsa suyuqlik to'lib ✓ chiqadi). */
@Composable
private fun PrayerCapsule(
    label: String,
    time: String,
    caption: String,
    isNext: Boolean,
    done: Boolean,
    level: Float,
    delayMs: Int,
    onMark: () -> Unit,
    onUndo: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .appear(delayMs)
            .glassSurface(shape, if (isNext) Glass.Pink else Color.White, if (isNext) 2.1f else 1f)
            .padding(start = 20.dp, end = 9.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Type.H2.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (done || isNext) Glass.Ink else Color(0xFFD3C8EC)))
            if (caption.isNotEmpty()) Text(caption, style = Type.Caption.copy(fontSize = 10.sp, color = if (done) Glass.PinkSoft else Color(0xFFD9C8FF), fontWeight = FontWeight.Bold))
        }
        Text(time, style = Type.H2.copy(fontSize = 16.sp, color = if (isNext) Color.White else Glass.PinkSoft), modifier = Modifier.padding(end = 12.dp))
        GlassOrb(done, size = 36.dp, level = level, active = isNext && !done, onClick = if (done) onUndo else onMark)
    }
}

@Composable
private fun SunriseLine(t: LocalTime) {
    Row(Modifier.fillMaxWidth().appear(110), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(Color(0xFFFFD9A0), CircleShape))
        Text("  Quyosh chiqishi  ${Fmt.time(t)}", style = Type.Caption.copy(fontSize = 11.sp))
    }
}

@Composable
private fun Warnings(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    rememberResumeTick()
    val problems = buildList {
        if (!Perms.notifications(ctx)) add("Bildirishnomalar o'chiq — eslatmalar ko'rinmaydi")
        if (!Perms.exactAlarms(ctx)) add("Aniq budilnik ruxsati yo'q — eslatmalar kechikishi mumkin")
        if (!Perms.overlay(ctx)) add("“Boshqa ilovalar ustida ko'rsatish” ruxsati yo'q — eslatma boshqa ilova ustida chiqmaydi")
        if (!Perms.fullScreen(ctx)) add("To'liq ekranli bildirishnoma ruxsati yo'q — qulflangan ekranda eslatma chiqmaydi")
    }
    if (problems.isEmpty()) return
    GlassCard(Modifier.fillMaxWidth(), tint = Glass.Danger, onClick = { go(Screen.SETTINGS) }) {
        problems.forEach { Text("• $it", style = Type.Caption.copy(color = Glass.Danger, fontSize = 13.sp)) }
        Text("Sozlamalar → Ruxsatlar", style = Type.BodyBold.copy(color = Glass.PinkSoft, fontSize = 13.sp))
    }
}

// ---- boshqa ekranlar ham ishlatadigan umumiy bo'laklar

private val DoneBrush = Brush.linearGradient(listOf(Glass.Violet.copy(alpha = 0.32f), Glass.Pink.copy(alpha = 0.22f)))

@Composable
fun DoneChip(onClick: () -> Unit) {
    Row(
        Modifier
            .pressable(scaleDown = 0.94f, onClick = onClick)
            .background(DoneBrush, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassTick(true, size = 20.dp)
        Text("  Bajarildi", style = Type.BodyBold.copy(fontSize = 14.sp, color = Glass.PinkSoft))
    }
}

private val TrackColor = Glass.Lilac.copy(alpha = 0.14f)
private val FillBrush = Brush.horizontalGradient(listOf(Glass.Violet, Glass.Pink))

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "progress")
    Box(modifier.fillMaxWidth().height(10.dp).background(TrackColor, RoundedCornerShape(50))) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .height(10.dp)
                .background(FillBrush, RoundedCornerShape(50)),
        )
    }
}
