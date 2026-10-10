package com.taifdigital.muslimassistant

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PrayerTimesTest {
    @Test fun supportedCitiesHaveOrderedDailyTimesThroughoutYear() {
        for (city in PrayerTimes.cities) for (method in listOf("MWL", "EGYPT")) for (day in 0L..364L) {
            val events = PrayerTimes.timeline(LocalDate.of(2026, 1, 1).plusDays(day), city, method, 0)
            assertEquals("${city.name} day $day", 5, events.size)
            assertEquals(listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء"), events.map { it.name })
            assertTrue(events.zipWithNext().all { (a, b) -> a.at.toInstant() < b.at.toInstant() })
        }
    }
    @Test fun afterIshaSelectsTomorrowAndHonorsSelectedZone() {
        val city = PrayerTimes.cities.first()
        val now = ZonedDateTime.of(2026, 10, 10, 23, 55, 0, 0, ZoneId.of(city.zone))
        val next = PrayerTimes.next(now, city, "MWL", 10)!!
        assertEquals("الفجر", next.name)
        assertEquals(LocalDate.of(2026, 10, 11), next.at.toLocalDate())
        assertEquals(next, PrayerTimes.next(now.withZoneSameInstant(ZoneId.of("America/New_York")), city, "MWL", 10))
    }
    @Test fun manualCorrectionIsAppliedOnceAndClamped() {
        val city = PrayerTimes.cities[1]
        val date = LocalDate.of(2026, 10, 10)
        val plain = PrayerTimes.timeline(date, city, "EGYPT", 0)
        for (offset in listOf(-30, 30)) {
            val shifted = PrayerTimes.timeline(date, city, "EGYPT", offset * 2)
            plain.zip(shifted).forEach { (a, b) -> assertEquals(offset * 60L, Duration.between(a.at, b.at).seconds) }
        }
    }
    @Test fun utcEquinoxHasPlausibleNoonAndSunset() {
        val events = PrayerTimes.calculate(LocalDate.of(2026, 3, 20), PrayerCity("Equator", 0.0, 0.0, "UTC"))
        assertTrue(events[1].time in LocalTime.of(12, 0)..LocalTime.of(12, 15))
        assertTrue(events[3].time in LocalTime.of(18, 0)..LocalTime.of(18, 20))
    }
}
