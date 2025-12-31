# Quick Start Testing Guide

## 🎯 What Was Fixed

로그인 버튼 클릭 시 발생하던 `NullPointerException` 에러를 수정했습니다.
이제 로그인 시 사용자의 role에 따라 적절한 화면으로 이동합니다:
- USER role → HomeScreen
- ADMIN role → AdminHomeScreen

## 📱 테스트 방법

### 1단계: 앱 빌드 및 설치

```bash
# 프로젝트 디렉토리로 이동
cd /path/to/Seatly

# Debug APK 빌드
./gradlew assembleDebug

# 디바이스에 설치
adb install app/build/outputs/apk/debug/app-debug.apk
```

또는 Android Studio에서:
1. 프로젝트 열기
2. Run > Run 'app' 클릭 (또는 Shift+F10)

### 2단계: 일반 사용자로 로그인 테스트

1. 앱 실행
2. 로그인 화면에서 입력:
   ```
   이메일: user@test.com
   비밀번호: password (아무거나 입력 가능)
   ```
3. "로그인" 버튼 클릭
4. **예상 결과:**
   - ✅ 앱이 크래시하지 않음
   - ✅ HomeScreen으로 이동
   - ✅ 일반 사용자 인터페이스 표시

### 3단계: 관리자로 로그인 테스트

1. 앱 재시작 (또는 로그아웃)
2. 로그인 화면에서 입력:
   ```
   이메일: admin@test.com
   비밀번호: password (아무거나 입력 가능)
   ```
3. "로그인" 버튼 클릭
4. **예상 결과:**
   - ✅ 앱이 크래시하지 않음
   - ✅ AdminHomeScreen으로 이동
   - ✅ 관리자 인터페이스 표시

### 4단계: 자동 로그인 테스트

#### 일반 사용자 자동 로그인:
1. `user@test.com`으로 로그인
2. "자동 로그인" 체크박스 선택
3. 앱 완전 종료
4. 앱 재실행
5. **예상 결과:** 자동으로 HomeScreen으로 이동

#### 관리자 자동 로그인:
1. `admin@test.com`으로 로그인
2. "자동 로그인" 체크박스 선택
3. 앱 완전 종료
4. 앱 재실행
5. **예상 결과:** 자동으로 AdminHomeScreen으로 이동

## 🔍 확인 포인트

### ✅ 성공 시나리오
- [ ] 앱이 크래시하지 않음
- [ ] USER 계정은 HomeScreen으로 이동
- [ ] ADMIN 계정은 AdminHomeScreen으로 이동
- [ ] 자동 로그인이 정상 작동
- [ ] HomeScreen에서 데이터를 로드할 때 에러 없음

### ❌ 만약 문제가 발생한다면

#### 앱이 여전히 크래시하는 경우:
```bash
# Logcat으로 에러 확인
adb logcat | grep -i "exception\|error"
```

다음을 확인하세요:
1. Debug 빌드를 사용하고 있나요? (Release 빌드는 Mock Server가 없습니다)
2. 이메일 주소를 정확히 입력했나요? (대소문자 구분)
3. DebugMockInterceptor가 로드되었나요? (Logcat에서 확인)

#### 잘못된 화면으로 이동하는 경우:
```bash
# 로그인 응답 확인
adb logcat | grep -i "role\|login"
```

## 📊 테스트 시나리오 요약

| 테스트 케이스 | 이메일 | 비밀번호 | 예상 화면 | 결과 |
|--------------|--------|---------|-----------|------|
| 일반 사용자 로그인 | user@test.com | any | HomeScreen | [ ] |
| 관리자 로그인 | admin@test.com | any | AdminHomeScreen | [ ] |
| 일반 사용자 자동 로그인 | user@test.com | any | HomeScreen | [ ] |
| 관리자 자동 로그인 | admin@test.com | any | AdminHomeScreen | [ ] |

## 🔧 트러블슈팅

### 빌드 실패 시:
```bash
# Gradle 캐시 정리
./gradlew clean

# 다시 빌드
./gradlew assembleDebug --refresh-dependencies
```

### 이전 APK가 설치되어 있는 경우:
```bash
# 앱 제거
adb uninstall kr.jiyeok.seatly

# 다시 설치
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 로그 수집:
```bash
# 전체 로그 저장
adb logcat > logcat.txt

# Seatly 앱 관련 로그만
adb logcat | grep "kr.jiyeok.seatly" > seatly_log.txt
```

## 📝 추가 문서

더 자세한 정보는 다음 문서를 참고하세요:
- `TESTING_GUIDE.md` - 상세한 테스트 가이드
- `IMPLEMENTATION_SUMMARY.md` - 기술적 구현 상세
- `LOGIN_FLOW_DIAGRAM.md` - 로그인 흐름 다이어그램

## 💡 Mock 계정 정보

### 일반 사용자
```json
{
  "id": 1,
  "email": "user@test.com",
  "name": "일반 사용자",
  "phone": "010-9876-5432",
  "roles": ["USER"]
}
```

### 관리자
```json
{
  "id": 2,
  "email": "admin@test.com",
  "name": "관리자",
  "phone": "010-1234-5678",
  "roles": ["ADMIN"]
}
```

## ⚠️ 중요 사항

1. **Debug 빌드 전용**: 이 Mock Server는 debug 빌드에서만 동작합니다.
2. **비밀번호**: 테스트 계정은 어떤 비밀번호든 허용합니다.
3. **실제 서버**: Release 빌드는 실제 백엔드 API를 사용합니다.
4. **네트워크 불필요**: Mock Server는 로컬에서 동작하므로 인터넷 연결이 필요 없습니다.

## ✅ 테스트 완료 체크리스트

- [ ] Debug APK 빌드 성공
- [ ] 앱 설치 성공
- [ ] 일반 사용자 로그인 테스트 통과
- [ ] 관리자 로그인 테스트 통과
- [ ] 일반 사용자 자동 로그인 테스트 통과
- [ ] 관리자 자동 로그인 테스트 통과
- [ ] 앱 크래시 없음 확인
- [ ] HomeScreen 데이터 로드 정상 확인

모든 항목을 체크했다면, 이제 실제 백엔드 API와 연동할 준비가 되었습니다! 🎉
