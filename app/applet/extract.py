import re

with open('/app/applet/bundle.js', 'r', encoding='utf-8') as f:
    text = f.read()

# Routes
routes = re.findall(r'path:\s*`([^`]+)`', text)
print('Routes:', routes)

# Puzzles and Chapters
for key in ['zA', 'RA', 'BA', 'Lk', 'Rk', 'zk', 'Bk', 'Dk', 'Ek']:
    pos = text.find(f'{key}=')
    if pos != -1:
        print(f'=== {key} ===')
        print(text[pos:pos+400])
        print()
