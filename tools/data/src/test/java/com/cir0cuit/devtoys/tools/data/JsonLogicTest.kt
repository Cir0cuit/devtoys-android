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
    fun errorColumnPointsAtTheRejectedCharacter() {
        fun at(input: String) = describeJsonError(errorOf { parseJson(input) }, input).substringBefore(":")
        assertEquals("Line 2, column 1", at("{\nx"))           // unquoted key, read before it is rejected
        assertEquals("Line 2, column 1", at("[1,\nx]"))        // unquoted value, reported where it starts
        assertEquals("Line 2, column 3", at("{\"a\":1\n  x}")) // unterminated object
        assertEquals("Line 1, column 4", at("{} x"))           // trailing content
        assertEquals("Line 1, column 6", at("{\"a\":tru}"))
        assertEquals("Line 1, column 2", at("{'a':1}"))
    }

    @Test
    fun strayCloserAfterTheValueIsTrailingContent() {
        for (lenient in listOf(true, false)) for ((input, column) in listOf("{\"a\":1}}" to 8, "{\"a\":1}]" to 8, "[1] :" to 5)) {
            val msg = describeJsonError(errorOf { parseJson(input, lenient) }, input)
            assertEquals("$input lenient=$lenient", "Line 1, column $column: Unexpected content after the JSON value.", msg)
        }
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
    fun yamlUsesTheCoreSchema() {
        val yaml = "on: yes\nver: 1.10\nd: 2024-01-01\nn: ~\nzip: 012\noct: 0o17\nhex: 0x1F\nt: True\nf: FALSE\n" +
            "time: 1:30\ninf: -.inf\nbase: &a {x: 1}\nref: *a\nm:\n  <<: *a\n  y: 2\n"
        assertEquals(
            parseJson(
                "{\"on\":\"yes\",\"ver\":1.1,\"d\":\"2024-01-01\",\"n\":null,\"zip\":12,\"oct\":15,\"hex\":31,\"t\":true,\"f\":false," +
                    "\"time\":\"1:30\",\"inf\":\"-Infinity\",\"base\":{\"x\":1},\"ref\":{\"x\":1},\"m\":{\"x\":1,\"y\":2}}"
            ),
            parseJson(yamlToJson(yaml, JsonIndent.MINIFY, false))
        )
        assertEquals("{\"on\":\"off\"}", yamlToJson("on: off\n", JsonIndent.MINIFY, false))
    }

    @Test
    fun jsonToYamlQuotesStringsThatYaml11Or12WouldRetype() {
        val json = "{\"a\":\"0o17\",\"b\":\"on\",\"c\":\"012\",\"d\":\"2024-01-01\",\"e\":\"null\",\"f\":\"1:30\"}"
        val yaml = jsonToYaml(json, 2, false, false)
        assertEquals(parseJson(json), parseJson(yamlToJson(yaml, JsonIndent.MINIFY, false)))
        val yaml11 = Yaml().load<Map<String, Any?>>(yaml)
        assertEquals("on", yaml11["b"])
        assertEquals("2024-01-01", yaml11["d"])
    }

    @Test
    fun treeFollowsSortKeys() {
        val (_, tree) = runJsonTool("{\"b\":1,\"a\":{\"d\":2,\"c\":3}}", JsonMode.FORMAT, JsonIndent.TWO, sort = true, lenient = false)
        assertEquals(listOf("a", "b"), tree!!.asJsonObject.keySet().toList())
        assertEquals(listOf("c", "d"), tree.asJsonObject["a"].asJsonObject.keySet().toList())
    }

    @Test
    fun diffErrorDoesNotMentionLenient() {
        val msg = runCatching { compareJson("[1]", "[3,1,2],}", false) }.exceptionOrNull()!!.message!!
        assertTrue(msg, msg.startsWith("Changed JSON, line 1, column ") && !msg.contains("Lenient"))
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
