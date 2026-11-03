from zipfile import ZipFile
from lxml import etree as E
p=r'C:/Users/jace0/Downloads/contest-application-consent.docx'
n={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
with ZipFile(p) as z:
 x=E.fromstring(z.read('word/document.xml'))
 for i,s in enumerate(x.findall('.//w:sdt',n)):
  print('CONTROL',i,E.tostring(s.find('w:sdtPr',n),encoding='unicode'), 'TEXT',s.xpath('string(w:sdtContent)',namespaces=n)[:200])
 for i,p in enumerate(x.findall('.//w:body/w:p',n)):
  print('P',i, ''.join(p.xpath('.//w:t/text()',namespaces=n))[:150])
 print('SECTIONS', [E.tostring(s,encoding='unicode') for s in x.findall('.//w:sectPr',n)])
