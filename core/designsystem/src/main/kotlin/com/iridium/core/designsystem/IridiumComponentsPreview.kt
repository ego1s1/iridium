package com.iridium.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iridium.core.model.ThemePreferences

/**
 * Light/dark catalog of every design-system component for visual-drift
 * detection. Each preview renders in both themes via [ThemePreviews].
 */
@ThemePreviews
@Composable
private fun ButtonsPreview() {
    IridiumPreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            IridiumPrimaryButton(onClick = {}) { Text("Primary") }
            IridiumPrimaryButton(onClick = {}, enabled = false) { Text("Primary disabled") }
            IridiumTonalButton(onClick = {}) { Text("Tonal") }
            IridiumTonalButton(onClick = {}, enabled = false) { Text("Tonal disabled") }
            IridiumOutlinedButton(onClick = {}) { Text("Outlined") }
            IridiumOutlinedButton(onClick = {}, enabled = false) { Text("Outlined disabled") }
            IridiumTextButton(onClick = {}) { Text("Text") }
            IridiumTextButton(onClick = {}, enabled = false) { Text("Text disabled") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IridiumIconButton(onClick = {}) {
                    Icon(IridiumIcons.Settings, contentDescription = null)
                }
                IridiumIconButton(onClick = {}, enabled = false) {
                    Icon(IridiumIcons.Settings, contentDescription = null)
                }
                IridiumFilledTonalIconButton(onClick = {}) {
                    Icon(IridiumIcons.Add, contentDescription = null)
                }
                IridiumFilledTonalIconButton(onClick = {}, enabled = false) {
                    Icon(IridiumIcons.Add, contentDescription = null)
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun SettingRowPreview() {
    IridiumPreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            IridiumSettingRow(
                title = "Title",
                subtitle = "Subtitle",
                icon = IridiumIcons.Settings,
                onClick = {},
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SettingSwitchPreview() {
    IridiumPreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            IridiumSettingSwitch(title = "On", checked = true, onCheckedChange = {})
            IridiumSettingSwitch(title = "Off", checked = false, onCheckedChange = {})
        }
    }
}

@ThemePreviews
@Composable
private fun SliderRowPreview() {
    IridiumPreview {
        Column(modifier = Modifier.padding(16.dp)) {
            IridiumSliderRow(
                label = "Font size",
                value = 0.5f,
                valueRange = 0f..1f,
                onValueChange = {},
                valueText = "50%",
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ChoiceGroupPreview() {
    IridiumPreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            IridiumChoiceGroup(
                options = listOf(
                    IridiumChoiceOption("System"),
                    IridiumChoiceOption("Light"),
                    IridiumChoiceOption("Dark"),
                ),
                selectedIndex = 0,
                onSelect = {},
            )
            IridiumFilterPills(
                options = listOf(
                    IridiumChoiceOption("All"),
                    IridiumChoiceOption("Reading"),
                    IridiumChoiceOption("Finished"),
                ),
                selectedIndex = 1,
                onSelect = {},
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SectionCardPreview() {
    IridiumPreview {
        Column(modifier = Modifier.padding(16.dp)) {
            IridiumSectionCard(title = "Section") {
                Text("Card body")
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ErrorCardPreview() {
    IridiumPreview {
        Column(modifier = Modifier.padding(16.dp)) {
            IridiumErrorCard(
                body = "Something went wrong.",
                primaryLabel = "Retry",
                onPrimary = {},
                secondaryLabel = "Dismiss",
                onSecondary = {},
            )
        }
    }
}

@ThemePreviews
@Composable
private fun EmptyStatePreview() {
    IridiumPreview {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.height(320.dp)) {
                IridiumEmptyState(
                    icon = IridiumIcons.MenuBook,
                    title = "Nothing here",
                    body = "Import a book to get started.",
                    actionLabel = "Import",
                    onAction = {},
                )
            }
            Box(modifier = Modifier.height(280.dp)) {
                IridiumEmptyState(
                    icon = IridiumIcons.MenuBook,
                    title = "Nothing here",
                    body = "No action available.",
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ProgressBarPreview() {
    IridiumPreview {
        Column(modifier = Modifier.padding(16.dp)) {
            IridiumProgressBar(progress = { 0.6f })
        }
    }
}

@ThemePreviews
@Composable
private fun ScrimPillPreview() {
    IridiumPreview {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            IridiumScrimPill(text = "Page 12 of 300")
            IridiumScrimPill(text = "", icon = IridiumIcons.Bookmark)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ThemePreviews
@Composable
private fun CollapsingTopBarPreview() {
    IridiumPreview {
        IridiumCollapsingTopBar(title = "Library")
    }
}

@ThemePreviews
@Composable
private fun SectionHeaderPreview() {
    IridiumPreview {
        Column(modifier = Modifier.padding(16.dp)) {
            IridiumSectionHeader(title = "Continue reading", badgeText = "3")
        }
    }
}

@ThemePreviews
@Composable
private fun LoadingIndicatorPreview() {
    IridiumPreview {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(16.dp),
        ) {
            IridiumLoadingIndicator(modifier = Modifier.size(48.dp))
        }
    }
}

@ThemePreviews
@Composable
private fun CoverArtPlaceholderPreview() {
    IridiumPreview {
        Box(modifier = Modifier.padding(16.dp)) {
            BookCoverArt(
                coverPath = null,
                contentDescription = null,
                modifier = Modifier.size(width = 120.dp, height = 180.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SchemePickerRowPreview() {
    IridiumPreview {
        Column(modifier = Modifier.padding(16.dp)) {
            SchemePickerRow(
                theme = ThemePreferences(),
                onDynamic = {},
                onScheme = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
