package com.rushgames.barrier.ui.screens.sounds

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rushgames.barrier.ui.components.BarrierCard
import com.rushgames.barrier.ui.components.BarrierSectionHeader
import com.rushgames.barrier.ui.theme.BarrierGreen
import com.rushgames.barrier.ui.theme.BarrierPurple

@Composable
fun SoundsScreen(viewModel: SoundsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playingCount = uiState.sounds.count { it.isPlaying }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Sounds", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("Ambient sounds for focus", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (playingCount > 0) {
                Button(
                    onClick = viewModel::stopAll,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Stop All", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Mix Mode Toggle
        BarrierCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Mix Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Play multiple sounds together", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = uiState.isMixMode,
                    onCheckedChange = { viewModel.toggleMixMode() },
                    colors = SwitchDefaults.colors(checkedTrackColor = BarrierPurple)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Now Playing bar
        AnimatedVisibility(visible = playingCount > 0) {
            Column {
                BarrierCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(BarrierGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = BarrierGreen, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$playingCount sound${if (playingCount > 1) "s" else ""} playing",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BarrierGreen
                            )
                        }
                        // Master Volume
                        Slider(
                            value = uiState.masterVolume,
                            onValueChange = viewModel::setMasterVolume,
                            modifier = Modifier.width(100.dp),
                            colors = SliderDefaults.colors(thumbColor = BarrierGreen, activeTrackColor = BarrierGreen)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Sounds by category
        listOf(SoundCategory.NATURE, SoundCategory.AMBIENT, SoundCategory.NOISE).forEach { cat ->
            val catSounds = uiState.sounds.filter { it.category == cat }
            BarrierSectionHeader(cat.name.replace("_", " "))
            Spacer(Modifier.height(8.dp))
            catSounds.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { sound ->
                        SoundTile(
                            sound = sound,
                            onToggle = { viewModel.toggleSound(sound.id) },
                            onVolumeChange = { v -> viewModel.setVolume(sound.id, v) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun SoundTile(
    sound: AmbientSound,
    onToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = if (sound.isPlaying) BarrierGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val bgColor = if (sound.isPlaying) BarrierGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier.clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, if (sound.isPlaying) BarrierGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sound.emoji, fontSize = 28.sp)
                if (sound.isPlaying) {
                    Icon(Icons.Outlined.Pause, contentDescription = null, tint = BarrierGreen, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = sound.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (sound.isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                color = if (sound.isPlaying) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (sound.isPlaying) {
                Slider(
                    value = sound.volume,
                    onValueChange = onVolumeChange,
                    colors = SliderDefaults.colors(thumbColor = BarrierGreen, activeTrackColor = BarrierGreen),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}
