package com.iridium.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.LibraryDisplayMode
import com.iridium.core.model.LibraryFilter
import com.iridium.core.model.LibrarySortOrder
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ReadingFlow
import com.iridium.core.model.TextAlign
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStorePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : IridiumPreferencesDataSource {

    override val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    override val readerPreferences: Flow<ReaderPreferences> =
        dataStore.data.map { prefs ->
            ReaderPreferences(
                flow = prefs[READING_FLOW]?.let {
                    runCatching { ReadingFlow.valueOf(it) }.getOrDefault(ReadingFlow.AUTO)
                } ?: ReadingFlow.AUTO,
                fontScale = prefs[FONT_SCALE] ?: 1f,
                textAlign = prefs[TEXT_ALIGN]?.let {
                    runCatching { TextAlign.valueOf(it) }.getOrDefault(TextAlign.ORIGINAL)
                } ?: TextAlign.ORIGINAL,
                theme = prefs[READER_THEME]?.let {
                    runCatching { ColorSchemeChoice.valueOf(it) }.getOrDefault(ColorSchemeChoice.SEPIA)
                } ?: ColorSchemeChoice.SEPIA,
                brightness = prefs[BRIGHTNESS] ?: -1f,
                keepScreenOn = prefs[KEEP_SCREEN_ON] ?: true,
                showPageCounter = prefs[SHOW_PAGE_COUNTER] ?: true,
                volumeKeys = prefs[VOLUME_KEYS] ?: false,
                volumeKeysInverted = prefs[VOLUME_KEYS_INVERTED] ?: false,
                // Single-switch era: legacy four-way invert maps to mirrored
                // taps when it flipped the horizontal axis; legacy keys are
                // dropped on the next write below.
                invertTaps = prefs[TAP_INVERT] ?: prefs[TAP_ZONE_INVERT].let { legacy ->
                    legacy == LEGACY_INVERT_HORIZONTAL || legacy == LEGACY_INVERT_BOTH
                },
                nightLight = prefs[NIGHT_LIGHT] ?: false,
                nightLightIntensity = prefs[NIGHT_LIGHT_INTENSITY] ?: 0.25f,
                pageMargins = prefs[PAGE_MARGINS] ?: 1f,
                lineHeight = prefs[LINE_HEIGHT] ?: 1.4f,
            )
        }

    override suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        val updated = transform(readerPreferences.first())
        dataStore.edit {
            it[READING_FLOW] = updated.flow.name
            it[FONT_SCALE] = updated.fontScale
            it[TEXT_ALIGN] = updated.textAlign.name
            it[READER_THEME] = updated.theme.name
            it[BRIGHTNESS] = updated.brightness
            it[KEEP_SCREEN_ON] = updated.keepScreenOn
            it[SHOW_PAGE_COUNTER] = updated.showPageCounter
            it[VOLUME_KEYS] = updated.volumeKeys
            it[VOLUME_KEYS_INVERTED] = updated.volumeKeysInverted
            it[TAP_INVERT] = updated.invertTaps
            it.remove(TAP_ZONE_MODE)
            it.remove(TAP_ZONE_INVERT)
            it[NIGHT_LIGHT] = updated.nightLight
            it[NIGHT_LIGHT_INTENSITY] = updated.nightLightIntensity
            it[PAGE_MARGINS] = updated.pageMargins
            it[LINE_HEIGHT] = updated.lineHeight
        }
    }

    override val themePreferences: Flow<ThemePreferences> =
        dataStore.data.map { prefs ->
            ThemePreferences(
                mode = prefs[THEME_MODE]?.let {
                    runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
                } ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
                colorScheme = prefs[APP_COLOR_SCHEME]?.let {
                    runCatching { AppColorScheme.valueOf(it) }
                        .getOrDefault(AppColorScheme.IRIDIUM)
                } ?: AppColorScheme.IRIDIUM,
                amoled = prefs[AMOLED] ?: false,
                hapticsEnabled = prefs[HAPTICS_ENABLED] ?: true,
            )
        }

    override suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        val updated = transform(themePreferences.first())
        dataStore.edit {
            it[THEME_MODE] = updated.mode.name
            it[DYNAMIC_COLOR] = updated.dynamicColor
            it[APP_COLOR_SCHEME] = updated.colorScheme.name
            it[AMOLED] = updated.amoled
            it[HAPTICS_ENABLED] = updated.hapticsEnabled
        }
    }

    override val motionStyle: Flow<MotionStyle> =
        dataStore.data.map { prefs ->
            prefs[MOTION_STYLE]?.let {
                runCatching { MotionStyle.valueOf(it) }.getOrDefault(MotionStyle.EXPRESSIVE)
            } ?: MotionStyle.EXPRESSIVE
        }

    override suspend fun updateMotionStyle(style: MotionStyle) {
        dataStore.edit { it[MOTION_STYLE] = style.name }
    }

    override val libraryDisplay: Flow<LibraryDisplay> =
        dataStore.data.map { prefs ->
            LibraryDisplay(
                sortOrder = prefs[LIBRARY_SORT]?.let {
                    runCatching { LibrarySortOrder.valueOf(it) }
                        .getOrDefault(LibrarySortOrder.RECENTLY_ADDED)
                } ?: LibrarySortOrder.RECENTLY_ADDED,
                filter = prefs[LIBRARY_FILTER]?.let {
                    runCatching { LibraryFilter.valueOf(it) }.getOrDefault(LibraryFilter.ALL)
                } ?: LibraryFilter.ALL,
                hideErrors = prefs[LIBRARY_HIDE_ERRORS] ?: false,
                displayMode = prefs[LIBRARY_DISPLAY_MODE]?.let {
                    runCatching { LibraryDisplayMode.valueOf(it) }
                        .getOrDefault(LibraryDisplayMode.COMPACT)
                } ?: LibraryDisplayMode.COMPACT,
                gridColumns = prefs[LIBRARY_GRID_COLUMNS] ?: 0,
            )
        }

    override suspend fun updateLibraryDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        val updated = transform(libraryDisplay.first())
        dataStore.edit {
            it[LIBRARY_SORT] = updated.sortOrder.name
            it[LIBRARY_FILTER] = updated.filter.name
            it[LIBRARY_HIDE_ERRORS] = updated.hideErrors
            it[LIBRARY_DISPLAY_MODE] = updated.displayMode.name
            it[LIBRARY_GRID_COLUMNS] = updated.gridColumns
        }
    }

    override val crashReportingEnabled: Flow<Boolean> =
        dataStore.data.map { it[CRASH_REPORTING_ENABLED] ?: false }

    override val crashReportingAsked: Flow<Boolean> =
        dataStore.data.map { it[CRASH_REPORTING_ASKED] ?: false }

    override val linkedFolders: Flow<Set<String>> =
        dataStore.data.map { it[LINKED_FOLDERS] ?: emptySet() }

    override suspend fun addLinkedFolder(uri: String) {
        dataStore.edit { it[LINKED_FOLDERS] = (it[LINKED_FOLDERS] ?: emptySet()) + uri }
    }

    override suspend fun removeLinkedFolder(uri: String) {
        dataStore.edit { it[LINKED_FOLDERS] = (it[LINKED_FOLDERS] ?: emptySet()) - uri }
    }

    override suspend fun setCrashReporting(enabled: Boolean) {
        dataStore.edit {
            it[CRASH_REPORTING_ENABLED] = enabled
            it[CRASH_REPORTING_ASKED] = true
        }
    }

    override suspend fun setCrashReportingAsked(asked: Boolean) {
        dataStore.edit { it[CRASH_REPORTING_ASKED] = asked }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
        val CRASH_REPORTING_ASKED = booleanPreferencesKey("crash_reporting_asked")
        val READING_FLOW = stringPreferencesKey("reading_flow")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val TEXT_ALIGN = stringPreferencesKey("text_align")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_PAGE_COUNTER = booleanPreferencesKey("show_page_counter")
        val VOLUME_KEYS = booleanPreferencesKey("volume_keys")
        val VOLUME_KEYS_INVERTED = booleanPreferencesKey("volume_keys_inverted")
        val TAP_INVERT = booleanPreferencesKey("tap_invert")
        val LINKED_FOLDERS = stringSetPreferencesKey("linked_folders")
        // Legacy tap-zone keys (pre single-switch): read once for migration.
        val TAP_ZONE_MODE = stringPreferencesKey("tap_zone_mode")
        val TAP_ZONE_INVERT = stringPreferencesKey("tap_zone_invert")
        val NIGHT_LIGHT = booleanPreferencesKey("night_light")

        /** Legacy four-way invert values that mirrored the horizontal axis. */
        const val LEGACY_INVERT_HORIZONTAL = "HORIZONTAL"
        const val LEGACY_INVERT_BOTH = "BOTH"
        val NIGHT_LIGHT_INTENSITY = floatPreferencesKey("night_light_intensity")
        val PAGE_MARGINS = floatPreferencesKey("page_margins")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val APP_COLOR_SCHEME = stringPreferencesKey("app_color_scheme")
        val AMOLED = booleanPreferencesKey("amoled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val MOTION_STYLE = stringPreferencesKey("motion_style")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val LIBRARY_FILTER = stringPreferencesKey("library_filter")
        val LIBRARY_HIDE_ERRORS = booleanPreferencesKey("library_hide_errors")
        val LIBRARY_DISPLAY_MODE = stringPreferencesKey("library_display_mode")
        val LIBRARY_GRID_COLUMNS = intPreferencesKey("library_grid_columns")
    }
}
