from zipfile import ZipFile
from lxml import etree as E
import fitz, hashlib, json
from pathlib import Path
base=Path(r'C:/Users/jace0/Pleos/tmp/road-quiz-proposal')
z=ZipFile(r'C:/Users/jace0/Downloads/contest-application-consent.docx'); x=E.fromstring(z.read('word/document.xml')); n={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
for s in x.findall('.//w:sdt',n):
 print(s.find('w:sdtPr/w:tag',n).get('{'+n['w']+'}val'),'parent',s.getparent().tag.split('}')[-1])
for i,p in enumerate(x.findall('w:body/w:p',n)):
 if i in [10,28,29,30,31,32,33,34,35,36,66]:
  print(i, E.tostring(p,encoding='unicode').split('>')[-12:])
d=fitz.open(base/'reference.pdf'); (base/'reference-render').mkdir(exist_ok=True)
for i,p in enumerate(d): p.get_pixmap(matrix=fitz.Matrix(1.2,1.2)).save(str(base/'reference-render'/f'page-{i+1}.png'))
from PIL import Image,ImageOps,ImageDraw
im=Image.new('RGB',(1000,750),'#aaaaaa')
for i in range(len(d)):
 p=Image.open(base/'reference-render'/f'page-{i+1}.png');p.thumbnail((330,370)); im.paste(p,((i%3)*333,(i//3)*375))
im.save(base/'reference-contact.png')
