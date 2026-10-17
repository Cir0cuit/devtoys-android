package com.cir0cuit.devtoys.tools.reference

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class UnitConverterTest {
    private fun convert(quantity: String, value: String, from: String, to: String): String {
        val units = QUANTITIES.first { it.name == quantity }.units
        return formatQuantity(convertUnit(BigDecimal(value), units.first { it.symbol == from }, units.first { it.symbol == to }))
    }

    @Test fun bitPrefixes() {
        assertEquals("125000", convert("Data size", "1", "Mbit", "B"))
        assertEquals("1073741824", convert("Data size", "1", "GiB", "B"))
        assertEquals("1024", convert("Data size", "1", "Mibit", "Kibit"))
        assertEquals("125", convert("Data rate", "1", "Gbit/s", "MB/s"))
    }

    @Test fun symbolsAreUnique() {
        QUANTITIES.forEach { q -> assertEquals(q.name, q.units.size, q.units.map { it.symbol }.toSet().size) }
    }
}
