# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-17 · 버튼 3번(전원·음량)으로 측정 시작
### 한 일
- 앱 밖 측정 켜기 아래 '버튼 3번' 칸: '3번 눌러 시작'·'음량 버튼도 세기'(기본 켬). 꺼져 있는 게 기본.
- 화면이 꺼진 채 전원·음량을 섞어 3번 빠르게 누르면(사이 1.2초·전체 3초 안) 1.2초 뒤 기록. 그 사이 4번째 누름 = 취소.
- 동작: 회기 없으면 회기 시작(어제 자동 회기는 닫고 새로). 회기 있으면 빈도 ＋1, 지속시간 행동 시작/끝, 지연시간 지시/반응. 강도·간격 기록은 회기 시작만.
- 진동: 길게 1번=시작, 짧게 1번=＋1, 길게 2번=끝, 짧게 2번=취소, 짧게 3번=기록 안 함. 진동 없는 기기는 짧은 소리.
### 데이터
- 기기 저장 btnOn·btnVol. 앱 기록 형식은 그대로(기존 begin·add·durToggle·latToggle 이벤트).
- 플러그인 ver 4, state에 btn{on,vol}, 새 메서드 setButtons. 새 apk 필요(옛 앱이면 칸 안 보임).
### 확인한 것
- Playwright: 설정 칸 표시, 체크 → setButtons 호출, 요약 문구 바뀜.
- GitHub Actions 빌드 성공(브랜치 run 44003690366).
- 시험하지 못한 것: 실기기 전부(화면 꺼짐 신호 지연, 음량 버튼이 화면 꺼짐에 오는지, 진동 세기).
### 알려진 한계
- 전원: 앱이 직접 못 받아 화면 켜짐/꺼짐 횟수로 셈. 갤럭시 '측면 버튼 > 두 번 누르기'(카메라) 켜져 있으면 안 됨. 화면 자동 꺼짐·뒤집개 커버도 1번으로 셀 수 있음.
- 음량: 화면 꺼졌을 때만 원격 음량 미디어 세션으로 받음[추정, 기기별]. 그동안 다른 앱 음악 음량·이어폰 재생 버튼이 안 먹을 수 있음.
- 켜 두면 측정 알림이 있는 동안 서비스가 계속 돎(배터리). 절전으로 꺼지면 안 셈[추정] → 배터리 '제한 없음' 권장.
- 갤럭시 긴급 SOS(측면 5번)와 겹치지 않게 5번 이상 누르지 말 것.
### 코드 위치 (index.html)
- `natBtnBox`·`natBtnSet`, `nat.btn`·`nat.btnOpen`, change 처리 `[data-btn3]`
- android: PressWatch(press·volPress·commit·act·feedback·updateSession), DurWatch(sync·schedule에 pressOn), CounterStore(btnOn·btnVol·pressOn·setBtn), PbsCounterPlugin.setButtons
### 다음에 할 일
- 사용자: 새 apk 설치 → 측면 버튼 두 번 누르기 끄기 → 화면 끈 채 전원 3번·음량 3번·섞어서 각 10번 시도해 성공 횟수, 4번째 취소, 주머니 속 오작동 여부.
- 결과에 따라 간격(GAP·SPAN·GRACE) 조정, 음량이 안 되는 기기면 전원만 안내.

