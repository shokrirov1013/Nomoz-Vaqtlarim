package uz.namozvaqti.pro

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import uz.namozvaqti.pro.ui.App
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
            NamozTheme {
                GlassBackground { App(vm) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ruxsat berilgan bo'lsa (sozlamalardan qaytganda) alarmlar darhol tiklansin.
        if (vm.settings.value.onboardingDone) vm.updateSettings { it }
    }
}
