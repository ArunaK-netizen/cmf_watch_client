package com.cmfwatch.companion.ui.pairing

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.R
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.domain.models.DiscoveredDevice
import com.cmfwatch.companion.ui.theme.*
import kotlinx.coroutines.delay

// ==========================================
// COLOR PALETTE (CMF EXPRESSIVE LIGHT THEME)
// ==========================================
private val PairingBg = Color(0xFFF9F9FC)
private val CmfOrange = Color(0xFFFF5722)
private val CmfOrangeContainer = Color(0xFFFFDBD1)
private val CmfOrangeDark = Color(0xFFB02F00)
private val CmfGreen = Color(0xFF008733)
private val CmfPurple = Color(0xFF4C4ACA)
private val TextOnSurface = Color(0xFF1A1C1E)
private val TextVariant = Color(0xFF5B4039)
private val TextMutedGray = Color(0xFF70777D)
private val CardBg = Color(0xFFFFFFFF)
private val TroughBg = Color(0xFFF3F3F6)
private val CardBorder = Color(0xFFE2E2E5)

private val PillShape = RoundedCornerShape(50)
private val CardShape = RoundedCornerShape(28.dp)

// ==========================================
// BOUNCY CLICK MODIFIER
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

// ==========================================
// MAIN WATCH PAIRING SCREEN
// ==========================================
@Composable
fun WatchPairingScreen(
    connectionState: DeviceConnectionState,
    discoveredDevices: List<DiscoveredDevice>,
    onStartScan: () -> Unit,
    onConnectDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isScanning = connectionState == DeviceConnectionState.SCANNING
    val isConnecting = connectionState == DeviceConnectionState.CONNECTING ||
            connectionState == DeviceConnectionState.BONDING ||
            connectionState == DeviceConnectionState.CONNECTED ||
            connectionState == DeviceConnectionState.SUBSCRIBING ||
            connectionState == DeviceConnectionState.AUTHENTICATING
    val isConnected = connectionState == DeviceConnectionState.CONNECTED_PAIRED
    val hasDevices = discoveredDevices.isNotEmpty() ||
            connectionState == DeviceConnectionState.DEVICES_FOUND

    // Success animation state
    var showSuccess by remember { mutableStateOf(false) }
    LaunchedEffect(isConnected) {
        if (isConnected) {
            showSuccess = true
            delay(2000) // Show success for 2 seconds before navigation
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PairingBg)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 1. Header
            item {
                PairingHeader()
            }

            // 2. Watch Hero Visual
            item {
                Spacer(modifier = Modifier.height(8.dp))
                WatchHeroSection(
                    isScanning = isScanning,
                    isConnecting = isConnecting,
                    isConnected = isConnected
                )
            }

            // 3. Title & Subtitle
            item {
                Spacer(modifier = Modifier.height(24.dp))
                TitleSection(
                    isScanning = isScanning,
                    isConnecting = isConnecting,
                    isConnected = isConnected,
                    connectionState = connectionState
                )
            }

            // 4. Action Button or Device List
            item {
                Spacer(modifier = Modifier.height(24.dp))
                AnimatedContent(
                    targetState = when {
                        isConnected || showSuccess -> PairingPhase.SUCCESS
                        isConnecting -> PairingPhase.CONNECTING
                        hasDevices -> PairingPhase.DEVICES_FOUND
                        isScanning -> PairingPhase.SCANNING
                        else -> PairingPhase.IDLE
                    },
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "pairingPhase"
                ) { phase ->
                    when (phase) {
                        PairingPhase.IDLE -> {
                            ScanButton(onStartScan = onStartScan)
                        }
                        PairingPhase.SCANNING -> {
                            ScanningIndicator()
                        }
                        PairingPhase.DEVICES_FOUND -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                DiscoveredDevicesList(
                                    devices = discoveredDevices,
                                    onDeviceClick = onConnectDevice
                                )
                                // Rescan button
                                RescanButton(onStartScan = onStartScan)
                            }
                        }
                        PairingPhase.CONNECTING -> {
                            ConnectingIndicator(connectionState = connectionState)
                        }
                        PairingPhase.SUCCESS -> {
                            SuccessIndicator()
                        }
                    }
                }
            }

            // 5. Help Section
            item {
                Spacer(modifier = Modifier.height(32.dp))
                HelpSection()
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

private enum class PairingPhase {
    IDLE, SCANNING, DEVICES_FOUND, CONNECTING, SUCCESS
}

// ==========================================
// HEADER
// ==========================================
@Composable
private fun PairingHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CMF",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextOnSurface
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(TroughBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "SETUP",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = TextVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(TroughBg)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.BluetoothSearching,
                    contentDescription = null,
                    tint = CmfOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BLE Ready",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = TextVariant
                )
            }
        }
    }
}

