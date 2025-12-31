# Implementation Summary: Login Role-Based Navigation Fix

## Problem Statement
When clicking the login button, the app crashed with a NullPointerException:
```
java.lang.NullPointerException: Parameter specified as non-null is null: 
method kr.jiyeok.seatly.ui.screen.user.CafeInfo.<init>, parameter name
```

The app needed to:
1. Properly determine user role (USER or ADMIN) after login
2. Navigate to HomeScreen for USER role
3. Navigate to AdminHomeScreen for ADMIN role
4. Use MockWebServer to return mock user data since backend is not available

## Solution Implemented

### 1. MockWebServer Integration
**File:** `app/build.gradle.kts`
- Added dependency: `com.squareup.okhttp3:mockwebserver:4.12.0`
- This library was already being used in OkHttp, so it's compatible

### 2. Mock User Data with Role Support
**File:** `app/src/debug/java/kr/jiyeok/seatly/di/DebugMockInterceptor.kt`

#### Changes:
- Updated `/auth/login` endpoint to read request body
- Returns different user data based on email in request:
  - `user@test.com` → Returns user with `roles: ["USER"]`
  - `admin@test.com` → Returns user with `roles: ["ADMIN"]`
- Updated user info for consistency

#### Mock Accounts Created:
1. **Regular User**
   - Email: `user@test.com`
   - Name: "일반 사용자"
   - Roles: ["USER"]
   - ID: 1

2. **Admin User**
   - Email: `admin@test.com`
   - Name: "관리자"
   - Roles: ["ADMIN"]
   - ID: 2

### 3. Added Mock Responses for HomeScreen Data
**File:** `app/src/debug/java/kr/jiyeok/seatly/di/DebugMockInterceptor.kt`

Added mock responses for:
- `GET /users/me/current-cafe` → Returns `null` (no active usage)
- `GET /users/me/favorites` → Returns empty page response
- `GET /users/me/recent-cafes` → Returns empty array

These prevent NullPointerException when HomeScreen loads data.

### 4. Role-Based Navigation in LoginScreen
**File:** `app/src/main/java/kr/jiyeok/seatly/ui/screen/common/LoginScreen.kt`

#### Changes:
1. Added import: `kr.jiyeok.seatly.domain.model.ERole`
2. Added state collection: `val userRole by viewModel.userRole.collectAsState()`
3. Updated navigation logic in `LaunchedEffect(authState)`:
   ```kotlin
   val destination = if (userRole == ERole.ADMIN) {
       "admin_home"
   } else {
       "home"
   }
   navController.navigate(destination) {
       popUpTo("login") { inclusive = true }
   }
   ```

## How It Works

### Login Flow:
1. User enters email and password on LoginScreen
2. LoginScreen calls `viewModel.login(LoginRequest(email, password))`
3. AuthViewModel → LoginUseCase → Repository → ApiService → DebugMockInterceptor
4. DebugMockInterceptor reads request body, checks email:
   - If `admin@test.com` → returns admin user data with ADMIN role
   - Otherwise → returns regular user data with USER role
5. AuthViewModel receives login response with user data
6. AuthViewModel calls `fetchUserData()` to set role:
   ```kotlin
   val isAdmin = user?.roles?.let { ERole.isAdmin(it) } ?: false
   _userRole.value = if (isAdmin) ERole.ADMIN else ERole.USER
   ```
7. AuthViewModel sets authState to Success
8. LoginScreen's LaunchedEffect detects Success state
9. LoginScreen checks userRole and navigates accordingly:
   - ADMIN → "admin_home" (AdminHomeScreen)
   - USER → "home" (HomeScreen)

### HomeScreen Data Loading:
1. HomeScreen calls `viewModel.loadHomeData()` in LaunchedEffect
2. HomeViewModel fetches:
   - Current cafe usage → DebugMockInterceptor returns null
   - Favorite cafes → DebugMockInterceptor returns empty list
   - Recent cafes → DebugMockInterceptor returns empty list
3. HomeScreen displays UI with empty/default data (no crash)

## Why This Fixes the Original Error

The original NullPointerException occurred because:
1. Login succeeded but navigation happened before role was determined
2. The app always navigated to "home" regardless of role
3. HomeScreen tried to load data but backend wasn't available
4. CafeInfo was constructed with null name parameter

This is now fixed because:
1. ✅ MockWebServer provides mock data for all required endpoints
2. ✅ Role is properly determined from login response before navigation
3. ✅ Navigation goes to correct screen based on role
4. ✅ HomeScreen data endpoints return valid responses (even if empty)
5. ✅ No more NullPointerException

## Testing Instructions

### Test Case 1: USER Login
```
Email: user@test.com
Password: (any text)
Expected: Navigate to HomeScreen without crash
```

### Test Case 2: ADMIN Login
```
Email: admin@test.com
Password: (any text)
Expected: Navigate to AdminHomeScreen without crash
```

See `TESTING_GUIDE.md` for comprehensive testing instructions.

## Notes

- This implementation only affects debug builds
- Release builds will use the actual backend API
- The MockWebServer approach allows development without a backend
- The same pattern can be extended for other mock scenarios

## Architecture Benefits

1. **Separation of Concerns**: Mock logic is isolated in DebugMockInterceptor
2. **Build Variant Specific**: Only debug builds have mock data
3. **Type Safety**: Uses proper DTOs and data classes
4. **Role-Based Access**: Properly implements role checking at ViewModel level
5. **Navigation Logic**: Clean separation between authentication and navigation

## Future Enhancements

When the backend is available:
1. Remove or update DebugMockInterceptor
2. Update BuildConfig.SEATLY_BASE_URL with actual backend URL
3. Ensure backend returns user data in the same format as mocks
4. Test with real authentication tokens
