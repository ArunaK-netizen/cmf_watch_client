package com.cmfwatch.companion.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DashboardSummary
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.domain.models.DiscoveredDevice
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
        modifier = modifier.fillMaxSize().background(BlackBackground),
        contentPadding = PaddingValues(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 112.dp)
    ) {
        item {
            Text("WATCH", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Text("Device", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(28.dp))
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth().background(DarkCardSurface, RoundedCornerShape(18.dp)).padding(20.dp)
            ) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("CMF Watch Pro 2", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (summary.connectionState == DeviceConnectionState.CONNECTED_PAIRED) "Connected" else "Searching for your watch",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                        StatusPill(summary.connectionState, summary.deviceBatteryLevel)
                    }
                    Spacer(Modifier.height(20.dp))
                    if (summary.connectionState == DeviceConnectionState.CONNECTED_PAIRED || summary.connectionState == DeviceConnectionState.SYNCING) {
                        Text(
                            if (summary.connectionState == DeviceConnectionState.SYNCING) "Updating your health data" else "Connected. Sync runs automatically.",
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onSyncNow,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCardVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Refresh now", fontSize = 14.sp, color = TextPrimary)
                        }
                        Button(
                            onClick = onDisconnectDevice,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disconnect", fontSize = 14.sp, color = TextSecondary)
                        }
                    } else {
                        Button(
                            onClick = onStartScan,
                            enabled = summary.connectionState != DeviceConnectionState.SCANNING,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (summary.connectionState == DeviceConnectionState.SCANNING) "Searching..." else "Find a different watch",
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        if (summary.discoveredDevices.isNotEmpty()) {
            item {
                Text("NEARBY WATCHES", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(12.dp))
            }
            items(summary.discoveredDevices) { device ->
                DiscoveredDeviceRow(device, { onConnectDevice(device.address) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceRow(device: DiscoveredDevice, onConnect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onConnect).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(device.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text("Nearby  •  ${device.rssi} dBm", fontSize = 13.sp, color = TextSecondary)
        }
        Text("Connect", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AccentRed)
    }
}
