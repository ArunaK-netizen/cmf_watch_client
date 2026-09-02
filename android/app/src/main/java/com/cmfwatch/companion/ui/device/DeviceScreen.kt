package com.cmfwatch.companion.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.domain.models.DiscoveredDevice
import com.cmfwatch.companion.ui.components.MetricCard
import com.cmfwatch.companion.ui.components.StatusPill
import com.cmfwatch.companion.ui.theme.*

@Composable
fun DeviceScreen(
    summary: DashboardSummary,
    onStartScan: () -> Unit = {},
    onConnectDevice: (String) -> Unit = {},
    onDisconnectDevice: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BlackBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            Text(
                text = "HARDWARE & BLE DISCOVERY",
                fontFamily = NType82FontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Device",
                fontFamily = NType82FontFamily,
                fontSize = 32.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Active Connection Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCardSurface)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CMF Watch",
                            fontFamily = NType82FontFamily,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                        StatusPill(
                            state = summary.connectionState,
                            batteryLevel = summary.deviceBatteryLevel
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (summary.connectionState == DeviceConnectionState.CONNECTED_PAIRED) {
                        Button(
                            onClick = onSyncNow,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "SYNC TELEMETRY NOW",
                                fontFamily = NType82FontFamily,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onDisconnectDevice,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCardVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "DISCONNECT WATCH",
                                fontFamily = NType82FontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = onStartScan,
                            enabled = summary.connectionState != DeviceConnectionState.SCANNING,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val scanBtnText = if (summary.connectionState == DeviceConnectionState.SCANNING) {
                                "SCANNING FOR WATCHES..."
                            } else {
                                "SCAN FOR NEARBY CMF WATCHES"
                            }
                            Text(
                                text = scanBtnText,
                                fontFamily = NType82FontFamily,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Discovered Peripherals Section
        if (summary.discoveredDevices.isNotEmpty()) {
            item {
                Text(
                    text = "DISCOVERED PERIPHERALS (${summary.discoveredDevices.size})",
                    fontFamily = NType82FontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(summary.discoveredDevices) { device ->
                DiscoveredDeviceCard(
                    device = device,
                    onConnect = { onConnectDevice(device.address) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else if (summary.connectionState == DeviceConnectionState.SCANNING) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCardSurface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Searching for advertising CMF Watch peripherals...",
                        fontFamily = NType82FontFamily,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            val batteryText = summary.deviceBatteryLevel?.let { "$it%" } ?: "--"
            val batterySubtitle = if (summary.deviceBatteryLevel != null) "Hardware AFE & BLE Radio Active" else "Battery telemetry unavailable"

            MetricCard(
                title = "Battery Telemetry",
                value = batteryText,
                unit = "State",
                subtitle = batterySubtitle,
                accentColor = AccentActivity
            )
        }
    }
}

@Composable
fun DiscoveredDeviceCard(
    device: DiscoveredDevice,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCardSurface)
            .clickable { onConnect() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name,
                    fontFamily = NType82FontFamily,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    text = "MAC: ${device.address}  •  RSSI: ${device.rssi} dBm",
                    fontFamily = NType82FontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = AccentActivity),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "CONNECT",
                    fontFamily = NType82FontFamily,
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }
        }
    }
}
