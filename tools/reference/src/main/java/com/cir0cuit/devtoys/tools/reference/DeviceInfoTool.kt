package com.cir0cuit.devtoys.tools.reference

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.LocaleList
import android.util.DisplayMetrics
import android.view.Display
import android.webkit.WebSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.ToolScaffold
import com.cir0cuit.devtoys.core.ui.shareText
import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolCategory
import com.cir0cuit.devtoys.tools.base.ToolDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

internal typealias InfoSection = Pair<String, List<Pair<String, String>>>

internal fun densityBucket(dpi: Int): String = when {
    dpi <= 120 -> "ldpi"
    dpi <= 160 -> "mdpi"
    dpi <= 213 -> "tvdpi"
    dpi <= 240 -> "hdpi"
    dpi <= 320 -> "xhdpi"
    dpi <= 480 -> "xxhdpi"
    else -> "xxxhdpi"
}

/** 1536 -> "1.5 KiB"; binary units, one decimal. */
internal fun formatBinarySize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    var v = bytes.toDouble()
    var unit = 0
    while (v >= 1024 && unit < 5) { v /= 1024; unit++ }
    return String.format(Locale.ROOT, "%.1f %s", v, listOf("B", "KiB", "MiB", "GiB", "TiB", "PiB")[unit])
}

internal fun infoMarkdown(sections: List<InfoSection>): String = sections.joinToString("\n\n") { (title, rows) ->
    "### $title\n" + rows.joinToString("\n") { (k, v) -> "- **$k**: $v" }
}

private fun collectDeviceInfo(context: Context, screenWidthDp: Int, screenHeightDp: Int, fontScale: Float): List<InfoSection> {
    val device = buildList {
        add("Manufacturer" to Build.MANUFACTURER)
        add("Model" to Build.MODEL)
        add("Brand" to Build.BRAND)
        add("Device" to Build.DEVICE)
        add("Product" to Build.PRODUCT)
        add("Hardware" to Build.HARDWARE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add("SoC" to "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}")
        add("Supported ABIs" to Build.SUPPORTED_ABIS.joinToString(", "))
    }
    val os = listOf(
        "Android version" to Build.VERSION.RELEASE,
        "API level" to Build.VERSION.SDK_INT.toString(),
        "Security patch" to Build.VERSION.SECURITY_PATCH,
        "Build" to Build.DISPLAY,
        "Fingerprint" to Build.FINGERPRINT,
        "Kernel" to (System.getProperty("os.version") ?: "Unknown")
    )

    val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
    val metrics = DisplayMetrics().also {
        @Suppress("DEPRECATION")
        display.getRealMetrics(it)
    }
    val dpi = metrics.densityDpi
    val maxHz = display.supportedModes.maxOf { it.refreshRate }
    val screen = listOf(
        "Resolution" to "${metrics.widthPixels} × ${metrics.heightPixels} px",
        "App window" to "$screenWidthDp × $screenHeightDp dp",
        "Density" to "$dpi dpi (${densityBucket(dpi)}), ${metrics.density}x",
        "Refresh rate" to "%.0f Hz (max %.0f Hz)".format(Locale.ROOT, display.refreshRate, maxHz),
        "Font scale" to "${fontScale}x"
    )

    val memInfo = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
    val memory = listOf(
        "Total RAM" to formatBinarySize(memInfo.totalMem),
        "Available RAM" to formatBinarySize(memInfo.availMem),
        "App heap limit" to formatBinarySize(Runtime.getRuntime().maxMemory())
    )

    val zone = ZoneId.systemDefault()
    val locale = listOf(
        "Locale" to Locale.getDefault().toLanguageTag(),
        "Preferred languages" to LocaleList.getDefault().toLanguageTags().replace(",", ", "),
        "Time zone" to "${zone.id} (UTC${zone.rules.getOffset(Instant.now()).id.replace("Z", "+00:00")})"
    )
    return listOf("Device" to device, "Android" to os, "Screen" to screen, "Memory" to memory, "Locale and time" to locale)
}

class DeviceInfoTool : Tool {
    override val descriptor = ToolDescriptor(
        "device-info",
        "Device Information",
        "Show OS, screen, ABI, memory and locale",
        Icons.Default.PhoneAndroid,
        ToolCategory.NETWORK_SYSTEM,
        listOf("device", "phone", "android", "api", "sdk", "screen", "dpi", "abi", "memory", "locale", "user agent", "build")
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val context = LocalContext.current
        val config = LocalConfiguration.current
        val clipboard = LocalClipboardManager.current
        val base = remember(config) { collectDeviceInfo(context, config.screenWidthDp, config.screenHeightDp, config.fontScale) }
        // Loading the WebView provider can take a few hundred ms, so keep it off the main thread.
        val userAgent by produceState<String?>(null) {
            value = withContext(Dispatchers.Default) {
                runCatching { WebSettings.getDefaultUserAgent(context) }.getOrDefault("Not available")
            }
        }
        val sections = base + ("WebView" to listOf("User agent" to (userAgent ?: "Loading…")))

        ToolScaffold(descriptor.title, modifier = modifier) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(infoMarkdown(sections)))
                        confirmCopied(context, "Device information")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Copy Markdown", maxLines = 1) }
                OutlinedButton(onClick = { shareText(context, infoMarkdown(sections)) }, modifier = Modifier.weight(1f)) {
                    Text("Share", maxLines = 1)
                }
            }
            sections.forEach { (title, rows) ->
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                LabeledValueSlab(rows)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
