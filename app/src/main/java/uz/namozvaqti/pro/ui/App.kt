package uz.namozvaqti.pro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import uz.namozvaqti.pro.AppViewModel
import java.time.temporal.ChronoUnit

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

    Box(Modifier.fillMaxSize()) {
        // Hamma sahifalar uchun BITTA umumiy osmon foni: bosh sahifada to'liq ko'rinadi,
        // boshqa sahifalarda xiralashadi (shaffof shisha kartalar orqasida sezilib turadi).
        AppBackdrop(vm, screen == Screen.HOME)

        Box(Modifier.fillMaxSize().imePadding()) {
            // Yumshoq, prujinali o'tish: yangi sahifa yon tomondan suzib kiradi, biroz kattalashadi va ravshanlashadi;
            // eskisi so'nadi va kichrayadi. Faqat graphicsLayer o'zgaradi — qayta o'lchash yo'q, shuning uchun silliq (FPS yuqori).
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = 380f)) { dir * (it / 5) } +
                        fadeIn(tween(260, delayMillis = 40)) +
                        scaleIn(spring(dampingRatio = 0.9f, stiffness = 380f), initialScale = 0.94f)) togetherWith
                        (slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -dir * (it / 8) } +
                            fadeOut(tween(150)) + scaleOut(tween(220), targetScale = 0.96f))
                },
                label = "screen",
                modifier = Modifier.fillMaxSize(),
            ) { target ->
                // Bosh sahifa holat satri ostigacha to'liq ekran (osmon), qolganlari holat satridan pastda.
                Column(Modifier.fillMaxSize().then(if (target == Screen.HOME) Modifier else Modifier.statusBarsPadding())) {
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
}

/** Haqiqiy quyosh/oy holatidagi osmon (daqiqada bir yangilanadi) va boshqa sahifalarda xiralashtiruvchi parda. */
@Composable
private fun AppBackdrop(vm: AppViewModel, homeVisible: Boolean) {
    val s by vm.settings.collectAsState()
    val now = vm.now.collectAsState()
    val minute by remember { derivedStateOf { now.value.truncatedTo(ChronoUnit.MINUTES) } }
    val scrim by animateFloatAsState(if (homeVisible) 0f else 0.62f, tween(450), label = "scrim")
    Box(Modifier.fillMaxSize()) {
        SkyBackdrop(s.lat, s.lon, s.gender, minute)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = scrim }.background(Glass.Bg))
    }
}

private val TABS = listOf(Screen.HOME, Screen.HISTORY, Screen.CALENDAR, Screen.QURAN, Screen.TAHAJJUD, Screen.SETTINGS)

/**
 * Yumaloq, suzuvchi shaffof shisha panel. Tanlangan bo'lim pushti-siyohrang "pufak" ichida; sahifa almashganda
 * pufak prujinali siljib boradi, ikonka esa biroz kattalashadi.
 */
@Composable
private fun BottomBar(current: Screen, onSelect: (Screen) -> Unit, modifier: Modifier) {
    val idx = TABS.indexOf(if (current == Screen.UPDATE) Screen.SETTINGS else current).coerceAtLeast(0)
    Box(modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Glass.Bg.copy(alpha = 0.42f), CircleShape)
                .glassSurface(CircleShape, strength = 1.15f),
        ) {
            val slot = (maxWidth - 12.dp) / TABS.size
            val x by animateDpAsState(6.dp + slot * idx + (slot - 48.dp) / 2, spring(dampingRatio = 0.62f, stiffness = 420f), label = "bubble")
            Box(
                Modifier
                    .offset { IntOffset(x.roundToPx(), 8.dp.roundToPx()) }
                    .size(48.dp)
                    .background(Brush.linearGradient(listOf(Glass.Pink.copy(alpha = 0.55f), Glass.Violet.copy(alpha = 0.5f))), CircleShape)
                    .gloss(CircleShape, 0.28f)
                    .glassSurface(CircleShape, Glass.Tint, 1.2f),
            )
            Row(
                Modifier.fillMaxSize().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TABS.forEachIndexed { i, t ->
                    val selected = i == idx
                    val iconScale by animateFloatAsState(if (selected) 1.14f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "tab-scale")
                    Box(
                        Modifier.weight(1f).height(48.dp).pressable(scaleDown = 0.84f) { onSelect(t) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            t.icon!!, t.label,
                            tint = if (selected) Glass.PinkSoft else Glass.InkSoft,
                            modifier = Modifier.size(24.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                        )
                    }
                }
            }
        }
    }
}
