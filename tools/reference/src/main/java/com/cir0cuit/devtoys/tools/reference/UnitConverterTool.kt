package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
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
import com.cir0cuit.devtoys.core.ui.SelectField
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

private val SI_PREFIXES = listOf("k" to "Kilo", "M" to "Mega", "G" to "Giga", "T" to "Tera", "P" to "Peta", "E" to "Exa")
private val IEC_PREFIXES = listOf("Ki" to "Kibi", "Mi" to "Mebi", "Gi" to "Gibi", "Ti" to "Tebi", "Pi" to "Pebi", "Ei" to "Exbi")

/** The unit itself, then every decimal (1000ⁿ) and binary (1024ⁿ) multiple of it. */
private fun withPrefixes(symbol: String, name: String, factor: BigDecimal, suffix: String = "", nameSuffix: String = ""): List<ConvUnit> =
    listOf(ConvUnit(symbol + suffix, name.replaceFirstChar { it.uppercase() } + nameSuffix, factor)) +
        listOf(SI_PREFIXES to 1000, IEC_PREFIXES to 1024).flatMap { (prefixes, base) ->
            prefixes.mapIndexed { i, (p, n) -> ConvUnit(p + symbol + suffix, n + name + nameSuffix, factor * pow(base, i + 1)) }
        }

internal val QUANTITIES = listOf(
    Quantity("Data size", withPrefixes("bit", "bits", d("0.125")) + withPrefixes("B", "bytes", BigDecimal.ONE), "MB"),
    Quantity(
        "Data rate",
        withPrefixes("bit", "bits", BigDecimal.ONE, "/s", " per second") + withPrefixes("B", "bytes", d("8"), "/s", " per second"),
        "Mbit/s"
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
            ConvUnit("nm", "Nanometers", d("1e-9")),
            ConvUnit("µm", "Micrometers", d("1e-6")),
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
    ),
    Quantity(
        "Volume", listOf(
            ConvUnit("mL", "Milliliters", d("0.001")),
            ConvUnit("L", "Liters", BigDecimal.ONE),
            ConvUnit("m³", "Cubic meters", d("1000")),
            ConvUnit("tsp", "Teaspoons (US)", d("0.00492892159375")),
            ConvUnit("tbsp", "Tablespoons (US)", d("0.01478676478125")),
            ConvUnit("fl oz", "Fluid ounces (US)", d("0.0295735295625")),
            ConvUnit("cup", "Cups (US)", d("0.2365882365")),
            ConvUnit("gal", "Gallons (US)", d("3.785411784")),
            ConvUnit("imp gal", "Gallons (imperial)", d("4.54609"))
        ), "L"
    ),
    Quantity(
        "Area", listOf(
            ConvUnit("mm²", "Square millimeters", d("1e-6")),
            ConvUnit("cm²", "Square centimeters", d("1e-4")),
            ConvUnit("m²", "Square meters", BigDecimal.ONE),
            ConvUnit("ha", "Hectares", d("10000")),
            ConvUnit("km²", "Square kilometers", d("1e6")),
            ConvUnit("ft²", "Square feet", d("0.09290304")),
            ConvUnit("ac", "Acres", d("4046.8564224")),
            ConvUnit("mi²", "Square miles", d("2589988.110336"))
        ), "m²"
    ),
    Quantity(
        "Pressure", listOf(
            ConvUnit("Pa", "Pascals", BigDecimal.ONE),
            ConvUnit("kPa", "Kilopascals", d("1000")),
            ConvUnit("bar", "Bars", d("100000")),
            ConvUnit("atm", "Atmospheres", d("101325")),
            ConvUnit("psi", "Pounds per square inch", d("6894.757293168")),
            ConvUnit("mmHg", "Millimeters of mercury", d("133.322387415"))
        ), "bar"
    ),
    Quantity(
        "Energy", listOf(
            ConvUnit("J", "Joules", BigDecimal.ONE),
            ConvUnit("kJ", "Kilojoules", d("1000")),
            ConvUnit("cal", "Calories", d("4.184")),
            ConvUnit("kcal", "Kilocalories", d("4184")),
            ConvUnit("Wh", "Watt-hours", d("3600")),
            ConvUnit("kWh", "Kilowatt-hours", d("3600000")),
            ConvUnit("eV", "Electronvolts", d("1.602176634e-19"))
        ), "kJ"
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
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(64) },
                label = { Text("Value") },
                singleLine = true,
                isError = input.isNotBlank() && value == null,
                textStyle = CodeTextStyle,
                keyboardOptions = KeyboardOptions(keyboardType = if (isTime) KeyboardType.Ascii else KeyboardType.Decimal, autoCorrect = false),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            // Full width: the menu is as wide as the field, and "Kibit/s  Kibibits per second" needs the room.
            SelectField("Unit", unit, quantity.units, { unitSymbol = it.symbol }, Modifier.fillMaxWidth(), optionLabel = { "${it.symbol}  ${it.name}" })
            Spacer(Modifier.height(16.dp))
            when {
                input.isBlank() -> Unit
                value == null -> ErrorText(
                    if (isTime) "\"$input\" is not a number or an ISO 8601 duration. Enter something like 90 or PT1H30M."
                    // No "2e3" here: the decimal keypad has no e key.
                    else "\"$input\" is not a number. Enter something like 1.5 or -40."
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
