import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

# 1. Update ToolScaffold.kt
scaffold_path = os.path.join(project_dir, "core/ui/src/main/java/com/devtoys/android/core/ui/ToolScaffold.kt")
with open(scaffold_path, "r", encoding="utf-8") as f:
    scaffold_content = f.read()

if "scrollable: Boolean = true," not in scaffold_content:
    scaffold_content = scaffold_content.replace(
        "import androidx.compose.foundation.layout.padding",
        "import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll"
    )
    scaffold_content = scaffold_content.replace(
        "modifier: Modifier = Modifier,",
        "modifier: Modifier = Modifier,\n    scrollable: Boolean = true,"
    )
    scaffold_content = scaffold_content.replace(
        """    } { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            content = content
        )
    }""",
        """    } { padding ->
        val colModifier = Modifier
            .padding(padding)
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            
        Column(
            modifier = if (scrollable) colModifier.verticalScroll(rememberScrollState()) else colModifier,
            content = content
        )
    }"""
    )
    with open(scaffold_path, "w", encoding="utf-8") as f:
        f.write(scaffold_content)
    print("ToolScaffold updated.")

# 2. Update Tools with LazyColumn
tools_to_update = [
    "tools/reference/src/main/java/com/devtoys/android/tools/reference/CheatsheetTool.kt",
    "tools/reference/src/main/java/com/devtoys/android/tools/reference/HttpStatusTool.kt",
    "tools/encoders/src/main/java/com/devtoys/android/tools/encoders/HtmlEntityTool.kt",
    "tools/data/src/main/java/com/devtoys/android/tools/data/CsvViewerTool.kt"
]

for tool_path in tools_to_update:
    full_path = os.path.join(project_dir, tool_path)
    with open(full_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    if "scrollable = false" not in content:
        content = content.replace(
            "modifier = modifier)",
            "modifier = modifier, scrollable = false)"
        ).replace(
            "modifier = modifier) {",
            "modifier = modifier, scrollable = false) {"
        )
        with open(full_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"{tool_path} updated.")

print("Scrolling patch applied successfully.")
