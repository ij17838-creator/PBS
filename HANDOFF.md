# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-28 · AI 도우미에 클로드(Opus·Sonnet) 추가
### 한 일
- AI 설정 모델 칩에 Claude Opus(claude-opus-5-5)·Claude Sonnet(claude-sonnet-5-5) 추가(사용자 요청으로 하이쿠는 뺌). 클로드를 고르면 'Claude API 키' 칸(가림)이 나오고, 키를 넣으면 중재 제안·실행 기록·평가 문장·흐름 읽기·보고서가 모두 클로드로 감
- 클로드는 Firebase를 거치지 않고 브라우저에서 Anthropic API로 바로 부름(공식 SDK 1.8.0을 jsDelivr에서 불러옴, 로그인 없어도 키만 있으면 됨). 사고 늘 켜짐·effort high, 안전 거절 시 서버 대체 모델(fallbacks 'default'). 공지 2026-11-28-1(web). 앱 재시작만
### 데이터
- 키는 이 기기 localStorage 'pbs-ai-ckey'에만(서버·계정 동기화 없음, 기기마다 따로 넣기). 모델은 기존 'pbs-ai'.model
### 확인한 것
- Anthropic API의 CORS 사전 요청(브라우저 직접 호출 헤더·authorization 허용, allow-origin *) curl로 확인
- Playwright(가짜 API 응답): 틀린 키 → '키가 맞지 않습니다', 맞는 키 → 연결됨(Opus·Sonnet), 칩 바꿔도 키 유지, 제미나이 고르면 키 칸 없음, 페이지 오류 없음. 화면 screens/AI-클로드/
- 시험 못 함: 실제 키로 실제 호출(키 없음), jsDelivr SDK 실제 로딩(작업 환경에서 막힘, 같은 버전 로컬 번들로 대신 시험), 탭·폰 앱
### 알려진 한계
- 키가 기기 브라우저 저장소에 그대로 있음(공용 기기 주의). 요금은 사용자의 Anthropic 콘솔 크레딧(구글 크레딧과 별개)
- SDK 1.x는 키를 Authorization: Bearer로 보냄(정상)
### 코드 위치 (index.html)
- AI_MODELS·AI_CKEY_KEY·ANTHROPIC_SDK·aiIsClaude·aiCKey·aiCKeySet·aiClaudeSdk·aiClaudeErr·aiClaude, aiCall 첫 줄 분기, aiAvail·aiReady, aiDlgOpen·aiDlgSave(#ai-ckey), CSS .ai-box input[type=password]
### 다음에 할 일
- 사용자: console.anthropic.com에서 키 만들기 → AI 설정에서 Claude Opus 고르고 키 넣고 '연결 시험'

---

## 2026-11-27 · PC 입력칸 마우스 커서 잘 보이게
### 한 일
- PC에서 입력칸에 마우스를 올리면 커서가 안 보이던 문제 → 입력칸·글상자 위 커서를 흰 테두리+검은 심의 I 모양(처음 20×30, 지금 11×16)으로 바꿈. 밝은·다크·고대비·유리·색 일렁임 어느 배경에서나 보임
- 마우스 쓰는 기기에서만 적용(@media hover:hover·pointer:fine), 탭·폰 터치는 그대로. 공지 2026-11-27-5(web). 앱 재시작만
### 확인한 것
- Playwright: input·textarea 계산 cursor가 새 그림으로 잡히고 그림 정상 로드(20x30), 페이지 오류 없음. 배경 6종 위 모양 확인(screens/입력칸-커서/새-커서-배경별.png) / 실제 PC 마우스로는 못 봄(헤드리스는 커서가 안 찍힘)
### 알려진 한계
- 날짜·시각·select 칸은 '고르는 창'이라 손가락 모양 그대로. Windows '마우스 포인터 크기' 설정은 이 커서에 적용 안 됨
- 추가: 사용자가 PC에서 너무 크다고 해 20×30 → 입력 글자(16px) 높이에 맞춰 11×16(흰 테두리 3.2·심 1.6 viewBox 기준, 핫스팟 5 8, 공지 2026-11-27-6)
### 코드 위치 (index.html)
- CSS 주석 'PC 마우스: 입력칸 위 글자 커서' (input:focus 규칙 바로 위)
### 다음에 할 일
- 사용자: PC 브라우저 새로고침 뒤 밝은·다크·고대비에서 입력칸에 마우스 올려 보기

---

## 2026-11-27 · 고대비에서도 앱 색(A안: 글씨 검정)
### 한 일
- 사용자가 시안 A안 고름 → 고대비를 켜도 앱 색 고르기 가능. 버튼·강조·선택만 앱 색을 아주 진하게(흰 바탕 대비 7 이상), 바탕 흰색·글씨 검정 그대로. 밝기 막대는 고대비에서 계속 잠김
- 설정 힌트 '고대비: 버튼만 진하게', 도움말 'set:hc' 문구, 공지 2026-11-27-4(web). 앱 재시작만
### 확인한 것
- Playwright(다크 기기): 고대비+파랑 → accent #0d2aa0·글씨 #000·바탕 #FFF, 고대비+기본색 → 예전 고대비 그대로, 고대비 끔 → 예전 앱 색. 오류 없음 / 실기기 못 함
### 코드 위치 (index.html)
- hcPal·hueApply·HUE_VARS, SET_ROWS id hue hint, hueMore 슬라이더 disabled 제거
### 다음에 할 일
- 사용자: 탭에서 고대비 켜고 앱 색 바꿔 보기, 새 apk(ver 16) 위젯 고리 확인

---

