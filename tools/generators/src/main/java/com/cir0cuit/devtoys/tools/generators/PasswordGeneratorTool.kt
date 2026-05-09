package com.cir0cuit.devtoys.tools.generators
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password

import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class PasswordGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("password-gen", android.R.string.untitled, android.R.string.untitled, Icons.Default.Password, ToolCategory.GENERATORS, listOf("password", "generator"))
    @Composable override fun Content(modifier: Modifier) {
        var length by rememberSaveable { mutableStateOf(16f) }
        var includeUpper by rememberSaveable { mutableStateOf(true) }
        var includeLower by rememberSaveable { mutableStateOf(true) }
        var includeNumbers by rememberSaveable { mutableStateOf(true) }
        var includeSymbols by rememberSaveable { mutableStateOf(true) }
        var output by rememberSaveable { mutableStateOf("") }
        
        fun generate() {
            val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val lower = "abcdefghijklmnopqrstuvwxyz"
            val numbers = "0123456789"
            val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"
            var chars = ""
            if (includeUpper) chars += upper
            if (includeLower) chars += lower
            if (includeNumbers) chars += numbers
            if (includeSymbols) chars += symbols
            
            if (chars.isEmpty()) {
                output = "Select at least one character set."
                return
            }
            output = (1..length.toInt()).map { chars.random() }.joinToString("")
        }

        ToolScaffold("Password Generator", modifier = modifier) {
            Text("Length: ${length.toInt()}")
            Slider(value = length, onValueChange = { length = it }, valueRange = 4f..64f)
            Row {
                Checkbox(checked = includeUpper, onCheckedChange = { includeUpper = it })
                Text("Uppercase", modifier = Modifier.padding(top=14.dp, end=8.dp))
                Checkbox(checked = includeLower, onCheckedChange = { includeLower = it })
                Text("Lowercase", modifier = Modifier.padding(top=14.dp))
            }
            Row {
                Checkbox(checked = includeNumbers, onCheckedChange = { includeNumbers = it })
                Text("Numbers", modifier = Modifier.padding(top=14.dp, end=8.dp))
                Checkbox(checked = includeSymbols, onCheckedChange = { includeSymbols = it })
                Text("Symbols", modifier = Modifier.padding(top=14.dp))
            }
            Button(onClick = { generate() }) { Text("Generate") }
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}