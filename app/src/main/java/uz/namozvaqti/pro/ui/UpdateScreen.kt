package uz.namozvaqti.pro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import uz.namozvaqti.pro.AppViewModel
import uz.namozvaqti.pro.BuildConfig
import uz.namozvaqti.pro.UpdateUi
import uz.namozvaqti.pro.update.UpdateManager

@Composable
fun UpdateScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val state by vm.update.collectAsState()
    resumeTickForInstallPermission()
    LaunchedEffect(Unit) { if (UpdateManager.repoConfigured()) vm.checkUpdate() }

    Column(
        Modifier.verticalScroll(rememberScrollState(), flingBehavior = SoftFling).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Yangilanish", style = Type.H1, modifier = Modifier.padding(top = 4.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Joriy versiya", style = Type.Caption)
            Text(BuildConfig.VERSION_NAME, style = Type.Big.copy(color = Glass.GoldBright))
            Text("GitHub: ${BuildConfig.GITHUB_REPO}", style = Type.Caption)
        }

        when (val st = state) {
            UpdateUi.Idle -> {
                if (!UpdateManager.repoConfigured()) {
                    GlassCard(Modifier.fillMaxWidth(), tint = Glass.Tint) {
                        Text("GitHub repository hali sozlanmagan. README’dagi “GitHub repository nomi” bosqichiga qarang.", style = Type.Body)
                    }
                } else GlassButton("Yangilanishni tekshirish", { vm.checkUpdate() }, Modifier.fillMaxWidth())
            }
            UpdateUi.Checking -> GlassCard(Modifier.fillMaxWidth()) { Text("Tekshirilmoqda…", style = Type.Body) }
            UpdateUi.UpToDate -> {
                GlassCard(Modifier.fillMaxWidth()) { Text("Sizda eng so'nggi versiya o'rnatilgan.", style = Type.BodyBold) }
                GlassButton("Qayta tekshirish", { vm.checkUpdate() }, Modifier.fillMaxWidth(), filled = false)
            }
            is UpdateUi.Error -> {
                GlassCard(Modifier.fillMaxWidth(), tint = Glass.Tint) { Text(st.message, style = Type.Body.copy(color = Glass.Danger)) }
                GlassButton("Qayta urinish", { vm.checkUpdate() }, Modifier.fillMaxWidth(), filled = false)
            }
            is UpdateUi.Available -> {
                GlassCard(Modifier.fillMaxWidth(), tint = Glass.Tint) {
                    Text("Yangi versiya mavjud", style = Type.Caption)
                    Text(st.release.version, style = Type.Big.copy(color = Glass.GoldBright))
                    if (st.release.sizeBytes > 0) Text("Hajmi: %.1f MB".format(st.release.sizeBytes / 1_048_576.0), style = Type.Caption)
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("O'zgarishlar", style = Type.H2)
                    Text(st.release.notes, style = Type.Body, modifier = Modifier.padding(top = 4.dp))
                }
                if (!UpdateManager.canInstall(ctx)) {
                    Text(
                        "O'rnatish uchun ushbu ilovaga “noma'lum manbalardan o'rnatish” ruxsatini bering.",
                        style = Type.Caption.copy(color = Glass.Danger),
                    )
                    GlassButton("Ruxsat berish", { UpdateManager.openInstallPermissionSettings(ctx) }, Modifier.fillMaxWidth(), filled = false)
                }
                GlassButton(
                    "Yuklab olish va o'rnatish", { vm.downloadAndInstall(st.release) }, Modifier.fillMaxWidth(),
                    enabled = UpdateManager.canInstall(ctx),
                )
                Text(
                    "Yangilanish Android o'rnatish oynasi orqali, sizning roziligingiz bilan o'rnatiladi. " +
                        "Namoz tarixi va sozlamalar saqlanib qoladi.",
                    style = Type.Caption,
                )
            }
            is UpdateUi.Downloading -> GlassCard(Modifier.fillMaxWidth()) {
                Text("Yuklab olinmoqda… ${(st.progress * 100).toInt()}%", style = Type.BodyBold)
                Box(Modifier.height(8.dp))
                ProgressBar(st.progress)
            }
        }
        Box(Modifier.height(96.dp))
    }
}

/** Sozlamalardan qaytganda "o'rnatish ruxsati" holatini qayta o'qish uchun. */
@Composable
private fun resumeTickForInstallPermission() {
    rememberResumeTick()
}
