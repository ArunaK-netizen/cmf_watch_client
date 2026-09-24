package com.cmfwatch.companion.ui.activity

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.storage.SavedWorkout
import com.cmfwatch.companion.storage.UserGoalStore
import com.cmfwatch.companion.storage.UserGoals
import com.cmfwatch.companion.ui.components.CmfAppHeader
import com.cmfwatch.companion.ui.components.cmfCard
import com.cmfwatch.companion.ui.components.isCmfConnected
import com.cmfwatch.companion.ui.theme.CmfBackground
import com.cmfwatch.companion.ui.theme.CmfError
import com.cmfwatch.companion.ui.theme.CmfErrorContainer
import com.cmfwatch.companion.ui.theme.CmfOnErrorContainer
import com.cmfwatch.companion.ui.theme.CmfOnPrimary
import com.cmfwatch.companion.ui.theme.CmfOnSecondaryFixedVariant
import com.cmfwatch.companion.ui.theme.CmfOnSurface
import com.cmfwatch.companion.ui.theme.CmfOnSurfaceVariant
import com.cmfwatch.companion.ui.theme.CmfOnTertiaryFixed
import com.cmfwatch.companion.ui.theme.CmfPrimary
import com.cmfwatch.companion.ui.theme.CmfPrimaryContainer
import com.cmfwatch.companion.ui.theme.CmfPrimaryFixed
import com.cmfwatch.companion.ui.theme.CmfSecondary
import com.cmfwatch.companion.ui.theme.CmfSecondaryContainer
import com.cmfwatch.companion.ui.theme.CmfSecondaryFixed
import com.cmfwatch.companion.ui.theme.CmfSecondaryFixedDim
import com.cmfwatch.companion.ui.theme.CmfSurfaceContainer
import com.cmfwatch.companion.ui.theme.CmfSurfaceHigh
import com.cmfwatch.companion.ui.theme.CmfSurfaceLow
import com.cmfwatch.companion.ui.theme.CmfSurfaceLowest
import com.cmfwatch.companion.ui.theme.CmfTertiary
import com.cmfwatch.companion.ui.theme.CmfTertiaryContainer
import com.cmfwatch.companion.ui.theme.CmfTertiaryFixed
import com.cmfwatch.companion.ui.theme.HeadlineFontFamily
import com.cmfwatch.companion.ui.theme.InterFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val PillShape = RoundedCornerShape(50)
private val InnerShape = RoundedCornerShape(16.dp)
private val OrangeGlow = Color(0x59FF5722)

