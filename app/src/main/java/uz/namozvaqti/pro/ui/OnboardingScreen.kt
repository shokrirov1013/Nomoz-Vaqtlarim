package uz.namozvaqti.pro.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.namozvaqti.core.Gender
import uz.namozvaqti.core.UzbekCities
import uz.namozvaqti.pro.AppViewModel

/** Birinchi ishga tushirish: ism/jins -> joylashuv -> ruxsatlar. */
@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf(Gender.MALE.key) }
    var locMsg by remember { mutableStateOf<String?>(null) }
    var locationDone by rememberSaveable { mutableStateOf(false) }
    val resume = rememberResumeTick()

    val gpsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            locMsg = "Joylashuv aniqlanmoqda…"
            scope.launch {
                val label = vm.useGpsLocation()
                locMsg = label?.let { "Aniqlandi: $it" } ?: "Aniqlab bo'lmadi. Shaharni qo'lda tanlang."
                if (label != null) locationDone = true
            }
        } else locMsg = "Ruxsat berilmadi. Shaharni qo'lda tanlang."
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Namoz Vaqtlarim", style = Type.H1)
                Text("${step + 1} / 3", style = Type.Caption)
            }
            CrescentMosqueArt(Modifier.size(width = 96.dp, height = 84.dp))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (step) {
                0 -> GlassCard(Modifier.fillMaxWidth()) {
                    Text("Assalomu alaykum!", style = Type.H2)
                    Text("Sizga mos eslatmalar uchun ozgina ma'lumot kerak. Boshqa shaxsiy ma'lumot yig'ilmaydi.", style = Type.Caption)
                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, label = { Text("Ismingiz") }, singleLine = true,
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    Text("Jinsingiz", style = Type.Body, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassChip("Erkak", gender == Gender.MALE.key, { gender = Gender.MALE.key })
                        GlassChip("Ayol", gender == Gender.FEMALE.key, { gender = Gender.FEMALE.key })
                    }
                    Text("Juma kuni erkaklarga Peshin vaqtida “Juma vaqti bo'ldi” deb eslatiladi.", style = Type.Caption, modifier = Modifier.padding(top = 6.dp))
                }

                1 -> GlassCard(Modifier.fillMaxWidth()) {
                    Text("Joylashuv", style = Type.H2)
                    Text("Namoz vaqtlari joylashuvingizga qarab aniq olinadi.", style = Type.Caption)
                    Box(Modifier.height(10.dp))
                    GlassButton("GPS orqali aniqlash", {
                        gpsLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    }, Modifier.fillMaxWidth())
                    locMsg?.let { Text(it, style = Type.Caption.copy(color = Glass.PurpleDeep), modifier = Modifier.padding(top = 6.dp)) }
                    Text("Yoki shaharni qo'lda tanlang:", style = Type.Body, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                        UzbekCities.all.forEach { c ->
                            Text(
                                c.name, style = Type.Body,
                                modifier = Modifier.fillMaxWidth()
                                    .clickableNoRipple { vm.selectCity(c); locMsg = "Tanlandi: ${c.name}"; locationDone = true }
                                    .padding(vertical = 9.dp),
                            )
                        }
                    }
                }

                else -> GlassCard(Modifier.fillMaxWidth()) {
                    @Suppress("UNUSED_EXPRESSION") resume
                    Text("Ruxsatlar", style = Type.H2)
                    Text("Eslatmalar o'z vaqtida ishlashi uchun quyidagilar kerak.", style = Type.Caption)
                    Box(Modifier.height(8.dp))
                    OnboardPerm("Bildirishnomalar", "Namoz eslatmalarini ko'rsatish uchun", Perms.notifications(ctx)) {
                        if (Perms.needsNotificationRuntimePermission(ctx)) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else Perms.openNotificationSettings(ctx)
                    }
                    OnboardPerm("Aniq vaqtdagi budilnik", "Namoz vaqtida aynan o'sha daqiqada chalish uchun", Perms.exactAlarms(ctx)) {
                        Perms.openExactAlarmSettings(ctx)
                    }
                    OnboardPerm("Batareya cheklovisiz ishlash", "Telefon “uyqu” rejimida ham eslatma o'tkazib yuborilmasligi uchun (tavsiya)", Perms.batteryUnrestricted(ctx)) {
                        Perms.requestIgnoreBattery(ctx)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) GlassButton("Orqaga", { step-- }, Modifier.weight(1f), filled = false)
            GlassButton(
                if (step < 2) "Davom etish" else "Boshlash",
                {
                    when {
                        step < 2 -> step++
                        else -> vm.completeOnboarding(name, Gender.fromKey(gender))
                    }
                },
                Modifier.weight(1f),
                enabled = when (step) { 0 -> name.isNotBlank(); else -> true },
            )
        }
        if (step == 1 && !locationDone) {
            Text("Joylashuv tanlanmasa, Toshkent ishlatiladi; keyin Sozlamalardan o'zgartirasiz.", style = Type.Caption.copy(fontSize = 12.sp))
        }
    }
}

@Composable
private fun OnboardPerm(title: String, why: String, granted: Boolean, onGrant: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = Type.BodyBold)
            Text(why, style = Type.Caption)
        }
        if (granted) Text("✓", style = Type.H2.copy(color = Glass.PurpleDeep)) else GlassButton("Ruxsat", onGrant, small = true)
    }
}
