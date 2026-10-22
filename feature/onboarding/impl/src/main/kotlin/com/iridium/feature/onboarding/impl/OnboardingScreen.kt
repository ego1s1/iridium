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
internal fun OnboardingRoute(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        finished = viewModel.finished,
        onOnboardingComplete = onOnboardingComplete,
        modifier = modifier,
    )
}

@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    finished: Flow<Unit> = emptyFlow(),
) {
    // Navigation fires only after the completion flag commits (the ViewModel
    // emits finished post-write, so a relaunch can never replay onboarding).
    LaunchedEffect(finished) {
        finished.collect { onOnboardingComplete() }
    }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        val stepEnter = IridiumEnter.enter(IridiumEnterKind.FADE_THROUGH)
        val stepExit = IridiumEnter.exit(IridiumEnterKind.FADE_THROUGH)
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { stepEnter togetherWith stepExit },
            label = "onboardingStep",
        ) { target ->
            when (target) {
                OnboardingUiState.Welcome -> WelcomeContent(
                    onGetStarted = { onAction(OnboardingAction.GetStarted) },
                    onSkip = {
                        onAction(OnboardingAction.Skip)
                    },
                )
                is OnboardingUiState.Access -> WizardStep(
                    stepIndex = 0,
                    totalSteps = 3,
                    title = stringResource(R.string.onboarding_access_title),
                    body = stringResource(R.string.onboarding_access_body),
                    onBack = { onAction(OnboardingAction.BackStep) },
                    onSkip = {
                        onAction(OnboardingAction.Skip)
                    },
                    onContinue = { onAction(OnboardingAction.Advance) },
                    continueLabel = stringResource(R.string.onboarding_continue),
                    continueCaption = "",
                ) {
                    AccessOptions(
                        folderCount = target.folderCount,
                        onAddFolder = { onAction(OnboardingAction.AddLinkedFolder(it)) },
                    )
                }
                is OnboardingUiState.Reading -> WizardStep(
                    stepIndex = 1,
                    totalSteps = 3,
                    title = stringResource(R.string.onboarding_reading_title),
                    body = stringResource(R.string.onboarding_reading_body),
                    onBack = { onAction(OnboardingAction.BackStep) },
                    onSkip = {
                        onAction(OnboardingAction.Skip)
                    },
                    onContinue = { onAction(OnboardingAction.Advance) },
                    continueLabel = stringResource(R.string.onboarding_continue),
                    continueCaption = "",
                ) {
                    ReadingOptions(prefs = target.prefs, onAction = onAction)
                }
                is OnboardingUiState.Appearance -> WizardStep(
                    stepIndex = 2,
                    totalSteps = 3,
                    title = stringResource(R.string.onboarding_appearance_title),
                    body = stringResource(R.string.onboarding_appearance_body),
                    onBack = { onAction(OnboardingAction.BackStep) },
                    onSkip = {
                        onAction(OnboardingAction.Skip)
                    },
                    onContinue = {
                        onAction(OnboardingAction.Finish)
                    },
                    continueLabel = stringResource(R.string.onboarding_continue),
                    continueCaption = stringResource(R.string.onboarding_appearance_caption),
                ) {
                    AppearanceOptions(theme = target.theme, onAction = onAction)
                }
            }
        }
    }
}

