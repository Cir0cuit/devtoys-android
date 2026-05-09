import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

# 1. Fix OutputPanel.kt
output_panel_path = os.path.join(project_dir, "core/ui/src/main/java/com/devtoys/android/core/ui/OutputPanel.kt")
with open(output_panel_path, "r", encoding="utf-8") as f:
    op_content = f.read()

if "verticalScroll(rememberScrollState())" in op_content:
    op_content = op_content.replace(
        """                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)""",
        """                    .padding(horizontal = 16.dp)"""
    )
    with open(output_panel_path, "w", encoding="utf-8") as f:
        f.write(op_content)
    print("OutputPanel fixed.")

# 2. Fix MarkdownTool.kt
markdown_path = os.path.join(project_dir, "tools/developer/src/main/java/com/devtoys/android/tools/developer/MarkdownTool.kt")
with open(markdown_path, "r", encoding="utf-8") as f:
    md_content = f.read()

if "Modifier.fillMaxSize()" in md_content:
    md_content = md_content.replace(
        "modifier = Modifier.fillMaxSize(),",
        "modifier = Modifier.fillMaxWidth().wrapContentHeight(),"
    )
    with open(markdown_path, "w", encoding="utf-8") as f:
        f.write(md_content)
    print("MarkdownTool fixed.")

# 3. Fix HtmlEntityTool.kt EncoderDecoder scrolling
html_path = os.path.join(project_dir, "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/HtmlEntityTool.kt")
with open(html_path, "r", encoding="utf-8") as f:
    html_content = f.read()

old_encoder = """    @Composable fun EncoderDecoder() {
        var input by remember { mutableStateOf("") }
        var isEncodeMode by remember { mutableStateOf(true) }
        val output = remember(input, isEncodeMode) {
            if (input.isEmpty()) "" else if (isEncodeMode) {
                input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\\"", "&quot;").replace("'", "&#39;")
            } else {
                input.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\\"").replace("&#39;", "'").replace("&#x27;", "'")
            }
        }
        Text("Mode: ${if (isEncodeMode) "Encode" else "Decode"}")
        Switch(checked = isEncodeMode, onCheckedChange = { isEncodeMode = it })
        Spacer(modifier = Modifier.height(16.dp))
        MonoTextField(input, { input = it }, "Input")
        Spacer(modifier = Modifier.height(16.dp))
        OutputPanel(output)
    }"""

new_encoder = """    @Composable fun EncoderDecoder() {
        var input by remember { mutableStateOf("") }
        var isEncodeMode by remember { mutableStateOf(true) }
        val output = remember(input, isEncodeMode) {
            if (input.isEmpty()) "" else if (isEncodeMode) {
                input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\\"", "&quot;").replace("'", "&#39;")
            } else {
                input.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\\"").replace("&#39;", "'").replace("&#x27;", "'")
            }
        }
        Column(modifier = Modifier.fillMaxSize().androidx.compose.foundation.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Text("Mode: ${if (isEncodeMode) "Encode" else "Decode"}")
            Switch(checked = isEncodeMode, onCheckedChange = { isEncodeMode = it })
            Spacer(modifier = Modifier.height(16.dp))
            MonoTextField(input, { input = it }, "Input")
            Spacer(modifier = Modifier.height(16.dp))
            OutputPanel(output)
        }
    }"""

if old_encoder in html_content:
    html_content = html_content.replace(old_encoder, new_encoder)
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html_content)
    print("HtmlEntityTool EncoderDecoder fixed.")
else:
    print("Could not find EncoderDecoder block to patch.")
