package com.iridium.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A corrupt/unreadable preferences file must never crash every collector:
 * emit defaults for IO failures and rethrow anything else.
 */
private fun Flow<Preferences>.catchOnCorruption(): Flow<Preferences> =
    catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

@Singleton
internal class DataStorePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : IridiumPreferencesDataSource {
    override val onboardingCompleted: Flow<Boolean> =
        dataStore.data.catchOnCorruption().map { it[ONBOARDING_COMPLETED] ?: false }

    override val readerPreferences: Flow<ReaderPreferences> =
        dataStore.data.catchOnCorruption().map { prefs -> prefs.toReaderPreferences() }

    private fun Preferences.toReaderPreferences(): ReaderPreferences =
        ReaderPreferences(
            flow = this[READING_FLOW]?.let {
                runCatching { ReadingFlow.valueOf(it) }.getOrDefault(ReadingFlow.AUTO)
            } ?: ReadingFlow.AUTO,
            fontScale = this[FONT_SCALE] ?: 1f,
            textAlign = this[TEXT_ALIGN]?.let {
                runCatching { TextAlign.valueOf(it) }.getOrDefault(TextAlign.ORIGINAL)
            } ?: TextAlign.ORIGINAL,
            theme = this[READER_THEME]?.let {
                runCatching { ColorSchemeChoice.valueOf(it) }.getOrDefault(ColorSchemeChoice.SEPIA)
            } ?: ColorSchemeChoice.SEPIA,
            brightness = this[BRIGHTNESS] ?: -1f,
            keepScreenOn = this[KEEP_SCREEN_ON] ?: true,
            showPageCounter = this[SHOW_PAGE_COUNTER] ?: true,
            volumeKeys = this[VOLUME_KEYS] ?: false,
            volumeKeysInverted = this[VOLUME_KEYS_INVERTED] ?: false,
            // Single-switch era: legacy four-way invert maps to mirrored
            // taps when it flipped the horizontal axis; legacy keys are
            // dropped on the next write below.
            invertTaps = this[TAP_INVERT] ?: this[TAP_ZONE_INVERT].let { legacy ->
                legacy == LEGACY_INVERT_HORIZONTAL || legacy == LEGACY_INVERT_BOTH
            },
            nightLight = this[NIGHT_LIGHT] ?: false,
            nightLightIntensity = this[NIGHT_LIGHT_INTENSITY] ?: 0.25f,
            pageMargins = this[PAGE_MARGINS] ?: 1f,
            lineHeight = this[LINE_HEIGHT] ?: 1.4f,
        )

    /**
     * Read-modify-write inside a single `edit`: the transform observes the
     * committed snapshot, so concurrent writers cannot interleave a stale
     * read between `first()` and the write and lose updates.
     */
    override suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toReaderPreferences())
            prefs[READING_FLOW] = updated.flow.name
            prefs[FONT_SCALE] = updated.fontScale
            prefs[TEXT_ALIGN] = updated.textAlign.name
            prefs[READER_THEME] = updated.theme.name
            prefs[BRIGHTNESS] = updated.brightness
            prefs[KEEP_SCREEN_ON] = updated.keepScreenOn
            prefs[SHOW_PAGE_COUNTER] = updated.showPageCounter
            prefs[VOLUME_KEYS] = updated.volumeKeys
            prefs[VOLUME_KEYS_INVERTED] = updated.volumeKeysInverted
            prefs[TAP_INVERT] = updated.invertTaps
            prefs.remove(TAP_ZONE_MODE)
            prefs.remove(TAP_ZONE_INVERT)
            prefs[NIGHT_LIGHT] = updated.nightLight
            prefs[NIGHT_LIGHT_INTENSITY] = updated.nightLightIntensity
            prefs[PAGE_MARGINS] = updated.pageMargins
            prefs[LINE_HEIGHT] = updated.lineHeight
        }
    }

    override val themePreferences: Flow<ThemePreferences> =
        dataStore.data.catchOnCorruption().map { prefs -> prefs.toThemePreferences() }

    private fun Preferences.toThemePreferences(): ThemePreferences =
        ThemePreferences(
            mode = this[THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM,
            dynamicColor = this[DYNAMIC_COLOR] ?: true,
            colorScheme = this[APP_COLOR_SCHEME]?.let {
                runCatching { AppColorScheme.valueOf(it) }
                    .getOrDefault(AppColorScheme.IRIDIUM)
            } ?: AppColorScheme.IRIDIUM,
            amoled = this[AMOLED] ?: false,
            hapticsEnabled = this[HAPTICS_ENABLED] ?: true,
        )

    override suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toThemePreferences())
            prefs[THEME_MODE] = updated.mode.name
            prefs[DYNAMIC_COLOR] = updated.dynamicColor
            prefs[APP_COLOR_SCHEME] = updated.colorScheme.name
            prefs[AMOLED] = updated.amoled
            prefs[HAPTICS_ENABLED] = updated.hapticsEnabled
        }
    }

    override val motionStyle: Flow<MotionStyle> =
        dataStore.data.catchOnCorruption().map { prefs ->
            prefs[MOTION_STYLE]?.let {
                runCatching { MotionStyle.valueOf(it) }.getOrDefault(MotionStyle.EXPRESSIVE)
            } ?: MotionStyle.EXPRESSIVE
        }

    override suspend fun updateMotionStyle(style: MotionStyle) {
        dataStore.edit { it[MOTION_STYLE] = style.name }
    }

    override val libraryDisplay: Flow<LibraryDisplay> =
        dataStore.data.catchOnCorruption().map { prefs -> prefs.toLibraryDisplay() }

    private fun Preferences.toLibraryDisplay(): LibraryDisplay =
        LibraryDisplay(
            sortOrder = this[LIBRARY_SORT]?.let {
                runCatching { LibrarySortOrder.valueOf(it) }
                    .getOrDefault(LibrarySortOrder.RECENTLY_ADDED)
            } ?: LibrarySortOrder.RECENTLY_ADDED,
            filter = this[LIBRARY_FILTER]?.let {
                runCatching { LibraryFilter.valueOf(it) }.getOrDefault(LibraryFilter.ALL)
            } ?: LibraryFilter.ALL,
            hideErrors = this[LIBRARY_HIDE_ERRORS] ?: false,
            displayMode = this[LIBRARY_DISPLAY_MODE]?.let {
                runCatching { LibraryDisplayMode.valueOf(it) }
                    .getOrDefault(LibraryDisplayMode.COMPACT)
            } ?: LibraryDisplayMode.COMPACT,
            gridColumns = this[LIBRARY_GRID_COLUMNS] ?: 0,
        )

    override suspend fun updateLibraryDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toLibraryDisplay())
            prefs[LIBRARY_SORT] = updated.sortOrder.name
            prefs[LIBRARY_FILTER] = updated.filter.name
            prefs[LIBRARY_HIDE_ERRORS] = updated.hideErrors
            prefs[LIBRARY_DISPLAY_MODE] = updated.displayMode.name
            prefs[LIBRARY_GRID_COLUMNS] = updated.gridColumns
        }
    }

    override val crashReportingEnabled: Flow<Boolean> =
        dataStore.data.catchOnCorruption().map { it[CRASH_REPORTING_ENABLED] ?: false }

    override val crashReportingAsked: Flow<Boolean> =
        dataStore.data.catchOnCorruption().map { it[CRASH_REPORTING_ASKED] ?: false }

    override val linkedFolders: Flow<Set<String>> =
        dataStore.data.catchOnCorruption().map { it[LINKED_FOLDERS] ?: emptySet() }

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
