package com.cmfwatch.companion.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.storage.SavedWorkout
import com.cmfwatch.companion.storage.UserGoalStore
import com.cmfwatch.companion.ui.components.CmfAppHeader
import com.cmfwatch.companion.ui.components.CmfPulseDot
import com.cmfwatch.companion.ui.components.isCmfConnected
import com.cmfwatch.companion.ui.theme.CmfBackground
import com.cmfwatch.companion.ui.theme.CmfError
import com.cmfwatch.companion.ui.theme.CmfOnSurface
import com.cmfwatch.companion.ui.theme.CmfOnSurfaceVariant
import com.cmfwatch.companion.ui.theme.CmfOutlineVariant
import com.cmfwatch.companion.ui.theme.CmfPrimary
import com.cmfwatch.companion.ui.theme.CmfPrimaryContainer
import com.cmfwatch.companion.ui.theme.CmfSecondaryContainer
import com.cmfwatch.companion.ui.theme.CmfSurfaceContainer
import com.cmfwatch.companion.ui.theme.CmfSurfaceLow
import com.cmfwatch.companion.ui.theme.CmfSurfaceLowest
import com.cmfwatch.companion.ui.theme.CmfTertiary
import com.cmfwatch.companion.ui.theme.CmfTertiaryContainer
import com.cmfwatch.companion.ui.theme.HeadlineFontFamily
import com.cmfwatch.companion.ui.theme.InterFontFamily
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(32.dp)
private val PillShape = RoundedCornerShape(50)
private val CardShadow = Color(0x14000000)

