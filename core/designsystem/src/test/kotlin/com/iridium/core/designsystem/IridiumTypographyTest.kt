package com.iridium.core.designsystem

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class IridiumTypographyTest {

    @Test
    fun verifiesL1ToL4TypographyTokens() {
        // L1: Top bar title (28sp / 34sp, wght 900)
        assertEquals(28.sp, ScreenTitleSize)
        assertEquals(34.sp, ScreenTitleLineHeight)
        assertEquals(28.sp, TopBarTitleStyle.fontSize)
        assertEquals(34.sp, TopBarTitleStyle.lineHeight)
        assertEquals(FontWeight.Black, TopBarTitleStyle.fontWeight)

        // L2: titleLarge (22sp / 28sp, wght 700)
        assertEquals(22.sp, IridiumTypography.titleLarge.fontSize)
        assertEquals(28.sp, IridiumTypography.titleLarge.lineHeight)
        assertEquals(FontWeight.Bold, IridiumTypography.titleLarge.fontWeight)
        assertEquals(22.sp, IridiumEmphasized.titleLarge.fontSize)
        assertEquals(28.sp, IridiumEmphasized.titleLarge.lineHeight)
        assertEquals(FontWeight.Bold, IridiumEmphasized.titleLarge.fontWeight)

        // L3: titleMedium (16sp / 24sp)
        assertEquals(16.sp, IridiumTypography.titleMedium.fontSize)
        assertEquals(24.sp, IridiumTypography.titleMedium.lineHeight)
        assertEquals(16.sp, IridiumEmphasized.titleMedium.fontSize)
        assertEquals(24.sp, IridiumEmphasized.titleMedium.lineHeight)

        // L4: bodyLarge (16sp / 24sp)
        assertEquals(16.sp, IridiumTypography.bodyLarge.fontSize)
        assertEquals(24.sp, IridiumTypography.bodyLarge.lineHeight)
        assertEquals(16.sp, IridiumEmphasized.bodyLarge.fontSize)
        assertEquals(24.sp, IridiumEmphasized.bodyLarge.lineHeight)
        assertEquals(FontWeight.SemiBold, IridiumEmphasized.bodyLarge.fontWeight)
    }

    @Test
    fun appFontsProvidesAllNamedDisplayRoles() {
        val fonts = appFonts(darkTheme = false)
        val darkFonts = appFonts(darkTheme = true)

        org.junit.Assert.assertNotNull(fonts.topBarTitle)
        org.junit.Assert.assertNotNull(fonts.annotatedString)
        org.junit.Assert.assertNotNull(fonts.displayFlex)
        org.junit.Assert.assertNotNull(fonts.displayFlexMedium)
        org.junit.Assert.assertNotNull(fonts.displayUnit)
        org.junit.Assert.assertNotNull(fonts.displaySoft)

        org.junit.Assert.assertNotNull(darkFonts.topBarTitle)
        org.junit.Assert.assertNotNull(darkFonts.annotatedString)
        org.junit.Assert.assertNotNull(darkFonts.displayFlex)
        org.junit.Assert.assertNotNull(darkFonts.displayFlexMedium)
        org.junit.Assert.assertNotNull(darkFonts.displayUnit)
        org.junit.Assert.assertNotNull(darkFonts.displaySoft)
    }
}
