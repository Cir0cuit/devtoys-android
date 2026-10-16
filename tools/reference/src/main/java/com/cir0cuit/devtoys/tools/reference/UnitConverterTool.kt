package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import java.math.BigDecimal
import java.math.MathContext
import java.time.Duration

/** base = (value + offset) * factor; offset is only non-zero for temperatures. */
internal class ConvUnit(val symbol: String, val name: String, val factor: BigDecimal, val offset: BigDecimal = BigDecimal.ZERO)

internal class Quantity(val name: String, val units: List<ConvUnit>, val defaultUnit: String)

private fun d(s: String) = BigDecimal(s)
private fun ratio(a: String, b: String) = d(a).divide(d(b), MathContext.DECIMAL128)
private fun pow(base: Int, exp: Int) = BigDecimal(base).pow(exp)

internal val QUANTITIES = listOf(
    Quantity(
        "Data size", listOf(
            ConvUnit("bit", "Bits", d("0.125")),
            ConvUnit("B", "Bytes", BigDecimal.ONE),
            ConvUnit("kB", "Kilobytes", pow(1000, 1)),
            ConvUnit("MB", "Megabytes", pow(1000, 2)),
            ConvUnit("GB", "Gigabytes", pow(1000, 3)),
            ConvUnit("TB", "Terabytes", pow(1000, 4)),
            ConvUnit("PB", "Petabytes", pow(1000, 5)),
            ConvUnit("KiB", "Kibibytes", pow(1024, 1)),
            ConvUnit("MiB", "Mebibytes", pow(1024, 2)),
            ConvUnit("GiB", "Gibibytes", pow(1024, 3)),
            ConvUnit("TiB", "Tebibytes", pow(1024, 4)),
            ConvUnit("PiB", "Pebibytes", pow(1024, 5))
        ), "MB"
    ),
    Quantity(
        "Data rate", listOf(
            ConvUnit("bit/s", "Bits per second", BigDecimal.ONE),
            ConvUnit("kbit/s", "Kilobits per second", pow(1000, 1)),
            ConvUnit("Mbit/s", "Megabits per second", pow(1000, 2)),
            ConvUnit("Gbit/s", "Gigabits per second", pow(1000, 3)),
            ConvUnit("B/s", "Bytes per second", d("8")),
            ConvUnit("kB/s", "Kilobytes per second", d("8000")),
            ConvUnit("MB/s", "Megabytes per second", d("8000000")),
            ConvUnit("GB/s", "Gigabytes per second", d("8000000000")),
            ConvUnit("KiB/s", "Kibibytes per second", pow(1024, 1) * d("8")),
            ConvUnit("MiB/s", "Mebibytes per second", pow(1024, 2) * d("8"))
        ), "Mbit/s"
    ),
    Quantity(
        "Time", listOf(
            ConvUnit("ns", "Nanoseconds", d("1e-9")),
            ConvUnit("µs", "Microseconds", d("1e-6")),
            ConvUnit("ms", "Milliseconds", d("0.001")),
            ConvUnit("s", "Seconds", BigDecimal.ONE),
            ConvUnit("min", "Minutes", d("60")),
            ConvUnit("h", "Hours", d("3600")),
            ConvUnit("d", "Days", d("86400")),
            ConvUnit("wk", "Weeks", d("604800")),
            ConvUnit("yr", "Years (365.25 days)", d("31557600"))
        ), "s"
    ),
    Quantity(
        "Temperature", listOf(
            ConvUnit("°C", "Celsius", BigDecimal.ONE, d("273.15")),
            ConvUnit("°F", "Fahrenheit", ratio("5", "9"), d("459.67")),
            ConvUnit("K", "Kelvin", BigDecimal.ONE)
        ), "°C"
    ),
    Quantity(
        "Length", listOf(
            ConvUnit("mm", "Millimeters", d("0.001")),
            ConvUnit("cm", "Centimeters", d("0.01")),
            ConvUnit("m", "Meters", BigDecimal.ONE),
            ConvUnit("km", "Kilometers", d("1000")),
            ConvUnit("in", "Inches", d("0.0254")),
            ConvUnit("ft", "Feet", d("0.3048")),
            ConvUnit("yd", "Yards", d("0.9144")),
            ConvUnit("mi", "Miles", d("1609.344")),
            ConvUnit("nmi", "Nautical miles", d("1852"))
        ), "m"
    ),
    Quantity(
        "Mass", listOf(
            ConvUnit("mg", "Milligrams", d("0.000001")),
            ConvUnit("g", "Grams", d("0.001")),
            ConvUnit("kg", "Kilograms", BigDecimal.ONE),
            ConvUnit("t", "Tonnes", d("1000")),
            ConvUnit("oz", "Ounces", d("0.028349523125")),
            ConvUnit("lb", "Pounds", d("0.45359237")),
            ConvUnit("st", "Stones", d("6.35029318"))
        ), "kg"
    ),
    Quantity(
        "Speed", listOf(
            ConvUnit("m/s", "Meters per second", BigDecimal.ONE),
            ConvUnit("km/h", "Kilometers per hour", ratio("1000", "3600")),
            ConvUnit("mph", "Miles per hour", d("0.44704")),
            ConvUnit("kn", "Knots", ratio("1852", "3600")),
            ConvUnit("ft/s", "Feet per second", d("0.3048"))
        ), "km/h"
    )
)

