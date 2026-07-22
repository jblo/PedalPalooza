# Add Custom Bike Logo

Replace the default Android launcher icons with a custom bike-themed logo.

## User Review Required

> [!NOTE]
> I will replace the default "Android" foreground with a stylized bike icon.
> If you have a specific `.png` or `.svg` file you'd rather use, please let me know!

## Proposed Changes

### Resources

#### [NEW] [ic_bike_logo.xml](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/res/drawable/ic_bike_logo.xml)
- Create a new vector drawable featuring a bike icon.

#### [MODIFY] [ic_launcher.xml](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml)
#### [MODIFY] [ic_launcher_round.xml](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml)
- Update the adaptive icon to use `@drawable/ic_bike_logo` as the foreground.
- Optionally update the background color to a more vibrant "cycling" theme (e.g., a bright green).

### Logic

#### [MODIFY] [MyFirebaseMessagingService.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/MyFirebaseMessagingService.kt)
- Update the notification small icon to use the new bike logo.

## Verification Plan

### Manual Verification
- Deploy the app to the emulator.
- Verify the new icon appears on the home screen.
- Send a test notification and verify the bike icon appears in the status bar.
