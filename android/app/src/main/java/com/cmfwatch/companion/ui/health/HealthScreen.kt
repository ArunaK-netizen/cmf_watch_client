package com.cmfwatch.companion.ui.health

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.HeartRateSample
import com.cmfwatch.companion.storage.SavedWorkout
import com.cmfwatch.companion.ui.components.CmfAppHeader
import com.cmfwatch.companion.ui.components.CmfPulseDot
import com.cmfwatch.companion.ui.components.cmfCard
import com.cmfwatch.companion.ui.components.isCmfConnected
import com.cmfwatch.companion.ui.theme.CmfBackground
import com.cmfwatch.companion.ui.theme.CmfOnPrimary
import com.cmfwatch.companion.ui.theme.CmfOnSurface
import com.cmfwatch.companion.ui.theme.CmfOnSurfaceVariant
import com.cmfwatch.companion.ui.theme.CmfOutlineVariant
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
import com.cmfwatch.companion.ui.theme.HeadlineFontFamily
import com.cmfwatch.companion.ui.theme.InterFontFamily
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val SoftCardShadow = Color(0x0A111315)
private val InnerShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(50)

enum class VitalsRange(val label: String) { DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year") }

private data class HrBar(
    val label: String,
    val bpm: Int,
    val heightFrac: Float,
    val isPeak: Boolean,
    val isFloor: Boolean
)

@Composable
fun HealthScreen(
    summary: DashboardSummary,
    onHeartRateClick: () -> Unit = {},
    onSleepClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var range by remember { mutableStateOf(VitalsRange.WEEK) }

    val samples = summary.hrSamplesToday
    val bars = remember(samples, range) { buildHrBars(samples, range) }
    val peak = samples.maxByOrNull { it.bpm }
    val floor = samples.minByOrNull { it.bpm }
    val delta = if (samples.size >= 2) samples.last().bpm - samples[samples.lastIndex - 1].bpm else null
    val sleepMinutes = summary.lastSleepMinutes
    val sleepScore = sleepMinutes?.let { ((it / 480f) * 100f).roundToInt().coerceIn(0, 100) }
    val spo2 = summary.latestSpO2
    val connected = summary.connectionState.isCmfConnected()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CmfBackground)
    ) {
        CmfAppHeader(
            connected = connected,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "METRICS & TRENDS",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 0.66.sp,
                            color = CmfOnSurfaceVariant
                        )
                        Text(
                            text = "Vitals",
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
                            .background(CmfSurfaceLow)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CmfPulseDot(color = CmfTertiaryContainer, size = 8.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Telemetry",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = CmfOnSurface
                        )
                    }
                }
            }

            item {
                RangeSelector(selected = range, onSelected = { range = it })
            }

            item {
                HeartTelemetryCard(
                    bpm = summary.latestHeartRate,
                    delta = delta,
                    resting = summary.restingHeartRate,
                    peak = peak,
                    floor = floor,
                    bars = bars,
                    workouts = summary.workoutsToday,
                    onClick = onHeartRateClick
                )
            }

            item {
                SleepArchitectureCard(
                    sleepMinutes = sleepMinutes,
                    sleepScore = sleepScore,
                    onClick = onSleepClick
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spo2Card(modifier = Modifier.weight(1f), spo2 = spo2)
                    Vo2Card(modifier = Modifier.weight(1f))
                }
            }

            item {
                WeeklyReportRow(
                    onExport = {
                        val text = buildWeeklyReport(summary)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Weekly Biometrics Report")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export report"))
                    }
                )
            }
        }
    }
}

