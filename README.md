# PedalPalooza

PedalPalooza is an Android app for the [Shift2Bikes Calendar](https://www.shift2bikes.org/calendar/), built for Portland's cycling community with a native daily ride list, local pre-ride reminders, Google Calendar export, and an integrated Shift2Bikes web view.

## Features

- **Native Daily Ride List**: Browse rides grouped by date via the Shift2Bikes API with debounced search, pull-to-refresh, offset pagination, retry handling, and clear visual indicators for cancelled rides.
- **Local Ride Reminders**: Tap **Remind Me** on any upcoming ride to schedule a local notification 30 minutes before start time (Portland `America/Los_Angeles` timezone) using `WorkManager`, and filter down to your saved rides with the **Reminders** chip.
- **Google Calendar Export**: Add rides directly to Google Calendar with timezone-aware start/end times and cleaned location formatting.
- **Integrated Shift2Bikes Website**: View individual ride details or browse the full Shift2Bikes calendar in-app with reactive back navigation and automatic external browser handoff for non-Shift2Bikes links.
- **Modern Material 3 UI**: Built with Jetpack Compose, edge-to-edge layout, dynamic color (Android 12+), and a custom Portland cycling fallback palette.

## Tech Stack

- **Kotlin & Jetpack Compose (Material 3)** with Navigation Compose
- **Architecture**: `ViewModel` + `StateFlow` + `EventRepository`
- **Networking**: Retrofit 2.11 (`converter-kotlinx-serialization`), OkHttp, Kotlinx Serialization
- **Background & Notifications**: WorkManager (`OneTimeWorkRequest`) + Firebase Cloud Messaging
- **Testing**: JUnit 4 + `kotlinx-coroutines-test`

## Getting Started

### Prerequisites

- JDK 21
- Android SDK 36.1 (`minSdk = 24`, `targetSdk = 36`)
- Android Studio or the Android CLI

### Build & Test

1. Clone the repository:
   ```bash
   git clone https://github.com/jblo/PedalPalooza.git
   cd PedalPalooza
   ```
2. Run local unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. Build a debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Build a minified release APK (R8 enabled):
   ```bash
   ./gradlew assembleRelease
   ```

## Contributing

Contributions are welcome! Feel free to open issues or submit pull requests for bug fixes or enhancements.

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Shift2Bikes](https://www.shift2bikes.org/) for maintaining Portland's community bike calendar.
- All ride organizers and participants in Pedalpalooza / Bike Summer.
