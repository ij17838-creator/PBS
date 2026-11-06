# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-06 · 공유: 입력 '완료' 전에는 서버로 보내지 않기 (사용량 줄이기)

### 한 일
- 글 칸에 키보드로 쓰는 중이거나 손글씨 칸이 열려 있으면 서버로 보내지 않음. 1.5초 자동 전송을 없앰.
- 보내는 때: 칸 벗어나기, 손글씨 '완료', 한 줄 칸에서 Enter(키보드 '완료'), 키보드를 내림(화면 높이가 다시 커짐). 선택·체크·버튼은 지금처럼 바로.
- 입력 중 보낼 변경이 있으면 화면 위에 '실시간 연결됨 · 입력 완료 후 보냄'. 이 기기 저장(store)은 그대로 바로 됨.

### 확인한 것
- 에뮬레이터 두 브라우저 시험 46개 통과(키보드 5초 넘게 멈춰도 0회 → 벗어나면 1회, Enter로 1회, 손글씨 획 3개 0회 → 완료 1회 포함).
- 시험하지 못한 것: 실기기에서 키보드 내림 감지(`visualViewport`), 실제 Firebase.

### 알려진 한계
- 칸에서 칸으로 옮겨 가며 계속 쓰면(다른 글 칸으로 바로 이동) 글 칸을 모두 벗어날 때까지 모아 두었다가 보냄.
- 한 번 보낼 때 수정 시각 칸(`updatedAt`) 2개도 함께 씀.

### 코드 위치 (index.html)
- `syncEditing`, `syncFlush(force)`, `syncFlushSoon(ms,force)`, `syncStatusUI`

### 다음에 할 일
- 사용자: 갤럭시 탭에서 키보드 '완료'·손글씨 '완료' 뒤에 다른 기기에 반영되는지 확인.

## 2026-11-06 · Firebase 설정값 넣어 공유 기능 켬

### 한 일
- `FIREBASE_CONFIG`에 사용자 프로젝트(`pbs-project-10874`) 값을 넣음. 이제 배포 화면에 공유 카드·받기 카드가 보임. Analytics(`measurementId`)는 넣지 않음.

### 확인한 것
- API 키 유효(이미지의 l/I 구분을 실제 요청으로 확인), Google 로그인 사용 설정됨, Firestore 데이터베이스 있음(로그인 없이 읽기 거부됨).
- 설정값 넣은 화면에서 공유·받기 카드 표시, 페이지 오류 없음(SDK는 로컬 파일로 대신).
- 시험하지 못한 것: Firestore 위치(사용자가 위치를 못 고르고 넘어갔다고 함), `firestore.rules` 게시 여부, 승인된 도메인 등록 여부, 실제 로그인·두 기기 공유.

### 알려진 한계
- 위치가 서울이 아니어도 동작은 같음(조금 느릴 수 있음). 위치는 바꿀 수 없어서 바꾸려면 Firebase 프로젝트를 새로 만들어야 함.

### 코드 위치 (index.html)
- `FIREBASE_CONFIG`

### 다음에 할 일
- 사용자: 콘솔에서 Firestore 위치 확인, 규칙 탭에 `firestore.rules` 게시, 승인된 도메인에 `ij17838-creator.github.io` 추가 후 `docs/firebase-setup.md` 끝 '확인 순서'로 두 기기 확인.

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

