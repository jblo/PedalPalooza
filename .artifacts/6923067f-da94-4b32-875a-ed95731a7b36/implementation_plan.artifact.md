# Add Push Notifications to PedalPalooza

This plan outlines the integration of Firebase Cloud Messaging (FCM) to support push notifications. This adds significant native value to the app, aiding in Google Play Store approval.

## User Review Required

> [!IMPORTANT]
> **Firebase Console Action Required**: You will need to create a project in the [Firebase Console](https://console.firebase.google.com/), add an Android app with package name `com.example.pedalpalooza`, and download the `google-services.json` file.
>
> You must place the `google-services.json` file into the `app/` directory of your project.

## Proposed Changes

### Build Configuration

#### [MODIFY] [root build.gradle.kts](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/build.gradle.kts)
- Add the Google Services classpath/plugin.

#### [MODIFY] [app build.gradle.kts](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/build.gradle.kts)
- Apply the `com.google.gms.google-services` plugin.
- Add Firebase BOM and FCM dependencies.

### Android Core

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/AndroidManifest.xml)
- Add `POST_NOTIFICATIONS` permission.
- Declare the `FirebaseMessagingService` implementation.

#### [NEW] [MyFirebaseMessagingService.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/MyFirebaseMessagingService.kt)
- Handle incoming messages and display native notifications.
- Create a notification channel on initialization.

### UI & Logic

#### [MODIFY] [MainActivity.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/MainActivity.kt)
- Implement a runtime permission request for notifications (required for Android 13+).
- Log the FCM token for testing purposes.

## Verification Plan

### Automated Tests
- Verify build with `./gradlew app:assembleDebug`.

### Manual Verification
1.  **Permission Request**: Launch the app and verify that a notification permission dialog appears (on Android 13+).
2.  **Token Generation**: Check Logcat for the FCM token.
3.  **Test Message**: Use the Firebase Console "Messaging" tab to send a test notification using the logged token.
