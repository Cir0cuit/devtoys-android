import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"
main_activity_path = os.path.join(project_dir, "app/src/main/java/com/devtoys/android/MainActivity.kt")

with open(main_activity_path, "r", encoding="utf-8") as f:
    content = f.read()

old_block = """            fun customDark(primary: Color, container: Color): ColorScheme = baseDark.copy(
                primary = primary, primaryContainer = container,
                secondary = primary, secondaryContainer = container,
                tertiary = primary, tertiaryContainer = container,
                outline = Color(0xFF888888), outlineVariant = Color(0xFF444444)
            )
            fun customLight(primary: Color, container: Color): ColorScheme = baseLight.copy(
                primary = primary, primaryContainer = container,
                secondary = primary, secondaryContainer = container,
                tertiary = primary, tertiaryContainer = container,
                outline = Color(0xFF777777), outlineVariant = Color(0xFFCCCCCC)
            )"""

new_block = """            fun customDark(primary: Color, container: Color): ColorScheme = baseDark.copy(
                primary = primary, onPrimary = Color(0xFF111111),
                primaryContainer = container, onPrimaryContainer = Color.White,
                secondary = primary, onSecondary = Color(0xFF111111),
                secondaryContainer = container, onSecondaryContainer = Color.White,
                tertiary = primary, onTertiary = Color(0xFF111111),
                tertiaryContainer = container, onTertiaryContainer = Color.White,
                outline = Color(0xFF888888), outlineVariant = Color(0xFF444444)
            )
            fun customLight(primary: Color, container: Color): ColorScheme = baseLight.copy(
                primary = primary, onPrimary = Color.White,
                primaryContainer = container, onPrimaryContainer = Color(0xFF111111),
                secondary = primary, onSecondary = Color.White,
                secondaryContainer = container, onSecondaryContainer = Color(0xFF111111),
                tertiary = primary, onTertiary = Color.White,
                tertiaryContainer = container, onTertiaryContainer = Color(0xFF111111),
                outline = Color(0xFF777777), outlineVariant = Color(0xFFCCCCCC)
            )"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open(main_activity_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("onPrimary colors fixed successfully.")
else:
    print("Could not find the block to replace.")
