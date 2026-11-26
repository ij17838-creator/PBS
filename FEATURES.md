# 기능 목록

한 줄 = 기능 · 주요 함수/상수(index.html) · 연결된 기능. 필요한 줄만 찾아 읽고(grep), 바꾼 줄만 고친다. 줄 번호는 적지 않는다(자주 바뀜).

## 바탕
- 저장(기기) · loadStore·saveNow·scheduleSave·ensureProject·defaultData·LS_KEY · 모든 절차, 공유, 여러 탭 병합
- 여러 탭 병합 · merge3·absorbRemote·applyRemoteView · 저장
- 화면 그리기·이동 · render·go·navPush·pbsBack·renderCrumbs·actions[...]·onField · 모든 화면, 뒤로 버튼(MainActivity)
- 절차·카테고리 정의 · PROCS·PROC·CATS·activeProcs·catProcs · 홈, 측정 방법 고르기, 찾기, 도움말
- 측정 방법 고르기(p.mprocs) · MPROCS·mprocsMissing·newMOrder·mAddBox·actions 'new-m'·'m-add-*' · 새 프로젝트, 측정 카테고리
- 예시 프로젝트 · exampleProject · 홈, 로그아웃 화면
- 백업·내보내기 · exportProjects·importBackup·exportCSV·duplicateProject·deleteProject · 녹음 파일, 공유(삭제)

## 화면
- 홈·프로젝트 카드 · viewHome·homeFold·projCard·viewProject·viewCat·procCard · 계정별 목록, 공유 카드, 공지 점
- 절차 한 장씩(묶음) · pgBuild·pgGo·pgPaint·pgAll·PG_* · 모든 절차, 찾기, 도움말 'pg:'
- 찾기 · findIndex·findRun·findOpen·findGo · 묶음 나누기
- 고르는 창(select·날짜·시각) · pkOpen·pkOpenSel·pkOpenDate·pkOpenTime·pkKey · 모든 입력 칸 자동
- 상태·완료·저장 버튼 · statusInfo·setDone·saveSend·doneBar·refreshStatus · 공유 보내기
- 위 막대 상태 신호 · sigUI·sigOpen·sigSave·sigSync·sigServer · 입력 모드, 저장, 공유
- 설정 창 · SET_ROWS·setRow·setOpen·setCtx·setRefresh · 거의 모든 켜고 끄기 옵션, 설정 계정 저장, 도움말 'set:'
- 설정 계정 저장 · PREF_KEYS·prefPush·prefLoad·prefMerge·prefApply·natPrefApply · 설정 창, 로그인, 앱 밖 측정 옵션
- 유리·고대비·앱 색 · GLASS_KEY·HC_KEY·lookApply·huePal·hueApply·hueSave·HUE_PRE · 설정, 계정 저장
- 도움말 ⓘ · HELP·hpBtn·hpOpen·hpProc · 설정, 절차, 묶음
- 첫날 안내 · GD_KEY·gdSteps·gdPaint·gdAuto · 회기 시작 준비하기, 새 계정
- 공지 · NOTICES·noticeUnseen·noticeOpen·noticeHome·noticeInit · 위 막대, 홈, apk 버전(PbsCounterPlugin ver)
- 개인정보 안내 · privInner·privOpen·PRIV_CONSENT · 설정 맨 아래
- 손글씨(S펜) · inkOpen·inkSave·inkBind·inkScan·inkPenDetected·INK_SEL · 모든 글 칸, 공유(.ink), 입력 모드
- 입력 모드·보며 쓰기 창 · inputModeSet·inputModeUI·kbOpen·kbApply · 손글씨, 상태 신호
- 이어서 하기 · resumeGet·resumeSet·resumeBoot · 손글씨, 화면 이동

## 절차
- 학생 기본 정보 · viewProfile·pfRef·PF_* · 가설·중재 계획 요약 카드
- 표적행동 정의 · viewDefine·defWarnings·MEASURES·MEASURE_GUIDE·measureTip · 기초선·중재(측정 단위), 회기 시작 줄
- 기초선 · viewBaseline·blStats·blJudge·blChart·sesRow·sesSection·BL_* · 정의, 중재, 평가, 앱 밖 측정, 함께 ＋1
- 회기 안 기록(지속·지연·강도·간격) · durEdit·latEd·lvlEd·ivEd·measureOpts·IV_*·LAT_MAXS · 기초선·중재, 앱 밖 측정
- QABF · viewQabf·qabfScores·qCompare·QABF_* · 가설
- 행동평정척도 · viewRating·ratingCompare·RATING_* · 가설
- ABC 관찰 · viewAbc·sheetView·rowView·startSession·endSession·rollover·planAutoStart · 녹음, 낱말 집계, 흐름 읽기, 간편 측정
- ABC 낱말 집계·창 · tally·kwOf·summaryView·kwExOpen·kwDlgOpen·KW_* · ABC, 가설
- ABC 흐름(맥락) · abcContext·ctxView·CTX_FN·C_FUNC · 가설, AI 흐름 읽기
- 가설 · viewHypo·hyBlock·hyAnalysis·hypoSentence·QABF_FUNC · QABF, ABC, 평정, 중재 계획
- 중재 계획 · viewPlan·aiPlanRun·AI_PLAN_KEYS · 가설, AI
- 중재 실행 · viewIntervene·ivAutoView · 기초선과 같은 측정, 평가
- 평가 · viewEvaluate·blCompare·evSentence·EV_DECS · 기초선·중재 값(PND 등), AI
- 녹음·받아쓰기 · startRecording·stopRecording·attachAudio·toggleDictation · ABC, 파일(기기에만)

