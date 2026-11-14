# AutoLog — 공용차 대여·반납 자동 대장

현대자동차그룹 차량용 앱 개발 공모전 출품작  
Pleos Connect 인포테인먼트 기반 **공용차 대여·반납 대장** 자동화 앱

---

## 빌드 환경

| 항목 | 버전 |
|------|------|
| Android API | 28+ (minSdk 28, targetSdk 34) |
| Gradle | 8.9 |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 2.0.21 |
| Jetpack Compose BOM | 2024.09.03 |
| Room | 2.6.1 |

## 테스트 화면 규격

- **화면 방향**: 가로(Landscape) 고정
- **권장 해상도**: 1280×800 (16:10) 또는 1920×1080 (16:9)
- **밀도**: mdpi ~ xhdpi
- **다크 테마** 기본 (배경 #0F1419)

---

## 주요 기능

### 1. 메인 대시보드 (HomeScreen)
- 대여 상태 배지 (`대기 중` / `대여 운행 중`)
- 운전자 선택 칩 (최근 운전자 원터치 선택 + 신규 입력)
- 기어(P/D/R/N) 상태, 계기판 주행거리, 연료 잔량(%)
- 대여 시작 후 경과 거리·기름 소모 실시간 표시
- 주행 중 배지: `"대여 운행 기록 중 · 00:32 경과"`
- 최근 대여·반납 기록 리스트

### 2. 반납 직후 확인 팝업 (TripSummaryOverlay)
- **트리거**: D/R → P 기어 전환 + 0.5km 이상 주행 감지 시 자동 표시
- 반납 영수증 형태: 운전자 · 대여→반납 시각 · 주행거리 · 기름 소모
- 목적지 직접 입력 + 목적 버튼 4개 (거래처 미팅 / 현장 방문 / 자재 수령 / 개인 용무)
- TTS 안내: `"반납 처리할까요? 방문 목적을 말씀하시거나 선택해 주세요."`
- 10초 무응답 시 자동 `미분류` 저장

### 3. 대장 내보내기 및 외부 공유 (ExportScreen)
| 버튼 | 동작 |
|------|------|
| **공용차 대여·반납 대장** (기본) | 운전자·빌린시각·반납시각·출발/목적지·주행거리·기름양 CSV |
| **국세청 운행기록부** (옵션) | 세무 제출 표준 포맷 CSV |
| **공유** | Android Share Intent (카카오톡·슬랙·이메일 등) |
| **웹훅/메일 전송** | Webhook URL 입력 → HTTP POST (JSON) |

---

## 시연 방법 (에뮬레이터 / 실기기)

1. `apk/AutoLog-debug.apk` 설치
2. 앱 실행 → 운전자 칩에서 이름 선택 (또는 + 버튼으로 신규 입력)
3. 우상단 **🐛** 버튼 → 시뮬레이터 패널 표시
4. **대여 시작** 클릭 → 대여 세션 오픈, 계기판 스냅샷 자동 저장
5. **주행 +18.4km** 클릭 → 주행 거리·연료 소모 반영
6. **P단 반납** 클릭 → 반납 확인 팝업 자동 표시
7. 목적지 입력 + 목적 선택 → **반납 완료**
8. 대시보드 최근 기록에 대여 세션 반영 확인
9. **대장 내보내기 / 공유** → 프리셋 선택 후 CSV 저장 또는 공유

---

## 빌드 방법

```bash
# Android SDK 경로를 local.properties에 설정
echo "sdk.dir=/path/to/android-sdk" > local.properties

./gradlew assembleDebug
# → apk/AutoLog-debug.apk
```

---

## 아키텍처

```
VehicleDataSource (interface)
  ├── MockVehicleDataSource   ← 현재 사용 (시뮬레이터)
  └── PleosVehicleDataSource  ← Pleos SDK AAR 연동 시 교체

Room DB (v2)
  ├── RentalTripEntity        (driverName, startTime, endTime,
  │                            startOdometer, endOdometer,
  │                            startFuelPct, endFuelPct,
  │                            origin, destination, purpose, status)
  └── FrequentPlaceEntity

UI (Jetpack Compose, MVVM + StateFlow)
  ├── HomeScreen + HomeViewModel   — 대여 세션 관리
  ├── TripSummaryOverlay           — 반납 영수증 팝업
  └── ExportScreen + ExportViewModel
        ├── ExportPreset.RENTAL_LOG   — 공용차 대장 CSV
        ├── ExportPreset.NTS          — 국세청 양식 CSV
        ├── Android Share Intent
        └── Webhook HTTP POST (HttpURLConnection, INTERNET 권한)
```

> **Pleos SDK 연동**: `libs/` 폴더에 SDK AAR을 추가하고 `PleosVehicleDataSource`를 구현한 뒤
> `AutoLogApp.kt`의 `MockVehicleDataSource()`를 교체하면 실차 연동 완료.
