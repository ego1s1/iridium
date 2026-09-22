package com.iridium.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumMotion
import com.iridium.core.designsystem.LocalExpressiveMotionEnabled

/**
 * The floating navigator: Library, History and Settings destinations with a
 * selected pill that carries icon + label while idle tabs are bare icons.
 * Tab state lives in [MainScreen]; every tap delegates out.
 */
@Composable
internal fun MainNavigator(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
        modifier = modifier.testTag(MainTestTags.Navigator),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(4.dp).selectableGroup(),
        ) {
            NavDestination(
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) },
                icon = IridiumIcons.MenuBook,
                label = "Library",
                contentDescription = "Library tab",
                testTag = MainTestTags.LibraryTab,
            )
            NavDestination(
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) },
                icon = IridiumIcons.History,
                label = "History",
                contentDescription = "History tab",
                testTag = MainTestTags.HistoryTab,
            )
            NavDestination(
                selected = selectedTab == 2,
                onClick = { onSelectTab(2) },
                icon = IridiumIcons.Settings,
                label = "Settings",
                contentDescription = "Settings tab",
                testTag = MainTestTags.SettingsTab,
            )
        }
    }
}

/** Standalone resume action: a 56dp circle riding beside the navigator. */
@Composable
internal fun ResumeButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
        modifier = modifier.size(56.dp).testTag(MainTestTags.ResumeAction),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = IridiumIcons.Play,
                contentDescription = "Resume $title",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun NavDestination(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Tab)
            .semantics { this.selected = selected }
            .testTag(testTag)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(container, CircleShape)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (selected) null else contentDescription,
                tint = content,
                modifier = Modifier.size(20.dp),
            )
            AnimatedVisibility(
                visible = selected,
                enter = if (expressiveMotion) {
                    fadeIn(animationSpec = IridiumMotion.defaultEffectsSpec()) +
                        expandHorizontally(animationSpec = IridiumMotion.defaultSpatialSpec())
                } else {
                    fadeIn()
                },
                exit = if (expressiveMotion) {
                    fadeOut(animationSpec = IridiumMotion.defaultEffectsSpec()) +
                        shrinkHorizontally(animationSpec = IridiumMotion.defaultSpatialSpec())
                } else {
                    fadeOut()
                },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}
