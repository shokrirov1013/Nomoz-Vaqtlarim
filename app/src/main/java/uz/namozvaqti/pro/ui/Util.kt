package uz.namozvaqti.pro.ui

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** Ilovaga qaytilganda (masalan, tizim sozlamalaridan) oshib boradigan hisoblagich: ruxsat holatini qayta o'qish uchun. */
@Composable
fun rememberResumeTick(): Int {
    val owner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return tick
}

/**
 * Yumshoq scroll: barmoq qo'yib yuborilgandan keyin sakrash yo'q, harakat ohista va tekis so'nadi
 * (standartdan biroz ko'proq ishqalanish, boshlang'ich tezlik biroz pasaytirilgan).
 */
object SoftFling : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        var remaining = initialVelocity * 0.92f
        if (kotlin.math.abs(remaining) < 1f) return initialVelocity
        var last = 0f
        AnimationState(initialValue = 0f, initialVelocity = remaining).animateDecay(exponentialDecay(frictionMultiplier = 1.35f)) {
            val delta = value - last
            val consumed = scrollBy(delta)
            last = value
            remaining = velocity
            if (kotlin.math.abs(delta - consumed) > 0.5f) cancelAnimation()
        }
        return remaining
    }
}
