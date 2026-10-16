package com.cir0cuit.devtoys.tools.developer

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class ColorPaletteTool : Tool {
    override val descriptor = ToolDescriptor(
        "color-palette",
        android.R.string.untitled,
        android.R.string.untitled,
        Icons.Default.Palette,
        ToolCategory.GRAPHICS,
        listOf("color", "palette", "hex", "rgb", "scheme")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var red by rememberSaveable { mutableStateOf(52f) }
        var green by rememberSaveable { mutableStateOf(152f) }
        var blue by rememberSaveable { mutableStateOf(219f) }
        var hexInput by rememberSaveable { mutableStateOf("3498DB") }

        fun syncHex() { hexInput = String.format("%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt()) }

        val baseColorInt = android.graphics.Color.rgb(red.toInt(), green.toInt(), blue.toInt())

        ToolScaffold("Color Palette Generator", modifier = modifier) {
            Box(modifier = Modifier.width(150.dp)) {
                MonoTextField(
                    value = hexInput,
                    onValueChange = { newValue ->
                        var s = newValue.trim()
                        if (s.startsWith("#")) s = s.substring(1)
                        else if (s.startsWith("0x", ignoreCase = true)) s = s.substring(2)
                        val clean = s.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }.take(6).uppercase()
                        hexInput = clean
                        if (clean.length == 6) {
                            val rgb = clean.toInt(16)
                            red = ((rgb shr 16) and 0xFF).toFloat()
                            green = ((rgb shr 8) and 0xFF).toFloat()
                            blue = (rgb and 0xFF).toFloat()
                        }
                    },
                    label = "HEX",
                    minLines = 1,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            ColorChannelSlider("Red", red) { red = it; syncHex() }
            Spacer(modifier = Modifier.height(8.dp))
            ColorChannelSlider("Green", green) { green = it; syncHex() }
            Spacer(modifier = Modifier.height(8.dp))
            ColorChannelSlider("Blue", blue) { blue = it; syncHex() }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val hsv = FloatArray(3)
            Color.colorToHSV(baseColorInt, hsv)

            val compHsv = hsv.clone()
            if (hsv[1] == 0f) {
                compHsv[2] = 1f - hsv[2]
            } else {
                compHsv[0] = (hsv[0] + 180f) % 360f
            }

            val ana1Hsv = hsv.clone()
            val ana2Hsv = hsv.clone()
            if (hsv[1] == 0f) {
                ana1Hsv[2] = (if (hsv[2] + 0.15f > 1f) hsv[2] - 0.3f else hsv[2] + 0.15f).coerceIn(0f, 1f)
                ana2Hsv[2] = (if (hsv[2] - 0.15f < 0f) hsv[2] + 0.3f else hsv[2] - 0.15f).coerceIn(0f, 1f)
            } else {
                ana1Hsv[0] = (hsv[0] + 30f) % 360f
                ana2Hsv[0] = (hsv[0] + 330f) % 360f
            }

            val tri1Hsv = hsv.clone()
            val tri2Hsv = hsv.clone()
            if (hsv[1] == 0f) {
                tri1Hsv[2] = (hsv[2] + 0.33f) % 1.0f
                tri2Hsv[2] = (hsv[2] + 0.66f) % 1.0f
            } else {
                tri1Hsv[0] = (hsv[0] + 120f) % 360f
                tri2Hsv[0] = (hsv[0] + 240f) % 360f
            }

            val mono1Hsv = hsv.clone()
            val mono2Hsv = hsv.clone()
            val mono3Hsv = hsv.clone()
            if (hsv[1] == 0f) {
                if (hsv[2] > 0.5f) {
                    mono1Hsv[2] = (hsv[2] - 0.25f).coerceAtLeast(0f)
                    mono2Hsv[2] = (hsv[2] - 0.5f).coerceAtLeast(0f)
                    mono3Hsv[2] = (hsv[2] - 0.75f).coerceAtLeast(0f)
                } else {
                    mono1Hsv[2] = (hsv[2] + 0.25f).coerceAtMost(1f)
                    mono2Hsv[2] = (hsv[2] + 0.5f).coerceAtMost(1f)
                    mono3Hsv[2] = (hsv[2] + 0.75f).coerceAtMost(1f)
                }
            } else {
                if (hsv[2] > 0.5f) {
                    mono1Hsv[1] = (hsv[1] - 0.3f).coerceAtLeast(0f)
                    mono1Hsv[2] = (hsv[2] + 0.1f).coerceAtMost(1f)
                    
                    mono2Hsv[1] = (hsv[1] + 0.2f).coerceAtMost(1f)
                    mono2Hsv[2] = (hsv[2] - 0.3f).coerceAtLeast(0f)
                    
                    mono3Hsv[1] = (hsv[1] + 0.4f).coerceAtMost(1f)
                    mono3Hsv[2] = (hsv[2] - 0.6f).coerceAtLeast(0f)
                } else {
                    mono1Hsv[1] = (hsv[1] + 0.3f).coerceAtMost(1f)
                    mono1Hsv[2] = (hsv[2] - 0.1f).coerceAtLeast(0f)
                    
                    mono2Hsv[1] = (hsv[1] - 0.2f).coerceAtLeast(0f)
                    mono2Hsv[2] = (hsv[2] + 0.3f).coerceAtMost(1f)
                    
                    mono3Hsv[1] = (hsv[1] - 0.4f).coerceAtLeast(0f)
                    mono3Hsv[2] = (hsv[2] + 0.6f).coerceAtMost(1f)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PaletteRow("Complementary", listOf(baseColorInt, Color.HSVToColor(compHsv)))
                PaletteRow("Analogous", listOf(Color.HSVToColor(ana2Hsv), baseColorInt, Color.HSVToColor(ana1Hsv)))
                PaletteRow("Triadic", listOf(baseColorInt, Color.HSVToColor(tri1Hsv), Color.HSVToColor(tri2Hsv)))
                PaletteRow("Monochromatic", listOf(Color.HSVToColor(mono1Hsv), baseColorInt, Color.HSVToColor(mono2Hsv), Color.HSVToColor(mono3Hsv)))
            }
        }
    }

    @Composable
    private fun ColorChannelSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
        var textValue by remember(value) { mutableStateOf(value.toInt().toString()) }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.width(50.dp))
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(3)
                    textValue = digits
                    digits.toIntOrNull()?.let { v -> onValueChange(v.coerceIn(0, 255).toFloat()) }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Slider(
                value = value,
                onValueChange = { onValueChange(it) },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    private fun PaletteRow(title: String, colors: List<Int>) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp))) {
                for (colorInt in colors) {
                    val hex = String.format("#%06X", 0xFFFFFF and colorInt)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(ComposeColor(colorInt)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = hex,
                            color = if (Color.luminance(colorInt) > 0.5) ComposeColor.Black else ComposeColor.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
