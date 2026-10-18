package uz.namozvaqti.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.namozvaqti.core.HijriCalendar
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.pro.AppViewModel
import java.time.LocalDate
import java.time.YearMonth

private data class Cell(val date: LocalDate, val primary: Int, val secondary: Int)

@Composable
fun CalendarScreen(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    val today by vm.today.collectAsState()
    var hijriMode by rememberSaveable { mutableStateOf(false) }
    var selectedStr by rememberSaveable { mutableStateOf(today.toString()) }
    val selected = LocalDate.parse(selectedStr)

    // Ko'rsatilayotgan oy: milodiy (yil, oy) yoki hijriy (yil, oy)
    var gy by rememberSaveable { mutableIntStateOf(today.year) }
    var gm by rememberSaveable { mutableIntStateOf(today.monthValue) }
    val todayH = HijriCalendar.fromGregorian(today, s.hijriAdjust)
    var hy by rememberSaveable { mutableIntStateOf(todayH.year) }
    var hm by rememberSaveable { mutableIntStateOf(todayH.month) }

    val cells: List<Cell> = if (!hijriMode) {
        val ym = YearMonth.of(gy, gm)
        (1..ym.lengthOfMonth()).map { d ->
            val date = ym.atDay(d)
            Cell(date, d, HijriCalendar.fromGregorian(date, s.hijriAdjust).day)
        }
    } else {
        HijriCalendar.monthDays(hy, hm, s.hijriAdjust).map { (h, g) -> Cell(g, h.day, g.dayOfMonth) }
    }
    val offset = cells.first().date.dayOfWeek.value - 1
    val from = cells.first().date
    val to = cells.last().date

    val doneSet by remember(from, to) { vm.doneSet(from, to) }.collectAsState(emptySet())
    val notes by remember(from, to) { vm.notes(from, to) }.collectAsState(emptyList())
    val doneByDate = doneSet.groupBy({ it.first }, { it.second })
    val notesByDate = notes.groupBy { it.date }

    Column(
        Modifier.verticalScroll(rememberScrollState(), flingBehavior = SoftFling).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Taqvim", style = Type.H1, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassChip("Milodiy", !hijriMode, { hijriMode = false })
            GlassChip("Hijriy", hijriMode, { hijriMode = true })
        }

        GlassCard(Modifier.fillMaxWidth()) {
            // oy sarlavhasi
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clickableNoRipple {
                    if (!hijriMode) { val p = YearMonth.of(gy, gm).minusMonths(1); gy = p.year; gm = p.monthValue }
                    else { val (y, m) = HijriCalendar.prevMonth(hy, hm); hy = y; hm = m }
                }.padding(8.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Oldingi oy", tint = Glass.PinkSoft) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!hijriMode) {
                        Text("${Fmt.months[gm - 1]} $gy", style = Type.H2)
                        Text(hijriRangeLabel(cells, s.hijriAdjust), style = Type.Caption.copy(color = Glass.PinkSoft))
                    } else {
                        Text("${HijriCalendar.monthNames[hm - 1]} $hy", style = Type.H2)
                        Text(gregorianRangeLabel(cells), style = Type.Caption)
                    }
                }
                Box(Modifier.clickableNoRipple {
                    if (!hijriMode) { val p = YearMonth.of(gy, gm).plusMonths(1); gy = p.year; gm = p.monthValue }
                    else { val (y, m) = HijriCalendar.nextMonth(hy, hm); hy = y; hm = m }
                }.padding(8.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Keyingi oy", tint = Glass.PinkSoft) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Fmt.weekdaysShort.forEach {
                    Text(it, style = Type.Caption.copy(fontSize = 12.sp), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            val total = offset + cells.size
            val rows = (total + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    for (c in 0..6) {
                        val idx = r * 7 + c - offset
                        if (idx !in cells.indices) {
                            Box(Modifier.weight(1f).aspectRatio(0.82f))
                        } else {
                            val cell = cells[idx]
                            DayCell(
                                cell, hijriMode,
                                isToday = cell.date == today, isSelected = cell.date == selected,
                                doneCount = doneByDate[cell.date]?.size ?: 0,
                                hasNote = notesByDate.containsKey(cell.date.toString()),
                                modifier = Modifier.weight(1f),
                            ) { selectedStr = cell.date.toString() }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("Bugun", {
                    selectedStr = today.toString(); gy = today.year; gm = today.monthValue
                    hy = todayH.year; hm = todayH.month
                }, small = true, filled = false)
            }
            if (s.hijriAdjust != 0) {
                Text("Hijriy sana tuzatishi: ${if (s.hijriAdjust > 0) "+" else ""}${s.hijriAdjust} kun (Sozlamalar)", style = Type.Caption, modifier = Modifier.padding(top = 6.dp))
            }
        }

        DayDetails(vm, selected, s.hijriAdjust, doneByDate[selected].orEmpty(), notes.filter { it.date == selected.toString() })
        Box(Modifier.height(96.dp))
    }
}

private fun hijriRangeLabel(cells: List<Cell>, adjust: Int): String {
    val a = HijriCalendar.fromGregorian(cells.first().date, adjust)
    val b = HijriCalendar.fromGregorian(cells.last().date, adjust)
    return if (a.month == b.month) "${a.monthName} ${a.year}" else "${a.monthName} — ${b.monthName} ${b.year}"
}

private fun gregorianRangeLabel(cells: List<Cell>): String {
    val a = cells.first().date; val b = cells.last().date
    return if (a.month == b.month) "${Fmt.months[a.monthValue - 1]} ${a.year}" else "${Fmt.months[a.monthValue - 1]} — ${Fmt.months[b.monthValue - 1]} ${b.year}"
}

private val CellShape = RoundedCornerShape(14.dp)
private val SelectedBrush = Brush.linearGradient(listOf(Glass.PinkSoft, Glass.Magenta))

@Composable
private fun DayCell(
    cell: Cell, hijriMode: Boolean, isToday: Boolean, isSelected: Boolean,
    doneCount: Int, hasNote: Boolean, modifier: Modifier, onClick: () -> Unit,
) {
    val bg = when {
        isSelected -> Modifier.background(SelectedBrush, CellShape)
        isToday -> Modifier.glassSurface(CellShape, Glass.Tint)
        else -> Modifier.background(Color.White.copy(alpha = 0.045f), CellShape)
    }
    Column(
        modifier.aspectRatio(0.82f).padding(2.dp).then(bg).clickableNoRipple(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val fg = if (isSelected) Glass.OnPink else Glass.Ink
        val sub = if (isSelected) Glass.OnPink.copy(alpha = 0.75f) else Glass.PinkSoft.copy(alpha = 0.7f)
        Text("${cell.primary}", style = Type.BodyBold.copy(fontSize = 16.sp, color = fg))
        Text("${cell.secondary}", style = Type.Caption.copy(fontSize = 10.sp, color = sub))
        Row(Modifier.height(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (doneCount > 0) Text("$doneCount/5", style = Type.Caption.copy(fontSize = 9.sp, color = if (isSelected) Glass.OnPink else Glass.Violet))
            if (hasNote) Box(Modifier.padding(start = 2.dp).size(5.dp).background(if (isSelected) Glass.OnPink else Glass.Pink, RoundedCornerShape(50)))
        }
    }
}

@Composable
private fun DayDetails(
    vm: AppViewModel, date: LocalDate, adjust: Int, done: List<Prayer>, notes: List<uz.namozvaqti.pro.data.Note>,
) {
    var text by remember(date) { mutableStateOf("") }
    GlassCard(Modifier.fillMaxWidth()) {
        Text(Fmt.gregorianFull(date), style = Type.H2)
        Text(Fmt.hijri(date, adjust), style = Type.Caption.copy(color = Glass.PinkSoft, fontSize = 14.sp))
        Box(Modifier.height(8.dp))
        Text("Namozlar", style = Type.BodyBold)
        Prayer.entries.forEach { p ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.title, style = Type.Body, modifier = Modifier.weight(1f))
                GlassTick(p in done, size = 22.dp)
                Text(
                    if (p in done) "  Bajarildi" else "  Belgilanmagan",
                    style = Type.Caption.copy(color = if (p in done) Glass.PinkSoft else Glass.InkSoft, fontSize = 14.sp),
                )
            }
        }
        Box(Modifier.height(10.dp))
        Text("Shaxsiy eslatmalar", style = Type.BodyBold)
        if (notes.isEmpty()) Text("Bu kun uchun eslatma yo'q.", style = Type.Caption, modifier = Modifier.padding(vertical = 4.dp))
        notes.forEach { n ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("• ${n.text}", style = Type.Body, modifier = Modifier.weight(1f))
                Box(Modifier.clickableNoRipple { vm.deleteNote(n.id) }.padding(6.dp)) {
                    Icon(Icons.Default.Delete, "O'chirish", tint = Glass.Danger, modifier = Modifier.size(20.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Yangi eslatma…") }, singleLine = true, shape = RoundedCornerShape(16.dp),
            )
            Box(Modifier.padding(start = 8.dp))
            GlassButton("Qo'shish", { vm.addNote(date, text); text = "" }, small = true, icon = Icons.Default.Add, enabled = text.isNotBlank())
        }
    }
}
