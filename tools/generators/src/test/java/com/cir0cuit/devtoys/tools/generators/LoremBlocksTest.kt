package com.cir0cuit.devtoys.tools.generators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LoremBlocksTest {
    @Test
    fun charactersAreExact() {
        for (n in listOf(1, 10, 255, 4096, 20_000)) {
            assertEquals(n, loremBlocks(LoremUnit.CHARACTERS, n, true, Random(n)).single().length)
        }
    }

    @Test
    fun wordsCountAndClassicStart() {
        val text = loremBlocks(LoremUnit.WORDS, 12, true, Random(1)).single()
        assertEquals(12, text.split(" ").size)
        assertTrue(text.startsWith("Lorem ipsum dolor sit amet consectetur"))
        assertFalse(loremBlocks(LoremUnit.WORDS, 12, false, Random(1)).single().startsWith("Lorem ipsum dolor sit amet"))
    }

    @Test
    fun paragraphsAndFormats() {
        val blocks = loremBlocks(LoremUnit.PARAGRAPHS, 3, true, Random(2))
        assertEquals(3, blocks.size)
        assertEquals(3, formatLorem(blocks, LoremFormat.HTML).lines().count { it.startsWith("<p>") && it.endsWith("</p>") })
        val json = formatLorem(blocks, LoremFormat.JSON)
        assertTrue(json.startsWith("[") && json.endsWith("]"))
        assertEquals(6, json.count { it == '"' })
        assertEquals(2, formatLorem(blocks, LoremFormat.PLAIN).split("\n\n").size - 1)
    }

    @Test
    fun zeroCountIsEmpty() {
        assertTrue(loremBlocks(LoremUnit.SENTENCES, 0, true, Random(3)).isEmpty())
    }
}
