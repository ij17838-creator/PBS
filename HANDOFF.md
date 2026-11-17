# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-17 · 앱 안 공지(바뀐 점·새 apk 알림)
### 한 일
- 위 막대에 '공지' 버튼(안 본 소식 수 빨간 점). 누르면 바뀐 점 목록, 항목마다 '새 apk 필요'/'앱 재시작만' 표시.
- 설치형 앱에서는 설치된 apk 단계와 비교해 항목마다 '설치됨'/'아직 안 깔림', 위에 '새 apk가 있어요 · apk 받기'.
- 홈 맨 위 작은 카드: 안 본 소식 n개 또는 '새 apk가 있어요'(apk 받기·공지 보기).
- 지금까지 기능 8개를 공지로 채움(11/14~11/17).
- CLAUDE.md에 규칙 추가: 변화마다 NOTICES에 항목, android 바꾸면 플러그인 ver 올리고 need에 적기.
### 데이터
- localStorage `pbs-notice-seen`(마지막으로 본 공지 id). 처음엔 전부 새 소식.
### 확인한 것
- Playwright(가짜 플러그인): 웹·ver 4·ver 5(횟수 없음)·최신 각각 점·홈 카드·대화 상자 표시, 닫으면 점 사라짐, 412px 폭 화면. 오류 없음.
- 시험하지 못한 것: 실기기.
### 알려진 한계
- apk 단계는 플러그인 ver로만 앎. ver 5 중 음량 횟수 있는 apk는 btn.count로 구분해 5.1로 봄. 앞으로 android 바꿀 때 ver를 안 올리면 '새 apk' 알림이 안 뜸.
- 앱은 화면을 받아 둔 뒤 다음 실행에 반영 → 공지도 그때 보임.
### 코드 위치 (index.html)
- `NOTICES`·`NOTICE_NEED`·`noticeInit`·`noticeOpen`·`noticeHome`·`noticeDot` / CSS `.notice-*`
### 다음에 할 일
- 사용자: 앱 재시작 후 '공지' 눌러 보기. 새 apk 깔았는데 '아직 안 깔림'이면 알려 주기.

## 2026-11-17 · 음량 버튼 누를 횟수 설정(기본 1번)
### 한 일
- '음량 3번' 칸 → '음량 버튼' 칸. '누를 횟수' 1·2·3번 선택, 기본 1번. 칸 제목 '음량 버튼 · 1번=＋1'처럼 표시.
- 1번: 누를 때마다 바로 기록(취소 대기 없음 → 빠르게 이어 눌러도 다 셈). 잘못 누르면 알림창 −1. 2·3번: 예전처럼 정한 횟수 뒤 1.2초 안 한 번 더 = 취소.
- 꾹 누름(반복 신호)은 계속 무시.
### 데이터
- 기기 저장 btnCount(없으면 1 → 예전에 3번으로 쓰던 기기도 1번으로 바뀜). state.btn.count, setButtons{count}.
### 확인한 것
- Playwright: 칸 표시·setButtons count. Actions 빌드 성공(run 44031319353). 시험하지 못한 것: 실기기 1번·2번 동작.
### 코드 위치 (index.html)
- `natBtnBox`·`natBtnSet` / android: PressWatch(press의 need, commitNow), CounterStore.btnCount·setBtn
### 다음에 할 일
- 사용자: 새 apk → 1번으로 빠르게 여러 번 눌러 모두 세지는지, 꾹 누름이 안 세지는지 확인.

