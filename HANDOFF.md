# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-14 · 앱 밖 측정을 ABC 관찰에도, QABF 1명도 비교 그래프
### 한 일
- ABC 관찰 화면 '지금 시각 찍기' 아래에 "앱 밖 측정 켜기"(위젯·빠른 설정·알림창). ＋1마다 발생 시각 한 행(B=표적행동 이름), −1은 마지막으로 찍은 빈 행 삭제. 앱을 열면 관찰지 1장(타이머 없음)으로 들어가고, 진행 중 관찰이 없으면 '지금 관찰'로 보임.
- QABF "응답자별 비교" 그래프가 응답자 1명이어도 나옴(전에는 2명 이상만).
- 기초선·중재는 이미 같은 기능이 있음(2026-11-14 앱 밖 측정 항목 참고). 측정 대상은 여전히 한 번에 하나.
- index.html만 바뀜 → 새 apk(화면 자동 업데이트)에서는 재설치 없이 반영.
### 데이터
- ABC 관찰지에 `natId`, 행에 `nat:1`(앱 밖에서 찍은 행). 앱 밖 관찰 세션 id는 'nat시작ms'.
### 확인한 것
- Playwright+가짜 플러그인: 앱 닫힌 동안 ＋1×3·−1 → 관찰지 1장·2행, 앱 열린 채 ＋1 → 바로 3행, QABF 응답자 1명 그래프 표시, 기존 기초선·지속시간 시험 다시 통과, 콘솔 오류 없음.
- 시험하지 못한 것: 실기기.
### 알려진 한계
- 알림·위젯의 버튼 글자는 ABC에서도 "회기 끝"(바꾸려면 재설치 필요해서 그대로 둠).
### 코드 위치 (index.html)
- `natAbcSheet`, `natAbcTap`, `natFind`·`natResolve`·`natMode(p,ph)`의 'abc' 분기, `obsView`(natTargetRow), `qCompare` 호출 조건(viewQabf)
### 다음에 할 일
- 사용자: 새 apk 설치 후 ABC 화면에서 앱 밖 측정 켜고 위젯 ＋1 → 관찰지 확인.

