package com.rushgames.barrier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rushgames.barrier.ui.theme.BarrierGreen
import com.rushgames.barrier.ui.theme.BarrierRed
import com.rushgames.barrier.ui.theme.BarrierOrange

enum class BadgeStatus { ACTIVE, INACTIVE, WARNING }

@Composable
fun StatusBadge(
    status: BadgeStatus,
    label: String,
    modifier: Modifier = Modifier
) {
    val (color, dotColor) = when (status) {
        BadgeStatus.ACTIVE -> Pair(BarrierGreen.copy(alpha = 0.12f), BarrierGreen)
        BadgeStatus.INACTIVE -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        BadgeStatus.WARNING -> Pair(BarrierOrange.copy(alpha = 0.12f), BarrierOrange)
    }
    val textColor = when (status) {
        BadgeStatus.ACTIVE -> BarrierGreen
        BadgeStatus.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
        BadgeStatus.WARNING -> BarrierOrange
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

@Composable
fun BlockedCountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count == 0) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(BarrierRed.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$count blocked",
            style = MaterialTheme.typography.labelSmall,
            color = BarrierRed
        )
    }
}
