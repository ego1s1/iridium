package com.iridium.feature.onboarding.impl

/**
 * Semantics tags for the onboarding flow. Kept in one object so UI tests
 * and screenshot harnesses share stable node identities.
 */
object OnboardingTestTags {
    const val Welcome = "onboardingWelcome"
    const val Hero = "onboardingHero"
    const val Cta = "onboardingGetStarted"
    const val StepIndicators = "onboardingStepIndicators"
    const val StepSegmentPrefix = "onboardingStepSegment"
    const val AccessGrant = "onboardingAccessGrant"
    const val ReadingTextSize = "onboardingReadingTextSize"
    const val ReadingLineSpacing = "onboardingReadingLineSpacing"
    const val ReadingTheme = "onboardingReadingTheme"
    const val ContinueButton = "onboardingContinue"
    const val AppearanceThemeOptions = "onboardingThemeOptions"
    const val AppearanceSchemeOptions = "onboardingSchemeOptions"
    const val AppearanceAmoled = "onboardingAmoledSwitch"

    fun stepSegment(index: Int): String = "${StepSegmentPrefix}_$index"
}
