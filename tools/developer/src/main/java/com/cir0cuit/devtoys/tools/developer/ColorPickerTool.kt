package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.CodeTextStyle
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*
import android.graphics.Color as AndroidColor
import kotlin.math.roundToInt

class ColorPickerTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "color-picker",
        title = "Color Converter",
        description = "Convert colors between HEX, RGB, HSL and HSV",
        icon = Icons.Default.Colorize,
        category = ToolCategory.GRAPHICS,
        keywords = listOf("color", "hex", "rgb", "hsl", "hsv")
    )

    @Composable override fun Content(modifier: Modifier) {
        var red by rememberSaveable { mutableStateOf(100f) }
        var green by rememberSaveable { mutableStateOf(150f) }
        var blue by rememberSaveable { mutableStateOf(255f) }
        var hexInput by rememberSaveable { mutableStateOf("6496FF") }

        fun syncHex() { hexInput = rgbToHex(red, green, blue) }

        val color = Color(red.toInt(), green.toInt(), blue.toInt())

        val rgbStr = "rgb(${red.toInt()}, ${green.toInt()}, ${blue.toInt()})"

        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(red.toInt(), green.toInt(), blue.toInt(), hsv)
        val hsvStr = "hsv(${hsv[0].roundToInt() % 360}°, ${(hsv[1]*100).roundToInt()}%, ${(hsv[2]*100).roundToInt()}%)"

        val hslStr = run {
            val r = red / 255f
            val g = green / 255f
            val b = blue / 255f
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val l = (max + min) / 2
            val s = if (max == min) 0f else if (l < 0.5f) (max - min) / (max + min) else (max - min) / (2f - max - min)
            "hsl(${hsv[0].roundToInt() % 360}°, ${(s*100).roundToInt()}%, ${(l*100).roundToInt()}%)"
        }

        val copy = rememberCopy()

        ToolScaffold(descriptor.title, modifier = modifier) {
            ColorHeader(color, hexInput, { hexInput = it }) { r, g, b -> red = r; green = g; blue = b }
            Spacer(modifier = Modifier.height(8.dp))
            ColorChannelSlider("Red", red) { red = it; syncHex() }
            ColorChannelSlider("Green", green) { green = it; syncHex() }
            ColorChannelSlider("Blue", blue) { blue = it; syncHex() }
            Spacer(modifier = Modifier.height(16.dp))

            Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    listOf("Hex" to "#" + rgbToHex(red, green, blue), "RGB" to rgbStr, "HSL" to hslStr, "HSV" to hsvStr).forEach { (label, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = "Copy $label") { copy(value) }
                                .heightIn(min = 44.dp)
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(44.dp)
                            )
                            Text(value, style = CodeTextStyle, maxLines = 1, modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