internal fun convertUnit(value: BigDecimal, from: ConvUnit, to: ConvUnit): BigDecimal {
    val base = (value + from.offset).multiply(from.factor, MathContext.DECIMAL128)
    return base.divide(to.factor, MathContext.DECIMAL128) - to.offset
}

/** 12 significant digits, plain notation unless the number is huge or tiny. */
internal fun formatQuantity(value: BigDecimal): String {
    val r = value.round(MathContext(12)).stripTrailingZeros()
    if (r.signum() == 0) return "0"
    val magnitude = r.precision() - r.scale() - 1
    return if (magnitude in -7..15) r.toPlainString() else r.toString().replace("E+", "e").replace("E", "e")
}

/** Accepts "1.5", "1,5", "1 000", "2e3", "1_000". Returns null when it is not a number. */
internal fun parseQuantity(text: String): BigDecimal? =
    text.trim().replace(" ", "").replace("_", "").replace(',', '.').toBigDecimalOrNull()

class UnitConverterTool : Tool {
    override val descriptor = ToolDescriptor(
        "unit-converter",
        "Unit Converter",
        "Convert data size, time, temperature and more",
        Icons.Default.Straighten,
        ToolCategory.CONVERTERS,
        listOf("unit", "convert", "bytes", "mib", "temperature", "celsius", "length", "mass", "speed", "duration", "bandwidth")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var quantityName by rememberSaveable { mutableStateOf(QUANTITIES[0].name) }
        val quantity = QUANTITIES.first { it.name == quantityName }
        var unitSymbol by rememberSaveable(quantityName) { mutableStateOf(quantity.defaultUnit) }
        val unit = quantity.units.firstOrNull { it.symbol == unitSymbol } ?: quantity.units[0]
        var input by rememberSaveable { mutableStateOf("1") }

        val isTime = quantity.name == "Time"
        // Time also accepts ISO 8601 durations such as PT1H30M.
        val isoSeconds = if (isTime && input.trim().startsWith("P", ignoreCase = true)) {
            runCatching { Duration.parse(input.trim().uppercase()) }.getOrNull()
                ?.let { BigDecimal(it.seconds) + BigDecimal(it.nano).movePointLeft(9) }
        } else null
        val value = isoSeconds?.let { convertUnit(it, quantity.units.first { u -> u.symbol == "s" }, unit) } ?: parseQuantity(input)

        ToolScaffold(descriptor.title, modifier = modifier) {
            SelectField("Quantity", quantity, QUANTITIES, { quantityName = it.name }, Modifier.fillMaxWidth(), optionLabel = { it.name })
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(64) },
                    label = { Text("Value") },
                    singleLine = true,
                    textStyle = CodeTextStyle,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = if (isTime) KeyboardType.Ascii else KeyboardType.Decimal, autoCorrect = false),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                SelectField(
                    "Unit", unit, quantity.units, { unitSymbol = it.symbol }, Modifier.width(132.dp),
                    optionLabel = { "${it.symbol}  ${it.name}" }, selectedLabel = { it.symbol }
                )
            }
            Spacer(Modifier.height(16.dp))
            when {
                input.isBlank() -> Unit
                value == null -> ErrorText(
                    if (isTime) "\"$input\" is not a number or an ISO 8601 duration. Enter something like 90 or PT1H30M."
                    else "\"$input\" is not a number. Enter something like 1.5 or 2e3."
                )
                else -> {
                    val rows = quantity.units.filter { it !== unit || isoSeconds != null }
                        .map { "${it.name} (${it.symbol})" to formatQuantity(convertUnit(value, unit, it)) }
                        .toMutableList()
                    if (isTime) {
                        val seconds = convertUnit(value, unit, quantity.units.first { it.symbol == "s" })
                        isoDuration(seconds)?.let { rows += "ISO 8601" to it }
                    }
                    LabeledValueSlab(rows)
                }
            }
        }
    }
}

/** ISO 8601 duration for [seconds], or null when it does not fit java.time.Duration. */
internal fun isoDuration(seconds: BigDecimal): String? = runCatching {
    val whole = seconds.setScale(0, java.math.RoundingMode.FLOOR)
    val nanos = (seconds - whole).movePointRight(9).setScale(0, java.math.RoundingMode.HALF_UP)
    Duration.ofSeconds(whole.longValueExact(), nanos.longValueExact()).toString()
}.getOrNull()
