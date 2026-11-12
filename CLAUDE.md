# 길쪽지 프로젝트 인수인계 (2026-11-12 기준)

> Claude Code가 이 저장소에서 이어서 작업하기 위한 요약. 저장소 루트에 `CLAUDE.md`로 두면 자동으로 읽힌다.
> **블라인드 심사** 때문에 이 파일을 포함해 저장소 어디에도 팀원 실명, 학교, 연락처를 적지 않는다.

## 1. 프로젝트

- **공모전**: 현대자동차그룹 차량용 앱 개발 공모전 (Pleos Connect, AAOS)
- **일정**: 접수 마감 **2026-11-15 23:59** (이메일은 저녁 9시 전에 발송), 1차 결과 11/23, 최종 PT 12/3~4
- **팀**: 밤바스콜, 2명. 둘 다 프로그래밍 입문자
  - 백엔드 담당(이 저장소의 사용자): 서버 + 가족용 폰 웹 + 제출 준비. 브랜치 `back`
  - 차 앱 담당(팀 대표): `pleosSample/` Kotlin 앱. 브랜치 `car`
- **앱**: **길쪽지**. 가족이 폰으로 지도 위 장소에 음성 쪽지를 남기면, 차가 그 장소 근처의 안전한 순간에 자동으로 들려준다. 들으면 보낸 사람 폰에 "들었어요"가 뜬다
- **심사 기준**: 차량특화성, 창의성, 기술 활용성, 사업성. 가산점: 시연 영상 + GitHub(소스, `apk/팀명_앱이름_release.apk`, README)

## 2. 사용자와 일하는 방법

- 한국어, 친근한 존댓말로 답한다. 사용자는 초보라서 **한 번에 한 단계씩**, 버튼 이름과 입력값을 정확히 알려 준다
- 코드는 가능하면 직접 파일에 써 주고, 사용자가 할 일(재시작, 테스트 명령, 커밋)만 안내한다
- 개념 질문("이게 뭐야?")이 많다. 비유 + 우리 코드의 실제 예로 짧게 설명한다
- 사용자는 **GitHub Desktop**으로 커밋한다 (터미널 git 아님). IntelliJ로 서버를 실행한다
- IntelliJ에서 열려 있는 파일을 바깥에서 고치면 IntelliJ가 옛 내용으로 덮어쓴 적이 있다. 고칠 파일은 탭을 닫게 하거나, 고친 뒤 디스크 내용이 유지됐는지 확인한다

## 3. 저장소 구조

```
Pleos/                      (GitHub: 비공개, 제출 전 실명 없는 계정/조직으로 옮겨야 함)
├─ server/                  Spring Boot 서버 + 폰 웹 (백엔드 담당)
│  ├─ src/main/java/com/gilnote/server/
│  │  ├─ domain/            Car, Member, PairCode, Note, NoteStatus, TriggerType
│  │  ├─ repository/        CarRepository, PairCodeRepository, MemberRepository, NoteRepository
│  │  └─ api/               CarController(1,2) JoinController(3) NoteController(4,5) CarNoteController(6,7,8)
│  ├─ src/main/resources/
│  │  ├─ application.properties
│  │  └─ static/            join.html, place.html, record.html, sent.html, kakao-key.js(무시됨), kakao-key.example.js
│  └─ data/                 H2 DB 파일 + 녹음 파일 (gitignore)
├─ pleosSample/             차 앱 (Kotlin, View 방식, 패키지 이름에 실명이 들어 있음 → 제출 전 변경 필요)
├─ docs/api.md              API 약속표 (8개 API의 요청/응답)
├─ apk/                     제출용 릴리스 APK만
└─ .gitignore
```

## 4. 기술 스택과 실행

- **서버**: Java 17, Spring Boot 4.1.2, Spring Data JPA, H2(파일 모드), Gradle
  - 의존성 주의: `jakarta.xml.bind:jakarta.xml.bind-api` 필요 (없으면 JAXBException)
  - 실행: IntelliJ에서 `ServerApplication` ▶ → `http://localhost:8080/hello`
  - H2 콘솔: `/h2-console`, JDBC URL `jdbc:h2:file:./data/gilnote`, user `sa`, 비밀번호 없음
  - 데이터 초기화: 서버 끄고 `server/data/` 삭제
- **폰 웹**: 순수 HTML/JS (프레임워크 없음), 서버의 `static/`에서 제공. API는 상대 경로(`/api/...`)로 호출
- **차 앱**: Kotlin, AppCompat/Material(View), OkHttp, ZXing(QR). 에뮬레이터에서 서버 주소는 `http://10.0.2.2:8080`

## 5. 구조와 API

```
[가족 폰 웹] ──API 3,4,5──▶ [서버] ◀──API 1,2,6,7,8── [차 앱]
```

