package com.rushgames.barrier.ui.screens.protection

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushgames.barrier.service.BarrierVpnService

enum class ProtectionProfile(val title: String, val description: String) {
    BASIC("Basic Protection", "Blocks known adult sites via Level 1 URL matching."),
    STRONG("Strong Protection", "Level 1 URL matching + Level 2 Adult Content Database lookup."),
    STRICT("Strict Protection", "3-Level Protection: URL Matching, Adult DB Search, and Deep HTML Content Extraction (Headers, Body, Images & Tables)."),
    ALLOWLIST_ONLY("Allowlist Only Mode", "Restricts web browsing exclusively to your pre-approved domain list.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionCenterScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedProfile by remember { mutableStateOf(ProtectionProfile.STRICT) }
    var adultBlockingEnabled by remember { mutableStateOf(true) }
    var level3DeepScanEnabled by remember { mutableStateOf(true) }
    var safeSearchEnabled by remember { mutableStateOf(true) }
    var youtubeRestrictedEnabled by remember { mutableStateOf(true) }
    var unsupportedBrowserBlockingEnabled by remember { mutableStateOf(true) }
    var vpnDnsFilterEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protection Center", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "3-Level Adult Content Shield",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "URL matching, adult database search, and deep content inspection.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // Protection Profile Selector
            item {
                Text(
                    text = "PROTECTION PROFILE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                ProtectionProfile.entries.forEach { profile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = { selectedProfile = profile },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedProfile == profile)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProfile == profile,
                                onClick = { selectedProfile = profile }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = profile.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Toggles
            item {
                Text(
                    text = "ENFORCEMENT MODULES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                ProtectionToggleCard(
                    title = "L1 & L2 Adult Filter (URL & Database)",
                    subtitle = "Blocks adult URLs, domains, and explicit search query terms.",
                    icon = Icons.Default.Block,
                    checked = adultBlockingEnabled,
                    onCheckedChange = { adultBlockingEnabled = it }
                )
            }

            item {
                ProtectionToggleCard(
                    title = "L3 Deep Content Extractor",
                    subtitle = "Extracts headers, body, images & tables to block hidden explicit media.",
                    icon = Icons.Default.Security,
                    checked = level3DeepScanEnabled,
                    onCheckedChange = { level3DeepScanEnabled = it }
                )
            }

            item {
                ProtectionToggleCard(
                    title = "Search Engine SafeSearch",
                    subtitle = "Enforces Strict SafeSearch on Google, Bing & DuckDuckGo.",
                    icon = Icons.Default.Search,
                    checked = safeSearchEnabled,
                    onCheckedChange = { safeSearchEnabled = it }
                )
            }

            item {
                ProtectionToggleCard(
                    title = "YouTube Restricted Mode",
                    subtitle = "Suppresses potentially mature videos and comments.",
                    icon = Icons.Default.Public,
                    checked = youtubeRestrictedEnabled,
                    onCheckedChange = { youtubeRestrictedEnabled = it }
                )
            }

            item {
                ProtectionToggleCard(
                    title = "Unsupported Browser Blocking",
                    subtitle = "Restricts browsers that cannot be reliably filtered.",
                    icon = Icons.Default.Lock,
                    checked = unsupportedBrowserBlockingEnabled,
                    onCheckedChange = { unsupportedBrowserBlockingEnabled = it }
                )
            }

            item {
                ProtectionToggleCard(
                    title = "Local VPN DNS Filter",
                    subtitle = "Runs high-speed local TUN interface for instant domain blocking.",
                    icon = Icons.Default.VpnKey,
                    checked = vpnDnsFilterEnabled,
                    onCheckedChange = { enabled ->
                        vpnDnsFilterEnabled = enabled
                        val intent = Intent(context, BarrierVpnService::class.java).apply {
                            action = if (enabled) BarrierVpnService.ACTION_START_VPN else BarrierVpnService.ACTION_STOP_VPN
                        }
                        if (enabled) context.startService(intent) else context.startService(intent)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProtectionToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
