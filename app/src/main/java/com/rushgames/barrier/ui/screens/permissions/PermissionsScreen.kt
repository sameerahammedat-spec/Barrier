package com.rushgames.barrier.ui.screens.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rushgames.barrier.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var hasUsageStats by remember { mutableStateOf(PermissionManager.hasUsageStatsPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionManager.hasAccessibilityPermission(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionManager.hasOverlayPermission(context)) }
    var hasNotificationPolicy by remember { mutableStateOf(PermissionManager.hasNotificationPolicyPermission(context)) }
    var isBatteryIgnored by remember { mutableStateOf(PermissionManager.isBatteryOptimizationIgnored(context)) }

    // Re-check permissions on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = PermissionManager.hasUsageStatsPermission(context)
                hasAccessibility = PermissionManager.hasAccessibilityPermission(context)
                hasOverlay = PermissionManager.hasOverlayPermission(context)
                hasNotificationPolicy = PermissionManager.hasNotificationPolicyPermission(context)
                isBatteryIgnored = PermissionManager.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protection Permissions", fontWeight = FontWeight.SemiBold) },
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Enforcement Protection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "These permissions let Barrier check some app and browser activity. Coverage is limited, and Android can stop or revoke the services at any time.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                PermissionCard(
                    title = "Usage Access",
                    description = "May let Barrier detect some foreground app switches. Android can limit or revoke access.",
                    icon = Icons.Default.QueryStats,
                    isGranted = hasUsageStats,
                    onClick = { PermissionManager.openUsageStatsSettings(context) }
                )
            }

            item {
                PermissionCard(
                    title = "Accessibility Service",
                    description = "Can redirect some blocked app launches and inspect accessible content in supported browsers. This is not device-wide enforcement.",
                    icon = Icons.Default.AccessibilityNew,
                    isGranted = hasAccessibility,
                    onClick = { PermissionManager.openAccessibilitySettings(context) }
                )
            }

            item {
                PermissionCard(
                    title = "Display Over Other Apps",
                    description = "Allows a restriction overlay in supported cases. Android may limit background display.",
                    icon = Icons.Default.Layers,
                    isGranted = hasOverlay,
                    onClick = { PermissionManager.openOverlaySettings(context) }
                )
            }

            item {
                PermissionCard(
                    title = "Do Not Disturb Access",
                    description = "Allows Barrier to suppress notification distractions during deep work focus sessions.",
                    icon = Icons.Default.Notifications,
                    isGranted = hasNotificationPolicy,
                    onClick = { PermissionManager.openNotificationPolicySettings(context) }
                )
            }

            item {
                PermissionCard(
                    title = "Battery Optimization Exemption",
                    description = "May improve timer reliability; Android can still stop background work.",
                    icon = Icons.Default.BatteryFull,
                    isGranted = isBatteryIgnored,
                    onClick = { PermissionManager.openBatteryOptimizationSettings(context) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isGranted) "Granted" else "Action Required",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                    )
                }

                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Enable", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
