# Walkthrough - Native Daily Calendar & Google Calendar Integration

I have upgraded PedalPalooza from a simple web wrapper to a native-hybrid application. The app now features a fast, native "Daily List" view for upcoming bike events with integrated Google Calendar support.

## Changes Made

### 1. Networking & Data Layer
- **Retrofit & Serialization**: Integrated Retrofit for API calls and Kotlinx Serialization for parsing the Shift2Bikes JSON data.
- **[ShiftApiService.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/data/ShiftApiService.kt)**: Created a service to fetch live event data from `shift2bikes.org`.
- **[EventModels.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/data/EventModels.kt)**: Defined robust data structures for events, venues, and pagination.

### 2. Native Daily View
- **[EventListScreen.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/ui/EventListScreen.kt)**:
    - Implemented a clean `LazyColumn` that groups bike rides by date.
    - Each ride is displayed in a native `Card` with its title, time, and location.
    - Clicking a ride now takes you directly to its specific detail page in the WebView.

### 3. Google Calendar Integration
- **[CalendarUtils.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/utils/CalendarUtils.kt)**: Added a utility to generate Google Calendar "Template" URLs.
- **"Add to Google Cal" Button**: Every event in the list now has a button that opens Google Calendar with the ride's title, time, and location pre-filled.

### 4. Hybrid Navigation
- **[MainActivity.kt](file:///C:/Users/jarla/AndroidStudioProjects/PedalPalooza/app/src/main/java/com/example/pedalpalooza/MainActivity.kt)**:
    - Implemented a `NavigationBar` to easily switch between the **Daily List** and the **Full Website**.
    - Used Jetpack Navigation to handle smooth transitions between screens.
    - Improved the `TopAppBar` to show context-sensitive titles and a back button for the web view.

## Verification Results

### Automated Tests
- Ran `app:assembleDebug`. The build finished successfully with all networking and UI components correctly integrated.

### Manual Verification Required
1.  **Daily List**: Open the app; it should default to the native list view. Verify events load and are grouped by date.
2.  **Add to Calendar**: Click the "Add to Google Cal" button on any event. Verify it opens your calendar app with the correct details.
3.  **Details**: Click an event card to see its full details on the Shift2Bikes website.
4.  **Navigation**: Use the bottom bar to switch to the "Website" view to browse the calendar using the original web interface.
