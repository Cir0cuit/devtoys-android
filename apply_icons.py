import os
import re

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

# 1. Add material-icons-extended dependency
for root, dirs, files in os.walk(project_dir):
    if "build.gradle.kts" in files:
        gradle_path = os.path.join(root, "build.gradle.kts")
        with open(gradle_path, "r", encoding="utf-8") as f:
            content = f.read()
        
        if "androidx.compose.material3:material3" in content and "material-icons-extended" not in content:
            content = content.replace(
                "implementation(libs.androidx.material3)",
                "implementation(libs.androidx.material3)\n    implementation(\"androidx.compose.material:material-icons-extended:1.6.0\")"
            )
            with open(gradle_path, "w", encoding="utf-8") as f:
                f.write(content)
            print(f"Added extended icons to {gradle_path}")

# 2. Map icons to tools
icon_mapping = {
    "Base64EncoderTool.kt": "Icons.Default.TextFormat",
    "UUIDTool.kt": "Icons.Default.Fingerprint",
    "UrlEncoderTool.kt": "Icons.Default.Link",
    "HexEncoderTool.kt": "Icons.Default.Numbers",
    "JsonFormatterTool.kt": "Icons.Default.DataObject",
    "RegexTesterTool.kt": "Icons.Default.FindInPage",
    "HashCalculatorTool.kt": "Icons.Default.Lock",
    "JwtDecoderTool.kt": "Icons.Default.VpnKey",
    "TimestampConverterTool.kt": "Icons.Default.Schedule",
    "CaseConverterTool.kt": "Icons.Default.FormatSize",
    "HtmlEntityTool.kt": "Icons.Default.Code",
    "Rot13Tool.kt": "Icons.Default.Sync",
    "QrCodeTool.kt": "Icons.Default.QrCode",
    "TextDiffTool.kt": "Icons.Default.Difference",
    "LineToolsTool.kt": "Icons.Default.FormatAlignJustify",
    "YamlJsonTool.kt": "Icons.Default.SyncAlt",
    "XmlFormatterTool.kt": "Icons.Default.Code",
    "PasswordGeneratorTool.kt": "Icons.Default.Password",
    "HmacCalculatorTool.kt": "Icons.Default.EnhancedEncryption",
    "CronBuilderTool.kt": "Icons.Default.Event",
    "ColorPickerTool.kt": "Icons.Default.Palette",
    "MarkdownTool.kt": "Icons.Default.EditNote",
    "CheatsheetTool.kt": "Icons.Default.MenuBook",
    "HttpStatusTool.kt": "Icons.Default.Http",
    "SqlFormatterTool.kt": "Icons.Default.Storage",
    "CsvViewerTool.kt": "Icons.Default.GridOn",
    "JsonTypesTool.kt": "Icons.Default.AccountTree",
    "NumberBaseConverterTool.kt": "Icons.Default.Filter1",
    "LoremIpsumTool.kt": "Icons.Default.TextFields"
}

# Recursively find tool files and update
for root, dirs, files in os.walk(os.path.join(project_dir, "tools")):
    for file in files:
        if file in icon_mapping:
            tool_path = os.path.join(root, file)
            with open(tool_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            new_icon = icon_mapping[file]
            
            # Replace Icons.Default.Build
            content = content.replace("Icons.Default.Build", new_icon)
            
            # Ensure correct import is present if it's an extended icon
            icon_name = new_icon.split(".")[-1]
            import_statement = f"import androidx.compose.material.icons.filled.{icon_name}"
            
            if import_statement not in content:
                content = content.replace(
                    "import androidx.compose.material.icons.filled.Build",
                    f"import androidx.compose.material.icons.filled.Build\n{import_statement}"
                )
            
            with open(tool_path, "w", encoding="utf-8") as f:
                f.write(content)
            print(f"Updated icon for {file}")

print("Icons mapped successfully.")
