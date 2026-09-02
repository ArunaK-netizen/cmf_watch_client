package com.cmfwatch.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.ui.theme.DarkCardSurface
import com.cmfwatch.companion.ui.theme.NType82FontFamily
import com.cmfwatch.companion.ui.theme.TextPrimary
import com.cmfwatch.companion.ui.theme.TextSecondary

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String = "",
    subtitle: String? = null,
    accentColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCardSurface)
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    fontFamily = NType82FontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontFamily = NType82FontFamily,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextPrimary
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = unit,
                        fontFamily = NType82FontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontFamily = NType82FontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
