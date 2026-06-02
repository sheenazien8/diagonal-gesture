package com.sheenadev.diagonalgesture

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.content.pm.ActivityInfo

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val componentName: ComponentName? = null,
    val activityInfo: ActivityInfo? = null
)
