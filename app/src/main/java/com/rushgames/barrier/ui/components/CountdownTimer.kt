package com.rushgames.barrier.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushgames.barrier.ui.theme.BarrierGreen
import kotlinx.coroutines.delay

@Composable
fun SessionCountdownTimer(
    endTimeMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 8.dp,
    activeColor: Color = BarrierGreen,
    trackColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
) {
    var remainingMs by remember { mutableLongStateOf(maxOf(0, endTimeMs - System.currentTimeMillis())) }

    LaunchedEffect(endTimeMs) {
        while (remainingMs > 0) {
            delay(1000)
            remainingMs = maxOf(0, endTimeMs - System.currentTimeMillis())
        }
    }

    val progress = if (totalDurationMs > 0) {
        (remainingMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = LinearEasing),
        label = "countdown"
    )

    val hours = (remainingMs / 3_600_000).toInt()
    val minutes = ((remainingMs % 3_600_000) / 60_000).toInt()
    val seconds = ((remainingMs % 60_000) / 1_000).toInt()
    val timeText = if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.minDimension - strokePx
            val topLeft = Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = if (hours > 0) 22.sp else 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "remaining",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
