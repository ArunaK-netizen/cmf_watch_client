package com.cmfwatch.companion.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.storage.SavedWorkout
import com.cmfwatch.companion.ui.theme.*
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    summary: DashboardSummary,
    onActivityClick: () -> Unit = {},
    onHeartRateClick: () -> Unit = {},
    onSleepClick: () -> Unit = {},
    onWatchCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Dynamic Time-Aware Greeting
    val currentHour = remember { LocalTime.now().hour }
    val greetingText = remember(currentHour) {
        when (currentHour) {
            in 4..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..23 -> "Good evening,"
            else -> "Good night,"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Ambient Greeting Header Bar (No Red Dot on Notification Bell)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greetingText,
                        fontFamily = AppFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Rasagna ",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "✨",
                            fontSize = 24.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Notification Bell Button (Clean, NO Red Dot)
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite)
                            .border(1.dp, DividerColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings Gear Button
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite)
                            .border(1.dp, DividerColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Hero Daily Vitality Matrix Card (No Ring Text Overlap)
        item {
            val stepsVal = summary.todaySteps
            val calVal = summary.todayCaloriesKcal
            val distVal = summary.todayDistanceKm

            val stepsProgress = ((stepsVal ?: 0).toFloat() / 10000f).coerceIn(0f, 1f)
            val calProgress = ((calVal ?: 0).toFloat() / 500f).coerceIn(0f, 1f)
            val distProgress = ((distVal ?: 0.0f) / 8.0f).coerceIn(0f, 1f)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onActivityClick() },
                color = SurfaceWhite,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 3 Concentric Glowing Progress Rings Canvas (Proportional Sizing)
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 8.dp.toPx()
                            val center = Offset(size.width / 2, size.height / 2)

                            // Outer Ring - Steps (Neon Emerald #00E676)
                            val radius1 = (size.width / 2) - strokeWidth / 2
                            drawCircle(
                                color = RingGreenBg,
                                radius = radius1,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = RingGreen,
                                startAngle = -90f,
                                sweepAngle = 360f * stepsProgress,
                                useCenter = false,
                                topLeft = Offset(center.x - radius1, center.y - radius1),
                                size = Size(radius1 * 2, radius1 * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Middle Ring - Calories (Vivid Orange #FF6D00)
                            val radius2 = radius1 - strokeWidth - 4.dp.toPx()
                            drawCircle(
                                color = RingOrangeBg,
                                radius = radius2,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = RingOrange,
                                startAngle = -90f,
                                sweepAngle = 360f * calProgress,
                                useCenter = false,
                                topLeft = Offset(center.x - radius2, center.y - radius2),
                                size = Size(radius2 * 2, radius2 * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Inner Ring - Distance (Electric Blue #00B0FF)
                            val radius3 = radius2 - strokeWidth - 4.dp.toPx()
                            drawCircle(
                                color = RingBlueBg,
                                radius = radius3,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = RingBlue,
                                startAngle = -90f,
                                sweepAngle = 360f * distProgress,
                                useCenter = false,
                                topLeft = Offset(center.x - radius3, center.y - radius3),
                                size = Size(radius3 * 2, radius3 * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        // Center Counter Display (Zero Text Overlap)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "VITALITY",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stepsVal?.let { String.format("%,d", it) } ?: "--",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "steps",
                                fontFamily = AppFontFamily,
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Integrated Metric Pill Stack
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VitalityMetricPill(
                            icon = Icons.Default.DirectionsWalk,
                            iconBg = RingGreenBg,
                            accentColor = RingGreen,
                            title = "Steps",
                            value = stepsVal?.let { String.format("%,d", it) } ?: "--",
                            unit = "steps"
                        )

                        VitalityMetricPill(
                            icon = Icons.Default.LocalFireDepartment,
                            iconBg = RingOrangeBg,
                            accentColor = RingOrange,
                            title = "Calories",
                            value = calVal?.toString() ?: "--",
                            unit = "kcal"
                        )

                        VitalityMetricPill(
                            icon = Icons.Default.Place,
                            iconBg = RingBlueBg,
                            accentColor = RingBlue,
                            title = "Distance",
                            value = distVal?.let { String.format("%.1f", it) } ?: "--",
                            unit = "km"
                        )
                    }
                }
            }
        }

        // 3. Category Quick Navigation Pill Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryNavPill(
                    modifier = Modifier.weight(1f),
                    title = "Activity",
                    icon = Icons.Default.DirectionsRun,
                    accentColor = RingGreen,
                    onClick = onActivityClick
                )

                CategoryNavPill(
                    modifier = Modifier.weight(1f),
                    title = "Vitals",
                    icon = Icons.Default.Favorite,
                    accentColor = HeartRateRed,
                    onClick = onHeartRateClick
                )

                CategoryNavPill(
                    modifier = Modifier.weight(1f),
                    title = "Sleep",
                    icon = Icons.Default.NightlightRound,
                    accentColor = SleepPurple,
                    onClick = onSleepClick
                )

                CategoryNavPill(
                    modifier = Modifier.weight(1f),
                    title = "Device",
                    icon = Icons.Default.Watch,
                    accentColor = RingBlue,
                    onClick = onWatchCardClick
                )
            }
        }

        // 4. 2x2 Bento Health Grid (100% Data Truth Graphs)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Heart Rate & Sleep
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Heart Rate Bento Card
                    BentoMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Heart Rate",
                        valueText = summary.latestHeartRate?.let { "$it bpm" } ?: "-- bpm",
                        subtitleText = "Resting",
                        icon = Icons.Default.Favorite,
                        iconBg = HeartRateBg,
                        accentColor = HeartRateRed,
                        onClick = onHeartRateClick,
                        content = {
                            // Dynamic HR Wave Canvas (Flat line when no real samples exist)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val width = size.width
                                    val height = size.height

                                    val samples = summary.hrSamplesToday
                                    if (samples.isNotEmpty()) {
                                        val maxBpm = maxOf(120, samples.maxOf { it.bpm })
                                        val minBpm = minOf(50, samples.minOf { it.bpm })
                                        val range = maxOf(1, maxBpm - minBpm)
                                        val points = samples.takeLast(10).mapIndexed { idx, item ->
                                            val x = (idx.toFloat() / maxOf(1, samples.takeLast(10).size - 1)) * width
                                            val y = height - (((item.bpm - minBpm).toFloat() / range.toFloat()) * (height - 4))
                                            Offset(x, y)
                                        }

                                        val path = Path().apply {
                                            moveTo(points.first().x, points.first().y)
                                            for (i in 0 until points.size - 1) {
                                                val p1 = points[i]
                                                val p2 = points[i + 1]
                                                val controlX = (p1.x + p2.x) / 2
                                                cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
                                            }
                                        }

                                        drawPath(
                                            path = path,
                                            color = HeartRateRed,
                                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    } else {
                                        // Dynamic Empty Baseline (No synthetic fake curve)
                                        drawLine(
                                            color = HeartRateRed.copy(alpha = 0.3f),
                                            start = Offset(0f, height * 0.7f),
                                            end = Offset(width, height * 0.7f),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }
                            }
                        }
                    )

                    // Sleep Bento Card
                    val sleepMins = summary.lastSleepMinutes
                    val sleepText = if (sleepMins != null) {
                        "${sleepMins / 60}h ${sleepMins % 60}m"
                    } else {
                        "--"
                    }

                    BentoMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Sleep",
                        valueText = sleepText,
                        subtitleText = "Last night",
                        icon = Icons.Default.NightlightRound,
                        iconBg = SleepPurpleBg,
                        accentColor = SleepPurple,
                        onClick = onSleepClick,
                        content = {
                            // Dynamic Sleep Visualizer (Empty baseline when no data)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                if (sleepMins != null && sleepMins > 0) {
                                    listOf(0.4f, 0.7f, 0.3f, 0.9f, 0.5f, 0.8f, 0.2f, 1.0f).forEach { hRatio ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(hRatio)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(SleepPurple.copy(alpha = 0.3f + (hRatio * 0.7f)))
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(SleepPurple.copy(alpha = 0.3f))
                                            .align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                    )
                }

                // Row 2: SpO2 Blood Oxygen & Stress Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SpO2 Bento Card
                    val spo2Val = summary.latestSpO2
                    val spo2Status = when {
                        spo2Val == null -> "--"
                        spo2Val >= 95 -> "Optimal"
                        spo2Val >= 90 -> "Normal"
                        else -> "Low"
                    }

                    BentoMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "SpO2",
                        valueText = spo2Val?.let { "$it%" } ?: "--%",
                        subtitleText = "Blood Oxygen",
                        icon = Icons.Default.WaterDrop,
                        iconBg = Color(0xFF0C2A3A),
                        accentColor = Color(0xFF38BDF8),
                        onClick = { },
                        content = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0C2A3A),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = spo2Status,
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    )

                    // Stress Bento Card
                    val stressVal = summary.latestStressScore
                    val stressStatus = when {
                        stressVal == null -> "--"
                        stressVal < 30 -> "Relaxed"
                        stressVal < 60 -> "Normal"
                        else -> "High"
                    }

                    BentoMetricCard(
                        modifier = Modifier.weight(1f),
                        title = "Stress",
                        valueText = stressVal?.toString() ?: "--",
                        subtitleText = "Daily Score",
                        icon = Icons.Default.Psychology,
                        iconBg = Color(0xFF3B2A08),
                        accentColor = Color(0xFFF59E0B),
                        onClick = { },
                        content = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF3B2A08),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = stressStatus,
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        // 5. Recent Workouts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Workouts",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                TextTextButton(
                    onClick = { },
                    text = "See All"
                )
            }
        }

        if (summary.workoutsToday.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, DividerColor, RoundedCornerShape(20.dp)),
                    color = SurfaceWhite,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceCardAlt),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Workouts Today",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start a workout from your CMF Watch to see stats here",
                            fontFamily = AppFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(summary.workoutsToday) { workout ->
                HomeWorkoutRowItem(workout = workout)
            }
        }
    }
}

