package com.cir0cuit.devtoys.tools.data

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Expectations follow RFC 4180 (Common Format and MIME Type for CSV Files). */
class CsvParserTest {

    @Test
    fun simpleRows() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parseCsv("a,b\n1,2"))
    }

    @Test
    fun crlfLineBreaks() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parseCsv("a,b\r\n1,2\r\n"))
    }

    @Test
    fun trailingLineBreakDoesNotAddRow() {
        assertEquals(listOf(listOf("a", "b")), parseCsv("a,b\n"))
    }

    @Test
    fun emptyInputHasNoRows() {
        assertTrue(parseCsv("").isEmpty())
    }

    @Test
    fun emptyFieldsArePreserved() {
        assertEquals(listOf(listOf("a", "", "c")), parseCsv("a,,c"))
        assertEquals(listOf(listOf("a", "b", "")), parseCsv("a,b,"))
        assertEquals(listOf(listOf("a", "b", "")), parseCsv("a,b,\n"))
        assertEquals(listOf(listOf("", "")), parseCsv(","))
    }

    @Test
    fun quotedFieldWithComma() {
        assertEquals(listOf(listOf("a,b", "c")), parseCsv("\"a,b\",c"))
    }

    @Test
    fun doubledQuoteIsEscapedQuote() {
        assertEquals(listOf(listOf("say \"hi\"", "x")), parseCsv("\"say \"\"hi\"\"\",x"))
    }

    @Test
    fun emptyQuotedField() {
        assertEquals(listOf(listOf("", "x")), parseCsv("\"\",x"))
        assertEquals(listOf(listOf("a", "")), parseCsv("a,\"\""))
    }

    @Test
    fun embeddedLineBreakInsideQuotesStaysInField() {
        val rows = parseCsv("id,text\n1,\"line1\nline2\"\n2,plain")
        assertEquals(3, rows.size)
        assertEquals(listOf("1", "line1\nline2"), rows[1])
        assertEquals(listOf("2", "plain"), rows[2])
    }

    @Test
    fun embeddedCrlfInsideQuotesIsPreserved() {
        assertEquals(listOf(listOf("a\r\nb")), parseCsv("\"a\r\nb\""))
    }

    @Test
    fun strayQuoteInsideUnquotedFieldIsLiteral() {
        assertEquals(listOf(listOf("5\" pipe", "x")), parseCsv("5\" pipe,x"))
        assertEquals(listOf(listOf("ab\"c")), parseCsv("ab\"c"))
    }

    @Test
    fun unterminatedQuoteKeepsContent() {
        assertEquals(listOf(listOf("abc")), parseCsv("\"abc"))
    }

    @Test
    fun blankLinesAreSkipped() {
        assertEquals(listOf(listOf("a"), listOf("b")), parseCsv("a\n\nb"))
    }

    @Test
    fun fieldsAreNotTrimmed() {
        assertEquals(listOf(listOf(" a ", " b")), parseCsv(" a , b"))
    }

    @Test
    fun markdownEscapesPipeBackslashAndNewline() {
        assertEquals("a\\|b", mdEscape("a|b"))
        assertEquals("c:\\\\dir", mdEscape("c:\\dir"))
        assertEquals("l1<br>l2<br>l3<br>l4", mdEscape("l1\nl2\r\nl3\rl4"))
    }

    @Test
    fun markdownTableHasHeaderSeparatorMatchingColumnCount() {
        val md = toMarkdown(listOf(listOf("h1", "h2", "h3"), listOf("a|b", "x\ny", "")))
        val lines = md.trimEnd('\n').split("\n")
        assertEquals(3, lines.size)
        assertEquals("| h1 | h2 | h3 |", lines[0])
        assertEquals("|---|---|---|", lines[1])
        assertEquals("| a\\|b | x<br>y |  |", lines[2])
    }

    @Test
    fun htmlEscapeCoversAllSpecialCharacters() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;&amp;&#39;", htmlEscape("<a href=\"x\">&'"))
        // & must be escaped first so that existing entities are not double-decoded later
        assertEquals("&amp;lt;", htmlEscape("&lt;"))
    }

    @Test
    fun htmlTableUsesThForHeaderAndEscapesCells() {
        val html = toHtmlTable(listOf(listOf("name"), listOf("<script>")))
        assertEquals(
            "<table>\n  <tr><th>name</th></tr>\n  <tr><td>&lt;script&gt;</td></tr>\n</table>",
            html
        )
    }

    @Test
    fun parsedQuotedFieldsRenderIntoSingleCells() {
        val html = toHtmlTable(parseCsv("h\n\"a,b\""))
        assertTrue(html.contains("<td>a,b</td>"))
    }

    @Test
    fun otherDelimiters() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parseCsv("a;b\n1;2", ';'))
        assertEquals(listOf(listOf("a", "b,c")), parseCsv("a\tb,c", '\t'))
    }

    @Test
    fun delimiterDetection() {
        assertEquals(';', detectCsvDelimiter("a;b;c\n1,2,3,4,5"))
        assertEquals('\t', detectCsvDelimiter("a\tb"))
        assertEquals('|', detectCsvDelimiter("a|b|c"))
        assertEquals(',', detectCsvDelimiter("\"x;y;z\",w"))
        assertEquals(',', detectCsvDelimiter("abc"))
    }

    @Test
    fun withoutHeaderRowColumnNamesAreAdded() {
        assertEquals(listOf(listOf("Column 1", "Column 2"), listOf("1", "2")), readCsvTable("1,2", null, false).rows)
        assertEquals(';', readCsvTable("a;b", null, true).delimiter)
    }

    @Test
    fun csvOutputQuotesPerRfc4180AndRoundTrips() {
        val rows = listOf(listOf("a,b", "say \"hi\"", "l1\nl2", "plain"))
        assertEquals("\"a,b\",\"say \"\"hi\"\"\",\"l1\nl2\",plain", toCsv(rows))
        assertEquals(rows, parseCsv(toCsv(rows)))
    }

    @Test
    fun csvToJsonInfersTypes() {
        val rows = listOf(listOf("n", "b", "s", "e"), listOf("1.5", "true", "007", ""))
        assertEquals(
            JsonParser.parseString("[{\"n\":1.5,\"b\":true,\"s\":\"007\",\"e\":null}]"),
            JsonParser.parseString(csvToJson(rows, inferTypes = true))
        )
        assertEquals(
            JsonParser.parseString("[{\"n\":\"1.5\",\"b\":\"true\",\"s\":\"007\",\"e\":\"\"}]"),
            JsonParser.parseString(csvToJson(rows, inferTypes = false))
        )
    }

    @Test
    fun jsonObjectsFlattenToRows() {
        val rows = jsonToCsvRows("[{\"a\":1,\"b\":{\"c\":\"x\"}},{\"a\":2,\"d\":[1,2],\"n\":null}]")
        assertEquals(listOf(listOf("a", "b.c", "d", "n"), listOf("1", "x", "", ""), listOf("2", "", "[1,2]", "")), rows)
        assertEquals(listOf(listOf("h1", "h2"), listOf("1", "2")), jsonToCsvRows("[[\"h1\",\"h2\"],[1,2]]"))
    }

    @Test
    fun jsonInputIsDetectedAndErrorsAreReported() {
        assertTrue(readCsvTable("  [{\"a\":1}]", null, true).fromJson)
        assertTrue(readCsvTable("[1,", null, true).error != null)
    }

    @Test
    fun sqlInsertEscapesQuotesAndNames() {
        val rows = listOf(listOf("id", "full name"), listOf("1", "O'Brien"), listOf("2", ""))
        assertEquals(
            "INSERT INTO public.people (id, \"full name\") VALUES\n  ('1', 'O''Brien'),\n  ('2', NULL);",
            toSqlInsert(rows, "public.people", emptyAsNull = true)
        )
        assertTrue(toSqlInsert(rows, "", emptyAsNull = false).endsWith("('2', '');"))
    }
}
