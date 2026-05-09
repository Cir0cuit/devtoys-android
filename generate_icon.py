import os
from PIL import Image
import shutil

img_path = r'C:\Users\barti\.gemini\antigravity\brain\213cf9b4-72ca-4563-acf7-5aa9671c2089\media__1777659160921.png'
base_res_dir = r'C:\Users\barti\.gemini\antigravity\scratch\devtoys-android\app\src\main\res'

sizes = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

adaptive_sizes = {
    'mipmap-mdpi': 108,
    'mipmap-hdpi': 162,
    'mipmap-xhdpi': 216,
    'mipmap-xxhdpi': 324,
    'mipmap-xxxhdpi': 432,
}

if not os.path.exists(img_path):
    print('Image not found!')
else:
    with Image.open(img_path) as img:
        for dirname, size in sizes.items():
            dir_path = os.path.join(base_res_dir, dirname)
            os.makedirs(dir_path, exist_ok=True)
            
            resized = img.resize((size, size), Image.Resampling.LANCZOS)
            resized.save(os.path.join(dir_path, 'ic_launcher.png'))
            resized.save(os.path.join(dir_path, 'ic_launcher_round.png'))
            
        for dirname, size in adaptive_sizes.items():
            dir_path = os.path.join(base_res_dir, dirname)
            resized = img.resize((size, size), Image.Resampling.LANCZOS)
            resized.save(os.path.join(dir_path, 'ic_launcher_foreground.png'))
            
    anydpi_dir = os.path.join(base_res_dir, 'mipmap-anydpi-v26')
    os.makedirs(anydpi_dir, exist_ok=True)
    xml_content = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>"""
    with open(os.path.join(anydpi_dir, 'ic_launcher.xml'), 'w') as f:
        f.write(xml_content)
    with open(os.path.join(anydpi_dir, 'ic_launcher_round.xml'), 'w') as f:
        f.write(xml_content)
        
    val_dir = os.path.join(base_res_dir, 'values')
    os.makedirs(val_dir, exist_ok=True)
    colors_xml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#FFFFFF</color>
</resources>"""
    colors_file = os.path.join(val_dir, 'ic_launcher_background.xml')
    if not os.path.exists(colors_file):
        with open(colors_file, 'w') as f:
            f.write(colors_xml)

    print('Icons generated successfully!')
