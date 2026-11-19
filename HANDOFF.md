# 인수인계

작업이 끝나거나 대화(스레드)가 끝날 때마다 맨 위에 새 항목을 추가합니다.

---

## 2026-11-19 · 앱 안에서 계정 합치기(구글↔이메일)
### 한 일
- 첫 화면 계정 카드(로그인 상태)에 '계정 합치기' → 창에서 '구글 계정 합치기' 또는 이메일·비밀번호 입력.
- 그 로그인 방법이 아직 계정이 아니면: 지금 계정에 로그인 방법으로 붙임(linkWithCredential/linkWithPopup, 앱은 PbsAuth) → 이후 어느 방법으로 들어와도 같은 uid.
- 이미 따로 있는 계정이면(부드러운 합치기): 두 번째 Firebase 앱으로 그 계정에 로그인해 공유 목록을 읽고, 서로의 공유 프로젝트에 저장된 공유 코드로 멤버 가입, 양쪽 users/{uid}/links에 서로 표시. 기기에서는 연결된 uid의 프로젝트도 보임. 지금 로그인은 그대로.
- 로그인 때 links를 받아 와 반영, 합친 계정으로만 보이는 공유 프로젝트는 기기에 남은 공유 코드로 이 계정도 멤버 가입. 공지 k:'web'.
### 데이터
- Firestore users/{uid}/links/{다른uid}. 규칙을 users/{uid}/{document=**} 본인만으로 넓힘 → 사용자가 다시 게시해야 함.
- localStorage 'pbs-acct-links' {uid:[연결 uid]}. projVisible이 acctIds(내 uid+연결 uid)로 판단.
### 확인한 것
- Playwright(가짜 어댑터): 구글 계정의 로컬·공유 프로젝트 → 네이버 이메일 로그인 시 예시만 → 합치기 후 모두 보임, 서버 목록·멤버·links 양쪽 기록, 새 기기 네이버 로그인 시 공유 프로젝트 자동 받기, 새 이메일 붙이기 메시지.
- 시험 못 함: 실제 Firebase(linkWithPopup·credentialFromError·두 번째 앱), 설치형 앱 구글 합치기.
### 알려진 한계
- 두 계정이 하나의 uid가 되지는 않음(Firebase는 이미 있는 두 계정을 못 합침). 다른 계정이 소유자인 공유 프로젝트의 삭제·코드 바꾸기·멤버 내보내기는 그 계정으로 로그인해야 함.
- 공유 코드가 바뀐 프로젝트는 못 옮김(공유 ID·코드로 받기 안내). 합치기 취소 기능 없음.
### 코드 위치 (index.html)
- 어댑터 linkOther·myLinks·addLink, ACCT_LINKS_KEY·acctLinks·acctLinksSet·acctIds·projVisible, acctMergeDlg·acctMerge, syncAutoPull(links·연결 계정 가입), syncAcctInner, actions acct-merge·acct-merge-google / firestore.rules
### 다음에 할 일
- 사용자: 새 firestore.rules 게시 → 팀원이 원래 계정(구글 등)으로 공유를 받았는지 확인 → 네이버로 로그인해 계정 합치기에서 구글 계정 합치기.

## 2026-11-19 · 로그인하면 공유 프로젝트 자동 받기
### 한 일
- 공유 시작·공유 받기·코드 바꾸기 때 서버의 '내 공유 프로젝트 목록'에 기록. 다른 기기에서 같은 계정으로 로그인하면 이 기기에 없는 공유 프로젝트를 코드 없이 받아 옴(멤버일 때만). 토스트 '공유 프로젝트 n개를 받았습니다'.
- 이 기능 전에 받은 공유 프로젝트는 그 기기에서 로그인할 때 목록에 올림. 소유자가 지웠거나 내보낸 프로젝트는 목록에서 뺌. 이 기기에서 공유 프로젝트를 지우면 목록에서도 빠짐(다른 기기 로그인 때 다시 안 내려옴). 공지 k:'web'.
### 데이터
- Firestore users/{uid}/projects/{pid} = {role, code, at}. firestore.rules에 본인만 읽기·쓰기 규칙 추가 → 사용자가 콘솔에 다시 게시해야 함(안 하면 자동 받기만 조용히 안 됨).
### 확인한 것
- Playwright(가짜 어댑터, 기기 3개): 기기1 받기 → 목록 기록, 기기2 같은 계정 로그인 → 자동으로 내려옴, 지워진 프로젝트는 목록에서 빠짐, 다른 계정 기기3엔 안 내려옴.
- 시험 못 함: 실제 Firebase 규칙·쿼리, 이 기기에서 지울 때 목록에서 빠지는 것.
### 코드 위치 (index.html)
- 어댑터 myList·myAdd·myDel·fetchShared, syncMyAdd·syncMyDel·syncBuildShared·syncAutoPull, syncOnAuth, actions sync-join·sync-start, deleteProject / firestore.rules, docs/firebase-setup.md 6
### 다음에 할 일
- 사용자: Firebase 콘솔 → Firestore → 규칙에 firestore.rules 전체 붙여 넣고 게시. 그 뒤 기존 기기에서 한 번 로그인(목록 올리기) → 다른 기기 로그인해 내려오는지 확인.

