import urllib.request, json
import os

# 1. Fetch HTML Entities
with urllib.request.urlopen('https://html.spec.whatwg.org/entities.json') as r:
    html_ents = json.loads(r.read().decode('utf-8'))

html_list = []
for k, v in html_ents.items():
    if k.endswith(';'):
        char = v['characters']
        html_list.append((char, k))

# Group HTML entities roughly (all under HTML5 Entities for now, or group by starting char?)
# To avoid a single massive list, let's group HTML entities by their first letter
html_groups = {}
for char, ent in html_list:
    group = "HTML Entities: " + ent[1].upper() if len(ent) > 1 and ent[1].isalpha() else "HTML Entities: Other"
    if group not in html_groups: html_groups[group] = []
    html_groups[group].append((char, ent))


# 2. Fetch Emojis
with urllib.request.urlopen('https://raw.githubusercontent.com/github/gemoji/master/db/emoji.json') as r:
    emojis = json.loads(r.read().decode('utf-8'))

emoji_cats = {}
for e in emojis:
    cat = 'Emoji: ' + e.get('category', 'Other')
    char = e.get('emoji', '')
    if char:
        if cat not in emoji_cats: emoji_cats[cat] = []
        # Compute the numeric HTML entity. E.g. 😀 is &#128512;
        # For multiple codepoints, join them. e.g. &#x1F468;&#x200D;...
        codepoints = [ord(c) for c in char]
        ent = ''.join(f'&#{cp};' for cp in codepoints) 
        emoji_cats[cat].append((char, ent))

os.makedirs('tools/encoders/src/main/java/com/devtoys/android/tools/encoders', exist_ok=True)
with open('tools/encoders/src/main/java/com/devtoys/android/tools/encoders/HtmlEntityData.kt', 'w', encoding='utf-8') as f:
    f.write('package com.devtoys.android.tools.encoders\n\n')
    f.write('object HtmlEntityData {\n')
    f.write('    val categoriesMap = mapOf(\n')
    
    all_groups = list(html_groups.items()) + list(emoji_cats.items())
    
    cat_blocks = []
    for cat, items in all_groups:
        cat_str = f'        "{cat}" to listOf(\n'
        item_lines = []
        for char, ent in items:
            esc_char = char.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r').replace('$', '\\$')
            item_lines.append(f'            "{esc_char}" to "{ent}"')
        cat_str += ',\n'.join(item_lines) + '\n        )'
        cat_blocks.append(cat_str)
        
    f.write(',\n'.join(cat_blocks))
    f.write('\n    )\n}\n')

print("Generated HtmlEntityData.kt with", sum(len(x) for x in html_groups.values()), "HTML entities and", sum(len(x) for x in emoji_cats.values()), "emojis.")
