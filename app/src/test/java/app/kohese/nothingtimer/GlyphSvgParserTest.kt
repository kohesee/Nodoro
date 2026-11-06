package app.kohese.nothingtimer

import app.kohese.nothingtimer.glyph.GlyphSvgParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class GlyphSvgParserTest {

    private fun getAssetStream(fileName: String): FileInputStream {
        val candidates = listOf(
            File("src/main/assets/glyphs/$fileName"),
            File("app/src/main/assets/glyphs/$fileName"),
            File("../app/src/main/assets/glyphs/$fileName")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find $fileName in ${candidates.map { it.absolutePath }}")
        return FileInputStream(file)
    }

    @Test
    fun parse_phone1() {
        val stream = getAssetStream("phone1.svg")
        val parsed = GlyphSvgParser.parse(stream)
        assertEquals("(1)", parsed.id)
        assertEquals(182f, parsed.width)
        assertEquals(382f, parsed.height)
        assertEquals(5, parsed.paths.size)
        assertNotNull(parsed.paths.find { it.id == "2" })
    }

    @Test
    fun parse_phone2() {
        val stream = getAssetStream("phone2.svg")
        val parsed = GlyphSvgParser.parse(stream)
        assertEquals("(2)", parsed.id)
        assertEquals(182f, parsed.width)
        assertEquals(382f, parsed.height)
        assertTrue(parsed.paths.size >= 10)
        assertNotNull(parsed.gradients["gradient-3-18"])
        assertEquals(16, parsed.gradients["gradient-3-18"]?.stops?.size)
        assertNotNull(parsed.gradients["gradient-25-32"])
        assertEquals(8, parsed.gradients["gradient-25-32"]?.stops?.size)
    }

    @Test
    fun parse_phone2a() {
        val stream = getAssetStream("phone2a.svg")
        val parsed = GlyphSvgParser.parse(stream)
        assertEquals("(2a)", parsed.id)
        assertEquals(182f, parsed.width)
        assertEquals(182f, parsed.height)
        assertTrue(parsed.paths.size >= 3)
        assertNotNull(parsed.gradients["gradient-0-23"])
        assertEquals(24, parsed.gradients["gradient-0-23"]?.stops?.size)
        assertEquals(1.128745f, parsed.transform.scaleX, 0.0001f)
        assertEquals(-13.02971f, parsed.transform.translateX, 0.0001f)
    }

    @Test
    fun parse_phone3a() {
        val stream = getAssetStream("phone3a.svg")
        val parsed = GlyphSvgParser.parse(stream)
        assertEquals("(3a)", parsed.id)
        assertEquals(182f, parsed.width)
        assertEquals(182f, parsed.height)
        assertTrue(parsed.paths.size >= 3)
        assertNotNull(parsed.gradients["gradient-0-19"])
        assertEquals(20, parsed.gradients["gradient-0-19"]?.stops?.size)
        assertNotNull(parsed.gradients["gradient-20-30"])
        assertEquals(11, parsed.gradients["gradient-20-30"]?.stops?.size)
        assertNotNull(parsed.gradients["gradient-31-35"])
        assertEquals(5, parsed.gradients["gradient-31-35"]?.stops?.size)
        assertEquals(1.03f, parsed.transform.scaleX, 0.001f)
        assertEquals(-2f, parsed.transform.translateX, 0.001f)
    }

    @Test
    fun parse_phone4a() {
        val stream = getAssetStream("phone4a.svg")
        val parsed = GlyphSvgParser.parse(stream)
        assertEquals("(4a)", parsed.id)
        assertEquals(21f, parsed.width)
        assertEquals(141f, parsed.height)
        assertEquals(7, parsed.rects.size)
    }

    @Test
    fun glyphPhoneModel_deviceMappingAndCycle() {
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_1, app.kohese.nothingtimer.glyph.GlyphPhoneModel.fromDeviceName("Nothing Phone (1)"))
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_2, app.kohese.nothingtimer.glyph.GlyphPhoneModel.fromDeviceName("Nothing Phone (2)"))
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_2A, app.kohese.nothingtimer.glyph.GlyphPhoneModel.fromDeviceName("Nothing Phone (2a)"))
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_2A, app.kohese.nothingtimer.glyph.GlyphPhoneModel.fromDeviceName("Nothing Phone (2a) Plus"))
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_3A, app.kohese.nothingtimer.glyph.GlyphPhoneModel.fromDeviceName("Nothing Phone (3a) / (3a) Pro"))
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_4A, app.kohese.nothingtimer.glyph.GlyphPhoneModel.fromDeviceName("Nothing Phone (4a)"))
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_2, app.kohese.nothingtimer.glyph.GlyphPhoneModel.fromDeviceName("Standard Android"))

        // Cycle through all models
        var current = app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_1
        current = current.next()
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_2, current)
        current = current.next()
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_2A, current)
        current = current.next()
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_3A, current)
        current = current.next()
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_4A, current)
        current = current.next()
        assertEquals(app.kohese.nothingtimer.glyph.GlyphPhoneModel.PHONE_1, current)
    }

    @Test
    fun convertAllSvgPathsToComposePaths() {
        val files = listOf("phone1.svg", "phone2.svg", "phone2a.svg", "phone3a.svg")
        for (file in files) {
            val parsed = GlyphSvgParser.parse(getAssetStream(file))
            for (p in parsed.paths) {
                val parser = androidx.compose.ui.graphics.vector.PathParser().parsePathString(p.d)
                assertTrue(parser.toNodes().isNotEmpty())
            }
        }
    }
}





