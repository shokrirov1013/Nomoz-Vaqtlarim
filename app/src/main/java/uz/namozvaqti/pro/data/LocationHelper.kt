package uz.namozvaqti.pro.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uz.namozvaqti.core.UzbekCities
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Joriy joylashuv: avval oxirgi ma'lum, bo'lmasa yangi so'rov (15 soniyagacha). */
    @SuppressLint("MissingPermission")
    suspend fun current(ctx: Context): Location? {
        if (!hasPermission(ctx)) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val fresh = withTimeoutOrNull(15_000) {
            for (p in providers.filter { it != LocationManager.PASSIVE_PROVIDER }) {
                val loc = single(lm, p)
                if (loc != null) return@withTimeoutOrNull loc
            }
            null
        }
        if (fresh != null) return fresh
        return providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun single(lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= 30) {
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                lm.getCurrentLocation(provider, signal, Runnable::run) { loc -> if (cont.isActive) cont.resume(loc) }
            } else {
                @Suppress("DEPRECATION")
                val last = lm.getLastKnownLocation(provider)
                cont.resume(last)
            }
        }

    /** Shahar nomi: yaqin O'zbekiston shahri, aks holda Geocoder, aks holda koordinatalar. */
    suspend fun label(ctx: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        val (city, km) = UzbekCities.nearest(lat, lon)
        if (km <= 40) return@withContext city.name
        val geo = runCatching {
            @Suppress("DEPRECATION")
            Geocoder(ctx, Locale("uz")).getFromLocation(lat, lon, 1)?.firstOrNull()
        }.getOrNull()
        geo?.locality ?: geo?.subAdminArea ?: geo?.adminArea ?: "%.2f, %.2f".format(Locale.US, lat, lon)
    }
}
