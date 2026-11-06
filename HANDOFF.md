# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-06 · Firebase(Firestore)로 여러 기기 실시간 공유

### 한 일
- `FIREBASE_CONFIG`가 비면 지금과 같음(버튼 숨김, SDK 안 불러옴).
- 프로젝트 화면 '여러 기기에서 함께 쓰기': 구글 로그인, 공유 시작(코드 6자 이상), 공유 ID 복사, 표시 이름, 멤버 목록, 소유자 내보내기·코드 바꾸기.
- 홈 '공유 코드로 프로젝트 받기'(공유 ID + 코드).
- 열어 둔 공유 프로젝트만 구독, 바뀐 칸만 전송(글 칸 1.5초·칸 벗어날 때, 선택은 바로). 입력 중인 칸은 벗어난 뒤 반영.
- 충돌: 나중 저장 우선, 칸 옆 '다른 버전 n개'(버튼 3개 + '그대로 두기'), 프로젝트 화면 '확인할 충돌 n개'.
- 칸 옆 '마지막 수정: 이름 · 시각' → 수정 기록('이 버전 다시 넣기').
- 화면 위 접속 상태 표시.
- 녹음·사진·PDF는 안 올림('다른 기기에 있는 파일' 표시). 하단 안내문 한 줄 추가.
- `firestore.rules`, `docs/firebase-setup.md` 추가.

### 데이터
- `p.shared={projectId(=p.id),role:'owner'|'member',since,code}`, `store.sync[pid]={base,revs}`, `settings.userName`.
- Firestore: `projects/{pid}` 아래 `members`·`cells`(칸 1개=문서 1개, value는 JSON 문자열)·`items`·`history`·`conflicts`, `invites/{SHA-256(pid:코드)}`. 필드는 `docs/firebase-setup.md`·코드 주석 참고.
- 경로는 id 기반(`procs/abc/data/sheets[s1]/rows[w2]/b`). 복제·불러온 사본은 `shared`를 지움.

### 확인한 것
- 에뮬레이터(Firestore+Auth) + Playwright 두 브라우저 43개 통과: 공유·받기, 틀린 코드 거절, 자동 반영, 입력 중 칸 보호, 디바운스·한 칸 쓰기 4회, 오프라인→재연결 전송, 서로 추가한 항목 둘 다 남음, 충돌 버튼 3개·목록·수정 기록, 내보내기, 375px, 콘솔 오류 없음.
- 규칙 시험 33개 통과(멤버 아님·코드 모름·history 수정·사칭 → 거부).
- 설정 비었을 때: 공유 UI 숨김, SDK 요청 없음, 기존 탭 병합 그대로.
- 시험하지 못한 것: 실제 Firebase·구글 팝업 로그인, 실기기, 큰 프로젝트의 무료 한도.

### 알려진 한계
- 쓰기는 트랜잭션이라 Firestore 대기열 대신 `base`와의 차이를 남겼다가 재연결 때 보냄.
- 충돌 기록은 글자 칸만.
- 입력칸 포커스 중에는 화면 다시 그리기를 미룸(기존과 같음).
- `base`가 프로젝트 사본이라 localStorage 사용량이 늘어남.
- 관찰 세션(`abc.session`)도 공유되어 다른 기기도 진행 중으로 보임.
- 서버 쪽 프로젝트 삭제·본인 탈퇴 버튼 없음.

### 코드 위치 (index.html)
- `/* ---------- 실시간 공유 (Firebase Firestore) ---------- */`: `FIREBASE_CONFIG`, `FIREBASE_VER`, `syncFlat`·`syncUnflat`·`syncDiff`, `makeFirestoreAdapter`(어댑터, `window.__pbsSyncAdapter`로 교체 가능), `syncWatch`·`syncAbsorb`·`syncFlush`, `syncDecorate`, `syncConfView`·`syncHistView`, `syncProjectCard`·`syncConflictCard`·`syncJoinCard`, actions `sync-*`
- `render()` 끝 `syncAfterRender()`, CSS `/* 실시간 공유 */`

