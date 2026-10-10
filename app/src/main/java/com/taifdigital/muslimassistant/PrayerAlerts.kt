package com.taifdigital.muslimassistant

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.ZonedDateTime
import java.time.ZoneId

/**
 * Inexact, battery-friendly prayer notifications. No full adhan audio yet.
 * AlarmManager may delay delivery during Doze; never promise exact timing.
 */
object PrayerAlerts {
    private const val CHANNEL = "prayer_reminders_v1"
    private const val REQUEST = 204
    private const val ACTION_PRAYER = "com.taifdigital.muslimassistant.PRAYER"
    private fun intent(context: Context): PendingIntent {
        val i = Intent(context, PrayerAlertReceiver::class.java).setAction(ACTION_PRAYER)
        return PendingIntent.getBroadcast(context, REQUEST, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun schedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(intent(context))
        val prefs = context.getSharedPreferences("preferences", 0)
        if (!prefs.getBoolean("prayer_alerts", false)) return
        val city = PrayerTimes.cities.firstOrNull { it.name == prefs.getString("prayer_city", "دمشق") } ?: PrayerTimes.cities.first()
        val zone = ZoneId.of(city.zone)
        val now = ZonedDateTime.now(zone)
        val angles = if (prefs.getString("prayer_method", "MWL") == "EGYPT") 19.5 to 17.5 else 18.0 to 17.0
        val next = (0L..2L).asSequence().flatMap { day ->
            val date = now.toLocalDate().plusDays(day)
            PrayerTimes.calculate(date, city, angles.first, angles.second).asSequence().map { prayer ->
                prayer.name to date.atTime(prayer.time).atZone(zone).toInstant().toEpochMilli()
            }
        }.firstOrNull { it.second > System.currentTimeMillis() + 1000L } ?: return
        // No exact-alarm permission: may arrive late under battery restrictions.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second, intent(context))
    }
    fun notifyPrayer(context: Context) {
        val prefs = context.getSharedPreferences("preferences", 0)
        if (!prefs.getBoolean("prayer_alerts", false)) return
        val city = PrayerTimes.cities.firstOrNull { it.name == prefs.getString("prayer_city", "دمشق") } ?: PrayerTimes.cities.first()
        val now = ZonedDateTime.now(ZoneId.of(city.zone))
        val angles = if (prefs.getString("prayer_method", "MWL") == "EGYPT") 19.5 to 17.5 else 18.0 to 17.0
        val closest = PrayerTimes.calculate(now.toLocalDate(), city, angles.first, angles.second)
            .minByOrNull { kotlin.math.abs(java.time.Duration.between(it.time, now.toLocalTime()).toMinutes()) }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "تذكير الصلاة", NotificationManager.IMPORTANCE_DEFAULT))
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت الصلاة")
                .setContentText("موعد صلاة ${closest?.name ?: ""} — ${city.name} (توقيت تقريبي)")
                .setAutoCancel(true).build()
            manager.notify(REQUEST, notification)
        }
    }
}
class PrayerAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerAlerts.notifyPrayer(context)
        PrayerAlerts.schedule(context)
    }
}
class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_TIMEZONE_CHANGED || intent.action == Intent.ACTION_TIME_CHANGED) {
            PrayerAlerts.schedule(context)
            DhikrAlerts.schedule(context)
        }
    }
}
