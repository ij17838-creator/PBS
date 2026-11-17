# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-17 · 휴대폰 회기를 PC 회기별 기록에 바로 넣기
### 한 일
- 사용자 보고: 새 apk로 실시간 카운팅은 확인, 그러나 PC 대기 창은 계속 '대기 중'·횟수 미반영('휴대폰 소식 아직 없음'). 앱에서 측정을 시작하면 PC 회기별 기록에 회기가 바로 생기고 횟수도 실시간 반영되길 원함.
- PC·칠판: 휴대폰 주인 문서를 받으면 같은 회기(id bsn+시작ms, natId new:ms, liveMade)를 회기별 기록에 만들고 횟수를 따라감 → 공유로 서버에도 올라감. 기초선/중재는 제목의 '중재'로 판단(liveTitlePh).
- 휴대폰: 앱 밖 측정 새 회기도 같은 id로 만듦(natResolve, natOwn). 칠판이 먼저 만든 회기를 받으면 한 번 0부터 다시 셈. 열린 빈도 회기 값은 앱 횟수(r.count)를 기준으로 맞춤(겹쳐 세기 방지).
- 대기 창 서버 읽기 오류를 '휴대폰 소식 못 읽음(이유)'로 표시. 공지 k:'web'.
### 확인한 것
- Playwright(가짜 주인 문서·가짜 앱): PC에 회기 생김(2→4 따라감), 휴대폰은 같은 회기에 4회로 맞음. 이전 시험 통과.
- 시험 못 함: 실기기. '휴대폰 소식 아직 없음' 원인 미확인(휴대폰 상태 줄 문구 필요).
### 코드 위치 (index.html)
- liveSes·liveSesId·liveTitlePh, natResolve, natDrain(liveMade 처리·r.count), liveWatch(werr)·liveSt
### 다음에 할 일
- 사용자: 휴대폰 기초선 화면 '칠판 함께 세기: …' 문구와 PC '＋1 창 대기' 옆 문구 알려 주기.

## 2026-11-17 · 앱이 꺼져 있어도 칠판·PC와 함께 세기(LiveSync, 새 apk)
### 한 일
- 원인: 빠른 설정으로 회기를 열면 앱 화면(JS)이 꺼져 있거나 멈춰 칠판으로 아무것도 안 보냄.
- 안드로이드가 직접 처리: 공유 프로젝트(빈도) 회기가 열려 있는 동안 포그라운드 서비스 LiveSync가 2초마다 '~live/host' 쓰기 + '~live/tap/*' 읽어 칠판 ＋1을 CounterStore에 넣음(앱이 열리면 natDrain이 회기 기록으로). 회기 중 알림 '칠판·PC와 함께 세는 중'.
- 서버는 Firestore REST(documents:commit, runQuery) + 구글 로그인 토큰. 앱 화면이 setLive로 로그인 정보·대상 프로젝트(로컬 id→공유 id)를 넘기고, 토큰은 refresh 토큰으로 새로 받음.
- 웹: nat.ver>=10이면 liveHostTick 안 씀. 휴대폰 상태 줄은 nat.live(앱 상태) 기준.
- 플러그인 ver 10, 공지 k:'apk' need 10.
### 데이터
- SharedPreferences lvOn·lvKey·lvProj·lvUid·lvName·lvDev·lvMap·lvRt·lvTok·lvExp·lvHost·lvSeen·lvSig·lvBeat·lvErr·lvOk. 회기 id는 key 세 번째 칸(new:시작ms 또는 회기 id).
### 확인한 것
- 웹 Playwright(옛 앱 ver 9 경로) 통과. GitHub Actions 빌드(아래 '다음에 할 일' 전 확인).
- 시험 못 함: REST 요청 실제 서버 응답(에뮬레이터 실행이 이 환경에서 막힘), 실기기 전체.
### 알려진 한계
- 칠판(안드로이드 앱)에서 누른 ＋1은 여전히 그 기기 앱 화면(JS)이 보냄.
- 서버 읽기: 회기 중 2초마다 1~2건(40분 약 2,400건).
### 코드 위치
- android LiveSync(config·want·sync·step·pushHost·sendEnd·pollTaps·writeCell·token), CounterStore.liveTap, CounterNotifier.refreshAll, PbsCounterPlugin.setLive·state.live
- index.html liveNative, adapter liveCred, liveHostSt
### 다음에 할 일
- 사용자: 새 apk 덮어 설치 → 공유 프로젝트로 빠른 설정 회기 시작 → 휴대폰 상태 줄('칠판 함께 세기 중' 또는 '못 보냄(이유)')과 PC 대기 창 확인.

## 2026-11-17 · 대기 창에 지금 횟수·연결 상태 줄
### 한 일
- 사용자 보고: 회기 끝·앱 ＋1이 브라우저에 반영 안 됨(원인 미확인). 대기 창에 횟수 표시 요청.
- 작은 창(PiP) 위에 큰 숫자 'n회'(대기 중엔 —), 창 높이 늘림.
- 휴대폰 앱 '앱 밖 측정' 줄 위에 상태 한 줄: 로그인 필요 / 빈도만 됨 / 앱 밖 측정 회기 시작 때 / 못 보냄(이유) / 함께 세기 중 · 시각 보냄.
- PC '＋1 창 대기' 줄에 '휴대폰 소식 아직 없음 / 휴대폰 회기 중 · 시각 소식 / 회기 끝 · 시각'.
- 공지 k:'web'.
### 확인한 것
- Playwright(가짜): 휴대폰 ＋2 → 작은 창 2회, 상태 줄 두 쪽 모두 표시. 이전 시험 통과.
- 시험 못 함: 실기기. 실제 미반영 원인은 상태 줄 결과로 판단해야 함.
### 알려진 한계
- 함께 세기는 앱 밖 측정(위젯·타일·알림창)으로 연 회기만. 앱 화면 안 ＋만 누른 회기는 칠판에 안 뜸.
### 코드 위치 (index.html)
- liveHostSt·liveHostStUI(.live-host-st), liveSt(.live-st), miniCount(mini-cnt)
### 다음에 할 일
- 사용자: 두 상태 줄 문구를 알려 주기 → 그에 따라 원인 고치기(백그라운드 멈춤이면 새 apk 방식 검토).

