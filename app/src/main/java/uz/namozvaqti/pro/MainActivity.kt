package uz.namozvaqti.pro

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import uz.namozvaqti.pro.ui.App
import uz.namozvaqti.pro.ui.LocalCyrillic
import uz.namozvaqti.pro.ui.GlassBackground
import uz.namozvaqti.pro.ui.NamozTheme

class MainActivity : ComponentActivity() {
    private lateinit var vm: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        vm = ViewModelProvider(this)[AppViewModel::class.java]
        if (vm.settings.value.onboardingDone) vm.onAppStart()
        setContent {
            val s by vm.settings.collectAsState()
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalCyrillic provides s.cyrillic,
                LocalDensity provides Density(base.density, base.fontScale * (if (s.bigText) 1.2f else 1f)),
            ) {
                NamozTheme {
                    GlassBackground { App(vm) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ruxsat berilgan bo'lsa (sozlamalardan qaytganda) alarmlar darhol tiklansin.
        if (vm.settings.value.onboardingDone) vm.updateSettings { it }
    }
}
