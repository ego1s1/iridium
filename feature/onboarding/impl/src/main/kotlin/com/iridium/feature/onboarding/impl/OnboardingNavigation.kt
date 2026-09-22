package com.iridium.feature.onboarding.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.iridium.feature.onboarding.api.OnboardingRoute

fun NavGraphBuilder.onboardingScreen(onOnboardingComplete: () -> Unit) {
    composable<OnboardingRoute> {
        OnboardingRoute(onOnboardingComplete = onOnboardingComplete)
    }
}
