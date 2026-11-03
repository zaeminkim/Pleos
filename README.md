# 길쪽지

> 가족이 폰으로 남긴 음성 쪽지를, 차가 '그 장소'에 다가가거나 '그 사람'이 함께 탔을 때 알아보고 안전한 순간에 들려주는 Pleos Connect 앱

<!-- 블라인드 심사: 이 저장소에는 실명, 학교, 연락처를 적지 않습니다. -->

## 1. 앱 소개
- **장소 쪽지**: 가족이 지도에서 고른 장소에 차가 다가가면 자동 재생
- **동승자 쪽지**: 특정 좌석에 사람이 탔을 때만 열리는 쪽지
- **쪽지 코스**: 경로를 따라 여러 쪽지를 이어 만드는 나만의 내비게이션
- **들었어요**: 재생되면 보낸 사람 폰에 표시

## 2. 구성
| 폴더 | 내용 | 기술 |
|---|---|---|
| `car-app/` | 차량 앱 | Kotlin, Jetpack Compose, Pleos Connect SDK |
| `server/` | 서버 + 가족용 폰 웹페이지 | Java 17, Spring Boot |
| `apk/` | 제출용 APK | |
| `docs/` | API 약속표 등 문서 | |

## 3. 실행 방법
### 서버
1. JDK 17 설치
2. `server/` 폴더를 IntelliJ로 열기
3. `GilNoteServerApplication` 실행 → `http://localhost:8080`

### 차량 앱
1. `car-app/` 폴더를 Android Studio로 열기
2. Pleos Connect Emulator 실행
3. Run ▶ (에뮬레이터에서 서버 주소는 `http://10.0.2.2:8080`)

### 가족 폰 웹
1. 차량 앱의 "가족 연결" 화면에 뜨는 QR을 폰 카메라로 스캔
2. (폰에서 접속하려면 서버를 https 주소로 열어야 함: ngrok 등)

## 4. 테스트 환경 및 필요한 설정
- Pleos Connect Emulator v2.1.0 (API 34)
- Android Studio (최신), Gradle 8.x, minSdk 28
- 사용 SDK: Vehicle, NaviHelper, FusedLocation, TextToSpeech
- 권한: `pleos.car.permission.CAR_DRIVE`, `CAR_LIGHTS`, `CAR_SEATS`, `NAVI_ROUTE`, `FUSED_LOCATION`, `TTS_SERVICE`
- 시연용 "가상 주행" 버튼으로 실제 이동 없이 쪽지 장소 도착을 재현할 수 있음

## 5. APK
- 위치: `apk/팀명_길쪽지_release.apk` <!-- 팀명 확정 후 수정 -->
