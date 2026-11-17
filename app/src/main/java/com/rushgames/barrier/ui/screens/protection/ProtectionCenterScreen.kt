package com.rushgames.barrier.ui.screens.protection

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class ProtectionFeatureStatus(
    val title: String,
    val detail: String,
    val active: Boolean? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionCenterScreen(
    onBack: () -> Unit,
    viewModel: ProtectionCenterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAccessibilityStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refreshAccessibilityStatus()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val session = state.activeSession
    val statuses = listOf(
        ProtectionFeatureStatus(
            title = "Session app and website rules",
            detail = when {
                session == null -> "Inactive — start a focus session to apply its rules."
                !state.accessibilityEnabled -> "Waiting for Accessibility access. Session “${session.name}” is saved, but enforcement is not active."
                !state.accessibilityServiceConnected -> "Accessibility is enabled, but its service is not connected. Session “${session.name}” is saved, but enforcement is not active."
                !state.sessionHasRules -> "Session “${session.name}” is running, but no app, website, or adult-content rules are selected."
                else -> "Active for session “${session.name}” until its scheduled end. App blocking and supported-browser checks depend on Accessibility access."
            },
            active = state.sessionRulesEnforced
        ),
        ProtectionFeatureStatus(
            title = "Adult URL and domain checks",
            detail = when {
                !state.adultProtectionEnabled -> "Off — turn on Adult Content Protection in Settings to apply these checks outside focus sessions."
                !state.accessibilityEnabled -> "Enabled in Settings, but waiting for Accessibility access. Checks are not active yet."
                !state.accessibilityServiceConnected -> "Accessibility is enabled, but the service is not connected. Checks are not active yet."
                else -> "Active for supported browsers while Accessibility is enabled. Detection uses URL, domain, search-query, and accessible text heuristics; it can miss content."
            },
            active = state.globalAdultFilteringEnforced
        ),
        ProtectionFeatureStatus(
            title = "Website coverage",
            detail = "Limited to supported browsers whose visible page content is accessible to Android Accessibility. This is not device-wide website blocking."
        ),
        ProtectionFeatureStatus(
            title = "Adult image detection",
            detail = "Experimental heuristic only. It is not a reliable adult-content classifier and should not be relied on for safety."
        ),
        ProtectionFeatureStatus(
            title = "SafeSearch and YouTube Restricted Mode",
            detail = "Unavailable — Barrier does not currently enforce these settings."
        ),
        ProtectionFeatureStatus(
            title = "VPN and device-wide DNS filtering",
            detail = "Unavailable — the VPN packet forwarding and DNS response engine is disabled."
        ),
        ProtectionFeatureStatus(
            title = "Cloud backup",
            detail = "Unavailable until Firebase is configured and authenticated backup is implemented."
        )
    )

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = if (state.sessionRulesEnforced || state.globalAdultFilteringEnforced) "Protection is active" else "Protection status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Status reflects the protections Barrier can currently verify.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session != null && (!state.accessibilityEnabled || !state.accessibilityServiceConnected)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                if (state.accessibilityEnabled) "Accessibility service is not connected" else "Accessibility access is required",
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (state.accessibilityEnabled) "Android currently reports the service as disconnected, so session rules are not being enforced. You can review Barrier's accessibility setting."
                                else "Android requires you to enable Barrier’s Accessibility service before session app and browser rules can be enforced."
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }) {
                                Text("Open Accessibility settings")
                            }
                        }
                    }
                }
            }

            items(statuses) { feature ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (feature.active) {
                            true -> MaterialTheme.colorScheme.primaryContainer
                            false -> MaterialTheme.colorScheme.surfaceVariant
                            null -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = when (feature.active) {
                                true -> Icons.Default.CheckCircle
                                false -> Icons.Default.Warning
                                null -> Icons.Default.Info
                            },
                            contentDescription = null
                        )
                        Column {
                            Text(feature.title, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(feature.detail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
