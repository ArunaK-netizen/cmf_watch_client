package com.cmfwatch.companion.ui.device

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.R
import com.cmfwatch.companion.ble.CmfBleManager
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// COLOR PALETTE (EXACT CMF EXPRESSIVE LIGHT THEME)
// ==========================================

private val DeviceBg = Color(0xFFF9F9FC)
private val DeviceCardBg = Color(0xFFFFFFFF)
private val TroughBg = Color(0xFFF3F3F6)
private val CmfOrange = Color(0xFFFF5722)
private val CmfOrangeContainer = Color(0xFFFFDBD1)
private val CmfOrangeDark = Color(0xFFB02F00)
private val CmfGreen = Color(0xFF008733)
private val CmfGreenBg = Color(0xFFE8FDF0)
private val CmfPurple = Color(0xFF4C4ACA)
private val CmfPurpleBg = Color(0xFFE2DFFF)
private val TextOnSurface = Color(0xFF1A1C1E)
private val TextVariant = Color(0xFF5B4039)
private val TextMutedGray = Color(0xFF70777D)
private val DeviceBorder = Color(0xFFE2E2E5)

// ==========================================
// MOTION EXTENSION
// ==========================================

@Composable
private fun Modifier.bouncyClickable(
    scaleDown: Float = 0.94f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncyClick"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
private fun Modifier.pulse(from: Float = 0.9f, to: Float = 1.25f, durationMs: Int = 1200): Modifier {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = from,
        targetValue = to,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ==========================================
// MAIN DEVICE SCREEN COMPOSABLE
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    summary: DashboardSummary,
    onStartScan: () -> Unit = {},
    onConnectDevice: (String) -> Unit = {},
    onDisconnectDevice: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isConnected = summary.connectionState == DeviceConnectionState.CONNECTED_PAIRED ||
            summary.connectionState == DeviceConnectionState.CONNECTED

    var isFindingWatch by remember { mutableStateOf(false) }
    var selectedHaptic by remember { mutableStateOf("Firm") }

    // Sensor Toggles
    var heartRateToggle by remember { mutableStateOf(true) }
    var spO2Toggle by remember { mutableStateOf(true) }
    var sleepApneaToggle by remember { mutableStateOf(true) }
    var stressToggle by remember { mutableStateOf(true) }
    var aodToggle by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeviceBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header Bar (CMF PRO + Connected Status + Avatar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CMF",
                            fontFamily = HeadlineFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextOnSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFEEEFEF))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = TextVariant
                            )
                        }
                    }

                    // Connected Badge
                    Surface(
                        shape = CircleShape,
                        color = TroughBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) CmfGreen else TextMutedGray)
                                    .pulse()
                            )
                            Text(
                                text = if (isConnected) "Connected · ${summary.deviceBatteryLevel ?: 84}%" else "Disconnected",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TextVariant
                            )
                        }
                    }
                }

                // Profile Avatar Circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CmfOrangeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = "Device Profile",
                        tint = CmfOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Primary Device Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = DeviceCardBg,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DeviceBorder, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar (Primary Device + Active Sync)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(TroughBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "PRIMARY DEVICE",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = TextVariant,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CmfGreen)
                                    .pulse()
                            )
                            Text(
                                text = "Active Sync",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = CmfGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Center Watch Showcase Image with Orange Radial Glow
                    Box(
                        modifier = Modifier
                            .size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Soft Radial Ambient Glow
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(CmfOrangeContainer.copy(alpha = 0.6f), Color.Transparent)
                                    )
                                )
                        )

                        // Render Watch Image from res/drawable or Vector
                        Image(
                            painter = painterResource(id = R.drawable.cmf_watch_pro2),
                            contentDescription = "CMF Watch Pro 2",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(200.dp)
                                .shadow(8.dp, CircleShape, clip = false)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title & Identity
                    Text(
                        text = "CMF Watch Pro 2",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextOnSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dark Gray Case · Liquid Orange Silicone",
                        fontFamily = AppFontFamily,
                        fontSize = 13.sp,
                        color = TextVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hardware Metrics Trough
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(TroughBg)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HardwareMetricColumn(
                            icon = Icons.Default.BluetoothConnected,
                            iconTint = CmfOrange,
                            label = "LINK",
                            value = "BLE 5.3",
                            subtext = "Optimal"
                        )
                        HardwareMetricColumn(
                            icon = Icons.Default.BatteryFull,
                            iconTint = CmfGreen,
                            label = "BATTERY",
                            value = summary.deviceBatteryLevel?.let { "$it%" } ?: "84%",
                            subtext = "~8d left"
                        )
                        HardwareMetricColumn(
                            icon = Icons.Default.Verified,
                            iconTint = CmfPurple,
                            label = "FIRMWARE",
                            value = "v2.1.04",
                            subtext = "Latest"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Find My Watch Action Button
                    Surface(
                        shape = CircleShape,
                        color = if (isFindingWatch) CmfOrangeContainer else TroughBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .bouncyClickable {
                                isFindingWatch = true
                                CmfBleManager.getInstance(context).findMyWatch()
                                Toast
                                    .makeText(
                                        context,
                                        "Pinging CMF Watch Pro 2... Audio alert sent!",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                                scope.launch {
                                    delay(3200)
                                    isFindingWatch = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Find Watch",
                                tint = CmfOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFindingWatch) "Pinging Watch..." else "Find My Watch",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (isFindingWatch) CmfOrangeDark else TextOnSurface
                            )
                        }
                    }

                    if (isFindingWatch) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Watch is vibrating with audio ping...",
                            fontFamily = AppFontFamily,
                            fontSize = 12.sp,
                            color = CmfGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 3. Watch Face Gallery Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Current Face",
                            fontFamily = HeadlineFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextOnSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CmfOrange)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { }
                    ) {
                        Text(
                            text = "Browse 100+",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = CmfOrange
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Browse",
                            tint = CmfOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Horizontal Carousel of Watch Faces
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        WatchFaceCard(
                            name = "CMF Digital Ring",
                            status = "Installed · Active",
                            isActive = true,
                            faceType = WatchFaceType.DIGITAL_RING
                        )
                    }
                    item {
                        WatchFaceCard(
                            name = "Studio Bauhaus",
                            status = "Stored on Watch",
                            isActive = false,
                            faceType = WatchFaceType.CHRONO
                        )
                    }
                    item {
                        WatchFaceCard(
                            name = "Vitals Trio",
                            status = "Cloud Preset",
                            isActive = false,
                            faceType = WatchFaceType.TRIPLE_RING
                        )
                    }
                }

                // Customize Button
                Surface(
                    shape = CircleShape,
                    color = CmfOrangeContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .bouncyClickable { }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Customize",
                            tint = CmfOrangeDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Customize Complications & Color",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = CmfOrangeDark
                        )
                    }
                }
            }
        }

        // 4. Health Monitoring Controls & Sensors
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Health & Telemetry",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextOnSurface
                    )
                    Text(
                        text = "Sensor Hub v3",
                        fontFamily = AppFontFamily,
                        fontSize = 12.sp,
                        color = TextMutedGray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DeviceCardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DeviceBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SensorToggleRow(
                            icon = Icons.Default.Favorite,
                            iconTint = HeartRateRed,
                            iconBg = HeartRateBg,
                            title = "Continuous Heart Rate",
                            subtitle = "Every 5 min · Optical sensor high-precision",
                            checked = heartRateToggle,
                            onCheckedChange = { heartRateToggle = it }
                        )

                        SensorToggleRow(
                            icon = Icons.Default.WaterDrop,
                            iconTint = CmfPurple,
                            iconBg = CmfPurpleBg,
                            title = "All-Day Blood Oxygen",
                            subtitle = "Automatic low oxygen alert under 90%",
                            checked = spO2Toggle,
                            onCheckedChange = { spO2Toggle = it }
                        )

                        SensorToggleRow(
                            icon = Icons.Default.NightlightRound,
                            iconTint = TextOnSurface,
                            iconBg = TroughBg,
                            title = "Sleep Apnea & Breathing",
                            subtitle = "Nightly respiratory rate disturbances",
                            checked = sleepApneaToggle,
                            onCheckedChange = { sleepApneaToggle = it }
                        )

                        SensorToggleRow(
                            icon = Icons.Default.Psychology,
                            iconTint = CmfOrange,
                            iconBg = CmfOrangeContainer,
                            title = "High Stress Reminders",
                            subtitle = "Guided haptic breath session trigger",
                            checked = stressToggle,
                            onCheckedChange = { stressToggle = it }
                        )
                    }
                }
            }
        }

        // 5. Preferences & Feedback Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Preferences & Feedback",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextOnSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DeviceCardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DeviceBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // App Notifications Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(TroughBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Notifications",
                                        tint = TextOnSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "App Notifications",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextOnSurface
                                    )
                                    Text(
                                        text = "Phone, WhatsApp, Calendar, Slack",
                                        fontFamily = AppFontFamily,
                                        fontSize = 12.sp,
                                        color = TextMutedGray
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = TextMutedGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Haptic Engine & Segmented Picker
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = "Haptics",
                                        tint = TextVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Haptic Engine & Digital Crown",
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextOnSurface
                                    )
                                }
                                Text(
                                    text = selectedHaptic.uppercase(),
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = CmfOrange,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // 3-Segment Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .background(TroughBg)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Subtle", "Medium", "Firm").forEach { level ->
                                    val isSelected = selectedHaptic == level
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .background(if (isSelected) DeviceCardBg else Color.Transparent)
                                            .clickable { selectedHaptic = level }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = level,
                                            fontFamily = AppFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (isSelected) TextOnSurface else TextMutedGray
                                        )
                                    }
                                }
                            }
                        }

                        // Always-On Display (AOD)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Always-On Display (AOD)",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextOnSurface
                                )
                                Text(
                                    text = "Scheduled · 07:00 to 23:00 daily",
                                    fontFamily = AppFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMutedGray
                                )
                            }

                            Switch(
                                checked = aodToggle,
                                onCheckedChange = { aodToggle = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CmfOrange
                                )
                            )
                        }
                    }
                }
            }
        }

        // 6. Storage & Device Health Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Storage & Device Health",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextOnSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DeviceCardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DeviceBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "On-Board Storage",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextOnSurface
                            )
                            Text(
                                text = "1.2 GB / 4.0 GB Used",
                                fontFamily = AppFontFamily,
                                fontSize = 12.sp,
                                color = TextMutedGray
                            )
                        }

                        // Segmented Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(TroughBg)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.20f)
                                        .background(CmfOrange)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.10f)
                                        .background(CmfPurple)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.05f)
                                        .background(CmfGreen)
                                )
                                Spacer(modifier = Modifier.weight(0.65f))
                            }
                        }

                        // Legend Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(color = CmfOrange, label = "Faces")
                            LegendItem(color = CmfPurple, label = "Music")
                            LegendItem(color = CmfGreen, label = "Logs")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Reboot Action
                        Surface(
                            shape = CircleShape,
                            color = TroughBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .bouncyClickable { }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reboot CMF Watch Pro 2",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = TextOnSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reboot",
                                    tint = TextMutedGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Unpair Action
                        Surface(
                            shape = CircleShape,
                            color = HeartRateBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .bouncyClickable {
                                    if (isConnected) onDisconnectDevice() else onStartScan()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isConnected) "Unpair & Factory Reset" else "Pair Watch",
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = HeartRateRed
                                )
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = "Unpair",
                                    tint = HeartRateRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUBCOMPONENTS
// ==========================================

@Composable
private fun HardwareMetricColumn(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subtext: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = TextVariant,
                letterSpacing = 0.5.sp
            )
        }
        Text(
            text = value,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextOnSurface
        )
        Text(
            text = subtext,
            fontFamily = AppFontFamily,
            fontSize = 11.sp,
            color = TextMutedGray
        )
    }
}

