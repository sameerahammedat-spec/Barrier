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
import com.rushgames.barrier.data.model.SessionDurationPolicy
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
                        enabled = !uiState.isSaving && SessionDurationPolicy.isValid(SessionDurationPolicy.toMinutes(uiState.durationHours, uiState.durationMinutes)),
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

private enum class DurationUnit { MINUTES, HOURS, DAYS }

@Composable
private fun DurationPicker(hours: Int, minutes: Int, onDurationChange: (Int, Int) -> Unit) {
    var unit by remember { mutableStateOf(DurationUnit.HOURS) }
    val totalMinutes = SessionDurationPolicy.toMinutes(hours, minutes)
    val amount = when (unit) {
        DurationUnit.MINUTES -> totalMinutes.coerceIn(5, 59)
        DurationUnit.HOURS -> ((totalMinutes + 59) / 60).coerceIn(1, 168)
        DurationUnit.DAYS -> ((totalMinutes + 1_439) / 1_440).coerceIn(1, 7)
    }
    val range = when (unit) {
        DurationUnit.MINUTES -> 5f..59f
        DurationUnit.HOURS -> 1f..168f
        DurationUnit.DAYS -> 1f..7f
    }
    val steps = when (unit) {
        DurationUnit.MINUTES -> 53
        DurationUnit.HOURS -> 166
        DurationUnit.DAYS -> 5
    }
    val durationLabel = when {
        totalMinutes < 60 -> totalMinutes.toString() + " minutes"
        totalMinutes % 1_440 == 0 -> (totalMinutes / 1_440).toString() + " days"
        totalMinutes % 60 == 0 -> (totalMinutes / 60).toString() + " hours"
        else -> (totalMinutes / 60).toString() + "h " + (totalMinutes % 60).toString() + "m"
    }
    val estimatedEnd = java.text.SimpleDateFormat("EEE, h:mm a", java.util.Locale.getDefault())
        .format(java.util.Date(System.currentTimeMillis() + totalMinutes.coerceAtLeast(0) * 60_000L))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(DurationUnit.MINUTES to "Minutes", DurationUnit.HOURS to "Hours", DurationUnit.DAYS to "Days").forEach { (option, label) ->
                FilterChip(
                    selected = unit == option,
                    onClick = {
                        unit = option
                        val nextTotal = when (option) {
                            DurationUnit.MINUTES -> totalMinutes.coerceIn(5, 59)
                            DurationUnit.HOURS -> ((totalMinutes + 59) / 60).coerceIn(1, 168) * 60
                            DurationUnit.DAYS -> ((totalMinutes + 1_439) / 1_440).coerceIn(1, 7) * 1_440
                        }
                        onDurationChange(nextTotal / 60, nextTotal % 60)
                    },
                    label = { Text(label) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple(0, 25, "Quick · 25m"), Triple(1, 0, "1 hour"), Triple(2, 0, "2 hours"), Triple(3, 0, "3 hours")).forEach { (presetHours, presetMinutes, label) ->
                FilterChip(
                    selected = hours == presetHours && minutes == presetMinutes,
                    onClick = {
                        onDurationChange(presetHours, presetMinutes)
                        unit = if (presetHours == 0) DurationUnit.MINUTES else DurationUnit.HOURS
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Text("Custom duration: " + durationLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Slider(
            value = amount.toFloat(),
            onValueChange = { raw ->
                val selected = raw.toInt()
                val nextTotal = when (unit) {
                    DurationUnit.MINUTES -> selected
                    DurationUnit.HOURS -> selected * 60
                    DurationUnit.DAYS -> selected * 1_440
                }
                onDurationChange(nextTotal / 60, nextTotal % 60)
            },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = BarrierPurple, activeTrackColor = BarrierPurple)
        )
        Text(
            "Estimated end: " + estimatedEnd + " · Minimum 5 minutes · Maximum 7 days",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
