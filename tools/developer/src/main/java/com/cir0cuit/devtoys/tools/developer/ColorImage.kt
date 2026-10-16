package com.cir0cuit.devtoys.tools.developer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.cir0cuit.devtoys.core.designsystem.LocalSlabColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Image input shared by ColorPickerTool (eyedropper) and ColorPaletteTool (palette from image).

/** A picked image, decoded and downsampled. Only the Uri is saved, so it is decoded again after rotation. */
internal class PickedImage(val bitmap: Bitmap?, val pick: () -> Unit, val clear: () -> Unit)

@Composable
internal fun rememberPickedImage(): PickedImage {
    val context = LocalContext.current
    var uri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        if (picked != null) uri = picked
    }
    val bitmap by produceState<Bitmap?>(null, uri) {
        val source = uri ?: run { value = null; return@produceState }
        value = withContext(Dispatchers.IO) { runCatching { decodeSampled(context, source, 1024) }.getOrNull() }
        if (value == null) {
            uri = null
            Toast.makeText(context, "Could not open the image. Pick a JPEG, PNG or WebP picture.", Toast.LENGTH_LONG).show()
        }
    }
    return PickedImage(
        bitmap,
        pick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        clear = { uri = null }
    )
}

// ponytail: ignores EXIF rotation, so some camera photos show sideways; read ExifInterface if that matters.
private fun decodeSampled(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

/** Swatches androidx.palette finds in [bitmap], as (label, ARGB) in a fixed order; missing ones are left out. */
@Composable
internal fun rememberImageSwatches(bitmap: Bitmap?): List<Pair<String, Int>> {
    val swatches by produceState(emptyList<Pair<String, Int>>(), bitmap) {
        value = if (bitmap == null) emptyList() else withContext(Dispatchers.Default) {
            val p = Palette.from(bitmap).generate()
            listOf(
                "Light vibrant" to p.lightVibrantSwatch, "Vibrant" to p.vibrantSwatch, "Dark vibrant" to p.darkVibrantSwatch,
                "Light muted" to p.lightMutedSwatch, "Muted" to p.mutedSwatch, "Dark muted" to p.darkMutedSwatch
            ).mapNotNull { (label, swatch) -> swatch?.let { label to it.rgb } }
        }
    }
    return swatches
}

/** Shows [bitmap]; tapping or dragging picks the pixel under the finger, with a magnifying loupe above it. */
@Composable
internal fun ImageEyedropper(bitmap: Bitmap, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var touch by remember(bitmap) { mutableStateOf<Offset?>(null) }
    var pixel by remember(bitmap) { mutableStateOf(IntOffset.Zero) }

    fun fit(size: Size): Rect {
        val scale = minOf(size.width / bitmap.width, size.height / bitmap.height)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        return Rect(Offset((size.width - w) / 2, (size.height - h) / 2), Size(w, h))
    }
    fun pick(at: Offset, size: Size) {
        val r = fit(size)
        val x = ((at.x - r.left) / r.width * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = ((at.y - r.top) / r.height * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        touch = Offset(r.left + (x + 0.5f) * r.width / bitmap.width, r.top + (y + 0.5f) * r.height / bitmap.height)
        pixel = IntOffset(x, y)
        onPick(bitmap.getPixel(x, y) or (0xFF shl 24))
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(LocalSlabColor.current)
            .pointerInput(bitmap) { detectTapGestures { pick(it, Size(size.width.toFloat(), size.height.toFloat())) } }
            .pointerInput(bitmap) {
                val s = Size(size.width.toFloat(), size.height.toFloat())
                detectDragGestures(onDragStart = { pick(it, s) }) { change, _ -> change.consume(); pick(change.position, s) }
            }
    ) {
        val r = fit(size)
        drawImage(
            image,
            dstOffset = IntOffset(r.left.toInt(), r.top.toInt()),
            dstSize = IntSize(r.width.toInt(), r.height.toInt()),
            filterQuality = FilterQuality.Low
        )
        val at = touch ?: return@Canvas
        val radius = 44.dp.toPx()
        // Loupe above the finger (below it when the finger is near the top), showing 11x11 pixels.
        val center = Offset(at.x.coerceIn(radius, size.width - radius), if (at.y - radius * 2.2f > 0) at.y - radius * 1.6f else at.y + radius * 1.6f)
        val half = 5
        val src = IntOffset((pixel.x - half).coerceIn(0, (bitmap.width - 11).coerceAtLeast(0)), (pixel.y - half).coerceIn(0, (bitmap.height - 11).coerceAtLeast(0)))
        val srcSize = IntSize(minOf(11, bitmap.width), minOf(11, bitmap.height))
        val circle = Path().apply { addOval(Rect(center, radius)) }
        clipPath(circle) {
            drawImage(
                image,
                srcOffset = src,
                srcSize = srcSize,
                dstOffset = IntOffset((center.x - radius).toInt(), (center.y - radius).toInt()),
                dstSize = IntSize((radius * 2).toInt(), (radius * 2).toInt()),
                filterQuality = FilterQuality.None
            )
        }
        val cell = radius * 2 / 11
        val cellCenter = Offset(
            center.x - radius + (pixel.x - src.x + 0.5f) * cell,
            center.y - radius + (pixel.y - src.y + 0.5f) * cell
        )
        drawRect(Color.White, Offset(cellCenter.x - cell / 2, cellCenter.y - cell / 2), Size(cell, cell), style = Stroke(1.5.dp.toPx()))
        drawCircle(Color.White, radius, center, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black, radius + 1.5.dp.toPx(), center, style = Stroke(1.dp.toPx()))
        drawCircle(Color.White, 8.dp.toPx(), at, style = Stroke(2.dp.toPx()))
        drawCircle(Color.Black, 9.dp.toPx(), at, style = Stroke(1.dp.toPx()))
    }
}
