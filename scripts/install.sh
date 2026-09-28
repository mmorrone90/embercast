#!/usr/bin/env bash
set -euo pipefail
if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
    echo 'Usage: scripts/install.sh FIRE_TV_IP [APK_PATH]' >&2
    exit 2
fi
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
device="$1"
case "$device" in *:*) ;; *) device="${device}:5555" ;; esac
apk="${2:-$project_dir/app/build/outputs/apk/debug/app-debug.apk}"
if ! command -v adb >/dev/null; then
    echo 'Add Android SDK platform-tools to PATH first.' >&2
    exit 1
fi
if [ ! -f "$apk" ]; then
    echo "APK not found: $apk. Build with ./gradlew :app:assembleDebug" >&2
    exit 1
fi
adb connect "$device"
if [ "$(adb -s "$device" get-state)" != 'device' ]; then
    echo 'Authorize this Mac on the Fire TV and try again.' >&2
    exit 1
fi
api="$(adb -s "$device" shell getprop ro.build.version.sdk | tr -d '\r')"
case "$api" in ''|*[!0-9]*) echo 'Could not identify Android API level.' >&2; exit 1 ;; esac
if [ "$api" -lt 24 ]; then
    echo 'FireMirror requires Android API 24 or newer.' >&2
    exit 1
fi
adb -s "$device" install -r "$apk"
adb -s "$device" shell am start -n dev.firemirror.tv/.MainActivity
