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
 * Exact when the user grants access; notification-only fallback otherwise.
 * AlarmManager may delay delivery during Doze; never promise exact timing.
 */
object PrayerAlerts {
    private const val CHANNEL = "prayer_reminders_v1"
    private const val REQUEST = 204
    private const val ACTION_PRAYER = "com.taifdigital.muslimassistant.PRAYER"
    private const val KEY_NAME = "scheduled_prayer_name"
    private const val KEY_TIME = "scheduled_prayer_time"
    private fun intent(context: Context): PendingIntent {
        val i = Intent(context, PrayerAlertReceiver::class.java).setAction(ACTION_PRAYER)
        return PendingIntent.getBroadcast(context, REQUEST, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun schedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(intent(context))
        context.getSharedPreferences("prayer_schedule", 0).edit().clear().apply()
        val prefs = context.getSharedPreferences("preferences", 0)
        if (!prefs.getBoolean("prayer_alerts", false)) return
        val city = PrayerTimes.cities.firstOrNull { it.name == prefs.getString("prayer_city", "مكة المكرمة") } ?: PrayerTimes.cities.first()
        val zone = ZoneId.of(city.zone)
        val now = ZonedDateTime.now(zone)
        val method = prefs.getString("prayer_method", "MWL") ?: "MWL"
        val correction = prefs.getInt("prayer_offset_minutes", 0)
        val event = PrayerTimes.next(now.plusSeconds(1), city, method, correction) ?: return
        val next = event.name to event.at.toInstant().toEpochMilli()
        context.getSharedPreferences("prayer_schedule", 0).edit()
            .putString(KEY_NAME, next.first).putLong(KEY_TIME, next.second).apply()
        val exact = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
        try {
            if (exact) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second, intent(context))
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second, intent(context))
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.second, intent(context))
        }
    }
    /** A separate test notification that does not depend on a future scheduled prayer. */
    fun testNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "تذكير الصلاة", NotificationManager.IMPORTANCE_DEFAULT))
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("تجربة تنبيه الصلاة")
            .setContentText("هذا إشعار تجريبي للتأكد من السماح بالإشعارات، وليس موعد صلاة.")
            .setAutoCancel(true).build()
        if (!manager.areNotificationsEnabled()) return false
        manager.notify(REQUEST + 1, notification)
        return true
    }
    fun notifyPrayer(context: Context) {
        val prefs = context.getSharedPreferences("preferences", 0)
        if (!prefs.getBoolean("prayer_alerts", false)) return
        val schedule = context.getSharedPreferences("prayer_schedule", 0)
        val prayerName = schedule.getString(KEY_NAME, null) ?: return
        val scheduledTime = schedule.getLong(KEY_TIME, 0L)
        // Ignore stale broadcasts after clock, location or calculation changes.
        val lateness = System.currentTimeMillis() - scheduledTime
        if (lateness < -1000L || lateness > 15L * 60L * 1000L) return
        schedule.edit().remove(KEY_TIME).remove(KEY_NAME).apply()
        val cityName = prefs.getString("prayer_city", "مكة المكرمة") ?: "مكة المكرمة"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "تذكير الصلاة", NotificationManager.IMPORTANCE_DEFAULT))
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت الصلاة")
                .setContentText("موعد صلاة $prayerName — $cityName (توقيت تقريبي)")
                .setAutoCancel(true).build()
            manager.notify(REQUEST, notification)
            val alarm = context.getSystemService(AlarmManager::class.java)
            val exact = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
            if (exact && lateness <= 2 * 60_000L && prefs.getBoolean("adhan", true) &&
                prefs.getString("voice", "makkah") == AdhanService.VOICE && manager.areNotificationsEnabled()) {
                AdhanService.start(context, "أذان $prayerName — $cityName")
            }
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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_TIMEZONE_CHANGED || intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            PrayerAlerts.schedule(context)
            DhikrAlerts.schedule(context)
        }
    }
}