@Composable
fun ActivityScreen(
    summary: DashboardSummary,
    onAvatarClick: () -> Unit = {},
    onStartWorkout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val goalStore = remember { UserGoalStore(context) }
    var goals by remember { mutableStateOf(goalStore.getGoals()) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var startLabel by remember { mutableStateOf("Start Workout") }
    val scope = rememberCoroutineScope()

    val workouts = summary.workoutsToday
    val weeklyKm = workouts.sumOf { it.distanceKm.toDouble() }.toFloat()
    val weeklyGoal = goals.weeklyDistanceGoalKm
    val goalPct = if (weeklyGoal > 0f) ((weeklyKm / weeklyGoal) * 100f).roundToInt().coerceAtMost(999) else 0
    val activeMinutes = workouts.sumOf { it.durationMinutes }
    val energy = workouts.sumOf { it.caloriesKcal }
    val avgPace = averagePace(workouts)
    val latest = workouts.maxByOrNull { it.startTime }
    val dayBars = remember(workouts) { weeklyBars(workouts) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CmfBackground)
    ) {
        CmfAppHeader(
            connected = summary.connectionState.isCmfConnected(),
            battery = summary.deviceBatteryLevel,
            onAvatarClick = onAvatarClick
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "FITNESS & TRAINING",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.66.sp,
                                color = CmfOnSurfaceVariant
                            )
                            Text(
                                text = "Workouts",
                                fontFamily = HeadlineFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                letterSpacing = (-0.56).sp,
                                color = CmfOnSurface
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(CmfSurfaceContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sensors,
                                contentDescription = null,
                                tint = CmfTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GPS Active",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = CmfOnSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, PillShape, ambientColor = OrangeGlow, spotColor = OrangeGlow)
                            .clip(PillShape)
                            .background(CmfPrimaryContainer)
                            .clickable {
                                onStartWorkout()
                                scope.launch {
                                    startLabel = "Starting GPS..."
                                    delay(1200)
                                    startLabel = "Start Workout"
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                tint = CmfOnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = startLabel,
                                fontFamily = HeadlineFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = CmfOnPrimary
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(CmfOnPrimary.copy(alpha = 0.20f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Watch,
                                contentDescription = null,
                                tint = CmfOnPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CMF PRO",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.66.sp,
                                color = CmfOnPrimary
                            )
                        }
                    }
                }
            }

            item {
                WeeklyVolumeCard(
                    weeklyKm = weeklyKm,
                    weeklyGoal = weeklyGoal,
                    goalPct = goalPct,
                    bars = dayBars,
                    avgPace = avgPace,
                    activeMinutes = activeMinutes,
                    sessions = workouts.size,
                    energy = energy
                )
            }

            item {
                PresetModesSection(onCustomize = { showGoalDialog = true })
            }

            item {
                LatestSessionCard(
                    workout = latest,
                    avgHr = summary.latestHeartRate,
                    onShare = {
                        val text = latest?.let {
                            "${it.type} · ${String.format(Locale.US, "%.2f", it.distanceKm)} km · ${it.durationMinutes} min"
                        } ?: "No workout yet"
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                },
                                "Share session"
                            )
                        )
                    }
                )
            }

            item {
                TrainingLoadCard(
                    load = deriveLoad(activeMinutes, energy),
                    readyHours = if (activeMinutes > 0) 14 else 0
                )
            }
        }
    }

    if (showGoalDialog) {
        GoalSettingsDialog(
            currentGoals = goals,
            onDismiss = { showGoalDialog = false },
            onSave = {
                goalStore.saveGoals(it)
                goals = it
                showGoalDialog = false
            }
        )
    }
}

