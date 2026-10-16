package com.cir0cuit.devtoys.tools.text

import org.junit.Assert.assertEquals
import org.junit.Test

class CaseConverterTest {

    private fun convert(case: String, s: String) = caseConversions.first { it.first == case }.second(s)

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
}
