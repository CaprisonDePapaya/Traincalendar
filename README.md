# Day Board for Android

Your calendar as a subway departure board. Each kind of plan rides a coloured
"line" (A Work, B Focus, C Deadlines…), the event happening now glows yellow
with a LIVE tag and a progress bar, and every other event counts down like an
arriving train: `18 MIN`, `1:58 HR`.

## Features

- **Board**: your day's departures with live countdowns that update every
  15 s. Tap the date to jump to another day, tap a line badge to filter, and
  tap the `◆ ALL-DAY` pill to see all-day plans.
- **Your events, your control**: add plans on the **Add** tab. Tap any event
  on the board to **edit** or **delete** it. Deleting shows an **Undo** button
  for a few seconds.
- **Phone calendar (optional)**: tap **Show phone calendar** to also see the
  events from your phone's calendar (Google, Samsung, Outlook…). Nothing leaves
  the device. Tap one of those events to **remove it from the board** (it stays
  in your calendar), open it in your calendar app to change or delete it there,
  or hide its whole calendar. **Settings** (⚙) lists your calendars with a
  switch for each and can bring back removed events.
- **Lines**: rename, re-letter and recolour lines, switch between circle and
  diamond badges, add lines, delete them with the trash button, and choose
  which calendars ride each line. A calendar with the same name as a line
  (e.g. "Work") rides it automatically.
- **Sample day**: the demo board from the video. It's off by default; turn it
  on in Settings to try it.
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

CI also runs `BoardFlowTest` on an emulator. It covers adding, editing,
deleting and undoing events, turning off the sample day, settings, and
deleting a line.
