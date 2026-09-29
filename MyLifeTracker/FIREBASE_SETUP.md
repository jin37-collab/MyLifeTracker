# Firebase 클라우드 동기화 설정

이 버전은 **이메일/비밀번호 로그인 + Firebase Realtime Database**로 데이터를 저장합니다.
한 번 설정한 뒤 APK를 다시 빌드하면, 앱을 삭제했다가 재설치하거나 다른 Android 기기에서 같은 계정으로 로그인해도 기존 데이터를 불러올 수 있습니다.

## 1. Firebase 프로젝트 만들기

1. https://console.firebase.google.com/ 에 로그인합니다.
2. **프로젝트 추가**를 눌러 새 프로젝트를 만듭니다.
3. Google Analytics는 이 개인 앱에서는 꺼도 됩니다.

## 2. 이메일/비밀번호 로그인 켜기

Firebase Console에서:

1. **Build → Authentication**
2. **Get started**
3. **Sign-in method**
4. **Email/Password** 선택
5. Email/Password를 **Enable** 후 저장

## 3. Realtime Database 만들기

1. **Build → Realtime Database**
2. **Create Database**
3. 사용할 수 있는 가까운 리전을 선택
4. 데이터베이스 생성
5. 상단에 표시되는 Database URL을 복사합니다.

예시 형식:

```text
https://YOUR-PROJECT-default-rtdb.asia-southeast1.firebasedatabase.app
```

프로젝트마다 주소가 다르므로 예시를 그대로 쓰면 안 됩니다.

## 4. 보안 규칙 설정

Realtime Database의 **Rules** 탭에서 기존 내용을 아래 규칙으로 바꾸고 Publish 합니다.

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid"
      }
    }
  }
}
```

이 규칙은 로그인한 사용자가 자기 UID 아래의 데이터만 읽고 쓸 수 있게 합니다.

## 5. Web API Key 확인

Firebase Console에서:

1. 왼쪽 위 톱니바퀴 → **Project settings**
2. **General**
3. **Web API Key** 값을 복사

## 6. GitHub Secrets 등록

GitHub의 `jin37-collab/MyLifeTracker` 저장소에서:

1. **Settings**
2. **Secrets and variables → Actions**
3. **New repository secret**
4. 아래 두 개를 각각 추가

### 첫 번째

Name:

```text
FIREBASE_API_KEY
```

Secret: Firebase의 **Web API Key**

### 두 번째

Name:

```text
FIREBASE_DATABASE_URL
```

Secret: 위에서 복사한 **Realtime Database URL**

끝의 `/`는 있어도 되고 없어도 됩니다.

## 7. APK 다시 빌드

Secrets를 저장한 뒤 GitHub의 **Actions → Build Android APK → Run workflow**를 실행합니다.

빌드가 성공하면 Artifacts의 `MyLifeTracker-debug-apk`를 내려받아 `app-debug.apk`를 설치합니다.

## 8. 앱에서 로그인

1. 앱의 **홈** 오른쪽 위 사람 모양 아이콘을 누릅니다.
2. 처음이면 이메일과 비밀번호를 입력하고 **새 계정 만들기**를 누릅니다.
3. 기존 계정이면 **로그인**을 누릅니다.

### 기존 휴대폰 데이터가 있는 상태에서 새 계정을 만드는 경우

현재 휴대폰의 할 일/일정/소비/수입/카드 설정/수입 목표가 그 계정의 첫 클라우드 데이터로 업로드됩니다.

### 앱 재설치 또는 다른 기기에서 로그인하는 경우

클라우드에 기존 데이터가 있으면 로그인 직후 그 데이터를 내려받아 기기 데이터를 복원합니다.

## 보안 메모

Firebase API Key 자체만으로 데이터에 접근할 수 있게 만들면 안 됩니다. 이 앱은 Realtime Database 규칙에서 `auth.uid`를 검사해 사용자별 접근을 제한하는 전제로 설계되어 있습니다. 따라서 위 Rules 설정을 반드시 적용하세요.

## 추가 권장: APK 서명키도 고정하기

클라우드 설정과 별개로, 앱 업데이트를 계속 덮어쓰기 설치하려면 같은 Android 서명키를 계속 사용해야 합니다.
`GITHUB_APK_GUIDE.md`의 **안정적인 APK 서명 설정**도 함께 적용하는 것을 권장합니다.