// ==========================================
// WATCH HERO VISUAL
// ==========================================
@Composable
private fun WatchHeroSection(
    isScanning: Boolean,
    isConnecting: Boolean,
    isConnected: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated pulsing rings
        if (isScanning || isConnecting) {
            PulsingRings(
                color = if (isConnecting) CmfOrange else CmfPurple,
                rings = 3
            )
        }

        // Success ring
        if (isConnected) {
            val successAlpha by rememberInfiniteTransition(label = "successGlow").animateFloat(
                initialValue = 0.3f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    tween(1500, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "successAlpha"
            )
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CmfGreen.copy(alpha = successAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Static subtle radial glow behind watch
        if (!isScanning && !isConnecting && !isConnected) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CmfOrangeContainer.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Watch image
        Image(
            painter = painterResource(id = R.drawable.cmf_watch_pro2),
            contentDescription = "CMF Watch Pro 2",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(200.dp)
                .shadow(12.dp, CircleShape, clip = false)
        )

        // Connection status indicator at bottom of watch
        val indicatorColor = when {
            isConnected -> CmfGreen
            isConnecting -> CmfOrange
            isScanning -> CmfPurple
            else -> TextMutedGray
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-8).dp)
                .clip(PillShape)
                .background(CardBg)
                .shadow(4.dp, PillShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val dotTransition = rememberInfiniteTransition(label = "dot")
                val dotAlpha by dotTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(900, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse
                    ),
                    label = "dotAlpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor.copy(alpha = if (isScanning || isConnecting) dotAlpha else 1f))
                )
                Text(
                    text = when {
                        isConnected -> "Paired"
                        isConnecting -> "Connecting..."
                        isScanning -> "Scanning..."
                        else -> "Ready to pair"
                    },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = TextOnSurface
                )
            }
        }
    }
}

@Composable
private fun PulsingRings(color: Color, rings: Int) {
    val transition = rememberInfiniteTransition(label = "pulseRings")
    (0 until rings).forEach { index ->
        val delay = index * 400
        val scale by transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                tween(1800, delayMillis = delay, easing = FastOutSlowInEasing),
                RepeatMode.Restart
            ),
            label = "ringScale$index"
        )
        val alpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                tween(1800, delayMillis = delay, easing = FastOutSlowInEasing),
                RepeatMode.Restart
            ),
            label = "ringAlpha$index"
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
        )
    }
}

// ==========================================
// TITLE SECTION
// ==========================================
@Composable
private fun TitleSection(
    isScanning: Boolean,
    isConnecting: Boolean,
    isConnected: Boolean,
    connectionState: DeviceConnectionState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                isConnected -> "Watch Connected!"
                isConnecting -> when (connectionState) {
                    DeviceConnectionState.BONDING -> "Creating Bluetooth Bond..."
                    DeviceConnectionState.AUTHENTICATING -> "Authenticating..."
                    DeviceConnectionState.SUBSCRIBING -> "Establishing Link..."
                    else -> "Connecting to Watch..."
                }
                isScanning -> "Searching for Watches..."
                else -> "Connect Your Watch"
            },
            fontFamily = HeadlineFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp,
            color = TextOnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                isConnected -> "Your CMF Watch is paired and syncing.\nIt's now visible in your phone's Bluetooth settings."
                isConnecting -> when (connectionState) {
                    DeviceConnectionState.BONDING -> "Registering device with your phone's Bluetooth system.\nThis creates a persistent connection."
                    DeviceConnectionState.AUTHENTICATING -> "Securely authenticating with your watch.\nExchanging encryption keys."
                    else -> "Establishing a secure BLE connection.\nPlease keep your watch nearby."
                }
                isScanning -> "Make sure your CMF Watch is powered on\nand within Bluetooth range."
                else -> "Pair your CMF Watch Pro 2 to sync health data,\ntrack workouts, and receive notifications."
            },
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextMutedGray,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// SCAN BUTTON
// ==========================================
@Composable
private fun ScanButton(onStartScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = PillShape,
            color = CmfOrange,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bouncyClickable(onClick = onStartScan)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.BluetoothSearching,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Scan for Watch",
                    fontFamily = HeadlineFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
        }

        // Bluetooth info chip
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(TroughBg)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = TextMutedGray,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Bluetooth Low Energy · System pairing enabled",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = TextMutedGray
            )
        }
    }
}

// ==========================================
// SCANNING INDICATOR
// ==========================================
@Composable
private fun ScanningIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated scanning bar
        val transition = rememberInfiniteTransition(label = "scan")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(2000, easing = FastOutSlowInEasing),
                RepeatMode.Restart
            ),
            label = "scanProgress"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(PillShape)
                .background(TroughBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .fillMaxHeight()
                    .offset(x = (progress * 250).dp)
                    .clip(PillShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(CmfPurple.copy(alpha = 0.3f), CmfPurple, CmfPurple.copy(alpha = 0.3f))
                        )
                    )
            )
        }

        Text(
            text = "Looking for CMF Watch devices nearby...",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = TextVariant
        )
    }
}

