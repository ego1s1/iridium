package com.iridium.feature.settings.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.iridium.core.data.DriveSyncManager
import com.iridium.core.designsystem.IridiumAlertDialog
import com.iridium.core.designsystem.rememberLinkedFolderPicker
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSectionCard
import com.iridium.core.designsystem.IridiumSettingRow
import com.iridium.core.designsystem.IridiumSettingSwitch
import com.iridium.core.designsystem.IridiumSliderRow
import com.iridium.core.designsystem.IridiumTonalButton
import com.iridium.core.designsystem.LocalAppFonts
import com.iridium.core.designsystem.SchemePickerRow
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.LibrarySortOrder
import com.iridium.core.model.MotionStyle
import kotlinx.coroutines.launch
import com.iridium.core.model.ThemeMode

/** Public tab content for the main viewport. */
@Composable
internal fun AppearanceSection(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
        IridiumSectionCard(title = stringResource(R.string.settings_card_appearance)) {
            OptionLabel(stringResource(R.string.settings_theme))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ThemeChoice(
                    selected = state.theme.mode == ThemeMode.SYSTEM,
                    onClick = { onAction(SettingsAction.SetThemeMode(ThemeMode.SYSTEM)) },
                    icon = IridiumIcons.Contrast,
                    label = stringResource(R.string.settings_theme_system),
                )
                ThemeChoice(
                    selected = state.theme.mode == ThemeMode.LIGHT,
                    onClick = { onAction(SettingsAction.SetThemeMode(ThemeMode.LIGHT)) },
                    icon = IridiumIcons.LightMode,
                    label = stringResource(R.string.settings_theme_light),
                )
                ThemeChoice(
                    selected = state.theme.mode == ThemeMode.DARK,
                    onClick = { onAction(SettingsAction.SetThemeMode(ThemeMode.DARK)) },
                    icon = IridiumIcons.DarkMode,
                    label = stringResource(R.string.settings_theme_dark),
                )
            }
            Spacer(Modifier.height(8.dp))
            IridiumSettingSwitch(
                title = stringResource(R.string.settings_wallpaper_title),
                subtitle = stringResource(R.string.settings_wallpaper_subtitle),
                checked = state.theme.dynamicColor,
                onCheckedChange = { onAction(SettingsAction.SetDynamicColor(it)) },
            )
            IridiumSettingSwitch(
                title = stringResource(R.string.settings_haptics_title),
                subtitle = stringResource(R.string.settings_haptics_subtitle),
                checked = state.theme.hapticsEnabled,
                onCheckedChange = { onAction(SettingsAction.SetHapticsEnabled(it)) },
                icon = IridiumIcons.Vibration,
            )
            IridiumSettingSwitch(
                title = stringResource(R.string.settings_amoled_title),
                subtitle = stringResource(R.string.settings_amoled_subtitle),
                checked = state.theme.amoled,
                onCheckedChange = { onAction(SettingsAction.SetAmoled(it)) },
                // Pure black only reads on a dark canvas: explicit LIGHT
                // never allows it; SYSTEM follows the system theme.
                enabled = state.theme.mode == ThemeMode.DARK ||
                    (state.theme.mode == ThemeMode.SYSTEM && isSystemInDarkTheme()),
            )
            Spacer(Modifier.height(8.dp))
            OptionLabel(stringResource(R.string.settings_colors))
            SchemePickerRow(
                theme = state.theme,
                onDynamic = { onAction(SettingsAction.SetDynamicColor(true)) },
                onScheme = { onAction(SettingsAction.SetColorScheme(it)) },
            )
        }
        IridiumSectionCard(title = stringResource(R.string.settings_card_motion)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ThemeChoice(
                    selected = state.motionStyle == MotionStyle.EXPRESSIVE,
                    onClick = {
                        onAction(SettingsAction.SetMotionStyle(MotionStyle.EXPRESSIVE))
                    },
                    icon = IridiumIcons.Animation,
                    label = stringResource(R.string.settings_motion_expressive),
                )
                ThemeChoice(
                    selected = state.motionStyle == MotionStyle.CALM,
                    onClick = {
                        onAction(SettingsAction.SetMotionStyle(MotionStyle.CALM))
                    },
                    icon = IridiumIcons.Spa,
                    label = stringResource(R.string.settings_motion_calm),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_motion_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
}

/** Small section label inside a card, heading-marked for accessibility. */
@Composable
internal fun OptionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = IridiumEmphasized.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * One pill in a single-choice row, with an optional leading icon. Plain
 * [ToggleButton] with default shapes: custom asymmetric container shapes
 * break tap hit-testing under Robolectric, so the morphing group stays out
 * until the design-system component is test-safe.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RowScope.ThemeChoice(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector? = null,
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { checked -> if (checked != selected) onClick() },
        modifier = Modifier.weight(1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}
