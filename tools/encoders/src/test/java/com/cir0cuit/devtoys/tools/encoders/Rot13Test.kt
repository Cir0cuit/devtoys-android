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

    @Test
    fun rot47KnownVectorAndInvolution() {
        assertEquals("w6==@", rot47("Hello"))
        val s = "The Quick Brown Fox! 123 ~"
        assertEquals(s, rot47(rot47(s)))
        assertEquals("é ü", rot47("é ü"))
    }

    @Test
    fun rot18RotatesLettersAndDigits() {
        assertEquals("Uryyb 5678901234", rot18("Hello 0123456789"))
        assertEquals("abc 123", rot18(rot18("abc 123")))
    }

    @Test
    fun atbashMirrorsTheAlphabet() {
        assertEquals("ZYX zyx 1!", atbash("ABC abc 1!"))
        assertEquals("Svool", atbash("Hello"))
    }

    @Test
    fun vigenereClassicVector() {
        // Standard textbook example: ATTACKATDAWN with key LEMON.
        assertEquals("LXFOPVEFRNHR", vigenere("ATTACKATDAWN", "LEMON", decrypt = false))
        assertEquals("ATTACKATDAWN", vigenere("LXFOPVEFRNHR", "lemon", decrypt = true))
        // Non-letters pass through and do not advance the key.
        assertEquals("Lxf opv", vigenere("Att ack", "LEMON", decrypt = false))
        assertEquals(null, vigenere("abc", "123", decrypt = false))
    }

    @Test
    fun xorIsItsOwnInverse() {
        val data = "secret".toByteArray()
        val key = "k".toByteArray()
        assertEquals(0x73 xor 0x6b, xorBytes(data, key)[0].toInt())
        assertEquals("secret", String(xorBytes(xorBytes(data, "key".toByteArray()), "key".toByteArray())))
    }

    @Test
    fun allShiftsRanksEnglishFirst() {
        val cipher = rotN("the quick brown fox jumps over the lazy dog", 3)
        val ranked = allShifts(cipher)
        assertEquals(25, ranked.size)
        assertEquals(23, ranked.first().first)
        assertEquals("the quick brown fox jumps over the lazy dog", ranked.first().second)
    }
}