## 2026-11-19 · 공유 받기: 다른 계정이 받아 둔 기기에서도 받아지게
### 한 일
- 버그: 같은 기기에 다른 계정으로 받아 둔(로그아웃 숨김으로 안 보이는) 공유 프로젝트가 있으면, 공유 ID·코드로 받기 → '이미 이 기기에 있는 공유 프로젝트' 후 숨김이라 첫 화면으로 튕김. 이제 서버에서 코드 확인 뒤 이 계정에도 보이게 함. 공지 k:'web'.
### 데이터
- p.users = 이 기기에서 같은 공유 프로젝트를 받은 다른 계정 uid 목록(기기에만, SYNC_SKIP_TOP). projVisible이 owner 또는 users로 판단.
### 확인한 것
- Playwright(가짜 어댑터): A 받기 → 로그아웃 → B 받기 → B 목록에 보임, C에게는 안 보임.
- 시험 못 함: 실제 Firebase, 팀원 휴대폰. 팀원 제보가 이 경우인지(코드를 넣었는지, 무슨 문구가 떴는지) 확인 안 됨.
### 알려진 한계
- 다른 기기에서 같은 계정으로 로그인해도 공유 프로젝트가 저절로 내려오지 않음(기기마다 공유 ID·코드 입력). 자동 받기는 Firestore 규칙 변경이 필요해 사용자 결정 대기.
### 코드 위치 (index.html)
- projVisible, actions sync-join, SYNC_SKIP_TOP
### 다음에 할 일
- 사용자 결정에 따라 '로그인하면 내 공유 프로젝트 자동 받기'(users/{uid} 문서 + 규칙) 구현.

## 2026-11-19 · 설치형 앱: 로그아웃하면 앱 밖 측정도 막기
### 한 일
- 앱에서 로그아웃하면: 누른 기록을 먼저 넣고(natDrain), 숨긴 프로젝트를 향한 앱 밖 측정 대상(위젯·빠른 설정·알림창·음량 버튼·떠 있는 ＋1)을 끄고, 열린 알림창 회기·간편 측정을 끝냄. 함께 세기 서버 쓰기(LiveSync)도 끔(원래 동작).
- 로그아웃 상태에서는 위젯 대상 자동 맞추기(natFollow)도 쉼. 예시 프로젝트 대상은 그대로. 공지 k:'web' → 새 apk 필요 없음(앱 재시작).
- 앱 화면의 목록 숨김은 2026-11-19 '로그아웃하면 예시 프로젝트만' 항목 그대로(웹 공통).
### 확인한 것
- Playwright(가짜 PbsCounter·어댑터): 대상·알림 회기 켠 채 로그아웃 → clearTarget·setLive off·stop 호출, 대상 비워짐.
- 시험 못 함: 실기기(위젯·음량·떠 있는 판이 실제로 꺼지는지).
### 알려진 한계
- 다시 로그인해도 앱 밖 측정은 자동으로 다시 켜지지 않음(화면에서 다시 켜기).
### 코드 위치 (index.html)
- natLockHidden, natDrain(끝에서 호출), natFollow, syncOnAuth
### 다음에 할 일
- 사용자: 앱 재시작(공지에 이 항목이 보이는지) → 위젯 켠 채 로그아웃해 꺼지는지 확인.

