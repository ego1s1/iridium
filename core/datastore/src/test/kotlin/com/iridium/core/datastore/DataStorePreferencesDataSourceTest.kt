package com.iridium.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.CustomReaderTheme
import com.iridium.core.model.LibraryFilter
import com.iridium.core.model.isCustomThemeActive
import com.iridium.core.model.resolvedColors
import com.iridium.core.model.LibrarySortOrder
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.TextAlign
import com.iridium.core.model.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlinx.collections.immutable.persistentListOf

class DataStorePreferencesDataSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var source: DataStorePreferencesDataSource

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(temporaryFolder.root, "prefs.preferences_pb") },
        )
        source = DataStorePreferencesDataSource(dataStore)
    }

    @Test
    fun `defaults reflect a fresh install`() = runTest {
        assertFalse(source.onboardingCompleted.first())
        assertEquals(ThemeMode.SYSTEM, source.themePreferences.first().mode)
        assertTrue(source.themePreferences.first().dynamicColor)
        assertEquals(AppColorScheme.IRIDIUM, source.themePreferences.first().colorScheme)
        assertFalse(source.themePreferences.first().amoled)
        assertEquals(MotionStyle.EXPRESSIVE, source.motionStyle.first())
        assertEquals(TextAlign.JUSTIFY, source.readerPreferences.first().textAlign)
        assertEquals(LibrarySortOrder.RECENTLY_ADDED, source.libraryDisplay.first().sortOrder)
        assertEquals(LibraryFilter.ALL, source.libraryDisplay.first().filter)
    }

    @Test
    fun `custom themes round-trip through the store`() = runTest {
        val custom = CustomReaderTheme("c1", "Forest", 0xFF1B2A1B.toInt(), 0xFFE8F0E8.toInt())
        source.updateReaderPreferences {
            it.copy(customThemes = persistentListOf(custom), customThemeId = "c1")
        }
        val prefs = source.readerPreferences.first()
        assertEquals(listOf(custom), prefs.customThemes)
        assertEquals("c1", prefs.customThemeId)
        assertEquals(0xFF1B2A1B.toInt() to 0xFFE8F0E8.toInt(), prefs.resolvedColors())
        assertTrue(prefs.isCustomThemeActive())
    }

    @Test
    fun `custom theme codec rejects junk`() {
        assertEquals(null, decodeCustomTheme("nope"))
        assertEquals(null, decodeCustomTheme("a|b|zz|00"))
        val theme = CustomReaderTheme("c1", "A|B", 0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        val decoded = decodeCustomTheme(encodeCustomTheme(theme))
        assertEquals("c1", decoded?.id)
        assertEquals("AB", decoded?.name)
    }

    @Test
    fun `legacy four-way invert migrates to the single switch`() = runTest {
        dataStore.edit {
            it[stringPreferencesKey("tap_zone_invert")] = "HORIZONTAL"
        }
        assertTrue(source.readerPreferences.first().invertTaps)

        dataStore.edit {
            it[stringPreferencesKey("tap_zone_invert")] = "VERTICAL"
        }
        // Toggle the new key once so the write path drops legacy keys.
        source.updateReaderPreferences { it.copy(invertTaps = false) }
        assertFalse(source.readerPreferences.first().invertTaps)
    }

    @Test
    fun `theme preferences round-trip`() = runTest {
        source.updateThemePreferences {
            it.copy(
                mode = ThemeMode.DARK,
                dynamicColor = false,
                colorScheme = AppColorScheme.OCEAN,
                amoled = true,
            )
        }
        val theme = source.themePreferences.first()
        assertEquals(ThemeMode.DARK, theme.mode)
        assertFalse(theme.dynamicColor)
        assertEquals(AppColorScheme.OCEAN, theme.colorScheme)
        assertTrue(theme.amoled)
    }

    @Test
    fun `reader preferences round-trip including floats`() = runTest {
        source.updateReaderPreferences {
            it.copy(
                fontScale = 1.4f,
                textAlign = TextAlign.JUSTIFY,
                theme = ColorSchemeChoice.BLACK,
                brightness = 0.25f,
                keepScreenOn = false,
                volumeKeys = true,
                volumeKeysInverted = true,
                invertTaps = true,
                nightLight = true,
                nightLightIntensity = 0.6f,
                extraDim = true,
                extraDimIntensity = 0.45f,
                pageMargins = 1.5f,
                lineHeight = 1.8f,
            )
        }
        val reader = source.readerPreferences.first()
        assertEquals(1.4f, reader.fontScale)
        assertEquals(TextAlign.JUSTIFY, reader.textAlign)
        assertEquals(ColorSchemeChoice.BLACK, reader.theme)
        assertEquals(0.25f, reader.brightness)
        assertFalse(reader.keepScreenOn)
        assertTrue(reader.volumeKeys)
        assertTrue(reader.volumeKeysInverted)
        assertTrue(reader.invertTaps)
        assertTrue(reader.nightLight)
        assertEquals(0.6f, reader.nightLightIntensity)
        assertTrue(reader.extraDim)
        assertEquals(0.45f, reader.extraDimIntensity)
        assertEquals(1.5f, reader.pageMargins)
        assertEquals(1.8f, reader.lineHeight)
    }

    @Test
    fun `library display round-trips`() = runTest {
        source.updateLibraryDisplay {
            it.copy(sortOrder = LibrarySortOrder.TITLE, filter = LibraryFilter.FAVORITES, hideErrors = true)
        }
        val display = source.libraryDisplay.first()
        assertEquals(LibrarySortOrder.TITLE, display.sortOrder)
        assertEquals(LibraryFilter.FAVORITES, display.filter)
        assertTrue(display.hideErrors)
    }

    @Test
    fun `motion style round-trips`() = runTest {
        source.updateMotionStyle(MotionStyle.CALM)
        assertEquals(MotionStyle.CALM, source.motionStyle.first())
    }

    @Test
    fun `onboarding completion round-trips`() = runTest {
        source.setOnboardingCompleted(true)
        assertTrue(source.onboardingCompleted.first())
    }

    @Test
    fun `crash reporting defaults off and records consent`() = runTest {
        assertFalse(source.crashReportingEnabled.first())
        assertFalse(source.crashReportingAsked.first())

        // Answering the prompt marks it asked, whether the answer is yes or no.
        source.setCrashReporting(false)
        assertFalse(source.crashReportingEnabled.first())
        assertTrue(source.crashReportingAsked.first())

        source.setCrashReporting(true)
        assertTrue(source.crashReportingEnabled.first())
    }

    @Test
    fun `legacy grid columns normalise to the default`() = runTest {
        // 0 was the old "auto" value (which resolved to two columns) and 5/6
        // were offered before the picker was reduced to 2..4. Either value
        // would leave the grid on GridCells.Fixed(0) or an unselectable mode.
        for (legacy in listOf(0, 1, 5, 6)) {
            dataStore.updateData { prefs ->
                prefs.toMutablePreferences().apply {
                    this[androidx.datastore.preferences.core.intPreferencesKey("library_grid_columns")] =
                        legacy
                }.toPreferences()
            }
            assertEquals(
                "columns $legacy should read back as 2",
                2,
                source.libraryDisplay.first().gridColumns,
            )
        }
    }

    @Test
    fun `supported grid columns survive a round-trip`() = runTest {
        for (columns in listOf(2, 3, 4)) {
            source.updateLibraryDisplay { it.copy(gridColumns = columns) }
            assertEquals(columns, source.libraryDisplay.first().gridColumns)
        }
    }

    @Test
    fun `removed book paths are remembered across reads`() = runTest {
        // A linked book is unlinked, never deleted from disk, so the scanner
        // needs this set to avoid re-adding it on the next pass.
        assertTrue(source.removedBookPaths.first().isEmpty())
        source.rememberRemovedBookPath("content://tree/book.epub")
        source.rememberRemovedBookPath("content://tree/other.epub")
        assertEquals(
            setOf("content://tree/book.epub", "content://tree/other.epub"),
            source.removedBookPaths.first(),
        )
    }

    @Test
    fun `corrupt enum values fall back to defaults instead of throwing`() = runTest {
        // A future/older build could have written a value this build does not
        // know; reading must not crash the app.
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")] = "NEON"
            }.toPreferences()
        }
        assertEquals(ThemeMode.SYSTEM, source.themePreferences.first().mode)
    }
}
