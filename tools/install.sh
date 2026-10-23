#!/usr/bin/env bash
# Installs the debug APK over the installed app and keeps its data (the user's progress).
#
# It never uninstalls, and it takes no arguments, so no flag can be added that loses the
# data: only -r, never -d (a downgrade leaves a database that the older app cannot open).
# It refuses an APK signed with a key other than the app's: that would need an uninstall.
# adb itself refuses an update with a lower versionCode or another key.
#
# Rewrite in Python as soon as this script grows beyond calling other commands.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

if (( $# > 0 )); then
    echo "install.sh takes no arguments" >&2
    exit 2
fi
if [[ ! -f $APK ]]; then
    echo "No APK at $APK; run tools/build.sh first" >&2
    exit 1
fi

build_tools="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -n 1)"
# One line per signer ("V2 Signer: certificate SHA-256 digest: ..."); exactly one is expected.
cert="$("$build_tools/apksigner" verify --print-certs "$APK" \
    | sed -n 's/^.*Signer.* certificate SHA-256 digest: //p')"
if [[ $cert != "$CERT_SHA256" ]]; then
    echo "Refused: the APK is signed with another key (SHA-256 '$cert')." >&2
    echo "Installing it would need an uninstall, which loses the progress data." >&2
    exit 1
fi

exec adb install -r "$APK"
