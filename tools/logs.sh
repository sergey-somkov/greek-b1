#!/usr/bin/env bash
# Shows the log of the app: every message from its Linux user ID, so the crashes and the
# messages of the libraries are included, and a restart of the app keeps the filter.
# Follows the log until Ctrl+C. Extra arguments go to logcat, e.g. -d to print and exit.
#
# Rewrite in Python as soon as this script grows beyond calling other commands.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

# pm matches by substring, so the line of the package itself is picked out.
uid="$(adb shell pm list packages -U "$PACKAGE" \
    | awk -v name="package:$PACKAGE" '$1 == name { sub("uid:", "", $2); print $2 }')"
if [[ -z $uid ]]; then
    echo "$PACKAGE is not installed" >&2
    exit 1
fi

exec adb logcat --uid="$uid" "$@"
