# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A single-Activity Android app (Kotlin + Jetpack Compose, `com.leida.lifecalendar`) that renders a
life calendar as one pillar per year. It is a **port of a design prototype** — option 2a
(年柱原型 · 深色) of `人生周历.dc.html` from the linked Claude Design project — and fidelity to that
prototype is the project's governing constraint (see *Fidelity rules* below). All UI text is Chinese.

## Build & run

```bash
./gradlew installDebug        # build + install debug on a connected device/emulator
./gradlew :app:assembleDebug  # compile check only
./gradlew :app:assembleRelease
adb shell am start -n com.leida.lifecalendar.debug/com.leida.lifecalendar.MainActivity
```

Requires an Android SDK (`local.properties` → `sdk.dir`, or `ANDROID_HOME`) and JDK 17+ (CI uses 21).
A bare cloud container has no SDK, so a Gradle build may be impossible there — reason about code
statically and let the release workflow do the compiling.

There are **no tests and no test source sets** in this repo, and no lint/format configuration beyond
Gradle defaults. Do not claim a change is verified by a build you did not actually run.

The debug build type carries `applicationIdSuffix = ".debug"`, so debug and release install side by
side under different package names — deliberate, and load-bearing for the updater (a release APK can
never replace a debug install; the keys differ and Android refuses the swap).

## Dependency policy

**No third-party dependencies.** Only AndroidX/Compose. Persistence is `SharedPreferences` + a JSON
blob via `org.json`; HTTP is `HttpURLConnection`. Reaching for OkHttp, Retrofit, kotlinx-serialization,
Room, or a DI framework contradicts an explicit design decision — don't add one without being asked.

## Architecture

Two `AndroidViewModel`s, Compose `mutableStateOf` for all state, no navigation library, no repository
layer, no DataStore/Flow.

* `MainActivity.kt` is the whole shell: edge-to-edge window, header, three-tab enum (`人生` /
  `里程碑` / `设置`), the year `ModalBottomSheet`, the toast, and the update dialog. Tab switching is a
  `when` on local state; each tab keeps its own `ScrollState`.
* `LifeViewModel` owns `Settings` + `List<Milestone>` and writes through to `Store` on **every**
  mutation (there is no explicit save). `calc` is a fresh `LifeCalc` derived from settings.
* `data/Model.kt` — `LifeCalc` is the arithmetic core, ported one-for-one from the prototype's
  `calc()`. Everything on screen derives from it.
* `data/Store.kt` — the only persistence. Milestone JSON uses the key `"w"` for the week index;
  keep that if you touch the format, or existing installs lose their marks.
* `ui/Theme.kt` — design tokens (colors, `sans()`/`serif()`/`kicker()` type scale) lifted verbatim
  from the `.dc.html`. Use these, not `MaterialTheme`. The palette is a fixed dark one; the app
  ignores the system theme by design.
* `ui/Common.kt` — `Snap` / `Settle` easings that mirror the prototype's CSS cubic-beziers, plus the
  shared card and pill switch.

## Fidelity rules (do not "fix" these)

`LifeCalc` reproduces the prototype's formulas including its quirks. These are intentional and match
the design canvas:

* A week is a flat 7 days from the birth date, **never** a calendar week.
* `monthsLeft = span * 12 - floor(livedWeeks / 4.345)`.
* Stage boundaries are 18 and 60; track tints are `Bone` at 7% / 10% / 5% alpha.
* Marking a year stores the milestone at its **middle** week (`year * 52 + 26`).
* The design's fixed frame paddings are replaced by real status/navigation-bar insets — that
  substitution is the one deliberate divergence.

## The in-app updater (`data/update/` + `ui/UpdateDialog.kt`)

A self-contained state machine: `UpdateState` (Idle → Checking → Available → Downloading →
ReadyToInstall / UpToDate / Failed) driven by `UpdateViewModel`, against this repo's GitHub Releases
(anonymous — the repo is public, nothing secret is in the APK).

Behaviour that is easy to break:

* **Cold start** check runs at most once per 24h and is *silent* unless an update exists; a failed
  check deliberately does **not** consume the daily slot. **Manual** check (设置 → 检查更新) always
  hits the network and always reports its outcome.
* `download()` awaits `cancelAndJoin()` on the previous job — a cancelled coroutine does not
  interrupt a blocking socket read, and two attempts appending to the same `.part` file splice
  duplicate bytes into the APK, which the size check then rejects permanently.
* `UpdateVersion` is pure Kotlin (no Android) and compares the release tag against
  `BuildConfig.VERSION_NAME`: numeric segments left-to-right, pre-release ranks below the final
  release, anything unparseable is "not newer". Get this wrong and the app either nags forever or
  never updates.
* The data layer is string-free: failures are `UpdateError` enum values, mapped to Chinese text in
  `UpdateDialog.kt`. `highlights()` there shows only the hand-written release-note prefix, cutting
  everything from GitHub's auto-generated `##` / `**Full Changelog**` onward.
* Install path is `installBlocker()` → `canInstall()` → unknown-sources settings intent →
  `installIntent()`, handing the APK over as a `content://` URI via `FileProvider`
  (`res/xml/file_paths.xml`, cache dir `updates/`). Silent self-update is not possible on Android.

`BuildConfig.UPDATE_REPO` and `UPDATE_ASSET_NAME` in `app/build.gradle.kts` must stay in sync with
what the release workflow publishes.

## Releasing

`.github/workflows/android-release.yml` builds a signed release APK and publishes it. Either push a
`v*` tag, or run the workflow manually on `main` — the manual path derives the tag from `versionName`
in `app/build.gradle.kts`, so the tag can never disagree with the `BuildConfig.VERSION_NAME` inside
the APK, and it fails if that tag already exists.

**Bump both `versionName` and `versionCode`** in `app/build.gradle.kts` before releasing; Android
compares `versionCode` and refuses to install anything lower. The workflow's optional *release notes*
input is exactly what the in-app dialog shows.

Signing comes from `SIGNING_KEYSTORE_FILE` / `_PASSWORD` / `SIGNING_KEY_ALIAS` / `SIGNING_KEY_PASSWORD`
env vars (repo secrets in CI). With no keystore present the release build is left **unsigned** rather
than failing — which also renames the output to `app-release-unsigned.apk` and breaks the workflow's
upload step. The keystore is not in the repo and cannot be regenerated: a different key means no
existing install can ever be updated.
