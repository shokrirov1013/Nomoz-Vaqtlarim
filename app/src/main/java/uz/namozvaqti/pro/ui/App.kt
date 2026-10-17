package uz.namozvaqti.pro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import uz.namozvaqti.pro.AppViewModel

enum class Screen(val label: String, val icon: ImageVector?) {
    HOME("Bosh sahifa", Icons.Default.Home),
    HISTORY("Tarix", Icons.AutoMirrored.Filled.List),
    CALENDAR("Taqvim", Icons.Default.DateRange),
    QURAN("Suralar", TabIcons.Book),
    TAHAJJUD("Tahajjud", TabIcons.Crescent),
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
        // Qisqa va yengil "crossfade": oynadan oynaga o'tishda qotish yo'q.
        Crossfade(targetState = screen, animationSpec = tween(140), label = "screen", modifier = Modifier.fillMaxSize()) { target ->
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                when (target) {
                    Screen.HOME -> HomeScreen(vm) { screen = it }
                    Screen.HISTORY -> HistoryScreen(vm)
                    Screen.CALENDAR -> CalendarScreen(vm)
                    Screen.QURAN -> QuranScreen()
                    Screen.TAHAJJUD -> TahajjudScreen(vm)
                    Screen.SETTINGS -> SettingsScreen(vm) { screen = it }
                    Screen.UPDATE -> UpdateScreen(vm)
                }
            }
        }
        BottomBar(screen, { screen = it }, Modifier.align(Alignment.BottomCenter))
    }
}

private val TABS = listOf(Screen.HOME, Screen.HISTORY, Screen.CALENDAR, Screen.QURAN, Screen.TAHAJJUD, Screen.SETTINGS)

/** Yumaloq, suzuvchi shisha panel: faqat ikonkalar, tanlangani yumaloq oltin "pufak" ichida. */
@Composable
private fun BottomBar(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier) {
    Box(modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Glass.Bg.copy(alpha = 0.62f), CircleShape)
                .glassSurface(CircleShape, strength = 1.15f)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TABS.forEach { t ->
                val selected = t == current || (current == Screen.UPDATE && t == Screen.SETTINGS)
                Box(
                    Modifier
                        .size(48.dp)
                        .then(if (selected) Modifier.background(Glass.Gold.copy(alpha = 0.22f), CircleShape).glassSurface(CircleShape, Glass.Tint, 1.2f) else Modifier)
                        .clickableNoRipple { onSelect(t) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        t.icon!!, t.label,
                        tint = if (selected) Glass.GoldBright else Glass.InkSoft,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
