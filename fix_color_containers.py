import os

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

main_activity_path = os.path.join(project_dir, "app/src/main/java/com/devtoys/android/MainActivity.kt")

with open(main_activity_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace the colorScheme = when (accentColor) block
old_block = """            val colorScheme = when (accentColor) {
                0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val dynamic = if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    if (amoledMode && isDarkTheme) dynamic.copy(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF111111)) else dynamic
                } else {
                    if (isDarkTheme) baseDark else baseLight
                }
                1 -> if (isDarkTheme) baseDark.copy(primary = Color(0xFF2196F3)) else baseLight.copy(primary = Color(0xFF2196F3))
                2 -> if (isDarkTheme) baseDark.copy(primary = Color(0xFF673AB7)) else baseLight.copy(primary = Color(0xFF673AB7))
                3 -> if (isDarkTheme) baseDark.copy(primary = Color(0xFF4CAF50)) else baseLight.copy(primary = Color(0xFF4CAF50))
                4 -> if (isDarkTheme) baseDark.copy(primary = Color(0xFFFF9800)) else baseLight.copy(primary = Color(0xFFFF9800))
                else -> if (isDarkTheme) baseDark else baseLight
            }"""

new_block = """            fun customDark(primary: Color, container: Color): ColorScheme = baseDark.copy(
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
            )

            val colorScheme = when (accentColor) {
                0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val dynamic = if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    if (amoledMode && isDarkTheme) dynamic.copy(background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF111111)) else dynamic
                } else {
                    if (isDarkTheme) baseDark else baseLight
                }
                1 -> if (isDarkTheme) customDark(Color(0xFF2196F3), Color(0xFF004B71)) else customLight(Color(0xFF2196F3), Color(0xFFCBE6FF))
                2 -> if (isDarkTheme) customDark(Color(0xFF673AB7), Color(0xFF4A148C)) else customLight(Color(0xFF673AB7), Color(0xFFEADDFF))
                3 -> if (isDarkTheme) customDark(Color(0xFF4CAF50), Color(0xFF005313)) else customLight(Color(0xFF4CAF50), Color(0xFFC8E6C9))
                4 -> if (isDarkTheme) customDark(Color(0xFFFF9800), Color(0xFF7A2E00)) else customLight(Color(0xFFFF9800), Color(0xFFFFDCC1))
                else -> if (isDarkTheme) baseDark else baseLight
            }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open(main_activity_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("Switch color containers patched successfully.")
else:
    print("Could not find the block to replace. Check indentation or previous modifications.")
