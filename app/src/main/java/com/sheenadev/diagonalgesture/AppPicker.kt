package com.sheenadev.diagonalgesture

import android.content.Context
import android.content.Intent
import android.provider.Settings

object AppPicker {

    fun getInstalledApps(context: Context): List<android.content.pm.ResolveInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(mainIntent, 0)
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
}
