#!/usr/bin/env bash
# Build, install and smoke-launch DevToys on a connected device, then report crashes.
# Usage: device-test.sh [--no-build] [--release] [-s SERIAL] [--wait SECONDS]
set -euo pipefail

PKG=com.cir0cuit.devtoys
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
BUILD=1 VARIANT=debug SERIAL="" WAIT=8

while [ $# -gt 0 ]; do
  case "$1" in
    --no-build) BUILD=0 ;;
    --release) VARIANT=release ;;
    -s) SERIAL="$2"; shift ;;
    --wait) WAIT="$2"; shift ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
  shift
done

sdk_dir() {
  if [ -f "$ROOT/local.properties" ]; then
    sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | sed 's/\\:/:/g; s/\\\\/\//g'
  fi
}
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$(sdk_dir)}}"
ADB="adb"
[ -n "$SDK" ] && [ -x "$SDK/platform-tools/adb" -o -x "$SDK/platform-tools/adb.exe" ] && ADB="$SDK/platform-tools/adb"

if [ -z "$SERIAL" ]; then
  SERIAL="$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
fi
[ -n "$SERIAL" ] || { echo "No device in 'device' state. Check 'adb devices' and USB debugging authorization." >&2; exit 1; }
A=("$ADB" -s "$SERIAL")
echo "device: $SERIAL ($("${A[@]}" shell getprop ro.product.model | tr -d '\r'), API $("${A[@]}" shell getprop ro.build.version.sdk | tr -d '\r'))"

TASK="assemble$(tr '[:lower:]' '[:upper:]' <<< "${VARIANT:0:1}")${VARIANT:1}"
if [ "$BUILD" = 1 ]; then
  (cd "$ROOT" && ./gradlew "$TASK" --console=plain -q)
fi

APK="$ROOT/app/build/outputs/apk/$VARIANT/app-$VARIANT.apk"
[ -f "$APK" ] || { echo "APK not found: $APK (a release build needs keystore.properties, see README)" >&2; exit 1; }

# Install for the foreground user only; secondary profiles (Secure Folder, work profile) reject shell access.
USER_ID="$("${A[@]}" shell am get-current-user | tr -d '\r')"
"${A[@]}" install --user "$USER_ID" -r "$APK"

"${A[@]}" logcat -c
"${A[@]}" shell am start -W -n "$PKG/.MainActivity" | tr -d '\r' | grep -E "Status|TotalTime|Error" || true
"${A[@]}" shell "sleep $WAIT"

PID="$("${A[@]}" shell pidof "$PKG" | tr -d '\r' || true)"
CRASH="$("${A[@]}" logcat -d -b crash,main -v brief | tr -d '\r' | grep -E "FATAL EXCEPTION|ANR in $PKG|Process: $PKG" -A20 || true)"

if [ -n "$CRASH" ]; then
  echo "RESULT: CRASH"
  echo "$CRASH" | head -60
  exit 3
elif [ -z "$PID" ]; then
  echo "RESULT: app process not running after ${WAIT}s and no crash logged"
  exit 4
else
  echo "RESULT: OK (pid $PID, no crash in logcat)"
fi
