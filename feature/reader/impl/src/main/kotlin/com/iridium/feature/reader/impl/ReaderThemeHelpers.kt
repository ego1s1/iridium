package com.iridium.feature.reader.impl

import androidx.compose.ui.graphics.Color
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.isLightScheme
import com.iridium.core.model.pageBackgroundArgb

/**
 * Page background per reader theme (mirrors the settings swatches): system
 * bars tint to this so they read as a seamless frame around the page.
 */
internal fun ColorSchemeChoice.pageBackground(): Color = Color(pageBackgroundArgb())

/** Dark status/nav icons on the light page themes, light icons otherwise. */
internal fun ColorSchemeChoice.lightSystemBars(): Boolean = isLightScheme()