// ==========================================
// DISCOVERED DEVICES LIST
// ==========================================
@Composable
private fun DiscoveredDevicesList(
    devices: List<DiscoveredDevice>,
    onDeviceClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Devices Found",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextOnSurface
            )
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(CmfOrangeContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${devices.size} found",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = CmfOrangeDark
                )
            }
        }

        devices.forEach { device ->
            DiscoveredDeviceCard(
                device = device,
                onClick = { onDeviceClick(device.address) }
            )
        }
    }
}

@Composable
private fun DiscoveredDeviceCard(
    device: DiscoveredDevice,
    onClick: () -> Unit
) {
    val signalStrength = when {
        device.rssi > -50 -> "Excellent"
        device.rssi > -70 -> "Good"
        device.rssi > -85 -> "Fair"
        else -> "Weak"
    }
    val signalColor = when {
        device.rssi > -50 -> CmfGreen
        device.rssi > -70 -> CmfOrange
        else -> TextMutedGray
    }

    Surface(
        shape = CardShape,
        color = CardBg,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Watch icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CmfOrangeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = CmfOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = device.name,
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = TextOnSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = device.address,
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = TextMutedGray
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(CardBorder)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = null,
                                tint = signalColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = signalStrength,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = signalColor
                            )
                        }
                    }
                }
            }

            // Connect arrow
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CmfOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Connect",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RescanButton(onStartScan: () -> Unit) {
    Surface(
        shape = PillShape,
        color = TroughBg,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onStartScan)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = CmfOrange,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Scan Again",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextOnSurface
            )
        }
    }
}

// ==========================================
// CONNECTING INDICATOR
// ==========================================
@Composable
private fun ConnectingIndicator(connectionState: DeviceConnectionState) {
    val steps = listOf(
        "Establishing BLE connection" to (connectionState.ordinal >= DeviceConnectionState.CONNECTING.ordinal),
        "Creating system Bluetooth bond" to (connectionState.ordinal >= DeviceConnectionState.BONDING.ordinal),
        "Discovering watch services" to (connectionState.ordinal >= DeviceConnectionState.SUBSCRIBING.ordinal),
        "Authenticating securely" to (connectionState.ordinal >= DeviceConnectionState.AUTHENTICATING.ordinal)
    )
    val currentStep = when (connectionState) {
        DeviceConnectionState.CONNECTING -> 0
        DeviceConnectionState.BONDING -> 1
        DeviceConnectionState.CONNECTED, DeviceConnectionState.SUBSCRIBING -> 2
        DeviceConnectionState.AUTHENTICATING -> 3
        else -> 0
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress bar
        val animatedProgress by animateFloatAsState(
            targetValue = (currentStep + 1f) / steps.size.toFloat(),
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "connectionProgress"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(PillShape)
                .background(TroughBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(PillShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(CmfOrange, CmfOrangeDark)
                        )
                    )
            )
        }

        // Step list
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                steps.forEachIndexed { index, (label, reached) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when {
                            index < currentStep -> {
                                // Completed
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(CmfGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            index == currentStep -> {
                                // In progress - spinning
                                val rotation by rememberInfiniteTransition(label = "spin$index").animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        tween(1200, easing = LinearEasing)
                                    ),
                                    label = "spinRotation$index"
                                )
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(20.dp).rotate(rotation)) {
                                        drawArc(
                                            color = CmfOrange,
                                            startAngle = 0f,
                                            sweepAngle = 270f,
                                            useCenter = false,
                                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                }
                            }
                            else -> {
                                // Pending
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(TroughBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextMutedGray
                                    )
                                }
                            }
                        }

                        Text(
                            text = label,
                            fontFamily = InterFontFamily,
                            fontWeight = if (index == currentStep) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp,
                            color = when {
                                index < currentStep -> CmfGreen
                                index == currentStep -> TextOnSurface
                                else -> TextMutedGray
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SUCCESS INDICATOR
// ==========================================
@Composable
private fun SuccessIndicator() {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(CmfGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Surface(
            shape = PillShape,
            color = CmfGreen.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothConnected,
                    contentDescription = null,
                    tint = CmfGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bonded & visible in Bluetooth Settings",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = CmfGreen
                )
            }
        }
    }
}

// ==========================================
// HELP SECTION
// ==========================================
@Composable
private fun HelpSection() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = TroughBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Having trouble?",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = TextOnSurface
            )

            HelpRow(
                icon = Icons.Outlined.Watch,
                text = "Make sure your watch is powered on and not paired to another phone."
            )
            HelpRow(
                icon = Icons.Outlined.Bluetooth,
                text = "Ensure Bluetooth is enabled on your phone."
            )
            HelpRow(
                icon = Icons.Outlined.NearMe,
                text = "Keep the watch within 1 meter during initial pairing."
            )
            HelpRow(
                icon = Icons.Outlined.Refresh,
                text = "If the watch doesn't appear, restart it and try scanning again."
            )
        }
    }
}

@Composable
private fun HelpRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CmfOrange,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Text(
            text = text,
            fontFamily = InterFontFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = TextVariant
        )
    }
}
