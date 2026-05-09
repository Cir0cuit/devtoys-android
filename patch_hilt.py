import os
import glob

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"

# Update root build.gradle.kts
root_gradle_path = os.path.join(project_dir, "build.gradle.kts")
with open(root_gradle_path, "r", encoding="utf-8") as f:
    root_content = f.read()

if "com.google.devtools.ksp" not in root_content:
    root_content = root_content.replace(
        'id("com.google.dagger.hilt.android") version "2.51.1" apply false',
        'id("com.google.dagger.hilt.android") version "2.51.1" apply false\n    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false'
    )
    with open(root_gradle_path, "w", encoding="utf-8") as f:
        f.write(root_content)

# Update submodules
for file_path in glob.glob(os.path.join(project_dir, "**/build.gradle.kts"), recursive=True):
    if file_path == root_gradle_path:
        continue
        
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
        
    if "com.google.dagger.hilt.android" in content and "com.google.devtools.ksp" not in content:
        content = content.replace(
            'id("com.google.dagger.hilt.android")',
            'id("com.google.dagger.hilt.android")\n    id("com.google.devtools.ksp")'
        )
        content = content.replace(
            'implementation("com.google.dagger:hilt-android:2.51.1")',
            'implementation("com.google.dagger:hilt-android:2.51.1")\n    ksp("com.google.dagger:hilt-compiler:2.51.1")'
        )
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)

print("Patch applied")
