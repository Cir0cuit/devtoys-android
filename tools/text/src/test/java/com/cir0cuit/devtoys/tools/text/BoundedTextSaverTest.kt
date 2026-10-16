package com.cir0cuit.devtoys.tools.text

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedTextSaverTest {

    private val scope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    private fun save(text: String): Any? = with(boundedTextSaver) { scope.save(mutableStateOf(text)) }
    private fun save(state: MutableState<String>): Any? = with(boundedTextSaver) { scope.save(state) }
    private fun restore(saved: Any): String? = boundedTextSaver.restore(saved)?.value

    @Test
    fun shortTextIsSavedVerbatim() {
        val saved = save("hello")
        assertEquals("hello", saved)
        assertEquals("hello", restore(saved!!))
    }

    @Test
    fun textAtLimitIsSavedVerbatim() {
        val text = "x".repeat(20_000)
        val saved = save(text)
        assertTrue(saved is String)
        assertEquals(text, restore(saved!!))
    }

    @Test
    fun textOverLimitIsNotPutIntoTheBundleButStillRestores() {
        val text = "y".repeat(20_001)
        val saved = save(text)
        assertTrue("expected a handle, got ${saved?.javaClass}", saved is Long)
        assertEquals(text, restore(saved!!))
    }

    @Test
    fun savingSameLargeInstanceTwiceReusesHandle() {
        val state = mutableStateOf("z".repeat(30_000))
        val first = save(state)
        val second = save(state)
        assertEquals(first, second)
    }

    @Test
    fun recentLargeTextsSurviveWhileOldestAreEvicted() {
        val texts = (0 until 9).map { i -> "$i".repeat(20_001) }
        val handles = texts.map { save(it)!! }
        for (i in 1 until 9) assertEquals(texts[i], restore(handles[i]))
        // Bounded cache: the oldest of 9 entries is dropped and restores as empty, never crashes.
        assertEquals("", restore(handles[0]))
    }

    @Test
    fun emptyTextRoundTrips() {
        assertEquals("", restore(save("")!!))
    }

    @Test
    fun nonAsciiTextRoundTrips() {
        val text = "zażółć 😀\r\n"
        assertEquals(text, restore(save(text)!!))
    }
}