| # | API | 호출자 | 인증 헤더 | 상태 |
|---|---|---|---|---|
| 1 | `POST /api/cars/register` 차 등록 → carToken | 차 | 없음 | ✅ |
| 2 | `POST /api/cars/pair-code` 6자리 코드(3분, 1회용) | 차 | `X-Car-Token` | ✅ |
| 3 | `POST /api/join` 코드+이름 → memberToken | 폰 | 없음 | ✅ |
| 4 | `POST /api/notes` 쪽지 올리기 (multipart: audio, triggerType, lat, lng, radiusM, placeLabel, seat) | 폰 | `X-Member-Token` | ✅ |
| 5 | `GET /api/notes` 내가 보낸 쪽지 | 폰 | `X-Member-Token` | ✅ |
| 6 | `GET /api/cars/notes` 안 들은 쪽지 목록 (PENDING→DELIVERED) | 차 | `X-Car-Token` | ✅ |
| 7 | `GET /api/cars/notes/{id}/audio` 음성 파일 | 차 | `X-Car-Token` | ✅ |
| 8 | `POST /api/cars/notes/{id}/played` 들었어요 (→PLAYED) | 차 | `X-Car-Token` | ✅ |

- 쪽지 상태: `PENDING`(전달 대기) → `DELIVERED`(차에 도착) → `PLAYED`(들었어요)
- 에러 응답 모양: `{ "error": "CODE", "message": "한국어 설명" }` (401 토큰, 404 없음/남의 쪽지, 410 코드 만료)
- QR 내용: `{서버주소}/join.html?code={6자리}`. 차 토큰은 QR에 넣지 않는다
- 차는 쪽지를 미리 받아 두고 **위치 판단은 차가 스스로** 한다. 서버는 차의 실시간 위치를 저장하지 않는다
- 음성 파일은 `server/data/audio/`에 저장, DB에는 파일 이름과 content-type만
- Service 계층은 일부러 생략했다 (컨트롤러 → 리포지토리). 토큰 확인 코드가 더 반복되면 그때 분리

### 폰 웹 흐름
`join.html?code=` → `place.html`(카카오맵, 장소·반경 선택) → `record.html?lat&lng&radius&label`(최대 30초 녹음, 미리 듣기) → `sent.html`(10초마다 새로고침)
- 토큰은 `localStorage`의 `memberToken`, `memberName`
- 폰 마이크는 **https에서만** 동작 (localhost는 예외). 진짜 폰 테스트에는 ngrok 필요
- 요청에 `ngrok-skip-browser-warning` 헤더를 이미 넣어 둠

## 6. 확정된 결정

| 항목 | 결정 |
|---|---|
| 1차 제출 범위 | QR 연결, 폰 녹음·업로드, 장소 쪽지 재생, 들었어요 |
| 차 앱 실행 방식 | 1차까지는 **화면을 계속 띄워 둔다** (내비와 화면 분할). 포그라운드 서비스는 최종 PT 목표 |
| 위치 | **NaviHelper `onCurrentLocationInfo`** (FusedLocation SDK는 공모전에서 "준비 중"이라 사용 불가) |
| 안내 멘트 | **안드로이드 기본 TTS** (Pleos TTS/STT SDK도 "준비 중"). 에뮬레이터에 한국어 음성이 없으면 미리 녹음한 mp3로 대체 |
| 재생 보류 | 다음 회전까지 150m 이내(NaviHelper `onTBTInfo`/`getTBTInfo`) 또는 방향지시등 켜짐(Vehicle `getTurnSignal`) |
| 주행 상태 | AAOS `CarUxRestrictionsManager` / `GEAR_SELECTION`. 매니페스트에 Distraction Optimization 선언 필요. D/R은 큰 글씨 한 줄, P에서만 목록·버튼 |
| 내비 지도에 쪽지 표시 | 불가 (마커 API 없음, `NAVI_CUSTOM_MAP`은 화면 전환 권한). 경유지 최대 2개는 가능 → 확장 |
| 미룬 기능 (12/3까지) | 쪽지 코스, 동승자(좌석) 쪽지, 놓친 쪽지함, 음성 답장, 가족 연결 해제, 주의 구간 보류 |
| 지도 | 카카오맵 JavaScript API. 키는 `static/kakao-key.js`(gitignore), 예시는 `kakao-key.example.js` |
| Git | 각자 브랜치 하나(`back`, `car`)에서 계속 작업, 체크포인트마다 main으로 PR. 커밋 제목 `[back]`, `[front]`, `[chore]` |

바로 쓸 수 있는 Pleos SDK는 **Vehicle 2.3.2, NaviHelper 3.0.5**뿐. Maven: `https://nexus-playground.pleos.ai/repository/maven-releases/`

## 7. 진행 상황

| 체크포인트 | 내용 | 상태 |
|---|---|---|
| A | 차 화면 QR → 연결 | ✅ 11/9 |
| B | 녹음 → 차가 받아 버튼 재생 → "들었어요" | ✅ 11/9 (노트북 브라우저 녹음 기준) |
| C | 지도에서 장소 선택 → 주행 시뮬레이션 중 자동 재생 | ⬜ 진행 중 |

