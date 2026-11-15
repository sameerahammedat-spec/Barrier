package com.rushgames.barrier.ui.screens.blocklists

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
import com.rushgames.barrier.data.model.Blocklist
import com.rushgames.barrier.data.model.BlocklistCategory
import com.rushgames.barrier.ui.components.BarrierCard
import com.rushgames.barrier.ui.components.BarrierSectionHeader
import com.rushgames.barrier.ui.components.BadgeStatus
import com.rushgames.barrier.ui.components.StatusBadge
import com.rushgames.barrier.ui.theme.BarrierGreen
import com.rushgames.barrier.ui.theme.BarrierRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistsScreen(
    onCreateBlocklist: () -> Unit,
    onBlocklistClick: (Long) -> Unit,
    viewModel: BlocklistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<Blocklist?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateBlocklist,
                containerColor = BarrierGreen,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Outlined.Add, "New Blocklist") },
                text = { Text("New Blocklist", fontWeight = FontWeight.SemiBold) }
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
            Text("Blocklists", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Define what gets blocked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BarrierGreen)
                }
            } else if (uiState.blocklists.isEmpty()) {
                BlocklistsEmptyState(onCreateBlocklist)
            } else {
                BarrierSectionHeader("Your Blocklists")
                Spacer(Modifier.height(8.dp))
                uiState.blocklists.forEach { blocklist ->
                    BlocklistCard(
                        blocklist = blocklist,
                        onClick = { onBlocklistClick(blocklist.id) },
                        onToggle = { viewModel.toggleEnabled(blocklist) },
                        onDuplicate = { viewModel.duplicateBlocklist(blocklist) },
                        onDelete = { deleteTarget = blocklist }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(100.dp))
        }
    }

    // Delete confirmation
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Blocklist") },
            text = { Text("Are you sure you want to delete \"${target.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBlocklist(target)
                    deleteTarget = null
                }) {
                    Text("Delete", color = BarrierRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BlocklistCard(
    blocklist: Blocklist,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val categoryIcon = when (blocklist.category) {
        BlocklistCategory.SOCIAL_MEDIA -> Icons.Outlined.People
        BlocklistCategory.NEWS -> Icons.Outlined.Article
        BlocklistCategory.ENTERTAINMENT -> Icons.Outlined.Movie
        BlocklistCategory.GAMING -> Icons.Outlined.SportsEsports
        BlocklistCategory.ADULT -> Icons.Outlined.Block
        BlocklistCategory.SHOPPING -> Icons.Outlined.ShoppingCart
        else -> Icons.Outlined.List
    }

    BarrierCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    categoryIcon,
                    contentDescription = null,
                    tint = if (blocklist.isEnabled) BarrierGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp).padding(top = 2.dp)
                )
                Column {
                    Text(blocklist.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (blocklist.appCount > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Apps, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${blocklist.appCount} apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (blocklist.websiteCount > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${blocklist.websiteCount} sites", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusBadge(
                        status = if (blocklist.isEnabled) BadgeStatus.ACTIVE else BadgeStatus.INACTIVE,
                        label = if (blocklist.isEnabled) "Enabled" else "Disabled"
                    )
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Switch(
                    checked = blocklist.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedTrackColor = BarrierGreen)
                )
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = { onClick(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = { onDuplicate(); showMenu = false }
                        )
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
private fun BlocklistsEmptyState(onCreateBlocklist: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Outlined.PlaylistAdd, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        Text("No Blocklists", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("Create your first blocklist to start blocking apps and websites.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = onCreateBlocklist,
            colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create Blocklist")
        }
    }
}
