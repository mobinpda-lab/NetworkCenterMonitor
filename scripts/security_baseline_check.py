from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path('app/src/main')
manifest = root / 'AndroidManifest.xml'
text = manifest.read_text(encoding='utf-8')

if 'usesCleartextTraffic="true"' in text:
    raise SystemExit('Cleartext traffic must not be explicitly enabled')

ANDROID = '{http://schemas.android.com/apk/res/android}'
tree = ET.parse(manifest)
for activity in tree.getroot().iter('activity'):
    exported = activity.attrib.get(ANDROID + 'exported')
    name = activity.attrib.get(ANDROID + 'name', '')
    if exported == 'true' and name != '.MainActivity':
        raise SystemExit(f'Unexpected exported activity: {name}')

assignment = re.compile(r'(?i)\b(password|passwd|token|secret|api[_-]?key)\b\s*=\s*["\'][^"\']+["\']')
http_literal = re.compile(r'["\']http://(?!schemas\.android\.com)[^"\']+["\']')

violations = []
for path in root.rglob('*'):
    if not path.is_file() or path.suffix.lower() not in {'.kt', '.kts', '.java', '.xml'}:
        continue
    content = path.read_text(encoding='utf-8', errors='ignore')
    if assignment.search(content):
        violations.append(f'possible plaintext credential assignment: {path}')
    if http_literal.search(content):
        violations.append(f'cleartext HTTP literal: {path}')

if violations:
    raise SystemExit('\n'.join(violations))

print('Android security baseline OK')
