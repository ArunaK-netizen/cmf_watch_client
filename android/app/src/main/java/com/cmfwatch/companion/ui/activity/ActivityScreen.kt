package com.cmfwatch.companion.ui.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.StepInterval
import com.cmfwatch.companion.storage.SavedWorkout
import com.cmfwatch.companion.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

enum class ActivityTimeRange(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

@Composable
fun ActivityScreen(
    summary: DashboardSummary,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(ActivityTimeRange.DAY) }

    val stepsCount = summary.todaySteps
    val caloriesKcal = summary.todayCaloriesKcal
    val distanceKm = summary.todayDistanceKm
    val stepGoal = 10000
    val calGoal = 500
    val distGoal = 8.0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with Title & Top Action Icons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More Options",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Segmented Time Range Selector (Day, Week, Month, Year)
        item {
            TimeRangeSegmentedControl(
                selectedRange = selectedRange,
                onRangeSelected = { selectedRange = it }
            )
        }

        // 3. Hero Activity Card (Concentric Rings & Goals Breakdown)
        item {
            HeroActivityCard(
                steps = stepsCount,
                calories = caloriesKcal,
                distanceKm = distanceKm,
                stepGoal = stepGoal,
                calGoal = calGoal,
                distGoal = distGoal
            )
        }

        // 4. Main Steps Bar Chart Card
        item {
            StepsBarChartCard(
                steps = stepsCount,
                intervals = summary.stepIntervalsToday,
                selectedRange = selectedRange
            )
        }

        // 5. 3 Metric Cards Strip (Active Calories, Distance, Floors)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricStripCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Calories",
                    valueText = caloriesKcal?.let { "$it kcal" } ?: "-- kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    iconBgColor = Color(0xFFFFEDD5),
                    accentColor = Color(0xFFF97316),
                    yLabels = listOf("150", "75", "0"),
                    xLabels = listOf("12 AM", "12 PM", "12 AM")
                )

                MetricStripCard(
                    modifier = Modifier.weight(1f),
                    title = "Distance",
                    valueText = distanceKm?.let { String.format("%.1f km", it) } ?: "-- km",
                    icon = Icons.Default.Place,
                    iconBgColor = Color(0xFFDBEAFE),
                    accentColor = Color(0xFF3B82F6),
                    yLabels = listOf("1.0", "0.5", "0"),
                    xLabels = listOf("12 AM", "12 PM", "12 AM")
                )

                MetricStripCard(
                    modifier = Modifier.weight(1f),
                    title = "Floors",
                    valueText = "6 floors",
                    icon = Icons.Default.Stairs,
                    iconBgColor = Color(0xFFF3E8FF),
                    accentColor = Color(0xFF8B5CF6),
                    yLabels = listOf("10", "5", "0"),
                    xLabels = listOf("12 AM", "12 PM", "12 AM")
                )
            }
        }

        // 6. Activity Goal Progress Card
        item {
            val remainingSteps = stepsCount?.let { max(0, stepGoal - it) }
            val subtitleText = if (remainingSteps != null) {
                if (remainingSteps == 0) "Goal achieved! Great job keeping active."
                else "Keep going! You're ${String.format("%,d", remainingSteps)} steps away from your goal."
            } else {
                "Keep going! Wear your watch to track daily activity."
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                color = SurfaceWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1FAE5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = "Goal Icon",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Activity Goal",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitleText,
                            fontFamily = AppFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Details",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 7. Recent Workouts Section
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
                        .clip(RoundedCornerShape(20.dp)),
                    color = SurfaceWhite,
                    shadowElevation = 1.dp
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
                                .background(Color(0xFFF3F4F6)),
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
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(summary.workoutsToday) { workout ->
                WorkoutRowItem(workout = workout)
            }
        }
    }
}

// ==========================================
// Sub-components
// ==========================================