### 다음에 할 일
- 사용자: `docs/firebase-setup.md`대로 설정 후 `firebaseConfig` 전달 → `FIREBASE_CONFIG`에 넣어 배포 → 문서 끝 '확인 순서'로 두 기기 확인.
- 서버에 학생 자료가 올라가므로 개인정보 동의 확인.

## 2026-11-05 · 여러 사람 동시 사용·기기 간 반영 점검 (분석만, 코드 변경 없음)

### 한 일
- 저장 구조를 점검하고 사용자 질문 두 가지에 답했습니다.
- 결론: 모든 자료는 각 기기 브라우저의 localStorage(`pbs-observe:v1`)와 IndexedDB(`pbs-observe-audio`)에만 저장. 서버·로그인·네트워크 전송 없음.
  1) 여러 사람이 써도 오류는 안 남. 다만 사람마다 각자 따로 복사본을 갖는 구조라 "한 프로젝트에 함께 접속"은 아님.
  2) 다른 기기 실시간 반영은 안 됨. 같은 기기·같은 브라우저의 다른 탭끼리만 반영(2026-11-03 항목 참고).
- 다른 기기로 옮기는 방법은 백업 파일 내보내기·불러오기뿐이고, 같은 프로젝트를 불러오면 합쳐지지 않고 "(불러옴)" 사본이 생김.

### 확인한 것
- 코드 읽기만 함: `loadStore`, `saveNow`, `merge3`, `absorbRemote`, `storage` 이벤트, `importBackup`, 화면 하단 안내 문구.
- 시험하지 못한 것: 실제 두 기기 동시 사용.

### 알려진 한계
- 기기 간 공유를 하려면 서버(예: Firebase Firestore, Supabase)와 로그인이 필요. 학생 행동 자료라 접근 권한·개인정보 동의를 함께 정해야 함.
- 기존 `merge3`(3자 병합)는 서버 동기화 때도 충돌 처리에 다시 쓸 수 있음.

### 코드 위치 (index.html)
- `/* ---------- storage ---------- */`의 `LS_KEY`, `loadStore`, `saveNow`, `merge3`, `absorbRemote`, `AudioDB`, `importBackup`

### 다음에 할 일
- 사용자가 공유 기능을 원하면: 서비스 선택(Firebase/Supabase), 로그인 방식, 프로젝트별 공유 대상, 녹음·첨부 파일까지 올릴지 결정 후 구현.
- 공유 전 임시 방법이 필요하면 백업 불러오기 시 같은 프로젝트를 사본 대신 `merge3`로 합치는 기능 검토.

## 2026-11-05 · 행동평정척도: 전용 입력 화면

### 한 일
- '기본 메모' 칸 4개(척도 이름·실시일·점수·메모)를 전용 화면으로 바꿨습니다. 절차 카드의 '기본 메모' 표시는 없앴습니다.
- **척도 정보**: 척도 이름, 척도 종류(행동 문제 선별 / 기능 평가(MAS·QABF 등) / 적응행동 / 기타, 하나만) + 종류별 해석 안내, 실시 목적.
- **응답자**(여러 명): 이름·호칭, 관계, 실시일, 학생을 관찰한 기간. 행 추가·삭제(삭제 확인).
- **하위영역별 점수**(행 추가): 하위영역, 응답자(위 목록에서 선택), 원점수, 표준점수·백분위, 기준(정상·경계·위험·임상·해당 없음). 응답자 2명 이상의 점수가 있으면 하위영역 × 응답자 비교표가 자동으로 나옴.
- **표적행동과의 연결**: 결과가 표적행동에 대해 말해 주는 것, 기능 가설 단서(가설 화면과 같은 5개 기능, 여러 개), 단서가 된 문항·영역.
- **응답자 간 차이**, **해석 시 주의점 · 메모**(예전 '메모'를 여기로 합침).
- **원본 파일 · 재실시**: 사진·PDF 첨부(미리보기·저장·삭제), 재실시 예정일, 재실시 메모.
- 예시 프로젝트(첫 실행 시)에 예시 값 채움.

