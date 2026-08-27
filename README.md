# 人生周历 · Life Calendar

Native Android implementation of option **2a (年柱原型 · 深色)** from the Claude Design project
[人生进度展示应用](https://claude.ai/design/p/5ad81501-a698-471d-8e88-7f19b7bc70f0) — the dark
year-pillar prototype from turn 2 of `人生周历.dc.html`.

One pillar per year of life. The filled height is the weeks already spent in that year; the year
you are living in is the only clay-coloured one. Tap a pillar to open that year's 52 weeks; long
press to leave a mark on it.

## Stack

Kotlin · Jetpack Compose · no third-party dependencies. State lives in `SharedPreferences`
(JSON via `org.json`), so the app is fully offline and has no database.

| | |
|---|---|
| minSdk / targetSdk / compileSdk | 26 / 34 / 36 |
| Gradle · AGP · Kotlin | 8.14.3 · 8.13.0 · 2.2.20 |
| Compose BOM | 2024.12.01 |
| Fonts | Inter Tight + Source Serif 4, bundled in `res/font` (Chinese falls back to the system CJK face, as it does in the browser) |

## Build & run

```bash
./gradlew installDebug
adb shell am start -n com.leida.lifecalendar/.MainActivity
```

`local.properties` points at the Android SDK; change `sdk.dir` if yours lives elsewhere.

## Layout

```
app/src/main/java/com/leida/lifecalendar/
  MainActivity.kt        root shell — header, tab bar, year sheet, toast
  LifeViewModel.kt       settings + milestones, persisted on every change
  data/
    Model.kt             LifeCalc — the prototype's arithmetic, ported one-for-one
    Store.kt             SharedPreferences persistence
  ui/
    Theme.kt             design tokens (colours, type scale) from the .dc.html
    LifeScreen.kt        年柱 grid
    MilestonesScreen.kt  里程碑 list
    SettingsScreen.kt    设置 — birth date, span, switches
    YearSheet.kt         the bottom sheet a pillar opens into
    Common.kt            card + pill switch
```

## Notes on fidelity

* `LifeCalc` reproduces the prototype's formulas exactly, including its quirks — a week is a flat
  7 days from the birth date (never a calendar week), and months remaining are
  `span * 12 - floor(livedWeeks / 4.345)`. The numbers on screen match the design canvas.
* Stage boundaries are 18 and 60; the three track tints are the design's `rgba(245,241,235,·)`
  values at 7% / 10% / 5%.
* The staggered pillar entrance and the 700ms fill are the `.lr` keyframes and the
  `cubic-bezier(0,0,0,1)` height transition from the source.
* The design's 58px / 30px frame paddings are replaced by real status-bar and navigation-bar
  insets, so the app is edge-to-edge on any device.
* Fixed dark palette — the design commits to one look, so the app ignores the system theme.
