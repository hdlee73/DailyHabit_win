# DailyHabit_win (Windows 데스크톱 에디션)

Android 앱 [Dailyhabit](https://github.com/hdlee73/Dailyhabit)을 기반으로 구축된 **Compose Multiplatform (Desktop JVM)** 윈도우 데스크톱 애플리케이션입니다.

---

## 주요 기능 및 구성

1. **오늘의 말씀 (복음) & 오늘의 명언**
   - **천주교인용 모드**: 한국천주교주교회의 '매일미사'(maria.catholic.or.kr) 웹 크롤링 및 로컬 오프라인 캐싱, 매일미사 원문 링크 연동
   - **일반 모드**: 매일 날짜에 맞추어 순환하는 오늘의 명언과 영문 번역, 클립보드 복사 지원
2. **기도서 & 묵주기도**
   - 가톨릭 기도서 주요 기도문 (기본 기도, 하루 기도, 성모 기도, 평화와 교회, 가정 기도 등)
   - 요일별 묵주기도 신비(환희/빛/고통/영광) 자동 안내 및 상세 기도 순서
   - 폰트 크기 확대/축소 지원
3. **할 일 (Todo)**
   - 우선순위(낮음/보통/높음), 카테고리 필터링(개인, 가정, 일, 신앙, 건강, 쇼핑)
   - 마감일 및 시간 설정, 완료된 할 일 일괄 정리
4. **매일 루틴 (Habit/Routine)**
   - 요일별 반복 설정, 스트릭(연속 실천일수), 최고 연속 기록, 최근 7일 달성률 통계
   - 천주교인용 '오늘의 말씀 읽기' / '기도하기' 화면 조회 시 자동 루틴 체크 연동
5. **한 줄 일기 (Journal)**
   - 날짜별 기분 이모지 선택 및 200자 한 줄 일기 기록
6. **맛집 (Restaurants)**
   - 식당 카테고리, 대표 메뉴, 주소, 블로그/지도 링크, 별점, 한 줄 평, 가보고 싶은 곳(위시리스트)
7. **독서 기록 (Books)**
   - 읽는 중 / 다 읽은 / 읽고 싶은 상태 관리, 페이지 수 진행률 바, 한 줄 소감
8. **일정 (Schedule)**
   - 마감일이 있는 할 일을 앞으로 7일 캘린더 뷰로 매핑
9. **장보기 (Shopping)**
   - 간편 품목 추가 및 체크리스트 관리
10. **월간 돌아보기 (Review)**
    - 월별 일기 수, 맛집 방문 횟수, 완독 권수, 루틴 달성 현황, 기분 분포 통계
11. **설정, 백업 & 복원 (Backup & Restore)**
    - Android 앱의 백업 JSON 파일과 **100% 상호 호환**되는 데이터 내보내기/가져오기
    - 한 줄 일기 및 맛집 데이터를 표 형식의 `.xlsx` 엑셀 파일로 내보내기
    - 천주교인용 ↔ 일반용 모드 즉시 전환 지원

---

## 기술 스택

- **UI Framework**: Compose Multiplatform for Desktop (Kotlin 2.1.0, Compose 1.7.3, Material 3)
- **Language & Runtime**: Kotlin JVM / Java 21 LTS
- **Database**: SQLite JDBC (`org.xerial:sqlite-jdbc`)
- **Network & Parsing**: Jsoup (복음 크롤링), org.json (백업/설정 JSON 직렬화)
- **Export**: 자체 구현 순수 XML ZipOutputStream 기반 XLSX 생성기

---

## 실행 방법

```bash
# 개발 모드 실행
./gradlew run

# 윈도우 설치형 패키징 (MSI / EXE)
./gradlew packageMsi
./gradlew packageExe
```
