@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.iridium.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Variable Google Sans Flex font families.
 *
 * Provides variable weight, optical sizing, and width.
 */
internal val BodyFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(14.sp),
        ),
    ),
)

internal val HeadingFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(18.sp),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f),
            FontVariation.grade(0),
            FontVariation.opticalSizing(20.sp),
        ),
    ),
)

private val baseline = Typography()

private fun TextStyle.body() = copy(fontFamily = BodyFlex)
private fun TextStyle.heading() = copy(fontFamily = HeadingFlex)

/**
 * Screen title size and leading shared by collapsing top bars.
 * L1: topBarTitle (28sp/28sp, wght 900).
 */
val ScreenTitleSize = 28.sp
val ScreenTitleLineHeight = 28.sp

/**
 * Top bar title style conforming to L1 typography hierarchy:
 * 28sp / 28sp with font weight 900 (Black).
 */
val TopBarTitleStyle = TextStyle(
    fontFamily = HeadingFlex,
    fontWeight = FontWeight.Black,
    fontSize = 28.sp,
    lineHeight = 28.sp,
)

/**
 * Iridium type scale built on variable Google Sans Flex.
 * L1-L4 hierarchy:
 * - L1: topBarTitle 28sp / 28sp, wght 900
 * - L2: titleLarge 22sp / 28sp, wght 700
 * - L3: titleMedium 16sp / 24sp
 * - L4: bodyLarge 16sp / 24sp
 */
val IridiumTypography = baseline.copy(
    displayLarge = baseline.displayLarge.heading(),
    displayMedium = baseline.displayMedium.heading(),
    displaySmall = baseline.displaySmall.heading(),
    headlineLarge = baseline.headlineLarge.heading(),
    headlineMedium = baseline.headlineMedium.heading(),
    headlineSmall = baseline.headlineSmall.heading(),
    titleLarge = baseline.titleLarge.heading().copy(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = baseline.titleMedium.heading().copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = baseline.titleSmall.heading(),
    bodyLarge = baseline.bodyLarge.heading().copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = baseline.bodyMedium.body(),
    bodySmall = baseline.bodySmall.body(),
    labelLarge = baseline.labelLarge.heading(),
    labelMedium = baseline.labelMedium.heading(),
    labelSmall = baseline.labelSmall.heading(),
)

private fun TextStyle.emphasized(weight: FontWeight = FontWeight.Bold) =
    copy(fontFamily = HeadingFlex, fontWeight = weight)

/**
 * Expressive emphasized styles for hero moments, selections, and headlines.
 * Aligns with M3 Expressive hierarchy:
 * - titleLarge: 22sp / 28sp, wght 700 (L2)
 * - titleMedium: 16sp / 24sp, wght 700 (L3)
 * - bodyLarge: 16sp / 24sp, wght 600 (L4)
 */
object IridiumEmphasized {
    val displaySmall: TextStyle = IridiumTypography.displaySmall.emphasized()
    val headlineMedium: TextStyle = IridiumTypography.headlineMedium.emphasized()
    val headlineSmall: TextStyle = IridiumTypography.headlineSmall.emphasized()
    val titleLarge: TextStyle = IridiumTypography.titleLarge.emphasized(FontWeight.Bold)
    val titleMedium: TextStyle = IridiumTypography.titleMedium.emphasized(FontWeight.Bold)
    val titleSmall: TextStyle = IridiumTypography.titleSmall.emphasized()
    val bodyLarge: TextStyle = IridiumTypography.bodyLarge.emphasized(FontWeight.SemiBold)
    val bodyMedium: TextStyle = IridiumTypography.bodyMedium.emphasized(FontWeight.SemiBold)
    val labelLarge: TextStyle = IridiumTypography.labelLarge.emphasized()
}
