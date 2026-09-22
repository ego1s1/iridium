package com.iridium.core.datastore

import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReadingFlow
import com.iridium.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory stand-in. Phase 2 replaces with Preferences DataStore. */
@Singleton
class IridiumPreferences @Inject constructor() {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: Flow<ThemeMode> = _themeMode

    private val _motionStyle = MutableStateFlow(MotionStyle.EXPRESSIVE)
    val motionStyle: Flow<MotionStyle> = _motionStyle

    private val _readingFlow = MutableStateFlow(ReadingFlow.AUTO)
    val readingFlow: Flow<ReadingFlow> = _readingFlow

    private val _fontScale = MutableStateFlow(1f)
    val fontScale: Flow<Float> = _fontScale
}
