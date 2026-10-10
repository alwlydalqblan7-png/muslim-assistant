package com.taifdigital.muslimassistant

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/** Audio is never substituted for an unavailable saved Makkah/Madinah voice. */
class AdhanService : Service() {
    companion object {
        const val VOICE = "licensed_andrewler"
        const val STOP = "com.taifdigital.muslimassistant.STOP_ADHAN"
        @Volatile var playing = false
        fun start(context: Context, title: String): Boolean = try {
            context.startForegroundService(Intent(context, AdhanService::class.java).putExtra("title", title))
            true
        } catch (_: RuntimeException) { false }
    }
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("adhan_playback", "تشغيل الأذان", NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        val stop = PendingIntent.getService(this, 801, Intent(this, AdhanService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(this, 802, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notice = NotificationCompat.Builder(this, "adhan_playback")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(intent?.getStringExtra("title") ?: "الأذان")
            .setContentText("اضغط إيقاف لإنهاء الصوت").setContentIntent(open).setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف", stop).build()
        try {
            val serviceType = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            ServiceCompat.startForeground(this, 800, notice, serviceType)
            if (getSharedPreferences("preferences", 0).getString("voice", "makkah") != VOICE) { stopSelf(); return START_NOT_STICKY }
            player?.release(); player = null
            val audio = getSystemService(AudioManager::class.java)
            focus?.let { audio.abandonAudioFocusRequest(it) }
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs).setOnAudioFocusChangeListener { change ->
                    if (change < 0) stopSelf()
                }.build()
            focus = request
            if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { stopSelf(); return START_NOT_STICKY }
            player = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                resources.openRawResourceFd(R.raw.adhan_licensed).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                setOnPreparedListener { playing = true; it.start() }
                setOnCompletionListener { stopSelf() }
                setOnErrorListener { _, _, _ -> stopSelf(); true }
                prepareAsync()
            }
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ stopSelf() }, 5 * 60_000L)
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release(); player = null; playing = false
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
