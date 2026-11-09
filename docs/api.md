# 길쪽지 API 약속표 (v0.1 초안)

> 이 표가 기준입니다. 바꿀 때는 둘이 말하고 **이 파일부터** 고친 다음 코드를 고칩니다.

## 기본 규칙

| 항목 | 내용 |
|---|---|
| 서버 주소 | 개발: `http://localhost:8080` / 에뮬레이터에서: `http://10.0.2.2:8080` / 폰에서: ngrok https 주소 |
| 데이터 형식 | JSON (쪽지 올리기만 `multipart/form-data`) |
| 차 인증 | 요청 헤더 `X-Car-Token: {carToken}` |
| 가족 인증 | 요청 헤더 `X-Member-Token: {memberToken}` |
| 시간 형식 | ISO-8601 문자열, 예: `"2026-11-09T18:42:00Z"` |
| 에러 응답 | `{ "error": "CODE_EXPIRED", "message": "연결 코드가 만료됐어요" }` + HTTP 상태코드 |

### 쪽지 상태 (`status`)
| 값 | 뜻 | 폰 화면 표시 |
|---|---|---|
| `PENDING` | 보냈고 차가 아직 안 받아감 | 전달 대기 |
| `DELIVERED` | 차가 받아감 (아직 재생 전) | 차에 도착 |
| `PLAYED` | 차에서 재생됨 | 들었어요 ✓ |

### 담당
| API | 담당 |
|---|---|
| 1 ~ 5 | 백엔드 A (폰·연결 쪽) |
| 6 ~ 8 | 백엔드 B (차 쪽, 차량 앱 담당이 직접 구현) |

---

## 1. 차 등록 `POST /api/cars/register`
차 앱이 **처음 켜질 때 한 번** 호출. 받은 `carId`, `carToken`은 차 앱에 저장.

요청
```json
{ "name": "우리 가족 차" }
```
응답 `200`
```json
{ "carId": "c_7f3a9b", "carToken": "ct_1a2b3c4d5e" }
```

## 2. 연결 코드 받기 `POST /api/cars/pair-code`
헤더: `X-Car-Token`
차 화면 "가족 연결" 버튼을 누를 때 호출. 차 앱은 `{웹주소}/join.html?code={code}`를 QR로 만들어 표시.

요청: 본문 없음
응답 `200`
```json
{ "code": "482913", "expiresAt": "2026-11-09T12:03:00Z" }
```
- 코드는 6자리 숫자, **3분 후 만료**, 한 번 쓰면 사라짐

## 3. 가족 등록 `POST /api/join`
폰 웹에서 QR로 들어와 이름을 입력하면 호출. 받은 `memberToken`은 폰 브라우저에 저장.

요청
```json
{ "code": "482913", "name": "엄마" }
```
응답 `200`
```json
{ "memberId": "m_91ab", "memberToken": "mt_9z8y7x6w", "name": "엄마", "carName": "우리 가족 차" }
```
에러: `404 CODE_NOT_FOUND`, `410 CODE_EXPIRED`(만료 또는 이미 사용)

## 4. 쪽지 올리기 `POST /api/notes`
헤더: `X-Member-Token` / 형식: `multipart/form-data`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `audio` | 파일 | ✓ | 녹음 파일 (webm 또는 m4a, 최대 30초, 10MB 이하) |
| `triggerType` | 문자 | ✓ | `PLACE` 또는 `SEAT` |
| `lat` | 숫자 | PLACE일 때 | 위도, 예: `37.5665` |
| `lng` | 숫자 | PLACE일 때 | 경도, 예: `126.9780` |
| `radiusM` | 숫자 | | 반경(m), 기본 `200` |
| `placeLabel` | 문자 | | 장소 이름, 예: `"회사 앞 빵집"` |
| `seat` | 문자 | SEAT일 때 | `ROW_2_LEFT` / `ROW_2_RIGHT` |
| `courseId` | 숫자 | | 쪽지 코스에 묶을 때 (v0.2) |

응답 `200`
```json
{ "noteId": 42, "status": "PENDING", "createdAt": "2026-11-09T03:12:00Z" }
```

## 5. 내가 보낸 쪽지 목록 `GET /api/notes`
헤더: `X-Member-Token`
폰 웹의 "보낸 쪽지" 화면. 10초마다 다시 불러서 "들었어요"를 갱신.

응답 `200`
```json
[
  {
    "noteId": 42,
    "triggerType": "PLACE",
    "placeLabel": "회사 앞 빵집",
    "seat": null,
    "status": "PLAYED",
    "createdAt": "2026-11-09T03:12:00Z",
    "playedAt": "2026-11-09T09:42:00Z"
  }
]
```
- 아직 안 들은 쪽지는 `playedAt`이 `null`
- 토큰이 없거나 틀리면 `401 INVALID_MEMBER_TOKEN`

## 6. 차의 쪽지 목록 `GET /api/cars/notes`
헤더: `X-Car-Token`
차 앱이 **20초마다** 호출. 아직 재생 안 된 쪽지(`PENDING`, `DELIVERED`)만 내려줌.
서버는 이때 `PENDING` → `DELIVERED`로 바꿈.

응답 `200`
```json
[
  {
    "noteId": 42,
    "from": "엄마",
    "triggerType": "PLACE",
    "lat": 37.5665,
    "lng": 126.9780,
    "radiusM": 200,
    "placeLabel": "회사 앞 빵집",
    "seat": null,
    "audioUrl": "/api/cars/notes/42/audio",
    "createdAt": "2026-11-09T03:12:00Z"
  }
]
```

## 7. 음성 파일 받기 `GET /api/cars/notes/{noteId}/audio`
헤더: `X-Car-Token`
차 앱은 목록을 받으면 **바로 미리 내려받아 저장** (터널·산길에서도 재생되도록).

응답 `200`: 음성 파일 그대로 (`Content-Type: audio/webm` 등)
에러: `404` (다른 차의 쪽지이거나 없음)

## 8. 들었어요 `POST /api/cars/notes/{noteId}/played`
헤더: `X-Car-Token`
차에서 재생이 끝나면 호출. 인터넷이 끊겨 있으면 차 앱에 모아뒀다가 연결되면 보냄.

요청
```json
{ "playedAt": "2026-11-09T09:42:00Z" }
```
응답 `200`
```json
{ "noteId": 42, "status": "PLAYED" }
```

---

## 나중에 추가할 것 (v0.2 이후)
| API | 용도 |
|---|---|
| `GET /api/cars/members` / `DELETE /api/cars/members/{id}` | 차 화면에서 연결된 가족 관리·해제 |
| `DELETE /api/notes/{noteId}` | 재생 전 쪽지 취소 |
| `POST /api/courses`, `GET /api/courses` | 쪽지 코스(나만의 내비) |
| `POST /api/cars/notes/{noteId}/reply` | 정차 중 음성 답장 |

## 개발 순서 팁
- 서버가 덜 됐을 때 차 앱은 **위 응답 예시를 가짜 데이터로** 넣고 먼저 화면을 만든다.
- API 하나 만들 때마다 **Postman(또는 IntelliJ HTTP Client)으로 직접 호출**해서 응답이 이 표와 같은지 확인한다.
