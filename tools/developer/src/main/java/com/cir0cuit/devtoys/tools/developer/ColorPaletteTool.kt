package com.cir0cuit.devtoys.tools.developer

import android.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.designsystem.PlexMono
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class ColorPaletteTool : Tool {
    override val descriptor = ToolDescriptor(
        id = "color-palette",
        title = "Color Palette Generator",
        description = "Build matching color schemes from one color",
        icon = Icons.Default.Palette,
        category = ToolCategory.GRAPHICS,
        keywords = listOf("color", "palette", "hex", "rgb", "scheme", "tailwind", "shades", "tints", "image")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        var red by rememberSaveable { mutableStateOf(52f) }
        var green by rememberSaveable { mutableStateOf(152f) }
        var blue by rememberSaveable { mutableStateOf(219f) }
        var hexInput by rememberSaveable { mutableStateOf("#3498DB") }
        var exportFormat by rememberSaveable { mutableStateOf(ScaleFormat.CSS) }

        fun syncHex() { hexInput = formatHex(argb(255, red.toInt(), green.toInt(), blue.toInt())) }

        val baseColorInt = Color.rgb(red.toInt(), green.toInt(), blue.toInt())
        val copy = rememberCopy()
        val context = LocalContext.current
        val image = rememberPickedImage()
        val imageSwatches = rememberImageSwatches(image.bitmap)
        val scale = remember(baseColorInt) { tintScale(baseColorInt) }
        val exportText = remember(scale, exportFormat) { exportScale(scale, exportFormat) }
        val useAsBase = { c: Int ->
            red = Color.red(c).toFloat(); green = Color.green(c).toFloat(); blue = Color.blue(c).toFloat(); syncHex()
        }

        ToolScaffold(descriptor.title, modifier = modifier) {
            ColorHeader(ComposeColor(baseColorInt), hexInput, { hexInput = it }, onColor = { c ->
                red = redOf(c).toFloat(); green = greenOf(c).toFloat(); blue = blueOf(c).toFloat()
            })
            Spacer(modifier = Modifier.height(8.dp))
            ColorChannelSlider("Red", red) { red = it; syncHex() }
            ColorChannelSlider("Green", green) { green = it; syncHex() }
            ColorChannelSlider("Blue", blue) { blue = it; syncHex() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = image.pick, contentPadding = ButtonDefaults.TextButtonWithIconContentPadding) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Palette from image", maxLines = 1)
                }
                if (image.bitmap != null) {
                    IconButton(onClick = image.clear) { Icon(Icons.Default.Close, contentDescription = "Remove image") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap a swatch to copy it. Long-press to make it the base color.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                PaletteRow("Complementary", listOf(baseColorInt, Color.HSVToColor(compHsv)), copy, useAsBase)
                PaletteRow("Analogous", listOf(Color.HSVToColor(ana2Hsv), baseColorInt, Color.HSVToColor(ana1Hsv)), copy, useAsBase)
                PaletteRow("Triadic", listOf(baseColorInt, Color.HSVToColor(tri1Hsv), Color.HSVToColor(tri2Hsv)), copy, useAsBase)
                PaletteRow("Monochromatic", listOf(Color.HSVToColor(mono1Hsv), baseColorInt, Color.HSVToColor(mono2Hsv), Color.HSVToColor(mono3Hsv)), copy, useAsBase)
                if (imageSwatches.isNotEmpty()) {
                    PaletteRow("From image", imageSwatches.map { it.second }, copy, useAsBase)
                }
                PaletteRow("Tints and shades", scale.map { it.second }, copy, useAsBase, captions = scale.map { it.first.toString() })
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    SelectField("Export as", exportFormat, ScaleFormat.entries, { exportFormat = it }, Modifier.weight(1f)) { it.label }
                    IconButton(onClick = { shareText(context, exportText) }, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share scale")
                    }
                }
                OutputPanel(exportText, label = "Scale", isCopyable = true)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun PaletteRow(
        title: String,
        colors: List<Int>,
        onCopy: (String) -> Unit,
        onUseAsBase: (Int) -> Unit,
        captions: List<String>? = null // shown instead of the hex code when swatches are too narrow for it
    ) {
        val haptics = LocalHapticFeedback.current
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Row(modifier = Modifier.fillMaxWidth().height(44.dp).clip(MaterialTheme.shapes.medium)) {
                colors.forEachIndexed { index, colorInt ->
                    val hex = "#%06X".format(0xFFFFFF and colorInt)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(ComposeColor(colorInt))
                            .combinedClickable(
                                onClickLabel = "Copy $hex",
                                onLongClickLabel = "Make $hex the base color",
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onUseAsBase(colorInt)
                                },
                                onClick = { onCopy(hex) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = captions?.getOrNull(index) ?: hex,
                            color = if (Color.luminance(colorInt) > 0.5) ComposeColor.Black else ComposeColor.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
