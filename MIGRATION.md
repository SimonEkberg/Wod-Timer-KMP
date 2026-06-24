# Migrating WOD Timer to Kotlin Multiplatform — the sketch

This document maps the existing Android app (`SimonEkberg/wod-timer`) onto this KMP project and lays out the
port plan. The headline: **~80–90% of the app moves to `commonMain` essentially unchanged**, and the rest is a
handful of small `expect`/`actual` platform shims (already written here) plus two thin host apps.

## Why KMP fits this app so well

The original app already has a clean separation between **pure logic** and **Android plumbing**. The timer engine,
the workout model, and the round-summary math have almost no Android dependencies — they are ordinary Kotlin. Compose
is already the UI layer, and Compose Multiplatform runs the *same* Compose code on iOS. So the migration is mostly
"move files into `commonMain` and swap five platform calls for `expect` functions."

## File-by-file migration map

Source paths are relative to the original app's `app/src/main/java/com/simon/wodtimer/`.
Destination paths are relative to this project's `shared/src/`.

### Pure logic & model — move to `commonMain` as-is (no changes)

| Original | Destination | Notes |
|---|---|---|
| `model/Segment.kt` | `commonMain/.../model/` | pure data |
| `model/TimerPhase.kt` | `commonMain/.../model/` | pure data + enum |
| `model/Workout.kt` | `commonMain/.../model/` | uses `java.util.UUID` → swap for a multiplatform id (see below) |
| `model/WorkoutFormat.kt` | `commonMain/.../model/` | pure string parsing |
| `model/WorkoutTemplate.kt` | `commonMain/.../model/` | pure |
| `model/QuickMode.kt` | `commonMain/.../model/` | pure |
| `model/RunSpec.kt` | `commonMain/.../model/` | pure |
| `model/TimerPlan.kt` | `commonMain/.../model/` | pure |
| `model/RoundSplit.kt` | `commonMain/.../model/` | pure data |
| `model/RoundBreakdown.kt` | `commonMain/.../model/` | pure logic (`RoundBreakdowns.grouped`, etc.) |
| `data/SeedWorkouts.kt` | `commonMain/.../data/` | pure data (the 82 workouts) |

**Only adjustment:** `Workout.kt` uses `java.util.UUID.randomUUID()`. Replace with a multiplatform id —
either `kotlin.uuid.Uuid` (stable in recent Kotlin) or a tiny `expect fun newId(): String`. One-line change.

### Engine & UI logic — move to `commonMain` with small swaps

| Original | Destination | Change needed |
|---|---|---|
| `ui/TimerEngine.kt` | `commonMain/.../ui/` | Replace `android.os.SystemClock.elapsedRealtime()` with `kotlin.time.TimeSource.Monotonic` (already multiplatform — no shim). Coroutines are already multiplatform. |
| `ui/RunState.kt` | `commonMain/.../ui/` | pure data |
| `ui/Format.kt` | `commonMain/.../ui/` | `String.format` → use the manual formatting already added (`formatMmSsCc` here builds the string without `String.format`, which is JVM-only) |
| `ui/Beeper.kt` | split | logic stays common; the actual tone call becomes `playBeep()` (**shim — done**) |
| `ui/WorkoutViewModel.kt` | `commonMain/.../ui/` | drop AndroidX `ViewModel`; use a plain class holding `StateFlow` + a `CoroutineScope`, or `androidx.lifecycle` KMP ViewModel (now multiplatform). Persistence calls go through `Settings` (**shim — done**) |

### Compose UI — move to `commonMain` (shared on both platforms)

