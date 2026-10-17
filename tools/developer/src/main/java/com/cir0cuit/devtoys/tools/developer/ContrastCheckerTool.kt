package com.cir0cuit.devtoys.tools.developer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.tools.base.*

class ContrastCheckerTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "contrast-checker",
        title = "Contrast Checker",
        description = "Check WCAG contrast between two colors",
        icon = Icons.Default.Contrast,
        category = ToolCategory.GRAPHICS,
        keywords = listOf("contrast", "wcag", "accessibility", "a11y", "color", "color blindness")
    )

    @Composable override fun Content(modifier: Modifier) {
        var fg by rememberSaveable { mutableStateOf(0xFF6B7280.toInt()) }
        var bg by rememberSaveable { mutableStateOf(0xFFFFFFFF.toInt()) }
        var fgText by rememberSaveable { mutableStateOf("#6B7280") }
        var bgText by rememberSaveable { mutableStateOf("#FFFFFF") }
        val context = LocalContext.current

        val ratio = contrastRatio(fg, bg)
        val opaqueBg = compositeOver(bg, 0xFFFFFFFF.toInt()) // a translucent background sits on white
        fun suggest(target: Double, name: String) {
            val fixed = suggestForeground(fg, bg, target)
            if (fixed == null) {
                Toast.makeText(context, "No shade of this text color reaches $name here. Pick a lighter or darker background.", Toast.LENGTH_LONG).show()
            } else {
                fg = fixed; fgText = formatHex(fixed)
            }
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            ColorHeader(Color(fg), fgText, { fgText = it }, { fg = it }, label = "Text color")
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                IconButton(onClick = { fg = bg.also { bg = fg }; fgText = bgText.also { bgText = fgText } }) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Swap text and background colors")
                }
            }
            ColorHeader(Color(bg), bgText, { bgText = it }, { bg = it }, label = "Background")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { suggest(4.5, "4.5:1") }, enabled = ratio < 4.5, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Fix for AA", maxLines = 1)
                }
                OutlinedButton(onClick = { suggest(7.0, "7:1") }, enabled = ratio < 7.0, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Fix for AAA", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { softenForeground(fg, bg, COMFORT_MAX_CONTRAST).let { fg = it; fgText = formatHex(it) } },
                    enabled = ratio > COMFORT_MAX_CONTRAST,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Soften", maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Live sample
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(opaqueBg), MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Text("${num(ratio, 2)}:1", color = Color(fg), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Large text, 24 px or 19 px bold", color = Color(fg), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Normal body text looks like this.", color = Color(fg), fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))

            val checks = listOf(
                "AA normal text" to 4.5, "AA large text" to 3.0, "AAA normal text" to 7.0,
                "AAA large text" to 4.5, "UI components and graphics" to 3.0
            )
            Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    checks.forEach { (label, need) ->
                        val pass = ratio >= need
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text("needs ${num(need, 1)}:1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                if (pass) "Pass" else "Fail",
                                style = CodeTextStyle,
                                fontWeight = FontWeight.Bold,
                                color = if (pass) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    val comfortable = ratio <= COMFORT_MAX_CONTRAST
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Comfortable reading", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (comfortable) "${num(COMFORT_MAX_CONTRAST, 0)}:1 or less" else "Very high contrast glares; tap Soften",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (comfortable) "Pass" else "Too high",
                            style = CodeTextStyle,
                            fontWeight = FontWeight.Bold,
                            color = if (comfortable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("Color vision simulation", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorVision.entries.forEach { vision ->
                    val simBg = vision.simulate(opaqueBg)
                    val simFg = vision.simulate(compositeOver(fg, opaqueBg))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color(simBg), MaterialTheme.shapes.small)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aa", color = Color(simFg), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(vision.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
                        Text("${num(contrastRatio(simFg, simBg), 2)}:1", style = CodeTextStyle.copy(fontSize = 12.sp))
                    }
                }
            }
        }
    }
}
