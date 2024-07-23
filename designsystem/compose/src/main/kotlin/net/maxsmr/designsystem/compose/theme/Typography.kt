package net.maxsmr.designsystem.compose.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.maxsmr.designsystem.compose.theme.AppTypography.bodyLgRegular
import net.maxsmr.designsystem.shared_res.R

val tekturNarrowFontFamily = FontFamily(
    Font(R.font.ttf_tektur_narrow_regular, FontWeight.Normal),
    Font(R.font.ttf_tektur_narrow_medium, FontWeight.Medium),
    Font(R.font.ttf_tektur_narrow_bold, FontWeight.Bold),
)

// Set of Material typography styles to start with
val Typography = Typography(
    // TODO
)

@Immutable
object AppTypography {
    val displayLgRegular = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 33.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.4.sp,
    )
    val displayMdRegular = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.29.sp,
    )
    val headerMdSemibold = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.29.sp,
    )
    val headerSmRegular = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )
    val headerSmSemibold = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )
    val bodyLgRegular = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyLgRegularInput = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyLgSemibold = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyLgMediumUppercaseCompact = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyMdRegular = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyMdSemibold = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodyMdMedium = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodySmRegular = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val bodySmSemibold = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val labelLgRegular = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
    val labelLgSemibold = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
    val labelMdRegular = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.06.sp,
    )
    val labelMdSemibold = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.06.sp,
    )
    val labelSmSemibold = TextStyle(
        fontFamily = robotoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp,
    )
}