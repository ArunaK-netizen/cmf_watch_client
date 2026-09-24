package com.cmfwatch.companion.ui.device

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.domain.models.DiscoveredDevice
import com.cmfwatch.companion.ui.theme.*

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
    val isConnected = summary.connectionState == DeviceConnectionState.CONNECTED_PAIRED ||
            summary.connectionState == DeviceConnectionState.CONNECTED

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 132.dp)
    ) {
        // 1. Top Bar (< Back, Options ...)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
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
        }

        // 2. Hero Header Section (CMF Watch Title, Battery, Status & Watch Illustration)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column Stats
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "CMF Watch",
                        fontFamily = HeadlineFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.56).sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) RingGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isConnected) RingGreen else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isConnected) "Last synced 2 min ago" else "Tap Sync to update",
                        fontFamily = AppFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Battery Badge Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE5E5EA))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryFull,
                                contentDescription = null,
                                tint = if (summary.deviceBatteryLevel != null) RingGreen else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = summary.deviceBatteryLevel?.let { "$it%" } ?: "--",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Right Watch Render Canvas Illustration
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFE3F2FD), Color(0xFFF6F8FA))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CmfWatchProVectorIllustration()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. Quick Actions Card Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(SurfaceWhite)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Quick actions",
                        fontFamily = AppFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionButton(
                            label = "Watch Faces",
                            icon = Icons.Default.Watch,
                            iconColor = Color(0xFF7C4DFF),
                            bgColor = Color(0xFFEDE7F6),
                            onClick = { }
                        )
                        QuickActionButton(
                            label = "Notifications",
                            icon = Icons.Default.Notifications,
                            iconColor = Color(0xFFFF5252),
                            bgColor = Color(0xFFFFEBEE),
                            hasBadge = false,
                            onClick = { }
                        )
                        QuickActionButton(
                            label = "Find My Watch",
                            icon = Icons.Default.MyLocation,
                            iconColor = Color(0xFF34C759),
                            bgColor = Color(0xFFE8F8EE),
                            onClick = { if (isConnected) onSyncNow() }
                        )
                        QuickActionButton(
                            label = "Watch Settings",
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFF29B6F6),
                            bgColor = Color(0xFFE3F2FD),
                            onClick = { }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. Main Settings Group Card 1
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(SurfaceWhite)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.BarChart,
                        iconColor = Color(0xFF7C4DFF),
                        iconBg = Color(0xFFEDE7F6),
                        title = "Health Settings",
                        subtitle = "Heart rate, sleep, activity tracking"
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.Folder,
                        iconColor = Color(0xFF29B6F6),
                        iconBg = Color(0xFFE3F2FD),
                        title = "Apps",
                        subtitle = "Manage apps on your watch"
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.Smartphone,
                        iconColor = Color(0xFFFFB300),
                        iconBg = Color(0xFFFFF8E1),
                        title = "Display & Brightness",
                        subtitle = "Watch face, brightness, always-on"
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.VolumeUp,
                        iconColor = Color(0xFFFF5252),
                        iconBg = Color(0xFFFFEBEE),
                        title = "Sound & Vibration",
                        subtitle = "Ringtones, vibration, do not disturb"
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.CloudSync,
                        iconColor = Color(0xFF34C759),
                        iconBg = Color(0xFFE8F8EE),
                        title = "Sync & Data",
                        subtitle = if (summary.connectionState == DeviceConnectionState.SYNCING) "Syncing data..." else "Last synced 2 min ago",
                        onClick = onSyncNow
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5. Settings Group Card 2 (About & Help)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(SurfaceWhite)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF8E8E93),
                        iconBg = Color(0xFFF2F2F7),
                        title = "About",
                        subtitle = "Device info, firmware, legal"
                    )
                    SettingsDivider()
                    SettingsRowItem(
                        icon = Icons.Default.Help,
                        iconColor = Color(0xFF007AFF),
                        iconBg = Color(0xFFE5F1FF),
                        title = "Help & Support",
                        subtitle = "User guide, troubleshooting"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 6. Soft Red Disconnect Watch Button
        item {
            val disconnectInteractionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFEBEE))
                    .clickable(
                        interactionSource = disconnectInteractionSource,
                        indication = null
                    ) {
                        if (isConnected) {
                            onDisconnectDevice()
                        } else {
                            onStartScan()
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Disconnect Watch" else "Pair Watch",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFFFF3B30)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 7. Nearby Watches Discovery List (When disconnected/scanning)
        if (summary.discoveredDevices.isNotEmpty()) {
            item {
                Text(
                    text = "NEARBY WATCHES",
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(summary.discoveredDevices) { device ->
                DiscoveredDeviceRow(device, { onConnectDevice(device.address) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    hasBadge: Boolean = false,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )

            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .offset(x = 10.dp, y = (-10).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = TextPrimary
        )
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
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
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontFamily = AppFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp)
            .height(1.dp)
            .background(Color(0xFFF2F2F7))
    )
}

@Composable
fun CmfWatchProVectorIllustration() {
    Canvas(modifier = Modifier.size(110.dp, 130.dp)) {
        val w = size.width
        val h = size.height

        // Watch Straps (Black vertical bands)
        drawRoundRect(
            color = Color(0xFF2C2C2E),
            topLeft = Offset(w * 0.25f, 0f),
            size = Size(w * 0.5f, h),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )

        // Watch Casing (Dark Gray Metallic Bezel)
        drawRoundRect(
            color = Color(0xFF1C1C1E),
            topLeft = Offset(w * 0.12f, h * 0.15f),
            size = Size(w * 0.76f, h * 0.7f),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
        )

        // Digital Crown / Dial Button on Right
        drawRoundRect(
            color = Color(0xFF48484A),
            topLeft = Offset(w * 0.88f, h * 0.42f),
            size = Size(w * 0.08f, h * 0.16f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Screen Display (Black glass)
        drawRoundRect(
            color = Color(0xFF000000),
            topLeft = Offset(w * 0.16f, h * 0.19f),
            size = Size(w * 0.68f, h * 0.62f),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )

        // Orange Accent Dial Graphic on Screen
        drawCircle(
            color = Color(0xFFFF5216),
            radius = 12.dp.toPx(),
            center = Offset(w * 0.62f, h * 0.58f)
        )
    }
}

@Composable
private fun DiscoveredDeviceRow(device: DiscoveredDevice, onConnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onConnect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Nearby • ${device.rssi} dBm",
                fontFamily = AppFontFamily,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            text = "Connect",
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = AccentRed
        )
    }
}
