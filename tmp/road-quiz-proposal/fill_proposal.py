from pathlib import Path
from zipfile import ZipFile
from lxml import etree as E
import hashlib,json
src=Path(r'C:/Users/jace0/Downloads/contest-application-consent.docx')
base=Path(r'C:/Users/jace0/Pleos/tmp/road-quiz-proposal')
out=Path(r'C:/Users/jace0/Pleos/outputs/로드 퀴즈쇼 기획서 초안.docx');out.parent.mkdir(exist_ok=True)
answers={
'app_summary':'로드 퀴즈쇼는 이동 경로와 차량 상황을 퀴즈 소재로 삼아, 차가 진행하고 동승자가 함께 참여하는 음성 중심의 차량용 퀴즈쇼 앱입니다.',
'planning_background':'가족이나 친구가 한 차로 여행해도 각자의 스마트폰을 보며 이동 시간을 따로 보내기 쉽습니다. 함께 즐길 놀이를 시작하려면 누군가 문제를 찾고 진행과 점수 계산을 맡아야 하며, 일반 퀴즈 앱은 지나가는 지역이나 남은 이동 시간과 연결되지 않습니다. 로드 퀴즈쇼는 차량이 진행자 역할을 맡아 이러한 준비 부담을 줄이고, 지금 함께 달리는 길 자체를 대화의 소재로 바꾸고자 합니다. 초기 타깃은 주말 나들이와 장거리 여행을 하는 가족 및 친구 동승 그룹입니다. 목적지까지 이어지는 짧은 라운드로 참여 부담을 낮추고, 운전 상황에 따라 진행을 보류하는 차량 특화 경험을 설계합니다.',
'vehicle_scenario':'첫째, 출발 전 동승자가 좌석을 선택하고 앞좌석 대 뒷좌석 등 팀을 구성합니다. 좌석 데이터는 지원 여부와 권한 확인 후 보조 수단으로 사용하며, 자동 인식이 불가능하면 수동 선택을 제공합니다. 둘째, 목적지와 남은 이동 시간을 바탕으로 라운드를 구성하고, 현재 지역의 문화·명소·상식 문제를 음성으로 제시합니다. 도착 시간 맞히기는 출발 시 예측을 저장해 도착 후 결과를 비교하는 방식으로 운영하며 빠른 도착을 보상하지 않습니다. 셋째, 내 차 퀴즈와 정차 중 휴게소 보너스 라운드를 제공하고, 목적지 도착 시 팀별 점수와 함께 맞힌 문제를 정리합니다. 넷째, 내비게이션 안내와 차량 상태를 바탕으로 주의가 필요한 구간에서는 출제·응답 타이머·음성 재생을 보류합니다. 재개 가능 여부를 확인한 뒤 문제를 다시 안내하며 중단에 따른 감점은 없습니다. 주행 중 운전자에게 화면 조작이나 제한 시간 내 정답 응답을 요구하지 않고, 동승자 중심으로 진행합니다. 운전자는 즉시 중단할 수 있습니다.',
'key_features':'가족 네 명이 주말 여행을 떠나는 상황을 가정합니다. 출발 전 정차 상태에서 동승자가 앱을 열고 좌석별 팀, 문제 난이도, 라운드 길이를 선택합니다. 내비게이션 목적지와 연계할 수 있으면 여행 세션을 구성하고, 연계가 불가능하면 일반 퀴즈 세션을 선택합니다. 출발 후 앱은 “지금 지나가는 지역으로 유명한 음식은 무엇일까요?”처럼 해당 지역과 연결된 짧은 객관식 문제를 읽습니다. 지정된 동승자 한 명이 팀의 답을 말하고, 앱이 인식 결과와 정답을 안내합니다. 여러 명이 동시에 말하거나 인식에 실패하면 재응답 또는 건너뛰기를 제공하며, 응답 음성의 방향만으로 좌석이나 화자를 판별하지 않습니다. 분기점 접근이나 내비 안내 등 주의 상황을 감지하면 퀴즈를 멈추고 안내가 끝난 뒤 진행 조건을 다시 확인합니다. 휴게소에서 주차 상태가 확인되면 화면을 함께 보는 보너스 라운드를 진행합니다. 목적지 도착 후 최종 순위와 여행 퀴즈 기록을 확인하고 세션을 종료합니다.',
'pleos_plan':'Pleos Connect에서 동작하는 Android 앱으로 퀴즈 엔진, 여행 세션·팀 점수 관리, 차량 상황 판단, 음성 입출력 모듈을 나누어 구현합니다. 첫 구현 범위는 정차 상태의 좌석 수동 선택, 검수된 문제 묶음, 음성 출제·응답, 점수 및 결과 화면입니다. 검증 전 주행 중 게임은 활성화하지 않습니다. Vehicle SDK는 주차·기어 등 제공되는 차량 상태를 읽는 용도로 검토하고, 지원 신호와 권한을 먼저 확인합니다. NaviHelper SDK는 경로 안내 및 내비게이션 정보 연동에 활용하되, 분기점 거리와 안내 상태를 원하는 형태로 받을 수 있는지는 해당 버전에서 검증합니다. 필요한 상태를 얻지 못하면 주행 중 모드를 허용하지 않고 정차 모드로 제한합니다. FusedLocation SDK는 현재 위치와 지역 문제 매칭에 활용하며, 위치 미수신·권한 거부 시 일반 문제로 전환합니다. TextToSpeech와 SpeechToText SDK는 문제 읽기와 동승자 답변 인식에 사용합니다. 음성인식 결과에 따른 재응답, 오디오 우선순위, 내비 안내 중 재생 중단을 검증합니다. 초기 문제는 출처와 정답을 검수해 앱에 탑재하고, 실시간 생성형 문제는 후속 과제로 둡니다. 좌석 자동 인식, 운전 종료 후 부드러운 운전 요약은 데이터 제공 여부와 평가 타당성 확인 후 확장하며, 운전 행동을 유도하는 점수 보상은 넣지 않습니다. 개발은 핵심 정차 시연 → 위치·내비·차량 어댑터 연동 → 에뮬레이터 예외 상황 검증 → 이용성 평가 순으로 진행합니다. 필수 검증 항목은 주행 전환, 분기점 접근, 경로 변경, 권한 거부, 위치 미수신, SDK 연결 해제, 음성인식 실패 및 중단 후 재개입니다. 위치·음성 원본은 기본 저장하지 않으며, 최소한의 세션 점수만 기기에 보관합니다.',
'reference_files':'동승자가 같은 문제를 풀며 대화를 시작하고, 이동 중 지나가는 지역에 대한 관심을 높이는 것을 기대합니다. 차량 데이터와 경로가 콘텐츠 및 진행 시점을 바꾸므로, 단순 문제 풀이를 넘어 여행마다 다른 공동 경험을 제공할 수 있습니다. 지루함 감소와 대화 증가 여부는 사용자 평가로 검증하며, 졸음운전 예방 효과를 서비스 성과로 단정하지 않습니다. 초기 검증은 동승 그룹 10팀을 대상으로 사용 흐름과 중단 동작을 평가하는 계획으로 시작합니다. 가설 목표는 시작한 팀의 라운드 완료율 70% 이상, 첫 사용 후 30일 내 재사용 팀 비율 30% 이상이며 실측 성과가 아닙니다. 완료율은 시작 라운드 중 정상 종료한 비율, 재사용률은 첫 사용 팀 중 30일 내 다른 세션을 시작한 비율로 정의합니다. MAU는 월 1회 이상 세션을 시작한 고유 이용자 기준으로 측정하되, 초기에는 기기·팀별 이용을 별도로 관리하고 배포 규모 확인 후 목표를 산정합니다. 이후 연령별 난이도, 지역별 문제 팩, 여행 기록과 연계한 복습, 다국어 콘텐츠로 확장합니다. 확장에 앞서 주행 중 자동 보류와 음성 상호작용의 적합성을 우선 검증합니다.'
}
n={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'};w='{'+n['w']+'}'
with ZipFile(src) as z:
 x=E.fromstring(z.read('word/document.xml'))
 for s in x.findall('.//w:sdt',n):
  tag=s.find('w:sdtPr/w:tag',n).get(w+'val')
  if tag in answers:
   ts=s.findall('.//w:sdtContent//w:t',n);assert len(ts)==1
   ts[0].text=answers[tag]
 for p in x.findall('.//w:p',n):
  ts=p.findall('.//w:t',n); txt=''.join(t.text or '' for t in ts)
  if txt.startswith('앱명 / 프로젝트명:'):
   filled=False
   for t in ts:
    if '_' in (t.text or ''):
     t.text=t.text[:t.text.index('_')]+('로드 퀴즈쇼' if not filled else '')
     filled=True
 for s in x.findall('.//w:sdt',n):
  if s.find('w:sdtPr/w:tag',n).get(w+'val')=='reference_files':
   for h in s.getparent().findall('.//w:highlight',n):h.getparent().remove(h)
 data=E.tostring(x,xml_declaration=True,encoding='UTF-8',standalone=True)
 with ZipFile(out,'w') as zo:
  for item in z.infolist():zo.writestr(item,data if item.filename=='word/document.xml' else z.read(item))
 with ZipFile(out) as zz, ZipFile(src) as fresh:
  changed=[i.filename for i in fresh.infolist() if fresh.read(i)!=zz.read(i.filename)]
 assert changed==['word/document.xml'],changed
 assert len(x.findall('.//w:sdt',n))==8
 (base/'filled-answers.json').write_text(json.dumps(answers,ensure_ascii=False,indent=2),encoding='utf-8')
 print('Created',out,'Changed parts',changed)
 print('Source SHA256',hashlib.sha256(src.read_bytes()).hexdigest())


