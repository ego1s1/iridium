package com.iridium.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.LibraryFilter
import com.iridium.core.model.LibrarySortOrder
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReadingFlow
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
        assertNull(source.sourceTreeUri.first())
        assertEquals(ThemeMode.SYSTEM, source.themePreferences.first().mode)
        assertTrue(source.themePreferences.first().dynamicColor)
        assertEquals(AppColorScheme.IRIDIUM, source.themePreferences.first().colorScheme)
        assertFalse(source.themePreferences.first().amoled)
        assertEquals(MotionStyle.EXPRESSIVE, source.motionStyle.first())
        assertEquals(ReadingFlow.AUTO, source.readerPreferences.first().flow)
        assertEquals(TextAlign.ORIGINAL, source.readerPreferences.first().textAlign)
        assertEquals(LibrarySortOrder.RECENTLY_ADDED, source.libraryDisplay.first().sortOrder)
        assertEquals(LibraryFilter.ALL, source.libraryDisplay.first().filter)
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
                flow = ReadingFlow.SCROLLED,
                fontScale = 1.4f,
                textAlign = TextAlign.JUSTIFY,
                theme = ColorSchemeChoice.BLACK,
                brightness = 0.25f,
                keepScreenOn = false,
                showPageCounter = false,
                volumeKeys = true,
            )
        }
        val reader = source.readerPreferences.first()
        assertEquals(ReadingFlow.SCROLLED, reader.flow)
        assertEquals(1.4f, reader.fontScale)
        assertEquals(TextAlign.JUSTIFY, reader.textAlign)
        assertEquals(ColorSchemeChoice.BLACK, reader.theme)
        assertEquals(0.25f, reader.brightness)
        assertFalse(reader.keepScreenOn)
        assertFalse(reader.showPageCounter)
        assertTrue(reader.volumeKeys)
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
    fun `source tree uri can be set and cleared`() = runTest {
        source.setSourceTreeUri("content://tree/books")
        assertEquals("content://tree/books", source.sourceTreeUri.first())

        source.setSourceTreeUri(null)
        assertNull(source.sourceTreeUri.first())
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
    fun `corrupt enum values fall back to defaults instead of throwing`() = runTest {
        // A future/older build could have written a value this build does not
        // know; reading must not crash the app.
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")] = "NEON"
                this[androidx.datastore.preferences.core.stringPreferencesKey("reading_flow")] = "DIAGONAL"
            }.toPreferences()
        }
        assertEquals(ThemeMode.SYSTEM, source.themePreferences.first().mode)
        assertEquals(ReadingFlow.AUTO, source.readerPreferences.first().flow)
    }
}