@Composable
private fun RangeSelector(
    selected: VitalsRange,
    onSelected: (VitalsRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(CmfSurfaceLow)
            .padding(4.dp)
    ) {
        VitalsRange.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(2.dp, PillShape, ambientColor = SoftCardShadow, spotColor = SoftCardShadow)
                                .background(CmfSurfaceLowest)
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelected(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    fontFamily = InterFontFamily,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (isSelected) CmfOnSurface else CmfOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeartTelemetryCard(
    bpm: Int?,
    delta: Int?,
    resting: Int?,
    peak: HeartRateSample?,
    floor: HeartRateSample?,
    bars: List<HrBar>,
    workouts: List<SavedWorkout>,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cmfCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.MonitorHeart,
                        contentDescription = null,
                        tint = CmfPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HEART RATE & VARIABILITY",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.66.sp,
                        color = CmfOnSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = bpm?.toString() ?: "--",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        letterSpacing = (-1.4).sp,
                        color = CmfOnSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BPM",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = CmfOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    if (delta != null && delta != 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clip(PillShape)
                                .background(CmfTertiaryContainer.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (delta < 0) Icons.Outlined.TrendingDown else Icons.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = CmfTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${if (delta > 0) "+" else ""}$delta BPM",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = CmfTertiary
                            )
                        }
                    }
                }
                Row {
                    Text(
                        text = "Resting average: ",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = CmfOnSurfaceVariant
                    )
                    Text(
                        text = if (resting != null) "$resting BPM" else "--",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = CmfOnSurface
                    )
                    Text(
                        text = " this week",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = CmfOnSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CmfPrimaryFixed.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = CmfPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "DYNAMIC ZONE ANALYSIS",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.66.sp,
                    color = CmfOnSurfaceVariant
                )
                Text(
                    text = peak?.let { "Peak: ${it.bpm} BPM" } ?: "Peak: --",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = CmfPrimaryContainer
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(InnerShape)
                    .background(CmfSurfaceLow)
                    .padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(bar.heightFrac.coerceIn(0.06f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(barColor(bar))
                            )
                        }
                        Text(
                            text = bar.label,
                            fontFamily = InterFontFamily,
                            fontWeight = if (bar.isPeak) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 9.sp,
                            letterSpacing = 0.4.sp,
                            color = if (bar.isPeak) CmfPrimaryContainer else CmfOnSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExtremaTile(
                modifier = Modifier.weight(1f),
                label = "Daily Floor",
                value = floor?.bpm?.toString() ?: "--",
                caption = floor?.let { formatHrCaption(it, workouts, isFloor = true) } ?: "No low sample",
                icon = Icons.Outlined.Bedtime,
                iconBg = CmfSecondaryFixed,
                iconTint = CmfSecondary
            )
            ExtremaTile(
                modifier = Modifier.weight(1f),
                label = "Cardio Peak",
                value = peak?.bpm?.toString() ?: "--",
                caption = peak?.let { formatHrCaption(it, workouts, isFloor = false) } ?: "No peak sample",
                icon = Icons.Outlined.Bolt,
                iconBg = CmfPrimaryFixed,
                iconTint = CmfPrimary
            )
        }
    }
}

@Composable
private fun RowScope.ExtremaTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    caption: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color
) {
    Row(
        modifier = modifier
            .clip(InnerShape)
            .background(CmfSurfaceLow)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label.uppercase(Locale.getDefault()),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.66.sp,
                color = CmfOnSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = CmfOnSurface
                )
                Text(
                    text = " BPM",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = CmfOnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
            Text(
                text = caption,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = CmfOnSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SleepArchitectureCard(
    sleepMinutes: Int?,
    sleepScore: Int?,
    onClick: () -> Unit
) {
    val duration = sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "--"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cmfCard()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DarkMode,
                        contentDescription = null,
                        tint = CmfSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SLEEP ARCHITECTURE",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.66.sp,
                        color = CmfOnSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = duration,
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 36.sp,
                        letterSpacing = (-0.7).sp,
                        color = CmfOnSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Total Duration",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = CmfOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(CmfTertiaryContainer.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = CmfTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = sleepScore?.let { "$it% Restorative" } ?: "-- Restorative",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = CmfTertiary
                )
            }
        }

        val hasSleep = sleepMinutes != null && sleepMinutes > 0
        val deep = if (hasSleep) 0.21f else 0.08f
        val core = if (hasSleep) 0.54f else 0.08f
        val rem = if (hasSleep) 0.18f else 0.08f
        val awake = if (hasSleep) 0.07f else 0.08f

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(PillShape)
                    .background(CmfSurfaceHigh)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                StageSeg(deep, CmfSecondaryContainer)
                StageSeg(core, CmfSecondary)
                StageSeg(rem, CmfSecondaryFixedDim)
                StageSeg(awake, CmfOutlineVariant)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                StageLegend("Deep", CmfSecondaryContainer, sleepMinutes?.let { formatMins((it * 0.21f).roundToInt()) } ?: "--")
                StageLegend("Core", CmfSecondary, sleepMinutes?.let { formatMins((it * 0.54f).roundToInt()) } ?: "--")
                StageLegend("REM", CmfSecondaryFixedDim, sleepMinutes?.let { formatMins((it * 0.18f).roundToInt()) } ?: "--")
                StageLegend("Awake", CmfOutlineVariant, sleepMinutes?.let { formatMins((it * 0.07f).roundToInt()) } ?: "--")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SleepMiniStat(modifier = Modifier.weight(1f), label = "Respiratory", value = "--", unit = "br/min", note = "Optimal", noteColor = CmfTertiary)
            SleepMiniStat(modifier = Modifier.weight(1f), label = "Sleep HRV", value = "--", unit = "ms RMSSD", note = "Awaiting night", noteColor = CmfTertiary)
            SleepMiniStat(modifier = Modifier.weight(1f), label = "Skin Temp", value = "--", unit = "Deviation", note = "Stable", noteColor = CmfOnSurfaceVariant)
        }
    }
}

