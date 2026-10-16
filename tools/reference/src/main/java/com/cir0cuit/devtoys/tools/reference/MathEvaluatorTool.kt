package com.cir0cuit.devtoys.tools.reference

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.MonoTextField
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

internal class EvalException(message: String) : Exception(message)

private val MC = MathContext.DECIMAL128

/** Whole-number digit limit for bitwise ops and radix output, roughly 4000 bits. */
private const val MAX_INT_DIGITS = 1200

/**
 * Recursive-descent evaluator over BigDecimal. Precedence, lowest first:
 * `|`, `xor`, `&`, `<< >>`, `+ -`, `* / %`, unary `- + ~`, `^` (power, right-assoc).
 * Literals: 12, 1.5e3, 0xFF, 0b1010, 0o17, `_` separators. Constants pi, e.
 */
internal fun evaluateExpression(text: String): BigDecimal {
    val p = ExprParser(text)
    val v = p.parseOr()
    p.skipWs()
    if (p.pos < text.length) throw p.unexpected()
    return v
}

private class ExprParser(val s: String) {
    var pos = 0

    fun skipWs() { while (pos < s.length && s[pos].isWhitespace()) pos++ }

    fun eat(token: String): Boolean {
        skipWs()
        if (!s.startsWith(token, pos)) return false
        // "<" must not match the start of "<<" and words must end at a word boundary.
        val end = pos + token.length
        if (token.last().isLetter() && end < s.length && s[end].isLetterOrDigit()) return false
        pos = end
        return true
    }

    fun unexpected(): EvalException {
        skipWs()
        return if (pos >= s.length) EvalException("The expression ends too early. Add the missing number or closing parenthesis.")
        else EvalException("Unexpected \"${s[pos]}\" at position ${pos + 1}. Check for a missing operator or parenthesis.")
    }

    fun parseOr(): BigDecimal {
        var v = parseXor()
        while (eat("|")) v = int(v, "|").or(int(parseXor(), "|")).toBigDecimal()
        return v
    }

    fun parseXor(): BigDecimal {
        var v = parseAnd()
        while (eat("xor")) v = int(v, "xor").xor(int(parseAnd(), "xor")).toBigDecimal()
        return v
    }

    fun parseAnd(): BigDecimal {
        var v = parseShift()
        while (eat("&")) v = int(v, "&").and(int(parseShift(), "&")).toBigDecimal()
        return v
    }

    fun parseShift(): BigDecimal {
        var v = parseAdd()
        while (true) {
            val left = when {
                eat("<<") -> true
                eat(">>") -> false
                else -> return v
            }
            val n = int(parseAdd(), "shift")
            if (n.abs() > BigInteger.valueOf(4096)) throw EvalException("Shift amounts above 4096 are not supported.")
            val bits = if (left) n.toInt() else -n.toInt()
            v = int(v, "shift").shiftLeft(bits).toBigDecimal()
        }
    }

    fun parseAdd(): BigDecimal {
        var v = parseMul()
        while (true) {
            v = when {
                eat("+") -> v.add(parseMul(), MC)
                eat("-") -> v.subtract(parseMul(), MC)
                else -> return v
            }
        }
    }

    fun parseMul(): BigDecimal {
        var v = parseUnary()
        while (true) {
            v = when {
                eat("*") -> v.multiply(parseUnary(), MC)
                eat("/") -> parseUnary().let { if (it.signum() == 0) throw divByZero() else v.divide(it, MC) }
                eat("%") -> parseUnary().let { if (it.signum() == 0) throw divByZero() else v.remainder(it, MC) }
                else -> return v
            }
        }
    }

    fun parseUnary(): BigDecimal = when {
        eat("-") -> parseUnary().negate()
        eat("+") -> parseUnary()
        eat("~") -> int(parseUnary(), "~").not().toBigDecimal()
        else -> parsePower()
    }

    fun parsePower(): BigDecimal {
        val base = parsePrimary()
        if (!eat("^")) return base
        return power(base, parseUnary())
    }

