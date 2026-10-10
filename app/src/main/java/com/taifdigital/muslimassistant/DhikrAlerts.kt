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

object DhikrAlerts {
    private const val ID = 305
    private const val CHANNEL = "dhikr_hourly_v1"
    private fun pending(context: Context) = PendingIntent.getBroadcast(
        context, ID, Intent(context, DhikrReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    fun schedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pending(context))
        val prefs = context.getSharedPreferences("preferences", 0)
        if (!prefs.getBoolean("dhikr", false)) return
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60L * 60L * 1000L, pending(context))
    }
    fun show(context: Context) {
        val prefs = context.getSharedPreferences("preferences", 0)
        if (!prefs.getBoolean("dhikr", false)) return
        val hour = ZonedDateTime.now(ZoneId.systemDefault()).hour
        if (prefs.getBoolean("quiet", true) && (hour >= 22 || hour < 7)) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "تذكير الأذكار", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(ID, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("اذكر الله")
            .setContentText("سبحان الله وبحمده، سبحان الله العظيم")
            .setSilent(!prefs.getBoolean("sound", true))
            .setAutoCancel(true).build())
    }
}
class DhikrReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DhikrAlerts.show(context)
        DhikrAlerts.schedule(context)
    }
}
