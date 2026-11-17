# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

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

## 2026-11-14 · 지속시간 측정: 끝 확인·늦은 시작 보정·에피소드 목록
### 한 일
- 한 버튼 토글: 위젯·알림·타일의 같은 버튼으로 행동 시작/끝(기존 그대로). 전원·음량·이어폰 버튼은 쓰지 않음(원래 기능과 충돌 방지).
- 끝 확인: 행동 진행 중 기본 30초마다 "아직 행동 중인가요?"(계속·행동 끝). 앱이 꺼져 있어도 동작.
  - 응답 없을 때: 다시 묻고, 응답 없는 확인이 n번(기본 3) 쌓인 뒤 다음 간격에 '마지막 확인 시각'(시작·계속·보정 누른 때)으로 자동 끝 + "자동으로 끝냈어요" 알림.
  - 설정(기초선·중재 '앱 밖 측정' 줄의 '끝 확인', 지속시간·켜짐일 때만): 간격 15초~5분, 알림 방식 진동/소리/소리+진동/무음 알림/끄기, 응답 없을 때 계속 재기·n번 뒤 자동 끝, 보정 버튼 보이기.
- 늦은 시작 보정: 진행 중 알림 −10초·−30초, 위젯 −10초·−30초·−1분. 앞 행동 끝보다 앞으로는 안 감. (보정 켜짐이면 진행 중 알림에 '회기 끝'이 빠짐: 버튼 3개 제한)
- 에피소드 목록: 지속시간 회기마다 접는 칸 "에피소드 n회 · 총 · 평균". 시작·끝 시각 고치기(24시간 칸)·지우기·+ 에피소드, 고치면 측정값 재계산. 자동 끝은 빨갛게 '확인 필요'로 펼쳐짐.
### 데이터
- durLog 칸 뒤 표시: '/a'(자동 끝), '/b'(시작 당김). 시각을 손으로 고치면 해당 표시가 빠짐. 예전 자료 그대로 읽힘.
- 앱 밖 기록 이벤트 추가: 'adj'(s=새 시작), 'de'의 f:'a'. 기기 설정은 앱(SharedPreferences: chkSec·chkMode·chkMiss·adjOn)에 저장.
### 확인한 것
- Playwright+가짜 플러그인: 시작·보정·끝·자동 끝 이벤트 → durLog·측정값(분/회 평균) 맞음, 시각 고치기·끝<시작 거부·추가·지우기, 설정 바꾸기→setCheck 값 전달, 콘솔 오류 없음. 휴대폰 폭 화면 확인.
- GitHub Actions 빌드 성공(브랜치 run 22): 자바 코드 컴파일됨.
- 시험하지 못한 것: 실기기 전부(알림 간격 정확도, 화면 꺼짐·잠금에서 알림·진동·소리, 포그라운드 서비스 시작, 타일에서 시작할 때 끝 확인, 위젯 2×2에 보정 줄이 들어가는지).
### 알려진 한계
- 끝 확인 시계는 포그라운드 서비스(specialUse) + 예비 알람. 기기·절전 설정에 따라 늦을 수 있음[추정]. 앱 화면(브라우저)만으로는 끝 확인 없음.
- 웹 화면이 새 앱보다 먼저 업데이트되면 '끝 확인' 칸은 숨겨짐(ver<2).
### 코드 위치 (index.html)
- `durParse`·`durStr`·`durEd`·`durEdit`·`durAdd`·`durDel`·`durApply`·`natChkBox`·`natChkSet`·`CHK_MODES`, `natDrain`(adj·f), CSS `.dur-ed`·`.dur-ep`·`.nat-chk`
- android: `DurWatch`(새), `CounterStore`(confirm·durEnd·adjust·checkTick·setCheck·nextCheck), `CounterNotifier`(build·showCheck·showAutoEnd·checkChannel), `CounterReceiver`(CONFIRM·DUR_END·CHECK·ADJ10/30/60), `PbsCounterPlugin.setCheck`, widget_counter.xml `w_adj`, 매니페스트 FGS 권한
### 다음에 할 일
- 사용자: 새 apk 설치(네이티브 변경) → 지속시간 프로젝트에서 앱 밖 측정 켜기 → 행동 시작 후 30초 기다려 확인 알림, 계속/무응답 2분 뒤 자동 끝, −30초 보정, 앱에서 에피소드 시각 고치기.
- 다섯 방법 기능 완성 후 모션 안내 일괄 제작.

## 2026-11-14 · 빈도 움직이는 안내 만들었다가 되돌림
### 한 일
- 측정 방법 '빈도' 움직이는 안내(정의·기초선·중재 화면)를 만들었으나 사용자 요청으로 되돌림(커밋 9827b60을 revert). 지금 앱은 그 전과 같음.
- 사용자 결정: 측정 방법별 기능을 먼저 만든 뒤, 모션 안내는 다섯 방법을 한 번에 만든다.
### 확인한 것
- 되돌린 뒤 index.html이 788b73c와 같음.
### 다음에 할 일
- 방법별 기능 완성 후 모션 안내 작업 시 9827b60 참고 가능(SVG SMIL 8초 반복 + 단계 3줄 강조, `MDEMO`·`measureDemo`). 미리보기 GIF: /mnt/project-files/screens/빈도-측정-안내.gif

