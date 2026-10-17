package uz.namozvaqti.pro.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.namozvaqti.core.Night
import uz.namozvaqti.core.TahajjudRules
import uz.namozvaqti.pro.AppViewModel
import uz.namozvaqti.pro.alarm.RingKind
import uz.namozvaqti.pro.alarm.RingService
import uz.namozvaqti.pro.data.TahajjudDay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun TahajjudScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val days by vm.tahajjud.collectAsState()
    val now by vm.now.collectAsState()
    val today = now.toLocalDate()
    var soundDialog by remember { mutableStateOf(false) }

    // Tunning oxirgi uchdan biri (Shomdan ertangi Bomdodgacha)
    var lastThird by remember { mutableStateOf<LocalDateTime?>(null) }
    LaunchedEffect(today, s.lat, s.lon, s.method, s.hanafiAsr) {
        lastThird = Night.lastThirdStart(vm.timesFor(today), vm.timesFor(today.plusDays(1)).fajr)
    }

    val enabledDays = days.filter { it.enabled }
    val next = if (s.tahajjudEnabled)
        TahajjudRules.nextOccurrence(now, enabledDays.associate { it.dayOfWeek to LocalTime.of(it.hour, it.minute) }) else null

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Tahajjud", style = Type.H1, modifier = Modifier.padding(top = 4.dp))

        GlassCard(Modifier.fillMaxWidth(), tint = Glass.PinkSoft) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tahajjud eslatmasi", style = Type.H2)
                    Text(
                        if (!s.tahajjudEnabled) "O'chiq" else if (next != null)
                            "Keyingisi: ${Fmt.weekday(next.dayOfWeek)}, ${Fmt.time(next.toLocalTime())}"
                        else "Kamida bitta kunni tanlang",
                        style = Type.Caption.copy(fontSize = 14.sp),
                    )
                }
                Switch(checked = s.tahajjudEnabled, onCheckedChange = { on ->
                    vm.updateSettings(reschedule = false) { it.copy(tahajjudEnabled = on) }
                    vm.rescheduleTahajjud()
                })
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Haftalik jadval", style = Type.H2)
            Text("Kerakli kunlarni belgilang va har bir kun uchun uyg'onish vaqtini tanlang.", style = Type.Caption)
            Box(Modifier.height(8.dp))
            days.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassChip(Fmt.weekdays[d.dayOfWeek - 1], d.enabled, { vm.setTahajjudDay(d.copy(enabled = !d.enabled)) }, Modifier.weight(1f))
                    Box(Modifier.padding(start = 10.dp))
                    GlassButton(
                        "%02d:%02d".format(d.hour, d.minute),
                        {
                            TimePickerDialog(ctx, { _, h, m -> vm.setTahajjudDay(d.copy(hour = h, minute = m)) }, d.hour, d.minute, true).show()
                        },
                        small = true, filled = false,
                    )
                }
            }
            Box(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("Barcha kunlar", { vm.setTahajjudDays(days.map { it.copy(enabled = true) }) }, small = true, filled = false)
                GlassButton("Tozalash", { vm.setTahajjudDays(days.map { it.copy(enabled = false) }) }, small = true, filled = false)
            }
            if (enabledDays.size > 1) {
                Box(Modifier.height(8.dp))
                GlassButton("Tanlangan kunlarga bir xil vaqt", {
                    val first = enabledDays.first()
                    TimePickerDialog(ctx, { _, h, m ->
                        vm.setTahajjudDays(days.map { if (it.enabled) it.copy(hour = h, minute = m) else it })
                    }, first.hour, first.minute, true).show()
                }, small = true, filled = false)
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Tunning oxirgi uchdan biri", style = Type.H2)
            val lt = lastThird
            if (lt != null) {
                Text(
                    "Boshlanishi: ${Fmt.time(lt.toLocalTime())} (${if (lt.toLocalDate() == today) "bugun" else "ertaga"})",
                    style = Type.Body, modifier = Modifier.padding(top = 4.dp),
                )
                Text("Shomdan ertangi Bomdodgacha bo'lgan tunning oxirgi 1/3 qismi — Tahajjud uchun eng fazilatli vaqt.", style = Type.Caption)
                Box(Modifier.height(8.dp))
                GlassButton("Shu vaqtni tanlangan kunlarga qo'llash", {
                    val t = lt.toLocalTime()
                    vm.setTahajjudDays(days.map { if (it.enabled) it.copy(hour = t.hour, minute = t.minute) else it })
                }, small = true, enabled = enabledDays.isNotEmpty())
            } else {
                Text("Hisoblanmoqda…", style = Type.Caption)
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Ovoz va vibratsiya", style = Type.H2)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ovoz", style = Type.Body)
                    Text(Sounds.titleOf(ctx, s.tahajjudSound), style = Type.Caption)
                }
                GlassButton("Tanlash", { soundDialog = true }, small = true, filled = false)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Vibratsiya", style = Type.Body, modifier = Modifier.weight(1f))
                Switch(checked = s.tahajjudVibration, onCheckedChange = { on -> vm.updateSettings(false) { it.copy(tahajjudVibration = on) } })
            }
            Box(Modifier.height(8.dp))
            GlassButton("Budilnikni sinab ko'rish", { RingService.start(ctx, RingKind.TAHAJJUD) }, small = true, filled = false)
        }
        Box(Modifier.height(96.dp))
    }

    if (soundDialog) SoundPickerDialog(ctx, s.tahajjudSound, { id ->
        vm.updateSettings(false) { it.copy(tahajjudSound = id) }; soundDialog = false
    }, { soundDialog = false })
}
