import os
import shutil

project_dir = "C:/Users/barti/.gemini/antigravity/scratch/devtoys-android"
old_package = "com.devtoys.android"
new_package = "com.cir0cuit.devtoys"

old_path_part = old_package.replace(".", "/")
new_path_part = new_package.replace(".", "/")

modules = [
    "app", "core/common", "core/designsystem", "core/ui",
    "tools/base", "tools/crypto", "tools/data", "tools/developer",
    "tools/encoders", "tools/generators", "tools/reference", "tools/text"
]

def update_file_content(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content = content.replace(old_package, new_package)

    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated: {file_path}")

# 1. Update build.gradle.kts and AndroidManifest.xml files
for root, dirs, files in os.walk(project_dir):
    if "build" in root or ".git" in root or ".gradle" in root:
        continue
    for file in files:
        if file == "build.gradle.kts" or file == "AndroidManifest.xml" or file.endswith(".kt"):
            update_file_content(os.path.join(root, file))

# 2. Move source files
for module in modules:
    src_dir = os.path.join(project_dir, module, "src/main/java")
    if not os.path.exists(src_dir):
        continue

    old_dir = os.path.join(src_dir, old_path_part)
    new_dir = os.path.join(src_dir, new_path_part)

    if os.path.exists(old_dir):
        os.makedirs(os.path.dirname(new_dir), exist_ok=True)
        # If new_dir already exists (e.g. from a previous partial run), we merge or overwrite
        if os.path.exists(new_dir):
            print(f"Target directory {new_dir} already exists. Merging...")
            for root, dirs, files in os.walk(old_dir):
                rel_path = os.path.relpath(root, old_dir)
                target_root = os.path.join(new_dir, rel_path)
                os.makedirs(target_root, exist_ok=True)
                for file in files:
                    shutil.move(os.path.join(root, file), os.path.join(target_root, file))
            shutil.rmtree(old_dir)
        else:
            shutil.move(old_dir, new_dir)
        print(f"Moved: {old_dir} -> {new_dir}")

    # Clean up empty parent directories like com/devtoys
    parent = os.path.join(src_dir, "com/devtoys")
    if os.path.exists(parent) and not os.listdir(parent):
        os.rmdir(parent)
    grandparent = os.path.join(src_dir, "com")
    # Don't delete 'com' if it contains 'cir0cuit'
    if os.path.exists(grandparent) and not os.listdir(grandparent):
        # This shouldn't happen if we moved to com/cir0cuit
        pass

print("Package rename complete.")
