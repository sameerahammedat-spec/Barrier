package com.rushgames.barrier.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.rushgames.barrier.data.dao.StatsDao
import com.rushgames.barrier.data.model.AttemptType
import com.rushgames.barrier.data.model.BlockedAttempt
import com.rushgames.barrier.data.model.WebsiteFilterMode
import com.rushgames.barrier.data.repository.SessionRepository
import com.rushgames.barrier.filter.AdultContentFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class BarrierVpnService : VpnService() {

    companion object {
        const val ACTION_START_VPN = "ACTION_START_VPN"
        const val ACTION_STOP_VPN = "ACTION_STOP_VPN"
    }

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var statsDao: StatsDao

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VPN -> startVpnTunnel()
            ACTION_STOP_VPN -> stopVpnTunnel()
        }
        return START_STICKY
    }

    private fun startVpnTunnel() {
        if (isRunning) return
        isRunning = true

        try {
            val builder = Builder()
                .setSession("Barrier Local DNS Protection")
                .addAddress("10.1.10.1", 24)
                .addDnsServer("1.1.1.1") // Upstream Cloudflare DNS
                .addDnsServer("8.8.8.8") // Upstream Google DNS

            vpnInterface = builder.establish()
            Log.d("BarrierVPN", "Local VPN Tunnel established successfully")

            startDnsProcessingLoop()
        } catch (e: Exception) {
            Log.e("BarrierVPN", "Failed to establish VPN interface", e)
            stopVpnTunnel()
        }
    }

    private fun startDnsProcessingLoop() {
        serviceScope.launch {
            val pfd = vpnInterface ?: return@launch
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteArray(32767)

            while (isActive && isRunning) {
                try {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        // Inspect IP packet for UDP DNS (port 53)
                        processPacket(buffer, length)
                    }
                } catch (e: Exception) {
                    if (!isRunning) break
                }
            }
        }
    }

    private suspend fun processPacket(buffer: ByteArray, length: Int) {
        if (length < 28) return

        val protocol = buffer[9].toInt() and 0xFF
        if (protocol != 17) return // Protocol 17 = UDP

        val ipHeaderLen = (buffer[0].toInt() and 0x0F) * 4
        val destPort = ((buffer[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (buffer[ipHeaderLen + 3].toInt() and 0xFF)

        if (destPort == 53) { // DNS Query
            val dnsOffset = ipHeaderLen + 8
            val domain = extractDomainFromDnsPayload(buffer, dnsOffset, length) ?: return
            handleDnsDomain(domain)
        }
    }

    private suspend fun handleDnsDomain(domain: String) {
        val cleanDomain = domain.lowercase(Locale.ROOT).trim()
        val activeSession = sessionRepository.getActiveSessionOnce() ?: return

        var shouldBlock = false
        var reason = ""

        // 1. Level 1 & Level 2 Adult Content Filtering
        if (activeSession.blockAdultContent || activeSession.websiteFilterMode == WebsiteFilterMode.ADULT_PROTECTION) {
            val eval = AdultContentFilter.evaluateUrlOrDomainFast(cleanDomain)
            if (eval.isAdult) {
                shouldBlock = true
                reason = "Adult Content Filter (L${eval.levelTriggered}: ${eval.reason})"
            }
        }

        // 2. Allowlist Only Mode
        if (!shouldBlock && (activeSession.websiteFilterMode == WebsiteFilterMode.ALLOWLIST_ONLY || activeSession.websiteFilterMode == WebsiteFilterMode.COMBINED)) {
            if (activeSession.allowedWebsites.isNotEmpty()) {
                val isAllowed = activeSession.allowedWebsites.any { cleanDomain.endsWith(it.lowercase()) }
                if (!isAllowed) {
                    shouldBlock = true
                    reason = "Website Allowlist Mode"
                }
            }
        }

        // 3. Blocklist Mode
        if (!shouldBlock && (activeSession.websiteFilterMode == WebsiteFilterMode.BLOCKLIST || activeSession.websiteFilterMode == WebsiteFilterMode.COMBINED)) {
            if (activeSession.blockedWebsites.any { cleanDomain.contains(it.lowercase()) }) {
                shouldBlock = true
                reason = "Blocked Website List"
            }
        }

        if (shouldBlock) {
            Log.d("BarrierVPN", "Blocking domain in DNS: $cleanDomain ($reason)")
            statsDao.insertBlockedAttempt(
                BlockedAttempt(
                    sessionId = activeSession.id,
                    type = AttemptType.WEBSITE,
                    packageNameOrDomain = cleanDomain,
                    displayName = cleanDomain
                )
            )
        }
    }

    private fun extractDomainFromDnsPayload(buffer: ByteArray, offset: Int, length: Int): String? {
        return try {
            val sb = StringBuilder()
            var pos = offset + 12 // Skip DNS header (12 bytes)
            while (pos < length) {
                val len = buffer[pos].toInt() and 0xFF
                if (len == 0) break
                pos++
                if (pos + len > length) break
                if (sb.isNotEmpty()) sb.append(".")
                sb.append(String(buffer, pos, len, Charsets.US_ASCII))
                pos += len
            }
            if (sb.isNotEmpty()) sb.toString() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun stopVpnTunnel() {
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
    }

    override fun onRevoke() {
        stopVpnTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
