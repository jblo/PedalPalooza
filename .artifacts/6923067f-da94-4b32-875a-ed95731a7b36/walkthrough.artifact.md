# Walkthrough - Daily List API Fix

I have fixed the HTTP 400 error that was occurring when loading the Daily List.

## Changes Made

### 1. API Query Fix
- **[ShiftApiService.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/data/ShiftApiService.kt)**:
    - Updated the default value for the `q` (query) parameter from an empty string `""` to `"%"`.
    - **Reason**: The Shift2Bikes API requires a non-empty search term. Using `%` acts as a wildcard, allowing the app to fetch all upcoming events without a specific search keyword, which was previously causing the server to reject the request with a 400 Bad Request error.

### 2. UI Robustness
- **[EventListScreen.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/ui/EventListScreen.kt)**:
    - Added an empty state message ("No upcoming events found") to handle cases where the API might return an empty list gracefully.

## Verification Results

### Automated Tests
- Ran `app:assembleDebug`. The build finished successfully.

### Manual Verification Required
- Open the "Daily List" tab. It should now load the events correctly instead of showing the "HTTP 400" error.