@Composable
fun HomeScreen(
    summary: DashboardSummary,
    onActivityClick: () -> Unit = {},
    onHeartRateClick: () -> Unit = {},
    onSleepClick: () -> Unit = {},
    onWatchCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val goals = remember(context) { UserGoalStore(context).getGoals() }

    val moveVal = summary.todayCaloriesKcal ?: 0
    val moveGoal = goals.caloriesGoal
    val moveProgress = (moveVal.toFloat() / moveGoal.toFloat()).coerceIn(0f, 1.2f)

    val exerciseVal = remember(summary.workoutsToday, summary.todaySteps) {
        val fromWorkouts = summary.workoutsToday.sumOf { it.durationMinutes }
        when {
            fromWorkouts > 0 -> fromWorkouts
            summary.todaySteps != null && summary.todaySteps > 0 -> (summary.todaySteps / 120).coerceAtMost(90)
            else -> 0
        }
    }
    val exerciseGoal = goals.exerciseMinutesGoal
    val exerciseProgress = (exerciseVal.toFloat() / exerciseGoal.toFloat()).coerceIn(0f, 1.2f)

    val standVal = remember(summary.stepIntervalsToday) {
        summary.stepIntervalsToday
            .map { it.timestamp.atZone(ZoneId.systemDefault()).hour }
            .distinct()
            .size
    }
    val standGoal = goals.standHoursGoal
    val standProgress = (standVal.toFloat() / standGoal.toFloat()).coerceIn(0f, 1.2f)

    val completedPct = (((moveProgress.coerceAtMost(1f) +
        exerciseProgress.coerceAtMost(1f) +
        standProgress.coerceAtMost(1f)) / 3f) * 100f).roundToInt()
    val goalPct = completedPct.coerceAtLeast(
        ((moveProgress.coerceAtMost(1f) * 100f)).roundToInt()
    )

    val sleepMinutes = summary.lastSleepMinutes
    val sleepScore = sleepMinutes?.let { ((it / 480f) * 100f).roundToInt().coerceIn(0, 100) }
    val hrMin = summary.hrSamplesToday.minOfOrNull { it.bpm }
    val hrMax = summary.hrSamplesToday.maxOfOrNull { it.bpm }
    val hrPoints = remember(summary.hrSamplesToday) {
        summary.hrSamplesToday.takeLast(24).map { it.bpm.toFloat() }
    }

    val latestWorkout = summary.workoutsToday.maxByOrNull { it.startTime }
    val remainingKcal = max(0, moveGoal - moveVal)

    val readiness = remember(sleepScore, summary.latestHeartRate, summary.latestStressScore) {
        deriveReadiness(sleepScore, summary.latestHeartRate, summary.latestStressScore)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CmfBackground)
    ) {
        CmfAppHeader(
            connected = summary.connectionState.isCmfConnected(),
            battery = summary.deviceBatteryLevel,
            onAvatarClick = onWatchCardClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 132.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                DateGreetingRow(lastSyncedAt = summary.lastSyncedAt)
            }

            item {
                ActivityRingsCard(
                    completedPct = completedPct,
                    goalPct = goalPct,
                    moveVal = moveVal,
                    moveGoal = moveGoal,
                    exerciseVal = exerciseVal,
                    exerciseGoal = exerciseGoal,
                    standVal = standVal,
                    standGoal = standGoal,
                    moveProgress = moveProgress.coerceAtMost(1f),
                    exerciseProgress = exerciseProgress.coerceAtMost(1f),
                    standProgress = standProgress.coerceAtMost(1f),
                    onClick = onActivityClick
                )
            }

            item {
                VitalsGlanceSection(
                    bpm = summary.latestHeartRate,
                    hrMin = hrMin,
                    hrMax = hrMax,
                    hrPoints = hrPoints,
                    sleepScore = sleepScore,
                    sleepMinutes = sleepMinutes,
                    spo2 = summary.latestSpO2,
                    stress = summary.latestStressScore,
                    onHeartRateClick = onHeartRateClick,
                    onSleepClick = onSleepClick
                )
            }

            item {
                ReadinessCard(readiness = readiness, onSuggestedClick = onActivityClick)
            }

            item {
                LatestWorkoutSection(
                    workout = latestWorkout,
                    fallbackDistanceKm = summary.todayDistanceKm,
                    fallbackKcal = summary.todayCaloriesKcal,
                    onViewAll = onActivityClick
                )
            }

            item {
                DailyMilestoneBanner(remainingKcal = remainingKcal)
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun DateGreetingRow(lastSyncedAt: Instant?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
                    .uppercase(Locale.getDefault()),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = CmfOnSurfaceVariant
            )
            Text(
                text = "Today",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp,
                color = CmfOnSurface
            )
        }
        Row(
            modifier = Modifier
                .shadow(1.dp, PillShape)
                .clip(PillShape)
                .background(CmfSurfaceLowest)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CmfPulseDot(color = CmfTertiaryContainer, size = 8.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatSynced(lastSyncedAt),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = CmfOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityRingsCard(
    completedPct: Int,
    goalPct: Int,
    moveVal: Int,
    moveGoal: Int,
    exerciseVal: Int,
    exerciseGoal: Int,
    standVal: Int,
    standGoal: Int,
    moveProgress: Float,
    exerciseProgress: Float,
    standProgress: Float,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, CardShape, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(CardShape)
            .background(CmfSurfaceLowest)
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Adjust,
                    contentDescription = null,
                    tint = CmfPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Activity Rings",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = CmfOnSurface
                )
            }
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(CmfSurfaceLow)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "GOAL $goalPct%",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = CmfOnSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(252.dp),
            contentAlignment = Alignment.Center
        ) {
            ConcentricActivityRings(
                move = moveProgress,
                exercise = exerciseProgress,
                stand = standProgress,
                modifier = Modifier.fillMaxSize()
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$completedPct",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 34.sp,
                        letterSpacing = (-0.7).sp,
                        color = CmfOnSurface
                    )
                    Text(
                        text = "%",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = CmfOnSurface,
                        modifier = Modifier.padding(bottom = 4.dp, start = 1.dp)
                    )
                }
                Text(
                    text = "COMPLETED",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = CmfOnSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RingMetricChip(
                modifier = Modifier.weight(1f),
                label = "MOVE",
                value = "$moveVal",
                subtitle = "/ $moveGoal kcal",
                dot = CmfPrimaryContainer
            )
            RingMetricChip(
                modifier = Modifier.weight(1f),
                label = "EXERCISE",
                value = "$exerciseVal",
                subtitle = "/ $exerciseGoal min",
                dot = CmfTertiaryContainer
            )
            RingMetricChip(
                modifier = Modifier.weight(1f),
                label = "STAND",
                value = "$standVal",
                subtitle = "/ $standGoal hrs",
                dot = CmfSecondaryContainer
            )
        }
    }
}