## 2026-11-17 · 측정 시간·자동 갱신, 회기 시작 때 1회 안 세기
### 한 일
- 실기기 확인: 음량 3번(소리 없는 재생 방식) 갤럭시 탭에서 작동(기록 '음량 1~3번째 → ＋1').
- 회기 시작 때 1회가 저절로 세지던 것 삭제: 열린 회기가 없을 때 ＋1(위젯·알림창)과 빠른 설정 타일은 회기 시작만(0회). 타일 이름 'PBS 회기 시작'.
- 앱 밖 측정 켜기 아래 '측정 시간' 칸: 시간(없음·5~60분), '끝나면 자동 갱신', '갱신 때 횟수 0으로'. 끝나면: 갱신 끔=회기 닫음 / 갱신+0으로=그 시각에 새 회기(지속시간 진행 중이면 새 회기에서 이어서) / 갱신+0으로 끔=같은 회기에서 시간만 다시. 알림 '측정 시간 n분 끝'+진동. 알림·위젯 설명에 'HH:mm까지'.
### 데이터
- 기기 저장 sesMin·sesRenew·sesReset·sesStart. 플러그인 ver 5, state.timer{min,renew,reset,end}, setTimer.
### 확인한 것
- Playwright: 측정 시간 칸 표시·setTimer. Actions 빌드 성공(run 44023596108).
- 시험하지 못한 것: 실기기에서 시간 끝 자동 갱신(DurWatch 시계+예비 알람 사용).
### 알려진 한계
- 측정 시간은 앱 밖 측정 회기(위젯·타일·알림창)에만 적용. 새로 만든 회기의 '관찰 시간(분)'은 자동으로 안 바뀜.
- 측정 시간 동안 시계 서비스가 계속 돎(배터리 조금).
### 코드 위치 (index.html)
- `natTimerBox`·`natTimerSet`, `nat.timer` / android: CounterStore(sesEnd·setTimer·timerLocked·modeWatching·modeNext), CounterReceiver(INC는 회기 없으면 begin, CHECK 6~8), CounterNotifier.showTimer·statusText, CounterTile.onClick
### 다음에 할 일
- 사용자: 새 apk → 측정 시간 5분·자동 갱신으로 시간 끝 알림·새 회기 생기는지 확인.

## 2026-11-17 · 음량 3번 다시 구현(회기 중만)
### 한 일
- 사용자 결정: 소리 없는 재생 방식 음량 트리거만 다시. 회기 시작은 기존 빠른 설정·위젯·알림창으로.
- 회기가 열려 있고(빈도·지속시간·지연시간) 화면이 꺼졌을 때만 소리 없는 재생을 틀고, 음량 3번 → 빈도 ＋1 / 지속시간 행동 시작·끝 / 지연시간 지시·반응. 강도·간격 기록은 '이 방법 미지원'.
- 유지: 재생·통화 중 제외, 4번째 누름 취소, 진동 알림, 진단 기록·'누를 때마다 진동(시험)'. 전원·측면 바로가기는 없음.
- 앱 밖 측정 켜기 아래 '음량 3번' 칸: '회기 중 음량 3번' 체크, '누름 세는 중/회기 열면 셈', 기록.
### 데이터
- 기기 저장 btnOn·btnTest·btnLog. 플러그인 ver 4, setButtons{on,test}.
### 확인한 것
- Playwright: 칸 문구·setButtons. Actions 빌드 성공(run 44020504468).
- 시험하지 못한 것: 실기기에서 화면 꺼짐 음량 키가 미디어 음량을 바꾸는지(이 방식의 핵심 가정[추정]).
### 알려진 한계
- 회기 중 화면이 꺼져 있으면 오디오 출력이 켜져 있음(배터리 조금). 블루투스 이어폰 연결 시 그쪽으로 무음 출력.
- 다른 앱 재생 판단은 재생 목록 수 비교[추정].
### 코드 위치 (index.html)
- `natBtnBox`·`natBtnSet`, `nat-btn-refresh`, CSS `.btn3-log`
- android: PressWatch(update·startSilent·stopSilent·volRx·busyWhy·act), CounterStore.pressOn(켬+회기 열림+방법), DurWatch.pressAlive
### 다음에 할 일
- 사용자: 새 apk → 앱 밖 측정 켜기 → '회기 중 음량 3번' 체크 → 타일로 회기 시작 → 화면 끄고 음량 3번 → 기록에 '음량 받기 켬'·'음량 · 1번째'·'→ ＋1' 확인 캡처.

