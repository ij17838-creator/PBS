# 새 주소 pbs-record.web.app 만들기 (2026-11-23)

main에 index.html이 올라가면 GitHub Actions '웹 주소(Firebase) 배포'가 `https://pbs-record.web.app`에 자동으로 올립니다.
옛 주소(`ij17838-creator.github.io/PBS`)도 그대로 둡니다. 지금 깔린 설치형 앱이 거기서 화면을 받기 때문입니다.
아래 1~4는 한 번만 하면 됩니다. 모두 Firebase 프로젝트 `pbs-project-10874`에서 합니다.

## 1. 사이트 이름 잡기
1. Firebase 콘솔 → 왼쪽 **빌드 → Hosting** → (처음이면 **시작하기**를 누르고 안내는 '다음'으로 넘김).
2. Hosting 화면 아래쪽 **다른 사이트 추가**(Add another site) → `pbs-record` 입력 → 추가.
   - "이미 사용 중"이면 다른 이름을 정해 Claude에게 알려 주세요(firebase.json의 site 값을 바꿉니다).

## 2. 로그인 허용
1. **Authentication → 설정 → 승인된 도메인 → 도메인 추가** → `pbs-record.web.app` 추가.

## 3. AI 도우미 허용(App Check 키)
1. Google Cloud 콘솔(console.cloud.google.com, 프로젝트 pbs-project-10874) → 검색창에 **reCAPTCHA** → 키 목록에서 앱에 쓰는 키(6LfI…로 시작) 누르기 → **키 수정**.
2. **도메인** 목록에 `pbs-record.web.app` 추가 → 저장.

## 4. 자동 배포 열쇠 넣기
1. Google Cloud 콘솔 → **IAM 및 관리자 → 서비스 계정 → 서비스 계정 만들기**.
   - 이름: `github-deploy` → 만들고 계속.
   - 역할 추가: **Firebase Hosting 관리자**, **API 키 뷰어** → 완료.
2. 만든 계정 누르기 → **키** 탭 → **키 추가 → 새 키 만들기 → JSON** → 파일이 내려받아집니다.
   - 이 파일은 비밀번호와 같습니다. 다른 곳에 올리거나 보내지 마세요.
3. GitHub 저장소 → **Settings → Secrets and variables → Actions → New repository secret**.
   - Name: `FIREBASE_SERVICE_ACCOUNT`
   - Secret: 내려받은 JSON 파일을 메모장으로 열어 전체를 복사해 붙여 넣기 → Add secret.
4. 저장소 **Actions** 탭 → **웹 주소(Firebase) 배포** → **Run workflow**. 초록 체크가 뜨면 `https://pbs-record.web.app`이 열립니다.
5. 다 했으면 JSON 파일은 지워도 됩니다.

## 알아 둘 것
- 설치형 앱은 다음에 새 apk를 낼 때 화면 받는 주소를 새 주소로 바꿉니다(그 전까지는 옛 주소에서 받음). 앱 자료는 그대로입니다.
- 브라우저로 쓰던 경우 기기 안 자료는 옛 주소에 남습니다. 로그인하면 서버 자료는 다시 받고, 손글씨는 옛 주소에서 '전체 백업 내보내기' → 새 주소에서 '백업 불러오기'로 옮깁니다.
