#!/usr/bin/env bash
# Builds the debug APK into app/build/outputs/apk/debug/. Extra arguments go to Gradle.
#
# Rewrite in Python as soon as this script grows beyond calling other commands.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

cd "$APP_DIR"
exec ./gradlew assembleDebug "$@"
