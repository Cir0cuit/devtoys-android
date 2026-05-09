import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

def read_file(path):
    with open(os.path.join(project_dir, path), "r", encoding="utf-8") as f:
        return f.read()

def write_file(path, content):
    with open(os.path.join(project_dir, path), "w", encoding="utf-8") as f:
        f.write(content)

# 1. MainScreen & ToolsListScreen state hoisting
main_screen_path = "app/src/main/java/com/devtoys/android/ui/MainScreen.kt"
main_screen_content = read_file(main_screen_path)

if "val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }" not in main_screen_content:
    main_screen_content = main_screen_content.replace(
        "val listState = rememberLazyListState()",
        "val listState = rememberLazyListState()\n    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }"
    )
    main_screen_content = main_screen_content.replace(
        "listState = listState,",
        "listState = listState,\n                        expandedCategories = expandedCategories,"
    )
    write_file(main_screen_path, main_screen_content)

tools_list_path = "app/src/main/java/com/devtoys/android/ui/ToolsListScreen.kt"
tools_list_content = read_file(tools_list_path)

if "expandedCategories: MutableMap<String, Boolean>" not in tools_list_content:
    tools_list_content = tools_list_content.replace(
        "listState: LazyListState = rememberLazyListState(),",
        "listState: LazyListState = rememberLazyListState(),\n    expandedCategories: MutableMap<String, Boolean>,"
    )
    tools_list_content = tools_list_content.replace(
        "val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }",
        ""
    )
    write_file(tools_list_path, tools_list_content)

# 2. Markdown Previewer Dark Mode
markdown_path = "tools/developer/src/main/java/com/devtoys/android/tools/developer/MarkdownTool.kt"
markdown_content = """package com.devtoys.android.tools.developer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import android.widget.TextView
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*
import io.noties.markwon.Markwon

class MarkdownTool : Tool {
    override val descriptor = ToolDescriptor("markdown", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.DEVELOPER, listOf("markdown", "preview"))
    @Composable override fun Content(modifier: Modifier) {
        var input by remember { mutableStateOf("# Hello World\\n\\nThis is **Markdown**.") }
        val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
        
        ToolScaffold("Markdown Previewer", modifier = modifier) {
            MonoTextField(input, { input = it }, "Markdown", minLines=6)
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context -> 
                    TextView(context).apply { setTextColor(textColor) }
                },
                update = { textView ->
                    textView.setTextColor(textColor)
                    val markwon = Markwon.create(textView.context)
                    markwon.setMarkdown(textView, input)
                }
            )
        }
    }
}"""
write_file(markdown_path, markdown_content)

# 3. UUID Generator Padding
uuid_path = "tools/generators/src/main/java/com/devtoys/android/tools/generators/UUIDTool.kt"
uuid_content = read_file(uuid_path)
if "modifier = Modifier.padding(end = 8.dp)" not in uuid_content:
    uuid_content = uuid_content.replace(
        "Text(\"Uppercase\")",
        "Text(\"Uppercase\", modifier = Modifier.padding(end = 8.dp))"
    )
    write_file(uuid_path, uuid_content)

# 4. Complete HTTP Status Codes
http_status_path = "tools/reference/src/main/java/com/devtoys/android/tools/reference/HttpStatusTool.kt"
http_status_content = """package com.devtoys.android.tools.reference
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class HttpStatusTool : Tool {
    override val descriptor = ToolDescriptor("http-status", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.REFERENCE, listOf("http", "status", "codes"))
    @Composable override fun Content(modifier: Modifier) {
        val categories = mapOf(
            "1xx Informational" to mapOf(100 to "Continue", 101 to "Switching Protocols", 102 to "Processing", 103 to "Early Hints"),
            "2xx Success" to mapOf(200 to "OK", 201 to "Created", 202 to "Accepted", 203 to "Non-Authoritative Information", 204 to "No Content", 205 to "Reset Content", 206 to "Partial Content", 207 to "Multi-Status", 208 to "Already Reported", 226 to "IM Used"),
            "3xx Redirection" to mapOf(300 to "Multiple Choices", 301 to "Moved Permanently", 302 to "Found", 303 to "See Other", 304 to "Not Modified", 305 to "Use Proxy", 307 to "Temporary Redirect", 308 to "Permanent Redirect"),
            "4xx Client Error" to mapOf(400 to "Bad Request", 401 to "Unauthorized", 402 to "Payment Required", 403 to "Forbidden", 404 to "Not Found", 405 to "Method Not Allowed", 406 to "Not Acceptable", 407 to "Proxy Authentication Required", 408 to "Request Timeout", 409 to "Conflict", 410 to "Gone", 411 to "Length Required", 412 to "Precondition Failed", 413 to "Payload Too Large", 414 to "URI Too Long", 415 to "Unsupported Media Type", 416 to "Range Not Satisfiable", 417 to "Expectation Failed", 418 to "I'm a teapot", 421 to "Misdirected Request", 422 to "Unprocessable Entity", 423 to "Locked", 424 to "Failed Dependency", 425 to "Too Early", 426 to "Upgrade Required", 428 to "Precondition Required", 429 to "Too Many Requests", 431 to "Request Header Fields Too Large", 451 to "Unavailable For Legal Reasons"),
            "5xx Server Error" to mapOf(500 to "Internal Server Error", 501 to "Not Implemented", 502 to "Bad Gateway", 503 to "Service Unavailable", 504 to "Gateway Timeout", 505 to "HTTP Version Not Supported", 506 to "Variant Also Negotiates", 507 to "Insufficient Storage", 508 to "Loop Detected", 510 to "Not Extended", 511 to "Network Authentication Required")
        )
        
        val expandedState = remember { mutableStateMapOf<String, Boolean>() }
        
        ToolScaffold("HTTP Status Codes", modifier = modifier) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                categories.forEach { (catName, statuses) ->
                    val isExpanded = expandedState[catName] ?: true
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { expandedState[catName] = !isExpanded },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(catName, fontWeight = FontWeight.Bold)
                                Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        }
                    }
                    if (isExpanded) {
                        items(statuses.entries.toList()) { entry ->
                            ListItem(
                                headlineContent = { Text(entry.value) },
                                leadingContent = { Text(entry.key.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                            )
                        }
                    }
                }
            }
        }
    }
}"""
write_file(http_status_path, http_status_content)

