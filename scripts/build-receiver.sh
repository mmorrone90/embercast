#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
receiver_dir="$project_dir/receiver"
patch_file="$project_dir/patches/receiver-embercast.patch"

git -C "$project_dir" submodule update --init --recursive receiver

if git -C "$receiver_dir" apply --reverse --check "$patch_file"; then
    echo "EmberCast receiver patch is already applied."
elif git -C "$receiver_dir" apply --check "$patch_file"; then
    git -C "$receiver_dir" apply "$patch_file"
else
    echo "Receiver source differs from the pinned revision. Review local changes before building." >&2
    exit 1
fi

if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
    export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

(cd "$receiver_dir" && ./gradlew :app:assembleDebug --console=plain)

mkdir -p "$project_dir/dist"
cp "$receiver_dir/app/build/outputs/apk/debug/app-debug.apk" "$project_dir/dist/EmberCast-receiver-debug.apk"
echo "Built $project_dir/dist/EmberCast-receiver-debug.apk"
