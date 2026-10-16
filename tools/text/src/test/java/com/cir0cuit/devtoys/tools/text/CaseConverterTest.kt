package com.cir0cuit.devtoys.tools.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class CaseConverterTest {

    private fun convert(case: String, s: String) = caseConversions().first { it.first == case }.second(s)

    @Test
    fun identifierCasesSplitMixedInput() {
        val input = "parseHTTPServer response_code-2"
        assertEquals("parseHttpServerResponseCode2", convert("camelCase", input))
        assertEquals("ParseHttpServerResponseCode2", convert("PascalCase", input))
        assertEquals("parse_http_server_response_code_2", convert("snake_case", input))
        assertEquals("parse-http-server-response-code-2", convert("kebab-case", input))
        assertEquals("PARSE_HTTP_SERVER_RESPONSE_CODE_2", convert("CONSTANT_CASE", input))
    }

    @Test
    fun identifierCasesKeepLines() {
        assertEquals("fooBar\nbazQux", convert("camelCase", "foo bar\nBaz qux"))
    }

    @Test
    fun titleAndSentenceCase() {
        assertEquals("Don't Stop Me Now", convert("Title case", "don't STOP me now"))
        assertEquals("Hello world. Bye now!", convert("Sentence case", "HELLO WORLD. bye NOW!"))
    }

    @Test
    fun moreIdentifierCases() {
        val input = "content type v2"
        assertEquals("Content-Type-V2", convert("Train-Case", input))
        assertEquals("CONTENT-TYPE-V2", convert("COBOL-CASE", input))
        assertEquals("content.type.v2", convert("dot.case", input))
        assertEquals("content/type/v2", convert("path/case", input))
        assertEquals("contenttypev2", convert("flatcase", input))
        assertEquals("CONTENTTYPEV2", convert("UPPERFLATCASE", input))
    }

    @Test
    fun swapAndAlternatingCase() {
        assertEquals("hELLO wORLD 1", convert("Swap case", "Hello World 1"))
        assertEquals("aBc-D", convert("Alternating case", "abc-d"))
    }

    @Test
    fun slugAndAsciiFold() {
        assertEquals("zazolc-gesla-jazn-strasse-aero", slugify("  Zażółć gęślą jaźń -- Straße, Ærø! "))
        assertEquals("Creme brulee, Lodz, Malmo", asciiFold("Crème brûlée, Łódź, Malmö"))
        assertEquals("a-b\nc", convert("Slug", "A B\nc"))
    }

    @Test
    fun identifierCasesIgnoreTurkishDefaultLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("USER_ID", convert("CONSTANT_CASE", "user id"))
            assertEquals("ID", convert("Uppercase", "id"))
            assertEquals("İD", caseConversions(Locale.forLanguageTag("tr-TR")).first { it.first == "Uppercase" }.second("id"))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun detectsCase() {
        assertEquals("camelCase", detectCase("parseHttpServer"))
        assertEquals("PascalCase", detectCase("ParseHttp"))
        assertEquals("snake_case", detectCase("user_id"))
        assertEquals("CONSTANT_CASE", detectCase("MAX_SIZE"))
        assertEquals("kebab-case", detectCase("my-value"))
        assertEquals("Train-Case", detectCase("Content-Type"))
        assertEquals("dot.case", detectCase("com.example.app"))
        assertEquals("Uppercase", detectCase("HELLO WORLD"))
        assertEquals("Lowercase", detectCase("hello world"))
        assertEquals("Title case", detectCase("Hello World"))
        assertEquals("Sentence case", detectCase("Hello world. Bye now"))
        assertNull(detectCase("   "))
    }
}
