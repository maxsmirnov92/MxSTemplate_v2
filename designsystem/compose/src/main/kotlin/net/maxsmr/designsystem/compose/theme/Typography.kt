package net.maxsmr.designsystem.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.maxsmr.designsystem.shared_res.R

val robotoFontFamily = FontFamily(
    Font(R.font.ttf_roboto_light, FontWeight.Light),
    Font(R.font.ttf_roboto_regular, FontWeight.Normal),
    Font(R.font.ttf_roboto_medium, FontWeight.Medium),
    Font(R.font.ttf_roboto_bold, FontWeight.Bold),
)

@Immutable
object AppTypography {
    val displayLgRegular = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 33.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.4.sp,
    )
    val displayMdRegular = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.29.sp,
    )
    val headerMdSemibold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.29.sp,
    )
    val headerSmRegular = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )
    val headerSmSemibold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )
    val bodyLgRegular = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyLgRegularInput = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyLgSemibold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyLgMediumUppercaseCompact = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyMdRegular = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyMdSemibold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyMdMedium = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodySmRegular = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodySmSemibold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val labelLgRegular = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
    val labelLgSemibold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
    val labelMdRegular = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.06.sp,
    )
    val labelMdSemibold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.06.sp,
    )
    val labelSmSemibold = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp,
    )
}