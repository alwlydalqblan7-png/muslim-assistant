"""Check source bytes and Quran structure before packaging (no network)."""
from pathlib import Path
import hashlib
import json
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
assets = root / 'app/src/main'
expected = json.loads((root / 'scripts/asset-hashes.json').read_text())
for name, digest in expected.items():
    assert hashlib.sha256((assets / name).read_bytes()).hexdigest() == digest, name
text = assets / 'assets/quran-uthmani.xml'
assert 'CHANGING IT IS NOT ALLOWED' in text.read_text()
quran = ET.parse(text).getroot()
metadata = ET.parse(assets / 'assets/quran-metadata.xml').getroot().find('suras')
assert len(quran) == len(metadata) == 114
assert sum(len(surah) for surah in quran) == 6236
for number, (surah, meta) in enumerate(zip(quran, metadata), 1):
    assert int(surah.attrib['index']) == number
    assert len(surah) == int(meta.attrib['ayas'])
    for verse, aya in enumerate(surah, 1):
        assert int(aya.attrib['index']) == verse and aya.attrib['text'].strip()
assert 'bismillah' not in quran[8][0].attrib  # At-Tawbah
assert (assets / 'res/raw/adhan_licensed.ogg').read_bytes().startswith(b'OggS')
print('PASS: verified source hashes, 114 surahs, 6236 sequential verses, per-surah counts and audio container')