- 서버 API 1~8, H2 파일 모드: main에 반영됨
- `place.html`(장소 고르기)과 화면 연결 수정: **11/12에 파일만 넣었고 아직 커밋 전**. 진짜 카카오맵 표시는 키 발급 전이라 미확인 (가짜 지도로 흐름만 테스트)
- 차 앱: 11/9에 API 호출, QR, 목록, 재생 버튼까지 동작. 그 뒤 main 반영 여부와 NaviHelper 진행은 **확인 필요**

## 8. 남은 일

### 백엔드 담당 (지금)
- [ ] 카카오 개발자 앱 생성 → JavaScript 키 → `kakao-key.js`에 입력, Web 도메인 `http://localhost:8080` 등록, 카카오맵 사용 설정 ON → `place.html`에서 지도 확인
- [ ] `place.html` 등 커밋 (`kakao-key.js`가 Changes에 보이면 안 됨)
- [ ] ngrok 설치(고정 도메인) → 카카오 도메인에 ngrok 주소 추가 → 진짜 폰(아이폰, 갤럭시)으로 QR·녹음 테스트. 녹음 형식 webm/mp4 둘 다 확인
- [ ] (선택) `type.html` 장소/좌석 선택 화면. 없어도 시연 가능
- [ ] README 작성 (공식 안내의 필수 항목 확인), `docs/api.md` 최신화

### 차 앱 담당
- [ ] NaviHelper 위치로 반경 진입 시 자동 재생 (haversine 거리)
- [ ] TTS 안내 멘트 → 녹음 재생 → API 8 순서
- [ ] 회전·방향지시등 보류, 주행 중 화면 단순화(DO 선언)
- [ ] 패키지 이름에서 실명 제거(`com.gilnote.car` 등), `pleosSample/outputs/`의 옛 기획서 docx 제거
- [ ] 릴리스 APK `밤바스콜_길쪽지_release.apk` → `apk/`

### 제출 준비 (같이)
- [ ] **11/13 개발 동결** (버그 수정만)
- [ ] 실명 없는 GitHub 계정/Organization으로 저장소 이전. Private이면 공지된 심사용 계정을 Collaborator로 초대
- [ ] 시연 영상: ① 차 QR → 폰 연결 ② 지도에서 장소 고르고 녹음 ③ 내비+길쪽지 분할 화면으로 주행 → 재생 ④ 폰에 "들었어요". 실명·연락처가 화면에 안 보이게
- [ ] 접수양식(v2 docx): 서명 2명, 서명일자, 작성일, 영상·GitHub 링크, 화면 시안 PDF 첨부 또는 해당 줄 삭제
- [ ] 이메일 제출: 제목 `[차량용 앱 개발 공모전] 팀명_대표자명`, 팀원 참조(CC)
- [ ] 자동 재생이 마감까지 안 되면 버튼 재생으로 영상을 찍는다

## 9. 테스트 명령 (PowerShell, 차 앱 없이 서버 확인)

```powershell
# 차 등록 → 연결 코드 → 브라우저로 가족 연결 화면 열기
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/cars/register -ContentType 'application/json' -Body '{"name":"test car"}'
$p = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/cars/pair-code -Headers @{ "X-Car-Token" = $r.carToken }
Start-Process "http://localhost:8080/join.html?code=$($p.code)"

# (브라우저에서 연결 → 장소 → 녹음 → 보내기 후) 차인 척 쪽지 받기, 음성 받기, 들었어요
$notes = @(Invoke-RestMethod -Uri http://localhost:8080/api/cars/notes -Headers @{ "X-Car-Token" = $r.carToken })
$id = $notes[0].noteId
Invoke-WebRequest -Uri "http://localhost:8080/api/cars/notes/$id/audio" -Headers @{ "X-Car-Token" = $r.carToken } -OutFile note.webm
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/cars/notes/$id/played" -Headers @{ "X-Car-Token" = $r.carToken } -ContentType 'application/json' -Body '{}'
```

파일 업로드를 명령으로 할 때는 `curl.exe`(`.exe` 포함)와 `-F "audio=@파일;type=audio/webm"`을 쓴다.

## 10. 하지 말 것

- 실명, 학교, 연락처를 코드·커밋·README·영상에 넣지 않기
- API 키(`kakao-key.js`), `.jks`, `local.properties`, `server/data/`, 디버그 APK, `.gradle`/`.idea`/`build` 커밋하지 않기
- GitHub Desktop의 "Untrusted server" 창에서 **Add certificate** 누르지 않기 (네트워크를 바꿔서 해결)
- 브랜치를 옮길 때 변경을 `main`에 stash로 남기지 않기 (변경이 사라진 것처럼 보임)
- 새 기능 욕심내지 않기. 마감 전 우선순위는 **체크포인트 C → 영상 → 제출 서류**
