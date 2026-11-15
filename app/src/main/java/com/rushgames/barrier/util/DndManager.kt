package com.rushgames.barrier.util

import android.app.NotificationManager
import android.content.Context
import android.util.Log

object DndManager {

    private const val TAG = "DndManager"

    fun isDndAccessGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun setDndMode(context: Context, enableDnd: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            Log.d(TAG, "DND notification policy access not granted.")
            return
        }

        try {
            val filter = if (enableDnd) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            nm.setInterruptionFilter(filter)
            Log.d(TAG, "Do Not Disturb set to: $enableDnd")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting DND mode", e)
        }
    }
}
