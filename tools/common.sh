# Shared settings of the scripts in tools/. Sourced by them, not run.
#
# The scripts only call gradlew and adb. Rewrite them in Python as soon as they grow
# beyond that: shell is fine for a list of commands, not for logic.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="$REPO_ROOT/app"
PACKAGE="com.somkov.greek_b1"
APK="$APP_DIR/build/outputs/apk/debug/greek-b1-debug.apk"

# The SHA-256 of the certificate in app/debug.keystore (see README.md). The installed app
# accepts updates only with this key; any other key needs an uninstall.
CERT_SHA256="591c4fc1e1aa3dbcf222a32eb92674a47d1a8be424733741c7e89f1cddb7634b"
