package com.cir0cuit.devtoys.tools.generators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Random

class PasswordModesTest {
    private val allSets = listOf(PASSWORD_UPPER, PASSWORD_LOWER, PASSWORD_DIGITS, PASSWORD_SYMBOLS)

    @Test
    fun minEachIsHonoured() {
        val rng = Random(1)
        repeat(1_000) {
            val pw = generatePassword(8, allSets, rng, minEach = 2)!!
            assertEquals(8, pw.length)
            for (s in allSets) assertTrue(pw.count { it in s } >= 2)
        }
    }

    @Test
    fun noRepeatGivesDistinctCharacters() {
        val rng = Random(2)
        repeat(500) {
            val pw = generatePassword(10, listOf(PASSWORD_DIGITS), rng, noRepeat = true)!!
            assertEquals(10, pw.toSet().size)
        }
        assertNull(generatePassword(11, listOf(PASSWORD_DIGITS), rng, noRepeat = true))
    }

    @Test
    fun entropyAndStrength() {
        assertEquals(128.0, entropyBits(16, 256), 1e-9)
        assertEquals("Weak", strengthLabel(30.0))
        assertEquals("Very strong", strengthLabel(100.0))
        assertEquals("less than a second", crackTime(20.0))
        assertEquals("more than a million years", crackTime(128.0))
    }

    @Test
    fun passphraseShape() {
        val words = listOf("alpha", "bravo", "charlie")
        val p = generatePassphrase(words, 5, ".", capitalize = true, addDigit = true, rng = Random(3))
        val parts = p.split(".")
        assertEquals(5, parts.size)
        assertTrue(parts.all { it[0].isUpperCase() })
        assertEquals(1, p.count { it.isDigit() })
    }

    @Test
    fun trivialPins() {
        for (pin in listOf("0000", "1234", "9876", "7890", "123456")) assertTrue(pin, isTrivialPin(pin))
        for (pin in listOf("1357", "2580", "1123")) assertFalse(pin, isTrivialPin(pin))
        val rng = Random(4)
        repeat(1_000) { assertFalse(isTrivialPin(generatePin(4, true, rng))) }
    }

    @Test
    fun bundledWordlistIsTheEffLargeList() {
        val words = File("src/main/assets/$EFF_WORDLIST_ASSET").useLines(block = ::parseWordlist)
        assertEquals(7776, words.size)
        assertEquals(7776, words.toSet().size)
        assertEquals("abacus", words.first())
        assertEquals(6 * 12.925, passphraseBits(words.size, 6, false), 0.01)
    }
}
