package com.cir0cuit.devtoys.tools.generators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class PasswordGeneratorTest {

    private val allSets = listOf(PASSWORD_UPPER, PASSWORD_LOWER, PASSWORD_DIGITS, PASSWORD_SYMBOLS)

    @Test
    fun noCharacterSetReturnsNull() {
        assertNull(generatePassword(16, emptyList(), Random(1)))
    }

    @Test
    fun lengthMatchesRequest() {
        for (len in 4..64) {
            assertEquals(len, generatePassword(len, allSets, Random(len.toLong()))!!.length)
        }
    }

    @Test
    fun lengthIsRaisedToNumberOfSelectedSets() {
        val pw = generatePassword(2, allSets, Random(7))
        assertNotNull(pw)
        assertEquals(4, pw!!.length)
        assertEveryClassPresent(pw, allSets)
    }

    @Test
    fun everySelectedClassIsPresentAtMinimumLength() {
        // At length == number of sets a uniform pick from the pooled alphabet would very often
        // miss a class; the generator must guarantee one of each.
        val rng = Random(42)
        repeat(5_000) {
            val pw = generatePassword(4, allSets, rng)!!
            assertEveryClassPresent(pw, allSets)
        }
    }

    @Test
    fun everySelectedClassIsPresentForAnySubset() {
        val rng = Random(1234)
        for (mask in 1 until 16) {
            val sets = allSets.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            val allowed = sets.joinToString("").toSet()
            repeat(500) {
                val len = 4 + rng.nextInt(61)
                val pw = generatePassword(len, sets, rng)!!
                assertEquals(len, pw.length)
                assertEveryClassPresent(pw, sets)
                assertTrue("unexpected char in $pw for sets $sets", pw.all { it in allowed })
            }
        }
    }

    @Test
    fun guaranteedCharactersAreShuffled() {
        // After the Fisher-Yates shuffle the first character must not always be an uppercase letter.
        val rng = Random(99)
        val firstChars = (1..2_000).map { generatePassword(16, allSets, rng)!![0] }
        assertTrue(firstChars.any { it !in PASSWORD_UPPER })
        assertTrue(firstChars.any { it in PASSWORD_UPPER })
    }

    @Test
    fun allAlphabetCharactersAreReachable() {
        val rng = Random(5)
        val seen = HashSet<Char>()
        repeat(2_000) { seen += generatePassword(64, allSets, rng)!!.toSet() }
        assertEquals(allSets.joinToString("").toSet(), seen)
    }

    @Test
    fun consecutiveCallsGiveDifferentPasswords() {
        val rng = Random(3)
        assertNotEquals(generatePassword(32, allSets, rng), generatePassword(32, allSets, rng))
    }

    @Test
    fun characterSetsAreDisjointPrintableAscii() {
        val all = allSets.joinToString("")
        assertEquals(all.length, all.toSet().size)
        assertTrue(all.all { it.code in 0x21..0x7E })
        assertEquals(26, PASSWORD_UPPER.length)
        assertEquals(26, PASSWORD_LOWER.length)
        assertEquals(10, PASSWORD_DIGITS.length)
        assertTrue(PASSWORD_UPPER.all { it in 'A'..'Z' })
        assertTrue(PASSWORD_LOWER.all { it in 'a'..'z' })
        assertTrue(PASSWORD_DIGITS.all { it in '0'..'9' })
        assertTrue(PASSWORD_SYMBOLS.none { it.isLetterOrDigit() })
    }

    private fun assertEveryClassPresent(pw: String, sets: List<String>) {
        for (set in sets) {
            assertTrue("password lacks a character from $set: $pw", pw.any { it in set })
        }
    }
}