| Original | Destination | Change needed |
|---|---|---|
| `ui/HomeScreen.kt` | `commonMain/.../ui/` | Material3 + Compose are multiplatform. `Icons.*` → add `compose.materialIconsExtended`. |
| `ui/StandardScreen.kt` | `commonMain/.../ui/` | pure Compose |
| `ui/RunScreen.kt` | `commonMain/.../ui/` | immersive nav-bar hide is Android-only → wrap in `setKeepAwake()` + an `expect fun setImmersive(Boolean)` (Android: window insets controller; iOS: no-op / prefersStatusBarHidden). Keep-awake shim already done. |
| `ui/WorkoutEditScreen.kt` | `commonMain/.../ui/` | pure Compose |
| `ui/WorkoutListScreen.kt` | `commonMain/.../ui/` | pure Compose (the delete-confirm dialog is plain Compose) |
| `ui/TimerDialog.kt`, `ui/EmomDialog.kt` | `commonMain/.../ui/` | pure Compose |
| `MainActivity.kt` (the `AppRoot` composable + nav) | `commonMain/.../App.kt` | the navigation state machine is pure Compose; the `BackHandler` is Android-only → `expect`/`actual` (Android: `androidx.activity.compose.BackHandler`; iOS: swipe-back gesture or a no-op, since iOS has no system back button) |

### Platform glue — `expect`/`actual` (all written in this scaffold)

| Concern | `commonMain` | `androidMain` | `iosMain` | Status |
|---|---|---|---|---|
| Beep / tones | `playBeep()` | `ToneGenerator` | `AudioServicesPlaySystemSound` | ✅ done |
| Persist workouts/sound flag | `Settings` | `SharedPreferences` | `NSUserDefaults` | ✅ done (extend with `getString`/`putString` for the workout JSON) |
| Keep screen awake | `setKeepAwake()` | `FLAG_KEEP_SCREEN_ON` | `idleTimerDisabled` | ✅ done |
| Platform name | `platformName()` | `Build` | `UIDevice` | ✅ done |
| Monotonic clock | `TimeSource.Monotonic` | — | — | ✅ multiplatform, no shim |
| Immersive / status bar | `setImmersive()` | insets controller | `prefersStatusBarHidden` | ⬜ to add during UI port |
| System back | `BackHandler` | activity BackHandler | gesture / no-op | ⬜ to add during UI port |

### App hosts — already scaffolded

| File | Role |
|---|---|
| `androidApp/.../MainActivity.kt` | wires `AndroidPlatform.appContext`/`currentActivity`, calls `setContent { App() }` |
| `iosApp/iosApp/ContentView.swift` | hosts `MainViewController()` (the shared Compose UI) in SwiftUI |
| `shared/iosMain/.../MainViewController.kt` | `ComposeUIViewController { App() }` |

## Step-by-step port plan (follow-up work)

1. **Move the model package** (`model/`, `data/SeedWorkouts.kt`) into `commonMain`. Swap `UUID` → multiplatform id. Build `:shared`.
2. **Extend `Settings`** with `getString`/`putString` and port `data/WorkoutRepository.kt` to use it (replaces `SharedPreferences` JSON storage). The JSON itself can use `kotlinx.serialization` (multiplatform) instead of manual encoding.
3. **Port the engine** (`TimerEngine`, `RunState`, `Format`) to `commonMain`; replace `SystemClock` with `TimeSource`. Unit-test it in `commonTest` (runs on both platforms).
4. **Port the ViewModel** — plain `StateFlow` holder or multiplatform `androidx.lifecycle.ViewModel`.
5. **Port the Compose UI** screen by screen into `commonMain`. Add the `setImmersive` and `BackHandler` shims. Replace the current demo `App()` with the real `AppRoot` navigation.
6. **Add the launcher icons** — Android adaptive icon (as today) + iOS `AppIcon.appiconset` (1024px master).
7. **Build & run** Android here; build & run iOS on a Mac (Xcode opens `iosApp.xcodeproj`, select a team, Run).

## Dependencies to add during the port

- `org.jetbrains.compose.material:material-icons-extended` (the `Icons.*` used across the UI)
- `org.jetbrains.kotlinx:kotlinx-serialization-json` + the serialization plugin (for workout persistence; replaces manual SharedPreferences JSON)
- optionally `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` (multiplatform ViewModel)

## What genuinely cannot be shared

- **The iOS build itself** must run on macOS + Xcode (Apple toolchain requirement).
- **App icons / launch screens** are per-platform asset formats.
- **~5 small platform calls** (the shims above) — by design, a few dozen lines each.

Everything else — the timer logic, the round-summary math, the 82 seeded workouts, and the entire Compose UI —
is written once in `commonMain` and runs natively on both Android and iOS.
