package com.sheenadev.diagonalgesture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Boot received: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.huawei.android.launcher.action.READY") {
            
            // Check if service was enabled before reboot
            val prefsManager = GesturePreferenceManager(context)
            
            if (prefsManager.isServiceEnabled) {
                Log.d(TAG, "Service was enabled, starting...")
                GestureOverlayService.startService(context)
            } else {
                Log.d(TAG, "Service was disabled, skipping auto-start")
            }
        }
    }
}