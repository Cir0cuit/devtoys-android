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
}
