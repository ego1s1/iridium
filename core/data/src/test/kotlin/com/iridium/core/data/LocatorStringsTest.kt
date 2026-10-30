package com.iridium.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The whole resume path rests on this String boundary: the locator is written
 * as JSON into the book row and parsed back on open. A silent decode failure
 * drops the reader at the start of the book, so the round trip is pinned here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocatorStringsTest {

    @Test
    fun `a locator survives the JSON boundary`() {
        val locator = Locator(
            href = Url("OEBPS/ch3.xhtml")!!,
            mediaType = MediaType.XHTML,
            title = "The Horse and His Boy",
            locations = Locator.Locations(
                fragments = listOf("p42"),
                progression = 0.25,
                position = 17,
                totalProgression = 0.5,
            ),
            text = Locator.Text(before = "a", highlight = "b", after = "c"),
        )

        val decoded = LocatorStrings.decode(LocatorStrings.encode(locator))

        assertEquals("OEBPS/ch3.xhtml", decoded?.href?.toString())
        assertEquals("The Horse and His Boy", decoded?.title)
        assertEquals(listOf("p42"), decoded?.locations?.fragments)
        assertEquals(0.25, decoded?.locations?.progression)
        assertEquals(17, decoded?.locations?.position)
        assertEquals(0.5, decoded?.locations?.totalProgression)
        assertEquals("b", decoded?.text?.highlight)
    }

    @Test
    fun `a locator without totalProgression still round-trips`() {
        // Some positions carry no totalProgression; losing the locator over a
        // missing optional field would reset the reader to page one.
        val locator = Locator(
            href = Url("OEBPS/ch1.xhtml")!!,
            mediaType = MediaType.XHTML,
            title = null,
            locations = Locator.Locations(progression = 0.1),
            text = Locator.Text(),
        )

        val decoded = LocatorStrings.decode(LocatorStrings.encode(locator))

        assertEquals("OEBPS/ch1.xhtml", decoded?.href?.toString())
        assertNull(decoded?.locations?.totalProgression)
    }

    @Test
    fun `missing or corrupt input decodes to null instead of throwing`() {
        assertNull(LocatorStrings.decode(null))
        assertNull(LocatorStrings.decode(""))
        assertNull(LocatorStrings.decode("{"))
        assertNull(LocatorStrings.decode("{\"href\":\"not a url\"}"))
    }
}
