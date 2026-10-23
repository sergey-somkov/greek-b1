# Greek B1

A personal Android app to prepare for the B1 Greek language exam of the University of Cyprus.

## Purpose

The app has one job: to get its single user through the B1 exam. It is not a general language course. Everything in it is measured against the exam: its four parts, its task types, its topics and its timing.

## Goals

- **Train the four parts of the exam:** listening, reading, writing and speaking, with tasks in the formats the exam uses.
- **Stay inside the B1 exam scope.** The content covers the topics and the task types that the exam uses at B1. Other topics are left out or kept to a minimum.
- **Regular daily practice.** The app records the time spent and the results of each day, and shows the progress towards the exam.
- **Use the device's own capabilities, offline where possible:**
  - text-to-speech for the listening tasks,
  - speech-to-text for the speaking tasks,
  - S Pen handwriting recognition for the writing tasks.
- **Use a cloud LLM where offline quality is not enough**, mainly to assess writing and speaking.

## Non-goals

- More than one user, accounts, or a backend of our own.
- Publication on Google Play.
- Devices other than the target phone (Samsung Galaxy S22 Ultra). The app can run on other Android phones, but it is not tested on them.
- Levels other than B1, and Greek outside the exam's scope.

## The exam

- **The target:** the B1 Greek Language Certificate of the [School of Modern Greek, University of Cyprus](https://www.ucy.ac.cy/mogr/greek-language-certification-exams/general-information/?lang=en) (UCY). The user lives in Cyprus and needs the certificate for naturalization; the Republic of Cyprus recognizes the UCY certificate for that purpose. The ΠΕΓΠ is not an option: it is the Greek naturalization exam.
- **The fallback:** the B1 Πιστοποίηση Ελληνομάθειας of the [Κέντρο Ελληνικής Γλώσσας](https://www.greek-language.gr/certification/application/index.html) (KEG), also recognized for naturalization. It is no second chance before the deadline (see below): its next B1 session in Cyprus is in May 2027, with results at the end of July. The KEG task design is the one the UCY exam copies, so the content follows the UCY exam and serves both; the differences are in [docs/exam-spec.md](docs/exam-spec.md#the-keg-exam-the-fallback).
- **B1 only.** A2 is also accepted for naturalization, but with a longer residence requirement that the user does not meet, so it is no safety net for the deadline.
- **The session:** January 2027 in Nicosia, 11–29 January; each candidate gets the exact date after the registration closes. Registration opens on **2026-11-02 at 09:00 CY time**, and the B1 places are usually gone within hours.
- **One attempt.** The naturalization application must go in before 2027-06-14, and the results come within 2.5 months of the session. The next session (September 2027) and a re-evaluation both end too late.
- **If the January attempt fails or no place is won,** the goal stays: the user still applies for naturalization, after the deadline and with less urgency. The retakes are then KEG in May 2027 (the earliest result) and UCY in September 2027, and A2 becomes an option too. The app keeps B1 as its target.
- **The format:** four parts of 25 points each. The written exam takes 2 h 30 min with no break: listening (30 min) first, then reading (50 min) and writing (70 min, two texts of about 150 words). Speaking is a separate session of 12 min per pair of candidates. The full specification is in [docs/exam-spec.md](docs/exam-spec.md).
- **The pass mark:** at least 60% in each of the four skills (listening, reading, writing, speaking; 25 points each). Failing one skill fails the whole exam.
- **The official documents** are in [docs/sources/](docs/sources/), with an index of what each one gives and what UCY does not publish: [docs/sources/README.md](docs/sources/README.md).

## How the project is built

- Claude Code writes the code and generates the learning content (texts, tasks, audio scripts).
- The user sets the direction, reviews every change, and uses the app every day.
- The work goes story by story, one git branch per story.

## Development setup

On Linux (Ubuntu 24.04), without Android Studio. The reasons for the toolchain are in [DESIGN.md](DESIGN.md#development-environment); the versions are in [app/gradle/libs.versions.toml](app/gradle/libs.versions.toml) and [app/build.gradle.kts](app/build.gradle.kts).

1. **JDK:** `sudo apt-get install openjdk-21-jdk-headless`.
2. **Android SDK** in `~/Android/Sdk`:
   - Unpack the command-line tools zip from [developer.android.com/studio](https://developer.android.com/studio) (check its SHA-256 against the page) to `~/Android/Sdk/cmdline-tools/latest`.
   - In `~/.profile`: `export ANDROID_HOME="$HOME/Android/Sdk"` and add `$ANDROID_HOME/cmdline-tools/latest/bin` and `$ANDROID_HOME/platform-tools` to `PATH`.
   - Put `--no-metrics` into `~/.androidrc`. The Android CLI (`android`) collects usage metrics by default: the commands, the option names and the names of environment variables. `~/.androidrc` applies to every call, also through `sdkmanager`, which since cmdline-tools 22.0 is a deprecated script that calls `android sdk`. On the first run the CLI unpacks itself with its own JRE into `~/.android/` (about 250 MB). `android init` (skills for agents) is not needed.
   - Install `cmdline-tools;latest`, `platform-tools`, the platform and the build tools that `app/build.gradle.kts` uses (`android sdk install …`), and accept the licenses (`sdkmanager --licenses`). Update with `android sdk update` and the CLI itself with `android update`.
3. **The phone:** enable the developer options and USB debugging, turn off Auto Blocker (it blocks commands over USB), connect by USB and authorize the machine; `adb devices` shows the phone as `device`. No udev rule is needed: the system hwdb marks the phone (`04e8:6860`) as a media player, and `70-uaccess.rules` gives the session user access to it.
4. **The signing key and the sources:** `git-crypt unlock` with the user's GPG key on the YubiKey (`2D4915021E880180A0B1963DCDF9857CB81BADBC`). That GPG key has no backup outside the YubiKey, so the exported git-crypt key is kept in the password manager. The unlocked `app/debug.keystore` has the certificate SHA-256 `59:1C:4F:C1:E1:AA:3D:BC:F2:22:A3:2E:B9:26:74:A4:7D:1A:8B:E4:24:73:37:41:C7:E8:9F:1C:DD:B7:63:4B` (`keytool -list -v -keystore app/debug.keystore -storepass android`).
5. **Build, install, run** (from `app/`):

   ```sh
   ./gradlew assembleDebug
   adb install -r build/outputs/apk/debug/greek-b1-debug.apk
   adb shell am start -n com.somkov.greek_b1/.MainActivity
   adb logcat -s GreekB1 GreekB1Probe
   ```

   Always install with `-r` over the installed app: an uninstall loses the progress data.

## Documents

- [PLAN.md](PLAN.md): the plan of the MVP as a list of stories, with their status.
- [DESIGN.md](DESIGN.md): the architecture, the decisions made, and the open questions.
- [CLAUDE.md](CLAUDE.md): the rules for Claude Code in this repository.
- [docs/exam-spec.md](docs/exam-spec.md): the B1 exam specification: the parts, the tasks, the counts, the timing and the rules, each fact with its source; and how the KEG exam differs.
- [docs/assessment-criteria.md](docs/assessment-criteria.md): the scoring of reading and listening, and the criteria and band descriptors for writing and speaking, for the LLM prompts.
- [docs/task-types.md](docs/task-types.md): the catalog of task types with their IDs, the input of a content item, the answer formats and the scoring.
- [docs/topics.md](docs/topics.md): the catalog of B1 topics and situations, and the communicative functions, with their IDs.
- [docs/sources/](docs/sources/): the official documents of the exam.