## 회기 측정 보조
- 학생별 회기 시작 방법(p.startWay) · SW_WAYS·swStrip·swGoStart·swSet·swPrepList·swRecWay·swExtras · 측정 화면 요약, 정의 한 줄, 첫날 안내, 앱 밖 측정, 작은 ＋1 창
- 간편 측정(앱) · qmStart·qmFinish·qmEvent·qmCard·qmZone·QM_* · 위젯·떠 있는 아이콘·음량, ABC
- 간편 측정(PC 단축키) · qmPcStart·qmPcInc·qkDown·QPC_KEY·QK_KEY · 작은 ＋1 창
- 작은 ＋1 창(PC) · miniShow·miniInc·miniTrig·padBind · 간편 측정 PC, 함께 ＋1
- 휴대폰 회기 ↔ PC 함께 ＋1 · liveHostTick·liveWatch·liveInc·liveShow·liveArmRow·LIVE_* · 공유(~live), 작은 ＋1 창, 앱 회기
- 앱 밖 측정(설치 앱) · natInit·natToggle·natTargetRow·natSync·natDrain·NAT_MODES·PCN · 안드로이드 PbsCounterPlugin·CounterStore, 설정 '앱 밖 측정'
- 앱 밖 측정 고르기 창 · npOpen·npList·npDraw·NP_PH · 앱 밖 측정
- 회기 시작 모양(떠 있는 아이콘) · SHP·shpMore·shpPick·shpDrawn·shpMatch·shpNorm·shpApply·pbs-nat-shp · 설정 '떠 있는 아이콘', 안드로이드 FloatBubble.shapeOk·setShape, PbsCounterPlugin.setBubShape
- 화면 자동 업데이트(설치 앱) · webCheck·webApply·webInit · 안드로이드 WebUpdater

## 서버 공유(Firebase)
- 연결·어댑터 · FIREBASE_CONFIG·SYNC_ON·syncAdapter·makeFirestoreAdapter · 모든 공유 기능
- 로그인(구글·이메일) · syncGoogleLogin·syncLoginDlg·syncOnAuth·acctOnAuth·PAUTH · 계정별 목록, 설정 저장, 앱 색
- 계정별 목록 · acctGate·projVisible·visibleProjects · 홈, 예시
- 계정 합치기 · acctMergeDlg·acctMerge·acctLinks · 로그인
- 받기·구독 · syncWatch·syncSubscribe·syncAbsorb·syncAutoPull·syncMyAdd · 저장, 화면
- 보내기 · syncDiff·syncFlush·syncPending·SYNC_SKIP_TOP·NO_HIST·INK_PATH · 저장 버튼, 손글씨, 상태 신호
- 다른 버전·수정 기록 · syncDecorate·syncConfView·syncHistView·syncSetValue · 보내기
- 저장 위치 표시 · svDecorate·SV_STUCK · 회기·관찰지, 상태 신호
- 협력자 접속 · presTick·presSend·presUI·PRES_* · 위 막대 왼쪽 아래 버튼
- 이메일 가리기 · emMask·emUI · 계정 카드
- 규칙 · firestore.rules(저장소·project-files/firebase) · 받기·보내기·삭제(ownerPurging)

## AI 도우미
- 호출·설정 · aiCall·aiSdk·aiCfg·AI_BACKEND·AI_MODELS·APPCHECK_SITE_KEY · 아래 모든 AI
- 쓰임 · aiPlanRun(중재 계획)·ivAutoView(실행 기록)·aiEvRun(평가 문장)·flowRun(ABC 흐름 읽기)·aiRepRun·repHTML(최종 보고서) · 각 절차

## 안드로이드(android/app/src/main/java/.../pbs)
- PbsCounterPlugin(ver) · 웹↔앱 다리 · 앱 밖 측정, 공지 need
- CounterStore·CounterReceiver·CounterNotifier·CounterWidget·CounterTile · 알림 ＋1·위젯·빠른 설정 · 앱 밖 측정
- PressWatch(음량)·FloatBubble(떠 있는 아이콘)·DurWatch(지속시간) · 회기 시작·기록 · 회기 시작 방법
- LiveSync · 앱 회기를 서버 ~live로 · 함께 ＋1
- PbsAuthPlugin · 앱 구글 로그인 · 로그인
- WebUpdater · 화면 자동 업데이트 / MainActivity · 뒤로 버튼(pbsBack) / BootReceiver
