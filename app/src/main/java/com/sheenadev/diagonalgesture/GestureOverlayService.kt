package com.sheenadev.diagonalgesture

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

class GestureOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefsManager: GesturePreferenceManager

    private var overlayViewRight: View? = null
    private var overlayViewLeft: View? = null

    private var isGestureActive = false
    private var startY = 0f
    private var startX = 0f

    companion object {
        private const val CHANNEL_ID = "gesture_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_ACTION = "action"
        private const val ACTION_START = "start"
        private const val ACTION_STOP = "stop"

        fun startService(context: Context) {
            val intent = Intent(context, GestureOverlayService::class.java).apply {
                putExtra(EXTRA_ACTION, ACTION_START)
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, GestureOverlayService::class.java).apply {
                putExtra(EXTRA_ACTION, ACTION_STOP)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefsManager = GesturePreferenceManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(EXTRA_ACTION)) {
            ACTION_STOP -> {
                removeOverlays()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification())
                setupOverlays()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlays()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gesture Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the gesture detection running"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gesture Detection Active")
            .setContentText("Monitoring swipe gestures in corner areas")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupOverlays() {
        removeOverlays()

        val position = prefsManager.triggerPosition

        if (position == GesturePreferenceManager.POSITION_BOTTOM_RIGHT || position == GesturePreferenceManager.POSITION_BOTH) {
            overlayViewRight = createOverlayView(Gravity.END or Gravity.BOTTOM)
        }

        if (position == GesturePreferenceManager.POSITION_BOTTOM_LEFT || position == GesturePreferenceManager.POSITION_BOTH) {
            overlayViewLeft = createOverlayView(Gravity.START or Gravity.BOTTOM)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView(gravity: Int): View {
        val density = resources.displayMetrics.density
        val widthDp = prefsManager.areaWidthDp
        val heightDp = prefsManager.areaHeightDp

        val layout = FrameLayout(this).apply {
            setBackgroundColor(0x00000000)
        }

        val layoutParams = WindowManager.LayoutParams(
            (widthDp * density).toInt(),
            (heightDp * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = gravity
        }

        layout.setOnTouchListener { _, event ->
            handleTouch(event)
        }

        windowManager.addView(layout, layoutParams)
        return layout
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isGestureActive = true
                startY = event.rawY
                startX = event.rawX
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isGestureActive) {
                    val deltaY = startY - event.rawY
                    val deltaX = event.rawX - startX

                    if (deltaY > prefsManager.swipeThreshold && deltaY > deltaX * 1.5) {
                        isGestureActive = false
                        launchTargetApp()
                        return true
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isGestureActive = false
                return true
            }
        }
        return true
    }

    private fun launchTargetApp() {
        val packageName = prefsManager.targetAppPackage
        if (packageName.isEmpty()) return

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun removeOverlays() {
        try {
            overlayViewRight?.let {
                windowManager.removeView(it)
                overlayViewRight = null
            }
            overlayViewLeft?.let {
                windowManager.removeView(it)
                overlayViewLeft = null
            }
        } catch (e: Exception) {
            // View might already be removed
        }
    }

    fun updateOverlaySize() {
        if (prefsManager.isServiceEnabled) {
            setupOverlays()
        }
    }
}