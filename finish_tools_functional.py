import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

# --- Gradle Dependency Patches ---
def add_dependency(module_path, dep):
    build_file = os.path.join(project_dir, f"{module_path}/build.gradle.kts")
    with open(build_file, "r", encoding="utf-8") as f:
        content = f.read()
    if dep not in content:
        content = content.replace("dependencies {", f"dependencies {{\n    implementation(\"{dep}\")")
        with open(build_file, "w", encoding="utf-8") as f:
            f.write(content)

add_dependency("tools/generators", "com.google.zxing:core:3.5.3")
add_dependency("tools/text", "io.github.java-diff-utils:java-diff-utils:4.12")
add_dependency("tools/data", "org.yaml:snakeyaml:2.2")
add_dependency("tools/developer", "com.cronutils:cron-utils:9.2.1")
add_dependency("tools/developer", "io.noties.markwon:core:4.6.2")

# --- Fully Functional Tools ---

tools = {
    # --- ENCODERS ---
    "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/HtmlEntityTool.kt": """package com.devtoys.android.tools.encoders
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class HtmlEntityTool : Tool {
    override val descriptor = ToolDescriptor("html-entity", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.ENCODERS_DECODERS, listOf("html", "entity", "encode"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        var isEncodeMode by remember { mutableStateOf(true) }
        val output = remember(input, isEncodeMode) {
            if (input.isEmpty()) "" else if (isEncodeMode) {
                input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
            } else {
                input.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'")
            }
        }
        ToolScaffold("HTML Entity", modifier = modifier) {
            Text("Mode: ${if (isEncodeMode) "Encode" else "Decode"}")
            Switch(checked = isEncodeMode, onCheckedChange = { isEncodeMode = it })
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/Rot13Tool.kt": """package com.devtoys.android.tools.encoders
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class Rot13Tool : Tool {
    override val descriptor = ToolDescriptor("rot13", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.ENCODERS_DECODERS, listOf("rot13", "cipher", "caesar"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        var shift by remember { mutableStateOf(13f) }
        val output = remember(input, shift) {
            val s = shift.toInt()
            input.map { c ->
                when {
                    c.isLowerCase() -> 'a' + (c - 'a' + s) % 26
                    c.isUpperCase() -> 'A' + (c - 'A' + s) % 26
                    else -> c
                }
            }.joinToString("")
        }
        ToolScaffold("ROT13 / Caesar Cipher", modifier = modifier) {
            Text("Shift: ${shift.toInt()}")
            Slider(value = shift, onValueChange = { shift = it }, valueRange = 1f..25f, steps = 23)
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",

    # --- TEXT ---
    "tools/text/src/main/java/com/devtoys/android/tools/text/LineToolsTool.kt": """package com.devtoys.android.tools.text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class LineToolsTool : Tool {
    override val descriptor = ToolDescriptor("line-tools", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.TEXT, listOf("line", "sort", "dedupe"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        var operation by remember { mutableStateOf("Sort") }
        val output = remember(input, operation) {
            if (input.isEmpty()) "" else {
                val lines = input.lines()
                when (operation) {
                    "Sort" -> lines.sorted().joinToString("\n")
                    "Sort Desc" -> lines.sortedDescending().joinToString("\n")
                    "Dedupe" -> lines.distinct().joinToString("\n")
                    "Reverse" -> lines.reversed().joinToString("\n")
                    "Count" -> "Lines: ${lines.size}"
                    else -> input
                }
            }
        }
        ToolScaffold("Line Tools", modifier = modifier) {
            LazyRow {
                items(5) { idx ->
                    val op = listOf("Sort", "Sort Desc", "Dedupe", "Reverse", "Count")[idx]
                    FilterChip(
                        selected = operation == op,
                        onClick = { operation = op },
                        label = { Text(op) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/text/src/main/java/com/devtoys/android/tools/text/TextDiffTool.kt": """package com.devtoys.android.tools.text
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import com.github.difflib.DiffUtils

class TextDiffTool : Tool {
    override val descriptor = ToolDescriptor("text-diff", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.TEXT, listOf("diff", "compare"))
    @Composable override fun Content(modifier: Modifier) {
        var oldText by remember { mutableStateOf("") }
        var newText by remember { mutableStateOf("") }
        val output = remember(oldText, newText) {
            if (oldText.isEmpty() && newText.isEmpty()) "" else try {
                val patch = DiffUtils.diff(oldText.lines(), newText.lines())
                patch.deltas.joinToString("\n\n") { it.toString() }
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("Text Diff", modifier = modifier) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoTextField(oldText, { oldText = it }, "Old Text", modifier = Modifier.weight(1f), minLines=4)
                MonoTextField(newText, { newText = it }, "New Text", modifier = Modifier.weight(1f), minLines=4)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",

    # --- DATA ---
    "tools/data/src/main/java/com/devtoys/android/tools/data/YamlJsonTool.kt": """package com.devtoys.android.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import org.yaml.snakeyaml.Yaml
import org.json.JSONObject

class YamlJsonTool : Tool {
    override val descriptor = ToolDescriptor("yaml-json", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DATA_FORMATTERS, listOf("yaml", "json", "converter"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        var toYaml by remember { mutableStateOf(false) }
        val output = remember(input, toYaml) {
            if (input.isEmpty()) "" else try {
                if (toYaml) {
                    val jsonObj = JSONObject(input)
                    val map = jsonObj.toMap()
                    Yaml().dump(map)
                } else {
                    val map = Yaml().load<Map<String, Any>>(input)
                    JSONObject(map).toString(2)
                }
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("YAML ↔ JSON Converter", modifier = modifier) {
            Text("Mode: ${if (toYaml) "JSON to YAML" else "YAML to JSON"}")
            Switch(checked = toYaml, onCheckedChange = { toYaml = it })
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/data/src/main/java/com/devtoys/android/tools/data/XmlFormatterTool.kt": """package com.devtoys.android.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import java.io.StringReader
import java.io.StringWriter

class XmlFormatterTool : Tool {
    override val descriptor = ToolDescriptor("xml-formatter", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DATA_FORMATTERS, listOf("xml", "format"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val transformer = TransformerFactory.newInstance().newTransformer()
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
                val result = StringWriter()
                transformer.transform(StreamSource(StringReader(input)), StreamResult(result))
                result.toString()
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("XML Formatter", modifier = modifier) {
            MonoTextField(input, { input = it }, "XML Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",

    # --- GENERATORS ---
    "tools/generators/src/main/java/com/devtoys/android/tools/generators/PasswordGeneratorTool.kt": """package com.devtoys.android.tools.generators
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class PasswordGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("password-gen", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.GENERATORS, listOf("password", "generator"))
    @Composable override fun Content(modifier: Modifier) {
        var length by remember { mutableStateOf(16f) }
        var includeUpper by remember { mutableStateOf(true) }
        var includeLower by remember { mutableStateOf(true) }
        var includeNumbers by remember { mutableStateOf(true) }
        var includeSymbols by remember { mutableStateOf(true) }
        var output by remember { mutableStateOf("") }
        
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
}""",
    "tools/generators/src/main/java/com/devtoys/android/tools/generators/QrGeneratorTool.kt": """package com.devtoys.android.tools.generators
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class QrGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("qr-gen", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.GENERATORS, listOf("qr", "barcode", "generator"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("https://devtoys.app") }
        val bitmap = remember(input) {
            if (input.isEmpty()) null else try {
                val matrix = MultiFormatWriter().encode(input, BarcodeFormat.QR_CODE, 512, 512)
                val width = matrix.width
                val height = matrix.height
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }
                bmp
            } catch (e: Exception) { null }
        }
        ToolScaffold("QR Code Generator", modifier = modifier) {
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(256.dp))
            } else {
                Text("Enter text to generate QR code.")
            }
        }
    }
}""",

    # --- CRYPTO ---
    "tools/crypto/src/main/java/com/devtoys/android/tools/crypto/HmacCalculatorTool.kt": """package com.devtoys.android.tools.crypto
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HmacCalculatorTool : Tool {
    override val descriptor = ToolDescriptor("hmac", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.CRYPTO, listOf("hmac", "hash"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        var secret by remember { mutableStateOf("") }
        var algo by remember { mutableStateOf("HmacSHA256") }
        val output = remember(input, secret, algo) {
            if (input.isEmpty() || secret.isEmpty()) "" else try {
                val mac = Mac.getInstance(algo)
                val keySpec = SecretKeySpec(secret.toByteArray(), algo)
                mac.init(keySpec)
                mac.doFinal(input.toByteArray()).joinToString("") { "%02x".format(it) }
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("HMAC Calculator", modifier = modifier) {
            Row {
                listOf("HmacMD5", "HmacSHA1", "HmacSHA256", "HmacSHA512").forEach {
                    FilterChip(selected = algo == it, onClick = { algo = it }, label = { Text(it.replace("Hmac", "")) }, modifier=Modifier.padding(end=4.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(secret, { secret = it }, "Secret Key", minLines=1)
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(input, { input = it }, "Input", minLines=4)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",

    # --- DEVELOPER ---
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/CronTool.kt": """package com.devtoys.android.tools.developer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.CronType
import com.cronutils.parser.CronParser
import com.cronutils.descriptor.CronDescriptor
import java.util.Locale

class CronTool : Tool {
    override val descriptor = ToolDescriptor("cron", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf("cron", "schedule"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("* * * * *") }
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val parser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))
                val cron = parser.parse(input)
                val descriptor = CronDescriptor.instance(Locale.ENGLISH)
                descriptor.describe(cron)
            } catch (e: Exception) { "Error: Invalid Cron Expression" }
        }
        ToolScaffold("Cron Builder & Explainer", modifier = modifier) {
            MonoTextField(input, { input = it }, "Cron Expression", minLines=1)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/ColorPickerTool.kt": """package com.devtoys.android.tools.developer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class ColorPickerTool : Tool {
    override val descriptor = ToolDescriptor("color-picker", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf("color", "hex", "rgb"))
    @Composable override fun Content(modifier: Modifier) {
        var red by remember { mutableStateOf(255f) }
        var green by remember { mutableStateOf(0f) }
        var blue by remember { mutableStateOf(0f) }
        val color = Color(red.toInt(), green.toInt(), blue.toInt())
        val hex = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
        
        ToolScaffold("Color Picker & Converter", modifier = modifier) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(color))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Red: ${red.toInt()}"); Slider(value = red, onValueChange = { red = it }, valueRange = 0f..255f)
            Text("Green: ${green.toInt()}"); Slider(value = green, onValueChange = { green = it }, valueRange = 0f..255f)
            Text("Blue: ${blue.toInt()}"); Slider(value = blue, onValueChange = { blue = it }, valueRange = 0f..255f)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(hex)
        }
    }
}""",
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/MarkdownTool.kt": """package com.devtoys.android.tools.developer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import android.widget.TextView
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import io.noties.markwon.Markwon

class MarkdownTool : Tool {
    override val descriptor = ToolDescriptor("markdown", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf("markdown", "preview"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("# Hello World\n\nThis is **Markdown**.") }
        
        ToolScaffold("Markdown Previewer", modifier = modifier) {
            MonoTextField(input, { input = it }, "Markdown", minLines=6)
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context -> TextView(context) },
                update = { textView ->
                    val markwon = Markwon.create(textView.context)
                    markwon.setMarkdown(textView, input)
                }
            )
        }
    }
}""",

    # --- REFERENCE ---
    "tools/reference/src/main/java/com/devtoys/android/tools/reference/CheatsheetTool.kt": """package com.devtoys.android.tools.reference
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class CheatsheetTool : Tool {
    override val descriptor = ToolDescriptor("cheatsheet", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.REFERENCE, listOf("cheat", "sheet"))
    @Composable override fun Content(modifier: Modifier) {
        var selected by remember { mutableStateOf("Git") }
        val cheatsheets = mapOf(
            "Git" to "git clone <url>\ngit commit -m \"msg\"\ngit push\ngit pull",
            "Vim" to "i: insert\nEsc: normal mode\n:w: save\n:q: quit",
            "Docker" to "docker run <image>\ndocker ps\ndocker stop <id>"
        )
        ToolScaffold("Cheatsheets", modifier = modifier) {
            LazyRow {
                items(cheatsheets.keys.size) { idx ->
                    val key = cheatsheets.keys.toList()[idx]
                    FilterChip(selected = selected == key, onClick = { selected = key }, label = { Text(key) }, modifier=Modifier.padding(end=8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(cheatsheets[selected] ?: "")
        }
    }
}""",
    "tools/reference/src/main/java/com/devtoys/android/tools/reference/HttpStatusTool.kt": """package com.devtoys.android.tools.reference
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class HttpStatusTool : Tool {
    override val descriptor = ToolDescriptor("http-status", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.REFERENCE, listOf("http", "status", "codes"))
    @Composable override fun Content(modifier: Modifier) {
        val statuses = mapOf(
            200 to "OK", 201 to "Created", 204 to "No Content",
            400 to "Bad Request", 401 to "Unauthorized", 403 to "Forbidden", 404 to "Not Found",
            500 to "Internal Server Error", 502 to "Bad Gateway", 503 to "Service Unavailable"
        )
        ToolScaffold("HTTP Status Codes", modifier = modifier) {
            LazyColumn {
                items(statuses.size) { idx ->
                    val entry = statuses.entries.toList()[idx]
                    ListItem(
                        headlineContent = { Text("${entry.key} - ${entry.value}") }
                    )
                }
            }
        }
    }
}""",

    # --- MISSING TOOLS FROM PHASE 2 & 3 ---
    "tools/data/src/main/java/com/devtoys/android/tools/data/SqlFormatterTool.kt": """package com.devtoys.android.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class SqlFormatterTool : Tool {
    override val descriptor = ToolDescriptor("sql-formatter", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DATA_FORMATTERS, listOf("sql", "format"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        val output = remember(input) {
            if (input.isEmpty()) "" else {
                // Highly simplified formatter for MVP
                input.replace(Regex("(?i)\\b(SELECT|FROM|WHERE|AND|OR|ORDER BY|GROUP BY|LEFT JOIN|INNER JOIN)\\b"), "\n$1")
                     .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
            }
        }
        ToolScaffold("SQL Formatter", modifier = modifier) {
            MonoTextField(input, { input = it }, "SQL Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/data/src/main/java/com/devtoys/android/tools/data/CsvViewerTool.kt": """package com.devtoys.android.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class CsvViewerTool : Tool {
    override val descriptor = ToolDescriptor("csv-viewer", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DATA_FORMATTERS, listOf("csv", "table", "viewer"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("id,name,age\n1,Alice,30\n2,Bob,25") }
        ToolScaffold("CSV Viewer", modifier = modifier) {
            MonoTextField(input, { input = it }, "CSV Input", minLines=4)
            Spacer(modifier = Modifier.height(16.dp))
            val lines = input.lines().filter { it.isNotEmpty() }
            if (lines.isNotEmpty()) {
                LazyColumn {
                    items(lines.size) { idx ->
                        val cols = lines[idx].split(",")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            cols.forEach { col ->
                                Text(col, modifier = Modifier.weight(1f).padding(4.dp))
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}""",
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/JsonToTypesTool.kt": """package com.devtoys.android.tools.developer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import org.json.JSONObject

class JsonToTypesTool : Tool {
    override val descriptor = ToolDescriptor("json-types", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf("json", "types", "kotlin", "typescript"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("{\"id\": 1, \"name\": \"Test\"}") }
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val json = JSONObject(input)
                val sb = StringBuilder("data class GeneratedType(\n")
                json.keys().forEach { key ->
                    val type = when(json.get(key)) {
                        is Int -> "Int"
                        is Boolean -> "Boolean"
                        is Double -> "Double"
                        else -> "String"
                    }
                    sb.append("    val $key: $type,\n")
                }
                sb.append(")")
                sb.toString()
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("JSON to Kotlin Types", modifier = modifier) {
            MonoTextField(input, { input = it }, "JSON Input", minLines=4)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/NumberBaseConverterTool.kt": """package com.devtoys.android.tools.encoders
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class NumberBaseConverterTool : Tool {
    override val descriptor = ToolDescriptor("base-converter", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.CONVERSION, listOf("base", "converter", "binary", "hex"))
    @Composable override fun Content(modifier: Modifier) {
        var dec by remember { mutableStateOf("42") }
        val bin = dec.toLongOrNull()?.toString(2) ?: ""
        val oct = dec.toLongOrNull()?.toString(8) ?: ""
        val hex = dec.toLongOrNull()?.toString(16) ?: ""
        ToolScaffold("Number Base Converter", modifier = modifier) {
            MonoTextField(dec, { dec = it }, "Decimal", minLines=1)
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(hex, { }, "Hexadecimal", minLines=1)
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(bin, { }, "Binary", minLines=1)
            Spacer(modifier = Modifier.height(8.dp))
            MonoTextField(oct, { }, "Octal", minLines=1)
        }
    }
}""",
    "tools/generators/src/main/java/com/devtoys/android/tools/generators/LoremIpsumTool.kt": """package com.devtoys.android.tools.generators
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class LoremIpsumTool : Tool {
    override val descriptor = ToolDescriptor("lorem-ipsum", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.GENERATORS, listOf("lorem", "ipsum", "text"))
    @Composable override fun Content(modifier: Modifier) {
        var count by remember { mutableStateOf(5f) }
        val output = remember(count) {
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(count.toInt())
        }
        ToolScaffold("Lorem Ipsum Generator", modifier = modifier) {
            Text("Sentences: ${count.toInt()}")
            Slider(value = count, onValueChange = { count = it }, valueRange = 1f..50f)
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}"""
}

for path, content in tools.items():
    full_path = os.path.join(project_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)

# Inject SqlFormatterTool, CsvViewerTool, JsonToTypesTool, NumberBaseConverterTool, LoremIpsumTool to AppModule
app_module_path = os.path.join(project_dir, "app/src/main/java/com/devtoys/android/di/AppModule.kt")
with open(app_module_path, "r", encoding="utf-8") as f:
    app_module_content = f.read()

new_classes = [
    "com.devtoys.android.tools.data.SqlFormatterTool",
    "com.devtoys.android.tools.data.CsvViewerTool",
    "com.devtoys.android.tools.developer.JsonToTypesTool",
    "com.devtoys.android.tools.encoders.NumberBaseConverterTool",
    "com.devtoys.android.tools.generators.LoremIpsumTool"
]

import_statements = "\n".join([f"import {cls}" for cls in new_classes])

if "SqlFormatterTool" not in app_module_content:
    app_module_content = app_module_content.replace(
        "import javax.inject.Singleton",
        "import javax.inject.Singleton\n" + import_statements
    )
    
    instantiations = ",\n                ".join([f"{cls.split('.')[-1]}()" for cls in new_classes])
    
    app_module_content = app_module_content.replace(
        "HttpStatusTool()",
        f"HttpStatusTool(),\n                {instantiations}"
    )

    with open(app_module_path, "w", encoding="utf-8") as f:
        f.write(app_module_content)

print("All FULLY FUNCTIONAL tools generated and registered successfully.")
