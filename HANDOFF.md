# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

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

## 2026-11-14 · 공유 보강: 서버 맞추기 버튼, 받기 안전장치

### 한 일
- 공유 프로젝트의 "여러 기기에서 함께 쓰기" 칸에 "서버를 이 기기 내용으로 맞추기"(확인 창) 추가. 이 기기와 다른 칸을 완료 상태까지 모두 서버로 보냄. 같으면 "이미 같습니다".
- 공유 받기는 서버에서만 읽음(연결이 불안하면 빈 사본 대신 "서버에 연결하지 못했습니다").
- 설치형 앱은 Firestore 롱 폴링 사용.
- 계기: 앱에서 받은 프로젝트가 비어 보인다는 신고 → 사용자가 다른 프로젝트와 헷갈린 것으로 확인(버그 아님).

### 확인한 것
- Firebase 에뮬레이터+Playwright: 브라우저 둘 사이 공유 받기 299칸 일치, 서버를 일부러 옛 내용으로 둔 뒤 맞추기 → 받은 쪽 일치, 다시 누르면 "이미 같습니다".
- 시험하지 못한 것: 실기기 앱에서 롱 폴링 연결.

### 코드 위치 (index.html)
- `sync-reup` 동작, `syncPending`(`S.forceAll`), `syncFlush`(완료 알림), `syncProjectCardInner`(버튼), firestore 어댑터 `getAll`(getDocsFromServer)·`initializeFirestore`

### 다음에 할 일
- 사용자: 실기기에서 알림창 ＋1 → 브라우저 반영 확인.

## 2026-11-14 · 설치형 앱 ↔ 브라우저 연동(앱 구글 로그인)

### 한 일
- 앱에서 "구글로 로그인"을 누르면 안드로이드 계정 선택 창이 뜨고, 그 계정으로 Firebase에 로그인(기존 공유 기능 그대로 사용).
- 연동 방법: 브라우저에서 "공유 시작" → 앱에서 로그인 후 "공유 ID·코드로 받기" → 이후 실시간 맞춤.
- 웹앱 홈 맨 아래(전체 백업 아래)에 "안드로이드 앱 · 앱 설치 파일 받기" 카드(브라우저에서만, 앱 안에서는 숨김).
- 사용자가 Firebase에 안드로이드 앱(패키지 io.github.ij17838creator.pbs, SHA-1 9E:52:…:9D:79) 등록, 웹 클라이언트 ID 받음.

### 확인한 것
- GitHub Actions 빌드 성공. 브라우저 화면 회귀(Playwright) 이상 없음.
- 시험하지 못한 것: 실기기 로그인·공유 받기·실시간 맞춤(이 환경엔 안드로이드·구글 로그인 없음).

### 알려진 한계
- SHA-1은 시험판 키(pbs-debug.keystore) 것. 플레이스토어 출시 키를 쓰면 그 SHA-1도 Firebase에 추가해야 함.
- 로그인 실패 시 "이 기기에 구글 계정이 없거나 앱이 Firebase에 등록되지 않았습니다"가 뜨면 SHA-1 등록부터 확인.

### 코드 위치
- index.html: `APP_DL_URL`, `viewHome`(설치 카드), `GOOGLE_WEB_CLIENT_ID`, `PAUTH`, firestore 어댑터 `signIn`·`signOut`, `syncEnsureUser`
- android: `PbsAuthPlugin`(Credential Manager), app/build.gradle credentials·googleid 의존성

### 다음에 할 일
- 사용자: 탭에서 새 apk 덮어 설치 → 로그인 → 공유 받기 → 알림창 ＋1이 브라우저에 보이는지 확인.

