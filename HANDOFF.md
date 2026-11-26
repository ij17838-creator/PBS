# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-26 · 떠 있는 아이콘 회기 시작 모양 고르기
### 한 일
- 설정 → 앱 밖 측정 → 떠 있는 아이콘 아래에 '시작 모양' 고르기: ○(기본)·✓·Z·∧·내가 그린 모양. 기본값은 그대로 ○.
- 내가 그린 모양: 설정의 작은 판에 3번 그려 등록('다시 등록'으로 지움). 등록 뒤·다른 모양일 때는 같은 판에 그려 맞는지 시험.
- 앱: 대기 중 아이콘·판 안내·알림 글자에 고른 모양 표시, 고른 모양일 때만 회기 시작. 공지 2026-11-26-2(apk, need 15). 새 apk 필요(플러그인 ver 15).
### 데이터
- 계정 설정 pbs-nat-shp={k, pts(내가 그린 본보기 3개, 정규화 좌표 문자열)}. 앱 SharedPreferences bubShape·bubTpl·bubMark.
### 확인한 것
- Playwright(가짜 플러그인): 모양 고르기→setBubShape 값, ✓ 판에 ✓는 맞음·Z는 아님, 내가 그린 세모 3번 등록 후 세모 맞음·Z 아님, 콘솔 오류 없음.
- node 모의 시험: ✓·Z·∧를 흔들어(±15°, 25% 늘림) 그리면 ✓·∧ 100%, Z 90% 맞음, 다른 모양은 0%.
- 브랜치 push로 안드로이드 빌드(Actions) 확인.
- 못 한 것: 갤럭시 탭·폰 실기기에서 판에 그려 시작, ver 14 앱에서 '새 앱 설치 필요' 표시.
### 알려진 한계
- 획 방향은 정·역 둘 다 받지만, 여러 획 모양은 긋는 순서가 등록 때와 같아야 함. 크게 기울여 그리면 안 맞을 수 있음(SHP_TOL 0.18).
- 웹 shpNorm·shpDist와 FloatBubble.shpNorm·shpDist는 같은 계산이라 함께 고쳐야 함.
### 코드 위치 (index.html)
- SHP·SHP_TOL·shp·shpMk·shpPts·shpNorm·shpDist·shpCircle·shpMatch·shpApply·shpPick·shpDrawn·shpReset·shpMore·shpInk, SET_ROWS bub(more), natPrefVal/natPrefSeed/natPrefApply 'shp', CSS .shp-box·.shp-pad
- android: FloatBubble.shape·mark·setShape·shapeOk·shpNorm·shpDist, PbsCounterPlugin.setBubShape(ver 15)
### 다음에 할 일
- 사용자: 새 apk 덮어 설치 → 설정에서 시작 모양 바꿔 아이콘 판에 그려 회기 시작 확인. 잘 안 맞으면 SHP_TOL 조정.
- 새 기본 모양은 SHP에 한 줄 더하면 됨(새 apk 없이).

---

## 2026-11-26 · 인수인계 줄이기·기능 목록(FEATURES.md)
### 한 일
- docs/측정설계-지침.md 새로 만듦(프로젝트 지침 6항을 그대로 옮김). FEATURES.md 새로 만듦: 한 줄에 '기능 · 주요 함수/상수 · 연결된 기능'(줄 번호 없음). 앱 화면 변화 없음, 공지 없음.
- HANDOFF.md에서 가장 오래된 항목(2026-11-25 작게 띄워 세기 켜고 끄기)을 HANDOFF-ARCHIVE.md 맨 위로 옮김(최근 3개 유지).
### 확인한 것
- FEATURES.md의 함수·상수 이름이 index.html·android·firestore.rules에 모두 있는지 grep으로 확인.
### 알려진 한계
- FEATURES.md는 손으로 관리: 기능을 더하거나 이름을 바꾸면 그 줄만 고쳐야 함.
- 편의성 13가지 항목은 사용자 허락을 받아 1,500자 안으로 줄임(빠진 세부는 git 기록 045f5d5 참고).
### 다음에 할 일
- 사용자: 새 프로젝트 지침(6항은 docs/측정설계-지침.md로 옮김, FEATURES.md 두 줄) 붙여 넣기.

---

## 2026-11-26 · 사용 편의성 제안 13가지 적용
### 한 일
- 홈: 내 목록 맨 위, 새 프로젝트·공유 코드는 접음.
- 공지: 새 기기는 모두 읽음, 최근 10개만 펼침(나머지 '지난 공지 n개').
- 회기 시작 고르기: 기기별 묶음+'· 추천'. 이름 통일: 앱 '떠 있는 아이콘', PC '작은 ＋1 창'.
- '추천 설정 한 번에' → 회기 시작 '준비하기'에 합침. 첫날 안내 2단계.
- 회기 화면: 작은 ＋1 창·앱 밖 측정 줄을 회기 시작 줄 바로 아래로.
- 측정 화면: 회기 시작 요약 한 줄, 다른 방법 학생은 간편 측정 접음, PC엔 '휴대폰 회기 작은 창' 버튼.
- 정의 '측정 방법' 아래 '회기 시작: 고르기/바꾸기' 한 줄.
- 설정 '앱 색' 접음, '작은 ＋1 창'은 묻지 않고 바로 띄움. 공지 2026-11-26-1(web). 앱 재시작만.
### 데이터
- pbs-mini-on·pbs-live-auto는 더 안 읽음(PREF_KEYS에서 뺌).
### 확인한 것
- Playwright(PC·휴대폰, 로그인 흉내): 위 화면 확인, 콘솔 오류 없음.
- 못 한 것: 설치 앱(PCN) 화면·준비하기 목록, 실제 로그인·공유, 갤럭시 실기기.
### 알려진 한계
- 설정 창의 간편 측정 세부 옵션은 다른 방법 학생에게도 펼쳐 보임('보조' 표시).
### 코드 위치 (index.html)
- viewHome·homeFold, noticeUnseen·noticeOpen, swStrip·swRecWay·swExtras·swPrepList, qmZone, def-sw·sw-cat-open, SET_ROWS hue·mini, gdSteps
- 지운 것: cover·coverSet·miniOn·miniAsk·MINI_ON·liveAuto·liveGesture·recList·gd-rec
### 다음에 할 일
- 사용자: 탭·폰 앱에서 회기 시작 고르기·준비하기·떠 있는 아이콘 이름 확인, PC에서 작은 ＋1 창 바로 뜨는지 확인.

---