## 2026-11-17 · 지연시간·강도·간격 기록 측정 기능
### 한 일
- 방법별 최적 방식 선정(점수표 /70): 지연시간=정한 지시만 '지시→반응' 시각 찍기+최대 대기 자동 마감(59), 강도=1건마다 단계 버튼(57), 간격=순간 표집 2분+진동 신호+정각 맞춤(54). 문서: 프로젝트 파일 발표/지연시간·강도·간격기록-측정방식-설계.md
- 정의 화면: 측정 방법을 고르면 설정 칸(지연시간 신호·반응 기준·최대 대기·회기당 지시 수 / 강도 단계 기준 3줄·평균·최고 / 간격 방식·길이·신호 방식·정각 맞춤).
- 회기 줄: 지연시간 '지시·반응 시작·중단' 버튼(경과 초, 최대 넘으면 자동)+시도 목록, 강도 '1 약·2 중·3 강'+건별 목록(단계·시각 고치기), 간격 칸 표('칸 만들기'=시작 시각·관찰 시간으로, 누르면 있음→없음→빈칸). 측정값 자동 계산.
- 앱 밖 측정(새 apk): 세 방법도 위젯·알림창·타일. 지연시간 지시/반응·중단, 강도 1·2·3·되돌리기, 간격 관찰 시작→신호 알림 있음/없음(다음 신호까지 답), 10분 넘게 무응답이면 회기 자동 닫힘. 옛 앱이면 '새 앱 설치 필요'.
### 데이터
- 회기: latLog(durLog 꼴, /m 최대·/x 중단 값 제외), lvlLog('시:분:초=단계'), ivLog('시:분:초=1|0|?'). 정의: latCue·latResp·latMax·latPlan·lvl1~3·lvlAgg·ivType·ivSec·ivAlign·ivAlert.
- 앱 밖 이벤트: ls·le(f m/x), lv(v)·lu, iv(v). 플러그인 ver 3, setTarget에 latMax·ivSec·ivAlign·ivMode.
### 확인한 것
- Playwright(브라우저): 세 방법 버튼·목록·값(지연 16.5초, 강도 2.5, 간격 67%), 칸 만들기 정각 맞춤, 콘솔 오류 없음.
- 가짜 플러그인: 이벤트 → 새 회기·값 맞음, 정의의 최대 대기 바꾸면 setTarget 다시 보냄, 방법 바꾸면 위젯 따라감.
- GitHub Actions 빌드 성공(브랜치 run 26): 자바 코드 컴파일됨.
- 시험하지 못한 것: 실기기 전부(신호 진동 정확도·절전, 알림 버튼 표시, 위젯 크기).
### 알려진 한계
- 간격 신호는 포그라운드 서비스+예비 알람이라 기기 따라 늦을 수 있음[추정]. 회기 중 간격 길이를 바꾸면 다음 신호부터 적용.
- 강도 타일은 회기 시작만(회기 중엔 앱 열기). 브라우저에선 신호 없음(벽시계로).
### 코드 위치 (index.html)
- `measureOpts`, `measEds`, `latEd`·`latAct`·`latEdit`·`latTick`·`latValue`, `lvlEd`·`lvlAct`·`lvlEdit`·`lvlValue`, `ivEd`·`ivAct`·`ivSlots`·`ivValue`, `edApply`·`edRedraw`, `NAT_MODES`·`natTargetArgs`·`natOptsPush`·`natNeedsApp`, natDrain(ls·le·lv·lu·iv), CSS `.meas-ed`·`.lvl-row`·`.iv-grid`·`.iv-cell`
- android: CounterStore(latToggle·latStop·lvlAdd·lvlUndo·ivAnswer·ivTickLocked·firstSignal), CounterReceiver(LAT·LAT_X·LV1~3·LV_UNDO·IV_YES·IV_NO), CounterNotifier(showLatMax·showIvSignal·showIvAutoEnd·alertChannel), CounterWidget·widget_counter.xml `w_lvl`, CounterTile
### 다음에 할 일
- 사용자: 새 apk 설치 → 간격 기록 프로젝트로 2분 신호 10회 받아 늦은 횟수, 알림창 1·2·3 버튼 표시, 지연시간 지시→60초 자동 마감 확인.
- 학생 정해지면 설계 문서 '확인할 질문'으로 재채점. 다섯 방법 모션 안내 일괄 제작.

## 2026-11-14 · 위젯이 정의의 측정 방법을 따라감
### 한 일
- 표적행동 정의에서 측정 방법을 바꾸면 위젯·알림·타일도 빈도↔지속시간으로 자동 전환(토스트 "위젯: 지속시간으로 바뀜"). 앱 밖 측정을 다시 켤 필요 없음.
- 위젯이 못 하는 방법(지연시간·강도·간격 기록)으로 바꾸면 위젯을 잠시 끄고("이 방법은 위젯 미지원") 기억했다가, 빈도·지속시간으로 돌아오면 다시 켬.
- 바뀌는 순간 열려 있던 앱 밖 회기는 닫힘(기존 setTarget 동작).
### 데이터
- 기기 localStorage `pbs-nat-follow`(잠시 끈 대상 {pid, ph}). 직접 켜기/끄기를 누르면 지움.
### 확인한 것
- Playwright+가짜 플러그인: 지속시간→빈도→지연시간(꺼짐·기억)→지속시간(다시 켬), 정의 화면 칩으로 바꾸기, 콘솔 오류 없음.
- 시험하지 못한 것: 실기기. index.html만 바뀌어 재설치 불필요(화면 자동 업데이트).
### 알려진 한계
- ABC 대상은 해당 없음. 공유로 다른 기기에서 바뀐 경우는 앱을 열거나 앞으로 가져올 때 맞춤.
### 코드 위치 (index.html)
- `natFollow`, `natTitle`, `NAT_FOLLOW_KEY`·`natFollowGet`·`natFollowSet`, `natDrain` 끝, 동작 'chip'(g==='measure')
### 다음에 할 일
- 바로 아래 항목의 실기기 확인과 같이 보기.

