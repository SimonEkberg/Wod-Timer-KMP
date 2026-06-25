# WOD Timer — KMP

A **Kotlin Multiplatform** rebuild of [WOD Timer](https://github.com/SimonEkberg/wod-timer), targeting **Android and iOS** from a single codebase, with the UI shared via **Compose Multiplatform**.

This repository is the multiplatform successor to the Android-only app. The original Kotlin/Jetpack Compose app continues to live at `SimonEkberg/wod-timer`; this project shares its logic and UI across both platforms.

> **Status: full app ported.** The entire WOD Timer app now lives in `shared/commonMain` — the timer engine, 82 seeded workouts, kotlinx-serialization persistence, all the Compose UI (home tabs, quick-start tiles, run screen with flash + round summaries, custom-workout editor, and workout notes with a draw/type whiteboard + gallery-image backgrounds). Platform specifics (sound, key-value storage, keep-awake, immersive mode, system back, image pick/decode) are behind `expect`/`actual` shims.
>
> The **Android target builds and runs** (verified on emulator: 82 workouts load, timer/clock run, full UI). The **iOS target compiles on macOS + Xcode** — its actuals (Skia image decode, `UIImagePickerController`, `NSUserDefaults`, etc.) are written but, like all iOS code here, are verified on a Mac. The iOS image picker in particular uses UIKit interop that should be sanity-checked on first Mac build. See [MIGRATION.md](MIGRATION.md) for the file-by-file mapping.

## Module layout

```
wod-timer-kmp/
├── shared/            # Kotlin Multiplatform library — the code shared by both apps
│   └── src/
│       ├── commonMain/    # Shared Kotlin + shared Compose UI (App.kt) + expect declarations
│       ├── androidMain/    # Android actual implementations (ToneGenerator, SharedPreferences, window flags)
│       └── iosMain/        # iOS actual implementations (AudioToolbox, NSUserDefaults, UIApplication) + MainViewController
├── androidApp/        # Android application — thin host that calls App()
└── iosApp/            # iOS application — Xcode project; ContentView hosts App() via a UIViewController
```

The `expect`/`actual` mechanism keeps everything platform-specific behind a common interface declared in `commonMain/.../platform/`:

| Capability | `commonMain` (expect) | `androidMain` (actual) | `iosMain` (actual) |
|---|---|---|---|
| Beep | `playBeep()` | `ToneGenerator` | `AudioServicesPlaySystemSound` |
| Persistence | `Settings` | `SharedPreferences` | `NSUserDefaults` |
| Keep screen awake | `setKeepAwake()` | `FLAG_KEEP_SCREEN_ON` | `UIApplication.idleTimerDisabled` |
| Platform name | `platformName()` | `Build.VERSION.RELEASE` | `UIDevice.systemVersion` |
| Monotonic clock | `kotlin.time.TimeSource` (no shim needed — multiplatform) | | |

## Building

### Android (works on Windows / Linux / macOS)

```bash
# Debug APK -> androidApp/build/outputs/apk/debug/androidApp-debug.apk
./gradlew :androidApp:assembleDebug
```

On Windows, set `JAVA_HOME` to the Android Studio JBR first:

```bat
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
gradlew.bat :androidApp:assembleDebug
```

A `local.properties` with `sdk.dir=<path-to-Android-Sdk>` is required (git-ignored).

The app installs as `com.simon.wodtimerkmp`, a **separate** application id from the original `com.simon.wodtimer`, so both can be installed side by side.

### iOS (requires macOS + Xcode)

iOS binaries can only be compiled on macOS — this is an Apple toolchain requirement, not a project limitation. From a Mac:

```bash
open iosApp/iosApp.xcodeproj
```

Then select a development team (Signing & Capabilities) and Run. The Xcode project has a **Compile Kotlin Framework** build phase that runs
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` to build and embed the shared framework before each build, so no manual Gradle step is needed.

Installing on a **physical iPhone** needs Apple code signing — a free Apple ID works for personal 7-day sideloading; the Apple Developer Program ($99/yr) is needed for TestFlight / App Store / longer signing.

## Tech

- Kotlin 2.1.21
- Compose Multiplatform 1.8.2 (K2)
- Android Gradle Plugin 8.7.3, Gradle 8.11.1
- `minSdk` 26, `targetSdk` / `compileSdk` 36 (Android); `IPHONEOS_DEPLOYMENT_TARGET` 14.0 (iOS)
- kotlinx-coroutines 1.10.1

## License

See [LICENSE](LICENSE).
