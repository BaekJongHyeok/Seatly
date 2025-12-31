# Testing Guide for Login Role-Based Navigation

## Overview
This document describes how to test the role-based login and navigation feature that was implemented to fix the NullPointerException crash when logging in.

## Changes Made

### 1. Mock Web Server Integration
- Added MockWebServer dependency to handle mock API responses
- Updated `DebugMockInterceptor.kt` to support different user roles

### 2. Mock User Accounts
Two test accounts have been configured in the debug build:

**Regular User:**
- Email: `user@test.com`
- Password: Any password (e.g., "password")
- Role: USER
- Expected behavior: Navigates to HomeScreen after login

**Admin User:**
- Email: `admin@test.com`
- Password: Any password (e.g., "password")
- Role: ADMIN
- Expected behavior: Navigates to AdminHomeScreen after login

### 3. Role-Based Navigation
- LoginScreen now checks the user's role from AuthViewModel
- USER role → navigates to "home" route (HomeScreen)
- ADMIN role → navigates to "admin_home" route (AdminHomeScreen)

### 4. Null Safety
- Added mock responses for HomeScreen data endpoints to prevent NullPointerException
- `/users/me/current-cafe` returns null (no active cafe usage)
- `/users/me/favorites` returns empty list
- `/users/me/recent-cafes` returns empty list

## Testing Steps

### Test Case 1: Login as Regular User
1. Launch the app (debug build)
2. On the login screen, enter:
   - Email: `user@test.com`
   - Password: `password` (or any text)
3. Click the login button
4. **Expected Result:** 
   - App should successfully log in
   - App should navigate to HomeScreen (user interface)
   - No crash should occur
   - HomeScreen should display with default/empty data

### Test Case 2: Login as Admin User
1. Launch the app (debug build)
2. On the login screen, enter:
   - Email: `admin@test.com`
   - Password: `password` (or any text)
3. Click the login button
4. **Expected Result:**
   - App should successfully log in
   - App should navigate to AdminHomeScreen (admin interface)
   - No crash should occur
   - AdminHomeScreen should display properly

### Test Case 3: Auto-Login with Regular User
1. Log in with `user@test.com` and enable auto-login checkbox
2. Close the app completely
3. Relaunch the app
4. **Expected Result:**
   - App should automatically log in
   - App should navigate to HomeScreen
   - No crash should occur

### Test Case 4: Auto-Login with Admin User
1. Log in with `admin@test.com` and enable auto-login checkbox
2. Close the app completely
3. Relaunch the app
4. **Expected Result:**
   - App should automatically log in
   - App should navigate to AdminHomeScreen
   - No crash should occur

## Troubleshooting

### If the app still crashes:
1. Check logcat for the error message
2. Verify that the debug build is being used (not release)
3. Ensure that DebugMockInterceptor is properly loaded
4. Check that the email address is exactly as specified (case-sensitive)

### If navigation goes to the wrong screen:
1. Verify the user's role in the login response by checking logcat
2. Ensure AuthViewModel is properly setting the userRole StateFlow
3. Check that the navigation logic in LoginScreen is correctly evaluating the role

## Building the App

To build and test the debug APK:

```bash
cd /home/runner/work/Seatly/Seatly
./gradlew assembleDebug
```

The APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

Install it on your device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or run directly:
```bash
./gradlew installDebug
```

## Implementation Details

### Files Modified:
1. `app/build.gradle.kts` - Added MockWebServer dependency
2. `app/src/debug/java/kr/jiyeok/seatly/di/DebugMockInterceptor.kt` - Added role-based mock responses
3. `app/src/main/java/kr/jiyeok/seatly/ui/screen/common/LoginScreen.kt` - Added role-based navigation

### Key Code Changes:

**DebugMockInterceptor.kt:**
- Reads request body to determine which user account is being used
- Returns different user data based on email address
- Admin users get `"roles": ["ADMIN"]`
- Regular users get `"roles": ["USER"]`

**LoginScreen.kt:**
- Collects `userRole` from AuthViewModel
- Uses `if (userRole == ERole.ADMIN)` to determine destination route
- Navigates to either "admin_home" or "home"

**AuthViewModel.kt:** (No changes needed - already implemented)
- Fetches user data after login
- Determines role from user response
- Sets `_userRole` StateFlow based on roles array

## Expected Behavior Summary

| User Email | Password | Role | Destination Screen | Should Crash? |
|------------|----------|------|-------------------|---------------|
| user@test.com | any | USER | HomeScreen | No |
| admin@test.com | any | ADMIN | AdminHomeScreen | No |

## Notes

- This implementation uses MockWebServer only in debug builds
- Release builds will use the actual backend API
- The mock server intercepts requests before they reach the network
- All test accounts accept any password for convenience during development
