package com.cmfwatch.companion.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.ui.components.MetricCard
import com.cmfwatch.companion.ui.components.StatusPill
import com.cmfwatch.companion.ui.theme.*

@Composable
fun DeviceScreen(
    connectionState: DeviceConnectionState,
    batteryLevel: Int,
    deviceName: String = "CMF Watch Pro 2",
    macAddress: String = "3C:B0:ED:3F:BA:70",
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
                text = "HARDWARE & GATT",
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
                            text = deviceName,
                            fontFamily = NType82FontFamily,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                        StatusPill(state = connectionState, batteryLevel = batteryLevel)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "MAC: $macAddress",
                        fontFamily = NType82FontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

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
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            MetricCard(
                title = "Battery Telemetry",
                value = "$batteryLevel%",
                unit = "State",
                subtitle = "Hardware AFE & BLE Radio Active",
                accentColor = AccentActivity
            )
        }
    }
}
