package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {
    private fun hex(c: Int?) = c?.let { "%08X".format(it) }

    @Test fun parsesHexForms() {
        assertEquals("FF6496FF", hex(parseColor("#6496FF")))
        assertEquals("FF6496FF", hex(parseColor("6496ff")))
        assertEquals("FFAABBCC", hex(parseColor("#abc")))
        assertEquals("88AABBCC", hex(parseColor("#abc8")))
        assertEquals("80112233", hex(parseColor("#11223380")))
        assertEquals("80112233", hex(parseColor("0x80112233"))) // Android order
        assertEquals("FF112233", hex(parseColor("0x112233")))
    }

    @Test fun parsesFunctionsAndNames() {
        assertEquals("FFFF0000", hex(parseColor("rgb(255, 0, 0)")))
        assertEquals("80FF0000", hex(parseColor("rgba(100%, 0%, 0%, 0.5)")))
        assertEquals("80FF0000", hex(parseColor("rgb(255 0 0 / 50%)")))
        assertEquals("FF00FF00", hex(parseColor("hsl(120, 100%, 50%)")))
        assertEquals("FF0000FF", hex(parseColor("hsla(240deg 100% 50% / 1)")))
        assertEquals("FF808080", hex(parseColor("hwb(0 50% 50%)")))
        assertEquals("FF6495ED", hex(parseColor("CornflowerBlue")))
        assertEquals("00000000", hex(parseColor("transparent")))
        assertEquals("FFFFFFFF", hex(parseColor("oklch(1 0 0)")))
        assertEquals("FFFF0000", hex(parseColor("oklch(0.62796 0.25768 29.234)")))
    }

    @Test fun rejectsGarbage() {
        listOf("", "#12", "#12345", "rgb(1, 2)", "notacolor", "0x1234", "hsl(a, b, c)").forEach { assertNull(it, parseColor(it)) }
    }

    @Test fun namedTableHas148Colors() = assertEquals(148, CSS_NAMED_COLORS.size)

    @Test fun oklabAndLabOfRed() {
        val ok = toOklab(0xFFFF0000.toInt())
        assertEquals(0.62796, ok[0], 1e-3); assertEquals(0.22486, ok[1], 1e-3); assertEquals(0.12585, ok[2], 1e-3)
        val lab = toLab(0xFFFF0000.toInt())
        assertEquals(54.29, lab[0], 0.05); assertEquals(80.8, lab[1], 0.1); assertEquals(69.89, lab[2], 0.1)
    }

    @Test fun formatsRoundTripThroughParser() {
        val c = 0xFF6496FF.toInt()
        val rows = colorFormats(c).toMap()
        assertEquals("#6496FF", rows["Hex"])
        assertEquals("rgb(100, 150, 255)", rows["RGB"])
        assertEquals("Color(0xFF6496FF)", rows["Compose"])
        assertEquals((0xFF6496FF).toInt().toString(), rows["Android int"])
        assertEquals("≈ cornflowerblue", rows["Name"])
        listOf("RGB", "HSL", "HWB", "OKLCH", "OKLab").forEach { label ->
            val back = parseColor(rows.getValue(label))
            assertNotNull(label, back)
            listOf(::redOf, ::greenOf, ::blueOf).forEach { ch -> assertTrue(label, kotlin.math.abs(ch(back!!) - ch(c)) <= 1) }
        }
        assertEquals("rgba(100, 150, 255, 0.502)", colorFormats(0x806496FF.toInt()).toMap()["RGB"])
        assertEquals("#6496FF80", formatHex(0x806496FF.toInt()))
    }

    @Test fun contrastRatios() {
        assertEquals(21.0, contrastRatio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 1e-9)
        assertEquals(1.0, contrastRatio(0xFF777777.toInt(), 0xFF777777.toInt()), 1e-9)
        assertEquals(4.48, contrastRatio(0xFF777777.toInt(), 0xFFFFFFFF.toInt()), 0.01)
    }

    @Test fun suggestionReachesTarget() {
        val bg = 0xFFFFFFFF.toInt()
        val fixed = suggestForeground(0xFF6496FF.toInt(), bg, 4.5)!!
        assertTrue(contrastRatio(fixed, bg) >= 4.5)
        assertTrue(contrastRatio(fixed, bg) < 5.0) // closest passing shade, not just black
        assertNull(suggestForeground(0xFF777777.toInt(), 0xFF777777.toInt(), 21.0))
    }

    @Test fun scaleKeepsBaseAndGetsDarker() {
        val base = 0xFF3498DB.toInt()
        val scale = tintScale(base)
        assertEquals(SCALE_STEPS, scale.map { it.first })
        assertEquals(base, scale[5].second)
        val lightness = scale.map { toOklab(it.second)[0] }
        assertEquals(lightness.sortedDescending(), lightness)
        assertTrue(exportScale(scale, ScaleFormat.CSS).contains("--primary-500: #3498DB;"))
        assertTrue(exportScale(scale, ScaleFormat.COMPOSE).contains("val Primary500 = Color(0xFF3498DB)"))
    }

    @Test fun colorVisionKeepsGrays() {
        ColorVision.entries.forEach { v ->
            val gray = v.simulate(0xFF808080.toInt())
            assertTrue(v.label, kotlin.math.abs(redOf(gray) - 128) <= 2 && kotlin.math.abs(blueOf(gray) - 128) <= 2)
        }
    }
}
