# DevToys for Android

An offline collection of developer utilities: Jetpack Compose, Hilt, Kotlin 2.0.21, AGP 8.7.2, Gradle 8.10.2, minSdk 26, targetSdk 35. It is an open-source project under Apache-2.0, distributed through GitHub releases (https://github.com/Cir0cuit/devtoys-android). It is no longer published on the Play Store.

## Build and test

- Set `JAVA_HOME` to JDK 17-21. The default `java` on PATH may be JDK 25, which Gradle 8.10.2 does not support.
  Git Bash example: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot"`.
- `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew assembleRelease` (R8-minified; unsigned unless signing is configured, see README).
- Run only one Gradle build at a time in this checkout. Parallel builds have failed in `packageDebug` and left test workers hanging. Subagents must not run Gradle concurrently.
- Before calling a change done, run `testDebugUnitTest lintDebug assembleRelease`. Lint errors fail the build, and the release build catches R8 problems.
- Manual check on a device: the `/device-test` skill. The `mobile-mcp` MCP server can drive the device interactively.

## Layout

- `app/`: `MainActivity`, `MainScreen` (tab and tool host, with no NavHost), Settings, Favorites, DataStore `SettingsRepository`, `LegalTexts`.
- `core/ui/`: shared composables (`ToolScaffold`, `MonoTextField`, `NumberedTextField`, `OutputPanel`).
- `tools/base/`: the `Tool` interface, `ToolDescriptor`, `ToolCategory` and `DefaultToolRegistry`. Tool title and description strings live here.
- `tools/<group>/`: one file per tool (`encoders`, `text`, `data`, `generators`, `crypto`, `developer`, `reference`). A new tool must also be added to the `DefaultToolRegistry` list in `app/.../di/AppModule.kt`.
- JVM unit tests (JUnit 4) are in `tools/*/src/test`. Pure logic belongs in top-level `internal` functions so it can be tested without Android. Android's built-in `org.json` and `android.util.*` are stubs on the JVM.

## Conventions

- Tool screens are hosted in a `SaveableStateHolder`, so `viewModel()` is scoped to the Activity. User input must survive rotation, but large text or binaries must not go into `rememberSaveable` (Binder limit). Heavy work runs off the main thread with debounce and cancellation. The `compose-state-reviewer` agent checks these rules. Run it after changing tool screens.
- No network access, analytics or telemetry. The app has no INTERNET permission, and the privacy policy in `LegalTexts` promises zero data collection.
- New dependencies must be maintained, Apache-2.0/MIT/BSD-compatible, pinned and on Maven Central or Google. List them in the PR description.
- Never read, create or commit `keystore.properties`, `*.jks`, `*.keystore` or `local.properties`. A hook in `.claude/settings.json` enforces this.
- Commit messages: imperative subject of 72 characters or less with no trailing period, and an optional bullet body listing what changed. No AI attribution.
