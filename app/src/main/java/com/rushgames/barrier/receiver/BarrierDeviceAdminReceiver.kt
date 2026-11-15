package com.rushgames.barrier.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Admin Receiver.
 *
 * Phase 1: Stub.
 * Phase 6: Will implement Device Owner / Device Admin management for:
 *   - Lock Task Mode (kiosk-style session launcher)
 *   - Stronger application restriction
 *   - Profile management
 *
 * Note: Device Owner requires special provisioning (ADB or QR setup).
 * Standard consumer install supports Device Admin only.
 */
class BarrierDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Log.d("BarrierAdmin", "Device admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.d("BarrierAdmin", "Device admin disabled")
    }
}
