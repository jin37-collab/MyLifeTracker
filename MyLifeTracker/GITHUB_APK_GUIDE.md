# GitHub에서 APK 만들기 (v1.1)

현재 저장소는 루트 아래 실제 Android 프로젝트가 `MyLifeTracker/` 폴더에 있는 구조를 기준으로 합니다.

```text
repository root
├─ .github/workflows/build-apk.yml
└─ MyLifeTracker/
   ├─ app/
   ├─ gradle/
   ├─ gradlew
   └─ ...
```

## 1. 파일 업데이트

새 ZIP의 `.github`와 `MyLifeTracker` 내용을 현재 저장소의 같은 위치에 반영하고 Commit합니다.

## 2. 안정적인 APK 서명 설정 — 권장

GitHub의 임시 실행기에서 기본 debug keystore를 매번 새로 만들면 빌드마다 서명이 달라져 기존 앱 위에 업데이트 설치가 안 될 수 있습니다.
이 버전의 workflow는 아래 4개 Secret이 있으면 항상 같은 개인 서명키를 사용합니다.

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

ChatGPT가 별도로 제공한 `MyLifeTracker_signing_private.zip`의 `SIGNING_VALUES.txt`에 값이 있습니다.

**중요:** `mylifetracker.jks`와 `SIGNING_VALUES.txt`는 GitHub 코드에 업로드하지 말고 개인적으로 보관하세요. GitHub에는 값만 **Settings → Secrets and variables → Actions**에서 Secret으로 등록합니다.

한 번 이 키로 설치한 버전부터는 앞으로 같은 Secrets를 유지해야 기존 앱 위에 계속 업데이트할 수 있습니다.

## 3. Firebase 동기화 설정

`FIREBASE_SETUP.md`를 따라 아래 Secret도 등록합니다.

- `FIREBASE_API_KEY`
- `FIREBASE_DATABASE_URL`

Firebase 값을 넣지 않아도 앱/로컬 저장/위젯은 동작하지만 로그인·클라우드 동기화는 사용할 수 없습니다.

## 4. APK 빌드

1. GitHub 상단 **Actions**
2. 왼쪽 **Build Android APK**
3. main에 Commit하면 자동 실행됩니다. 또는 **Run workflow**를 눌러 수동 실행합니다.
4. 초록색 **Success**가 되면 해당 run을 엽니다.
5. **Artifacts → MyLifeTracker-debug-apk**를 내려받습니다.
6. ZIP을 풀어 `app-debug.apk`를 휴대폰에 설치합니다.

## 기존에 설치한 구버전과 서명이 다른 경우

Android는 같은 패키지의 업데이트 APK가 기존 설치 앱과 같은 서명 인증서를 사용해야 업데이트를 허용합니다.
이전에 GitHub Actions가 임시 debug key로 만든 APK를 설치했다면 새 안정 서명키 버전이 바로 덮어쓰기 설치되지 않을 수 있습니다.

이 경우 **기존 앱에 중요한 데이터가 있으면 바로 삭제하지 마세요.** 기존 버전은 클라우드 기능이 없으므로 삭제하면 기기 안 데이터가 사라질 수 있습니다.
