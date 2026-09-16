package com.werare.proxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * Фоновый сервис: держит плавающую кнопку поверх окон.
 * Тап по кнопке включает/выключает прокси; на кнопке идёт обратный
 * отсчёт секунд + полоска прогресса, которая заканчивается по таймеру.
 */
class OverlayService : Service() {

    companion object {
        const val EXTRA_TOGGLE = "com.werare.proxy.TOGGLE"
        private const val CHANNEL_ID = "werare_overlay"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private var button: RingButtonView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var engine: ProxyEngine? = null
    private var running = false
    private var totalSeconds = 60
    private var secondsLeft = 0

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            secondsLeft--
            updateButton()
            if (secondsLeft <= 0) stopProxy() else handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        val prefs = Prefs(this)
        if (prefs.showOverlayButton && button == null) addButton(prefs)
        if (intent?.getBooleanExtra(EXTRA_TOGGLE, false) == true) toggle()
        return START_STICKY
    }

    private fun toggle() {
        if (running) stopProxy() else startProxy()
    }

    private fun startProxy() {
        val prefs = Prefs(this)
        val server = ProxyServer.parse(prefs.lastProxy)
        if (server == null) {
            updateButton()
            return
        }
        engine = ProxyEngine(server, prefs.lagMs, prefs.lossPercent).apply { start() }
        running = true
        totalSeconds = prefs.durationSec
        secondsLeft = totalSeconds
        handler.post(tick)
        updateButton()
    }

    private fun stopProxy() {
        running = false
        handler.removeCallbacks(tick)
        engine?.stop()
        engine = null
        secondsLeft = 0
        updateButton()
    }

    private fun updateButton() {
        button?.let { b ->
            b.ringProgress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds else 1f
            b.secondsText = if (running) secondsLeft.toString() else ""
            b.label = if (running) "STOP" else "GO"
        }
    }

    private fun addButton(prefs: Prefs) {
        val metrics = resources.displayMetrics
        val sizePx = (prefs.buttonSize * metrics.density).toInt()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            sizePx, sizePx, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val view = RingButtonView(this).apply {
            color = prefs.buttonColor
            cornerRadius = prefs.cornerRadius * metrics.density
            alpha = prefs.buttonOpacity / 100f
        }

        var dragging = false
        var startX = 0f; var startY = 0f
        var offsetX = 0; var offsetY = 0

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragging = false
                    startX = event.rawX; startY = event.rawY
                    offsetX = params.x; offsetY = params.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (kotlin.math.abs(event.rawX - startX) > 12 ||
                        kotlin.math.abs(event.rawY - startY) > 12) dragging = true
                    params.x = offsetX + (event.rawX - startX).toInt()
                    params.y = offsetY + (event.rawY - startY).toInt()
                    windowManager.updateViewLayout(view, params)
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) toggle()
                }
            }
            true
        }

        windowManager.addView(view, params)
        button = view
    }

    private fun removeButton() {
        button?.let { runCatching { windowManager.removeView(it) } }
        button = null
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "WerareProxy", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WerareProxy")
            .setContentText("Кнопка поверх окон активна")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(pi)
            .build()
    }

    override fun onDestroy() {
        stopProxy()
        removeButton()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
