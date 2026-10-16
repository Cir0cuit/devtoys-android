package com.cir0cuit.devtoys.tools.data

import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.math.BigDecimal
import java.math.BigInteger

/** jsonToPlain must keep RFC 8259 numbers exact (no binary64 rounding) and preserve types. */
class JsonToYamlTest {

    private fun plain(json: String): Any? {
        val reader = JsonReader(StringReader(json)).apply { setStrictness(Strictness.STRICT) }
        return jsonToPlain(JsonParser.parseReader(reader))
    }

    private fun dump(v: Any?): String = Yaml(DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        indent = 2
        isPrettyFlow = true
    }).dump(v)

    @Test
    fun smallIntegersBecomeLong() {
        assertEquals(42L, plain("42"))
        assertEquals(-7L, plain("-7"))
        assertEquals(Long.MAX_VALUE, plain(Long.MAX_VALUE.toString()))
        assertEquals(Long.MIN_VALUE, plain(Long.MIN_VALUE.toString()))
    }

    @Test
    fun integersBeyondLongStayExact() {
        assertEquals(BigInteger("9223372036854775808"), plain("9223372036854775808"))
        assertEquals(BigInteger("123456789012345678901234567890"), plain("123456789012345678901234567890"))
    }

    @Test
    fun integersBeyondDoublePrecisionStayExact() {
        // 2^53 + 1 is not representable as a double
        assertEquals(9007199254740993L, plain("9007199254740993"))
    }

    @Test
    fun decimalsStayExact() {
        assertEquals(BigDecimal("0.1"), plain("0.1"))
        assertEquals(BigDecimal("1.0"), plain("1.0"))
        assertEquals(BigDecimal("3.141592653589793238462643383279"), plain("3.141592653589793238462643383279"))
        assertEquals(BigDecimal("1e400"), plain("1e400"))
        assertEquals(BigDecimal("-2.5E-3"), plain("-2.5E-3"))
    }

    @Test
    fun primitivesAndNull() {
        assertEquals(true, plain("true"))
        assertEquals(false, plain("false"))
        assertNull(plain("null"))
        assertEquals("123", plain("\"123\""))
        assertEquals("caf\u00e9 \n", plain("\"caf\\u00e9 \\n\""))
    }

    @Test
    fun objectKeyOrderIsPreserved() {
        val m = plain("{\"z\":1,\"a\":2,\"m\":3}") as Map<*, *>
        assertEquals(listOf("z", "a", "m"), m.keys.toList())
    }

    @Test
    fun nestedStructures() {
        val m = plain("{\"a\":[1,{\"b\":null}],\"c\":{}}") as Map<*, *>
        assertEquals(listOf(1L, mapOf("b" to null)), m["a"])
        assertEquals(emptyMap<String, Any?>(), m["c"])
    }

    @Test
    fun yamlOutputKeepsNumericPrecisionAndStringTypes() {
        val y = dump(plain("{\"d\":0.1,\"big\":12345678901234567890,\"one\":1.0,\"s\":\"123\",\"b\":\"yes\",\"n\":null}"))
        assertTrue(y, y.contains("d: 0.1\n"))
        assertTrue(y, y.contains("big: 12345678901234567890\n"))
        assertTrue(y, y.contains("one: 1.0\n"))
        // strings that look like scalars of another type must be quoted to survive a round trip
        val back = Yaml().load<Map<String, Any?>>(y)
        assertEquals("123", back["s"])
        assertEquals("yes", back["b"])
        assertNull(back["n"])
        assertEquals(BigInteger("12345678901234567890"), back["big"])
    }
}