### 데이터
- `rating.data` 새 필드: `type, purpose, raters[{id,name,role,date,period}], rows[{id,domain,rater,raw,std,level}], target, funcs[], funcNote, diff, cautions, retestDate, retestNote, files[{id,name,mime,size,createdAt}]`.
- 예전 필드: `scale`은 그대로 척도 이름. `when`·`scores`는 값이 있을 때만 접힌 "예전에 적은 내용"에서 보이고 고칠 수 있음. `memo`는 `ensureProject`에서 `cautions`로 옮기고 비움.
- 첨부 파일은 녹음과 같은 IndexedDB(`pbs-observe-audio`, projectId 색인)에 저장. 백업 '녹음 포함'·불러오기·복제·프로젝트 삭제가 첨부 파일도 함께 처리하도록 `blobIds(p)`를 추가해 썼습니다.

### 확인한 것
- Playwright(개발용 사본에서 내부 함수 접근): 예전 형식 자료 이전, 응답자·점수 행 추가, 응답자 이름 바꾸면 점수 행 선택지 이름도 바뀜, 비교표 표시, 종류 안내, 사진 첨부·미리보기, 응답자 삭제 시 점수 행의 응답자만 비워짐, 새로고침 후 유지, 프로젝트 복제 시 첨부 파일 복사, 375px 폭에서 가로 스크롤 없음, 콘솔 오류 없음.
- 시험하지 못한 것: 실기기 S펜 손글씨(새 칸에도 자동으로 붙는 것만 확인), 백업 파일 내보내기·불러오기로 첨부 파일 왕복(코드만 고침), 큰 사진·PDF.

### 알려진 한계
- 척도는 프로젝트당 하나만 기록합니다(척도를 여러 개 쓰면 다른 척도는 메모에).
- 비교표는 하위영역 이름이 글자까지 같아야 같은 줄로 묶입니다.
- 첨부 파일 크기 제한이 없습니다. 백업 파일은 '녹음 포함'일 때만 첨부 파일이 들어갑니다(버튼 이름은 그대로).
- 가설 화면의 자료 요약에는 평정척도 결과가 아직 나오지 않습니다.

### 코드 위치 (`index.html`)
- 상수: `RATING_TYPES`, `RATING_TYPE_TIP`, `RATING_ROLES`, `RATING_LEVELS`
- 화면: `/* 3. rating */`의 `viewRating`, `raterRow`, `scoreRow`, `ratingCompare`, `rFileRow`, `hydrateRatingFiles`
- 입력: `onField`의 `rating`·`rrow`, `actions`의 `r-add`, `del-rrow`, `del-rfile`, `rfile-save`, `chip`의 `rtype`(하나)·`rfunc`(여러 개), change 처리의 `[data-rfile]`, `inkTarget`의 `rating`·`rrow`
- 백업: `blobIds`, `remapAudio`, `exportProjects`, `importBackup`, `duplicateProject`, `deleteProject`
- CSS: `.r-list`, `.r-row`, `.r-grid`, `.r-file`, `.r-thumb`, `.r-doc`

### 다음에 할 일
- 가설 화면 자료 요약에 평정척도 기능 단서 보여 주기.
- 재실시 결과를 같은 화면에 '사후' 점수로 넣어 전후 비교하기.
- 실기기에서 확인.

## 2026-11-05 · 표적행동 정의 화면: 관찰·면담 전 '초안' 흐름으로 개선

