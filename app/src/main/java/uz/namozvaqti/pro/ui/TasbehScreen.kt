package uz.namozvaqti.pro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.namozvaqti.core.Tasbeh
import uz.namozvaqti.pro.data.TasbehData
import uz.namozvaqti.pro.data.TasbehStore
import java.time.LocalDate

/**
 * Tasbeh (zikr hisoblagichi): katta doirani bossangiz son oshadi, halqa maqsadgacha to'lib boradi, davra tugaganda
 * kuchliroq tebranadi. Har bir zikrning soni va maqsadi alohida saqlanadi; ilova yopilsa ham yo'qolmaydi.
 */
@Composable
fun TasbehScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val store = remember { TasbehStore(ctx.applicationContext) }
    var data by remember { mutableStateOf(store.load()) }
    val haptic = LocalHapticFeedback.current

    val dhikr = Tasbeh.byKey(data.dhikr)
    val counter = data.counter()
    val target = data.target()

    fun commit(next: TasbehData) {
        data = next
        store.save(next)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState(), flingBehavior = SoftFling).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // zikr tanlash
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tasbeh.dhikrs.forEach { d -> GlassChip(d.title, d.key == dhikr.key, { commit(data.copy(dhikr = d.key)) }) }
        }

        GlassCard(Modifier.fillMaxWidth(), tint = Glass.Tint) {
            if (dhikr.arabic.isNotEmpty()) {
                Text(
                    dhikr.arabic,
                    style = Type.H1.copy(fontSize = 30.sp, lineHeight = 46.sp, color = Glass.PinkSoft, textDirection = TextDirection.Rtl),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(dhikr.title, style = Type.H2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(dhikr.meaning, style = Type.Caption, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CounterDial(counter.count, target, counter.progress(target), counter.isClosed(target)) {
                val c = counter.tap(target)
                val date = LocalDate.now()
                commit(
                    data.copy(
                        counters = data.counters + (dhikr.key to c),
                        todayDate = date.toString(),
                        today = data.todayCount(date) + 1,
                    ),
                )
                if (data.haptic) haptic.performHapticFeedback(if (c.isClosed(target)) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Maqsad", style = Type.BodyBold)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tasbeh.targets.forEach { t ->
                    GlassChip(Tasbeh.targetLabel(t), t == target, {
                        if (t != target) {
                            // maqsad o'zgarsa, joriy davra nolga tushadi (davralar va jami saqlanadi)
                            val c = if (counter.count > 0) counter.copy(count = 0) else counter
                            commit(data.copy(targets = data.targets + (dhikr.key to t), counters = data.counters + (dhikr.key to c)))
                        }
                    })
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth()) {
                StatCell("Davralar", counter.rounds, Modifier.weight(1f))
                StatCell("Jami", counter.total, Modifier.weight(1f))
                StatCell("Bugun", data.todayCount(), Modifier.weight(1f))
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    "−1", { commit(data.copy(counters = data.counters + (dhikr.key to counter.undo(target)), today = (data.todayCount() - 1).coerceAtLeast(0), todayDate = LocalDate.now().toString())) },
                    Modifier.weight(1f), filled = false, small = true, enabled = counter.count > 0,
                )
                GlassButton(
                    "Nolga tushirish", { commit(data.copy(counters = data.counters + (dhikr.key to counter.reset()))) },
                    Modifier.weight(2f), filled = false, small = true, enabled = counter.count > 0 || counter.rounds > 0,
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tebranish", style = Type.BodyBold)
                    Text("Har bosishda yengil, davra tugaganda kuchliroq", style = Type.Caption.copy(fontSize = 12.sp))
                }
                GlassSwitch(data.haptic, { commit(data.copy(haptic = it)) })
            }
        }
        Text("Sanagich telefonni yopsangiz ham saqlanadi.", style = Type.Caption, modifier = Modifier.padding(horizontal = 4.dp))
        Box(Modifier.height(100.dp))
    }
}

@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = Type.H1.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Glass.PinkSoft))
        Text(label, style = Type.Caption.copy(fontSize = 12.sp))
    }
}

/** Katta sanash doirasi: tashqarida to'lib boruvchi halqa, ichida bosiladigan shisha doira va son. */
@Composable
private fun CounterDial(count: Int, target: Int, progress: Float, closed: Boolean, onTap: () -> Unit) {
    val anim by animateFloatAsState(progress, spring(dampingRatio = 0.8f, stiffness = 300f), label = "ring")
    Box(Modifier.size(244.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 10.dp.toPx()
            val topLeft = Offset(sw / 2f, sw / 2f)
            val arc = Size(size.width - sw, size.height - sw)
            drawArc(Glass.Lilac.copy(alpha = 0.16f), 0f, 360f, false, topLeft, arc, style = Stroke(sw))
            if (anim > 0.002f) drawArc(Glass.Pink, -90f, 360f * anim, false, topLeft, arc, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Box(
            Modifier.size(196.dp).pressable(scaleDown = 0.93f, onClick = onTap).glassSurface(CircleShape, Glass.Tint, 1.5f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    count.toString(),
                    style = Type.Big.copy(fontSize = 68.sp, fontWeight = FontWeight.ExtraBold, color = if (closed) Glass.PinkSoft else Glass.Ink),
                )
                Text(
                    when {
                        closed -> "Davra tugadi ✓"
                        target > 0 -> "/ $target"
                        else -> "bosing"
                    },
                    style = Type.Caption.copy(fontSize = 14.sp, color = if (closed) Glass.PinkSoft else Glass.InkSoft),
                )
            }
        }
    }
}
