package uz.namozvaqti.core

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** [region] — islom.uz ma'lumotlarida shahar nomi. */
data class City(val name: String, val region: String, val lat: Double, val lon: Double)

object UzbekCities {
    val all: List<City> = listOf(
        City("Toshkent", "Toshkent", 41.3111, 69.2797),
        City("Samarqand", "Samarqand", 39.6542, 66.9597),
        City("Buxoro", "Buxoro", 39.7681, 64.4556),
        City("Andijon", "Andijon", 40.7821, 72.3442),
        City("Namangan", "Namangan", 41.0011, 71.6683),
        City("Farg'ona", "Farg'ona", 40.3864, 71.7864),
        City("Qo'qon", "Qo'qon", 40.5286, 70.9425),
        City("Marg'ilon", "Marg'ilon", 40.4711, 71.7247),
        City("Qarshi", "Qarshi", 38.8606, 65.7891),
        City("Navoiy", "Navoiy", 40.0844, 65.3792),
        City("Jizzax", "Jizzax", 40.1158, 67.8422),
        City("Guliston", "Guliston", 40.4897, 68.7842),
        City("Termiz", "Termiz", 37.2242, 67.2783),
        City("Urganch", "Urganch", 41.5506, 60.6317),
        City("Xiva", "Xiva", 41.3783, 60.3639),
        City("Nukus", "Nukus", 42.4531, 59.6103),
    )

    fun byName(name: String?): City? = all.firstOrNull { it.name == name }

    fun nearest(lat: Double, lon: Double): Pair<City, Double> =
        all.map { it to distanceKm(lat, lon, it.lat, it.lon) }.minBy { it.second }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Foydalanuvchi shahar markazidan sharqda bo'lsa quyosh barvaqt chiqadi: har 1° uzunlik = 4 daqiqa. */
    fun longitudeShiftMinutes(cityLon: Double, userLon: Double): Int =
        ((cityLon - userLon) * 4.0).roundToInt()

    /** Shahar jadvali shu masofagacha foydalanuvchi joylashuviga mos deb hisoblanadi. */
    const val MAX_CITY_DISTANCE_KM = 120.0
}
