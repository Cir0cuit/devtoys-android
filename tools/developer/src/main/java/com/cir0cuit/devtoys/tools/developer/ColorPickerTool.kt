package com.cir0cuit.devtoys.tools.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

class ColorPickerTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "color-picker",
        title = "Color Converter",
        description = "Convert colors between HEX, RGB, HSL and HSV",
        icon = Icons.Default.Colorize,
        category = ToolCategory.GRAPHICS,
        keywords = listOf("color", "hex", "rgb", "hsl", "hsv", "oklch", "lab", "cmyk", "eyedropper")
    )

    @Composable override fun Content(modifier: Modifier) {
        var red by rememberSaveable { mutableStateOf(100f) }
        var green by rememberSaveable { mutableStateOf(150f) }
        var blue by rememberSaveable { mutableStateOf(255f) }
        var alpha by rememberSaveable { mutableStateOf(255f) }
        var colorText by rememberSaveable { mutableStateOf("#6496FF") }

        val current = argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt())
        fun syncText() { colorText = formatHex(argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt())) }
        fun setColor(c: Int) {
            alpha = alphaOf(c).toFloat(); red = redOf(c).toFloat(); green = greenOf(c).toFloat(); blue = blueOf(c).toFloat()
        }

        val formats = remember(current) { colorFormats(current) }
        val image = rememberPickedImage()
        val swatches = rememberImageSwatches(image.bitmap)
        val copy = rememberCopy()

        ToolScaffold(descriptor.title, modifier = modifier) {
            ColorHeader(Color(current), colorText, { colorText = it }, ::setColor)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = image.pick, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("From image", maxLines = 1)
                }
                if (image.bitmap != null) {
                    TextButton(onClick = image.clear, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Remove image", maxLines = 1)
                    }
                }
            }
            image.bitmap?.let { bitmap ->
                ImageEyedropper(bitmap, onPick = { setColor(it); syncText() })
                if (swatches.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Dominant colors", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        swatches.forEach { (label, c) ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .background(Color(c), MaterialTheme.shapes.small)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                    .clickable(onClickLabel = "Use $label ${formatHex(c)}") { setColor(c); syncText() }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ColorChannelSlider("Red", red) { red = it; syncText() }
            ColorChannelSlider("Green", green) { green = it; syncText() }
            ColorChannelSlider("Blue", blue) { blue = it; syncText() }
            ColorChannelSlider("Alpha", alpha) { alpha = it; syncText() }
            Spacer(modifier = Modifier.height(16.dp))

            Surface(color = LocalSlabColor.current, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    formats.forEach { (label, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = "Copy $label") { copy(value.removePrefix("≈ ")) }
                                .heightIn(min = 44.dp)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, style = CodeTextStyle)
                            }
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp).size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
