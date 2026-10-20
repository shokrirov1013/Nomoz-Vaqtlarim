package uz.namozvaqti.pro.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.namozvaqti.core.Prayer
import uz.namozvaqti.core.ReminderEvent
import uz.namozvaqti.core.ReminderKind
import uz.namozvaqti.pro.MainActivity
import uz.namozvaqti.pro.ServiceLocator
import uz.namozvaqti.pro.ui.DesignMode
import uz.namozvaqti.pro.ui.Fmt
import uz.namozvaqti.pro.ui.Glass
import uz.namozvaqti.pro.ui.GlassBackground
import uz.namozvaqti.pro.ui.GlassButton
import uz.namozvaqti.pro.ui.GlassCard
import uz.namozvaqti.pro.ui.LocalCyrillic
import uz.namozvaqti.pro.ui.NamozTheme
import uz.namozvaqti.pro.ui.SkyBackdrop
import uz.namozvaqti.pro.ui.Text
import uz.namozvaqti.pro.ui.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Namoz vaqti eslatma ekrani: qulflangan ekran va BOSHQA ILOVALAR USTIDAN ochiladi.
 * Ochilish yo'llari: (1) "Boshqa ilovalar ustida ko'rsatish" ruxsati bo'lsa — to'g'ridan-to'g'ri ishga tushiriladi,
 * (2) ruxsat bo'lmasa — to'liq ekranli bildirishnoma (qulflangan ekranda).
 */
class ReminderActivity : ComponentActivity() {
    private class Info(val date: LocalDate, val prayer: Prayer, val text: String)

    private var info by mutableStateOf<Info?>(null)

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
        info = parse(intent)
        if (info == null) {
            finish()
            return
        }
        val app = applicationContext
        Glass.apply(ServiceLocator.get(app).settings.value.design)

        setContent {
            val st = ServiceLocator.get(app).settings.value
            val now = remember { LocalDateTime.now() }
            CompositionLocalProvider(LocalCyrillic provides st.cyrillic) {
                NamozTheme {
                    GlassBackground {
                        if (Glass.mode == DesignMode.SKY) {
                            SkyBackdrop(st.lat, st.lon, st.gender, now)
                            Box(Modifier.fillMaxSize().background(Glass.Bg.copy(alpha = 0.5f)))
                        }
                        val cur = info
                        if (cur != null) {
                            // Hech kim tegmasa 2 daqiqadan keyin o'zi yopiladi (bildirishnoma va qayta eslatmalar davom etadi).
                            LaunchedEffect(cur) { delay(120_000); finish() }
                            Column(
                                Modifier.fillMaxSize().padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(Fmt.time(LocalTime.now()), style = Type.Big.copy(fontSize = 44.sp, color = Glass.PinkSoft))
                                Text("${cur.prayer.title} vaqti", style = Type.H1)
                                Text(
                                    cur.text, style = Type.Body, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp),
                                )
                                Spacer(Modifier.height(26.dp))
                                GlassCard(Modifier.fillMaxWidth(), tint = Glass.Tint) {
                                    GlassButton("Bajarildi", {
                                        CoroutineScope(Dispatchers.IO).launch { PrayerActions.setDone(app, cur.date, cur.prayer, true) }
                                        finish()
                                    }, Modifier.fillMaxWidth())
                                    Spacer(Modifier.height(10.dp))
                                    GlassButton("Keyinroq", { finish() }, Modifier.fillMaxWidth(), filled = false)
                                    Spacer(Modifier.height(10.dp))
                                    GlassButton("Ilovani ochish", {
                                        startActivity(Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        finish()
                                    }, Modifier.fillMaxWidth(), filled = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parse(intent)?.let { info = it }
    }

    private fun parse(i: Intent?): Info? {
        i ?: return null
        val date = i.getStringExtra(Extra.DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val prayer = Prayer.fromKey(i.getStringExtra(Extra.PRAYER)) ?: return null
        return Info(date, prayer, i.getStringExtra(Extra.TEXT).orEmpty())
    }

    companion object {
        private const val TAG = "ReminderActivity"

        private fun intent(ctx: Context, e: ReminderEvent, text: String): Intent =
            Intent(ctx, ReminderActivity::class.java)
                .putExtra(Extra.DATE, e.date.toString()).putExtra(Extra.PRAYER, e.prayer.key).putExtra(Extra.TEXT, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        /** To'liq ekranli bildirishnoma uchun (qulflangan ekranda ochiladi). */
        fun pendingIntent(ctx: Context, e: ReminderEvent, text: String): PendingIntent =
            PendingIntent.getActivity(
                ctx, 70 + e.prayer.ordinal, intent(ctx, e, text),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        /** "Boshqa ilovalar ustida ko'rsatish" ruxsati bo'lsa, oynani shu zahoti ochadi. */
        fun launch(ctx: Context, e: ReminderEvent, text: String) {
            if (e.kind == ReminderKind.PRE) return
            if (!Settings.canDrawOverlays(ctx)) return
            try {
                ctx.startActivity(intent(ctx, e, text))
            } catch (t: Throwable) {
                Log.w(TAG, "eslatma oynasini ochib bo'lmadi: ${t.message}")
            }
        }
    }
}
