package com.cmfwatch.companion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cmfwatch.companion.R

// Nothing NType82 Font Family Definition
val NType82FontFamily = FontFamily(
    Font(R.font.ntype82_regular, FontWeight.Normal)
)

// Nothing-Style Typography Scale
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NType82FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = NType82FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = NType82FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = NType82FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = NType82FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = NType82FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = NType82FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = NType82FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = TextSecondary
    )
)
