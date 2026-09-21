# GitHub에서 APK 만들기

이 프로젝트에는 `.github/workflows/build-apk.yml`이 포함되어 있습니다.
GitHub 저장소에 프로젝트 내용을 올리면 GitHub Actions가 Android APK를 빌드합니다.

## 가장 쉬운 순서

1. GitHub에서 새 repository를 만듭니다.
2. 이 프로젝트 폴더 안의 파일과 폴더를 전부 repository 최상위에 업로드합니다.
   - `app/`, `gradle/`, `.github/`, `build.gradle.kts`, `settings.gradle.kts` 등이 바로 보여야 합니다.
   - `MyLifeTracker/MyLifeTracker/app/...`처럼 프로젝트 폴더가 한 단계 더 중첩되면 안 됩니다.
3. 업로드 후 repository의 **Actions** 탭을 엽니다.
4. 왼쪽에서 **Build Android APK**를 선택합니다.
5. **Run workflow** 버튼을 눌러 실행합니다. (main에 push할 때도 자동 실행됩니다.)
6. 실행 결과가 초록색 체크로 끝나면 해당 실행(run)을 엽니다.
7. 페이지 아래 **Artifacts**에서 `MyLifeTracker-debug-apk`를 다운로드합니다.
8. 다운로드한 ZIP의 압축을 풀면 `app-debug.apk`가 있습니다.
9. APK를 Android 휴대폰으로 옮겨 열고 설치합니다.

## 휴대폰에서 설치가 막힐 때

Android가 보안을 위해 브라우저/파일 앱에서 받은 APK의 설치를 처음에는 막을 수 있습니다.
설치 화면에서 해당 앱에 대해 '알 수 없는 앱 설치 허용'을 켠 뒤 다시 APK를 열면 됩니다.
설치가 끝난 뒤 이 권한을 다시 꺼도 됩니다.

## 참고

현재 workflow는 개인 테스트용 `debug APK`를 만듭니다. Google Play 배포용 서명 APK/AAB는 별도 release signing 설정이 필요합니다.
