# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-20 · AI 도우미(제미나이) · 실행 기록 자동 · 평가 문장 · 최종 보고서
### 한 일
- 멘토 'API KEY 사용 요소' 반영. 앱에 키를 넣지 않고 Firebase AI Logic(Blaze 요금제, 사용자가 선택)으로 제미나이를 부름. 기본 모델 gemini-4-neon, 사고 수준 높음/보통(xhigh 안 씀, Neon이 보통을 거절하면 사고 칸 빼고 다시 부름). 로그인한 사람만 사용.
- 중재 계획: 'AI 제안 받기' → 선행·대체·후속·위기 제안 카드, 고른 것만 '칸에 넣기'. 가설·기초선/목표 줄을 위에 보여 줌.
- 중재 실행: 회기 기록으로 '자동 실행 기록' 목록. 수기 칸 이름은 '프로그램 밖 기록 (수기)'(묶음 띠 이름은 '실행 기록').
- 평가: 수치 근거 변화 문장(평균·범위·%·PND 판정·추세·목표) 규칙으로 자동, AI 보고서 문단은 선택. 둘 다 '아래 칸에 넣기' 후 고쳐 저장.
- 프로젝트 화면 '최종 보고서' 카드: AI 설정, 보고서 보기(1~8절 + 종합 의견 AI·직접 수정) → 인쇄(앱은 HTML 저장). 공지 '2026-11-20-2' k:'web'(앱 재시작만).
- AI 연결 제공자 한 줄 전환: index.html `AI_BACKEND`('google'=Gemini Developer API, 'vertex'=Vertex AI, Google Cloud 크레딧 쪽). 콘솔 AI Logic에서 고른 제공자와 같게.
### 데이터
- 저장 자료: plan 칸·evaluate change·intervene log에 고른 글만 들어감. AI 결과는 기기에만 localStorage 'pbs-ai-out', 설정 'pbs-ai'.
- 보내는 내용: 학생 특성(장애·약·건강·가족 제외)·표적행동·기능평가·가설·기초선/중재 수치. 학생 코드·이름은 안 보냄(화면 '보낼 내용 보기'로 확인 가능).
### 확인한 것
- Playwright 1280·390, SDK 12.19.0 실제 모듈 + 가짜 응답: 요청 주소 firebasevertexai.googleapis.com/.../gemini-4-neon, thinkingLevel HIGH·JSON·system 지시 들어감, 학생 코드 안 나감, 칸 넣기·자동 기록·PND 문장·보고서·인쇄 PDF 통과. 화면 /mnt/project-files/screens/AI-도우미/.
- 시험 못 한 것: 실제 Gemini 호출, App Check(reCAPTCHA Enterprise) 토큰, 안드로이드 webview(https://localhost)에서의 App Check, 크레딧이 Gemini Developer API에 적용되는지, Vertex 쪽 실제 호출.
### 한계
- APPCHECK_SITE_KEY 넣음(reCAPTCHA Enterprise 6LfI…). 키 없이 부르면 'Firebase App Check token is invalid'(401)로 거절됨을 실제 호출로 확인 = AI Logic 켜짐·App Check 강제 중. 이 작업 환경은 gstatic·google.com(reCAPTCHA)이 막혀 키를 넣은 실제 호출은 못 해 봄 → 기기에서 AI 설정 '연결 시험'으로 확인.
- 모델 이름이 바뀌면 AI 설정에서 고치기.
### 다음
- 사용자: 결제 계정 바꾸기 → AI Logic 시작(제공자 선택) → App Check 웹 앱 등록, 사이트 키 전달(도메인 ij17838-creator.github.io, localhost).
- 키 받으면 APPCHECK_SITE_KEY 넣기, Vertex 고르면 AI_BACKEND='vertex'.
- 정리 목록(Claude Doc) 중 AI 외 항목(순서 바꾸기·작은 묶음 합치기)은 아직.

---

## 2026-11-20 · 절차 화면 한 장씩 나눠 보기 + 찾기
### 한 일
- 멘토 피드백(정보 줄이기·잘게 나누기) 반영. 절차 화면에서 카드(묶음) 1개씩만 보임. 제목 아래 묶음 띠(쓴 묶음은 초록 점, 눌러 바로 이동), 아래 고정 막대 '‹ 앞 묶음 | n/전체 · 전체 보기 | 다음 묶음 ›'. 마지막 장에 '이 절차 완료' 칸과 '다음 절차' 버튼.
- 입력 칸 없는 작은 카드(안내·측정 방법 표시·녹음 카드·접힌 '나중에 채울 내용')는 앞(맨 앞이면 다음) 묶음에 붙임. 묶음을 지우거나 순서를 바꾸지는 않음(사용자: 아직 하지 말 것).
- QABF 문항은 5문항씩 5장. '전체 보기'는 기기마다 기억, 예전처럼 한 화면 + 띠 누르면 그 카드로 내려감. 절차마다 마지막 본 묶음 기억.
- 맨 위 '찾기'(프로젝트·절차 화면, 단축키 / 또는 Ctrl+K): 절차·묶음·칸 이름과 '내가 쓴 내용'을 찾고, 누르면 그 절차의 그 장으로 가서 칸을 노랗게 반짝. 휴대폰은 전체 화면 창. 공지 k:'web'(앱 재시작만).
### 데이터
- 저장 자료 변화 없음. localStorage 'pbs-pg-all'(전체 보기), 'pbs-pg-pos'(프로젝트/절차별 마지막 묶음 이름).
### 확인한 것
- Playwright 1280·390·1920px: 10개 절차 묶음 수, 이전·다음·띠 이동, 입력값 유지, 마지막 장 완료 칸, 묶음 기억, 전체 보기 전환, QABF 6~10 표시·응답 뒤 그대로, / 로 찾기·Esc·Enter 이동(종료 기준→행동 정의 장), QABF 문항 찾기, 찾기 전후 저장 자료 같음, 손글씨 모드 전환 뒤 넘기기 오류 없음.
- 시험 못 함: 실제 갤럭시 탭·폰·전자칠판 터치, 설치형 앱, 공유 프로젝트 충돌 칸 이동(pgReveal 넣음), 회기 측정 중 넘기기.
### 알려진 한계
- 묶음 나누기는 카드 높이로 작은 카드를 판단해 휴대폰에서 일부 다름(중재 실행 '그래프'가 비어 있으면 앞 묶음에 붙음).
- 줄이 늘어나는 목록(회기 기록·하위영역 점수·ABC 관찰지)과 휴대폰의 긴 카드는 한 장 안에서 스크롤이 남음.
- 찾기의 '내가 쓴 내용'은 글자 칸만(손글씨 그림·녹음·회기 숫자 제외).
### 코드 위치 (index.html)
- PG_ALL_KEY·PG_POS_KEY·PG_SMALL·QABF_PER·PG_NAMES·PG_MEMO·pg, pgSmall·pgTitle·pgBuild·pgPaint·pgGo·pgReveal, MutationObserver(main), procBody(viewProc가 사용), find·findIndex·findRun·findOpen·findClose·findGo·FIND_SVG, render의 pgBuild·#find-btn, actions pg-go·pg-all·find-*, CSS .pg-strip·.pg-pill·.pg-nav·.pg-mid·.find*, #find, NOTICES 2026-11-20-1
### 다음에 할 일
- 사용자: 앱 재시작 후 탭·폰·전자칠판에서 넘기기와 찾기 확인, 묶음 정리(지우기·순서 바꾸기) 여부 결정.

## 2026-11-19 · 공유 프로젝트 협력자 접속 표시
### 한 일
- 공유 프로젝트를 열면 왼쪽 아래에 '협력자 n' 버튼(다른 사람이 접속해 있을 때만). 누르면 사람별로 기기마다 '어느 화면 · 하는 일'(보는 중/쓰는 중/기초선·중재 회기 측정 중/간편 측정/함께 세기/ABC 녹음 중), 기기 종류, 다른 창 여부, 마지막 소식.
- 본인(이 기기, 같은 계정의 다른 기기, 합친 계정)은 안 보임. 같은 사람 여러 기기는 한 이름 아래 묶음.
- 이름표 대신 버튼으로 한 이유: 휴대폰에서 화면을 덜 가림(사용자가 두 방식 다 괜찮다고 함). 공지 k:'web'(앱 재시작만).
### 데이터
- 서버 cells '~pres/<기기>' {on, where, doing, away, dk, t}. 바뀌면 바로, 그대로면 45초마다 덮어씀. 프로젝트 자료로 합치지 않음(LIVE_RE를 ~live|~pres로 넓힘). 규칙 변경 없음(cells 규칙 by==본인).
- 끊김: 다른 프로젝트·목록으로 이동, 로그아웃, 창 닫기(pagehide)에 on:false. 못 보내도 150초 소식 없으면 숨김.
### 확인한 것
- Playwright 가짜 어댑터 3창(교사A PC 2대, 보조B 휴대폰 390px): 서로 1명 표시, 같은 계정 다른 기기 숨김, A 두 기기 묶음, 칸에 들어가면 '쓰는 중', 목록으로 가면 그 기기 빠짐, 시계 200초 뒤 사라짐, 창 열고 닫기.
- 시험 못 함: 실제 Firestore(어댑터 watch의 h.pres 경로), 설치형 앱 백그라운드, 실제 회기 측정 중 표시.
### 알려진 한계
- 접속 칸은 서버에서 지울 수 없어(cells delete 금지) 기기마다 문서 1개가 남음. 창을 갑자기 닫으면 최대 2분 30초 더 보임. 기기 시계가 크게 틀리면 처음 표시가 늦을 수 있음.
### 코드 위치 (index.html)
- PRES·PRES_RE·PRES_BEAT·PRES_STALE·pres, presDk·presAct·presSend·presLeave·presTick·presSoon·presGot·presPeople·presUI, 어댑터 watch의 h.pres, syncSubscribe pres, sync-logout, CSS .pres·.pres-btn·.pres-pop·.pres-list, #pres
### 다음에 할 일
- 사용자: 두 계정으로 같은 공유 프로젝트를 열어 왼쪽 아래 '협력자' 버튼 확인.