# 5. Expand Cheatsheets and Add Copy Functionality
cheatsheet_path = "tools/reference/src/main/java/com/devtoys/android/tools/reference/CheatsheetTool.kt"
cheatsheet_content = """package com.devtoys.android.tools.reference
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.devtoys.android.core.ui.*
import com.devtoys.android.tools.base.*

class CheatsheetTool : Tool {
    override val descriptor = ToolDescriptor("cheatsheet", android.R.string.untitled, android.R.string.untitled, Icons.Default.Build, ToolCategory.REFERENCE, listOf("cheat", "sheet", "commands"))
    @Composable override fun Content(modifier: Modifier) {
        var selected by remember { mutableStateOf("Git") }
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        
        val cheatsheets = mapOf(
            "Git" to listOf(
                "Clone a repository" to "git clone <url>",
                "Add all changes" to "git add .",
                "Commit changes" to "git commit -m \\"Message\\"",
                "Push to remote" to "git push origin main",
                "Pull from remote" to "git pull",
                "Check status" to "git status",
                "View history" to "git log",
                "Create branch" to "git checkout -b <branch>"
            ),
            "Vim" to listOf(
                "Enter Insert Mode" to "i",
                "Enter Normal Mode" to "Esc",
                "Save file" to ":w",
                "Quit Vim" to ":q",
                "Save and Quit" to ":wq",
                "Force Quit" to ":q!",
                "Delete line" to "dd",
                "Undo" to "u"
            ),
            "Docker" to listOf(
                "Run image" to "docker run <image>",
                "List running containers" to "docker ps",
                "List all containers" to "docker ps -a",
                "Stop container" to "docker stop <id>",
                "Remove container" to "docker rm <id>",
                "List images" to "docker images",
                "Remove image" to "docker rmi <id>",
                "Build image" to "docker build -t <name> ."
            ),
            "Linux" to listOf(
                "List files" to "ls -la",
                "Change directory" to "cd <path>",
                "Show current path" to "pwd",
                "Create directory" to "mkdir <dir>",
                "Remove directory" to "rm -r <dir>",
                "Copy file" to "cp <src> <dest>",
                "Move file" to "mv <src> <dest>",
                "Find file" to "find . -name <file>"
            ),
            "Regex" to listOf(
                "Any character" to ".",
                "Zero or more" to "*",
                "One or more" to "+",
                "Zero or one" to "?",
                "Digit" to "\\\\d",
                "Word character" to "\\\\w",
                "Whitespace" to "\\\\s",
                "Start of string" to "^",
                "End of string" to "$"
            )
        )
        ToolScaffold("Cheatsheets", modifier = modifier) {
            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(cheatsheets.keys.size) { idx ->
                    val key = cheatsheets.keys.toList()[idx]
                    FilterChip(
                        selected = selected == key,
                        onClick = { selected = key },
                        label = { Text(key) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(cheatsheets[selected] ?: emptyList()) { cmd ->
                    ListItem(
                        headlineContent = { Text(cmd.second, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary) },
                        supportingContent = { Text(cmd.first) },
                        trailingContent = {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(cmd.second))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Copy")
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}"""
write_file(cheatsheet_path, cheatsheet_content)

print("UX upgrades (Markdown theme, UUID layout, HTTP categories, Cheatsheets, Category State) applied successfully.")
