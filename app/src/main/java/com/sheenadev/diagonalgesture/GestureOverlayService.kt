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
import android.util.Log
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

    private var isTracking = false
    private var startY = 0f
    private var startX = 0f
    private var currentOverlay: View? = null

    companion object {
        private const val TAG = "GestureOverlay"
        private const val DEBUG = true

        const val ACTION_UPDATE_SETTINGS = "update_settings"

        private const val CHANNEL_ID = "gesture_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_ACTION = "action"
        private const val ACTION_START = "start"
        private const val ACTION_STOP = "stop"

        private const val OVERLAY_WIDTH_DP = 12f
        private const val OVERLAY_HEIGHT_DP = 80f

        fun startService(context: Context) {
            val intent = Intent(context, GestureOverlayService::class.java).apply {
                putExtra(EXTRA_ACTION, ACTION_START)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, GestureOverlayService::class.java).apply {
                putExtra(EXTRA_ACTION, ACTION_STOP)
            }
            context.startService(intent)
        }

        fun updateSettings(context: Context) {
            val intent = Intent(context, GestureOverlayService::class.java).apply {
                putExtra(EXTRA_ACTION, ACTION_UPDATE_SETTINGS)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "Service created")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefsManager = GesturePreferenceManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.getStringExtra(EXTRA_ACTION)) {
            ACTION_STOP -> {
                removeOverlays()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_SETTINGS -> {
                refreshOverlays()
                return START_STICKY
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
            CHANNEL_ID, "Gesture Service", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps gesture detection running"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gesture Detection Active")
            .setContentText("Swipe from corner to open app")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupOverlays() {
        if (DEBUG) Log.d(TAG, "Setting up overlays...")
        removeOverlays()
        createOverlays()
    }

    private fun refreshOverlays() {
        removeOverlays()
        createOverlays()
    }

    private fun createOverlays() {
        val position = prefsManager.triggerPosition
        if (position == GesturePreferenceManager.POSITION_BOTTOM_RIGHT || position == GesturePreferenceManager.POSITION_BOTH) {
            overlayViewRight = createOverlayView(Gravity.END or Gravity.BOTTOM, "right")
        }
        if (position == GesturePreferenceManager.POSITION_BOTTOM_LEFT || position == GesturePreferenceManager.POSITION_BOTH) {
            overlayViewLeft = createOverlayView(Gravity.START or Gravity.BOTTOM, "left")
        }
        if (DEBUG) Log.d(TAG, "Overlays ready. R=${overlayViewRight != null}, L=${overlayViewLeft != null}")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView(gravity: Int, side: String): View? {
        val density = resources.displayMetrics.density

        val widthDp = if (prefsManager.areaWidthDp > 0) prefsManager.areaWidthDp else OVERLAY_WIDTH_DP
        val heightDp = if (prefsManager.areaHeightDp > 0) prefsManager.areaHeightDp else OVERLAY_HEIGHT_DP

        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()

        if (DEBUG) Log.d(TAG, "Creating overlay: ${widthDp}dp (${widthPx}px) x ${heightDp}dp (${heightPx}px), gravity=$gravity, side=$side")

        val layout = FrameLayout(this)
        layout.tag = side

        if (prefsManager.debugMode) {
            layout.setBackgroundColor(android.graphics.Color.argb(128, 255, 0, 0))
        }

        val flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

        val params = WindowManager.LayoutParams(
            widthPx, heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = gravity

        layout.setOnTouchListener { v, event ->
            handleTouch(event, v)
        }

        try {
            windowManager.addView(layout, params)
            return layout
        } catch (e: Exception) {
            if (DEBUG) Log.e(TAG, "Failed: ${e.message}")
            return null
        }
    }

    private fun handleTouch(event: MotionEvent, view: View): Boolean {
        if (DEBUG) Log.d(TAG, "Touch: ${MotionEvent.actionToString(event.action)} rawY=${event.rawY.toInt()}")

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTracking = true
                startY = event.rawY
                startX = event.rawX
                currentOverlay = view
                if (DEBUG) Log.d(TAG, "DOWN Y=${startY.toInt()}, threshold=${prefsManager.swipeThreshold}")
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTracking) {
                    val deltaY = startY - event.rawY
                    if (DEBUG && deltaY > 50) {
                        Log.d(TAG, "MOVE deltaY=$deltaY, threshold=${prefsManager.swipeThreshold}")
                    }
                    if (deltaY > prefsManager.swipeThreshold) {
                        isTracking = false
                        if (DEBUG) Log.w(TAG, "SWIPE TRIGGERED! dy=$deltaY")
                        launchTargetApp(view.tag as String)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTracking) {
                    val deltaY = startY - event.rawY
                    if (DEBUG) {
                        val status = if (deltaY > prefsManager.swipeThreshold) "TRIGGERED" else "not enough"
                        Log.d(TAG, "UP deltaY=$deltaY, threshold=${prefsManager.swipeThreshold} - $status")
                    }
                    if (deltaY > prefsManager.swipeThreshold) {
                        isTracking = false
                        if (DEBUG) Log.w(TAG, "SWIPE on UP! dy=$deltaY")
                        launchTargetApp(view.tag as String)
                    }
                }
                isTracking = false
                currentOverlay = null
                return true
            }
        }
        return true
    }

    private fun launchTargetApp(side: String) {
        val packageName: String
        val activityName: String

        when (side) {
            "right" -> {
                packageName = prefsManager.rightAppPackage
                activityName = prefsManager.rightActivityName
            }
            "left" -> {
                packageName = prefsManager.leftAppPackage
                activityName = prefsManager.leftActivityName
            }
            else -> return
        }

        if (packageName.isEmpty()) {
            if (DEBUG) Log.e(TAG, "No target app for side=$side")
            return
        }

        if (DEBUG) Log.d(TAG, "Launching side=$side: package=$packageName component=${if (activityName.isNotEmpty()) activityName else "default"}")

        val intent = if (activityName.isNotEmpty()) {
            Intent().apply {
                component = android.content.ComponentName(packageName, activityName)
            }
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            if (DEBUG) Log.d(TAG, "Launched!")
        } else {
            if (DEBUG) Log.e(TAG, "No launch intent for $packageName")
        }
    }

    private fun removeOverlays() {
        try { overlayViewRight?.let { windowManager.removeView(it); overlayViewRight = null } } catch (e: Exception) {}
        try { overlayViewLeft?.let { windowManager.removeView(it); overlayViewLeft = null } } catch (e: Exception) {}
    }
}