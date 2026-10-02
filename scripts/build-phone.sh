#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
(cd "$project_dir" && ./gradlew :phone:assembleDebug --console=plain)
mkdir -p "$project_dir/dist"
cp "$project_dir/phone/build/outputs/apk/debug/phone-debug.apk" "$project_dir/dist/FireMirror-phone-debug.apk"
echo "Built $project_dir/dist/FireMirror-phone-debug.apk"