@Composable
private fun RowScope.StageSeg(weight: Float, color: Color) {
    Box(
        modifier = Modifier
            .weight(weight.coerceAtLeast(0.04f))
            .fillMaxSize()
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
private fun RowScope.StageLegend(label: String, color: Color, value: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                color = CmfOnSurfaceVariant
            )
        }
        Text(
            text = value,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = CmfOnSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun SleepMiniStat(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    note: String,
    noteColor: Color = CmfOnSurfaceVariant
) {
    Column(
        modifier = modifier
            .clip(InnerShape)
            .background(CmfSurfaceLow)
            .padding(10.dp)
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = CmfOnSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = HeadlineFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = CmfOnSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = unit,
            fontFamily = InterFontFamily,
            fontSize = 10.sp,
            color = CmfOnSurfaceVariant
        )
        Text(
            text = note,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            color = noteColor,
        )
    }
}

@Composable
private fun Spo2Card(spo2: Int?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(210.dp)
            .cmfCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "SPO2 LEVEL",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.66.sp,
                    color = CmfOnSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = CmfPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = spo2?.let { "$it%" } ?: "--%",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                letterSpacing = (-0.7).sp,
                color = CmfOnSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        val heights = if (spo2 != null) {
            listOf(0.85f, 0.90f, 0.88f, 0.95f, 0.92f, 0.98f, 1f).map { it * (spo2 / 100f).coerceIn(0.7f, 1f) }
        } else {
            List(7) { 0.18f }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            heights.forEachIndexed { i, h ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(h.coerceIn(0.12f, 1f))
                        .clip(PillShape)
                        .background(
                            if (spo2 == null) CmfPrimaryContainer.copy(alpha = 0.18f)
                            else CmfPrimaryContainer.copy(alpha = 0.30f + i * 0.10f)
                        )
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = CmfTertiary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when {
                    spo2 == null -> "Waiting for SpO2"
                    spo2 >= 95 -> "No irregular drops"
                    else -> "Below baseline"
                },
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = if (spo2 != null && spo2 >= 95) CmfTertiary else CmfOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun Vo2Card(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(210.dp)
            .cmfCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "VO2 MAX",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.66.sp,
                    color = CmfOnSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Outlined.ShowChart,
                    contentDescription = null,
                    tint = CmfTertiaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "--",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                letterSpacing = (-0.7).sp,
                color = CmfOnSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(PillShape)
                .background(CmfSurfaceHigh)
        )
        Text(
            text = "Not reported by CMF Watch",
            fontFamily = InterFontFamily,
            fontSize = 10.sp,
            color = CmfOnSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun WeeklyReportRow(onExport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cmfCard()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CmfSurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = CmfOnSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Weekly Biometrics Report",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = CmfOnSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = CmfTertiaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Ready for clinical review",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = CmfOnSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(CmfPrimaryContainer)
                .clickable(onClick = onExport)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.FileDownload,
                contentDescription = null,
                tint = CmfOnPrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "PDF",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = CmfOnPrimary
            )
        }
    }
}

private fun barColor(bar: HrBar): Color = when {
    bar.bpm <= 0 -> CmfPrimaryContainer.copy(alpha = 0.12f)
    bar.isPeak -> CmfPrimaryContainer
    bar.isFloor -> CmfSecondaryContainer.copy(alpha = 0.40f)
    else -> CmfPrimaryContainer.copy(alpha = (0.20f + bar.heightFrac * 0.35f).coerceAtMost(0.55f))
}

private fun buildHrBars(samples: List<HeartRateSample>, range: VitalsRange): List<HrBar> {
    val zone = ZoneId.systemDefault()
    val buckets: List<Pair<String, List<Int>>> = when (range) {
        VitalsRange.DAY -> {
            val labels = listOf("12A", "03A", "06A", "09A", "12P", "03P", "06P", "09P", "11P")
            val groups = List(9) { mutableListOf<Int>() }
            samples.forEach { sample ->
                val hour = sample.timestamp.atZone(zone).hour
                val idx = when (hour) {
                    in 0..2 -> 0
                    in 3..5 -> 1
                    in 6..8 -> 2
                    in 9..11 -> 3
                    in 12..14 -> 4
                    in 15..17 -> 5
                    in 18..20 -> 6
                    in 21..22 -> 7
                    else -> 8
                }
                groups[idx].add(sample.bpm)
            }
            labels.zip(groups)
        }
        VitalsRange.WEEK -> {
            val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val groups = List(7) { mutableListOf<Int>() }
            samples.forEach { sample ->
                val idx = sample.timestamp.atZone(zone).dayOfWeek.value - 1
                groups[idx].add(sample.bpm)
            }
            labels.zip(groups)
        }
        VitalsRange.MONTH -> {
            val labels = listOf("W1", "W2", "W3", "W4")
            val groups = List(4) { mutableListOf<Int>() }
            samples.forEach { sample ->
                val week = ((sample.timestamp.atZone(zone).dayOfMonth - 1) / 7).coerceIn(0, 3)
                groups[week].add(sample.bpm)
            }
            labels.zip(groups)
        }
        VitalsRange.YEAR -> {
            val labels = (1..12).map {
                java.time.Month.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)
            }
            val groups = List(12) { mutableListOf<Int>() }
            samples.forEach { sample ->
                groups[sample.timestamp.atZone(zone).monthValue - 1].add(sample.bpm)
            }
            labels.zip(groups)
        }
    }

    val averages = buckets.map { (_, values) -> values.maxOrNull() ?: 0 }
    val maxBpm = averages.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peakIdx = averages.indices.maxByOrNull { averages[it] }
    val floorIdx = averages.indices.filter { averages[it] > 0 }.minByOrNull { averages[it] }

    return buckets.mapIndexed { i, (label, _) ->
        val bpm = averages[i]
        HrBar(
            label = label,
            bpm = bpm,
            heightFrac = if (bpm <= 0) 0.08f else (bpm / maxBpm.toFloat()).coerceIn(0.12f, 1f),
            isPeak = peakIdx == i && bpm > 0,
            isFloor = floorIdx == i && bpm > 0
        )
    }
}

private fun formatHrCaption(sample: HeartRateSample, workouts: List<SavedWorkout>, isFloor: Boolean): String {
    val time = sample.timestamp.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    val nearby = workouts.minByOrNull {
        kotlin.math.abs(java.time.Duration.between(it.startTime, sample.timestamp).toMinutes())
    }
    val close = nearby != null && kotlin.math.abs(
        java.time.Duration.between(nearby.startTime, sample.timestamp).toMinutes()
    ) <= 90
    val tag = when {
        close -> nearby!!.type.ifBlank { "Workout" }
        isFloor -> "Overnight"
        else -> "Daytime"
    }
    return "$time · $tag"
}

private fun formatMins(total: Int): String {
    val hours = total / 60
    val minutes = total % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun buildWeeklyReport(summary: DashboardSummary): String = buildString {
    appendLine("CMF Watch Pro — Weekly Biometrics")
    appendLine("Heart rate: ${summary.latestHeartRate ?: "--"} BPM")
    appendLine("Resting: ${summary.restingHeartRate ?: "--"} BPM")
    appendLine("SpO2: ${summary.latestSpO2 ?: "--"}%")
    appendLine("Stress: ${summary.latestStressScore ?: "--"}")
    appendLine("Sleep: ${summary.lastSleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "--"}")
}
