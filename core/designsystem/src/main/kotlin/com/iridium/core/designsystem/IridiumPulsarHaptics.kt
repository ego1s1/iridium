package com.iridium.core.designsystem

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.swmansion.pulsar.Pulsar
import com.swmansion.pulsar.presets.PresetsWrapper
import com.swmansion.pulsar.types.CompatibilityMode

/** Ambient toggle controlling whether semantic haptic feedback is dispatched. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/**
 * Pulsar-backed haptics dispatcher for [IridiumHaptic] events with graceful fallback
 * to [LocalHapticFeedback].
 *
 * Uses Pulsar 1.3.0 (`com.swmansion:pulsar`) to drive OEM-tuned haptic patterns on
 * capable devices (Pixel, Samsung), falling back to framework [LocalHapticFeedback]
 * when:
 * 1. The context is not an [Activity].
 * 2. Device haptic capability is below [CompatibilityMode.LIMITED_SUPPORT].
 * 3. Pulsar instantiation or playback fails.
 *
 * Usage:
 * ```kotlin
 * val haptics = rememberIridiumHaptics()
 * Button(onClick = { haptics(IridiumHaptic.Tap) }) { ... }
 * ```
 */
@Composable
fun rememberIridiumHaptics(): (IridiumHaptic) -> Unit {
    val enabled = LocalHapticsEnabled.current
    if (!enabled) {
        return remember { {} }
    }
    val context = LocalContext.current
    val framework = LocalHapticFeedback.current

    val pulsar = remember(context) {
        val activity = context as? Activity ?: return@remember null
        runCatching { Pulsar(activity) }.getOrNull()
    }

    val pulsarCapable = remember(pulsar) {
        runCatching {
            (pulsar?.hapticSupport() ?: CompatibilityMode.NO_SUPPORT) >=
                CompatibilityMode.LIMITED_SUPPORT
        }.getOrDefault(false)
    }

    return remember(pulsar, framework, pulsarCapable) {
        { event ->
            val played = runCatching {
                if (!pulsarCapable) return@runCatching false
                val presets = pulsar?.getPresets() ?: return@runCatching false
                event.playWith(presets)
                true
            }.getOrDefault(false)
            if (!played) {
                framework.perform(event)
            }
        }
    }
}

/**
 * Dispatches one semantic event to its corresponding Pulsar system preset.
 */
private fun IridiumHaptic.playWith(presets: PresetsWrapper) {
    when (this) {
        IridiumHaptic.Tap,
        IridiumHaptic.Select -> presets.systemSelection()
        IridiumHaptic.ToggleOn -> presets.systemToggleOn()
        IridiumHaptic.ToggleOff -> presets.systemToggleOff()
        IridiumHaptic.Tick -> presets.systemSegmentTick()
        IridiumHaptic.FrequentTick -> presets.systemSegmentFrequentTick()
        IridiumHaptic.Confirm -> presets.systemNotificationSuccess()
        IridiumHaptic.Warning -> presets.systemNotificationWarning()
        IridiumHaptic.PrimaryAction -> presets.systemImpactMedium()
        IridiumHaptic.Reject -> presets.systemNotificationError()
        IridiumHaptic.LongPress -> presets.systemLongPress()
    }
}
