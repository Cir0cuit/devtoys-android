---
name: release-devtoys-android
description: Release a new DevToys version - bump the version, write the changelog, push, tag, and check the signed GitHub release and the F-Droid reproducible build. Use for "release", "publish a version", "cut v1.2.3", "new DevToys release".
disable-model-invocation: true
---

# release-devtoys-android

Argument: the new version, digits and dots only (`1.0.3`). If it is missing, propose the next patch version from `versionName` in `app/build.gradle.kts` and ask.

GitHub Actions signs and publishes. Pushing the tag triggers `.github/workflows/release.yml`, which builds with JDK 21, signs with the `KEYSTORE_*` repo secrets and publishes `DevToys-<version>.apk` and `SHA256SUMS.txt`. Never sign or upload a release locally. F-Droid picks the tag up by itself (`UpdateCheckMode: Tags`, `AutoUpdateMode: Version` in `fdroiddata/metadata/com.cir0cuit.devtoys.yml`). It rebuilds the commit and ships the GitHub APK only if its build is byte-identical.

## 1. Preconditions

Stop and report if any check fails:

- `git status --porcelain` is empty, the branch is `master` and `git fetch` shows it in sync with `origin/master`.
- `gh auth status` succeeds and `gh secret list -R Cir0cuit/devtoys-android` lists `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. List names only, and never read or print secret values or keystore files.
- `v<version>` exists neither locally (`git tag -l`) nor on the remote (`git ls-remote --tags origin`).
- `<version>` is higher than the current `versionName`, compared numerically per component.
- The latest CI run on `HEAD` passed (`gh run list -R Cir0cuit/devtoys-android -L 3`).

## 2. Changelog

1. List the changes: `git log --oneline <previous tag>..HEAD`, where the previous tag is `git describe --tags --abbrev=0`.
2. Write `fastlane/metadata/android/en-US/changelogs/<new versionCode>.txt`:
   - user-facing changes only, as `- ` bullets
   - plain text, at most 500 characters
   - no commit hashes and no internal refactors
3. Show it to the user and apply their edits. F-Droid shows this file as "What's new".

## 3. Reproducibility risk

Run `git diff --stat <previous tag>..HEAD -- '*.gradle.kts' gradle/ gradle.properties settings.gradle.kts`. A change there can make the F-Droid build differ from the GitHub APK, and then F-Droid publishes nothing for this version. Risky changes are:

- new Gradle plugins, or Gradle, AGP, Kotlin or KSP upgrades
- anything that writes a timestamp, an absolute path or random data into the APK

If any of these changed, tell the user and plan step 7 as mandatory.

## 4. Bump and push

1. In `app/build.gradle.kts`, set `versionCode` to the old value + 1 and `versionName = "<version>"`.
2. Commit with subject `Release <version>`, no body, following the commit rules in CLAUDE.md. Include the bump and the changelog file.
3. Push to `origin master`.
4. Wait for CI on that commit: `gh run watch <id> -R Cir0cuit/devtoys-android --exit-status`. CI runs unit tests, lint and the debug build; local Gradle unit-test runs hang on this machine, so CI is the gate.
5. If CI fails, fix the cause in a new commit, re-run CI, and only then continue.

## 5. Tag (publishes, so confirm first)

1. Show the user the version, the changelog and the CI result, and wait for an explicit yes.
2. Tag the commit that CI passed on, and push the tag:
   ```sh
   git tag -a v<version> -m "DevToys <version>" <commit>
   git push origin v<version>
   ```
3. Watch the `Release` workflow (`gh run list -R Cir0cuit/devtoys-android --workflow Release -L 1`, then `gh run watch`).

Never delete, move or re-push a published tag. F-Droid records the commit hash, and its rebuild of that commit has to keep matching the published APK. To fix a bad release, publish a new version.

## 6. Check the GitHub release

Download into a new scratchpad directory: `gh release download v<version> -R Cir0cuit/devtoys-android -D <dir>`. Then check:

- `sha256sum -c SHA256SUMS.txt` passes.
- `apksigner verify --print-certs DevToys-<version>.apk` (latest build-tools under `$ANDROID_HOME`) prints the certificate SHA-256 digest
  `c9680cf469fc025144b51139198f5b1109e1cbbcc497b0d9da56ad9f757e000b`. Any other digest means the wrong key: stop and tell the user.
- `aapt2 dump badging` shows the new `versionCode` and `versionName`.

## 7. Check F-Droid reproducibility

This step is mandatory when step 3 found risky changes. Otherwise it is recommended. It takes about 10 minutes and needs Docker Desktop to be running.

```sh
bash .claude/skills/release-devtoys-android/fdroid-verify.sh <version> <versionCode> <commit>
```

The script runs the steps of fdroiddata's `fdroid build` CI job in `registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie`, against the published GitHub APK.

- **Exit 0** (`successfully verified`): F-Droid will ship this version.
- **Any other exit:** the script prints the differing files inside the APK. Find what makes them differ, fix it, and release a new patch version. F-Droid skips the failed version and never re-signs it with its own key.

## 8. Report

Report:

- the release URL and the APK checksum
- the signature check and the F-Droid check, or say that the F-Droid check was skipped
- that F-Droid's update bot will add the version within a few days, with no merge request needed
- whether the website (`cir0cuit.dev/devtoys.html`, homepage card) or the README needs updating, for example when the tool count changed

## Background

- **Signing key:** `C:\Users\barti\Cir0cuit\signing\` (outside every repo). Every release must be signed with it, or installed apps cannot update.
- **Tags F-Droid ignores:** tags that are not `v` + digits and dots, such as `v1.1.0-beta`. The release workflow still publishes those on GitHub.
- **F-Droid submission:** https://gitlab.com/fdroid/fdroiddata/-/merge_requests/52979. Before it is merged, F-Droid publishes nothing. After the merge, the recipe lives in fdroiddata, so never edit it for a normal release.
