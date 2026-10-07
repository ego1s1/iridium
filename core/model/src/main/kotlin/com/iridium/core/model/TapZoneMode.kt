package com.iridium.core.model

/**
 * Tap-zone layout for page turns inside the reading surface.
 * DISABLED routes every tap to MENU (chrome toggle only).
 */
enum class TapZoneMode {
    DEFAULT,
    L_SHAPE,
    KINDLISH,
    EDGE,
    RIGHT_AND_LEFT,
    DISABLED,
}
