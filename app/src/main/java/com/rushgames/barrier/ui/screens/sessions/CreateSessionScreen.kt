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
import com.rushgames.barrier.data.model.SessionBreakSettings
import com.rushgames.barrier.data.model.WebsiteFilterMode
import com.rushgames.barrier.ui.theme.BarrierGreen
import com.rushgames.barrier.ui.theme.BarrierPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionScreen(
    onBack: () -> Unit,
    onSessionCreated: () -> Unit,
    viewModel: CreateSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAppPicker by remember { mutableStateOf(false) }
    var newWebsite by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New Session", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick = { viewModel.createSession(onSessionCreated) },
                        modifier = Modifier.weight(2f),
                        enabled = !uiState.isSaving && (uiState.durationHours > 0 || uiState.durationMinutes > 0),
                        colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Session", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            uiState.saveError?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Name
            SectionLabel("Session Name")
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::setName,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. Deep Work") }
            )

            // Duration
            SectionLabel("Duration")
            DurationPicker(
                hours = uiState.durationHours,
                minutes = uiState.durationMinutes,
                onDurationChange = viewModel::setDuration
            )

            // Blocklists
            if (uiState.availableBlocklists.isNotEmpty()) {
                SectionLabel("Blocklists")
                uiState.availableBlocklists.forEach { bl ->
                    val selected = uiState.selectedBlocklistIds.contains(bl.id)
                    BlocklistToggleRow(
                        name = bl.name,
                        appCount = bl.appCount,
                        selected = selected,
                        onToggle = { viewModel.toggleBlocklist(bl.id) }
                    )
                }
            }

            // Allowed Apps (Session Allowlist)
            SectionLabel("Apps Allowed in Session")
            Text(
                text = "Only these apps will be accessible during the session (leave empty to block nothing).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { showAppPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (uiState.allowedApps.isEmpty()) "Select Allowed Apps"
                    else "${uiState.allowedApps.size} app${if (uiState.allowedApps.size > 1) "s" else ""} selected"
                )
            }

            // Website Allowlist
            SectionLabel("Website Filter Mode")
            WebsiteFilterModeSelector(
                selected = uiState.websiteFilterMode,
                onSelect = viewModel::setWebsiteFilterMode
            )

            if (uiState.websiteFilterMode == WebsiteFilterMode.ALLOWLIST_ONLY ||
                uiState.websiteFilterMode == WebsiteFilterMode.COMBINED) {
                SectionLabel("Allowed Websites")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newWebsite,
                        onValueChange = { newWebsite = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g. github.com") }
                    )
                    IconButton(
                        onClick = {
                            viewModel.addAllowedWebsite(newWebsite)
                            newWebsite = ""
                        }
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add", tint = BarrierGreen)
                    }
                }
                uiState.allowedWebsites.forEach { domain ->
                    WebsiteChip(domain = domain, onRemove = { viewModel.removeAllowedWebsite(domain) })
                }
            }

            // Options
            SectionLabel("Session Options")
            OptionToggleRow(
                icon = Icons.Outlined.Lock,
                title = "Locked Mode",
                description = "Blocks in-app cancellation; Android can still stop or uninstall Barrier.",
                checked = uiState.isLocked,
                onToggle = viewModel::toggleLocked
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            OptionToggleRow(
                icon = Icons.Outlined.Security,
                title = "Strict Mode",
                description = "Locks this session in Barrier until it expires; Android can still stop or uninstall the app.",
                checked = uiState.isStrictMode,
                onToggle = viewModel::toggleStrictMode
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            OptionToggleRow(
                icon = Icons.Outlined.Block,
                title = "Block Adult Content",
                description = "Enable adult content filter during session",
                checked = uiState.blockAdultContent,
                onToggle = viewModel::toggleAdultContent
            )

            // Break Settings
            SectionLabel("Breaks")
            BreakSettingsRow(
                settings = uiState.breakSettings,
                onSettingsChange = viewModel::setBreakSettings
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    // App picker bottom sheet
    if (showAppPicker) {
        AppPickerSheet(
            installedApps = uiState.installedApps,
            selectedApps = uiState.allowedApps,
            onToggleApp = viewModel::toggleAllowedApp,
            onDismiss = { showAppPicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DurationPicker(hours: Int, minutes: Int, onDurationChange: (Int, Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            Pair(1, "Quick · 25m") to { onDurationChange(0, 25) },
            Pair(2, "1 hour") to { onDurationChange(1, 0) },
            Pair(3, "2 hours") to { onDurationChange(2, 0) },
            Pair(4, "3 hours") to { onDurationChange(3, 0) }
        ).forEach { (labelPair, action) ->
            val (_, label) = labelPair
            val durationH = when (label) {
                "Quick · 25m" -> 0; "1 hour" -> 1; "2 hours" -> 2; else -> 3
            }
            val durationM = when (label) { "Quick · 25m" -> 25; else -> 0 }
            val isSelected = hours == durationH && minutes == durationM
            FilterChip(
                selected = isSelected,
                onClick = action,
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BarrierGreen.copy(alpha = 0.15f),
                    selectedLabelColor = BarrierGreen
                )
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    // Custom duration sliders
    Text("Custom: ${hours}h ${minutes}m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Slider(
        value = hours.toFloat(),
        onValueChange = { onDurationChange(it.toInt(), minutes) },
        valueRange = 0f..12f,
        steps = 11,
        colors = SliderDefaults.colors(thumbColor = BarrierPurple, activeTrackColor = BarrierPurple)
    )
}

@Composable
private fun BlocklistToggleRow(name: String, appCount: Int, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selected) BarrierGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (appCount > 0) Text("$appCount apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = BarrierGreen)
        )
    }
}

@Composable
private fun WebsiteFilterModeSelector(
    selected: WebsiteFilterMode,
    onSelect: (WebsiteFilterMode) -> Unit
) {
    val modes = listOf(
        WebsiteFilterMode.BLOCKLIST to "Block Selected",
        WebsiteFilterMode.ALLOWLIST_ONLY to "Allowlist Only",
        WebsiteFilterMode.ADULT_PROTECTION to "Adult Protection",
        WebsiteFilterMode.COMBINED to "Combined"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        modes.forEach { (mode, label) ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BarrierPurple.copy(alpha = 0.15f),
                    selectedLabelColor = BarrierPurple
                )
            )
        }
    }
}

@Composable
private fun WebsiteChip(domain: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(domain, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OptionToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (checked) BarrierGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = BarrierGreen)
        )
    }
}

@Composable
private fun BreakSettingsRow(
    settings: SessionBreakSettings,
    onSettingsChange: (SessionBreakSettings) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Coffee, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (settings.allowBreaks) BarrierGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text("Allow Breaks", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                if (settings.allowBreaks) {
                    Text("${settings.breakDurationMinutes}min × ${settings.breaksPerSession} break${if (settings.breaksPerSession > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Switch(
            checked = settings.allowBreaks,
            onCheckedChange = { onSettingsChange(settings.copy(allowBreaks = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = BarrierGreen)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    installedApps: List<com.rushgames.barrier.data.model.AppInfo>,
    selectedApps: List<String>,
    onToggleApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Select Allowed Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${selectedApps.size} selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Column(modifier = Modifier.height(400.dp).verticalScroll(rememberScrollState())) {
                installedApps.forEach { app ->
                    val isSelected = selectedApps.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleApp(app.packageName) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                if (app.isBrowser) Icons.Outlined.Language else Icons.Outlined.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isSelected) BarrierGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(app.appName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleApp(app.packageName) },
                            colors = CheckboxDefaults.colors(checkedColor = BarrierGreen)
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Done (${selectedApps.size} selected)") }
            Spacer(Modifier.height(32.dp))
        }
    }
}
