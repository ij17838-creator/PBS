# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-23 · AI 결과에 경고 문장
### 한 일
- AI가 글을 만드는 네 자리에 경고 문장 그대로 한 줄: "AI 생성 결과에는 오류나 편향이 포함될 수 있으므로 중요한 의사결정에는 참고용으로만 활용하세요."
- 자리: 중재 계획 AI 제안 막대, 가설 자료 요약 'AI로 흐름 읽기', 평가 'AI로 문단 쓰기', 최종 보고서 종합 의견 아래.
- 글씨는 '이어짐: 앞 상황과…'와 같은 .hint(옅은 회색 var(--muted)). 다크·고대비도 같은 변수 따름.
- 최종 보고서 경고는 화면에만(no-print): 인쇄·PDF·HTML 저장에는 안 나옴. 공지 2026-11-23-2(web).
### 확인한 것
- Playwright(탭 1280·휴대폰 390 다크, 예시 프로젝트): 중재 계획·가설·보고서에 보임, 인쇄 미디어에서 보고서 경고 display:none, 오류 없음. 그림: screens/AI-경고문장/.
- 평가 'AI로 문단 쓰기'는 예시 프로젝트에 중재 회기가 없어 화면 확인 못 함(같은 방식으로 넣음). 실기기 미확인.
### 알려진 한계
- '실행 기록'의 자동 실행 기록은 AI가 아니라 회기 기록 규칙으로 만들므로 넣지 않음.
### 코드 위치 (index.html)
- AI_WARN, aiWarn, aiPlanBar·flowAiView·evAutoView·repHTML, CSS .ai-warn
### 다음에 할 일
- 사용자: 앱 재시작 후 문장 위치·색 확인. 보고서 인쇄에도 넣고 싶으면 repHTML의 aiWarn('no-print')에서 no-print만 빼면 됨.

---

## 2026-11-23 · AI 모델에 Hydrogen 추가
### 한 일
- AI 설정 창에 '모델' 칩 Neon·Hydrogen 추가. 누르면 모델 이름 칸이 gemini-4-neon / gemini-4-hydrogen으로 바뀌고 바로 저장. 모델 이름 칸 직접 입력도 그대로.
- 사용자 요청으로 사고 수준 고르기(높음·보통 칩) 뺌. 늘 HIGH로 부르고 '사고 수준: 높음' 안내 한 줄. 공지 2026-11-23-1(web).
### 확인한 것
- Firebase AI Logic 문서: gemini-4-hydrogen은 thinkingLevel HIGH만 받음(MEDIUM은 400). aiCall이 400이면 사고 수준 빼고 다시 불러 모델 기본값(높음)으로 답함.
- Playwright 가짜 AI(__pbsAi): Hydrogen 고르면 pbs-ai model 저장, 예전 level MEDIUM이 있어도 HIGH로 호출됨 확인, 오류 없음. 그림: screens/AI-도우미/07-탭-AI설정-Hydrogen.png.
- 시험 못 함: 실제 Hydrogen 호출(Vertex 콘솔에서 쓸 수 있는지), 실기기.
### 알려진 한계
- 보통을 받는 모델을 직접 적어도 늘 높음으로 부름. 기본 모델은 여전히 Neon.
### 코드 위치 (index.html)
- AI_MODELS, aiCfg(level 늘 HIGH), aiDlgOpen, actions['ai-model-pick']
### 다음에 할 일
- 사용자: 앱 재시작 → AI 설정에서 Hydrogen 누르고 '연결 시험'. 안 되면 오류 문구 알려 주기.

---

## 2026-11-21 · ABC 흐름을 AI로 읽기('그 뒤 학생은?' 버튼 뺌)
### 한 일
- 사용자 의견(학생 반응이 다양해 버튼은 불명확)으로 ABC 행의 '그 뒤 학생은?' 버튼 뺌(bb4a17d). CSV 열도 되돌림.
- 자료 요약 'ABC 전체 맥락' 위에 '✦ AI로 흐름 읽기'(누를 때만). 모든 관찰지·행을 시간 순서 R1…로 보내 학생 입장 기능 제안: 기능·뚜렷/기울어짐/약함·반복된 흐름·이유·근거 행(펼치면 A→B→C)·더 볼 것·QABF와 같음 표시. '근거 칸에 넣기'로 가설 1 근거 끝에 붙임.
- 기존 규칙 집계는 '낱말로 센 참고'로 이름 바꿔 아래에 그대로 둠(QABF 일치·불일치 판정도 규칙 기준 그대로).
### 데이터
- AI 결과는 이 기기 localStorage(AI_OUT_KEY) 프로젝트별 'flow'={at,n,sig,fns,unclear,summary}. 공유 안 됨. 기록이 바뀌면 '그 뒤 기록 바뀜' 표시.
- 이미 눌러 둔 r.out은 지우지 않고 규칙 집계에서만 읽음.
### 확인한 것
- Playwright 가짜 AI(__pbsAi): 보낸 글(R 번호·상황·일화 기술·해석 구분) 확인, 결과 표시·근거 행·근거 칸 넣기 정상(탭·휴대폰). 그림: screens/ABC-전체맥락/*-4-AI흐름읽기.png.
- 시험 못 함: 실제 제미나이 응답 품질, 실기기.
### 알려진 한계
- 인터넷·로그인 필요. 행이 아주 많으면 보내는 글이 길어짐(자르지 않음).
### 코드 위치 (index.html)
- flowRows·flowFacts·flowRun·flowSig·flowAiView·flowText, actions['ai-flow']·['flow-put'], ctxView, CSS .flow-ai·.flow-rows·.flow-q
### 다음에 할 일
- 사용자: 앱 재시작 후 실제 기록으로 'AI로 흐름 읽기' 눌러 보고, 해석이 맞는지·프롬프트 고칠 점 알려 주기.

---

