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
  UpdateViewModel.kt     the updater state machine — check, download, hand off to the installer
  data/
    Model.kt             LifeCalc — the prototype's arithmetic, ported one-for-one
    Store.kt             SharedPreferences persistence
    update/
      UpdateVersion.kt   tag-vs-BuildConfig version comparison (semver-ish, pure Kotlin)
      UpdateModels.kt    UpdateState / UpdateError
      UpdateService.kt   GitHub Releases lookup + resumable APK download
      ApkInstaller.kt    FileProvider handoff, and why an install would be refused
  ui/
    Theme.kt             design tokens (colours, type scale) from the .dc.html
    LifeScreen.kt        年柱 grid
    MilestonesScreen.kt  里程碑 list
    SettingsScreen.kt    设置 — birth date, span, switches, 检查更新
    YearSheet.kt         the bottom sheet a pillar opens into
    UpdateDialog.kt      the updater's only screen
    Common.kt            card + pill switch
```

## Releasing

`.github/workflows/android-release.yml` builds a signed release APK and publishes it to GitHub
Releases. Two ways to fire it:

* **Tag** — `git tag v1.1.0 && git push origin v1.1.0`
* **Actions → Android Release → Run workflow** on `main` — derives the tag from `versionName` in
  `app/build.gradle.kts`, so the tag can never disagree with the `BuildConfig.VERSION_NAME` inside
  the APK. The optional *release notes* input is what the in-app update dialog shows.

Bump **both** `versionName` and `versionCode` in `app/build.gradle.kts` before releasing — Android
compares `versionCode` and refuses to install anything lower.

Signing secrets live on the repo (`SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`,
`SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`). The keystore itself is **not** in this repo and must
be backed up somewhere durable: lose it and every future update fails to install, because Android
never replaces an app with one signed by a different key.

## In-app updates

On a cold start — at most once every 24h — the app asks
`GET /repos/<owner>/<repo>/releases/latest` whether there is a newer tag than its own
`VERSION_NAME`. If there is, a dialog offers the download; 设置 → 检查更新 does the same check on
demand and always reports the outcome. The repo is public, so the request is anonymous and nothing
secret is baked into the APK.

The download resumes across attempts (`Range`), verifies the finished size against the release
metadata, and hands the APK to the system installer through a `FileProvider`. There is no silent
self-update on Android: the user still confirms in the system installer, and the app needs the
per-app "install unknown apps" permission, which the dialog links to when it is missing.

Debug builds carry `applicationIdSuffix = ".debug"` so they install alongside a release build
instead of occupying its slot — without that, a release APK can never replace a locally installed
debug one (different signing keys, and Android refuses the swap outright).

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
