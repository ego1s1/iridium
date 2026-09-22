package com.iridium.core.designsystem

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** Light + dark previews in one annotation, on the expressive theme. */
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews

/** Wraps [content] in the theme for previews that need explicit composition. */
@Composable
fun IridiumPreview(content: @Composable () -> Unit) {
    IridiumTheme(dynamicColor = false, content = content)
}
