---
name: device-test
description: Build DevToys, install it on a connected Android device or emulator over ADB, launch it and report crashes from logcat. Use for "install on my phone", "device test", "smoke test on device", "does it crash on the device".
disable-model-invocation: true
---

# device-test

Run the bundled script from the repo root (Git Bash on Windows):

```sh
bash .claude/skills/device-test/device-test.sh [--no-build] [--release] [-s SERIAL] [--wait SECONDS]
```

- Default: `assembleDebug`, install on the first device in `adb devices`, launch `MainActivity`, wait 8 s, then check logcat (`crash` and `main` buffers) for `FATAL EXCEPTION` / `ANR` of `com.cir0cuit.devtoys`.
- `--no-build` reinstalls the existing APK. `--release` installs the release APK, which only exists signed when `keystore.properties` is configured (see README).
- Exit codes: 0 OK, 1 no device / no APK, 3 crash found (stack trace printed), 4 process not running and no crash logged.

Notes:
- The script installs for the foreground user (`am get-current-user`). Phones with Secure Folder or a work profile reject `pm` commands that target every user with `SecurityException: Shell does not have permission to access user 150`.
- adb is taken from `ANDROID_HOME`, `ANDROID_SDK_ROOT` or `sdk.dir` in `local.properties`, before falling back to `adb` on PATH. Mixing two adb versions restarts the adb server and drops the connection.
- Gradle needs `JAVA_HOME` set to a JDK 17-21 (see CLAUDE.md).
- `adb install` failing with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` means a build with a different signature (for example the old Play Store release) is installed. Ask the user before uninstalling it, because uninstalling deletes the app's data.

After a crash, read the stack trace, find the frame in `com.cir0cuit.devtoys`, and fix the cause. For interactive checks (tapping through tools, screenshots), use the `mobile-mcp` tools when they are available.
