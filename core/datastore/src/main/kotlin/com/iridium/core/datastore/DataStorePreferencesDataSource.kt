package com.iridium.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.LibraryDisplay
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

    override val sourceTreeUri: Flow<String?> =
        dataStore.data.map { it[SOURCE_TREE_URI] }

    override suspend fun setSourceTreeUri(uri: String?) {
        dataStore.edit {
            if (uri == null) it.remove(SOURCE_TREE_URI) else it[SOURCE_TREE_URI] = uri
        }
    }

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
            )
        }

    override suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        val updated = transform(themePreferences.first())
        dataStore.edit {
            it[THEME_MODE] = updated.mode.name
            it[DYNAMIC_COLOR] = updated.dynamicColor
            it[APP_COLOR_SCHEME] = updated.colorScheme.name
            it[AMOLED] = updated.amoled
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
            )
        }

    override suspend fun updateLibraryDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        val updated = transform(libraryDisplay.first())
        dataStore.edit {
            it[LIBRARY_SORT] = updated.sortOrder.name
            it[LIBRARY_FILTER] = updated.filter.name
            it[LIBRARY_HIDE_ERRORS] = updated.hideErrors
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SOURCE_TREE_URI = stringPreferencesKey("source_tree_uri")
        val READING_FLOW = stringPreferencesKey("reading_flow")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val TEXT_ALIGN = stringPreferencesKey("text_align")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_PAGE_COUNTER = booleanPreferencesKey("show_page_counter")
        val VOLUME_KEYS = booleanPreferencesKey("volume_keys")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val APP_COLOR_SCHEME = stringPreferencesKey("app_color_scheme")
        val AMOLED = booleanPreferencesKey("amoled")
        val MOTION_STYLE = stringPreferencesKey("motion_style")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val LIBRARY_FILTER = stringPreferencesKey("library_filter")
        val LIBRARY_HIDE_ERRORS = booleanPreferencesKey("library_hide_errors")
    }
}
