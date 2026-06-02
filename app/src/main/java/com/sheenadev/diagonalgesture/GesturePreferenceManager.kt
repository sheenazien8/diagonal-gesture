package com.sheenadev.diagonalgesture

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class GesturePreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "gesture_prefs"
        private const val KEY_ENABLED = "service_enabled"
        private const val KEY_TARGET_APP = "target_app_package"
        private const val KEY_TARGET_NAME = "target_app_name"
        private const val KEY_TARGET_ACTIVITY = "target_activity_name"
        private const val KEY_TARGET_ACTIVITY_LABEL = "target_activity_label"
        private const val KEY_TRIGGER_POSITION = "trigger_position"
        private const val KEY_AREA_WIDTH = "area_width_dp"
        private const val KEY_AREA_HEIGHT = "area_height_dp"
        private const val KEY_SWIPE_THRESHOLD = "swipe_threshold"
        private const val KEY_DEBUG_MODE = "debug_mode"

        const val POSITION_BOTTOM_RIGHT = 0
        const val POSITION_BOTTOM_LEFT = 1
        const val POSITION_BOTH = 2

        private const val DEFAULT_AREA_WIDTH = 80f
        private const val DEFAULT_AREA_HEIGHT = 120f
        private const val DEFAULT_SWIPE_THRESHOLD = 80
    }

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var targetAppPackage: String
        get() = prefs.getString(KEY_TARGET_APP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TARGET_APP, value).apply()

    var targetAppName: String
        get() = prefs.getString(KEY_TARGET_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TARGET_NAME, value).apply()

    var targetActivityName: String
        get() = prefs.getString(KEY_TARGET_ACTIVITY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TARGET_ACTIVITY, value).apply()


    var targetActivityLabel: String
        get() = prefs.getString(KEY_TARGET_ACTIVITY_LABEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TARGET_ACTIVITY_LABEL, value).apply()

    var triggerPosition: Int
        get() = prefs.getInt(KEY_TRIGGER_POSITION, POSITION_BOTTOM_RIGHT)
        set(value) = prefs.edit().putInt(KEY_TRIGGER_POSITION, value).apply()

    var areaWidthDp: Float
        get() = prefs.getFloat(KEY_AREA_WIDTH, DEFAULT_AREA_WIDTH)
        set(value) = prefs.edit().putFloat(KEY_AREA_WIDTH, value).apply()

    var areaHeightDp: Float
        get() = prefs.getFloat(KEY_AREA_HEIGHT, DEFAULT_AREA_HEIGHT)
        set(value) = prefs.edit().putFloat(KEY_AREA_HEIGHT, value).apply()

    var swipeThreshold: Int
        get() = prefs.getInt(KEY_SWIPE_THRESHOLD, DEFAULT_SWIPE_THRESHOLD)
        set(value) = prefs.edit().putInt(KEY_SWIPE_THRESHOLD, value).apply()

    var debugMode: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()

    fun hasTargetApp(): Boolean = targetAppPackage.isNotEmpty()

    fun saveTargetApp(packageName: String, appName: String, activityName: String = "", activityLabel: String = "") {
        prefs.edit()
            .putString(KEY_TARGET_APP, packageName)
            .putString(KEY_TARGET_NAME, appName)
            .putString(KEY_TARGET_ACTIVITY, activityName)
            .putString(KEY_TARGET_ACTIVITY_LABEL, activityLabel)
            .apply()
    }

    fun clearTargetApp() {
        prefs.edit()
            .remove(KEY_TARGET_APP)
            .remove(KEY_TARGET_NAME)
            .remove(KEY_TARGET_ACTIVITY)
            .remove(KEY_TARGET_ACTIVITY_LABEL)
            .apply()
    }
}