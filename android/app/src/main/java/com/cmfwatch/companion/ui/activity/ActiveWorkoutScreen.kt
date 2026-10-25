package com.cmfwatch.companion.ui.activity

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.CmfSport
import com.cmfwatch.companion.ui.theme.CmfBackground
import com.cmfwatch.companion.ui.theme.CmfError
import com.cmfwatch.companion.ui.theme.CmfOnPrimary
import com.cmfwatch.companion.ui.theme.CmfOnSurface
import com.cmfwatch.companion.ui.theme.CmfOnSurfaceVariant
import com.cmfwatch.companion.ui.theme.CmfPrimary
import com.cmfwatch.companion.ui.theme.CmfPrimaryContainer
import com.cmfwatch.companion.ui.theme.CmfSurface
import com.cmfwatch.companion.ui.theme.CmfSurfaceContainer
import com.cmfwatch.companion.ui.theme.CmfSurfaceLowest
import com.cmfwatch.companion.ui.theme.CmfTertiaryContainer
import com.cmfwatch.companion.ui.theme.HeadlineFontFamily
import com.cmfwatch.companion.ui.theme.InterFontFamily
import java.util.Locale

/**
 * Dedicated Full-Screen Live Workout Tracking Page.
 * Automatically adapts between GPS Route Map mode (for Outdoor Run, Cycle, Walk, Hike)
 * and High-Tech Telemetry HUD mode (for Cricket, Strength, Gym, Yoga, etc.).
 */
