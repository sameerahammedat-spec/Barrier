package com.rushgames.barrier.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rushgames.barrier.data.model.Session
import com.rushgames.barrier.data.model.SessionStatus
import com.rushgames.barrier.ui.components.*
import com.rushgames.barrier.ui.theme.BarrierGreen
import com.rushgames.barrier.ui.theme.BarrierPurple
import com.rushgames.barrier.ui.theme.BarrierRed

@Composable
fun TodayScreen(
    onStartSession: () -> Unit,
    onNavigateSessions: () -> Unit,
    onNavigatePermissions: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Barrier",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Your focus companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(
                status = if (uiState.activeSession != null) BadgeStatus.ACTIVE else BadgeStatus.INACTIVE,
                label = if (uiState.activeSession != null) "Protected" else "Inactive"
            )
        }

        Spacer(Modifier.height(28.dp))

        // Active Session Card
        AnimatedContent(
            targetState = uiState.activeSession,
            label = "active_session"
        ) { session ->
            if (session != null) {
                ActiveSessionCard(session = session)
            } else {
                NoSessionCard(onStartSession = onStartSession)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Timer,
                value = formatFocusTime(uiState.todayFocusMinutes),
                label = "Focus Today",
                accentColor = BarrierPurple
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Block,
                value = "${uiState.todayBlockedAttempts}",
                label = "Blocked",
                accentColor = BarrierRed
            )
        }

        Spacer(Modifier.height(24.dp))

        // Permission Warnings
        if (!uiState.hasUsageStatsPermission) {
            PermissionWarningCard(
                icon = Icons.Outlined.AdminPanelSettings,
                title = "Usage Access Required",
                description = "Grant Usage Access so Barrier can monitor and block apps.",
                actionLabel = "Grant Access",
                onClick = onNavigatePermissions
            )
            Spacer(Modifier.height(12.dp))
        }

        if (!uiState.hasAccessibilityPermission) {
            PermissionWarningCard(
                icon = Icons.Outlined.Accessibility,
                title = "Accessibility Service",
                description = "Enable Barrier's Accessibility Service for stronger app blocking.",
                actionLabel = "Enable",
                onClick = onNavigatePermissions
            )
            Spacer(Modifier.height(12.dp))
        }

        // Upcoming Sessions
        if (uiState.upcomingSessions.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            BarrierSectionHeader(
                title = "Upcoming",
                action = {
                    TextButton(onClick = onNavigateSessions) {
                        Text("See all", style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            uiState.upcomingSessions.forEach { session ->
                UpcomingSessionRow(session = session)
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(100.dp)) // bottom nav clearance
    }
}

@Composable
private fun ActiveSessionCard(session: Session) {
    val totalDurationMs = session.durationMinutes * 60_000L
    BarrierCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(BadgeStatus.ACTIVE, "Active")
                    if (session.preventsCancellation) {
                        StatusBadge(BadgeStatus.WARNING, "Locked")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${session.durationMinutes} min session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.blocklistIds.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${session.blocklistIds.size} blocklist${if (session.blocklistIds.size > 1) "s" else ""} active",
                        style = MaterialTheme.typography.bodySmall,
                        color = BarrierGreen
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            SessionCountdownTimer(
                endTimeMs = session.endTime,
                totalDurationMs = totalDurationMs,
                size = 110.dp,
                strokeWidth = 6.dp
            )
        }
    }
}

@Composable
private fun NoSessionCard(onStartSession: () -> Unit) {
    BarrierCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No Active Session",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Start a session to begin protecting your focus.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStartSession,
                colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Session", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    BarrierCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PermissionWarningCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun UpcomingSessionRow(session: Session) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = BarrierPurple,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(session.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("${session.durationMinutes} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (session.preventsCancellation) {
                Icon(Icons.Outlined.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun formatFocusTime(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
