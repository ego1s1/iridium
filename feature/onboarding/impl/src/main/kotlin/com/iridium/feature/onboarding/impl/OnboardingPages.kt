package com.iridium.feature.onboarding.impl

import android.os.Environment
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.rememberLinkedFolderPicker
import kotlin.math.roundToInt
import com.iridium.core.designsystem.IridiumEnter
import com.iridium.core.designsystem.IridiumEnterKind
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumMotion
import com.iridium.core.designsystem.IridiumSectionCard
import com.iridium.core.designsystem.IridiumSettingSwitch
import com.iridium.core.designsystem.IridiumThemeSwatch
import com.iridium.core.designsystem.LocalExpressiveMotionEnabled
import com.iridium.core.designsystem.SchemePickerRow
import com.iridium.core.designsystem.topSheet
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.pageBackgroundArgb
import com.iridium.core.model.isLightScheme
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences
import kotlinx.coroutines.delay

@Composable
internal fun WelcomeContent(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val staggerMotion = LocalExpressiveMotionEnabled.current
    var step by remember(staggerMotion) {
        mutableIntStateOf(if (!staggerMotion) WELCOME_STEPS else 0)
    }
    LaunchedEffect(staggerMotion) {
        if (!staggerMotion) {
            step = WELCOME_STEPS
            return@LaunchedEffect
        }
        repeat(WELCOME_STEPS) {
            delay(110)
            step++
        }
    }
    fun visibleAt(index: Int) = step > index
    Column(modifier = modifier.fillMaxSize().testTag(OnboardingTestTags.Welcome)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_brand),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
            )
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            AnimatedVisibility(
                visible = visibleAt(0),
                enter = IridiumEnter.enter(IridiumEnterKind.FAB),
                exit = IridiumEnter.exit(IridiumEnterKind.FAB),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.testTag(OnboardingTestTags.Hero),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .size(88.dp)
                            .offset((-58).dp, 44.dp)
                            .graphicsLayer { rotationZ = -14f },
                    ) {}
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(56.dp).offset(66.dp, (-52).dp),
                    ) {}
                    MorphingHero(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = IridiumIcons.MenuBook,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            AnimatedVisibility(
                visible = visibleAt(1),
                enter = IridiumEnter.enter(IridiumEnterKind.RISE),
                exit = IridiumEnter.exit(IridiumEnterKind.RISE),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.onboarding_eyebrow),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.onboarding_hero_prefix))
                            withStyle(
                                SpanStyle(
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                append(stringResource(R.string.onboarding_hero_accent))
                            }
                            append(stringResource(R.string.onboarding_hero_suffix))
                        },
                        style = IridiumEmphasized.displaySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            AnimatedVisibility(
                visible = visibleAt(2),
                enter = IridiumEnter.enter(IridiumEnterKind.RISE),
                exit = IridiumEnter.exit(IridiumEnterKind.RISE),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.Cta),
                    ) {
                        Text(stringResource(R.string.onboarding_get_started))
                    }
                    Text(
                        text = stringResource(R.string.onboarding_get_started_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

private const val WELCOME_STEPS = 3

@Composable
internal fun AccessOptions(
    folderCount: Int,
    onAddFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val launchPicker = rememberLinkedFolderPicker(onFolderPicked = onAddFolder)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = launchPicker,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag(OnboardingTestTags.AccessGrant),
        ) {
            Text(stringResource(R.string.onboarding_access_grant))
        }
        Text(
            text = if (folderCount > 0) {
                pluralStringResource(
                    R.plurals.onboarding_access_granted,
                    folderCount,
                    folderCount,
                )
            } else {
                stringResource(R.string.onboarding_access_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (folderCount > 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.onboarding_access_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun ReadingOptions(
    prefs: ReaderPreferences,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.onboarding_reading_text_size), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${(prefs.fontScale * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            // Quantized tenths: repeated float +/-0.1f accumulates error,
            // and the buttons park at the clamps instead of overshooting.
            IconButton(
                onClick = {
                    val stepped = ((prefs.fontScale * 10).roundToInt() - 1) / 10f
                    onAction(OnboardingAction.SetFontScale(stepped))
                },
                enabled = prefs.fontScale > 0.5f,
            ) {
                Icon(IridiumIcons.Remove, contentDescription = stringResource(R.string.onboarding_text_smaller))
            }
            IconButton(
                onClick = {
                    val stepped = ((prefs.fontScale * 10).roundToInt() + 1) / 10f
                    onAction(OnboardingAction.SetFontScale(stepped))
                },
                enabled = prefs.fontScale < 3f,
            ) {
                Icon(IridiumIcons.Add, contentDescription = stringResource(R.string.onboarding_text_larger))
            }
        }
        Text(stringResource(R.string.onboarding_reading_line_spacing), style = MaterialTheme.typography.titleMedium)
        // Draft thumb while dragging: prefs-backed value lags the gesture, so
        // rendering it directly makes the thumb snap back mid-scrub.
        var lineHeightDraft by remember(prefs.lineHeight) { mutableFloatStateOf(prefs.lineHeight) }
        var lineHeightDragging by remember { mutableStateOf(false) }
        LaunchedEffect(prefs.lineHeight) {
            if (!lineHeightDragging) lineHeightDraft = prefs.lineHeight
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${(lineHeightDraft * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Slider(
                value = lineHeightDraft,
                onValueChange = {
                    lineHeightDragging = true
                    lineHeightDraft = it
                    onAction(OnboardingAction.SetLineHeight(it))
                },
                onValueChangeFinished = { lineHeightDragging = false },
                valueRange = 1f..2.5f,
                modifier = Modifier.weight(2f),
            )
        }
        Text(stringResource(R.string.onboarding_reading_colors), style = MaterialTheme.typography.titleMedium)
        ReaderThemeSwatches(
            selected = prefs.theme,
            onSelect = { onAction(OnboardingAction.SetReaderTheme(it)) },
        )
    }
}

@Composable
internal fun ReaderThemeSwatches(
    selected: ColorSchemeChoice,
    onSelect: (ColorSchemeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        ColorSchemeChoice.entries.forEach { theme ->
            IridiumThemeSwatch(
                background = Color(theme.pageBackgroundArgb()),
                contentColor = if (theme.isLightScheme()) Color.Black else Color.White,
                selected = selected == theme,
                onSelect = { onSelect(theme) },
            )
        }
    }
}

@Composable
internal fun AppearanceOptions(
    theme: ThemePreferences,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.onboarding_theme), style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.AppearanceThemeOptions),
        ) {
            SegmentedButton(
                selected = theme.mode == ThemeMode.SYSTEM,
                onClick = { onAction(OnboardingAction.SetThemeMode(ThemeMode.SYSTEM)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                label = { Text(stringResource(R.string.onboarding_theme_system)) },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.LIGHT,
                onClick = { onAction(OnboardingAction.SetThemeMode(ThemeMode.LIGHT)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text(stringResource(R.string.onboarding_theme_light)) },
            )
            SegmentedButton(
                selected = theme.mode == ThemeMode.DARK,
                onClick = { onAction(OnboardingAction.SetThemeMode(ThemeMode.DARK)) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text(stringResource(R.string.onboarding_theme_dark)) },
            )
        }
        Text(stringResource(R.string.onboarding_colors), style = MaterialTheme.typography.titleMedium)
        SchemePickerRow(
            theme = theme,
            onDynamic = { onAction(OnboardingAction.SetDynamicColor(true)) },
            onScheme = { onAction(OnboardingAction.SetColorScheme(it)) },
            modifier = Modifier.testTag(OnboardingTestTags.AppearanceSchemeOptions),
        )
        IridiumSettingSwitch(
            title = stringResource(R.string.onboarding_amoled_title),
            subtitle = stringResource(R.string.onboarding_amoled_subtitle),
            checked = theme.amoled,
            onCheckedChange = { onAction(OnboardingAction.SetAmoled(it)) },
            modifier = Modifier.testTag(OnboardingTestTags.AppearanceAmoled),
        )
    }
}

@Composable
private fun MorphingHero(
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    var morphed by remember { mutableStateOf(false) }
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    LaunchedEffect(expressiveMotion) {
        if (!expressiveMotion) {
            morphed = true
            return@LaunchedEffect
        }
        delay(HERO_MORPH_DELAY_MS)
        morphed = true
    }
    val corner by animateDpAsState(
        targetValue = if (morphed) 64.dp else 28.dp,
        animationSpec = if (expressiveMotion) {
            IridiumMotion.heroSpring()
        } else {
            IridiumMotion.calmFadeSpec()
        },
        label = "heroMorph",
    )
    Surface(
        shape = RoundedCornerShape(corner),
        color = containerColor,
        modifier = modifier.size(128.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

private const val HERO_MORPH_DELAY_MS = 350L
