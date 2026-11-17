package com.rushgames.barrier.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BarrierDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.d("BarrierAdmin", "Device Administrator enabled successfully")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.d("BarrierAdmin", "Device Administrator disabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d("BarrierAdmin", "Device Administrator disable requested")
        return "Disabling Device Administrator removes its limited device-management access. It does not control uninstall or override Android session limits."
    }
}
