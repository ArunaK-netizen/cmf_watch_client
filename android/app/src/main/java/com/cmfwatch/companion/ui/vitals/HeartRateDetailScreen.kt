package com.cmfwatch.companion.ui.vitals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.HeartRateSample
import com.cmfwatch.companion.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class HrTimeRange(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

@Composable
fun HeartRateDetailScreen(
    allSamples: List<HeartRateSample>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(HrTimeRange.DAY) }

    // Filter samples based on time range
    val filteredSamples = remember(allSamples, selectedRange) {
        when (selectedRange) {
            HrTimeRange.DAY -> allSamples
            HrTimeRange.WEEK -> allSamples.takeLast(70)
            HrTimeRange.MONTH -> allSamples.takeLast(300)
            HrTimeRange.YEAR -> allSamples
        }
    }

    val latestBpm = filteredSamples.lastOrNull()?.bpm
    val bpms = filteredSamples.map { it.bpm }
    val avgBpm = if (bpms.isNotEmpty()) bpms.average().toInt() else null
    val highestBpm = bpms.maxOrNull()
    val lowestBpm = bpms.minOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
    ) {
        // 1. Top Bar Navigation (< Today, title, ...)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onBackClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Today",
                        fontFamily = AppFontFamily,
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceWhite)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More options",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Heart Rate",
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Time Range Segmented Control (Day, Week, Month, Year)
        item {
            TimeRangeSegmentedControl(
                selectedRange = selectedRange,
                onRangeSelected = { selectedRange = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Main Chart & Heart Rate Display Card
        item {
            MainHeartRateChartCard(
                bpm = latestBpm,
                avgBpm = avgBpm,
                highestBpm = highestBpm,
                lowestBpm = lowestBpm,
                samples = filteredSamples
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. Heart Rate Zones Card
        item {
            HeartRateZonesCard(samples = filteredSamples)

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5. Insights Card
        item {
            InsightsCard(avgBpm = avgBpm)

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 6. About Heart Rate Card
        item {
            AboutHeartRateCard()
        }
    }
}

@Composable
fun TimeRangeSegmentedControl(
    selectedRange: HrTimeRange,
    onRangeSelected: (HrTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE5E5EA))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HrTimeRange.values().forEach { range ->
                val selected = range == selectedRange
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) SurfaceWhite else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onRangeSelected(range) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        fontFamily = AppFontFamily,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (selected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MainHeartRateChartCard(
    bpm: Int?,
    avgBpm: Int?,
    highestBpm: Int?,
    lowestBpm: Int?,
    samples: List<HeartRateSample>,
    modifier: Modifier = Modifier
) {
    var selectedSampleIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .padding(16.dp)
    ) {
        Column {
            // Header Row (Icon, Title, Value, Normal Range Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(HeartRateBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = HeartRateRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Current Heart Rate",
                            fontFamily = AppFontFamily,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = bpm?.let { "$it" } ?: "--",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "bpm",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = if (bpm != null) "Resting" else "Unavailable",
                            fontFamily = AppFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Green Normal Range Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE8F8EE))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Normal range",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "60 – 100 bpm",
                                fontFamily = AppFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Bezier Chart Canvas with Gridlines & Pointer Tooltip Inspection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (samples.isNotEmpty()) {
                    val activeIndex = selectedSampleIndex
                    val activeSample = activeIndex?.let { samples.getOrNull(it) }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(samples) {
                                detectTapGestures { offset ->
                                    val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
                                    val idx = (offset.x / stepX).toInt().coerceIn(0, samples.size - 1)
                                    selectedSampleIndex = idx
                                }
                            }
                            .pointerInput(samples) {
                                detectDragGestures { change, _ ->
                                    val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
                                    val idx = (change.position.x / stepX).toInt().coerceIn(0, samples.size - 1)
                                    selectedSampleIndex = idx
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val chartPaddingRight = 30.dp.toPx()
                        val plotWidth = width - chartPaddingRight

                        val gridYValues = listOf(180f, 120f, 60f, 0f)
                        val minY = 0f
                        val maxY = 180f

                        // Draw Gridlines (Dotted)
                        gridYValues.forEach { gridValue ->
                            val y = height * (1f - gridValue / maxY)
                            drawLine(
                                color = Color(0xFFE5E5EA),
                                start = Offset(0f, y),
                                end = Offset(plotWidth, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        // Plot Bezier Line Path
                        val bpms = samples.map { it.bpm.toFloat().coerceIn(minY, maxY) }
                        val stepX = if (bpms.size > 1) plotWidth / (bpms.size - 1) else plotWidth

                        val path = Path()
                        if (bpms.size == 1) {
                            val y = height * (1f - bpms[0] / maxY)
                            path.moveTo(0f, y)
                            path.lineTo(plotWidth, y)
                        } else {
                            path.moveTo(0f, height * (1f - bpms[0] / maxY))
                            for (i in 1 until bpms.size) {
                                val currentX = i * stepX
                                val currentY = height * (1f - bpms[i] / maxY)
                                val prevX = (i - 1) * stepX
                                val prevY = height * (1f - bpms[i - 1] / maxY)
                                val controlX = (prevX + currentX) / 2f
                                path.cubicTo(controlX, prevY, controlX, currentY, currentX, currentY)
                            }
                        }

                        // Draw Gradient Area Fill
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(plotWidth, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(HeartRateRed.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )

                        // Draw Stroke Path
                        drawPath(
                            path = path,
                            color = HeartRateRed,
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        // Draw Selected Inspection Point Highlight Line & Marker
                        if (activeIndex != null && activeIndex in samples.indices) {
                            val highlightX = activeIndex * stepX
                            val highlightY = height * (1f - bpms[activeIndex] / maxY)

                            // Vertical guideline
                            drawLine(
                                color = HeartRateRed.copy(alpha = 0.5f),
                                start = Offset(highlightX, 0f),
                                end = Offset(highlightX, height),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                            )

                            // Highlight circle dot
                            drawCircle(
                                color = SurfaceWhite,
                                radius = 7.dp.toPx(),
                                center = Offset(highlightX, highlightY)
                            )
                            drawCircle(
                                color = HeartRateRed,
                                radius = 5.dp.toPx(),
                                center = Offset(highlightX, highlightY)
                            )
                        }
                    }

                    // Y-Axis Labels Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("180", fontSize = 9.sp, color = TextMuted)
                        Text("120", fontSize = 9.sp, color = TextMuted)
                        Text("60", fontSize = 9.sp, color = TextMuted)
                        Text("0", fontSize = 9.sp, color = TextMuted)
                    }

                    // Floating Tooltip Pill for Active Selection
                    activeSample?.let { sample ->
                        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                        val timeStr = sample.timestamp.atZone(ZoneId.systemDefault()).format(timeFormatter)

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceWhite)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${sample.bpm} bpm",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = timeStr,
                                    fontFamily = AppFontFamily,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No heart rate data recorded for this period",
                            fontFamily = AppFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Time Labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("12 AM", fontSize = 10.sp, color = TextMuted)
                Text("6 AM", fontSize = 10.sp, color = TextMuted)
                Text("12 PM", fontSize = 10.sp, color = TextMuted)
                Text("6 PM", fontSize = 10.sp, color = TextMuted)
                Text("12 AM", fontSize = 10.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Metrics Row (Resting, Highest, Lowest)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn(
                    value = avgBpm?.let { "$it" } ?: "--",
                    unit = "bpm",
                    label = "Resting (avg)"
                )
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color(0xFFE5E5EA))
                )
                MetricColumn(
                    value = highestBpm?.let { "$it" } ?: "--",
                    unit = "bpm",
                    label = "Highest"
                )
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color(0xFFE5E5EA))
                )
                MetricColumn(
                    value = lowestBpm?.let { "$it" } ?: "--",
                    unit = "bpm",
                    label = "Lowest"
                )
            }
        }
    }
}

@Composable
fun MetricColumn(
    value: String,
    unit: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Text(
            text = label,
            fontFamily = AppFontFamily,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun HeartRateZonesCard(
    samples: List<HeartRateSample>,
    modifier: Modifier = Modifier
) {
    val total = samples.size.coerceAtLeast(1)
    val peakCount = samples.count { it.bpm >= 150 }
    val cardioCount = samples.count { it.bpm in 130..149 }
    val fatBurnCount = samples.count { it.bpm in 110..129 }
    val lightCount = samples.count { it.bpm in 90..109 }
    val restingCount = samples.count { it.bpm < 90 }

    val hasData = samples.isNotEmpty()

    val peakPct = if (hasData) (peakCount * 100) / total else 0
    val cardioPct = if (hasData) (cardioCount * 100) / total else 0
    val fatBurnPct = if (hasData) (fatBurnCount * 100) / total else 0
    val lightPct = if (hasData) (lightCount * 100) / total else 0
    val restingPct = if (hasData) (restingCount * 100) / total else 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Heart Rate Zones",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ZoneRow(label = "Peak", color = Color(0xFFFF453A), pct = peakPct, count = peakCount, hasData = hasData)
            Spacer(modifier = Modifier.height(12.dp))
            ZoneRow(label = "Cardio", color = Color(0xFFFF9500), pct = cardioPct, count = cardioCount, hasData = hasData)
            Spacer(modifier = Modifier.height(12.dp))
            ZoneRow(label = "Fat Burn", color = Color(0xFFFFCC00), pct = fatBurnPct, count = fatBurnCount, hasData = hasData)
            Spacer(modifier = Modifier.height(12.dp))
            ZoneRow(label = "Light", color = Color(0xFF007AFF), pct = lightPct, count = lightCount, hasData = hasData)
            Spacer(modifier = Modifier.height(12.dp))
            ZoneRow(label = "Resting", color = Color(0xFF8E8E93), pct = restingPct, count = restingCount, hasData = hasData)
        }
    }
}

@Composable
fun ZoneRow(
    label: String,
    color: Color,
    pct: Int,
    count: Int,
    hasData: Boolean
) {
    // 5 minutes per sample interval estimation
    val minutesTotal = count * 5
    val hours = minutesTotal / 60
    val mins = minutesTotal % 60
    val durationText = when {
        !hasData -> "0 min"
        hours > 0 -> "${hours} h ${mins} min"
        else -> "${mins} min"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontFamily = AppFontFamily,
                fontSize = 13.sp,
                color = TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F2F7))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (hasData) (pct / 100f).coerceIn(0.02f, 1f) else 0f)
                    .clip(CircleShape)
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$pct%",
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = TextPrimary,
            modifier = Modifier.width(36.dp)
        )

        Text(
            text = durationText,
            fontFamily = AppFontFamily,
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
fun InsightsCard(
    avgBpm: Int?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HeartRateBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = HeartRateRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Insights",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (avgBpm != null) {
                            "Your average heart rate today ($avgBpm bpm) is within your normal resting range."
                        } else {
                            "No heart rate recorded yet. Wear your watch to start tracking real-time insights."
                        },
                        fontFamily = AppFontFamily,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AboutHeartRateCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "About Heart Rate",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Heart rate is the number of times your heart beats per minute (bpm). It can indicate your fitness level, stress, and overall health.",
                    fontFamily = AppFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