@Composable
private fun WeeklyVolumeCard(
    weeklyKm: Float,
    weeklyGoal: Float,
    goalPct: Int,
    bars: List<DayBar>,
    avgPace: String,
    activeMinutes: Int,
    sessions: Int,
    energy: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cmfCard()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CmfPrimaryContainer)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WEEKLY VOLUME",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.66.sp,
                        color = CmfOnSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = if (weeklyKm > 0f) String.format(Locale.US, "%.1f", weeklyKm) else "--",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        letterSpacing = (-1.4).sp,
                        color = CmfOnSurface
                    )
                    Text(
                        text = " km",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = CmfOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .clip(PillShape)
                    .background(CmfSurfaceLow)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "GOAL (${weeklyGoal.roundToInt()} km)",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.66.sp,
                    color = CmfOnSurfaceVariant
                )
                Text(
                    text = "$goalPct% on track",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = CmfPrimaryContainer
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(InnerShape)
                .background(CmfSurfaceLow.copy(alpha = 0.70f))
                .padding(16.dp)
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (bar.peakLabel != null) {
                            Text(
                                text = bar.peakLabel,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = CmfPrimaryContainer,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(bar.heightFrac.coerceIn(0.08f, 1f))
                                .clip(PillShape)
                                .background(bar.color)
                        )
                    }
                    Text(
                        text = bar.label,
                        fontFamily = InterFontFamily,
                        fontWeight = if (bar.isPeak) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (bar.isPeak) CmfPrimary else CmfOnSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(modifier = Modifier.weight(1f), label = "Avg Pace", value = avgPace, note = if (sessions > 0) "from $sessions sessions" else "No sessions", noteColor = CmfTertiary)
            StatChip(modifier = Modifier.weight(1f), label = "Active Time", value = formatHours(activeMinutes), note = "$sessions sessions")
            StatChip(modifier = Modifier.weight(1f), label = "Energy", value = if (energy > 0) String.format(Locale.US, "%,d", energy) else "--", note = "kcal burned")
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier,
    label: String,
    value: String,
    note: String,
    noteColor: Color = CmfOnSurfaceVariant
) {
    Column(
        modifier = modifier
            .clip(InnerShape)
            .background(CmfSurfaceLow.copy(alpha = 0.50f))
            .padding(10.dp)
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            color = CmfOnSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = HeadlineFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = CmfOnSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            text = note,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = noteColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun PresetModesSection(onCustomize: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preset Modes",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CmfOnSurface
            )
            Row(
                modifier = Modifier.clickable(onClick = onCustomize),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customize",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = CmfPrimary
                )
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = CmfPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeTile(
                modifier = Modifier.weight(1f),
                title = "Outdoor Run",
                subtitle = "GPS Locked · Ready",
                subtitleColor = CmfTertiary,
                icon = Icons.Outlined.DirectionsRun,
                iconBg = CmfPrimaryFixed,
                iconTint = CmfPrimary,
                trailing = { Icon(Icons.Outlined.GpsFixed, null, tint = CmfTertiary, modifier = Modifier.size(18.dp)) }
            )
            ModeTile(
                modifier = Modifier.weight(1f),
                title = "Outdoor Cycling",
                subtitle = "Cadence linked",
                icon = Icons.Outlined.DirectionsBike,
                iconBg = CmfSecondaryFixed,
                iconTint = CmfSecondary,
                trailing = { Icon(Icons.Outlined.Bluetooth, null, tint = CmfSecondary, modifier = Modifier.size(18.dp)) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeTile(
                modifier = Modifier.weight(1f),
                title = "HIIT & Strength",
                subtitle = "HR Zones configured",
                icon = Icons.Outlined.FitnessCenter,
                iconBg = CmfErrorContainer,
                iconTint = CmfError,
                trailing = {
                    Text(
                        text = "HR Z5",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = CmfOnSurfaceVariant,
                        modifier = Modifier
                            .clip(PillShape)
                            .background(CmfSurfaceContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            )
            ModeTile(
                modifier = Modifier.weight(1f),
                title = "Pool Swim",
                subtitle = "50m pool preset",
                icon = Icons.Outlined.Pool,
                iconBg = CmfSecondaryFixedDim,
                iconTint = CmfOnSecondaryFixedVariant,
                trailing = { Icon(Icons.Outlined.WaterDrop, null, tint = CmfOnSurfaceVariant, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

@Composable
private fun ModeTile(
    modifier: Modifier,
    title: String,
    subtitle: String,
    subtitleColor: Color = CmfOnSurfaceVariant,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    trailing: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .height(144.dp)
            .cmfCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            trailing()
        }
        Column {
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = CmfOnSurface
            )
            Text(
                text = subtitle,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = subtitleColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun LatestSessionCard(
    workout: SavedWorkout?,
    avgHr: Int?,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cmfCard()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.History, null, tint = CmfPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LATEST SESSION",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.66.sp,
                        color = CmfOnSurfaceVariant
                    )
                }
                Text(
                    text = workout?.type?.ifBlank { "Workout" } ?: "No session yet",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CmfOnSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = workout?.let { formatSessionWhen(it) } ?: "Start a workout on your CMF Watch",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = CmfOnSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CmfSurfaceContainer)
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = CmfOnSurface, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(InnerShape)
                .background(CmfSurfaceLow)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            ) {
                RouteMap()
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(PillShape)
                        .background(CmfSurfaceLowest.copy(alpha = 0.92f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = CmfTertiary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = workout?.let { String.format(Locale.US, "%.2f KM VERIFIED", it.distanceKm) } ?: "-- KM",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.4.sp,
                        color = CmfOnSurface
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CmfSurfaceLowest.copy(alpha = 0.80f))
                    .padding(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "ELEVATION GAIN: --",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = CmfOnSurfaceVariant
                    )
                    Text(
                        text = "Peak --",
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp,
                        color = CmfOnSurfaceVariant
                    )
                }
                ElevationSpark(modifier = Modifier.fillMaxWidth().height(32.dp))
            }
        }

        val pace = workout?.let { formatPace(it) } ?: "--"
        val km = workout?.let { String.format(Locale.US, "%.2f", it.distanceKm) } ?: "--"
        val zone = avgHr?.let { hrZone(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCell(modifier = Modifier.weight(1f), label = "Total Distance", value = km, unit = "km")
            MetricCell(modifier = Modifier.weight(1f), label = "Avg Pace", value = pace, unit = "/km")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCell(
                modifier = Modifier.weight(1f),
                label = "Avg Heart Rate",
                value = avgHr?.toString() ?: "--",
                unit = "bpm",
                valueColor = CmfError,
                badge = zone
            )
            MetricCell(modifier = Modifier.weight(1f), label = "Cadence", value = "--", unit = "spm")
        }

        val kmCount = workout?.distanceKm?.toInt()?.coerceIn(1, 6) ?: 0
        if (kmCount > 0 && workout != null) {
            val even = formatPaceClock(workout)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "SPLITS BREAKDOWN (PACE)",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.66.sp,
                        color = CmfOnSurfaceVariant
                    )
                    Text(
                        text = "All $kmCount km",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = CmfPrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(kmCount) { i ->
                        val highlight = i == (kmCount / 2).coerceAtLeast(0)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (highlight) CmfPrimaryFixed.copy(alpha = 0.40f) else CmfSurfaceLow.copy(alpha = 0.50f))
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "km ${i + 1}",
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                color = if (highlight) CmfPrimary else CmfOnSurfaceVariant
                            )
                            Text(
                                text = even,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (highlight) CmfPrimary else CmfOnSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCell(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    valueColor: Color = CmfOnSurface,
    badge: String? = null
) {
    Column(
        modifier = modifier
            .clip(InnerShape)
            .background(CmfSurfaceLow.copy(alpha = 0.60f))
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = CmfOnSurfaceVariant
            )
            if (badge != null) {
                Text(
                    text = badge,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    color = CmfOnErrorContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CmfErrorContainer)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                letterSpacing = (-0.7).sp,
                color = valueColor
            )
            Text(
                text = " $unit",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = CmfOnSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}

@Composable
private fun RouteMap() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        val grid = Color(0xFFE2E2E5)
        listOf(0.18f, 0.50f, 0.82f).forEach { y ->
            drawLine(grid, Offset(0f, size.height * y), Offset(size.width, size.height * y), 2f, pathEffect = dash)
        }
        listOf(0.16f, 0.50f, 0.84f).forEach { x ->
            drawLine(grid, Offset(size.width * x, 0f), Offset(size.width * x, size.height), 2f, pathEffect = dash)
        }
        val path = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.62f)
            cubicTo(size.width * 0.22f, size.height * 0.58f, size.width * 0.28f, size.height * 0.22f, size.width * 0.38f, size.height * 0.24f)
            cubicTo(size.width * 0.50f, size.height * 0.26f, size.width * 0.52f, size.height * 0.78f, size.width * 0.64f, size.height * 0.72f)
            cubicTo(size.width * 0.74f, size.height * 0.66f, size.width * 0.82f, size.height * 0.20f, size.width * 0.90f, size.height * 0.70f)
        }
        drawPath(path, CmfPrimaryContainer, style = Stroke(width = 7f, cap = StrokeCap.Round))
        drawCircle(CmfTertiaryContainer, 10f, Offset(size.width * 0.08f, size.height * 0.62f))
        drawCircle(Color.White, 6f, Offset(size.width * 0.08f, size.height * 0.62f))
        drawCircle(CmfPrimaryContainer, 10f, Offset(size.width * 0.90f, size.height * 0.70f))
        drawCircle(Color.White, 5f, Offset(size.width * 0.90f, size.height * 0.70f))
    }
}

@Composable
private fun ElevationSpark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val line = Path().apply {
            moveTo(0f, size.height * 0.75f)
            cubicTo(size.width * 0.15f, size.height * 0.70f, size.width * 0.25f, size.height * 0.40f, size.width * 0.38f, size.height * 0.45f)
            cubicTo(size.width * 0.52f, size.height * 0.52f, size.width * 0.62f, size.height * 0.18f, size.width * 0.78f, size.height * 0.50f)
            lineTo(size.width, size.height * 0.62f)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(listOf(CmfPrimaryContainer.copy(alpha = 0.25f), Color.Transparent))
        )
        drawPath(line, CmfPrimaryContainer, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
    }
}

@Composable
private fun TrainingLoadCard(load: Int, readyHours: Int) {
    val band = when {
        load < 120 -> "Low"
        load < 340 -> "Optimal Training Load"
        else -> "Overreaching"
    }
    val badge = when {
        load < 120 -> "RECOVER"
        load < 340 -> "PRODUCTIVE"
        else -> "REST"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cmfCard()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CmfTertiaryFixed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.BatteryChargingFull, null, tint = CmfOnTertiaryFixed, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "PHYSIOLOGICAL LOAD",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.66.sp,
                        color = CmfOnSurfaceVariant
                    )
                    Text(
                        text = band,
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CmfOnSurface
                    )
                }
            }
            Text(
                text = badge,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.66.sp,
                color = CmfTertiary,
                modifier = Modifier
                    .clip(PillShape)
                    .background(CmfTertiaryContainer.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(PillShape)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(CmfSecondaryFixed))
                    Box(Modifier.weight(2f).fillMaxHeight().background(CmfTertiaryFixed))
                    Box(Modifier.weight(1f).fillMaxHeight().background(CmfErrorContainer))
                }
                val pin = (load / 500f).coerceIn(0.08f, 0.92f)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 0.dp)
                        .fillMaxWidth(pin)
                        .height(12.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CmfTertiary)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Low (120)", fontFamily = InterFontFamily, fontSize = 10.sp, color = CmfOnSurfaceVariant)
                Text("Optimal (340)", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = CmfTertiary)
                Text("Overreaching (500+)", fontFamily = InterFontFamily, fontSize = 10.sp, color = CmfOnSurfaceVariant)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(InnerShape)
                .background(CmfSurfaceLow)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CmfPrimaryContainer.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Timelapse, null, tint = CmfPrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (readyHours > 0) "$readyHours hours until peak readiness" else "Recovered — ready when you are",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = CmfOnSurface
                )
                Text(
                    text = "Ready for an interval tempo run by tomorrow 08:00 AM",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = CmfOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GoalSettingsDialog(
    currentGoals: UserGoals,
    onDismiss: () -> Unit,
    onSave: (UserGoals) -> Unit
) {
    var stepInput by remember { mutableStateOf(currentGoals.stepGoal.toString()) }
    var calInput by remember { mutableStateOf(currentGoals.caloriesGoal.toString()) }
    var distInput by remember { mutableStateOf(currentGoals.distanceGoalKm.toString()) }
    var weeklyInput by remember { mutableStateOf(currentGoals.weeklyDistanceGoalKm.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Training goals",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CmfOnSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = stepInput,
                    onValueChange = { stepInput = it },
                    label = { Text("Daily steps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = calInput,
                    onValueChange = { calInput = it },
                    label = { Text("Daily calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = distInput,
                    onValueChange = { distInput = it },
                    label = { Text("Daily distance (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weeklyInput,
                    onValueChange = { weeklyInput = it },
                    label = { Text("Weekly distance (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentGoals.copy(
                            stepGoal = stepInput.toIntOrNull() ?: currentGoals.stepGoal,
                            caloriesGoal = calInput.toIntOrNull() ?: currentGoals.caloriesGoal,
                            distanceGoalKm = distInput.toFloatOrNull() ?: currentGoals.distanceGoalKm,
                            weeklyDistanceGoalKm = weeklyInput.toFloatOrNull() ?: currentGoals.weeklyDistanceGoalKm
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private data class DayBar(
    val label: String,
    val heightFrac: Float,
    val color: Color,
    val isPeak: Boolean,
    val peakLabel: String?
)

private fun weeklyBars(workouts: List<SavedWorkout>): List<DayBar> {
    val zone = ZoneId.systemDefault()
    val km = FloatArray(7)
    workouts.forEach {
        val idx = it.startTime.atZone(zone).dayOfWeek.value - 1
        km[idx] += it.distanceKm
    }
    var max = 0.1f
    km.forEach { if (it > max) max = it }
    val today = LocalDate.now().dayOfWeek.value - 1
    val peakIdx = km.indices.maxByOrNull { km[it] }
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    return labels.mapIndexed { i, label ->
        val value = km[i]
        val isPeak = peakIdx == i && value > 0f
        DayBar(
            label = label,
            heightFrac = if (value <= 0f) 0.08f else (value / max).coerceIn(0.12f, 1f),
            color = when {
                isPeak -> CmfPrimaryContainer
                i == today && value > 0f -> CmfSecondaryContainer.copy(alpha = 0.80f)
                value > 0f -> CmfSurfaceHigh
                else -> CmfSurfaceContainer
            },
            isPeak = isPeak,
            peakLabel = if (isPeak) String.format(Locale.US, "%.1fk", value) else null
        )
    }
}

private fun averagePace(workouts: List<SavedWorkout>): String {
    val dist = workouts.sumOf { it.distanceKm.toDouble() }
    val mins = workouts.sumOf { it.durationMinutes }
    if (dist <= 0.0 || mins <= 0) return "--"
    val paceMin = mins / dist
    val m = paceMin.toInt()
    val s = ((paceMin - m) * 60).roundToInt()
    return "${m}'${s.toString().padStart(2, '0')}\""
}

private fun formatPace(workout: SavedWorkout): String {
    if (workout.distanceKm <= 0f || workout.durationMinutes <= 0) return "--"
    val paceMin = workout.durationMinutes / workout.distanceKm
    val m = paceMin.toInt()
    val s = ((paceMin - m) * 60).roundToInt().coerceIn(0, 59)
    return "${m}'${s.toString().padStart(2, '0')}\""
}

private fun formatPaceClock(workout: SavedWorkout): String {
    if (workout.distanceKm <= 0f || workout.durationMinutes <= 0) return "--"
    val paceMin = workout.durationMinutes / workout.distanceKm
    val m = paceMin.toInt()
    val s = ((paceMin - m) * 60).roundToInt().coerceIn(0, 59)
    return "%d:%02d".format(m, s)
}

private fun formatHours(minutes: Int): String {
    if (minutes <= 0) return "--"
    return "${minutes / 60}h ${minutes % 60}m"
}

private fun formatSessionWhen(workout: SavedWorkout): String {
    val zone = ZoneId.systemDefault()
    val date = workout.startTime.atZone(zone).toLocalDate()
    val time = workout.startTime.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
    val day = when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
    return "$day, $time"
}

private fun hrZone(bpm: Int): String = when {
    bpm < 120 -> "Z2"
    bpm < 140 -> "Z3"
    bpm < 160 -> "Z4"
    else -> "Z5"
}

private fun deriveLoad(minutes: Int, kcal: Int): Int {
    if (minutes <= 0 && kcal <= 0) return 80
    return (minutes * 3 + kcal / 8).coerceIn(80, 520)
}