@Composable
fun ActiveWorkoutScreen(
    workoutType: String,
    bpm: Int?,
    durationSec: Int,
    todayKcal: Int,
    todayDistKm: Float,
    onStopWorkout: () -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sport = remember(workoutType) { CmfSport.fromName(workoutType) }
    var isPaused by remember { mutableStateOf(false) }
    var showEndConfirmDialog by remember { mutableStateOf(false) }

    // Session HR history for live sparkline curve
    val sessionHrList = remember { mutableStateListOf<Int>() }
    LaunchedEffect(bpm) {
        if (bpm != null && bpm > 0) {
            sessionHrList.add(bpm)
            if (sessionHrList.size > 60) {
                sessionHrList.removeAt(0)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CmfBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Sport Title, Mode, Minimize Button
            WorkoutTopBar(
                sport = sport,
                isPaused = isPaused,
                pulseScale = pulseScale,
                onMinimize = onMinimize
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (sport.isGpsEnabled) {
                // -------------------------------------------------------------
                // GPS OUTDOOR MODE: Interactive Route Track Map & Distance HUD
                // -------------------------------------------------------------
                GpsWorkoutMapSection(
                    sport = sport,
                    durationSec = durationSec,
                    distKm = todayDistKm,
                    bpm = bpm,
                    kcal = todayKcal,
                    pulseScale = pulseScale
                )
            } else {
                // -------------------------------------------------------------
                // NON-GPS INDOOR / ARENA MODE: High-Impact Telemetry & BPM HUD
                // -------------------------------------------------------------
                IndoorWorkoutTelemetrySection(
                    sport = sport,
                    durationSec = durationSec,
                    bpm = bpm,
                    kcal = todayKcal,
                    hrHistory = sessionHrList,
                    pulseScale = pulseScale
                )
            }
        }

        // Bottom Controls Bar: Pause/Resume & End Workout
        WorkoutBottomControlsBar(
            isPaused = isPaused,
            onTogglePause = { isPaused = !isPaused },
            onEndClick = { showEndConfirmDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }

    if (showEndConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEndConfirmDialog = false },
            title = {
                Text(
                    text = "End Workout?",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CmfOnSurface
                )
            },
            text = {
                Text(
                    text = "This will conclude your active ${sport.displayName} session on both your CMF Watch and mobile app, saving the finalized summary to your activity history.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = CmfOnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndConfirmDialog = false
                        onStopWorkout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CmfError)
                ) {
                    Text("End Session", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmDialog = false }) {
                    Text("Continue Workout", color = CmfOnSurface)
                }
            }
        )
    }
}

@Composable
private fun WorkoutTopBar(
    sport: CmfSport,
    isPaused: Boolean,
    pulseScale: Float,
    onMinimize: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(if (!isPaused) pulseScale else 1.0f)
                    .clip(CircleShape)
                    .background(if (isPaused) Color(0xFFFFB300) else Color(0xFF00E676))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sport.displayName.uppercase(Locale.US),
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp,
                        color = CmfOnSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CmfPrimaryContainer.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (sport.isGpsEnabled) "GPS ROUTE" else sport.category.displayName.uppercase(Locale.US),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            color = CmfPrimary
                        )
                    }
                }
                Text(
                    text = if (isPaused) "SESSION PAUSED" else "LIVE WATCH TELEMETRY",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = CmfOnSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onMinimize,
            modifier = Modifier
                .clip(CircleShape)
                .background(CmfSurfaceContainer)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Minimize",
                tint = CmfOnSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =============================================================================
// GPS OUTDOOR SECTION
// =============================================================================
@Composable
private fun GpsWorkoutMapSection(
    sport: CmfSport,
    durationSec: Int,
    distKm: Float,
    bpm: Int?,
    kcal: Int,
    pulseScale: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Map Container Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CmfSurfaceLowest)
                .border(1.dp, CmfSurfaceContainer, RoundedCornerShape(24.dp))
        ) {
            // Simulated / Stylized GPS Path Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw subtle grid map pattern
                val gridStep = 40f
                for (x in 0..(w / gridStep).toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(x * gridStep, 0f),
                        end = Offset(x * gridStep, h),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..(h / gridStep).toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(0f, y * gridStep),
                        end = Offset(w, y * gridStep),
                        strokeWidth = 1f
                    )
                }

                // Stylized workout route line
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.75f)
                    cubicTo(w * 0.3f, h * 0.5f, w * 0.4f, h * 0.85f, w * 0.55f, h * 0.45f)
                    cubicTo(w * 0.7f, h * 0.15f, w * 0.8f, h * 0.6f, w * 0.85f, h * 0.35f)
                }

                // Glowing outer track stroke
                drawPath(
                    path = path,
                    color = CmfPrimary.copy(alpha = 0.35f),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )

                // Inner sharp track line
                drawPath(
                    path = path,
                    color = CmfPrimary,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // Start pin
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(w * 0.15f, h * 0.75f)
                )
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = 5f,
                    center = Offset(w * 0.15f, h * 0.75f)
                )

                // Current position pulsing beacon
                drawCircle(
                    color = CmfPrimary.copy(alpha = 0.25f),
                    radius = 24f * pulseScale,
                    center = Offset(w * 0.85f, h * 0.35f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 9f,
                    center = Offset(w * 0.85f, h * 0.35f)
                )
                drawCircle(
                    color = CmfPrimary,
                    radius = 6f,
                    center = Offset(w * 0.85f, h * 0.35f)
                )
            }

            // Map Status Overlays
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sensors,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GPS LOCK ACTIVE",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
            }
        }

        // Primary Large Numbers: Duration & Distance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkoutStatHeroCard(
                label = "TIME",
                value = formatTimer(durationSec),
                icon = Icons.Outlined.Timer,
                modifier = Modifier.weight(1f)
            )
            WorkoutStatHeroCard(
                label = "DISTANCE",
                value = String.format(Locale.US, "%.2f", distKm),
                unit = "km",
                icon = Icons.Outlined.LocationOn,
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary Metrics: Live Heart Rate, Pace, Calories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkoutMetricSmallCard(
                label = "HEART RATE",
                value = if (bpm != null && bpm > 0) "$bpm" else "--",
                unit = "BPM",
                icon = Icons.Outlined.Favorite,
                tint = CmfPrimary,
                modifier = Modifier.weight(1f)
            )
            WorkoutMetricSmallCard(
                label = "PACE",
                value = computeLivePace(distKm, durationSec),
                unit = "/km",
                icon = Icons.Outlined.Speed,
                tint = CmfTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            WorkoutMetricSmallCard(
                label = "ACTIVE CALS",
                value = "$kcal",
                unit = "kcal",
                icon = Icons.Outlined.LocalFireDepartment,
                tint = Color(0xFFFF9100),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =============================================================================
// INDOOR / NON-GPS SECTION (Cricket, Gym, Yoga, etc.)
// =============================================================================
@Composable
private fun IndoorWorkoutTelemetrySection(
    sport: CmfSport,
    durationSec: Int,
    bpm: Int?,
    kcal: Int,
    hrHistory: List<Int>,
    pulseScale: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Giant Digital Timer Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CmfSurfaceLowest)
                .border(1.dp, CmfSurfaceContainer, RoundedCornerShape(24.dp))
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ACTIVE DURATION",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = CmfOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimer(durationSec),
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    letterSpacing = 2.sp,
                    color = CmfOnSurface
                )
            }
        }

        // Giant Heart Rate & Zone Dial Card
        val effectiveBpm = bpm ?: 0
        val zone = getHeartRateZone(effectiveBpm)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CmfSurfaceLowest)
                .border(1.dp, CmfSurfaceContainer, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HEART RATE TELEMETRY",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = CmfOnSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(zone.color.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = zone.name.uppercase(Locale.US),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = zone.color
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Favorite,
                        contentDescription = null,
                        tint = zone.color,
                        modifier = Modifier
                            .size(32.dp)
                            .scale(if (effectiveBpm > 0) pulseScale else 1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (effectiveBpm > 0) "$effectiveBpm" else "--",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp,
                        color = CmfOnSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BPM",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = CmfOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time BPM Sparkline Canvas
                if (hrHistory.size >= 2) {
                    LiveHrSparklineCanvas(
                        samples = hrHistory,
                        color = zone.color,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    )
                }
            }
        }

        // Secondary Metrics Row: Calories & Intensity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkoutMetricSmallCard(
                label = "CALORIES",
                value = "$kcal",
                unit = "kcal",
                icon = Icons.Outlined.LocalFireDepartment,
                tint = Color(0xFFFF9100),
                modifier = Modifier.weight(1f)
            )
            WorkoutMetricSmallCard(
                label = "SPORT FOCUS",
                value = sport.displayName,
                unit = "",
                icon = Icons.Outlined.DirectionsRun,
                tint = CmfTertiaryContainer,
                modifier = Modifier.weight(1.3f)
            )
        }
    }
}

