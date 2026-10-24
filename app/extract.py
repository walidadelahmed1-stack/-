import re

with open('bundle.js', 'r', encoding='utf-8') as f:
    text = f.read()

routes = re.findall(r'path:`([^`]+)`', text)
print('Routes found:', routes)

# Let's extract riddles list RA and sequence zA and tiles BA
for name in ['RA', 'zA', 'BA']:
    pos = text.find(f'var {name}=')
    if pos == -1:
        pos = text.find(f'{name}=[')
    if pos != -1:
        print(f'=== {name} ===')
        print(text[pos:pos+1500])

# Let's find chapters definition
pos = text.find('function Dk(')
if pos == -1:
    pos = text.find('Dk=')
if pos != -1:
    print('=== Chapters Dk ===')
    print(text[pos:pos+1000])

# Match-3 board logic
pos = text.find('function Ek(')
if pos == -1:
    pos = text.find('Ek=')
if pos != -1:
    print('=== Level Ek ===')
    print(text[pos:pos+1000])
