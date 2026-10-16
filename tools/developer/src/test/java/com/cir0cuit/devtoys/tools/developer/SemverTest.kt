package com.cir0cuit.devtoys.tools.developer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemverTest {
    private fun v(s: String) = parseSemver(s)!!
    private fun ok(version: String, range: String, cargo: Boolean = false) = rangeFailure(v(version), parseRange(range, cargo)) == null

    @Test fun parses() {
        assertEquals(Semver(1, 2, 3, listOf("rc", "1"), "build.5"), parseSemver("v1.2.3-rc.1+build.5"))
        assertNull(parseSemver("1.2"))
        assertNull(parseSemver("01.2.3"))
        assertNull(parseSemver("1.2.3-01"))
        assertNull(parseSemver("1.2.3-"))
    }

    @Test fun specPrecedenceOrder() {
        // The example chain from semver.org section 11.
        val chain = listOf("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0", "2.0.0", "2.1.0", "2.1.1")
        assertEquals(chain, sortVersions(chain.reversed()).first.map { it.toString() })
        assertEquals(0, v("1.0.0+a").compareTo(v("1.0.0+b")))
        assertTrue(v("1.10.0") > v("1.9.0"))
    }

    @Test fun sortReportsInvalidLines() {
        val (sorted, bad) = sortVersions(listOf("2.0.0", "nope", "1.0.0", ""), descending = true)
        assertEquals(listOf("2.0.0", "1.0.0"), sorted.map { it.toString() })
        assertEquals(listOf("nope"), bad)
    }

    @Test fun explains() {
        assertEquals("1.2.3 < 1.10.0" to "Minor 2 < 10", explainCompare(v("1.2.3"), v("1.10.0")))
        assertEquals("A pre-release sorts before its release", explainCompare(v("1.0.0-rc.1"), v("1.0.0")).second)
        assertTrue(explainCompare(v("1.0.0-1"), v("1.0.0-a")).second.contains("numbers sort before text"))
    }

    @Test fun caretAndTilde() {
        assertTrue(ok("1.9.9", "^1.2.3"))
        assertTrue(!ok("2.0.0", "^1.2.3"))
        assertTrue(ok("0.2.9", "^0.2.3"))
        assertTrue(!ok("0.3.0", "^0.2.3"))
        assertTrue(!ok("0.0.4", "^0.0.3"))
        assertTrue(ok("1.2.9", "~1.2.3"))
        assertTrue(!ok("1.3.0", "~1.2"))
        assertEquals(listOf(listOf(RangeComparator(">=", v("1.2.3")), RangeComparator("<", v("2.0.0-0")))), parseRange("^1.2.3"))
    }

    @Test fun xRangesHyphensAndOr() {
        assertTrue(ok("1.5.0", "1.x"))
        assertTrue(!ok("2.0.0", "1.x"))
        assertTrue(ok("3.0.0", "*"))
        assertTrue(ok("2.3.9", "1.2.3 - 2.3"))
        assertTrue(!ok("2.4.0", "1.2.3 - 2.3"))
        assertTrue(ok("3.1.0", ">=1.0 <2.0 || >=3"))
        assertTrue(!ok("2.5.0", ">=1.0 <2.0 || >=3"))
        assertTrue(ok("1.3.0", ">1.2"))
        assertTrue(!ok("1.2.9", ">1.2"))
        assertTrue(ok("1.2.9", "<=1.2"))
    }

    @Test fun preReleasesNeedAMatchingComparator() {
        assertTrue(!ok("1.5.0-beta", "^1.2.3"))
        assertTrue(ok("1.2.4-beta", "^1.2.4-alpha"))
        assertTrue(rangeFailure(v("1.5.0-beta"), parseRange("^1.2.3"))!!.startsWith("Pre-release excluded"))
        assertEquals("Fails <2.0.0-0", rangeFailure(v("2.0.0"), parseRange("^1.2.3")))
    }

    @Test fun cargoBareVersionIsCaret() {
        assertTrue(ok("1.9.0", "1.2.3", cargo = true))
        assertTrue(!ok("1.9.0", "1.2.3"))
        assertTrue(ok("1.4.0", ">= 1.2, < 1.5", cargo = true))
        assertTrue(!ok("1.3.0", "1.2.*", cargo = true))
        assertTrue(ok("1.2.3", "=1.2.3", cargo = true))
    }
}