    fun parsePrimary(): BigDecimal {
        skipWs()
        if (eat("(")) {
            val v = parseOr()
            if (!eat(")")) throw EvalException("A \"(\" is never closed. Add the missing \")\".")
            return v
        }
        if (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) return number()
        if (pos < s.length && s[pos].isLetter()) {
            val start = pos
            while (pos < s.length && s[pos].isLetterOrDigit()) pos++
            val name = s.substring(start, pos).lowercase()
            if (!eat("(")) return when (name) {
                "pi" -> BigDecimal.valueOf(Math.PI)
                "e" -> BigDecimal.valueOf(Math.E)
                else -> throw EvalException("Unknown name \"$name\". Use pi, e or a function such as sqrt(2).")
            }
            val args = mutableListOf(parseOr())
            while (eat(",")) args += parseOr()
            if (!eat(")")) throw EvalException("The call to $name( is never closed. Add the missing \")\".")
            return call(name, args)
        }
        throw unexpected()
    }

    fun number(): BigDecimal {
        val radix = if (s[pos] == '0' && pos + 1 < s.length) when (s[pos + 1].lowercaseChar()) {
            'x' -> 16; 'b' -> 2; 'o' -> 8; else -> 10
        } else 10
        if (radix != 10) {
            pos += 2
            val start = pos
            while (pos < s.length && (s[pos].isLetterOrDigit() || s[pos] == '_')) pos++
            val digits = s.substring(start, pos).replace("_", "")
            return digits.toBigIntegerOrNull(radix)?.toBigDecimal()
                ?: throw EvalException("\"${s.substring(start - 2, pos)}\" is not a valid base-$radix number.")
        }
        val start = pos
        while (pos < s.length && (s[pos].isDigit() || s[pos] == '.' || s[pos] == '_')) pos++
        if (pos < s.length && s[pos].lowercaseChar() == 'e') {
            val mark = pos
            pos++
            if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) pos++
            if (pos < s.length && s[pos].isDigit()) while (pos < s.length && s[pos].isDigit()) pos++ else pos = mark
        }
        val raw = s.substring(start, pos)
        return raw.replace("_", "").toBigDecimalOrNull() ?: throw EvalException("\"$raw\" is not a valid number.")
    }

    fun call(name: String, args: List<BigDecimal>): BigDecimal {
        fun arity(n: Int) { if (args.size != n) throw EvalException("$name takes $n argument${if (n == 1) "" else "s"}, not ${args.size}.") }
        fun dbl(f: (Double) -> Double): BigDecimal {
            arity(1)
            val r = f(args[0].toDouble())
            if (r.isNaN() || r.isInfinite()) throw EvalException("$name(${args[0].toPlainString()}) is undefined. Check the argument's range.")
            return BigDecimal.valueOf(r)
        }
        return when (name) {
            "sqrt" -> dbl(Math::sqrt)
            "cbrt" -> dbl(Math::cbrt)
            "ln" -> dbl(Math::log)
            "log", "log10" -> dbl(Math::log10)
            "log2" -> dbl { Math.log(it) / Math.log(2.0) }
            "exp" -> dbl(Math::exp)
            "sin" -> dbl(Math::sin)
            "cos" -> dbl(Math::cos)
            "tan" -> dbl(Math::tan)
            "asin" -> dbl(Math::asin)
            "acos" -> dbl(Math::acos)
            "atan" -> dbl(Math::atan)
            "abs" -> { arity(1); args[0].abs() }
            "floor" -> { arity(1); args[0].setScale(0, RoundingMode.FLOOR) }
            "ceil" -> { arity(1); args[0].setScale(0, RoundingMode.CEILING) }
            "round" -> { arity(1); args[0].setScale(0, RoundingMode.HALF_UP) }
            "min" -> args.min()
            "max" -> args.max()
            else -> throw EvalException("Unknown function \"$name\". Try sqrt, ln, log, log2, sin, abs, round, min or max.")
        }
    }

    fun power(base: BigDecimal, exp: BigDecimal): BigDecimal {
        val n = runCatching { exp.intValueExact() }.getOrNull()
        if (n != null && n in -9999..9999) {
            if (base.signum() == 0 && n < 0) throw divByZero()
            return base.pow(n, MC)
        }
        val r = Math.pow(base.toDouble(), exp.toDouble())
        if (r.isNaN() || r.isInfinite()) throw EvalException("${base.toPlainString()} ^ ${exp.toPlainString()} is undefined or too large.")
        return BigDecimal.valueOf(r)
    }

    fun int(v: BigDecimal, op: String): BigInteger {
        if (v.precision() - v.scale() > MAX_INT_DIGITS) throw EvalException("\"$op\" supports numbers up to about 4000 bits. Use smaller operands.")
        return runCatching { v.toBigIntegerExact() }.getOrElse {
            throw EvalException("\"$op\" needs whole numbers, but got ${formatDecimal(v)}. Wrap it in round().")
        }
    }

    fun divByZero() = EvalException("Division by zero. Change the divisor to a non-zero value.")
}