@Composable
fun TimeRangeSegmentedControl(
    selectedRange: ActivityTimeRange,
    onRangeSelected: (ActivityTimeRange) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF3F4F6))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ActivityTimeRange.values().forEach { range ->
                val isSelected = range == selectedRange
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) SurfaceWhite else Color.Transparent)
                        .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onRangeSelected(range) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        fontFamily = AppFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun HeroActivityCard(
    steps: Int?,
    calories: Int?,
    distanceKm: Float?,
    stepGoal: Int,
    calGoal: Int,
    distGoal: Float
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Concentric Rings (Canvas)
            val stepsProgress = ((steps ?: 0).toFloat() / stepGoal).coerceIn(0f, 1f)
            val calProgress = ((calories ?: 0).toFloat() / calGoal).coerceIn(0f, 1f)
            val distProgress = ((distanceKm ?: 0.0f) / distGoal).coerceIn(0f, 1f)

            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    val center = Offset(size.width / 2, size.height / 2)

                    // Outer Ring - Steps (Green)
                    val radius1 = (size.width / 2) - strokeWidth / 2
                    drawCircle(
                        color = Color(0xFFD1FAE5),
                        radius = radius1,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = Color(0xFF10B981),
                        startAngle = -90f,
                        sweepAngle = 360f * stepsProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - radius1, center.y - radius1),
                        size = Size(radius1 * 2, radius1 * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Middle Ring - Calories (Orange)
                    val radius2 = radius1 - strokeWidth - 5.dp.toPx()
                    drawCircle(
                        color = Color(0xFFFFEDD5),
                        radius = radius2,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = Color(0xFFF97316),
                        startAngle = -90f,
                        sweepAngle = 360f * calProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - radius2, center.y - radius2),
                        size = Size(radius2 * 2, radius2 * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Inner Ring - Distance (Blue)
                    val radius3 = radius2 - strokeWidth - 5.dp.toPx()
                    drawCircle(
                        color = Color(0xFFDBEAFE),
                        radius = radius3,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = Color(0xFF3B82F6),
                        startAngle = -90f,
                        sweepAngle = 360f * distProgress,
                        useCenter = false,
                        topLeft = Offset(center.x - radius3, center.y - radius3),
                        size = Size(radius3 * 2, radius3 * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Today",
                        fontFamily = AppFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = steps?.let { String.format("%,d", it) } ?: "--",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "steps",
                        fontFamily = AppFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Goals Breakdown List
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Steps Breakdown
                GoalBreakdownRow(
                    icon = Icons.Default.DirectionsWalk,
                    iconBg = Color(0xFFD1FAE5),
                    accentColor = Color(0xFF10B981),
                    title = "Steps",
                    valueText = steps?.let { String.format("%,d", it) } ?: "--",
                    goalText = "/ ${String.format("%,d", stepGoal)}",
                    pctText = "${(stepsProgress * 100).toInt()}%",
                    progress = stepsProgress
                )

                // Calories Breakdown
                GoalBreakdownRow(
                    icon = Icons.Default.LocalFireDepartment,
                    iconBg = Color(0xFFFFEDD5),
                    accentColor = Color(0xFFF97316),
                    title = "Calories",
                    valueText = calories?.toString() ?: "--",
                    goalText = "/ $calGoal kcal",
                    pctText = "${(calProgress * 100).toInt()}%",
                    progress = calProgress
                )

                // Distance Breakdown
                GoalBreakdownRow(
                    icon = Icons.Default.Place,
                    iconBg = Color(0xFFDBEAFE),
                    accentColor = Color(0xFF3B82F6),
                    title = "Distance",
                    valueText = distanceKm?.let { String.format("%.1f", it) } ?: "--",
                    goalText = "/ ${distGoal} km",
                    pctText = "${(distProgress * 100).toInt()}%",
                    progress = distProgress
                )
            }
        }
    }
}

@Composable
fun GoalBreakdownRow(
    icon: ImageVector,
    iconBg: Color,
    accentColor: Color,
    title: String,
    valueText: String,
    goalText: String,
    pctText: String,
    progress: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = AppFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = valueText,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = goalText,
                        fontFamily = AppFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = pctText,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accentColor,
            trackColor = iconBg
        )
    }
}

