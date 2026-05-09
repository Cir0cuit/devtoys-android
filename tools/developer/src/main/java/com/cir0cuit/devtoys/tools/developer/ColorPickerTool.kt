package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import android.graphics.Color as AndroidColor

class ColorPickerTool : Tool {
    override val descriptor = ToolDescriptor("color-picker", com.cir0cuit.devtoys.tools.base.R.string.tool_color_picker_title, com.cir0cuit.devtoys.tools.base.R.string.tool_color_picker_desc, Icons.Default.Colorize, ToolCategory.GRAPHICS, listOf("color", "hex", "rgb", "hsl", "hsv"))

    @Composable override fun Content(modifier: Modifier) {
        var red by rememberSaveable { mutableStateOf(100f) }
        var green by rememberSaveable { mutableStateOf(150f) }
        var blue by rememberSaveable { mutableStateOf(255f) }
        var hexInput by rememberSaveable { mutableStateOf("6496FF") }

        // Whenever RGB changes from sliders, update the Hex input
        LaunchedEffect(red, green, blue) {
            hexInput = String.format("%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
        }

        val color = Color(red.toInt(), green.toInt(), blue.toInt())
        
        val rgbStr = "rgb(${red.toInt()}, ${green.toInt()}, ${blue.toInt()})"
        
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(red.toInt(), green.toInt(), blue.toInt(), hsv)
        val hsvStr = "hsv(${hsv[0].toInt()}°, ${(hsv[1]*100).toInt()}%, ${(hsv[2]*100).toInt()}%)"
        
        val hslStr = run {
            val r = red / 255f
            val g = green / 255f
            val b = blue / 255f
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val l = (max + min) / 2
            val s = if (max == min) 0f else if (l < 0.5f) (max - min) / (max + min) else (max - min) / (2f - max - min)
            val h = hsv[0]
            "hsl(${h.toInt()}°, ${(s*100).toInt()}%, ${(l*100).toInt()}%)"
        }

        ToolScaffold("Color Converter", modifier = modifier) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(color))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Box(modifier = Modifier.width(150.dp)) {
                    MonoTextField(
                        value = hexInput,
                        onValueChange = { newValue ->
                            hexInput = newValue.take(6).uppercase()
                            try {
                                if (hexInput.length == 6) {
                                    val parsedColor = android.graphics.Color.parseColor("#$hexInput")
                                    red = android.graphics.Color.red(parsedColor).toFloat()
                                    green = android.graphics.Color.green(parsedColor).toFloat()
                                    blue = android.graphics.Color.blue(parsedColor).toFloat()
                                }
                            } catch (e: Exception) {}
                        },
                        label = "HEX",
                        minLines = 1,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("RGB", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutputPanel(rgbStr)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("HSL", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutputPanel(hslStr)
            Spacer(modifier = Modifier.height(8.dp))

            Text("HSV", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutputPanel(hsvStr)
            Spacer(modifier = Modifier.height(16.dp))

            ColorChannelSlider("Red", red) { red = it }
            Spacer(modifier = Modifier.height(8.dp))
            ColorChannelSlider("Green", green) { green = it }
            Spacer(modifier = Modifier.height(8.dp))
            ColorChannelSlider("Blue", blue) { blue = it }
        }
    }

    @Composable
    private fun ColorChannelSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
        var textValue by remember(value) { mutableStateOf(value.toInt().toString()) }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.width(50.dp))
            OutlinedTextField(
                value = textValue,
                onValueChange = { 
                    textValue = it
                    it.toFloatOrNull()?.let { floatVal ->
                        onValueChange(floatVal.coerceIn(0f, 255f))
                    }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true
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
}