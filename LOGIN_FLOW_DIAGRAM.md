# Login Flow Diagram

## Before Fix (Crashed)

```
┌─────────────────┐
│  LoginScreen    │
│                 │
│ Click Login     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  AuthViewModel  │
│  - login()      │
│  - (role not    │
│    determined)  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Navigate to    │
│  "home"         │
│  (always)       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  HomeScreen     │
│  - Loads data   │
│  - Backend not  │
│    available    │
│  ❌ CRASH!      │
└─────────────────┘
```

## After Fix (Works)

```
┌─────────────────────────────────────────────────────┐
│                   LoginScreen                       │
│                                                     │
│  Email: user@test.com / admin@test.com             │
│  Password: (any)                                   │
│  Click Login ────────────────────────────┐         │
└──────────────────────────────────────────┼─────────┘
                                          │
                                          ▼
                        ┌──────────────────────────────┐
                        │      DebugMockInterceptor    │
                        │                              │
                        │  Checks email in request:    │
                        │  ├─ user@test.com?           │
                        │  │  └─ Return USER role      │
                        │  └─ admin@test.com?          │
                        │     └─ Return ADMIN role     │
                        └──────────────┬───────────────┘
                                      │
                                      ▼
                        ┌──────────────────────────────┐
                        │      AuthViewModel           │
                        │                              │
                        │  - Receives login response   │
                        │  - Sets _loginData           │
                        │  - Calls fetchUserData()     │
                        │  - Determines role from      │
                        │    user.roles array          │
                        │  - Sets _userRole            │
                        │  - Sets authState = Success  │
                        └──────────────┬───────────────┘
                                      │
                                      ▼
                        ┌──────────────────────────────┐
                        │      LoginScreen             │
                        │  LaunchedEffect(authState)   │
                        │                              │
                        │  if (userRole == ADMIN)      │
                        │    navigate("admin_home")    │
                        │  else                        │
                        │    navigate("home")          │
                        └──────────────┬───────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    │                                   │
                    ▼                                   ▼
        ┌────────────────────┐            ┌────────────────────┐
        │   HomeScreen       │            │  AdminHomeScreen   │
        │   (USER role)      │            │  (ADMIN role)      │
        │                    │            │                    │
        │  - Loads data      │            │  - Shows admin UI  │
        │  - Gets mock data  │            │  - No crash        │
        │  ✅ Works!         │            │  ✅ Works!         │
        └────────────────────┘            └────────────────────┘
```

## Mock Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    DebugMockInterceptor                     │
│                                                             │
│  Intercepts ALL API calls in debug build                   │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ├─► POST /auth/login
                   │   ├─ Reads request body
                   │   ├─ Checks email
                   │   └─ Returns user with role
                   │
                   ├─► GET /users/me
                   │   └─ Returns USER role by default
                   │
                   ├─► GET /users/me/current-cafe
                   │   └─ Returns null (no active usage)
                   │
                   ├─► GET /users/me/favorites
                   │   └─ Returns empty list
                   │
                   └─► GET /users/me/recent-cafes
                       └─ Returns empty list
```

## Role Determination Logic

```kotlin
// In AuthViewModel.fetchUserData()

when (val userResult = getCurrentUserUseCase()) {
    is ApiResult.Success -> {
        val user = userResult.data
        _userData.value = user
        
        // Determine role from user data
        val isAdmin = user?.roles?.let { 
            ERole.isAdmin(it)  // Checks if "ADMIN" in roles list
        } ?: false
        
        _userRole.value = if (isAdmin) ERole.ADMIN else ERole.USER
        _authState.value = AuthUiState.Success(user)
    }
    is ApiResult.Failure -> {
        // Default to USER role on failure
        _userRole.value = ERole.USER
        _authState.value = AuthUiState.Success(_loginData.value)
    }
}
```

## Navigation Decision

```kotlin
// In LoginScreen.LaunchedEffect(authState)

val destination = if (userRole == ERole.ADMIN) {
    "admin_home"  // AdminHomeScreen
} else {
    "home"        // HomeScreen
}

navController.navigate(destination) {
    popUpTo("login") { inclusive = true }
}
```

## Test Accounts

| Email            | Password | Role  | Destination     |
|------------------|----------|-------|-----------------|
| user@test.com    | any      | USER  | HomeScreen      |
| admin@test.com   | any      | ADMIN | AdminHomeScreen |

## Key Files Modified

```
app/
├── build.gradle.kts                                    [Modified]
│   └─ Added: mockwebserver:4.12.0
│
├── src/debug/java/kr/jiyeok/seatly/di/
│   └── DebugMockInterceptor.kt                        [Modified]
│       ├─ Added role-based login responses
│       ├─ Added mock data endpoints
│       └─ Reads request body to determine role
│
└── src/main/java/kr/jiyeok/seatly/ui/screen/common/
    └── LoginScreen.kt                                  [Modified]
        ├─ Added: import ERole
        ├─ Added: val userRole by viewModel.userRole.collectAsState()
        └─ Modified: Navigation logic to check role
```

## Crash Resolution

### Original Error:
```
java.lang.NullPointerException: Parameter specified as non-null is null: 
method kr.jiyeok.seatly.ui.screen.user.CafeInfo.<init>, parameter name
at kr.jiyeok.seatly.ui.screen.user.HomeScreen(HomeScreen.kt:156)
```

### Root Causes:
1. ❌ Backend not available → No real data
2. ❌ Always navigating to "home" → Wrong screen for admin
3. ❌ HomeScreen loading data without mock → Null values

### Solutions Applied:
1. ✅ MockWebServer provides data → Mock responses
2. ✅ Role-based navigation → Correct screen for each role
3. ✅ Mock endpoints for HomeScreen → No null values

## Security Note

⚠️ This implementation is for **DEBUG builds only**. The MockWebServer and test accounts are automatically excluded from release builds. In production, the app will use the real backend API with proper authentication.
