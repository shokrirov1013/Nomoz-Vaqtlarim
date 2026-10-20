package uz.namozvaqti.pro.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.namozvaqti.pro.ui.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.pro.ServiceLocator
import uz.namozvaqti.pro.ui.CrescentMosqueArt
import uz.namozvaqti.pro.ui.Fmt
import uz.namozvaqti.pro.ui.Glass
import uz.namozvaqti.pro.ui.GlassBackground
import uz.namozvaqti.pro.ui.GlassButton
import uz.namozvaqti.pro.ui.GlassCard
import uz.namozvaqti.pro.ui.NamozTheme
import uz.namozvaqti.pro.ui.Type
import java.time.LocalDate
import java.time.LocalTime

/** Budilnik ekrani (qulflangan ekran ustida ham ochiladi). */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val kind = RingKind.fromKey(intent.getStringExtra(Extra.KIND))
        val app = applicationContext

        uz.namozvaqti.pro.ui.Glass.apply(ServiceLocator.get(app).settings.value.design)
        setContent {
            val cyr = ServiceLocator.get(app).settings.value.cyrillic
            androidx.compose.runtime.CompositionLocalProvider(uz.namozvaqti.pro.ui.LocalCyrillic provides cyr) {
            NamozTheme {
                GlassBackground {
                    val active by RingState.active.collectAsState()
                    var seenActive by remember { mutableStateOf(false) }
                    LaunchedEffect(active) {
                        if (active != null) seenActive = true else if (seenActive) finish()
                    }
                    val snooze = ServiceLocator.get(app).settings.value.snoozeMinutes
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CrescentMosqueArt(Modifier.size(width = 140.dp, height = 120.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(Fmt.time(LocalTime.now()), style = Type.Big.copy(color = Glass.PinkSoft))
                        Text(kind.title, style = Type.H1)
                        val st = ServiceLocator.get(app).settings.value
                        Text(
                            RingTexts.of(kind, st.gentle, st.gender),
                            style = Type.Body, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp),
                        )
                        Spacer(Modifier.height(28.dp))
                        GlassCard(Modifier.fillMaxWidth()) {
                            GlassButton("To'xtatish", { RingControl.stop(app); finish() }, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            GlassButton("Keyinga surish ($snooze daq.)", { RingControl.snooze(app, kind); finish() }, Modifier.fillMaxWidth(), filled = false)
                            if (kind == RingKind.FAJR) {
                                Spacer(Modifier.height(10.dp))
                                GlassButton("Bomdod bajarildi", {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        PrayerActions.setDone(app, LocalDate.now(), Prayer.FAJR, true)
                                    }
                                    RingControl.stop(app); finish()
                                }, Modifier.fillMaxWidth(), filled = false)
                            }
                        }
                    }
                }
            }
            }
        }
    }
}