private enum class WatchFaceType {
    DIGITAL_RING,
    CHRONO,
    TRIPLE_RING
}

@Composable
private fun WatchFaceCard(
    name: String,
    status: String,
    isActive: Boolean,
    faceType: WatchFaceType
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DeviceCardBg,
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(170.dp)
            .border(
                1.dp,
                if (isActive) CmfOrange else DeviceBorder,
                RoundedCornerShape(24.dp)
            )
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(CmfOrange)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular Watch Face Preview
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14151D)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(104.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2

                        when (faceType) {
                            WatchFaceType.DIGITAL_RING -> {
                                drawCircle(
                                    color = CmfOrange,
                                    radius = radius - 3.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                            WatchFaceType.CHRONO -> {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.2f),
                                    radius = radius - 4.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawLine(
                                    color = CmfOrange,
                                    start = center,
                                    end = Offset(center.x + 25.dp.toPx(), center.y - 25.dp.toPx()),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            WatchFaceType.TRIPLE_RING -> {
                                drawCircle(
                                    color = CmfOrange,
                                    radius = radius - 4.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                drawCircle(
                                    color = CmfGreen,
                                    radius = radius - 10.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MON 26",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = CmfOrange
                        )
                        Text(
                            text = "10:09",
                            fontFamily = HeadlineFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "8,452",
                            fontFamily = AppFontFamily,
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Text(
                    text = name,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextOnSurface
                )
                Text(
                    text = status,
                    fontFamily = AppFontFamily,
                    fontSize = 11.sp,
                    color = if (isActive) CmfOrange else TextMutedGray
                )
            }
        }
    }
}

@Composable
private fun SensorToggleRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextOnSurface
                )
                Text(
                    text = subtitle,
                    fontFamily = AppFontFamily,
                    fontSize = 12.sp,
                    color = TextMutedGray
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CmfOrange
            )
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontFamily = AppFontFamily,
            fontSize = 12.sp,
            color = TextMutedGray
        )
    }
}
