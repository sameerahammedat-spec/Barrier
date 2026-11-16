package com.rushgames.barrier.service

import android.content.Intent
import android.net.VpnService
import android.util.Log

/**
 * VPN filtering is intentionally unavailable until packet forwarding and DNS responses are
 * implemented. Establishing a TUN device without those pieces would interrupt device traffic.
 */
class BarrierVpnService : VpnService() {

    companion object {
        const val ACTION_START_VPN = "ACTION_START_VPN"
        const val ACTION_STOP_VPN = "ACTION_STOP_VPN"
        private const val TAG = "BarrierVPN"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VPN -> Log.w(TAG, "VPN filtering is unavailable; no tunnel was established")
            ACTION_STOP_VPN, null -> Unit
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
