# Day Board for Android

Your calendar as a subway departure board. Each kind of plan rides a coloured
"line" (A Work, B Focus, C Deadlines…), the event happening now glows yellow
with a LIVE tag and a progress bar, and every other event counts down like an
arriving train: `18 MIN`, `1:58 HR`.

## Features

- **Board**: today's departures with live countdowns that update every 15 s.
  Tap the date to jump to another day, tap a line badge to filter, and tap the
  `◆ ALL-DAY` pill to see all-day plans.
- **Your calendar**: reads the events on your phone's calendar (Google,
  Samsung, Outlook… anything synced to Android). Tapping an event opens it in
  your calendar app. Nothing leaves the device.
- **Sample day**: a demo board, on by default, so you can try the app before
  you allow calendar access. Close it with the ✕ on the `SAMPLE DAY` bar.
- **Lines**: rename, re-letter and recolour lines, switch between circle and
  diamond badges, add or delete lines, and choose which calendars ride each
  line. A calendar with the same name as a line (e.g. "Work") rides it
  automatically.
- **Add**: quick plans that are stored in the app, with a line, a time and a
  duration. Tap one on the board to remove it.
- **Home-screen widget**: an amber, dot-matrix style arrivals sign that you can
  resize. It refreshes about every 15 minutes, and right away when you change
  something in the app.

## Install

Every push builds a debug APK on GitHub Actions. Open the latest run of
**Android build**, download the `day-board-debug-apk` artifact, unzip it, and
install `app-debug.apk` on your phone. You'll need to allow installs from
unknown sources.

## Build locally

You need Android Studio, or JDK 17 plus the Android SDK (API 35).

```sh
./gradlew assembleDebug
```

Requires Android 8.0 (API 26) or newer. Built with Kotlin, Jetpack Compose and
Glance.
