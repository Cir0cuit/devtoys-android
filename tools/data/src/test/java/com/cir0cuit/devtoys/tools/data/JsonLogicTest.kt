package com.cir0cuit.devtoys.tools.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.yaml.snakeyaml.Yaml

class JsonLogicTest {

    private fun errorOf(block: () -> Unit): Throwable {
        try { block() } catch (e: Throwable) { return e }
        fail("expected an exception")
        throw AssertionError()
    }

    @Test
    fun sortKeysAndTabIndent() {
        val out = formatJson(parseJson("{\"b\":1,\"a\":{\"d\":2,\"c\":3}}"), JsonIndent.TAB, sort = true)
        assertEquals("{\n\t\"a\": {\n\t\t\"c\": 3,\n\t\t\"d\": 2\n\t},\n\t\"b\": 1\n}", out)
        assertEquals("{\"a\":[1,2.50]}", formatJson(parseJson("{ \"a\" : [ 1, 2.50 ] }"), JsonIndent.MINIFY))
    }

    @Test
    fun lenientAcceptsCommentsAndSingleQuotes() {
        val input = "{\n  // comment\n  'a': 'x', b: 1\n}"
        assertEquals("{\"a\":\"x\",\"b\":1}", formatJson(parseJson(input, lenient = true), JsonIndent.MINIFY))
        val msg = describeJsonError(errorOf { parseJson(input) })
        assertTrue(msg, msg.startsWith("Line 2, column "))
    }

    @Test
    fun errorsNameLineAndColumn() {
        val msg = describeJsonError(errorOf { parseJson("{\n\"a\":\n}") })
        assertTrue(msg, msg.startsWith("Line 3, column "))
        assertFalse(msg, msg.contains("http"))
        val trailing = describeJsonError(errorOf { parseJson("{} []", lenient = true) })
        assertTrue(trailing, trailing.startsWith("Line 1, column ") && trailing.contains("Unexpected content"))
        val strictTrailing = runCatching { runJsonTool("{} x", JsonMode.FORMAT, JsonIndent.TWO, false, false) }.exceptionOrNull()!!.message!!
        assertTrue(strictTrailing, strictTrailing.startsWith("Line 1, column ") && strictTrailing.contains("Lenient"))
    }

    @Test
    fun escapeAndUnescape() {
        val text = "{\"a\":\"x\"}\n<b>"
        val escaped = escapeJsonString(text)
        assertEquals("\"{\\\"a\\\":\\\"x\\\"}\\n<b>\"", escaped)
        assertEquals(text, unescapeJsonString(escaped))
        assertEquals("a\tb", unescapeJsonString("a\\tb"))
        val (out, tree) = runJsonTool("\"{\\\"a\\\":1}\"", JsonMode.UNESCAPE, JsonIndent.TWO, false, false)
        assertEquals("{\n  \"a\": 1\n}", out)
        assertTrue(tree!!.isJsonObject)
    }

    @Test
    fun jsonPaths() {
        assertEquals("$.ok_1", childPath("$", "ok_1"))
        assertEquals("$['a b']", childPath("$", "a b"))
        assertEquals("$['it\\'s']", childPath("$", "it's"))
        assertEquals("$.a[2]", childPath("$.a", 2))
    }

    @Test
    fun treeRowsFollowExpansion() {
        val root = parseJson("{\"a\":{\"b\":1},\"c\":[1,2]}")
        assertEquals(listOf("$", "$.a", "$.c"), jsonRows(root, { _, d -> d < 1 }, 100).map { it.path })
        val all = jsonRows(root, { _, _ -> true }, 100)
        assertEquals(listOf("$", "$.a", "$.a.b", "$.c", "$.c[0]", "$.c[1]"), all.map { it.path })
        assertEquals("[1]", all.last().label)
        assertEquals("[2]", summarize(all[3].value))
        assertEquals(2, jsonRows(root, { _, _ -> true }, 2).size)
    }

    @Test
    fun yamlMultiDocumentAndExactNumbers() {
        assertEquals("[{\"a\":1},{\"b\":2}]", yamlToJson("a: 1\n---\nb: 2\n", JsonIndent.MINIFY, false))
        assertEquals(
            "{\"a\":\"http://x/y\",\"n\":12345678901234567890,\"d\":0.1}",
            yamlToJson("a: http://x/y\nn: 12345678901234567890\nd: 0.1\n", JsonIndent.MINIFY, false)
        )
        assertEquals("{\"a\":2,\"b\":1}", yamlToJson("b: 1\na: 2\n", JsonIndent.MINIFY, true))
        assertEquals("", yamlToJson("", JsonIndent.MINIFY, false))
    }

    @Test
    fun yamlErrorsNameLineAndColumn() {
        val msg = describeYamlError(errorOf { yamlToJson("a: b: c\n", JsonIndent.TWO, false) })
        assertTrue(msg, msg.startsWith("Line "))
    }

    @Test
    fun jsonToYamlIndentAndDocuments() {
        assertEquals("a:\n    b: 1\n", jsonToYaml("{\"a\":{\"b\":1}}", 4, false, false))
        val docs = jsonToYaml("[{\"a\":1},{\"b\":2}]", 2, false, true)
        assertTrue(docs, docs.contains("---"))
        assertEquals(listOf(mapOf("a" to 1), mapOf("b" to 2)), Yaml().loadAll(docs).toList())
        assertEquals("a: 1\nb: 2\n", jsonToYaml("{\"b\":2,\"a\":1}", 2, true, false))
    }

    @Test
    fun structuralDiff() {
        val a = parseJson("{\"name\":\"x\",\"tags\":[\"j\"],\"stars\":42,\"gone\":true,\"v\":1}")
        val b = parseJson("{\"stars\":43,\"v\":1.0,\"name\":\"x\",\"tags\":[\"j\",\"d\"],\"new\":null}")
        val changes = diffJson(a, b, ignoreArrayOrder = false)
        assertEquals(
            "+ $.tags[1]: \"d\"\n~ $.stars: 42 -> 43\n- $.gone: true\n+ $.new: null",
            describeChanges(changes)
        )
        assertEquals("2 added, 1 removed, 1 changed", changeSummary(changes))
        assertTrue(diffJson(parseJson("{\"a\":1,\"b\":[2]}"), parseJson("{\"b\":[2],\"a\":1}"), false).isEmpty())
        assertEquals("No differences", changeSummary(emptyList()))
    }

    @Test
    fun diffArrayOrderAndPrecision() {
        assertTrue(diffJson(parseJson("[1,2,3]"), parseJson("[3,1,2]"), true).isEmpty())
        assertEquals(3, diffJson(parseJson("[1,2,3]"), parseJson("[3,1,2]"), false).size)
        assertEquals("- $[0]: 1\n+ $[1]: 3", describeChanges(diffJson(parseJson("[1,2]"), parseJson("[2,3]"), true)))
        assertEquals(1, diffJson(parseJson("9007199254740993"), parseJson("9007199254740992"), false).size)
        val (summary, _) = compareJson("{\"a\":[{\"x\":1},{\"y\":2}]}", "{\"a\":[{\"y\":2},{\"x\":1.0}]}", true)
        assertEquals("No differences", summary)
    }
}
