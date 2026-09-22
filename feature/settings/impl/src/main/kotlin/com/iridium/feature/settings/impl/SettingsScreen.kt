package com.iridium.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSectionCard
import com.iridium.core.designsystem.IridiumSettingSwitch
import com.iridium.core.designsystem.LocalNavAnimatedVisibilityScope
import com.iridium.core.designsystem.screenEnter
import com.iridium.core.designsystem.screenExit
import com.iridium.core.designsystem.screenPopEnter
import com.iridium.core.designsystem.screenPopExit
import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen(onBackClick: () -> Unit) {
    composable<SettingsRoute>(
        enterTransition = { screenEnter() },
        exitTransition = { screenExit() },
        popEnterTransition = { screenPopEnter() },
        popExitTransition = { screenPopExit() },
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            SettingsRoute(onBackClick = onBackClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(IridiumIcons.Back, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IridiumSectionCard(title = "Appearance") {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                ButtonGroup(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEach { mode ->
                        ToggleButton(
                            checked = state.theme.mode == mode,
                            onCheckedChange = { onAction(SettingsAction.SetThemeMode(mode)) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                IridiumSettingSwitch(
                    title = "Wallpaper color",
                    subtitle = "Match the system palette (Android 12+)",
                    checked = state.theme.dynamicColor,
                    onCheckedChange = { onAction(SettingsAction.SetDynamicColor(it)) },
                )
                IridiumSettingSwitch(
                    title = "Pure black",
                    subtitle = "True-black surfaces for AMOLED screens",
                    checked = state.theme.amoled,
                    onCheckedChange = { onAction(SettingsAction.SetAmoled(it)) },
                    enabled = state.theme.mode != ThemeMode.LIGHT,
                )
                Spacer(Modifier.height(12.dp))
                Text("Preset", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppColorScheme.entries.forEach { scheme ->
                        SchemeSwatch(
                            scheme = scheme,
                            selected = state.theme.colorScheme == scheme,
                            onClick = { onAction(SettingsAction.SetColorScheme(scheme)) },
                        )
                    }
                }
            }

            IridiumSectionCard(title = "Motion") {
                ButtonGroup(modifier = Modifier.fillMaxWidth()) {
                    ToggleButton(
                        checked = state.motionStyle == MotionStyle.EXPRESSIVE,
                        onCheckedChange = { onAction(SettingsAction.SetMotionStyle(MotionStyle.EXPRESSIVE)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Expressive") }
                    ToggleButton(
                        checked = state.motionStyle == MotionStyle.CALM,
                        onCheckedChange = { onAction(SettingsAction.SetMotionStyle(MotionStyle.CALM)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Calm") }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Calm uses plain fades; Expressive uses spring physics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IridiumSectionCard(title = "Reading") {
                IridiumSettingSwitch(
                    title = "Keep screen on",
                    checked = state.reader.keepScreenOn,
                    onCheckedChange = { onAction(SettingsAction.SetKeepScreenOn(it)) },
                )
                IridiumSettingSwitch(
                    title = "Show page counter",
                    checked = state.reader.showPageCounter,
                    onCheckedChange = { onAction(SettingsAction.SetShowPageCounter(it)) },
                )
                IridiumSettingSwitch(
                    title = "Volume keys turn pages",
                    checked = state.reader.volumeKeys,
                    onCheckedChange = { onAction(SettingsAction.SetVolumeKeys(it)) },
                )
            }
        }
    }
}

@Composable
private fun SchemeSwatch(
    scheme: AppColorScheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = when (scheme) {
        AppColorScheme.IRIDIUM -> Color(0xFF6445A8)
        AppColorScheme.OCEAN -> Color(0xFF00639B)
        AppColorScheme.FOREST -> Color(0xFF2E7D32)
        AppColorScheme.SUNSET -> Color(0xFFB14A00)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Icon(
                imageVector = IridiumIcons.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