### 한 일
- 바로 아래 항목(경계 사례 규칙·관찰 계획 카드)을 이 화면 구성에 합쳤습니다. 그 항목의 필드(`boundary, episode, planWhen, planLength, planPeriod, planObserver, planTools, planIoa`)는 이름 그대로 씁니다.
- **정의 상태**(초안 → 관찰 후 수정 → 확정)를 맨 위에 추가. 상태마다 안내 한 줄. 확정 전이면 다른 절차 화면 위 표적행동 배너에 상태 태그가 붙음.
- **경계 사례 판단 규칙** 칸 추가(애매한 경우 셀지 말지).
- 해당하는 예/해당하지 않는 예 위에 "처음엔 1~2개만, 관찰하면서 보충" 안내.
- **정의의 근거** 카드: 정의의 출처(담임교사 의견·보호자 의견·의뢰서·기록·짧게 본 것·기타)+메모, 표적으로 고른 이유(본인 학습 방해·다른 학생 방해·안전 문제·또래 관계 영향·자주 나타남)+메모.
- **관찰 준비** 카드: 확인할 질문, 관찰 계획(언제·어떤 수업 / 얼마 동안 / 누가 기록 / 첫 관찰 예정일), 측정 방법(예정 1개만 선택)·방법별 안내(강도는 세기가 없는 행동에 맞지 않다는 안내 포함)·측정 단위(방법별 예시)·고른 이유·"관찰 후 확정" 체크, 관찰자 간 일치도(IOA) 체크와 메모.
- "주로 나타나는 상황"은 접힌 "관찰한 뒤에 채울 내용"으로 옮김(값은 그대로 보존).
- **정의 점검 자동 경고**: 정의 빈칸(1번), 측정 방법 미선택(2번), 경계 사례 규칙 빈칸(3번), 시작·종료 기준 빈칸(4번), 마음 상태 추측 말('화가', '짜증', '일부러' 등)(5번). 입력하는 즉시 갱신, 개수 옆에 "경고 n".
- **수정 기록** 카드: "오늘 날짜로 한 줄 추가" 버튼.
- 손글씨 버튼은 그대로 두었고, 새 글자 칸에도 자동으로 붙음.
- 예시 프로젝트(첫 실행 시 생성)에 새 칸 예시값을 채움.

### 데이터
- `define.data` 새 필드: `stage, sources[], sourceNote, priority[], priorityNote, boundary, questions, episode, planWhen, planLength, planPeriod, planObserver, planDate, planTools, planIoa, measure, unit, measureWhy, measureFixed, ioaDone, ioaNote, revLog`.
- 예전 `measures`(여러 개)는 지우지 않음. `measure`가 비어 있으면 `measures[0]`을 예정 측정 방법으로 가져옴(`ensureProject`).

### 확인한 것
- Playwright로: 새 프로젝트에서 경고 4개 표시 → 정의에 '화가' 입력 시 5번 경고 → 측정 방법 단일 선택·안내·단위 예시 바뀜 → 상태 칩·출처 칩·IOA 체크·수정 기록 버튼 동작 → 저장 후 새로고침해도 유지. 콘솔 오류 없음.
- 실제 갤럭시 탭 + S펜으로는 시험하지 못함(새 칸의 손글씨 포함).

### 알려진 한계
- 측정 방법은 1개만 고를 수 있음(예전에 2개 고른 자료는 첫 번째만 선택된 것으로 보임).
- 마음 상태 단어 경고는 단어 목록(`MIND_WORDS`)에 있는 말만 잡음.
- 정의 상태 '확정'과 절차 '완료'는 따로 움직임.

### 코드 위치 (`index.html`)
- 상수: `DEF_STAGES`, `DEF_STAGE_TIP`, `DEF_SOURCES`, `DEF_PRIORITY`, `MEASURE_GUIDE`, `MIND_WORDS`
- 화면: `/* 1. define */`의 `viewDefine`, `defWarnings`, `refreshDefine`, `measureTip`
- 입력: `onField`의 `define`·`defbool`·`defcheck`, `actions.chip`의 `defsrc/defpri`(여러 개)·`measure/defstage`(하나), `actions['def-revlog']`
- CSS: `.def-warn`, `.measure-tip`, `.stage-tag`, `.three`, `details.card.later`

### 다음에 할 일
- 관찰 계획·측정 방법·단위를 ABC 관찰/기초선 화면에서 불러 쓰기.
- 실기기에서 확인.
