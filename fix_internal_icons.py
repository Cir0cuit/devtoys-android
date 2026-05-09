import os

project_dir = 'C:/Users/barti/.gemini/antigravity/scratch/devtoys-android'

def fix_file(filename, replacement_map, imports):
    path = None
    for r, d, fs in os.walk(project_dir):
        if filename in fs:
            path = os.path.join(r, filename)
            break
            
    if not path:
        print(f'File {filename} not found.')
        return
        
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    for old, new in replacement_map.items():
        content = content.replace(old, new)
        
    for imp in imports:
        imp_str = f'import androidx.compose.material.icons.filled.{imp}'
        if imp_str not in content:
            content = content.replace('import androidx.compose.material.icons.Icons', f'import androidx.compose.material.icons.Icons\n{imp_str}')
            
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Fixed {filename}')

fix_file('CheatsheetTool.kt', {
    'Icon(if (isExpanded) Icons.Default.MenuBook else Icons.Default.MenuBook, contentDescription = null)': 'Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)',
    'Icon(Icons.Default.MenuBook, contentDescription = "Copy")': 'Icon(Icons.Default.Share, contentDescription = "Copy")'
}, ['KeyboardArrowDown', 'KeyboardArrowUp', 'Share'])

fix_file('HttpStatusTool.kt', {
    'Icon(if (isExpanded) Icons.Default.Http else Icons.Default.Http, contentDescription = null)': 'Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)'
}, ['KeyboardArrowDown', 'KeyboardArrowUp'])

fix_file('HtmlEntityTool.kt', {
    'Icon(if (isExpanded) Icons.Default.Html else Icons.Default.Html, contentDescription = null)': 'Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)'
}, ['KeyboardArrowDown', 'KeyboardArrowUp'])
