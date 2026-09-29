# Vardiya 3.0

> **Vardiya** is a modern, privacy-first, local-only Android shift and real-time earnings tracker engineered with Jetpack Compose and Material 3 Expressive design.

---

## Overview

Vardiya 3.0 transforms the application into an enterprise-grade, adaptive shift companion designed for workers, contractors, and shift laborers. All calculations are executed completely offline on-device with zero telemetry, zero advertisements, and zero network access.

---

## Architectural Highlights (Phases B — I)

- **Phase B — Adaptive Navigation Shell:** Powered by Jetpack Navigation 3 (`androidx.navigation3`). Uses a responsive navigation bar on compact screens and automatically transitions to a side `NavigationRail` on medium and expanded displays (foldables, tablets, desktops).
- **Phase C — High-Precision Earnings Engine:** Deterministic financial arithmetic using Java `BigDecimal` with 16 internal calculation scale to eliminate IEEE-754 floating-point drift. Dual-clock architecture pairs `SystemClock.elapsedRealtime()` for monotonic elapsed time tracking with wall-clock epoch timestamps for system reboot survival and midnight crossing invariance. Fully supports custom overtime multipliers (e.g. 1.5x) and night differential supplements (e.g. +15%).
- **Phase D — Material 3 Expressive Home Dashboard:** Features an animated wave circular progress hero card with real-time counters, state badges, tactile haptic control center (Start, Pause, Resume, Finish), and live rate breakdown chips.
- **Phase E — Break Management & Shift Templates:** Real-time break logging with distinct paid and unpaid break tracking. Includes 4 pre-configured industry shift templates (Sabah 08-16, Akşam 16-24, Gece 00-08, 12 Saatlik 08-20) plus custom configurations.
- **Phase F — Shift History, Calendar Heatmap & Search:** Interactive calendar heatmap showing monthly intensity based on hours worked and earnings. Features instant full-text search across notes, dates, and times, multi-criteria category filtering (All, This Month, This Week, Overtime, Night Shift), and rich decomposed shift detail sheets.
- **Phase G — Earnings Analytics & Canvas Charts:** Comprehensive weekly and monthly earning trends rendered via lightweight, hardware-accelerated Canvas bar charts with smooth animations and zero external chart dependencies. Displays average hourly rates, top-earning periods, and KPI summaries.
- **Phase H — Advanced Settings & Dynamic Color:** Expressive settings center featuring Material 3 `TimePickerDialog` for custom start/end shift hours and night differential windows. Displays a live, reactive rate preview card and supports Material You Dynamic Color with seamless persistence.
- **Phase I — Adaptive Large Screen Optimization:** Pure Compose responsive layout foundation (`VardiyaTwoPaneLayout`). On foldables and tablets (`>= 600dp`), History switches to a dual-pane List-Detail view preserving selection across rotations, while Home, Analytics, and Settings adapt into balanced two-column dashboards.

---

## Current Version Metadata

- **Application ID:** `com.example.androidapp`
- **Version Name:** `3.0.0`
- **Version Code:** `8`
- **Minimum SDK:** `24` (Android 7.0 Nougat)
- **Target / Compile SDK:** `36` (Android 16 Preview)
- **Java Toolchain:** `Java 17`

---

## Build & Test Commands

All commands should be executed from the `AndroidApp/` directory with a valid JDK 17 environment:

### Quality Assurance & Validation

```bash
# Run unit test suite (210 deterministic unit tests)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest --rerun-tasks

# Run Android Lint analysis
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lintDebug
```

### Application Builds

```bash
# Debug APK build
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug

# Signed Production Release APK build
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleRelease

# Signed Production App Bundle (AAB) build
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew bundleRelease
```

---

## Distribution Formats: APK vs AAB

- **APK (`.apk`):** Standalone installation binary (`app/build/outputs/apk/release/app-release.apk`). Intended for direct sideloading and offline distribution on Android smartphones and tablets without requiring Google Play services.
- **AAB (`.aab`):** Android App Bundle (`app/build/outputs/bundle/release/app-release.aab`). Optimized publishing format for Google Play Store distribution, enabling Google Play Dynamic Delivery to serve device-tailored APK slices.

---

## License & Privacy

Local-first application. No internet permissions (`android.permission.INTERNET` is not declared). Your data never leaves your device.