@Composable
private fun ConcentricActivityRings(
    move: Float,
    exercise: Float,
    stand: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 11.dp.toPx()
        val gap = 10.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = size.minDimension / 2f - stroke / 2f - 6.dp.toPx()
        val mid = outer - stroke - gap
        val inner = mid - stroke - gap
        val track = CmfSurfaceContainer

        fun ring(radius: Float, progress: Float, color: Color) {
            val topLeft = Offset(cx - radius, cy - radius)
            val arcSize = Size(radius * 2, radius * 2)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        ring(outer, move, CmfPrimaryContainer)
        ring(mid, exercise, CmfTertiaryContainer)
        ring(inner, stand, CmfSecondaryContainer)
    }
}

@Composable
private fun RingMetricChip(
    label: String,
    value: String,
    subtitle: String,
    dot: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CmfSurfaceLow)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dot)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = CmfOnSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = HeadlineFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = CmfOnSurface
        )
        Text(
            text = subtitle,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = CmfOnSurfaceVariant
        )
    }
}

@Composable
private fun VitalsGlanceSection(
    bpm: Int?,
    hrMin: Int?,
    hrMax: Int?,
    hrPoints: List<Float>,
    sleepScore: Int?,
    sleepMinutes: Int?,
    spo2: Int?,
    stress: Int?,
    onHeartRateClick: () -> Unit,
    onSleepClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vitals Glance",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CmfOnSurface
            )
            Text(
                text = "Live stream",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = CmfPrimary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeartRateCard(
                modifier = Modifier.weight(1f),
                bpm = bpm,
                min = hrMin,
                max = hrMax,
                points = hrPoints,
                onClick = onHeartRateClick
            )
            SleepCard(
                modifier = Modifier.weight(1f),
                score = sleepScore,
                minutes = sleepMinutes,
                onClick = onSleepClick
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BloodOxygenCard(modifier = Modifier.weight(1f), spo2 = spo2)
            StressCard(modifier = Modifier.weight(1f), score = stress)
        }
    }
}

@Composable
private fun VitalCardScaffold(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .height(176.dp)
            .shadow(1.dp, CardShape, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(CardShape)
            .background(CmfSurfaceLowest)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@Composable
private fun HeartRateCard(
    bpm: Int?,
    min: Int?,
    max: Int?,
    points: List<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    VitalCardScaffold(modifier = modifier, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "HEART RATE",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
                color = CmfOnSurfaceVariant
            )
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = CmfError,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = bpm?.toString() ?: "--",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = (-0.7).sp,
                    color = CmfOnSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BPM",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = CmfOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(
                text = if (min != null && max != null) "$min - $max range" else "Waiting for samples",
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = CmfOnSurfaceVariant
            )
        }
        Sparkline(points = points, color = CmfError, modifier = Modifier.fillMaxWidth().height(32.dp))
    }
}

@Composable
private fun SleepCard(
    score: Int?,
    minutes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = minutes?.let { "${it / 60}h ${it % 60}m" } ?: "--"
    val delta = minutes?.let { it - 438 }
    VitalCardScaffold(modifier = modifier, onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "SLEEP",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
                color = CmfOnSurfaceVariant
            )
            Icon(
                imageVector = Icons.Outlined.Bedtime,
                contentDescription = null,
                tint = CmfSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = score?.toString() ?: "--",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = (-0.7).sp,
                    color = CmfOnSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "/ 100",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = CmfOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(
                text = when {
                    score == null -> "No sleep data"
                    score >= 80 -> "Optimal rest"
                    score >= 60 -> "Fair recovery"
                    else -> "Needs rest"
                },
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = CmfTertiary
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(CmfSurfaceLow)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = duration,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = CmfOnSurface
            )
            Text(
                text = when {
                    delta == null -> ""
                    delta >= 0 -> "+${delta}m"
                    else -> "${delta}m"
                },
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = CmfTertiary
            )
        }
    }
}

