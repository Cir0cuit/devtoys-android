package com.cir0cuit.devtoys.tools.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlFormatterTest {

    @Test
    fun clausesGetOwnLineWithIndentedContent() {
        assertEquals(
            "SELECT\n  a,\n  b\nFROM\n  t\nWHERE\n  x = 1\n  AND y = 2\nORDER BY\n  a",
            formatSql("SELECT a, b FROM t WHERE x = 1 AND y = 2 ORDER BY a")
        )
    }

    @Test
    fun keywordCaseIsPreservedByDefault() {
        assertEquals("select\n  a\nfrom\n  t", formatSql("select a from t"))
    }

    @Test
    fun keywordCaseCanBeChangedWithoutTouchingIdentifiers() {
        assertEquals("SELECT\n  a\nFROM\n  t", formatSql("select a from t", keywordCase = SqlKeywordCase.UPPER))
        assertEquals("select\n  A\nfrom\n  T", formatSql("SELECT A FROM T", keywordCase = SqlKeywordCase.LOWER))
        assertEquals("SELECT 'from' FROM t", minifySql("select 'from' from t", SqlKeywordCase.UPPER))
    }

    @Test
    fun multiWordKeywordWhitespaceIsCollapsed() {
        assertEquals("SELECT\n  a\nFROM\n  t\nGROUP BY\n  a", formatSql("SELECT a FROM t GROUP \n  BY a"))
    }

    @Test
    fun joinsAreIndentedUnderFrom() {
        assertEquals(
            "SELECT\n  *\nFROM\n  a\n  LEFT JOIN b ON a.id = b.id\n  INNER JOIN c ON c.id = a.id\n  LEFT OUTER JOIN d ON d.id = a.id",
            formatSql("SELECT * FROM a LEFT JOIN b ON a.id = b.id INNER JOIN c ON c.id = a.id LEFT OUTER JOIN d ON d.id = a.id")
        )
    }

    @Test
    fun subqueriesNest() {
        assertEquals(
            "SELECT\n  *\nFROM\n  (\n    SELECT\n      a\n    FROM\n      t\n  ) x\nWHERE\n  id IN (\n    SELECT\n      id\n    FROM\n      u\n  )",
            formatSql("SELECT * FROM (SELECT a FROM t) x WHERE id IN (SELECT id FROM u)")
        )
    }

    @Test
    fun functionCallsAndValueListsStayInline() {
        assertEquals(
            "SELECT\n  COUNT(*),\n  MAX(a)\nFROM\n  t\nWHERE\n  a IN (1, 2)\n  AND b BETWEEN 1 AND 5",
            formatSql("SELECT COUNT(*), MAX(a) FROM t WHERE a IN (1,2) AND b BETWEEN 1 AND 5")
        )
    }

    @Test
    fun clauseKeywordsInsideFunctionParensDoNotBreak() {
        assertEquals("SELECT\n  EXTRACT(YEAR FROM d)\nFROM\n  t", formatSql("SELECT EXTRACT(YEAR FROM d) FROM t"))
    }

    @Test
    fun caseExpressionIsIndented() {
        assertEquals(
            "SELECT\n  CASE\n    WHEN a = 1 AND b = 2 THEN 'x'\n    ELSE 'y'\n  END AS c\nFROM\n  t",
            formatSql("SELECT CASE WHEN a = 1 AND b = 2 THEN 'x' ELSE 'y' END AS c FROM t")
        )
    }

    @Test
    fun insertWithMultipleRows() {
        assertEquals(
            "INSERT INTO\n  t (a, b)\nVALUES\n  (1, 'x'),\n  (2, 'y')",
            formatSql("INSERT INTO t (a, b) VALUES (1, 'x'), (2, 'y')")
        )
    }

    @Test
    fun updateSetAndReturning() {
        assertEquals(
            "UPDATE\n  t\nSET\n  a = 1,\n  b = 2\nWHERE\n  id = 3\nRETURNING\n  id",
            formatSql("UPDATE t SET a = 1, b = 2 WHERE id = 3 RETURNING id")
        )
    }

    @Test
    fun cteAndUnion() {
        assertEquals(
            "WITH\n  x AS (\n    SELECT\n      1\n  )\nSELECT\n  *\nFROM\n  x\nUNION ALL\nSELECT\n  2",
            formatSql("WITH x AS (SELECT 1) SELECT * FROM x UNION ALL SELECT 2")
        )
    }

    @Test
    fun statementsAreSeparatedByBlankLine() {
        assertEquals("SELECT\n  1;\n\nDELETE FROM\n  t;", formatSql("SELECT 1; DELETE FROM t;"))
    }

    @Test
    fun indentWidthFour() {
        assertEquals("SELECT\n    a\nFROM\n    t", formatSql("SELECT a FROM t", indentWidth = 4))
    }

    @Test
    fun keywordsInsideStringLiteralsAreUntouched() {
        assertEquals("SELECT\n  'select from where'\nFROM\n  t", formatSql("SELECT 'select from where' FROM t"))
        assertEquals("SELECT\n  'it''s from here'\nFROM\n  t", formatSql("SELECT 'it''s from here' FROM t"))
    }

    @Test
    fun keywordsInsideQuotedIdentifiersAreUntouched() {
        assertEquals("SELECT\n  \"from\",\n  `where`,\n  [and]\nFROM\n  t", formatSql("SELECT \"from\", `where`, [and] FROM t"))
    }

    @Test
    fun keywordsInsideCommentsAreUntouched() {
        assertEquals("SELECT\n  a /* from where */\nFROM\n  t", formatSql("SELECT a /* from where */ FROM t"))
        assertEquals("SELECT\n  a -- from where\nFROM\n  t", formatSql("SELECT a -- from where\nFROM t"))
    }

    @Test
    fun keywordSubstringsInIdentifiersAreNotSplit() {
        assertEquals(
            "SELECT\n  orders,\n  android,\n  fromage,\n  sel_or\nFROM\n  t",
            formatSql("SELECT orders, android, fromage, sel_or FROM t")
        )
    }

    @Test
    fun leadingWhitespaceIsTrimmed() {
        assertEquals("SELECT\n  1", formatSql("   SELECT 1"))
    }

    @Test
    fun minifyCollapsesWhitespaceAndDropsComments() {
        assertEquals(
            "SELECT a, b FROM t WHERE x = 'a  b';",
            minifySql("SELECT a, -- c\n  b /* x */ FROM t\nWHERE  x = 'a  b';")
        )
        assertEquals("SELECT a b", minifySql("SELECT a/*x*/b"))
    }

    @Test
    fun unterminatedLiteralRunsToTheEndAndIsReported() {
        assertEquals("select\n  'abc from t where (x", formatSql("select 'abc from t where (x"))
        assertEquals("select 'a  from", minifySql("select 'a  from"))
        assertTrue(hasUnterminatedSql("select 'abc from t"))
        assertTrue(hasUnterminatedSql("select \"a from t"))
        assertTrue(hasUnterminatedSql("select 'it''s"))
        assertTrue(hasUnterminatedSql("select 1 /* note"))
        assertFalse(hasUnterminatedSql("select 'it''s', \"a\", [b] from t /* c */ -- d"))
    }
}
