from zipfile import ZipFile
from lxml import etree as E
from pathlib import Path
import hashlib,json
base=Path(r'C:/Users/jace0/Pleos/tmp/road-quiz-proposal');src=Path(r'C:/Users/jace0/Downloads/contest-application-consent.docx')
n={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
with ZipFile(src) as z:
 x=E.fromstring(z.read('word/document.xml'))
 inventory={i.filename:{'size':i.file_size,'sha256':hashlib.sha256(z.read(i)).hexdigest()} for i in z.infolist()}
 (base/'package-baseline.json').write_text(json.dumps(inventory,indent=2),encoding='utf-8')
 styles=E.fromstring(z.read('word/styles.xml'))
 (base/'style-evidence.xml').write_bytes(z.read('word/styles.xml'))
 content='''# 로드 퀴즈쇼 양식 작성 계약
Reference: C:/Users/jace0/Downloads/contest-application-consent.docx
SHA256: {sha}
Reference pages: 5. One section. Reference render: reference-render/page-1.png through page-5.png. Renderer: packaged render_docx.py unavailable because bundled LibreOffice is missing on Windows; Word read-only PDF export and bundled Poppler used instead.
Page geometry: A4 11906 x 16838 twips; margins top935 right1020 bottom907 left1020; header/footer720; one column, grid line360.
Page patterns: p1 participant information and member table; p2-3 project introduction with eight bordered controls; p4 consent; p5 signature table. Existing section headings use dark blue bold; preserve reference appearance. Original footer page fields preserved. Source exact styles and table geometry retained in word/styles.xml and document.xml, package inventory package-baseline.json.
Edit scope: word/document.xml only. Existing plain-text controls in order map app_summary=1 overview; planning_background=2 background; vehicle_scenario=3 features; key_features=4 scenario; pleos_plan=5 development; reference_files=6 expected effects; reference_link=7 optional materials; shortform_link=8 optional video. Misleading tags are mapped by visible question order. Controls are inline in bordered paragraphs with exact280 line spacing and after0. Replace existing space text with wrapped Korean prose, preserving all run and paragraph properties. Optional links stay blank. Paragraphs with 앱명 / 프로젝트명 get 로드 퀴즈쇼. Participant, consent, signature entries stay blank.
No cloning, font reduction, added headers, or section changes. Preserve all package parts except document.xml byte for byte, all sdtPr and paragraph/run properties, tables, borders, footer relationships and page fields. Natural expansion of filled answers is expected. All new pages require visual review; intact source pages may shift page numbers only. Source SHA must remain unchanged.
'''.format(sha=hashlib.sha256(src.read_bytes()).hexdigest())
 (base/'artifact.md').write_text(content,encoding='utf-8')
 print('Recorded template evidence and package baseline')
