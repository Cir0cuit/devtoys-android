import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

tools = {
    "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/UrlEncoderTool.kt": """package com.devtoys.android.tools.encoders

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import java.net.URLDecoder
import java.net.URLEncoder

class UrlEncoderTool : Tool {
    override val descriptor = ToolDescriptor("url", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.ENCODERS_DECODERS, listOf("url", "encode"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        var isEncodeMode by remember { mutableStateOf(true) }
        val output = remember(input, isEncodeMode) {
            try {
                if (input.isEmpty()) "" else if (isEncodeMode) URLEncoder.encode(input, "UTF-8") else URLDecoder.decode(input, "UTF-8")
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("URL Encoder / Decoder", modifier = modifier) {
            Text("Mode: ${if (isEncodeMode) "Encode" else "Decode"}")
            androidx.compose.material3.Switch(checked = isEncodeMode, onCheckedChange = { isEncodeMode = it })
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/HexEncoderTool.kt": """package com.devtoys.android.tools.encoders

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class HexEncoderTool : Tool {
    override val descriptor = ToolDescriptor("hex", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.ENCODERS_DECODERS, listOf("hex", "encode"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        var isEncodeMode by remember { mutableStateOf(true) }
        val output = remember(input, isEncodeMode) {
            try {
                if (input.isEmpty()) "" else if (isEncodeMode) input.toByteArray().joinToString("") { "%02x".format(it) } else input.chunked(2).map { it.toInt(16).toByte() }.toByteArray().let { String(it) }
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("Hex Encoder / Decoder", modifier = modifier) {
            Text("Mode: ${if (isEncodeMode) "Encode" else "Decode"}")
            Switch(checked = isEncodeMode, onCheckedChange = { isEncodeMode = it })
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/data/src/main/java/com/devtoys/android/tools/data/JsonFormatterTool.kt": """package com.devtoys.android.tools.data

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import org.json.JSONArray
import org.json.JSONObject

class JsonFormatterTool : Tool {
    override val descriptor = ToolDescriptor("json-formatter", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DATA_FORMATTERS, listOf("json", "format"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val trimmed = input.trim()
                if (trimmed.startsWith("[")) JSONArray(trimmed).toString(2) else JSONObject(trimmed).toString(2)
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("JSON Formatter", modifier = modifier) {
            MonoTextField(input, { input = it }, "JSON Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/RegexTesterTool.kt": """package com.devtoys.android.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import java.util.regex.Pattern

class RegexTesterTool : Tool {
    override val descriptor = ToolDescriptor("regex", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf("regex", "tester"))
    @Composable override fun Content(modifier: Modifier) {
        var regex by remember { mutableStateOf("") }
        var input by remember { mutableStateOf("") }
        val output = remember(regex, input) {
            if (regex.isEmpty() || input.isEmpty()) "" else try {
                val matcher = Pattern.compile(regex).matcher(input)
                val matches = mutableListOf<String>()
                while (matcher.find()) matches.add("Match: ${matcher.group()}")
                matches.joinToString("\n")
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("Regex Tester", modifier = modifier) {
            MonoTextField(regex, { regex = it }, "Regex Pattern", minLines = 2)
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Text Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/crypto/src/main/java/com/devtoys/android/tools/crypto/HashCalculatorTool.kt": """package com.devtoys.android.tools.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import java.security.MessageDigest

class HashCalculatorTool : Tool {
    override val descriptor = ToolDescriptor("hash", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.CRYPTO, listOf("hash", "md5", "sha"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val md5 = MessageDigest.getInstance("MD5").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
                val sha1 = MessageDigest.getInstance("SHA-1").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
                val sha256 = MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
                "MD5: $md5\nSHA-1: $sha1\nSHA-256: $sha256"
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("Hash Calculator", modifier = modifier) {
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/JwtDecoderTool.kt": """package com.devtoys.android.tools.developer

import android.util.Base64
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

class JwtDecoderTool : Tool {
    override val descriptor = ToolDescriptor("jwt", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf("jwt", "decode"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        val output = remember(input) {
            if (input.isEmpty()) "" else try {
                val parts = input.split(".")
                if (parts.size >= 2) {
                    val header = String(Base64.decode(parts[0], Base64.URL_SAFE))
                    val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                    "Header:\n${JSONObject(header).toString(2)}\n\nPayload:\n${JSONObject(payload).toString(2)}"
                } else "Invalid JWT format"
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("JWT Decoder", modifier = modifier) {
            MonoTextField(input, { input = it }, "JWT Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/TimestampTool.kt": """package com.devtoys.android.tools.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimestampTool : Tool {
    override val descriptor = ToolDescriptor("timestamp", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf("time", "stamp"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf((System.currentTimeMillis() / 1000).toString()) }
        val output = remember(input) {
            try {
                val sec = input.toLongOrNull() ?: 0L
                val inst = Instant.ofEpochSecond(sec)
                inst.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: Exception) { "Error: ${e.message}" }
        }
        ToolScaffold("Timestamp Converter", modifier = modifier) {
            MonoTextField(input, { input = it }, "Unix Timestamp (Seconds)")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/text/src/main/java/com/devtoys/android/tools/text/CaseConverterTool.kt": """package com.devtoys.android.tools.text

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class CaseConverterTool : Tool {
    override val descriptor = ToolDescriptor("case", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.TEXT, listOf("case", "upper", "lower"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("") }
        val output = remember(input) {
            if (input.isEmpty()) "" else {
                "Upper: ${input.uppercase()}\nLower: ${input.lowercase()}"
            }
        }
        ToolScaffold("Case Converter", modifier = modifier) {
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }
}""",
    "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/HtmlEntityTool.kt": """package com.devtoys.android.tools.encoders
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class HtmlEntityTool : Tool {
    override val descriptor = ToolDescriptor("html-entity", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.ENCODERS_DECODERS, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("HTML Entity", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/Rot13Tool.kt": """package com.devtoys.android.tools.encoders
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class Rot13Tool : Tool {
    override val descriptor = ToolDescriptor("rot13", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.ENCODERS_DECODERS, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("ROT13", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/generators/src/main/java/com/devtoys/android/tools/generators/QrGeneratorTool.kt": """package com.devtoys.android.tools.generators
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class QrGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("qr-gen", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.GENERATORS, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("QR Generator", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/text/src/main/java/com/devtoys/android/tools/text/TextDiffTool.kt": """package com.devtoys.android.tools.text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class TextDiffTool : Tool {
    override val descriptor = ToolDescriptor("text-diff", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.TEXT, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("Text Diff", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/text/src/main/java/com/devtoys/android/tools/text/LineToolsTool.kt": """package com.devtoys.android.tools.text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class LineToolsTool : Tool {
    override val descriptor = ToolDescriptor("line-tools", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.TEXT, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("Line Tools", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/data/src/main/java/com/devtoys/android/tools/data/YamlJsonTool.kt": """package com.devtoys.android.tools.data
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class YamlJsonTool : Tool {
    override val descriptor = ToolDescriptor("yaml-json", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DATA_FORMATTERS, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("YAML/JSON", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/data/src/main/java/com/devtoys/android/tools/data/XmlFormatterTool.kt": """package com.devtoys.android.tools.data
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class XmlFormatterTool : Tool {
    override val descriptor = ToolDescriptor("xml-formatter", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DATA_FORMATTERS, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("XML Formatter", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/generators/src/main/java/com/devtoys/android/tools/generators/PasswordGeneratorTool.kt": """package com.devtoys.android.tools.generators
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class PasswordGeneratorTool : Tool {
    override val descriptor = ToolDescriptor("password-gen", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.GENERATORS, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("Password Gen", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/crypto/src/main/java/com/devtoys/android/tools/crypto/HmacCalculatorTool.kt": """package com.devtoys.android.tools.crypto
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class HmacCalculatorTool : Tool {
    override val descriptor = ToolDescriptor("hmac", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.CRYPTO, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("HMAC Calc", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/CronTool.kt": """package com.devtoys.android.tools.developer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class CronTool : Tool {
    override val descriptor = ToolDescriptor("cron", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("Cron Builder", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/ColorPickerTool.kt": """package com.devtoys.android.tools.developer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class ColorPickerTool : Tool {
    override val descriptor = ToolDescriptor("color-picker", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("Color Picker", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/developer/src/main/java/com/devtoys/android/tools/developer/MarkdownTool.kt": """package com.devtoys.android.tools.developer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class MarkdownTool : Tool {
    override val descriptor = ToolDescriptor("markdown", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("Markdown", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/reference/src/main/java/com/devtoys/android/tools/reference/CheatsheetTool.kt": """package com.devtoys.android.tools.reference
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class CheatsheetTool : Tool {
    override val descriptor = ToolDescriptor("cheatsheet", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.REFERENCE, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("Cheatsheets", modifier=modifier) { Text("Placeholder") } }
}""",
    "tools/reference/src/main/java/com/devtoys/android/tools/reference/HttpStatusTool.kt": """package com.devtoys.android.tools.reference
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devtoys.android.tools.base.*
import com.devtoys.android.core.ui.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
class HttpStatusTool : Tool {
    override val descriptor = ToolDescriptor("http-status", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.REFERENCE, listOf())
    @Composable override fun Content(modifier: Modifier) { ToolScaffold("HTTP Status", modifier=modifier) { Text("Placeholder") } }
}"""
}

# Add org.json dependency to tools/data, and tools/developer
data_build_file = os.path.join(project_dir, "tools/data/build.gradle.kts")
dev_build_file = os.path.join(project_dir, "tools/developer/build.gradle.kts")

for build_file in [data_build_file, dev_build_file]:
    with open(build_file, "r", encoding="utf-8") as f:
        content = f.read()
    if 'implementation("org.json:json:20231013")' not in content:
        content = content.replace("dependencies {", "dependencies {\n    implementation(\"org.json:json:20231013\")")
        with open(build_file, "w", encoding="utf-8") as f:
            f.write(content)

import glob
for file in glob.glob(os.path.join(project_dir, "tools/*/build.gradle.kts")):
    with open(file, "r", encoding="utf-8") as f:
        content = f.read()
    
    changed = False
    if 'implementation(project(":core:ui"))' not in content:
        content += '\n    implementation(project(":core:ui"))'
        changed = True
    if 'implementation(project(":tools:base"))' not in content and not file.endswith("base\\build.gradle.kts") and not file.endswith("base/build.gradle.kts"):
        content += '\n    implementation(project(":tools:base"))'
        changed = True
        
    if changed:
        # We might have appended outside the block if the file didn't end properly.
        # It's better to replace the closing brace if we want to be clean, but since we know the template ends with '}', let's be careful.
        pass

for file in glob.glob(os.path.join(project_dir, "tools/*/build.gradle.kts")):
    with open(file, "r", encoding="utf-8") as f:
        content = f.read()
    if 'implementation(project(":core:ui"))' not in content:
        content = content.replace("\n}", '\n    implementation(project(":core:ui"))\n}')
        with open(file, "w", encoding="utf-8") as f:
            f.write(content)
    with open(file, "r", encoding="utf-8") as f:
        content = f.read()
    if 'implementation(project(":tools:base"))' not in content and not ("tools\\base" in file or "tools/base" in file):
        content = content.replace("\n}", '\n    implementation(project(":tools:base"))\n}')
        with open(file, "w", encoding="utf-8") as f:
            f.write(content)

for path, content in tools.items():
    full_path = os.path.join(project_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)

# Update AppModule.kt
app_module_path = os.path.join(project_dir, "app/src/main/java/com/devtoys/android/di/AppModule.kt")
with open(app_module_path, "r", encoding="utf-8") as f:
    app_module_content = f.read()

tools_classes = [
    "com.devtoys.android.tools.encoders.UrlEncoderTool",
    "com.devtoys.android.tools.encoders.HexEncoderTool",
    "com.devtoys.android.tools.data.JsonFormatterTool",
    "com.devtoys.android.tools.developer.RegexTesterTool",
    "com.devtoys.android.tools.crypto.HashCalculatorTool",
    "com.devtoys.android.tools.developer.JwtDecoderTool",
    "com.devtoys.android.tools.developer.TimestampTool",
    "com.devtoys.android.tools.text.CaseConverterTool",
    "com.devtoys.android.tools.encoders.HtmlEntityTool",
    "com.devtoys.android.tools.encoders.Rot13Tool",
    "com.devtoys.android.tools.generators.QrGeneratorTool",
    "com.devtoys.android.tools.text.TextDiffTool",
    "com.devtoys.android.tools.text.LineToolsTool",
    "com.devtoys.android.tools.data.YamlJsonTool",
    "com.devtoys.android.tools.data.XmlFormatterTool",
    "com.devtoys.android.tools.generators.PasswordGeneratorTool",
    "com.devtoys.android.tools.crypto.HmacCalculatorTool",
    "com.devtoys.android.tools.developer.CronTool",
    "com.devtoys.android.tools.developer.ColorPickerTool",
    "com.devtoys.android.tools.developer.MarkdownTool",
    "com.devtoys.android.tools.reference.CheatsheetTool",
    "com.devtoys.android.tools.reference.HttpStatusTool"
]

import_statements = "\n".join([f"import {cls}" for cls in tools_classes])

if "UrlEncoderTool" not in app_module_content:
    # Insert imports below existing ones
    app_module_content = app_module_content.replace(
        "import javax.inject.Singleton",
        "import javax.inject.Singleton\n" + import_statements
    )
    
    # Replace list of tools
    instantiations = ",\n                ".join([f"{cls.split('.')[-1]}()" for cls in tools_classes])
    
    app_module_content = app_module_content.replace(
        "Base64Tool(),\n                UUIDTool()",
        f"Base64Tool(),\n                UUIDTool(),\n                {instantiations}"
    )

    with open(app_module_path, "w", encoding="utf-8") as f:
        f.write(app_module_content)

print("All tools generated and registered.")
