package com.cir0cuit.devtoys.tools.data
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cir0cuit.devtoys.core.ui.*
import com.cir0cuit.devtoys.tools.base.*

class SqlFormatterTool : Tool {
    override val descriptor = ToolDescriptor("sql-formatter", "SQL Formatter", "Break SQL queries into readable lines", Icons.Default.Storage, ToolCategory.FORMATTERS, listOf("sql", "format"))
    @Composable override fun Content(modifier: Modifier) {
        var input by rememberSaveable { mutableStateOf("select id, name from users where active = 1 and age > 18 order by name") }
        val output = remember(input) {
            if (input.isEmpty()) "" else formatSql(input)
        }
        ToolScaffold(descriptor.title, modifier = modifier) {
            PasteableInput(input, { input = it }, "SQL input")
            Spacer(modifier = Modifier.height(8.dp))
            OutputPanel(output)
        }
    }
}

private val SQL_TOKEN = Regex(
    "'(?:[^']|'')*'|\"(?:[^\"]|\"\")*\"|`[^`]*`|\\[[^\\]]*]|--[^\\n]*|/\\*[\\s\\S]*?\\*/" +
        "|\\s*\\b(SELECT|FROM|WHERE|AND|OR|ORDER\\s+BY|GROUP\\s+BY|LEFT\\s+JOIN|INNER\\s+JOIN)\\b",
    RegexOption.IGNORE_CASE
)

internal fun formatSql(input: String): String =
    SQL_TOKEN.replace(input) { m ->
        val keyword = m.groups[1]?.value
        if (keyword == null) m.value else "\n" + keyword.replace(Regex("\\s+"), " ")
    }.trimStart()