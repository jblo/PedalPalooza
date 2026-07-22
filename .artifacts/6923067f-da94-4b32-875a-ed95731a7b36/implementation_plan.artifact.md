# Custom Native Calendar UI with Google Calendar Integration

Transition from a simple web wrapper to a native experience with a clean daily list view and calendar export functionality.

## User Review Required

> [!NOTE]
> This is a significant architectural upgrade. The app will now fetch data directly from the Shift2Bikes API and render it using native Compose components.

## Proposed Changes

### Dependencies & Architecture

#### [MODIFY] [libs.versions.toml](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/gradle/libs.versions.toml)
- Add Retrofit, OkHttp, and Kotlin Serialization for networking and JSON parsing.

#### [MODIFY] [app build.gradle.kts](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/build.gradle.kts)
- Add the new networking dependencies.

### Data Layer

#### [NEW] [ShiftApiService.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/data/ShiftApiService.kt)
- Define the Retrofit interface for `https://www.shift2bikes.org/api/search.php`.

#### [NEW] [EventModels.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/data/EventModels.kt)
- Define data classes for the API response (Event, Venue, Pagination).

### UI Layer (Native)

#### [NEW] [EventListScreen.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/ui/EventListScreen.kt)
- Implement a `LazyColumn` that displays events grouped by date.
- Create a `EventCard` component with:
    - Title, Time, and Location.
    - "Add to Google Calendar" button.
    - Click to open full details (via the existing WebView or a new native detail screen).

#### [NEW] [CalendarUtils.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/utils/CalendarUtils.kt)
- Helper to generate Google Calendar URLs (`https://www.google.com/calendar/render?action=TEMPLATE...`).

### Main Integration

#### [MODIFY] [MainActivity.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/MainActivity.kt)
- Add a `NavigationSuiteScaffold` (from Material 3) or a simple toggle to switch between the **Native List** and the **Full Website (WebView)**.

## Verification Plan

### Automated Tests
- Verify successful build.
- (Optional) Unit tests for JSON parsing logic.

### Manual Verification
- Verify upcoming events load correctly in the native list.
- Verify "Add to Google Calendar" button opens the Google Calendar app/web with correct details.
- Verify switching between Web and Native views works seamlessly.
