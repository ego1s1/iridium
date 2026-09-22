package com.iridium.feature.onboarding.impl

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.iridium.core.designsystem.IridiumEnter
import com.iridium.core.designsystem.IridiumEnterKind
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumMotion
import com.iridium.core.designsystem.IridiumSectionCard
import com.iridium.core.designsystem.IridiumSettingSwitch
import com.iridium.core.designsystem.SchemePickerRow
import com.iridium.core.designsystem.topSheet
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences
import kotlinx.coroutines.delay

@Composable
internal fun OnboardingRoute(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) {
            viewModel.onAction(OnboardingAction.FolderPickerDismissed)
        } else {
            val granted = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.isSuccess
            if (granted) {
                viewModel.onAction(OnboardingAction.FolderSelected(uri))
            } else {
                viewModel.onAction(OnboardingAction.FolderPickerDismissed)
            }
        }
    }
    OnboardingScreen(
        uiState = uiState,
        onPickFolder = { folderLauncher.launch(null) },
        onAction = viewModel::onAction,
        onOnboardingComplete = onOnboardingComplete,
        modifier = modifier,
    )
}

@Composable
@Suppress("UnusedContentLambdaTargetStateParameter")
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onPickFolder: () -> Unit,
    onAction: (OnboardingAction) -> Unit,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        val stepEnter = IridiumEnter.enter(IridiumEnterKind.FADE_THROUGH)
        val stepExit = IridiumEnter.exit(IridiumEnterKind.FADE_THROUGH)
        val stepKey = when (uiState) {
            OnboardingUiState.Welcome -> 0
            is OnboardingUiState.Folder -> 1
            is OnboardingUiState.Appearance -> 2
        }
        AnimatedContent(
            targetState = stepKey,
            transitionSpec = { stepEnter togetherWith stepExit },
            label = "onboardingStep",
        ) { _ ->
            when (uiState) {
                OnboardingUiState.Welcome -> WelcomeContent(
                    onGetStarted = { onAction(OnboardingAction.GetStarted) },
                    onSkip = {
                        onAction(OnboardingAction.Skip)
                        onOnboardingComplete()
                    },
                )
                is OnboardingUiState.Folder -> WizardStep(
                    stepIndex = 0,
                    totalSteps = 2,
                    title = stringResource(R.string.onboarding_folder_title),
                    body = stringResource(R.string.onboarding_folder_body),
                    onBack = { onAction(OnboardingAction.BackStep) },
                    onSkip = {
                        onAction(OnboardingAction.Skip)
                        onOnboardingComplete()
                    },
                    onContinue = {
                        onAction(OnboardingAction.Skip)
                        onOnboardingComplete()
                    },
                    continueLabel = stringResource(R.string.onboarding_continue_without_linking),
                    continueCaption = "",
                ) {
                    FolderOptions(
                        onPickFolder = onPickFolder,
                        pickerHintVisible = uiState.pickerHintVisible,
                    )
                }
                is OnboardingUiState.Appearance -> WizardStep(
                    stepIndex = 1,
                    totalSteps = 2,
                    title = stringResource(R.string.onboarding_appearance_title),
                    body = stringResource(R.string.onboarding_appearance_body),
                    onBack = { onAction(OnboardingAction.BackStep) },
                    onSkip = {
                        onAction(OnboardingAction.Skip)
                        onOnboardingComplete()
                    },
                    onContinue = {
                        onAction(OnboardingAction.Finish)
                        onOnboardingComplete()
                    },
                    continueLabel = stringResource(R.string.onboarding_continue),
                    continueCaption = stringResource(R.string.onboarding_appearance_caption),
                ) {
                    AppearanceOptions(theme = uiState.theme, onAction = onAction)
                }
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(WELCOME_STEPS) {
            delay(110)
            step++
        }
    }
    fun visibleAt(index: Int) = step > index
    Column(modifier = modifier.fillMaxSize()) {
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
                Box(contentAlignment = Alignment.Center) {
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
                        modifier = Modifier.fillMaxWidth(),
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
private fun FolderOptions(
    onPickFolder: () -> Unit,
    pickerHintVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onPickFolder,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.onboarding_pick_folder))
        }
        AnimatedVisibility(
            visible = pickerHintVisible,
            enter = IridiumEnter.enter(IridiumEnterKind.FADE),
            exit = IridiumEnter.exit(IridiumEnterKind.FADE),
        ) {
            Text(
                text = stringResource(R.string.onboarding_folder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = stringResource(R.string.onboarding_folder_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WizardStep(
    stepIndex: Int,
    totalSteps: Int,
    title: String,
    body: String,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: (() -> Unit)? = null,
    continueLabel: String,
    continueCaption: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(IridiumIcons.Back, contentDescription = stringResource(R.string.onboarding_back))
            }
            Text(
                text = stringResource(R.string.onboarding_step, stepIndex + 1, totalSteps),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            repeat(totalSteps) { index ->
                key(index) {
                    val target = if (index <= stepIndex) 1f else 0f
                    // Quiet tween, not a spring: a bar bouncing past 1.0 reads broken.
                    val fill by animateFloatAsState(
                        targetValue = target,
                        animationSpec = IridiumMotion.calmFadeSpec(),
                        label = "stepSegment",
                    )
                    LinearProgressIndicator(
                        progress = { fill },
                        modifier = Modifier.weight(1f).height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(text = title, style = IridiumEmphasized.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            IridiumSectionCard { content() }
            Spacer(Modifier.height(16.dp))
        }
        if (onContinue != null) {
            Surface(
                shape = MaterialTheme.shapes.topSheet,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                ) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text(continueLabel)
                    }
                    if (continueCaption.isNotBlank()) {
                        Text(
                            text = continueCaption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceOptions(
    theme: ThemePreferences,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.onboarding_theme), style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
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
        )
        IridiumSettingSwitch(
            title = stringResource(R.string.onboarding_amoled_title),
            subtitle = stringResource(R.string.onboarding_amoled_subtitle),
            checked = theme.amoled,
            onCheckedChange = { onAction(OnboardingAction.SetAmoled(it)) },
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
    LaunchedEffect(Unit) {
        delay(HERO_MORPH_DELAY_MS)
        morphed = true
    }
    val corner by animateDpAsState(
        targetValue = if (morphed) 64.dp else 28.dp,
        animationSpec = IridiumMotion.heroSpec(),
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
