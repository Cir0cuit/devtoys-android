package com.cir0cuit.devtoys.tools.encoders

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberBaseParseTest {

    @Test
    fun parsesSimpleValuesInEachBase() {
        assertEquals(BigInteger.valueOf(255), parseBig("255", 10))
        assertEquals(BigInteger.valueOf(255), parseBig("ff", 16))
        assertEquals(BigInteger.valueOf(255), parseBig("FF", 16))
        assertEquals(BigInteger.valueOf(255), parseBig("11111111", 2))
        assertEquals(BigInteger.valueOf(255), parseBig("377", 8))
    }

    @Test
    fun acceptsConventionalPrefixes() {
        assertEquals(BigInteger.valueOf(255), parseBig("0xff", 16))
        assertEquals(BigInteger.valueOf(255), parseBig("0XFF", 16))
        assertEquals(BigInteger.valueOf(10), parseBig("0b1010", 2))
        assertEquals(BigInteger.valueOf(10), parseBig("0B1010", 2))
        assertEquals(BigInteger.valueOf(15), parseBig("0o17", 8))
    }

    @Test
    fun ignoresDigitSeparatorsAndSurroundingWhitespace() {
        assertEquals(BigInteger.valueOf(1_000_000), parseBig("1_000_000", 10))
        assertEquals(BigInteger.valueOf(42), parseBig("  42 \n", 10))
        assertEquals(BigInteger.valueOf(0xDEADBEEFL), parseBig("dead_beef", 16))
    }

    @Test
    fun valuesBeyondLongRangeAreExact() {
        val twoTo64 = BigInteger.ONE.shiftLeft(64)
        assertEquals(twoTo64, parseBig("18446744073709551616", 10))
        assertEquals("10000000000000000", parseBig("18446744073709551616", 10)!!.toString(16))
        assertEquals(twoTo64.subtract(BigInteger.ONE), parseBig("0xFFFFFFFFFFFFFFFF", 16))
        assertEquals(twoTo64, parseBig("1" + "0".repeat(64), 2))
        val huge = "123456789012345678901234567890123456789012345678901234567890"
        val v = parseBig(huge, 10)!!
        assertEquals(huge, parseBig(v.toString(16), 16)!!.toString(10))
        assertEquals(huge, parseBig(v.toString(2), 2)!!.toString(10))
        assertEquals(huge, parseBig(v.toString(8), 8)!!.toString(10))
    }

    @Test
    fun negativeValues() {
        assertEquals(BigInteger.valueOf(-255), parseBig("-255", 10))
        assertEquals("-ff", parseBig("-255", 10)!!.toString(16))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(parseBig("", 10))
        assertNull(parseBig("   ", 10))
        assertNull(parseBig("12a", 10))
        assertNull(parseBig("2", 2))
        assertNull(parseBig("8", 8))
        assertNull(parseBig("g", 16))
        assertNull(parseBig("0x", 16))
        assertNull(parseBig("1.5", 10))
    }

    @Test
    fun rejectsOverlongInput() {
        assertNull(parseBig("1".repeat(4097), 10))
        assertEquals(BigInteger("1".repeat(4096)), parseBig("1".repeat(4096), 10))
    }

    @Test
    fun toleratesSpacesAndSignedPrefixedInput() {
        assertEquals(BigInteger.valueOf(0xDEADBEEFL), parseBig("de ad be ef", 16))
        assertEquals(BigInteger.valueOf(-255), parseBig("-0xff", 16))
        assertNull(parseBig("--5", 10))
    }

    @Test
    fun twosComplementAtFixedWidths() {
        assertEquals(BigInteger.valueOf(0xFF), toWidth(BigInteger.valueOf(-1), 8))
        assertEquals(BigInteger.valueOf(0x80), toWidth(BigInteger.valueOf(-128), 8))
        assertNull(toWidth(BigInteger.valueOf(-129), 8))
        assertNull(toWidth(BigInteger.valueOf(256), 8))
        assertEquals(BigInteger.valueOf(-1), signedOf(BigInteger.valueOf(0xFFFFFFFFL), 32))
        assertEquals(BigInteger.valueOf(Int.MIN_VALUE.toLong()), signedOf(BigInteger.valueOf(0x80000000L), 32))
        assertEquals(BigInteger.valueOf(127), signedOf(BigInteger.valueOf(127), 8))
        assertEquals(BigInteger.valueOf(-1), signedOf(toWidth(BigInteger.valueOf(-1), 64)!!, 64))
    }

    @Test
    fun formatsWithGroupingPrefixAndPadding() {
        assertEquals("1_000_000", formatRadix(BigInteger.valueOf(1_000_000), 10, group = true))
        assertEquals("0xDEAD_BEEF", formatRadix(BigInteger.valueOf(0xDEADBEEFL), 16, group = true, prefix = true))
        assertEquals("0b1010_1010", formatRadix(BigInteger.valueOf(0xAA), 2, group = true, prefix = true))
        assertEquals("0o17", formatRadix(BigInteger.valueOf(15), 8, prefix = true))
        assertEquals("00FF", formatRadix(BigInteger.valueOf(255), 16, padTo = 4))
        assertEquals("-0x1F", formatRadix(BigInteger.valueOf(-31), 16, prefix = true))
        assertEquals("zz", formatRadix(BigInteger.valueOf(36 * 36 - 1), 36))
    }

    @Test
    fun byteOrderOfAPattern() {
        assertEquals("12 34 56 78", byteOrder(BigInteger.valueOf(0x12345678), 32, littleEndian = false))
        assertEquals("78 56 34 12", byteOrder(BigInteger.valueOf(0x12345678), 32, littleEndian = true))
        assertEquals("00 01", byteOrder(BigInteger.ONE, 16, littleEndian = false))
    }
}