@Composable
private fun BloodOxygenCard(spo2: Int?, modifier: Modifier = Modifier) {
    VitalCardScaffold(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "BLOOD OXYGEN",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
                color = CmfOnSurfaceVariant
            )
            Icon(
                imageVector = Icons.Outlined.Air,
                contentDescription = null,
                tint = CmfSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = spo2?.toString() ?: "--",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = (-0.7).sp,
                    color = CmfOnSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = CmfOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(
                text = when {
                    spo2 == null -> "No reading yet"
                    spo2 >= 95 -> "Baseline normal"
                    else -> "Below baseline"
                },
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = CmfOnSurfaceVariant
            )
        }
        ThinProgress(progress = (spo2 ?: 0) / 100f, color = CmfSecondaryContainer)
    }
}

@Composable
private fun StressCard(score: Int?, modifier: Modifier = Modifier) {
    VitalCardScaffold(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "STRESS",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
                color = CmfOnSurfaceVariant
            )
            Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = null,
                tint = CmfTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = score?.toString() ?: "--",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = (-0.7).sp,
                    color = CmfOnSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "/ 100",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = CmfOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(
                text = when {
                    score == null -> "No reading yet"
                    score <= 30 -> "Relaxed state"
                    score <= 60 -> "Moderate load"
                    else -> "High strain"
                },
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = CmfTertiary
            )
        }
        ThinProgress(progress = (score ?: 0) / 100f, color = CmfTertiaryContainer)
    }
}

