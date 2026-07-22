# Code Quality Cleanup

Address remaining minor lint warnings and ensure the project follows the latest best practices.

## User Review Required

> [!NOTE]
> These are minor changes to satisfy lint warnings and improve code consistency.

## Proposed Changes

### UI & Theme

#### [MODIFY] [Type.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/ui/theme/Type.kt)
- Add missing trailing comma to `TextStyle` definition.

### MainActivity

#### [MODIFY] [MainActivity.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/MainActivity.kt)
- Use explicit parameter name `value` in `mutableStateOf(true)` to satisfy lint.
- Add trailing comma to `WebViewScreen` call in `MainActivity`.

## Verification Plan

### Automated Tests
- Verify build with `./gradlew app:assembleDebug`.