## 2026-11-14 · 설치형 앱 화면 자동 업데이트(재설치 줄이기)
### 한 일
- 앱이 열릴 때·앞으로 나올 때(5분에 한 번까지) GitHub Pages의 index.html을 받아 두고, 다음 실행부터 그 화면을 씀. 위에 "새 화면 버전이 있어요 · 지금 적용/나중에" 막대.
- 화면(index.html)만 바뀐 업데이트는 재설치 불필요. android 폴더가 바뀐 때만 새 apk 필요.
- 받은 화면이 열리다 멈추면 다음 실행에 설치본 화면으로 돌아가고 그 버전은 다시 받지 않음.
- 사용 안내: docs/android-app.md "화면 자동 업데이트".
### 데이터
- 앱 주소(https://localhost)는 그대로라 localStorage 등 앱 자료 유지. 받은 화면은 앱 안 files/web/(sha 16자리)/.
### 확인한 것
- Playwright+가짜 플러그인: 새 버전 막대 표시, 지금 적용 호출, 앱 밖 측정 시험 다시 통과, 콘솔 오류 없음.
- GitHub Actions 빌드 성공(브랜치 run 15). 자바 코드 컴파일됨.
- 시험하지 못한 것: 실기기에서 실제 받기·적용·오프라인 동작·되돌리기. 이 환경에서는 github.io 접속이 막혀 있어 받기 자체도 못 봄.
### 알려진 한계
- 새 화면이 예전 apk에 없는 안드로이드 기능을 부르면 그 버튼만 안 됨(오류 알림).
- Pages 캐시 때문에 main 반영 후 최대 10분쯤 늦게 받을 수 있음.
- main에 올린 index.html은 시험 전에도 앱에 바로 퍼짐(웹과 같음).
### 코드 위치 (index.html)
- `webInit`, `webCheck`, `webShow`, `webApply`, 동작 `web-apply`·`web-later`, CSS `.web-upd`
- android: `WebUpdater`(boot·check·applyNow·ready, PAGES_URL), `MainActivity.onCreate`(boot), `PbsCounterPlugin`(webCheck·webApply·webReady). Capacitor `CapWebViewSettings.serverBasePath` 사용.
### 다음에 할 일
- 사용자: 이번 apk 한 번 설치 → 앱 열고 인터넷 연결 확인. 다음 화면 수정 때 막대가 뜨는지 봐 주기.
- 앱 밖 측정 실기기 확인(바로 아래 항목 참고).

## 2026-11-14 · 앱이 꺼져 있어도 측정(위젯·빠른 설정·알림창)
### 한 일
- 기초선·중재 화면 회기 목록 아래 "앱 밖 측정 켜기/끄기"(빈도·지속시간만, 그 밖은 "앱 밖 측정 미지원"). 켜면 "위젯 놓기"·"빠른 설정 추가" 버튼.
- 홈 화면 위젯 'PBS 측정', 빠른 설정 타일 'PBS 측정', 알림창 버튼: 앱을 실행 안 했거나 최근 앱에서 지웠거나 재부팅 뒤에도 바로 기록.
  - 빈도: ＋1(첫 ＋1에 새 회기 시작) · −1 · 회기 끝, 0회용 "회기 시작". 지속시간: 행동 시작/끝(흐르는 시간 표시) · 회기 끝.
  - 회기 끝을 잊어도 날짜가 바뀌면 다음 버튼부터 새 회기.
- 앱을 열면 누른 날짜·시각으로 회기를 새로 만들고 값 반영, "앱 밖에서 시작한 회기 n개를 넣었습니다".
- 재부팅·앱 업데이트 뒤 알림·위젯 다시 그림. 사용 안내는 docs/android-app.md에 추가.
### 데이터
- 회기에 `natId`('new:시작ms', 앱 밖에서 연 회기), `durLog`('시:분:초-시:분:초' 목록) 추가. 예전 자료는 그대로.
- 지속시간 값: 단위에 '/회'만 있으면 1회 평균 분, 아니면 총 분. durLog로 다시 계산.
### 확인한 것
- GitHub Actions 빌드 성공(브랜치, run 13). 자바 코드가 컴파일됨.
- Playwright + 자바 동작을 옮긴 가짜 플러그인: 앱 닫힌 동안 ＋1×3·−1·회기 끝·회기 시작/끝 → 앱 열면 회기 2개(값 2, 0) 생김, 앱 열린 채 위젯 ＋1 → 새 회기 즉시 반영, 지속시간 90·150·30초(분/회) → 1.5, 끄기·강도 미지원 표시, 콘솔 오류 없음.
- 시험하지 못한 것: 실기기 전부(위젯 모양·눌림, 타일, 잠금 화면, 재부팅 뒤 알림, 위젯 놓기·타일 추가 창, 진동).
### 알려진 한계
- 측정 대상은 한 번에 하나. 지속시간 값을 손으로 고쳐도 다음 버튼 때 durLog로 다시 계산됨.
- 잠금 화면에서 타일·알림 버튼이 눌리는지는 기기 설정에 따라 다름[추정]. 위젯은 1분마다 새로 그리지 않음(버튼 누를 때만).
### 코드 위치 (index.html)
- `natTargetRow`, `natTargetToggle`, `natMode`, `natResolve`, `natDurValue`, `natDurRow`, `natDrain`(k: b·ds·de), `natKey`(natId), 동작 `nat-target`·`nat-pin`·`nat-tile`
- android: `CounterStore`(setTarget·begin·durToggle·prepare), `CounterWidget`, `CounterTile`, `BootReceiver`, `CounterNotifier.refreshAll`, `PbsCounterPlugin`(setTarget·clearTarget·pinWidget·addTile), res/layout/widget_counter.xml
### 다음에 할 일
- 사용자: 새 apk 설치 → 앱 밖 측정 켜기 → 위젯·타일 추가 → 앱을 최근 앱에서 지운 뒤 ＋1 → 앱 열어 회기 확인. 재부팅 뒤 알림 다시 뜨는지도.
- 후보: 이어폰·음량 버튼 ＋1, 측정 대상 여러 개 고르기.

