# 길쪽지 · Pleos Connect

가족이 휴대폰에서 남긴 음성 쪽지를 차량에서 듣는 Pleos Connect 앱입니다. 장소 쪽지에 저장된 좌표와 반경을 이용해 차량이 가까워지면 TTS로 도착을 알리고 가족의 녹음을 재생합니다.

## APK 다운로드

[길쪽지 디버그 APK 다운로드](https://github.com/bambascol/Pleos/raw/refs/heads/main/apk/gilnote-debug.apk)

- 버전 `1.0` / versionCode `1` / 패키지 `com.jaemin.pleos`
- 개발·시연용 디버그 서명 빌드입니다.
- Android 최소 API 31, targetSdk 36, compileSdk 37을 사용합니다.
- 차량 SDK 서비스가 제공되는 Pleos Connect 환경과 실행 중인 백엔드가 필요합니다.
- 이 APK의 API 주소는 `http://10.0.2.2:8080`, QR 웹 주소는 `http://localhost:8080`입니다. 실제 휴대폰 연결에는 아래 설정 변경 후 재빌드가 필요합니다.
- 소스 기준 커밋 `030dcda` · SHA-256 `72ffd8e0998e88cabd5b81f3a5c1e2026fb7a862965137803e6e0f0e7cca12b6`

설치 예시

```sh
adb install -r apk/gilnote-debug.apk
```

## 사용 흐름

1. 백엔드를 실행하고 차량 앱을 엽니다. 차량 등록 후 인증 토큰을 앱에 저장합니다.
2. 차량의 **가족 연결**에서 QR을 발급합니다. 가족은 휴대폰으로 접속해 이름을 입력하고 차량과 연결합니다.
3. 가족 웹에서 최대 30초 음성을 녹음해 쪽지를 전송합니다.
4. 차량 앱이 20초마다 쪽지를 확인하고 음성 파일을 다운로드합니다.
5. NaviHelper로 2초마다 현재 좌표를 조회합니다. 장소 쪽지의 `radiusM` 안에 들어오면 온디바이스 TTS 안내 후 녹음을 재생합니다.
6. 녹음이 끝나면 완료 시각을 로컬에 보관하고 다음 동기화 때 서버에 전송합니다. 가족은 보낸 쪽지 화면에서 상태를 확인합니다.

## 현재 구현

| 영역 | 구현 내용 |
| --- | --- |
| 차량 UI | 받은 쪽지 목록·상세, 가로/좁은 창 대응, 수동 재생·중지, 가족 연결 QR·만료·재발급 |
| 장소 자동 재생 | NaviHelper 좌표와 쪽지 반경 비교, 동일 Activity 내 자동 재생 시도 중복 억제 |
| 음성 안내 | Pleos TextToSpeech ON_DEVICE 모드로 보낸 사람의 도착 안내 후 녹음 재생 |
| 완료 동기화 | 녹음 완료 기록을 로컬에 저장하고 서버 확인 후 제거 |
| 가족 웹 | QR 연결, 음성 녹음·미리 듣기·업로드, 보낸 쪽지 상태 조회 |
| 서버 | 차량·가족 토큰, 연결 코드, 쪽지 저장·조회, 음성 파일, 재생 완료 API |
| 차량 SDK 확인 | 별도 DoorTestActivity의 도어 조회 코드 |

현재 자동 재생은 차량 앱이 화면에 열린 동안 동작합니다. TTS는 쪽지 도착 안내를 합성하며, 메시지 본문은 가족의 녹음 파일을 사용합니다. 녹음 재생 중 오디오 포커스를 잃으면 재생을 중지합니다. 자동 재생 시도 후 중지·실패한 쪽지는 수동 재생으로 다시 들을 수 있습니다.

## 프로젝트 구성

```text
pleosSample/                     Kotlin Android 차량 앱
  app/src/main/java/com/jaemin/pleos/
    MainActivity.kt              화면, 동기화, 녹음 재생
    PlacePlayback.kt             NaviHelper 위치 조회, TTS 안내
    GilnoteApi.kt                차량용 HTTP API 및 응답 모델
    CarStore.kt                  차량 토큰 저장
    Config.kt                    API 및 가족 웹 주소
    Qr.kt                        QR 비트맵 생성
    DoorTestActivity.kt          차량 도어 SDK 테스트
server/                          Java Spring Boot 백엔드
  src/main/resources/static/     join.html, record.html, sent.html
apk/                             시연용 APK
docs/                            기획서와 API 명세
```

## 기술 구성

- 차량 앱은 Kotlin, Android Views, AppCompat, Coroutines, OkHttp, ZXing을 사용합니다.
- Pleos SDK는 Vehicle `2.3.2`, FusedLocation `1.1.3`, NaviHelper `3.0.5`, TextToSpeech `3.3.1`을 참조합니다. 위치 자동 재생에는 NaviHelper를 사용합니다.
- 서버는 Java 17, Spring Boot `4.1.2`, Spring Data JPA, H2 파일 DB를 사용합니다.
- 가족 웹은 HTML/JavaScript와 브라우저 MediaRecorder를 사용하며 서버가 정적 파일을 제공합니다.
- 차량 UI는 Pleos Connect-S 17의 앱 창 구성을 참고했습니다.

## 실행 및 빌드

### 백엔드

JDK 17을 준비하고 `server` 폴더에서 실행합니다.

```powershell
cd server
.\gradlew.bat bootRun
```

macOS/Linux에서는 `./gradlew bootRun`을 사용합니다. IntelliJ에서는 `com.gilnote.server.ServerApplication`을 실행할 수 있습니다. 기본 포트는 `8080`입니다.

H2 DB와 녹음 파일은 서버 실행 디렉터리의 `data/` 아래에 저장됩니다. `server` 디렉터리에서 실행하면 `server/data/`가 사용됩니다. 이 데이터는 Git에서 제외됩니다.

### 차량 앱

1. Android Studio에서 `pleosSample/`을 엽니다.
2. Android SDK 37과 Gradle 실행용 JDK를 설정합니다. 프로젝트 Gradle Wrapper는 `9.3.1`입니다. 서버의 Java 17과 별도로 Android Studio의 호환 JBR을 사용할 수 있습니다.
3. Pleos Connect SDK 서비스가 제공되는 에뮬레이터를 실행합니다.
4. `Config.kt`의 주소를 환경에 맞게 설정합니다.

```kotlin
const val API_BASE = "http://10.0.2.2:8080"
const val WEB_BASE = "https://your-public-server.example"
```

`10.0.2.2`는 Android 에뮬레이터에서 호스트 PC의 서버에 접근할 때 사용합니다. `WEB_BASE`는 휴대폰이 접근할 수 있는 동일 백엔드의 HTTPS 주소로 설정합니다. 휴대폰에서 `localhost`는 휴대폰 자체를 가리키므로 PC 서버에 연결되지 않습니다. 모바일 브라우저의 마이크 권한과 HTTPS 접속도 필요합니다.

```powershell
cd pleosSample
.\gradlew.bat assembleDebug
```

출력 파일은 `pleosSample/app/build/outputs/apk/debug/app-debug.apk`입니다. 주소를 바꾸면 다시 빌드해 설치합니다.

앱은 `INTERNET`, `CAR_DOORS`, `FUSED_LOCATION`, `NAVI_ROUTE`, `TTS_SERVICE` 권한을 Manifest에 선언합니다. SDK 호출 성공 여부는 에뮬레이터의 서비스 및 권한 설정에서도 확인해야 합니다.

### 가족 웹과 위치 시연

차량에서 발급한 QR의 `join.html?code=...`로 연결한 후 녹음 화면으로 이동합니다. 현재 지도 선택 화면은 개발 예정이며, 녹음 화면은 URL 파라미터로 장소를 받을 수 있습니다.

```text
record.html?lat=37.5665&lng=126.9780&radius=200&label=회사앞
```

파라미터가 없으면 기본 좌표 `37.5665, 126.9780`, 반경 `200m`를 사용합니다. 차량 앱을 열어 쪽지를 내려받은 뒤 Pleos 시뮬레이션 환경에서 NaviHelper가 해당 범위의 좌표를 전달하도록 설정해 자동 재생을 확인합니다.

## API

| 메서드 | 경로 | 역할 |
| --- | --- | --- |
| POST | `/api/cars/register` | 차량 등록 |
| POST | `/api/cars/pair-code` | 연결 코드 발급 |
| POST | `/api/join` | 가족 연결 |
| POST | `/api/notes` | 음성 쪽지 업로드 |
| GET | `/api/notes` | 가족이 보낸 쪽지 조회 |
| GET | `/api/cars/notes` | 차량의 미청취 쪽지 조회 |
| GET | `/api/cars/notes/{noteId}/audio` | 음성 다운로드 |
| POST | `/api/cars/notes/{noteId}/played` | 재생 완료 기록 |

차량은 `X-Car-Token`, 가족은 `X-Member-Token` 헤더를 사용합니다. 상세 요청과 응답은 [API 명세](docs/api.md)를 참고하세요.

## 검증 범위와 개발 예정

Android `assembleDebug` 빌드 성공을 확인했습니다. 실제 에뮬레이터에서 위치 진입, TTS 음성 출력, 서버와 휴대폰을 포함한 전체 흐름은 추가 검증이 필요합니다.

다음 기능은 기획에 포함되어 있으며 추가 개발 대상입니다.

- 좌석 착석 조건에 따른 동승자 쪽지 자동 재생
- 지도에서 장소 선택, 쪽지 코스, 음성 답장
- 백그라운드 위치 감시 및 재생
- CarUxRestrictions에 따른 주행 중 조작 제한
- 회전 안내 시점에 따른 재생 보류, 위치 오차·방향을 고려한 판단
- Figma 아이콘·Asta Sans 적용, 야간 테마 완성

기획 내용은 [프로젝트 기획서](docs/plan.md), 화면 설계는 [길쪽지 Figma](https://www.figma.com/design/D5Xqscf3JgNJaBabpyPxni?node-id=14-5582)에서 확인할 수 있습니다.
