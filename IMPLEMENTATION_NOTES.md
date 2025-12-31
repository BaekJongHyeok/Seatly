# Login Flow Enhancement - Implementation Notes

## Overview
This implementation enhances the login flow to fetch user data after successful authentication and navigate users to the appropriate home screen based on their role (USER or ADMIN).

## Changes Made

### 1. ERole Enum (`domain/model/ERole.kt`)
- Created an enum to represent user roles: `USER` and `ADMIN`
- Added helper methods:
  - `fromString(role: String)`: Converts string to ERole, defaulting to USER
  - `isAdmin(roles: List<String>)`: Checks if a list contains ADMIN role

### 2. AuthViewModel Updates (`presentation/viewmodel/AuthViewModel.kt`)
- Added dependency injection for `GetCurrentUserUseCase`
- Added new StateFlows:
  - `userRole: StateFlow<ERole>` - Exposes the current user's role
  - `userData: StateFlow<UserResponseDto?>` - Exposes the current user's full data
- Modified `login()` function:
  - After successful login, calls `fetchUserData()` instead of immediately setting success state
  - `fetchUserData()` makes an API call to `/users/me` to get user details
  - Determines user role from the `roles` field in UserResponseDto
  - Updates `_userRole` based on whether user has ADMIN role
  - Handles errors gracefully by defaulting to USER role if fetch fails
- Modified `logout()` function:
  - Resets user data and role to defaults

### 3. RootNavigation Updates (`ui/navigation/RootNavigation.kt`)
- Injected `AuthViewModel` using Hilt
- Changed from local mutable `ownerState` to derived state from `authViewModel.userRole`
- The `ownerState` now automatically updates based on user role:
  - `ERole.ADMIN` -> shows AdminHomeScreen and admin bottom navigation
  - `ERole.USER` -> shows UserHomeScreen and user bottom navigation

### 4. MockInterceptor Updates (`data/remote/mock/MockInterceptor.kt`)
- Updated `/auth/login` endpoint to return proper `LoginResponseDTO` structure with:
  - `accessToken`, `refreshToken`, `expiresIn`
  - Complete `user` object with `roles` array
- Added mock credentials:
  - User: `user@example.com` / `password123` -> roles: ["USER"]
  - Admin: `admin@example.com` / `admin123` -> roles: ["ADMIN", "USER"]
- Added `/users/me` endpoint that returns UserResponseDto with roles

## Navigation Flow

### Before:
1. User logs in
2. Navigate to "home" route
3. Hard-coded `ownerState` determines screen

### After:
1. User enters credentials and logs in
2. `AuthViewModel` calls login API
3. On success, `AuthViewModel` fetches user data from `/users/me`
4. User role is determined from the `roles` field
5. `AuthViewModel` updates `userRole` StateFlow
6. `RootNavigation` observes `userRole` and updates `ownerState`
7. User is navigated to "home" route
8. Appropriate screen is shown based on actual user role

## Testing Instructions

### Test Case 1: USER Login
1. Run the app
2. Login with:
   - Email: `user@example.com`
   - Password: `password123`
3. Expected: Navigate to UserHomeScreen with user bottom navigation

### Test Case 2: ADMIN Login
1. Run the app
2. Login with:
   - Email: `admin@example.com`
   - Password: `admin123`
3. Expected: Navigate to AdminHomeScreen with admin bottom navigation

### Test Case 3: Error Handling
1. Test with invalid credentials
2. Expected: Error message displayed, no navigation

### Test Case 4: Network Failure Handling
1. Simulate network failure after login but before user data fetch
2. Expected: User is logged in and navigates to USER home screen (graceful degradation)

## Error Handling

The implementation includes robust error handling:

1. **Login API Failure**: Shows error message, stays on login screen
2. **User Data Fetch Failure**: Logs user in but defaults to USER role
3. **Invalid Role Data**: Defaults to USER role if role parsing fails
4. **Logout**: Properly resets all user state including role

## Security Considerations

1. User role is determined server-side (from API response)
2. Role cannot be manipulated client-side
3. Graceful degradation defaults to USER role (least privilege)
4. Token-based authentication is maintained

## Future Enhancements

1. Add role-based route guards to prevent unauthorized navigation
2. Cache user role in SharedPreferences for faster startup
3. Add loading states during user data fetch
4. Implement retry logic for failed user data fetches
5. Add analytics tracking for role-based navigation

## Build Notes

The project uses:
- Android Gradle Plugin 8.3.0
- Kotlin 1.9.22
- Hilt for dependency injection
- Retrofit for API calls
- Compose for UI

To build:
```bash
./gradlew assembleDebug
```

To run:
```bash
./gradlew installDebug
```
