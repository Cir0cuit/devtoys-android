package com.cir0cuit.devtoys.tools.encoders

import org.junit.Assert.assertEquals
import org.junit.Test

class Rot13Test {

    @Test
    fun rot13KnownVector() {
        assertEquals("Uryyb, Jbeyq!", rotN("Hello, World!", 13))
        assertEquals("NOPQRSTUVWXYZABCDEFGHIJKLM", rotN("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 13))
        assertEquals("nopqrstuvwxyzabcdefghijklm", rotN("abcdefghijklmnopqrstuvwxyz", 13))
    }

    @Test
    fun rot13IsAnInvolution() {
        val s = "The Quick Brown Fox Jumps Over The Lazy Dog 0123456789"
        assertEquals(s, rotN(rotN(s, 13), 13))
    }

    @Test
    fun caesarShiftWrapsAround() {
        assertEquals("abc", rotN("xyz", 3))
        assertEquals("DEF", rotN("ABC", 3))
        assertEquals("ZAB", rotN("ABC", 25))
    }

    @Test
    fun shiftAndComplementaryShiftAreInverse() {
        val s = "Attack at Dawn"
        for (k in 1..25) assertEquals(s, rotN(rotN(s, k), 26 - k))
    }

    @Test
    fun nonAsciiLettersAreUnchanged() {
        // ROT13 is defined over the 26 ASCII Latin letters only
        assertEquals("Üaïpöqé", rotN("Ünïcödé", 13))
        assertEquals("ß Привет Ωω", rotN("ß Привет Ωω", 13))
        // fullwidth Latin letters are not ASCII
        assertEquals("Ａａ", rotN("Ａａ", 13))
    }

    @Test
    fun surrogatePairsDigitsAndPunctuationAreUnchanged() {
        val s = "😀 123 !?@[`{"
        assertEquals(s, rotN(s, 13))
    }
}
