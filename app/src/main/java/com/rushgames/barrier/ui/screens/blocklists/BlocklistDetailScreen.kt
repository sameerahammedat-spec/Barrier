package com.rushgames.barrier.ui.screens.blocklists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rushgames.barrier.data.model.BlockedApp
import com.rushgames.barrier.data.model.BlockedWebsite
import com.rushgames.barrier.ui.theme.BarrierGreen
import com.rushgames.barrier.ui.theme.BarrierRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistDetailScreen(
    blocklistId: Long,
    onBack: () -> Unit,
    viewModel: BlocklistDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(blocklistId) {
        viewModel.loadBlocklist(blocklistId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Apps, 1 = Websites
    var searchQuery by remember { mutableStateOf("") }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.blocklist?.name ?: "Edit Blocklist", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Header
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BarrierGreen
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Blocked Apps (${uiState.blockedApps.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Blocked Sites (${uiState.blockedWebsites.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Language, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                // Apps Tab
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        placeholder = { Text("Search installed apps to block...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    val blockedPackages = remember(uiState.blockedApps) {
                        uiState.blockedApps.map { it.packageName }.toSet()
                    }

                    val filteredApps = remember(searchQuery, uiState.allInstalledApps) {
                        if (searchQuery.isBlank()) uiState.allInstalledApps
                        else uiState.allInstalledApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            val isBlocked = blockedPackages.contains(app.packageName)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBlocked) BarrierGreen.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(app.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    if (isBlocked) {
                                        val blockedItem = uiState.blockedApps.find { it.packageName == app.packageName }
                                        IconButton(onClick = { blockedItem?.let { viewModel.removeAppFromBlocklist(it) } }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = BarrierRed)
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.addAppToBlocklist(app) },
                                            colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("Block", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Websites Tab
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Button(
                        onClick = { showAddWebsiteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Website Domain")
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.blockedWebsites) { website ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = BarrierGreen)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(website.domain, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    }
                                    IconButton(onClick = { viewModel.removeWebsiteFromBlocklist(website) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BarrierRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddWebsiteDialog) {
        var domainInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddWebsiteDialog = false },
            title = { Text("Add Website to Blocklist") },
            text = {
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    label = { Text("Domain (e.g. reddit.com)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addWebsiteToBlocklist(domainInput)
                        showAddWebsiteDialog = false
                    },
                    enabled = domainInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen)
                ) {
                    Text("Add Domain")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWebsiteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