@Composable
private fun ThinProgress(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(PillShape)
            .background(CmfSurfaceContainer)
    ) {
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .clip(PillShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun Sparkline(points: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.size < 2) {
            val w = size.width
            val h = size.height
            val line = Path().apply {
                moveTo(0f, h * 0.75f)
                cubicTo(w * 0.12f, h * 0.88f, w * 0.22f, h * 0.45f, w * 0.32f, h * 0.56f)
                cubicTo(w * 0.42f, h * 0.68f, w * 0.48f, h * 0.38f, w * 0.58f, h * 0.44f)
                cubicTo(w * 0.70f, h * 0.52f, w * 0.78f, h * 0.72f, w * 0.88f, h * 0.38f)
                lineTo(w, h * 0.28f)
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(fill, color.copy(alpha = 0.10f))
            drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            return@Canvas
        }
        val min = points.min()
        val max = points.max().coerceAtLeast(min + 1f)
        val line = Path()
        val fill = Path()
        points.forEachIndexed { i, v ->
            val x = size.width * (i / (points.lastIndex.toFloat()))
            val y = size.height * (1f - ((v - min) / (max - min)))
            if (i == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(fill, color.copy(alpha = 0.12f))
        drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun ReadinessCard(readiness: Readiness, onSuggestedClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, CardShape, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(CardShape)
            .background(CmfSurfaceLowest)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CmfTertiaryContainer.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BatteryChargingFull,
                        contentDescription = null,
                        tint = CmfTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "DAILY READINESS",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                        color = CmfOnSurfaceVariant
                    )
                    Text(
                        text = "Score: ${readiness.score} · ${readiness.band}",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CmfOnSurface
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(CmfTertiaryContainer.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = readiness.badge,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    color = CmfTertiary
                )
            }
        }
        Text(
            text = readiness.blurb,
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = CmfOnSurfaceVariant
        )
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(CmfSurfaceContainer)
                .clickable(onClick = onSuggestedClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = CmfPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = readiness.suggestion,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = CmfOnSurface
            )
        }
    }
}

@Composable
private fun LatestWorkoutSection(
    workout: SavedWorkout?,
    fallbackDistanceKm: Float?,
    fallbackKcal: Int?,
    onViewAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Latest Workout",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CmfOnSurface
            )
            Row(
                modifier = Modifier.clickable(onClick = onViewAll),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View all",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = CmfPrimary
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = CmfPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        val title = workout?.type?.ifBlank { null } ?: if (fallbackDistanceKm != null) "Today's Activity" else "No workout yet"
        val timeLabel = workout?.startTime?.atZone(ZoneId.systemDefault())
            ?.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
            ?.uppercase(Locale.getDefault())
            ?: "--"
        val dist = workout?.distanceKm ?: fallbackDistanceKm
        val duration = workout?.let { formatDuration(it.durationMinutes) } ?: "--"
        val kcal = workout?.caloriesKcal ?: fallbackKcal

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, CardShape, ambientColor = CardShadow, spotColor = CardShadow)
                .clip(CardShape)
                .background(CmfSurfaceLowest)
                .clickable(onClick = onViewAll)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CmfSurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                RouteThumb()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CmfPrimary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsRun,
                        contentDescription = null,
                        tint = CmfPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = CmfOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(CmfSurfaceLow)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = timeLabel,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 0.4.sp,
                            color = CmfOnSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dist?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CmfOnSurface
                    )
                    Text(
                        text = " km",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = CmfOnSurfaceVariant
                    )
                    Dot()
                    Text(
                        text = duration,
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = CmfOnSurface
                    )
                    Dot()
                    Text(
                        text = kcal?.toString() ?: "--",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = CmfPrimary
                    )
                    Text(
                        text = " kcal",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = CmfOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteThumb() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val w = size.width
        val h = size.height
        drawRoundRect(Color(0xFFE8EDE4), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
        drawRoundRect(Color(0xFFD7E4CC), topLeft = Offset(w * 0.08f, h * 0.12f), size = Size(w * 0.38f, h * 0.42f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
        drawRoundRect(Color(0xFFCDD8C6), topLeft = Offset(w * 0.52f, h * 0.38f), size = Size(w * 0.40f, h * 0.48f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
        path.moveTo(w * 0.18f, h * 0.72f)
        path.cubicTo(w * 0.28f, h * 0.30f, w * 0.45f, h * 0.78f, w * 0.58f, h * 0.40f)
        path.cubicTo(w * 0.70f, h * 0.12f, w * 0.82f, h * 0.55f, w * 0.88f, h * 0.28f)
        drawPath(path, CmfPrimaryContainer, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(4.dp)
            .clip(CircleShape)
            .background(CmfOutlineVariant)
    )
}

@Composable
private fun DailyMilestoneBanner(remainingKcal: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CmfSurfaceLow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CmfPrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "DAILY MILESTONE",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
                color = CmfOnSurfaceVariant
            )
            Text(
                text = if (remainingKcal > 0) {
                    "Only $remainingKcal kcal left to close all three rings today!"
                } else {
                    "All three rings are closed. Outstanding work today!"
                },
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = CmfOnSurface
            )
        }
    }
}

private fun formatSynced(instant: Instant?): String {
    if (instant == null) return "Not synced"
    val mins = Duration.between(instant, Instant.now()).toMinutes()
    return when {
        mins < 1 -> "Synced just now"
        mins < 60 -> "Synced ${mins}m ago"
        else -> "Synced ${mins / 60}h ago"
    }
}

private fun formatDuration(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:00", hours, minutes)
    } else {
        String.format(Locale.US, "%d:%02d", totalMinutes, 0)
    }
}

private data class Readiness(
    val score: Int,
    val band: String,
    val badge: String,
    val blurb: String,
    val suggestion: String
)

private fun deriveReadiness(sleepScore: Int?, hr: Int?, stress: Int?): Readiness {
    val sleepPart = sleepScore ?: 70
    val hrPart = when {
        hr == null -> 75
        hr < 60 -> 95
        hr < 70 -> 88
        hr < 80 -> 72
        else -> 58
    }
    val stressPart = when {
        stress == null -> 80
        stress <= 24 -> 94
        stress <= 40 -> 80
        stress <= 60 -> 62
        else -> 45
    }
    val score = ((sleepPart * 0.45f) + (hrPart * 0.3f) + (stressPart * 0.25f)).roundToInt().coerceIn(0, 100)
    val (band, badge) = when {
        score >= 85 -> "High" to "READY"
        score >= 70 -> "Steady" to "GO"
        else -> "Low" to "REST"
    }
    val blurb = when {
        score >= 85 -> "Your resting heart rate is lower than baseline and deep sleep was restorative. Great day for a high-intensity session."
        score >= 70 -> "Recovery looks solid. Keep intensity moderate and stay hydrated through the afternoon."
        else -> "Recovery is lagging. Prioritize easy movement, daylight, and an earlier wind-down tonight."
    }
    val suggestion = when {
        score >= 85 -> "Suggested: 5km Outdoor Run"
        score >= 70 -> "Suggested: Zone 2 Walk"
        else -> "Suggested: Recovery Stretch"
    }
    return Readiness(score, band, badge, blurb, suggestion)
}
