package com.taifdigital.muslimassistant

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.*

data class PrayerCity(val name: String, val latitude: Double, val longitude: Double, val zone: String)
data class PrayerMoment(val name: String, val time: LocalTime)

object PrayerTimes {
    val cities = listOf(
        PrayerCity("مكة المكرمة", 21.4225, 39.8262, "Asia/Riyadh"),
        PrayerCity("دمشق", 33.5138, 36.2765, "Asia/Damascus"),
        PrayerCity("المدينة المنورة", 24.4672, 39.6111, "Asia/Riyadh"),
        PrayerCity("الرياض", 24.7136, 46.6753, "Asia/Riyadh"),
        PrayerCity("القاهرة", 30.0444, 31.2357, "Africa/Cairo"),
        PrayerCity("إسطنبول", 41.0082, 28.9784, "Europe/Istanbul")
    )

    // NOAA solar approximation. Times are estimates, not official local mosque schedules.
    fun calculate(date: LocalDate, city: PrayerCity, fajrAngle: Double = 18.0, ishaAngle: Double = 17.0): List<PrayerMoment> {
        val n = date.dayOfYear.toDouble()
        val days = if (date.isLeapYear) 366.0 else 365.0
        val gamma = 2.0 * PI / days * (n - 1.0)
        val eq = 229.18 * (0.000075 + 0.001868*cos(gamma) - 0.032077*sin(gamma) -
            0.014615*cos(2*gamma) - 0.040849*sin(2*gamma))
        val decl = 0.006918 - 0.399912*cos(gamma) + 0.070257*sin(gamma) -
            0.006758*cos(2*gamma) + 0.000907*sin(2*gamma) -
            0.002697*cos(3*gamma) + 0.00148*sin(3*gamma)
        val lat = Math.toRadians(city.latitude)
        val zone = ZoneId.of(city.zone)
        val offset = date.atTime(12, 0).atZone(zone).offset.totalSeconds / 3600.0
        val noon = 720.0 - 4.0*city.longitude - eq + 60.0*offset
        fun hourAngle(altitude: Double): Double? {
            val ratio = (sin(Math.toRadians(altitude)) - sin(lat)*sin(decl))/(cos(lat)*cos(decl))
            if (ratio !in -1.0..1.0) return null
            return Math.toDegrees(acos(ratio))*4.0
        }
        fun time(minutes: Double): LocalTime = LocalTime.ofSecondOfDay(
            ((minutes * 60.0).roundToLong() % 86400L + 86400L) % 86400L
        )
        val rise = hourAngle(-0.833) ?: return emptyList()
        val fajr = hourAngle(-fajrAngle) ?: return emptyList()
        val isha = hourAngle(-ishaAngle) ?: return emptyList()
        val asrAltitude = Math.toDegrees(atan(1.0 / (1.0 + tan(abs(lat - decl)))))
        val asr = hourAngle(asrAltitude) ?: return emptyList()
        return listOf(
            PrayerMoment("الفجر", time(noon - fajr)),
            PrayerMoment("الظهر", time(noon)),
            PrayerMoment("العصر", time(noon + asr)),
            PrayerMoment("المغرب", time(noon + rise)),
            PrayerMoment("العشاء", time(noon + isha))
        )
    }
}
