#!/usr/bin/env bash
# Runs the unit tests on the JVM; the phone is not needed. Extra arguments go to Gradle,
# e.g. --tests '*CountdownTest*'. The report is in app/build/reports/tests/testInstrumentedUnitTest/.
# AGP makes unit tests only for the tested build type, which is the one of the device tests
# (see app/build.gradle.kts); its code is the same as the debug app's.
#
# Rewrite in Python as soon as this script grows beyond calling other commands.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

cd "$APP_DIR"
exec ./gradlew testInstrumentedUnitTest "$@"