@Composable
private fun LiveHrSparklineCanvas(
    samples: List<Int>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minHr = (samples.minOrNull() ?: 60).toFloat().coerceAtLeast(40f)
        val maxHr = (samples.maxOrNull() ?: 180).toFloat().coerceAtLeast(minHr + 10f)

        val path = Path()
        val stepX = w / (samples.size - 1).coerceAtLeast(1)

        samples.forEachIndexed { index, hr ->
            val normY = 1.0f - ((hr - minHr) / (maxHr - minHr)).coerceIn(0f, 1f)
            val x = index * stepX
            val y = normY * (h - 10f) + 5f
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// =============================================================================
// REUSABLE CARDS & CONTROLS
// =============================================================================
@Composable
private fun WorkoutStatHeroCard(
    label: String,
    value: String,
    unit: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CmfSurfaceLowest)
            .border(1.dp, CmfSurfaceContainer, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = CmfOnSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CmfOnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = CmfOnSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = CmfOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutMetricSmallCard(
    label: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CmfSurfaceLowest)
            .border(1.dp, CmfSurfaceContainer, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = CmfOnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CmfOnSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = CmfOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutBottomControlsBar(
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onEndClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        CmfBackground.copy(alpha = 0.95f),
                        CmfBackground
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pause / Resume Button
            OutlinedButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                    contentDescription = null,
                    tint = CmfOnSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPaused) "Resume" else "Pause",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CmfOnSurface
                )
            }

            // End Workout Button
            Button(
                onClick = onEndClick,
                modifier = Modifier
                    .weight(1.3f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CmfError)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Stop,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "End Workout",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

// =============================================================================
// HELPERS
// =============================================================================
private fun formatTimer(seconds: Int): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

private fun computeLivePace(distKm: Float, sec: Int): String {
    if (distKm < 0.05f || sec < 5) return "--"
    val paceSecPerKm = (sec / distKm).toInt()
    if (paceSecPerKm > 3600) return "--"
    val pMin = paceSecPerKm / 60
    val pSec = paceSecPerKm % 60
    return String.format(Locale.US, "%d'%02d\"", pMin, pSec)
}

private enum class HrZone(val color: Color) {
    REST(Color(0xFF90A4AE)),
    WARM_UP(Color(0xFF29B6F6)),
    FAT_BURN(Color(0xFF66BB6A)),
    AEROBIC(Color(0xFFFFEE58)),
    ANAEROBIC(Color(0xFFFFA726)),
    PEAK(Color(0xFFEF5350))
}

private fun getHeartRateZone(bpm: Int): HrZone = when {
    bpm < 90 -> HrZone.REST
    bpm < 115 -> HrZone.WARM_UP
    bpm < 135 -> HrZone.FAT_BURN
    bpm < 155 -> HrZone.AEROBIC
    bpm < 175 -> HrZone.ANAEROBIC
    else -> HrZone.PEAK
}
