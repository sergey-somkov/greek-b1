# Technical Design

The architecture of the app: the main ideas, the decisions made, and the open questions. See [README.md](README.md) for the purpose and [PLAN.md](PLAN.md) for the plan. The details of the implementation and the local decisions are in the comments of the code; this document links to them.

## Status

Story 1 settled the exam (see [README.md](README.md#the-exam)), the phone and the toolchain. Story 2 described the exam format and fixed the catalogs that the content refers to. Story 3 built the skeleton of the app: the navigation, the database, the tests and the scripts. Story 4 built the content pipeline: the format of the items, the catalog of the IDs, the validator, the bundle in the APK and its load into the database, with a sample set of four items. Story 5 built the tracking: the user tables of the sessions, the session recorder, the settings with the daily goal, the progress screen with the readiness and the time per day, and today on home. The rest is still a first draft, written before the features.

## Constraints

- **One user, one phone:** Samsung Galaxy S22 Ultra (`SM-S908B`, `arm64-v8a`, built-in S Pen), Android 16 (API level 36), One UI 8.0. No backend of our own, no accounts.
- **A fixed deadline:** the exam session of January 2027, with one attempt (see [README.md](README.md#the-exam)).
- **The progress data is never lost.** It exists only on the phone (until the backup of story 18), so the signing key, the application ID, the install script, the database migrations and the tests are all built so that nothing removes it.
- **Offline first.** Practice works without a network. Only the assessment of writing and speaking needs the network, and it can wait until the network is back.
- **The exam is the scope.** Each piece of content refers to a task type and a B1 topic of the catalogs of story 2.
- **Claude Code writes the code and the content; the user reviews the code.** The user is learning Greek and cannot find the errors in the Greek of the content; another model reviews it (see [Content](#content)). The code must be easy to understand: few layers, no clever abstractions.
- **Use libraries, do not reinvent them.** A well-known library is preferred to code of our own that does the same work.

## Overview

```
repository
├── app/        Android app (Kotlin, Jetpack Compose), a self-contained Gradle build
├── content/    source of the learning content: the catalog of the IDs and the items, in YAML
├── tools/      scripts: build, install and tests; the content scripts; later the audio generation
└── docs/       exam specification, catalogs and the official sources
```

Two flows:

1. **Content flow (on the development machine).** Claude Code writes the content into `content/`. The validator checks its form, and another model reviews its Greek. The audio script makes the audio files. The build packages the content and the audio into the APK.
2. **Practice flow (on the phone).** The app loads the content into its database, runs the exercises, records the sessions and the results, and sends writing and speaking answers to the cloud LLM for assessment.

## Exam specification

The exam is written down in `docs/`: [exam-spec.md](docs/exam-spec.md) (the structure, the timing, the rules, and how the KEG exam differs), [assessment-criteria.md](docs/assessment-criteria.md), [task-types.md](docs/task-types.md) and [topics.md](docs/topics.md). UCY publishes little, so the gaps are filled from the KEG B1 exam, whose task design UCY shares; each fact names its source.

- **Stable IDs** for the task types, topics, situations, functions and criteria. An ID in use is never renamed or reused, since the user tables keep results and mistakes by ID. The IDs and their data live in the catalog in `content/catalog/` (see [Content](#content)); the tables of these documents are written from it.
- **Every content item is in the exam form** (the counts and the shape of its task type), so any item can go into a mock exam. Practice changes how an item is run, not the item.
- **The official sources are part of the repository**, encrypted with git-crypt: `docs/sources/` holds copyrighted sample papers, and the repository is public. Only the index [docs/sources/README.md](docs/sources/README.md) stays readable.

## The app

- **Language and UI:** Kotlin, Jetpack Compose, Material 3 with the phone's dynamic colors.
- **The language of the UI is English.** The tasks are in Greek, as in the exam. Where training shows a translation of Greek (a dictionary, for example), it is Greek to Russian, the user's language.
- **Structure:** one Gradle module, the only project of the build in `app/`; packages by feature (`home`, `reading`, `listening`, `writing`, `speaking`, `progress`, `settings`, `diagnostics`) and a few shared packages (`data`, `theme`, `content`, `session`; later `speech`, `ink`, `assessment`).
- **Navigation:** Navigation 3; home is a hub with a tile for each of the other screens, and back returns to it (see [Navigation.kt](app/src/main/kotlin/com/somkov/greek_b1/Navigation.kt)).
- **State:** a `ViewModel` per screen with `StateFlow`; Compose screens observe it. A ViewModel lives as long as its screen is on the back stack.
- **Data:** Room (SQLite) on the phone's own SQLite. Two kinds of tables:
  - content tables, rewritten from the APK's content on each content update;
  - user tables, never rewritten, linked to content by stable IDs: the sessions and the checks of their answers (see [SessionTables.kt](app/src/main/kotlin/com/somkov/greek_b1/session/SessionTables.kt), with their rules); later the assessments and the recordings.
- **The database is migrated, never dropped**, and every schema version is exported and tested; the rules are in [Migrations.kt](app/src/main/kotlin/com/somkov/greek_b1/data/Migrations.kt).
- **Settings:** Jetpack DataStore (Preferences), the standard library for them, in a file of its own, not the database (see [Settings.kt](app/src/main/kotlin/com/somkov/greek_b1/settings/Settings.kt)). A setting never changed reads as its default, which is not written to the file; a stored value outside its range reads as the nearest bound; a corrupt file is replaced with the defaults, since the settings are cheap to set again and are not progress data. The first setting is the daily goal.
- **Time:** the code that reads the time takes a `java.time.Clock`: `PhoneClock` in the app, which follows the time zone of the phone across a trip (see [SessionRecorder.kt](app/src/main/kotlin/com/somkov/greek_b1/session/SessionRecorder.kt)), a fixed clock in the tests. The times are stored in UTC.
- **Dependency injection:** manual, with a small app container ([AppContainer.kt](app/src/main/kotlin/com/somkov/greek_b1/AppContainer.kt)), unless the app grows enough to need Hilt.
- **SDK levels:** `minSdk` is the phone's API level, so no compatibility code for older Android versions. The values and their reasons are in [app/build.gradle.kts](app/build.gradle.kts).
- **The signing key is part of the repository**, encrypted with git-crypt, because a new key forces an uninstall. Details in [app/build.gradle.kts](app/build.gradle.kts).
- **Diagnostics:** a screen under the settings (package `diagnostics`) shows the content version and the items per part, so the phone shows which content it has, and probes what a system update or a new phone changes: the TTS voices, the recognizer languages, the ink model. The probes keep the findings about the phone in their comments.

## Content

- **Source format:** YAML in `content/`, one item per file: `content/items/<part>/<type>/<id>.yaml`, where the ID is the file name, of the form `<task type>.<NNN>` (`reading.true_false.001`). Each item names its task type and its situation (see [Exam specification](#exam-specification)). YAML, because long Greek texts read as they are in its block scalars; the app never reads it. The format is in [content/README.md](content/README.md).
- **The catalog** of the task types, topics, situations, functions and criteria is in `content/catalog/`, the one source of these IDs and of their names. The tables of the docs are written from it by a script.
- **Stable item IDs:** an item is never deleted or renamed; it is marked `retired` and the app stops offering it. A change of the questions or the key of an item in use makes a new item.
- **Audio and pictures** are found by the ID of their item: `content/audio/<id>.<ext>`, `content/pictures/<id>.<ext>`.
- **Validation:** a Python script ([tools/content.py](tools/content.py)) checks the format (a Pydantic model per task type), the IDs, the references to the catalog, the counts and lengths of each task type, and that the tables of the docs match the catalog; a wrong count or shape is an error, a length or a balance of answers out of range a warning. A second command reports the coverage: the items per topic, per situation, and per topic and task type; story 17 adds the targets.
- **Delivery:** the build runs the validator and turns the content into one JSON bundle in the APK's assets, so an APK never carries content that failed the validation (see [app/build.gradle.kts](app/build.gradle.kts)). The content version is a hash of the bundle: when it differs from the loaded one, the app rewrites its content tables in one transaction at the start, in the background; a failed load keeps the old content (see [ContentLoader.kt](app/src/main/kotlin/com/somkov/greek_b1/content/ContentLoader.kt)).
- **Content tables:** `content_item` has the columns that the app filters and counts by, and the rest of the item as JSON, decoded by the screen of its part; the catalog has a table per kind of ID (see [ContentTables.kt](app/src/main/kotlin/com/somkov/greek_b1/content/ContentTables.kt)). A change of the item format needs no migration. The Kotlin classes of an item's body come with the screen of its part (stories 6, 7, 10, 13), since the shape that a screen needs is known only with the screen; until then the validator keeps the bundle to the format.
- **Quality of generated Greek:** text that an LLM writes can contain errors, and the user, who is learning Greek, cannot find them. Measures: each new or changed item is reviewed by a Claude Code subagent on a lighter model (Sonnet, [greek-content-reviewer](.claude/agents/greek-content-reviewer.md)), against a checklist: the key follows from the text and no other answer does, the level is B1, the rules of [topics.md](docs/topics.md#rules-for-content-items) hold, the grammar and the wording are correct and natural. Claude Code weighs each remark and may reject it. The KEG sample papers are the reference for the style and the level. A blind solver (an agent that answers an item without its key, to compare its answers with the key) was tried by hand on the sample set and found nothing that the review missed, so it is not built; to try again when content is written in volume (story 17), or when a wrong key reaches the phone.

## Audio (listening)

- **Pre-generated neural audio** for the listening content: dialogue scripts with speakers, turned into audio files by a cloud neural TTS on the development machine. Real male and female voices; plays offline.
- **System TTS** (Android `TextToSpeech`) for text that exists only at run time: single words, the prompts of the speaking part, or as a fallback. The phone gives the app one offline Greek voice (see [TtsProbe.kt](app/src/main/kotlin/com/somkov/greek_b1/diagnostics/TtsProbe.kt)), so different speakers differ only by pitch and rate.

## Speech (speaking)

- An interface `SpeechToText` with one implementation chosen in story 12 from: Vosk, whisper.cpp. The Android recognizer has no Greek on the phone (see [SpeechProbe.kt](app/src/main/kotlin/com/somkov/greek_b1/diagnostics/SpeechProbe.kt)).
- The app keeps the recording with its transcript.
- What is assessed: content, vocabulary, grammar as far as the transcript shows it, and fluency (words per minute, pauses). Pronunciation is not assessed. Speech recognizers tend to correct errors, so the transcript can look better than the speech was. This is a known limit.

## Handwriting (writing)

- A drawing canvas that takes S Pen input (`MotionEvent` with the stylus tool type).
- ML Kit Digital Ink Recognition with the Greek model, offline after a one-time download; tested on the phone in story 1 (see [InkProbe.kt](app/src/main/kotlin/com/somkov/greek_b1/diagnostics/InkProbe.kt)).
- The recognized text is shown for correction before it is submitted. Keyboard input stays available.

## Assessment

- An interface `Assessor` with a cloud LLM client behind it, so that the model and the provider can change.
- Each part has a prompt that states the exam's criteria for the task type and asks for structured output (JSON): a score per criterion, the errors with corrections, and advice. The criteria are the KEG scales, split into criteria with IDs; the input, the procedure and an output sketch are in [assessment-criteria.md](docs/assessment-criteria.md#for-the-llm-prompts-story-9).
- The results are stored with the model name and the date, so that results from different models can be told apart.
- Answers made while offline wait in a queue and are sent when the network is back.

## Progress

Progress is the readiness for each part of the exam on each topic, not the time spent. The order of learning is reading, listening, writing, speaking: what one cannot read, one cannot hear, write or say.

- **A session is the work on one content item**, of one of two kinds:
  - a **trial**: the first sight of a new item, under exam conditions. Its timer runs on the clock without a stop, also with the screen off, against the time of the task (in the catalog of the task types, derived from the time of its part in the exam, and copied into the session at its start, so that a later change of the catalog keeps the past readiness); the user may finish after the time is up, and the real time is recorded. A trial ends with the check of its answers, or the user abandons it. A new item is open only to a trial, and the list of the items marks the new ones; the result screen of a trial offers a study session on the same item, while it is fresh in memory.
  - a **study session**: any work on an item after its trial, any number of times. Its timer counts only the time with the item on the screen (another screen, another app or the screen off stop it), never stopping on idle; the user ends the session by returning to the list. The time outside the app is not counted.
- **The session recorder** ([SessionRecorder.kt](app/src/main/kotlin/com/somkov/greek_b1/session/SessionRecorder.kt)) is the one writer of the sessions, with at most one open session. The answers are saved as they change, so a session survives the death of the process and is resumed at the next start: a study session with its timer stopped, a trial with its clock run on. Each check of the answers is recorded with its answers and points: the mistakes of all the checks go to the review of mistakes (story 16).
- **The log of an item:** the list of the items shows, per item, whether it is new, the number of its sessions, the last date and the points of the last session; the item's own screen shows each session (the kind, the date, the time, the points of its first check and the mistakes). It comes with the screens of the parts (story 6).
- **Readiness** has a cell per topic and part (15 × 4), from the trials only: the repeats of an item show that the item was learned, not the topic (see [Readiness.kt](app/src/main/kotlin/com/somkov/greek_b1/progress/Readiness.kt)). The readiness of a cell is its last trial whose result is known (an open trial, or one whose points the assessment has not given yet, is passed over): points% × min(1, time of the task / real time), so the time over the limit shrinks the points and speed earns nothing; an abandoned trial is 0%, so abandoning a bad trial never helps. The readiness of a part is the mean of its cells, all topics weighing the same, a cell without a result counting as 0. The log of the trials keeps everything, so the history of the readiness over the weeks can be added when it is wanted.
- **The chain:** per topic, a part is advised once the previous part is at the pass mark, 60%: reading → listening → writing → speaking. An advice, not a lock: any item can be opened.
- **The time per day** against a daily goal (a setting, 2 hours by default) is a lower threshold, not the measure of progress, and there is no streak of days. Each session keeps the zone offset of the phone at its start; the day ends at 3:00 local time, so late practice belongs to its evening, and a session counts to the day it started, so a trip to another time zone does not move the past days (see [DailyTime.kt](app/src/main/kotlin/com/somkov/greek_b1/progress/DailyTime.kt)). The past days are shown against the goal of today. The screens that show the day read the clock at each full minute, so the next day comes at 3:00 sharp.
- **The progress screen** ([ProgressScreen.kt](app/src/main/kotlin/com/somkov/greek_b1/progress/ProgressScreen.kt)) shows the days left until the exam; the readiness as a heat map, a row per topic and a column per part with the readiness of the part at its head, colored red (0%) → yellow (60%) → green (100%), gray without a result (writing and speaking until the assessment of story 9), outlined while not advised, and a tap on a cell shows its trials; and the minutes per day as a calendar from the first day of study to the exam, a week per row, each day from pale to full up to the goal with a check mark when it is met. Home shows today in one line under the countdown: the minutes against the goal and the trials. No chart library: the heat map and the calendar are grids of colored squares in plain Compose; a library (Vico) comes with a real chart, such as the history of the readiness.
- The daily plan (story 15) and the review of mistakes (story 16) read the same data.

## Development environment

- **The command line only:** JDK, the Android SDK command-line tools and the Gradle wrapper on Linux, no Android Studio; the phone over USB. The setup and its reasons are in [README.md](README.md#development-setup).
- **One script per command** in `tools/`: build, install, start, the log, the tests. They are shell scripts while they only call `gradlew` and `adb`; scripts with logic of their own (the content scripts, the audio generation) are in Python, with their dependencies in a virtual environment `.venv/` set up from [pyproject.toml](pyproject.toml): libraries where they make the code simpler (Pydantic, ruamel.yaml), and pyright checks the types, since the user reviews the code but does not know Python well.
- **Tests:** JUnit 4. Unit tests run on the JVM, and the Python scripts have their own with pytest, on small fixtures; tests on the phone only where the platform is the point (the navigation, the database, the screens, later TTS, STT and ink). The tests on the phone run against a copy of the app under its own application ID, never against the installed app (see [app/build.gradle.kts](app/build.gradle.kts)). The tests of the database run on a database in memory; the tests of a screen on made-up data show its content in an empty activity, so the database of the app never gets made-up sessions, and they take screenshots for a review. The code that waits on the clock is tested in virtual time (`kotlinx-coroutines-test`).

## Open questions

### The exam

- [ ] **The listening tasks of a UCY paper:** which of the five listening task types it uses, with how many items and points. The largest gap of the specification; until it is filled, the app trains all five and the mock exam uses the KEG pair. UCY could answer by email (smgcertification@ucy.ac.cy). The other gaps and their assumptions are in [exam-spec.md](docs/exam-spec.md#gaps).

### The design

- [ ] **Which cloud TTS** for the pre-generated audio, and its cost and terms?
- [ ] **How to keep the API keys:** in a local build file outside git, compiled into the app, or entered in the app's settings and kept with the Android Keystore?
- [ ] **Which LLM** for the assessment (story 9)? Decided on real samples of writing and speaking.
- [ ] **Assess speaking from audio?** Some cloud models take audio directly. That could assess fluency and pronunciation better than a transcript. To check after story 13.
- [ ] **APK size:** how much audio fits before the content needs a separate download?
- [ ] **The pictures of `speaking.picture`:** where they come from (story 13). The format already requires one per item, `content/pictures/<id>.<ext>`.
- [ ] **Real audio:** where to get authentic listening material (official sample papers, other sources), and how to use it within its terms. The KEG series B sample has published audio, linked in [docs/sources/README.md](docs/sources/README.md).
