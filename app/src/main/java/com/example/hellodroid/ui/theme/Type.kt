package com.example.hellodroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.hellodroid.R

// Set of Material typography styles to start with
private val robotoCondensedFont = FontFamily(
    Font(R.font.robotocondensed_thin, FontWeight.Thin),
    Font(R.font.robotocondensed_light, FontWeight.Light),
    Font(R.font.robotocondensed_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.robotocondensed_bold, FontWeight.Bold),
    Font(R.font.robotocondensed_regular, FontWeight.Normal),
    Font(R.font.robotocondensed_extrabold, FontWeight.ExtraBold)
)


val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = robotoCondensedFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = robotoCondensedFont,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = robotoCondensedFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)