package com.cir0cuit.devtoys.tools.generators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LoremIpsumTest {

    private val classicOpening = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

    private fun sentences(text: String): List<String> =
        Regex("[^.]+\\.").findAll(text).map { it.value.trim() }.toList()

    @Test
    fun zeroSentencesIsEmpty() {
        assertEquals("", generateLorem(0, Random(1)))
    }

    @Test
    fun singleSentenceIsClassicOpening() {
        assertEquals(classicOpening, generateLorem(1, Random(1)))
    }

    @Test
    fun startsWithClassicOpening() {
        assertTrue(generateLorem(10, Random(2)).startsWith("$classicOpening "))
    }

    @Test
    fun producesRequestedNumberOfSentences() {
        for (n in 1..50) {
            assertEquals(n, sentences(generateLorem(n, Random(n))).size)
        }
    }

    @Test
    fun sentencesSeparatedBySingleSpaceWithoutTrailingWhitespace() {
        val text = generateLorem(20, Random(3))
        assertEquals(text.trim(), text)
        assertFalse(text.contains("  "))
        assertTrue(text.endsWith("."))
    }

    @Test
    fun generatedSentencesAreWellFormed() {
        val vocabulary = LOREM_WORDS.toSet()
        val body = sentences(generateLorem(50, Random(4))).drop(1)
        assertEquals(49, body.size)
        for (s in body) {
            assertTrue("not capitalised: $s", s[0].isUpperCase())
            val words = s.removeSuffix(".").split(" ")
            assertTrue("word count ${words.size} in: $s", words.size in 6..15)
            for (w in words) assertTrue("unknown word $w", w.lowercase() in vocabulary)
            assertTrue("only first letter capitalised: $s", words.drop(1).all { it == it.lowercase() })
        }
    }

    @Test
    fun sameSeedIsDeterministic() {
        assertEquals(generateLorem(10, Random(77)), generateLorem(10, Random(77)))
    }

    @Test
    fun differentSeedsDiffer() {
        assertNotEquals(generateLorem(10, Random(1)), generateLorem(10, Random(2)))
    }

    @Test
    fun sentencesAreNotAllIdentical() {
        val body = sentences(generateLorem(10, Random(5))).drop(1)
        assertTrue(body.toSet().size > 1)
    }

    @Test
    fun vocabularyHasNoBlankOrUppercaseEntries() {
        assertTrue(LOREM_WORDS.isNotEmpty())
        assertTrue(LOREM_WORDS.all { w -> w.isNotEmpty() && w.all { it.isLowerCase() } })
    }
}
