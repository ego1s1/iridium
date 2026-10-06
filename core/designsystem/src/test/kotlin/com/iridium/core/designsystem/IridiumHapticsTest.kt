package com.iridium.core.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class IridiumHapticsTest {

    @Test
    fun tapAndSelectMapToSegmentTick() {
        assertEquals(HapticFeedbackType.SegmentTick, IridiumHaptic.Tap.type())
        assertEquals(HapticFeedbackType.SegmentTick, IridiumHaptic.Select.type())
        assertEquals(HapticFeedbackType.SegmentTick, IridiumHaptic.Tick.type())
    }

    @Test
    fun togglesHaveDistinctOnOff() {
        assertEquals(HapticFeedbackType.ToggleOn, IridiumHaptic.ToggleOn.type())
        assertEquals(HapticFeedbackType.ToggleOff, IridiumHaptic.ToggleOff.type())
    }

    @Test
    fun scrubbingUsesFrequentTick() {
        assertEquals(HapticFeedbackType.SegmentFrequentTick, IridiumHaptic.FrequentTick.type())
    }

    @Test
    fun confirmAndWarningAreDistinct() {
        assertEquals(HapticFeedbackType.Confirm, IridiumHaptic.Confirm.type())
        assertEquals(HapticFeedbackType.Reject, IridiumHaptic.Warning.type())
        assertEquals(HapticFeedbackType.Reject, IridiumHaptic.Reject.type())
    }

    @Test
    fun primaryActionMapsToConfirm() {
        assertEquals(HapticFeedbackType.Confirm, IridiumHaptic.PrimaryAction.type())
    }

    @Test
    fun longPressMapsThrough() {
        assertEquals(HapticFeedbackType.LongPress, IridiumHaptic.LongPress.type())
    }
}
