package com.iridium.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/** Material baseline type scale; contrast comes from emphasis, not size alone. */
val IridiumTypography = Typography()

/**
 * Bold twins of the headline/title styles, for hero moments: screen titles,
 * progress numbers, empty-state headings. Used sparingly (M3 Expressive
 * guidance) so emphasis stays meaningful.
 */
object IridiumEmphasized {
    val displaySmall: TextStyle =
        IridiumTypography.displaySmall.copy(fontWeight = FontWeight.Bold)
    val headlineMedium: TextStyle =
        IridiumTypography.headlineMedium.copy(fontWeight = FontWeight.Bold)
    val headlineSmall: TextStyle =
        IridiumTypography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    val titleLarge: TextStyle =
        IridiumTypography.titleLarge.copy(fontWeight = FontWeight.Bold)
    val titleMedium: TextStyle =
        IridiumTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
}
