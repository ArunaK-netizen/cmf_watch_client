package com.cmfwatch.companion.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.domain.models.DeviceConnectionState
import com.cmfwatch.companion.ui.theme.CmfOnSurface
import com.cmfwatch.companion.ui.theme.CmfOnSurfaceVariant
import com.cmfwatch.companion.ui.theme.CmfPrimary
import com.cmfwatch.companion.ui.theme.CmfSurfaceContainer
import com.cmfwatch.companion.ui.theme.CmfSurfaceHigh
import com.cmfwatch.companion.ui.theme.CmfSurfaceLow
import com.cmfwatch.companion.ui.theme.CmfSurfaceLowest
import com.cmfwatch.companion.ui.theme.CmfTertiaryContainer
import com.cmfwatch.companion.ui.theme.HeadlineFontFamily
import com.cmfwatch.companion.ui.theme.InterFontFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val CmfCardShape = RoundedCornerShape(32.dp)
val CmfPillShape = RoundedCornerShape(50)
val CmfSoftShadow = Color(0x14000000)

fun Modifier.cmfCard(
    radius: Dp = 32.dp,
    elevation: Dp = 1.dp
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = CmfSoftShadow,
            spotColor = CmfSoftShadow
        )
        .clip(shape)
        .background(CmfSurfaceLowest)
}

@Composable
fun CmfPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
        .uppercase(Locale.getDefault()),
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = eyebrow,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.66.sp,
                color = CmfOnSurfaceVariant
            )
            Text(
                text = title,
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = (-0.56).sp,
                color = CmfOnSurface
            )
        }
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

@Composable
fun CmfSectionTitle(
    title: String,
    action: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = HeadlineFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = CmfOnSurface
        )
        if (action != null) {
            Text(
                text = action,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = CmfPrimary
            )
        }
    }
}

@Composable
fun CmfStatusPill(
    text: String,
    container: Color,
    content: Color = CmfOnSurfaceVariant
) {
    Box(
        modifier = Modifier
            .clip(CmfPillShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.66.sp,
            color = content
        )
    }
}

fun DeviceConnectionState.isCmfConnected(): Boolean =
    this == DeviceConnectionState.CONNECTED ||
        this == DeviceConnectionState.CONNECTED_PAIRED ||
        this == DeviceConnectionState.SYNCING ||
        this == DeviceConnectionState.SUBSCRIBING ||
        this == DeviceConnectionState.AUTHENTICATING ||
        this == DeviceConnectionState.DISCOVERING_SERVICES

@Composable
fun CmfPulseDot(color: Color, size: Dp = 6.dp) {
    val transition = rememberInfiniteTransition(label = "pulseDot")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun CmfAppHeader(
    connected: Boolean,
    battery: Int?,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                ambientColor = Color(0x08000000),
                spotColor = Color(0x08000000)
            )
            .background(CmfSurfaceLowest.copy(alpha = 0.82f))
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CMF",
                fontFamily = HeadlineFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CmfOnSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CmfPillShape)
                    .background(CmfSurfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "PRO",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.66.sp,
                    color = CmfOnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(CmfPillShape)
                    .background(CmfSurfaceLow)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CmfPulseDot(color = if (connected) CmfTertiaryContainer else CmfOnSurfaceVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.width(6.dp))
                val status = buildString {
                    append(if (connected) "Connected" else "Offline")
                    if (battery != null) append(" · $battery%")
                }
                Text(
                    text = status,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = CmfOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(CmfSurfaceHigh)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile",
                tint = CmfOnSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
