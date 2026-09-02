package com.cmfwatch.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.ui.theme.*

@Composable
fun StatusPill(
    state: DeviceConnectionState,
    batteryLevel: Int?,
    modifier: Modifier = Modifier
) {
    val batteryText = batteryLevel?.let { " • $it%" } ?: ""
    val (statusText, statusColor) = when (state) {
        DeviceConnectionState.CONNECTED_PAIRED -> "CONNECTED$batteryText" to AccentActivity
        DeviceConnectionState.CONNECTED -> "CONNECTED$batteryText" to AccentActivity
        DeviceConnectionState.SYNCING -> "SYNCING..." to AccentSpO2
        DeviceConnectionState.AUTHENTICATING -> "AUTHENTICATING..." to AccentSleepAwake
        DeviceConnectionState.CONNECTING -> "CONNECTING..." to AccentSleepAwake
        DeviceConnectionState.SCANNING -> "SCANNING..." to AccentSleepAwake
        DeviceConnectionState.DISCONNECTED -> "DISCONNECTED" to TextSecondary
        DeviceConnectionState.ERROR -> "CONNECTION ERROR" to AccentRed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCardVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                fontFamily = NType82FontFamily,
                fontSize = 11.sp,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
        }
    }
}