@Composable
fun StepsBarChartCard(
    steps: Int?,
    intervals: List<StepInterval>,
    selectedRange: ActivityTimeRange
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title, Value, Dropdown Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Steps",
                        fontFamily = AppFontFamily,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = steps?.let { String.format("%,d", it) } ?: "--",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier.clickable { }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedRange) {
                                ActivityTimeRange.DAY -> "Today"
                                ActivityTimeRange.WEEK -> "This Week"
                                ActivityTimeRange.MONTH -> "This Month"
                                ActivityTimeRange.YEAR -> "This Year"
                            },
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "▾",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Interactive Bar Chart Canvas
            val xLabels = when (selectedRange) {
                ActivityTimeRange.DAY -> listOf("12 AM", "6 AM", "12 PM", "6 PM", "12 AM")
                ActivityTimeRange.WEEK -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                ActivityTimeRange.MONTH -> listOf("W1", "W2", "W3", "W4")
                ActivityTimeRange.YEAR -> listOf("Jan", "Apr", "Jul", "Oct", "Dec")
            }

            // Create bar data height ratios based on step intervals or default profile
            val sampleBars = remember(intervals, selectedRange) {
                if (intervals.isNotEmpty()) {
                    val buckets = FloatArray(24) { 0f }
                    intervals.forEach { item ->
                        val hour = item.timestamp.atZone(ZoneId.systemDefault()).hour
                        buckets[hour] += item.steps.toFloat()
                    }
                    val maxVal = max(1f, buckets.maxOrNull() ?: 1f)
                    buckets.map { (it / maxVal).coerceIn(0.05f, 1.0f) }
                } else {
                    // Clean subtle visual bar profile when initial empty
                    listOf(
                        0.05f, 0.05f, 0.05f, 0.05f, 0.08f, 0.15f, 0.35f, 0.55f,
                        0.85f, 0.40f, 0.25f, 0.10f, 0.20f, 0.30f, 0.15f, 0.45f,
                        0.60f, 0.90f, 0.75f, 0.40f, 0.20f, 0.10f, 0.05f, 0.05f
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = 6.dp.toPx()
                        val chartHeight = size.height - 24.dp.toPx()
                        val availableWidth = size.width
                        val stepX = availableWidth / (sampleBars.size - 1)

                        // Subtle Horizontal Grid Lines
                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val y = chartHeight * (i.toFloat() / gridLines)
                            drawLine(
                                color = Color(0xFFF3F4F6),
                                start = Offset(0f, y),
                                end = Offset(availableWidth, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw Rounded Vertical Bars
                        sampleBars.forEachIndexed { index, ratio ->
                            val x = index * stepX
                            val barHeight = chartHeight * ratio
                            val topY = chartHeight - barHeight

                            drawRoundRect(
                                color = Color(0xFF10B981),
                                topLeft = Offset(x - barWidth / 2, topY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }

                    // X-Axis Labels Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        xLabels.forEach { label ->
                            Text(
                                text = label,
                                fontFamily = AppFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Y-Axis Labels Column
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("1.5K", "1K", "500", "0").forEach { yLabel ->
                        Text(
                            text = yLabel,
                            fontFamily = AppFontFamily,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStripCard(
    modifier: Modifier = Modifier,
    title: String,
    valueText: String,
    icon: ImageVector,
    iconBgColor: Color,
    accentColor: Color,
    yLabels: List<String>,
    xLabels: List<String>
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)),
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontFamily = AppFontFamily,
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = valueText,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = 4.dp.toPx()
                        val chartHeight = size.height - 16.dp.toPx()
                        val availableWidth = size.width
                        val barsCount = 10
                        val stepX = availableWidth / (barsCount - 1)

                        val miniBars = listOf(0.2f, 0.4f, 0.8f, 0.3f, 0.1f, 0.6f, 0.9f, 0.5f, 0.2f, 0.1f)

                        miniBars.forEachIndexed { idx, ratio ->
                            val x = idx * stepX
                            val bHeight = chartHeight * ratio
                            val topY = chartHeight - bHeight

                            drawRoundRect(
                                color = accentColor,
                                topLeft = Offset(x - barWidth / 2, topY),
                                size = Size(barWidth, bHeight),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = xLabels.first(),
                            fontFamily = AppFontFamily,
                            fontSize = 8.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = xLabels.last(),
                            fontFamily = AppFontFamily,
                            fontSize = 8.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    yLabels.take(2).forEach { y ->
                        Text(
                            text = y,
                            fontFamily = AppFontFamily,
                            fontSize = 8.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutRowItem(workout: SavedWorkout) {
    val icon = when (workout.type.lowercase()) {
        "running", "run" -> Icons.Default.DirectionsRun
        "cycling", "cycle" -> Icons.Default.DirectionsBike
        "walking", "walk" -> Icons.Default.DirectionsWalk
        else -> Icons.Default.FitnessCenter
    }

    val iconBg = when (workout.type.lowercase()) {
        "running", "run" -> Color(0xFFF3E8FF)
        "cycling", "cycle" -> Color(0xFFDBEAFE)
        "walking", "walk" -> Color(0xFFD1FAE5)
        else -> Color(0xFFFFEDD5)
    }

    val iconTint = when (workout.type.lowercase()) {
        "running", "run" -> Color(0xFF8B5CF6)
        "cycling", "cycle" -> Color(0xFF3B82F6)
        "walking", "walk" -> Color(0xFF10B981)
        else -> Color(0xFFF97316)
    }

    val timeStr = remember(workout.startTime) {
        val dt = workout.startTime.atZone(ZoneId.systemDefault())
        dt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = workout.type,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

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

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
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
        color = Color(0xFF2563EB),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    )
}
