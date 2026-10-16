package com.cir0cuit.devtoys.tools.developer

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uses the real org.json artifact declared by the module (on device the platform copy is used).
 * Numeric decimals are deliberately avoided: the two org.json implementations map them to
 * different Java types.
 */
class JsonToTypesTest {
    private val tool = JsonToTypesTool()

    private fun gen(json: String, root: String = "Root") =
        tool.generateKotlinClass(root, JSONObject(json), mutableSetOf(root))

    // ---- identifiers ----

    @Test fun propertyNamesAreLowerCamelCase() {
        assertEquals("firstName", tool.toPropertyName("first_name"))
        assertEquals("userId", tool.toPropertyName("user-id"))
        assertEquals("fooBarBaz", tool.toPropertyName("foo bar.baz"))
        assertEquals("already", tool.toPropertyName("already"))
        assertEquals("ref", tool.toPropertyName("\$ref"))
    }

    @Test fun propertyNamesNeverStartWithDigit() {
        assertEquals("_123abc", tool.toPropertyName("123abc"))
    }

    @Test fun propertyNameFallbackForNoIdentifierChars() {
        assertEquals("field", tool.toPropertyName(""))
        assertEquals("field", tool.toPropertyName("!!!"))
    }

    @Test fun classNamesAreUpperCamelCase() {
        assertEquals("FirstName", tool.toClassName("first_name"))
        assertEquals("_2fa", tool.toClassName("2fa"))
        assertEquals("Field", tool.toClassName("--"))
    }

    @Test fun uniqueNameAppendsCounter() {
        val used = mutableSetOf("A")
        assertEquals("A2", tool.uniqueName("A", used))
        assertEquals("A3", tool.uniqueName("A", used))
        assertEquals("B", tool.uniqueName("B", used))
    }

    // ---- depth guard ----

    @Test fun maxDepthCountsNesting() {
        assertEquals(0, tool.maxDepth(""))
        assertEquals(1, tool.maxDepth("{}"))
        assertEquals(3, tool.maxDepth("[[[]]]"))
        assertEquals(2, tool.maxDepth("{\"a\":[1],\"b\":{}}"))
    }

    @Test fun maxDepthIgnoresBracketsInStrings() {
        assertEquals(1, tool.maxDepth("{\"a\":\"[[[{{{\"}"))
        assertEquals(1, tool.maxDepth("{\"a\":\"\\\"[[[\"}"))
        assertEquals(1, tool.maxDepth("{\"a\\\\\":\"x\"}"))
    }

    // ---- element types ----

    @Test fun elementTypes() {
        assertEquals("Int", tool.elementType(JSONArray("[1,2]")))
        assertEquals("Long", tool.elementType(JSONArray("[12345678901]")))
        assertEquals("String", tool.elementType(JSONArray("[\"a\"]")))
        assertEquals("Boolean", tool.elementType(JSONArray("[true]")))
        assertEquals("List<Int>", tool.elementType(JSONArray("[[1]]")))
        assertEquals("List<Any>", tool.elementType(JSONArray("[[]]")))
        assertEquals("Any", tool.elementType(JSONArray("[]")))
        assertEquals("Any", tool.elementType(JSONArray("[null]")))
    }

    // ---- class generation ----

    @Test fun basicTypes() {
        val out = gen("{\"id\":1,\"name\":\"x\",\"ok\":true,\"big\":12345678901,\"nothing\":null}")
        assertTrue(out, out.startsWith("data class Root(\n"))
        assertTrue(out, out.contains("    val id: Int,\n"))
        assertTrue(out, out.contains("    val name: String,\n"))
        assertTrue(out, out.contains("    val ok: Boolean,\n"))
        assertTrue(out, out.contains("    val big: Long,\n"))
        assertTrue(out, out.contains("    val nothing: Any? /* null */,\n"))
    }