/** Decimal rendering trimmed to 20 significant digits; scientific when very large or small. */
internal fun formatDecimal(v: BigDecimal): String {
    val r = v.round(MathContext(20)).stripTrailingZeros()
    if (r.signum() == 0) return "0"
    val magnitude = r.precision() - r.scale() - 1
    return if (magnitude in -9..30) r.toPlainString() else r.toString()
}

/** Hex, binary and octal rows for whole results; negatives in 64-bit two's complement when they fit. */
internal fun radixRows(v: BigDecimal): List<Pair<String, String>> {
    if (v.precision() - v.scale() > MAX_INT_DIGITS) return emptyList()
    val i = runCatching { v.toBigIntegerExact() }.getOrNull() ?: return emptyList()
    if (i.signum() < 0 && i.bitLength() < 64) {
        val l = i.toLong()
        return listOf(
            "Hex (64-bit)" to "0x" + java.lang.Long.toHexString(l).uppercase(),
            "Binary (64-bit)" to "0b" + java.lang.Long.toBinaryString(l),
            "Octal (64-bit)" to "0o" + java.lang.Long.toOctalString(l)
        )
    }
    val sign = if (i.signum() < 0) "-" else ""
    val a = i.abs()
    return listOf(
        "Hex" to sign + "0x" + a.toString(16).uppercase(),
        "Binary" to sign + "0b" + a.toString(2),
        "Octal" to sign + "0o" + a.toString(8)
    )
}

class MathEvaluatorTool : Tool {
    override val descriptor = ToolDescriptor(
        "math-evaluator",
        "Math Evaluator",
        "Evaluate math with hex, binary and bitwise ops",
        Icons.Default.Calculate,
        ToolCategory.CONVERTERS,
        listOf("math", "calculator", "expression", "programmer", "hex", "binary", "bitwise", "evaluate")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var expr by rememberSaveable { mutableStateOf("(0xFF + 1) * 2 ^ 3") }
        var result by remember { mutableStateOf<Result<BigDecimal>?>(null) }

        LaunchedEffect(expr) {
            if (expr.isBlank()) { result = null; return@LaunchedEffect }
            delay(150)
            result = withContext(Dispatchers.Default) { runCatching { evaluateExpression(expr) } }
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            MonoTextField(expr, { expr = it.take(2000) }, "Expression", minLines = 3)
            Text(
                "Operators: + - * / % ^ (power), & | xor ~ << >>. Functions: sqrt, ln, log, log2, sin, cos, tan, abs, round, min, max.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            result?.onSuccess { v ->
                LabeledValueSlab(listOf("Decimal" to formatDecimal(v)) + radixRows(v))
            }?.onFailure { e ->
                ErrorText(if (e is EvalException) e.message.orEmpty() else "This expression is too large to evaluate. Use smaller numbers.")
            }
        }
    }
}
