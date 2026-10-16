package com.cir0cuit.devtoys.tools.encoders
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Filter1

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import java.math.BigInteger

internal fun parseBig(s: String, radix: Int): BigInteger? {
    var t = s.trim().replace("_", "")
    when (radix) {
        16 -> t = t.removePrefix("0x").removePrefix("0X")
        2 -> t = t.removePrefix("0b").removePrefix("0B")
        8 -> t = t.removePrefix("0o").removePrefix("0O")
    }
    if (t.isEmpty() || t.length > 4096) return null
    return runCatching { BigInteger(t, radix) }.getOrNull()
}

class NumberBaseConverterTool : Tool {
    override val descriptor = ToolDescriptor("base-converter", android.R.string.untitled, android.R.string.untitled, Icons.Default.Filter1, ToolCategory.CONVERTERS, listOf("base", "converter", "binary", "hex"))
    @Composable override fun Content(modifier: Modifier) {
        var dec by rememberSaveable { mutableStateOf("42") }
        var hex by rememberSaveable { mutableStateOf("2A") }
        var bin by rememberSaveable { mutableStateOf("101010") }
        var oct by rememberSaveable { mutableStateOf("52") }
        
        ToolScaffold("Number Base Converter", modifier = modifier) {
            MonoTextField(dec, { 
                dec = it
                val v = parseBig(it, 10)
                if (v != null) { hex = v.toString(16).uppercase(); bin = v.toString(2); oct = v.toString(8) } 
                else { hex = ""; bin = ""; oct = "" }
            }, "Decimal", minLines=1)
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(hex, { 
                hex = it
                val v = parseBig(it, 16)
                if (v != null) { dec = v.toString(10); bin = v.toString(2); oct = v.toString(8) } 
                else { dec = ""; bin = ""; oct = "" }
            }, "Hexadecimal", minLines=1)
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(bin, { 
                bin = it
                val v = parseBig(it, 2)
                if (v != null) { dec = v.toString(10); hex = v.toString(16).uppercase(); oct = v.toString(8) } 
                else { dec = ""; hex = ""; oct = "" }
            }, "Binary", minLines=1)
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(oct, { 
                oct = it
                val v = parseBig(it, 8)
                if (v != null) { dec = v.toString(10); hex = v.toString(16).uppercase(); bin = v.toString(2) } 
                else { dec = ""; hex = ""; bin = "" }
            }, "Octal", minLines=1)
        }
    }
}