    @Test fun renamedPropertyGetsSerialName() {
        val out = gen("{\"first_name\":\"x\"}")
        assertTrue(out, out.contains("    @SerialName(\"first_name\")\n    val firstName: String,"))
    }

    @Test fun unchangedPropertyHasNoSerialName() {
        assertFalse(gen("{\"name\":\"x\"}").contains("@SerialName"))
    }

    @Test fun hardKeywordIsBacktickQuoted() {
        val out = gen("{\"class\":\"x\",\"val\":1}")
        assertTrue(out, out.contains("val `class`: String,"))
        assertTrue(out, out.contains("val `val`: Int,"))
        assertFalse(out, out.contains("@SerialName"))
    }

    @Test fun serialNameEscapesDollarQuoteAndBackslash() {
        assertTrue(gen("{\"\$ref\":\"x\"}").contains("@SerialName(\"\\\$ref\")"))
        assertTrue(gen("{\"a\\\"b\":\"x\"}").contains("@SerialName(\"a\\\"b\")"))
        assertTrue(gen("{\"a\\\\b\":\"x\"}").contains("@SerialName(\"a\\\\b\")"))
    }

    @Test fun serialNameEscapesControlCharacters() {
        // JSON key "a<LF>b" must become a valid single-line Kotlin string literal "a\nb".
        val out = gen("{\"a\\nb\":\"x\"}")
        assertTrue(out, out.contains("@SerialName(\"a\\nb\")"))
    }

    @Test fun collidingPropertyNamesAreMadeUnique() {
        val out = gen("{\"a_b\":1,\"aB\":2}")
        assertTrue(out, out.contains("val aB: Int,"))
        assertTrue(out, out.contains("val aB2: Int,"))
    }

    @Test fun nestedObjectGetsOwnClass() {
        val out = gen("{\"data\":{\"value\":5}}")
        assertTrue(out, out.contains("val data: Data,"))
        assertTrue(out, out.contains("data class Data(\n    val value: Int,\n)"))
    }

    @Test fun nestedClassDoesNotCollideWithRoot() {
        val out = gen("{\"root\":{\"a\":1}}")
        assertTrue(out, out.contains("val root: Root2,"))
        assertTrue(out, out.contains("data class Root2("))
    }

    @Test fun sameKeyAtDifferentLevelsGetsDistinctClasses() {
        val out = gen("{\"a\":{\"b\":{\"x\":1}},\"b\":{\"y\":\"s\"}}")
        assertEquals(1, Regex("data class B\\(").findAll(out).count())
        assertEquals(1, Regex("data class B2\\(").findAll(out).count())
    }

    @Test fun arrayOfObjects() {
        val out = gen("{\"items\":[{\"id\":1}]}")
        assertTrue(out, out.contains("val items: List<ItemsItem>,"))
        assertTrue(out, out.contains("data class ItemsItem(\n    val id: Int,\n)"))
    }

    @Test fun arrayOfPrimitives() {
        val out = gen("{\"tags\":[\"a\"],\"nums\":[1],\"empty\":[]}")
        assertTrue(out, out.contains("val tags: List<String>,"))
        assertTrue(out, out.contains("val nums: List<Int>,"))
        assertTrue(out, out.contains("val empty: List<Any>,"))
    }

    @Test fun generatedClassesDoNotShadowKotlinBuiltins() {
        // A nested class named String/List/Int would shadow kotlin.String etc. for every other
        // property in the same file, so `val name: String` would refer to the generated class.
        val out = gen("{\"string\":{\"a\":1},\"list\":{\"b\":2},\"name\":\"x\"}")
        assertFalse(out, out.contains("data class String("))
        assertFalse(out, out.contains("data class List("))
    }

    @Test fun emptyObjectDoesNotProduceParameterlessDataClass() {
        // Kotlin: "Data class must have at least one primary constructor parameter".
        val out = gen("{\"meta\":{}}")
        assertFalse(out, Regex("data class \\w+\\(\\s*\\)").containsMatchIn(out))
    }
}
