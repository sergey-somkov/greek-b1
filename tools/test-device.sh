#!/usr/bin/env bash
# Runs the tests on the phone (app/src/androidTest/); the phone must be connected. Extra
# arguments go to Gradle, e.g. -Pandroid.testInstrumentationRunnerArguments.class=com.somkov.greek_b1.NavigationTest.
# The report is in app/build/reports/androidTests/connected/.
#
# The tests run against a copy of the app under another application ID (the build type
# "instrumented"), which Gradle installs before the run and uninstalls after it. The installed
# app and its progress data are not touched.
#
# Rewrite in Python as soon as this script grows beyond calling other commands.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

cd "$APP_DIR"
exec ./gradlew connectedInstrumentedAndroidTest "$@"
