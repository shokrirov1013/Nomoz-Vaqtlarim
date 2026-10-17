package uz.namozvaqti.pro.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.namozvaqti.core.CalcMethod
import uz.namozvaqti.core.Gender
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.PrayerTexts
import uz.namozvaqti.core.UzbekCities
import uz.namozvaqti.pro.AppViewModel
import uz.namozvaqti.pro.BuildConfig
import uz.namozvaqti.pro.alarm.RingKind
import uz.namozvaqti.pro.alarm.RingService
import uz.namozvaqti.pro.alarm.SpeechService
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AppViewModel, go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val resume = rememberResumeTick()
    val scope = rememberCoroutineScope()
    var gpsMessage by remember { mutableStateOf<String?>(null) }
    var cityDialog by remember { mutableStateOf(false) }
    var fajrSoundDialog by remember { mutableStateOf(false) }

    val gpsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            gpsMessage = "Joylashuv aniqlanmoqda…"
            scope.launch { gpsMessage = vm.useGpsLocation()?.let { "Aniqlandi: $it" } ?: "Joylashuvni aniqlab bo'lmadi. GPS yoqilganini tekshiring." }
        } else gpsMessage = "Joylashuv ruxsati berilmadi. Shaharni qo'lda tanlang."
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sozlamalar", style = Type.H1, modifier = Modifier.padding(top = 4.dp))

        // ---------------------------------------------------------------- profil
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Profil", style = Type.H2)
            OutlinedTextField(
                value = s.name, onValueChange = { v -> vm.updateSettings(false) { it.copy(name = v) } },
                label = { Text("Ismingiz") }, singleLine = true, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("Erkak", s.gender == Gender.MALE, { vm.updateSettings { it.copy(gender = Gender.MALE) } })
                GlassChip("Ayol", s.gender == Gender.FEMALE, { vm.updateSettings { it.copy(gender = Gender.FEMALE) } })
            }
            Text("Jins Juma kuni Peshin eslatmasi matnini belgilaydi.", style = Type.Caption, modifier = Modifier.padding(top = 4.dp))
        }

        // ---------------------------------------------------------------- joylashuv
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Joylashuv", style = Type.H2)
            Text(
                "${s.locationLabel}  (${if (s.useGps) "GPS" else "qo'lda"}) · %.3f, %.3f".format(java.util.Locale.US, s.lat, s.lon),
                style = Type.Caption.copy(fontSize = 14.sp), modifier = Modifier.padding(vertical = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("GPS orqali aniqlash", {
                    gpsLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                }, small = true, modifier = Modifier.weight(1f))
                GlassButton("Shaharni tanlash", { cityDialog = true }, small = true, filled = false, modifier = Modifier.weight(1f))
            }
            gpsMessage?.let { Text(it, style = Type.Caption.copy(color = Glass.PurpleDeep), modifier = Modifier.padding(top = 6.dp)) }
        }

        // ---------------------------------------------------------------- hisoblash
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Namoz vaqtini hisoblash", style = Type.H2)
            DropdownRow(
                "Usul", s.method.label, CalcMethod.entries.map { it.key to it.label },
            ) { key -> vm.updateSettings(false) { it.copy(method = CalcMethod.fromKey(key)) }; vm.refreshTimes() }
            Text("Asr hisoblash qoidasi", style = Type.Body, modifier = Modifier.padding(top = 10.dp))
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("Hanafiy (O'zbekiston)", s.hanafiAsr, { vm.updateSettings(false) { it.copy(hanafiAsr = true) }; vm.refreshTimes() })
                GlassChip("Shofe'iy", !s.hanafiAsr, { vm.updateSettings(false) { it.copy(hanafiAsr = false) }; vm.refreshTimes() })
            }
            StepperRow("Hijriy sanani moslash", s.hijriAdjust, " kun", -2, 2) { v -> vm.updateSettings(false) { it.copy(hijriAdjust = v) } }
            Text(
                "Vaqtlar islom.uz jadvalidan (eng yaqin shahar + joylashuvingizga moslab) olinadi. Bu faqat “O'zbekiston” usuli va Hanafiy Asr " +
                    "tanlanganda ishlaydi; boshqa holatda Aladhan yoki qurilmada hisoblash ishlatiladi.",
                style = Type.Caption, modifier = Modifier.padding(top = 6.dp),
            )
        }

        // ---------------------------------------------------------------- eslatmalar
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Namoz eslatmalari", style = Type.H2)
            Prayer.entries.forEach { p ->
                SwitchRow(p.title, null, s.prayerReminders[p] ?: true) { on ->
                    vm.updateSettings { it.copy(prayerReminders = it.prayerReminders + (p to on)) }
                }
            }
            Divider(Modifier.padding(vertical = 6.dp))
            SwitchRow("Oldindan eslatma", "Namozdan ${s.preMinutes} daqiqa oldin: ting + “iltimos, o'qib oling”", s.preEnabled) { on ->
                vm.updateSettings { it.copy(preEnabled = on) }
            }
            if (s.preEnabled) StepperRow("Necha daqiqa oldin", s.preMinutes, " daq.", 1, 30) { v -> vm.updateSettings { it.copy(preMinutes = v) } }
            Divider(Modifier.padding(vertical = 6.dp))
            SwitchRow("Bajarilmagan namozni qayta eslatish", "Belgilanmasa — ${s.followupIntervalMinutes} daqiqada bir, ${s.followupCount} marta", s.followupEnabled) { on ->
                vm.updateSettings { it.copy(followupEnabled = on) }
            }
            if (s.followupEnabled) {
                StepperRow("Qayta eslatish oralig'i", s.followupIntervalMinutes, " daq.", 1, 60) { v -> vm.updateSettings { it.copy(followupIntervalMinutes = v) } }
                StepperRow("Qayta eslatish soni", s.followupCount, " marta", 1, 6) { v -> vm.updateSettings { it.copy(followupCount = v) } }
            }
            Divider(Modifier.padding(vertical = 6.dp))
            SwitchRow("Sokin rejimda ham ovoz chiqarish", "Ovoz budilnik kanalidan chaladi", s.playInSilentMode) { on ->
                vm.updateSettings(false) { it.copy(playInSilentMode = on) }
            }
            DropdownRow(
                "Ovozli xabar tili", ttsLabel(s.ttsLang),
                listOf("auto" to "Avtomatik (o'zbek → turk → rus)", "uz" to "O'zbekcha", "tr" to "Turkcha (muqobil)", "ru" to "Ruscha (muqobil)", "en" to "Inglizcha (muqobil)"),
            ) { v -> vm.updateSettings(false) { it.copy(ttsLang = v) } }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton("Ovozni sinash", {
                    SpeechService.speak(ctx, PrayerTexts.mainText(Prayer.DHUHR, LocalDate.of(2026, 1, 5), s.gender))
                }, small = true, filled = false, modifier = Modifier.weight(1f))
                GlassButton("Ovoz ma'lumoti", { Perms.openTtsSettings(ctx) }, small = true, filled = false, modifier = Modifier.weight(1f))
            }
            Text(
                "O'zbekcha ovoz chiqmasa: Google “Speech Services”da o'zbek tilini yuklab oling yoki yuqoridan muqobil til tanlang.",
                style = Type.Caption, modifier = Modifier.padding(top = 6.dp),
            )
        }

        // ---------------------------------------------------------------- Bomdod budilnigi
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Bomdod budilnigi", style = Type.H2)
            SwitchRow("Budilnik", "Bomdod vaqtida ovoz + vibratsiya bilan uyg'otadi", s.fajrAlarmEnabled) { on ->
                vm.updateSettings { it.copy(fajrAlarmEnabled = on) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ovoz", style = Type.Body)
                    Text(Sounds.titleOf(ctx, s.fajrSound), style = Type.Caption)
                }
                GlassButton("Tanlash", { fajrSoundDialog = true }, small = true, filled = false)
            }
            SwitchRow("Vibratsiya", null, s.fajrVibration) { on -> vm.updateSettings(false) { it.copy(fajrVibration = on) } }
            StepperRow("Keyinga surish", s.snoozeMinutes, " daq.", 1, 30) { v -> vm.updateSettings(false) { it.copy(snoozeMinutes = v) } }
            Box(Modifier.height(6.dp))
            GlassButton("Budilnikni sinab ko'rish", { RingService.start(ctx, RingKind.FAJR) }, small = true, filled = false)
        }

        GlassCard(Modifier.fillMaxWidth(), onClick = { go(Screen.TAHAJJUD) }) {
            Text("Tahajjud jadvali", style = Type.H2)
            Text(if (s.tahajjudEnabled) "Yoqilgan — o'zgartirish uchun bosing" else "O'chiq — sozlash uchun bosing", style = Type.Caption)
        }

        // ---------------------------------------------------------------- ruxsatlar
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Ruxsatlar", style = Type.H2)
            @Suppress("UNUSED_EXPRESSION") resume
            PermRow("Bildirishnomalar", Perms.notifications(ctx)) {
                if (Perms.needsNotificationRuntimePermission(ctx)) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else Perms.openNotificationSettings(ctx)
            }
            PermRow("Aniq vaqtdagi budilnik", Perms.exactAlarms(ctx)) { Perms.openExactAlarmSettings(ctx) }
            PermRow("Joylashuv (GPS)", Perms.location(ctx)) {
                gpsLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }
            PermRow("To'liq ekran budilnigi", Perms.fullScreen(ctx)) { Perms.openFullScreenSettings(ctx) }
            PermRow("Batareya cheklovisiz ishlash", Perms.batteryUnrestricted(ctx)) { Perms.requestIgnoreBattery(ctx) }
        }

        // ---------------------------------------------------------------- yangilanish
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Ilova va yangilanish", style = Type.H2)
            Text("Versiya ${BuildConfig.VERSION_NAME}", style = Type.Body, modifier = Modifier.padding(top = 4.dp))
            SwitchRow("Yangilanishni avtomatik tekshirish", "GitHub Releases orqali, har 12 soatda", s.autoUpdateCheck) { on ->
                vm.updateSettings(false) { it.copy(autoUpdateCheck = on) }
            }
            SwitchRow("Wi-Fi'da oldindan yuklab qo'yish", "O'rnatish faqat sizning tasdig'ingiz bilan", s.autoDownloadUpdate) { on ->
                vm.updateSettings(false) { it.copy(autoDownloadUpdate = on) }
            }
            Box(Modifier.height(6.dp))
            GlassButton("Yangilanish sahifasi", { go(Screen.UPDATE) }, small = true, filled = false)
            Text("Dizayn: Oq-Pushti-Siyohrang Liquid Glass (doimiy).", style = Type.Caption, modifier = Modifier.padding(top = 8.dp))
        }

        // ---------------------------------------------------------------- ilova haqida
        Text(
            "Shokirov tomonidan yaratilgan. 18 yoshli talaba ishlab chiqqan — o'z istaklari yo'lida qilgan ilova.",
            style = Type.Caption.copy(fontSize = 12.sp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Box(Modifier.height(96.dp))
    }

    if (cityDialog) {
        AlertDialog(
            onDismissRequest = { cityDialog = false },
            title = { Text("Shaharni tanlang") },
            text = {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    UzbekCities.all.forEach { c ->
                        Text(
                            (if (!s.useGps && s.manualCity == c.name) "● " else "○ ") + c.name,
                            style = Type.Body,
                            modifier = Modifier.fillMaxWidth().clickableNoRipple { vm.selectCity(c); cityDialog = false; gpsMessage = null }.padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { cityDialog = false }) { Text("Yopish") } },
        )
    }
    if (fajrSoundDialog) SoundPickerDialog(ctx, s.fajrSound, { id ->
        vm.updateSettings(false) { it.copy(fajrSound = id) }; fajrSoundDialog = false
    }, { fajrSoundDialog = false })
}

private fun ttsLabel(code: String) = when (code) {
    "uz" -> "O'zbekcha"; "tr" -> "Turkcha (muqobil)"; "ru" -> "Ruscha (muqobil)"; "en" -> "Inglizcha (muqobil)"
    else -> "Avtomatik (o'zbek → turk → rus)"
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = Type.Body)
            if (subtitle != null) Text(subtitle, style = Type.Caption)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun StepperRow(title: String, value: Int, suffix: String, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Type.Body, modifier = Modifier.weight(1f))
        GlassButton("−", { if (value > min) onChange(value - 1) }, small = true, filled = false, enabled = value > min)
        Text("$value$suffix", style = Type.BodyBold, modifier = Modifier.padding(horizontal = 10.dp))
        GlassButton("+", { if (value < max) onChange(value + 1) }, small = true, filled = false, enabled = value < max)
    }
}

@Composable
private fun DropdownRow(title: String, current: String, options: List<Pair<String, String>>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(title, style = Type.Body)
        Box {
            GlassButton(current, { open = true }, small = true, filled = false, modifier = Modifier.padding(top = 6.dp).fillMaxWidth())
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (key, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { open = false; onPick(key) })
                }
            }
        }
    }
}

@Composable
private fun PermRow(title: String, granted: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.Body)
            Text(if (granted) "Ruxsat berilgan" else "Ruxsat yo'q", style = Type.Caption.copy(color = if (granted) Glass.PurpleDeep else Glass.Danger))
        }
        if (!granted) GlassButton("Yoqish", onFix, small = true)
    }
}
