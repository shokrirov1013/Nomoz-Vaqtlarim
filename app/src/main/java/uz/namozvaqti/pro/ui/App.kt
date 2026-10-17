package uz.namozvaqti.pro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.namozvaqti.pro.AppViewModel

enum class Screen(val label: String, val icon: ImageVector?) {
    HOME("Bosh", Icons.Default.Home),
    HISTORY("Tarix", Icons.AutoMirrored.Filled.List),
    CALENDAR("Taqvim", Icons.Default.DateRange),
    TAHAJJUD("Tahajjud", Icons.Default.Star),
    SETTINGS("Sozlamalar", Icons.Default.Settings),
    UPDATE("Yangilanish", null),
}

@Composable
fun App(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    if (!s.onboardingDone) {
        OnboardingScreen(vm)
        return
    }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.UPDATE) Screen.SETTINGS else Screen.HOME
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            when (screen) {
                Screen.HOME -> HomeScreen(vm) { screen = it }
                Screen.HISTORY -> HistoryScreen(vm)
                Screen.CALENDAR -> CalendarScreen(vm)
                Screen.TAHAJJUD -> TahajjudScreen(vm)
                Screen.SETTINGS -> SettingsScreen(vm) { screen = it }
                Screen.UPDATE -> UpdateScreen(vm)
            }
        }
        BottomBar(screen, { screen = it }, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BottomBar(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier) {
    val tabs = listOf(Screen.HOME, Screen.HISTORY, Screen.CALENDAR, Screen.TAHAJJUD, Screen.SETTINGS)
    Box(modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
        GlassCard(Modifier.fillMaxWidth(), corner = 28.dp, padding = 8.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                tabs.forEach { t ->
                    val sel = t == current || (current == Screen.UPDATE && t == Screen.SETTINGS)
                    Column(
                        Modifier.clickableNoRipple { onSelect(t) }.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(t.icon!!, t.label, tint = if (sel) Glass.PurpleDeep else Glass.InkSoft.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                        Text(t.label, style = Type.Caption.copy(fontSize = 11.sp, color = if (sel) Glass.PurpleDeep else Glass.InkSoft))
                    }
                }
            }
        }
    }
}
