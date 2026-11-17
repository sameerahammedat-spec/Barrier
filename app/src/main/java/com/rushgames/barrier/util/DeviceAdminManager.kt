package com.rushgames.barrier.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.rushgames.barrier.receiver.BarrierDeviceAdminReceiver

object DeviceAdminManager {

    fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(context, BarrierDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(getAdminComponent(context))
    }

    fun openDeviceAdminSettings(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponent(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Barrier's Device Administrator access does not prevent app uninstallation. Full device lockdown requires managed-device provisioning."
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
