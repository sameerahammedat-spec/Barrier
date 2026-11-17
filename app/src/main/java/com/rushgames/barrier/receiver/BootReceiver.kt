package com.rushgames.barrier.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rushgames.barrier.data.model.SessionStatus
import com.rushgames.barrier.data.repository.SessionRepository
import com.rushgames.barrier.service.BlockingService
import com.rushgames.barrier.util.PermissionManager
import com.rushgames.barrier.worker.SessionExpirationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionRepository: SessionRepository

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            Log.d("BarrierBoot", "Device reboot detected. Restoring active Barrier sessions...")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val now = System.currentTimeMillis()
                    sessionRepository.completeExpiredSessions(now)
                    val activeSession = sessionRepository.getActiveSessionOnce()
                    if (activeSession != null && activeSession.status == SessionStatus.ACTIVE) {
                        Log.d("BarrierBoot", "Restoring active session: '${activeSession.name}'")
                        val remainingMs = activeSession.endTime - now

                        // Keep the durable expiry even if Android refuses a service restart.
                        SessionExpirationWorker.scheduleExpiration(context, activeSession.id, remainingMs)

                        if (PermissionManager.hasUsageStatsPermission(context)) {
                            try {
                                val serviceIntent = Intent(context, BlockingService::class.java).apply {
                                    this.action = BlockingService.ACTION_START
                                }
                                context.startForegroundService(serviceIntent)
                            } catch (serviceStartError: Exception) {
                                Log.e("BarrierBoot", "Could not restore app-usage monitoring", serviceStartError)
                            }
                        } else {
                            Log.w("BarrierBoot", "Usage Access is not granted; app-usage monitoring was not restarted")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BarrierBoot", "Error restoring active session on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
