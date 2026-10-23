#!/usr/bin/env bash
# Starts the app on the phone and waits until its first screen is shown.
#
# Rewrite in Python as soon as this script grows beyond calling other commands.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

exec adb shell am start -W -n "$PACKAGE/.MainActivity"
