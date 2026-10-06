package com.iridium.core.designsystem

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class IridiumHapticsTest {

    @Test
    fun selectionUsesLightTick() {
        assertEquals(HapticFeedbackType.SegmentTick, IridiumHaptic.Select.type())
        assertEquals(HapticFeedbackType.SegmentTick, IridiumHaptic.Tick.type())
    }

    @Test
    fun tapIsAliasForSelect() {
        assertEquals(HapticFeedbackType.SegmentTick, IridiumHaptic.Tap.type())
        assertEquals(IridiumHaptic.Select.type(), IridiumHaptic.Tap.type())
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
    fun completionAndFailureAreDistinct() {
        assertEquals(HapticFeedbackType.Confirm, IridiumHaptic.Confirm.type())
        assertEquals(HapticFeedbackType.Reject, IridiumHaptic.Reject.type())
    }

    @Test
    fun warningSharesRejectPath() {
        assertEquals(HapticFeedbackType.Reject, IridiumHaptic.Warning.type())
        assertEquals(IridiumHaptic.Reject.type(), IridiumHaptic.Warning.type())
    }

    @Test
    fun primaryActionIsAFirmConfirm() {
        assertEquals(HapticFeedbackType.Confirm, IridiumHaptic.PrimaryAction.type())
    }

    @Test
    fun longPressMapsThrough() {
        assertEquals(HapticFeedbackType.LongPress, IridiumHaptic.LongPress.type())
    }
}
