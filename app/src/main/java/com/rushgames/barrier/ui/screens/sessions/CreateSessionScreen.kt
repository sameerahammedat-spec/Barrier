package com.rushgames.barrier.ui.screens.sessions

import androidx.compose.foundation.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rushgames.barrier.data.model.SessionBreakSettings
import com.rushgames.barrier.data.model.AppFilterMode
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
    var durationValid by remember { mutableStateOf(true) }

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
                        enabled = !uiState.isSaving && durationValid && SessionDurationPolicy.isValid(SessionDurationPolicy.toMinutes(uiState.durationHours, uiState.durationMinutes)),
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
                onDurationChange = viewModel::setDuration,
                onValidityChange = { durationValid = it }
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

            // App rules are independent of website filtering.
            SectionLabel("App Blocking")
            AppFilterModeSelector(
                selected = uiState.appFilterMode,
                onSelect = viewModel::setAppFilterMode
            )
            Text(
                text = when (uiState.appFilterMode) {
                    AppFilterMode.NONE -> "App blocking is off. Your installed apps remain available, subject to other enabled rules."
                    AppFilterMode.BLOCK_SELECTED -> "Only the apps you select below are blocked during this session."
                    AppFilterMode.ALLOWLIST_ONLY -> "Only the apps you select below remain accessible. Other supported launchable apps are blocked."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.appFilterMode != AppFilterMode.NONE) {
                val isBlockMode = uiState.appFilterMode == AppFilterMode.BLOCK_SELECTED
                val selectedCount = if (isBlockMode) uiState.blockedApps.size else uiState.allowedApps.size
                OutlinedButton(
                    onClick = { showAppPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedCount == 0) {
                            if (isBlockMode) "Choose Apps to Block" else "Choose Allowed Apps"
                        } else {
                            "$selectedCount app(s) selected"
                        }
                    )
                }
            }

            // Website Allowlist
            SectionLabel("Website Filter Mode")
            WebsiteFilterModeSelector(
                selected = uiState.websiteFilterMode,
                onSelect = viewModel::setWebsiteFilterMode
            )
            Text(
                text = when (uiState.websiteFilterMode) {
                    WebsiteFilterMode.BLOCKLIST -> "Apply the session's selected website block rules. This does not control app selection."
                    WebsiteFilterMode.ALLOWLIST_ONLY -> "Only configured allowed websites are accessible in supported browsers."
                    WebsiteFilterMode.ADULT_PROTECTION -> "Apply Barrier's URL and accessible-text adult-content heuristics in supported browsers."
                    WebsiteFilterMode.COMBINED -> "Apply both configured website block rules and website allowlist rules."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                description = "Prevents canceling this active session inside Barrier. Android can still force-stop or uninstall the app.",
                checked = uiState.isLocked,
                onToggle = viewModel::toggleLocked
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            OptionToggleRow(
                icon = Icons.Outlined.Security,
                title = "Strict Mode",
                description = "Marks this as a strict session and prevents in-app cancellation. It is enabled by default when the global Strict Mode setting is on; Android can still stop or uninstall Barrier.",
                checked = uiState.isStrictMode,
                onToggle = viewModel::toggleStrictMode
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            OptionToggleRow(
                icon = Icons.Outlined.Block,
                title = "Block Adult Content",
                description = "Checks URLs and accessible text in supported browsers using heuristics. Coverage is limited and it can miss content; this is not system-wide filtering.",
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
        val blockMode = uiState.appFilterMode == AppFilterMode.BLOCK_SELECTED
        val selectedApps = if (blockMode) uiState.blockedApps else uiState.allowedApps
        AppPickerSheet(
            title = if (blockMode) "Choose Apps to Block" else "Choose Allowed Apps",
            installedApps = uiState.installedApps,
            selectedApps = selectedApps,
            onToggleApp = if (blockMode) viewModel::toggleBlockedApp else viewModel::toggleAllowedApp,
            onSelectAll = {
                if (blockMode) viewModel.setBlockedApps(uiState.installedApps.map { it.packageName })
                else viewModel.setAllowedApps(uiState.installedApps.map { it.packageName })
            },
            onClearAll = {
                if (blockMode) viewModel.setBlockedApps(emptyList())
                else viewModel.setAllowedApps(emptyList())
            },
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
private fun DurationPicker(
    hours: Int,
    minutes: Int,
    onDurationChange: (Int, Int) -> Unit,
    onValidityChange: (Boolean) -> Unit
) {
    var unit by remember { mutableStateOf(DurationUnit.HOURS) }
    val totalMinutes = SessionDurationPolicy.toMinutes(hours, minutes)
    val amount = when (unit) {
        DurationUnit.MINUTES -> totalMinutes
        DurationUnit.HOURS -> (totalMinutes + 59) / 60
        DurationUnit.DAYS -> (totalMinutes + 1_439) / 1_440
    }
    var amountText by remember(unit, amount) { mutableStateOf(amount.toString()) }
    val minValue = when (unit) { DurationUnit.MINUTES -> 5; DurationUnit.HOURS -> 1; DurationUnit.DAYS -> 1 }
    val maxValue = when (unit) { DurationUnit.MINUTES -> 10_080; DurationUnit.HOURS -> 168; DurationUnit.DAYS -> 7 }
    val unitLabel = when (unit) { DurationUnit.MINUTES -> "minutes"; DurationUnit.HOURS -> "hours"; DurationUnit.DAYS -> "days" }
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
                            DurationUnit.MINUTES -> totalMinutes.coerceIn(5, 10_080)
                            DurationUnit.HOURS -> ((totalMinutes + 59) / 60).coerceIn(1, 168) * 60
                            DurationUnit.DAYS -> ((totalMinutes + 1_439) / 1_440).coerceIn(1, 7) * 1_440
                        }
                        amountText = when (option) {
                            DurationUnit.MINUTES -> nextTotal.toString()
                            DurationUnit.HOURS -> (nextTotal / 60).toString()
                            DurationUnit.DAYS -> (nextTotal / 1_440).toString()
                        }
                        onValidityChange(true)
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
                        amountText = if (presetHours == 0) presetMinutes.toString() else presetHours.toString()
                        onValidityChange(true)
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        OutlinedTextField(
            value = amountText,
            onValueChange = { newValue ->
                if (newValue.length <= 5 && newValue.all { it.isDigit() }) {
                    amountText = newValue
                    val parsed = newValue.toIntOrNull()
                    val valid = parsed != null && parsed in minValue..maxValue
                    onValidityChange(valid)
                    if (valid) {
                        val nextTotal = when (unit) {
                            DurationUnit.MINUTES -> parsed!!
                            DurationUnit.HOURS -> parsed!! * 60
                            DurationUnit.DAYS -> parsed!! * 1_440
                        }
                        onDurationChange(nextTotal / 60, nextTotal % 60)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Custom duration") },
            suffix = { Text(unitLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = amountText.toIntOrNull()?.let { it !in minValue..maxValue } ?: true
        )
        Text(
            "Enter $minValue–$maxValue $unitLabel. Selected: " + durationLabel + ". Estimated end: " + estimatedEnd,
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
private fun AppFilterModeSelector(
    selected: AppFilterMode,
    onSelect: (AppFilterMode) -> Unit
) {
    val modes = listOf(
        AppFilterMode.NONE to "Off",
        AppFilterMode.BLOCK_SELECTED to "Block selected",
        AppFilterMode.ALLOWLIST_ONLY to "Allowlist only"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        modes.forEach { (mode, label) ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BarrierGreen.copy(alpha = 0.15f),
                    selectedLabelColor = BarrierGreen
                )
            )
        }
    }
}

@Composable
private fun WebsiteFilterModeSelector(
    selected: WebsiteFilterMode,
    onSelect: (WebsiteFilterMode) -> Unit
) {
    val modes = listOf(
        WebsiteFilterMode.BLOCKLIST to "Blocklisted Sites",
        WebsiteFilterMode.ALLOWLIST_ONLY to "Website Allowlist",
        WebsiteFilterMode.ADULT_PROTECTION to "Adult Protection",
        WebsiteFilterMode.COMBINED to "Combined Website Rules"
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

@Composable
private fun InstalledAppIcon(
    app: com.rushgames.barrier.data.model.AppInfo,
    selected: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    if (icon != null) {
        androidx.compose.foundation.Image(
            bitmap = icon,
            contentDescription = app.appName,
            modifier = Modifier.size(40.dp)
        )
    } else {
        Icon(
            if (app.isBrowser) Icons.Outlined.Language else Icons.Outlined.Apps,
            contentDescription = app.appName,
            modifier = Modifier.size(40.dp),
            tint = if (selected) BarrierGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    title: String,
    installedApps: List<com.rushgames.barrier.data.model.AppInfo>,
    selectedApps: List<String>,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val visibleApps = installedApps.filter {
        it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${selectedApps.size} selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search installed apps") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSelectAll) { Text("Select all") }
                TextButton(onClick = onClearAll) { Text("Clear all") }
            }
            Column(modifier = Modifier.height(360.dp).verticalScroll(rememberScrollState())) {
                visibleApps.forEach { app ->
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
                            InstalledAppIcon(app = app, selected = isSelected)
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
                if (visibleApps.isEmpty()) {
                    Text("No matching installed apps.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
