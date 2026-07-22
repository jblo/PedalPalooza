# Tasks

- [x] Configure Dependencies
    - [x] Add Firebase and Google Services to `libs.versions.toml`
    - [x] Apply plugins in root and app `build.gradle.kts`
    - [x] Add FCM dependencies in app `build.gradle.kts`
- [x] Update Manifest
    - [x] Add `POST_NOTIFICATIONS` permission
    - [x] Declare `MyFirebaseMessagingService`
- [x] Implement Push Notification Logic
    - [x] Create `MyFirebaseMessagingService.kt` with notification channel handling
    - [x] Update `MainActivity.kt` with permission request and token logging
- [x] Finalize Configuration
    - [x] **Add `google-services.json`**
- [x] Verify build
