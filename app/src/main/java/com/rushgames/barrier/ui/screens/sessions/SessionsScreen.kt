package com.rushgames.barrier.ui.screens.sessions

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rushgames.barrier.data.model.Session
import com.rushgames.barrier.data.model.SessionStatus
import com.rushgames.barrier.ui.components.*
import com.rushgames.barrier.ui.theme.BarrierGreen
import com.rushgames.barrier.ui.theme.BarrierPurple
import com.rushgames.barrier.ui.theme.BarrierRed
import com.rushgames.barrier.ui.theme.BarrierOrange
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    onCreateSession: () -> Unit,
    onSessionClick: (Long) -> Unit,
    viewModel: SessionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateSession,
                containerColor = BarrierGreen,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Outlined.Add, "New Session") },
                text = { Text("New Session", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Sessions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage your focus sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BarrierGreen)
                }
            } else {
                // Active
                if (uiState.activeSessions.isNotEmpty()) {
                    BarrierSectionHeader("Active")
                    Spacer(Modifier.height(8.dp))
                    uiState.activeSessions.forEach { session ->
                        SessionCard(
                            session = session,
                            onClick = { onSessionClick(session.id) },
                            onCancel = { viewModel.cancelSession(session) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Upcoming
                if (uiState.upcomingSessions.isNotEmpty()) {
                    BarrierSectionHeader("Scheduled")
                    Spacer(Modifier.height(8.dp))
                    uiState.upcomingSessions.forEach { session ->
                        SessionCard(
                            session = session,
                            onClick = { onSessionClick(session.id) },
                            onDelete = { viewModel.deleteSession(session) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // History
                if (uiState.history.isNotEmpty()) {
                    BarrierSectionHeader("History")
                    Spacer(Modifier.height(8.dp))
                    uiState.history.forEach { session ->
                        SessionCard(
                            session = session,
                            onClick = { onSessionClick(session.id) },
                            onDelete = { viewModel.deleteSession(session) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // Empty state
                if (uiState.activeSessions.isEmpty() && uiState.upcomingSessions.isEmpty() && uiState.history.isEmpty()) {
                    SessionsEmptyState(onCreateSession = onCreateSession)
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    val (badgeStatus, badgeLabel) = when (session.status) {
        SessionStatus.ACTIVE -> Pair(BadgeStatus.ACTIVE, "Active")
        SessionStatus.IDLE -> Pair(BadgeStatus.WARNING, "Scheduled")
        SessionStatus.COMPLETED -> Pair(BadgeStatus.INACTIVE, "Completed")
        SessionStatus.CANCELLED -> Pair(BadgeStatus.INACTIVE, "Cancelled")
        SessionStatus.PAUSED -> Pair(BadgeStatus.WARNING, "Paused")
    }

    BarrierCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(badgeStatus, badgeLabel)
                    if (session.preventsCancellation) {
                        Icon(Icons.Outlined.Lock, contentDescription = "Locked",
                            modifier = Modifier.size(14.dp),
                            tint = BarrierOrange)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SessionInfoChip(Icons.Outlined.Timer, "${session.durationMinutes}m")
                    if (session.blocklistIds.isNotEmpty()) {
                        SessionInfoChip(Icons.Outlined.Block, "${session.blocklistIds.size} list${if (session.blocklistIds.size > 1) "s" else ""}")
                    }
                    if (session.isStrictMode) {
                        SessionInfoChip(Icons.Outlined.Security, "Strict")
                    }
                }
                if (session.status == SessionStatus.ACTIVE) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Ends ${sdf.format(Date(session.endTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BarrierGreen
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (session.status == SessionStatus.ACTIVE && onCancel != null && !session.preventsCancellation) {
                        DropdownMenuItem(
                            text = { Text("Cancel Session", color = BarrierRed) },
                            leadingIcon = { Icon(Icons.Outlined.Stop, contentDescription = null, tint = BarrierRed) },
                            onClick = { onCancel(); showMenu = false }
                        )
                    }
                    if (session.status != SessionStatus.ACTIVE && onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = BarrierRed) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = BarrierRed) },
                            onClick = { onDelete(); showMenu = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SessionsEmptyState(onCreateSession: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Outlined.PlayCircle, contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        Text("No Sessions Yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("Create a focus session to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = onCreateSession,
            colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create First Session")
        }
    }
}
