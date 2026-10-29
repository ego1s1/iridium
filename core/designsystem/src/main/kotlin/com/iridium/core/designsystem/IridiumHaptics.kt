package com.iridium.core.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Semantic haptic events for Iridium, mapped to platform-tuned effects.
 *
 * Provides a clean seam for all tactile feedback:
 * - Tap / Select: chips, tabs, segment controls
 * - ToggleOn / ToggleOff: switches and toggles
 * - Tick: discrete steps, slider detents
 * - FrequentTick: rapid scrubbing (scrubbers, sliders)
 * - Confirm: action completion, save, bookmark
 * - Warning / Reject: destructive actions or validation errors
 * - PrimaryAction: high-impact CTA trigger
 * - LongPress: contextual actions
 */
enum class IridiumHaptic {
    /** Tab, chip, segmented option, FAB, or card selected. */
    Tap,

    /** Semantic alias for Tap. */
    Select,

    /** Switch or toggle moved to on. */
    ToggleOn,

    /** Switch or toggle moved to off. */
    ToggleOff,

    /** Discrete step: slider detent, scrubbed page, frequent tick. */
    Tick,

    /** Frequent steps while scrubbing (pages, percentages). */
    FrequentTick,

    /** An action completed: refresh done, collection created, cache cleared. */
    Confirm,

    /** Warning or destructive action trigger. */
    Warning,

    /** Highest-priority CTA fired: resume FAB, primary action button. */
    PrimaryAction,

    /** An action failed or was rejected. */
    Reject,

    /** Long-press that opens an action (menus, reordering). */
    LongPress,

    /**
     * A word or phrase just became the active text selection. Crisp and
     * slightly textured rather than a soft blip, so the moment the selection
     * snaps to a word is felt without looking at the screen.
     */
    TextSelect,
}

/**
 * Pure mapping from semantic event to platform [HapticFeedbackType].
 * Pure so it stays unit-testable without Android runtime.
 */
fun IridiumHaptic.type(): HapticFeedbackType = when (this) {
    IridiumHaptic.Tap,
    IridiumHaptic.Select -> HapticFeedbackType.SegmentTick
    IridiumHaptic.ToggleOn -> HapticFeedbackType.ToggleOn
    IridiumHaptic.ToggleOff -> HapticFeedbackType.ToggleOff
    IridiumHaptic.Tick -> HapticFeedbackType.SegmentTick
    IridiumHaptic.FrequentTick -> HapticFeedbackType.SegmentFrequentTick
    IridiumHaptic.Confirm -> HapticFeedbackType.Confirm
    IridiumHaptic.Warning -> HapticFeedbackType.Reject
    IridiumHaptic.PrimaryAction -> HapticFeedbackType.Confirm
    IridiumHaptic.Reject -> HapticFeedbackType.Reject
    IridiumHaptic.LongPress -> HapticFeedbackType.LongPress
    // Framework text-selection feedback: the platform's own handle-move cue.
    IridiumHaptic.TextSelect -> HapticFeedbackType.TextHandleMove
}

/** Performs the semantic event on this [HapticFeedback] channel. */
fun HapticFeedback.perform(event: IridiumHaptic) {
    performHapticFeedback(event.type())
}