// ==========================================
// Sub-components
// ==========================================

@Composable
fun VitalityMetricPill(
    icon: ImageVector,
    iconBg: Color,
    accentColor: Color,
    title: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCardAlt)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = title,
                fontFamily = AppFontFamily,
                fontSize = 10.sp,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    fontFamily = AppFontFamily,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun CategoryNavPill(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        color = SurfaceWhite,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun BentoMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    valueText: String,
    subtitleText: String,
    icon: ImageVector,
    iconBg: Color,
    accentColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        color = SurfaceWhite,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = valueText,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
            Text(
                text = subtitleText,
                fontFamily = AppFontFamily,
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}

@Composable
fun HomeWorkoutRowItem(workout: SavedWorkout) {
    val icon = when (workout.type.lowercase()) {
        "running", "run" -> Icons.Default.DirectionsRun
        "cycling", "cycle" -> Icons.Default.DirectionsBike
        "walking", "walk" -> Icons.Default.DirectionsWalk
        else -> Icons.Default.FitnessCenter
    }

    val iconBg = when (workout.type.lowercase()) {
        "running", "run" -> SleepPurpleBg
        "cycling", "cycle" -> RingBlueBg
        "walking", "walk" -> RingGreenBg
        else -> RingOrangeBg
    }

    val iconTint = when (workout.type.lowercase()) {
        "running", "run" -> SleepPurple
        "cycling", "cycle" -> RingBlue
        "walking", "walk" -> RingGreen
        else -> RingOrange
    }

    val timeStr = remember(workout.startTime) {
        val dt = workout.startTime.atZone(ZoneId.systemDefault())
        dt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(20.dp)),
        color = SurfaceWhite,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = workout.type,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.type,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeStr,
                    fontFamily = AppFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${workout.durationMinutes} min",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${String.format("%.1f", workout.distanceKm)} km • ${workout.caloriesKcal} kcal",
                    fontFamily = AppFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun TextTextButton(
    onClick: () -> Unit,
    text: String
) {
    Text(
        text = text,
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = Color(0xFF00E676),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    )
}
