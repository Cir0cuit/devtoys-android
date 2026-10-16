package com.cir0cuit.devtoys.tools.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlFormatterTest {

    @Test
    fun breaksBeforeClauseKeywords() {
        assertEquals(
            "SELECT a, b\nFROM t\nWHERE x = 1\nAND y = 2\nORDER BY a",
            formatSql("SELECT a, b FROM t WHERE x = 1 AND y = 2 ORDER BY a")
        )
    }

    @Test
    fun keywordCaseIsPreserved() {
        assertEquals("select a\nfrom t", formatSql("select a from t"))
    }

    @Test
    fun multiWordKeywordWhitespaceIsCollapsed() {
        assertEquals("SELECT a\nFROM t\nGROUP BY a", formatSql("SELECT a FROM t GROUP \n  BY a"))
    }

    @Test
    fun joinsStartNewLine() {
        assertEquals(
            "SELECT *\nFROM a\nLEFT JOIN b ON a.id = b.id\nINNER JOIN c ON c.id = a.id",
            formatSql("SELECT * FROM a LEFT JOIN b ON a.id = b.id INNER JOIN c ON c.id = a.id")
        )
    }

    @Test
    fun keywordsInsideStringLiteralsAreUntouched() {
        assertEquals("SELECT 'select from where'\nFROM t", formatSql("SELECT 'select from where' FROM t"))
    }

    @Test
    fun escapedQuoteInStringLiteral() {
        assertEquals("SELECT 'it''s from here'\nFROM t", formatSql("SELECT 'it''s from here' FROM t"))
    }

    @Test
    fun keywordsInsideQuotedIdentifiersAreUntouched() {
        assertEquals("SELECT \"from\", `where`, [and]\nFROM t", formatSql("SELECT \"from\", `where`, [and] FROM t"))
    }

    @Test
    fun keywordsInsideCommentsAreUntouched() {
        assertEquals("SELECT a /* from where */\nFROM t", formatSql("SELECT a /* from where */ FROM t"))
        assertEquals("SELECT a -- from where\nFROM t", formatSql("SELECT a -- from where\nFROM t"))
    }

    @Test
    fun keywordSubstringsInIdentifiersAreNotSplit() {
        assertEquals("SELECT orders, android, fromage, sel_or\nFROM t", formatSql("SELECT orders, android, fromage, sel_or FROM t"))
    }

    @Test
    fun leadingWhitespaceIsTrimmed() {
        assertEquals("SELECT 1", formatSql("   SELECT 1"))
    }
}
