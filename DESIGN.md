# Technical Design

The architecture of the app: the main ideas, the decisions made, and the open questions. See [README.md](README.md) for the purpose and [PLAN.md](PLAN.md) for the plan. The details of the implementation and the local decisions are in the comments of the code; this document links to them.

## Status

Story 1 settled the exam (see [README.md](README.md#the-exam)), the phone and the toolchain. Story 2 described the exam format and fixed the catalogs that the content refers to. Story 3 built the skeleton of the app: the navigation, the database, the tests and the scripts. The rest is still a first draft, written before the features.

## Constraints

- **One user, one phone:** Samsung Galaxy S22 Ultra (`SM-S908B`, `arm64-v8a`, built-in S Pen), Android 16 (API level 36), One UI 8.0. No backend of our own, no accounts.
- **A fixed deadline:** the exam session of January 2027, with one attempt (see [README.md](README.md#the-exam)).
- **The progress data is never lost.** It exists only on the phone (until the backup of story 18), so the signing key, the application ID, the install script, the database migrations and the tests are all built so that nothing removes it.
- **Offline first.** Practice works without a network. Only the assessment of writing and speaking needs the network, and it can wait until the network is back.
- **The exam is the scope.** Each piece of content refers to a task type and a B1 topic of the catalogs of story 2.
- **Claude Code writes the code and the content; the user reviews.** The code must be easy to understand: few layers, no clever abstractions.
- **Use libraries, do not reinvent them.** A well-known library is preferred to code of our own that does the same work.

## Overview

```
repository
├── app/        Android app (Kotlin, Jetpack Compose), a self-contained Gradle build
├── content/    source of the learning content: topics, tasks, dialogue scripts
├── tools/      scripts: build, install and tests; later the content validator and audio generation
└── docs/       exam specification, catalogs and the official sources
```

Two flows:

1. **Content flow (on the development machine).** Claude Code writes the content into `content/`. The validator checks it. The audio script makes the audio files. The build packages the content and the audio into the APK.
2. **Practice flow (on the phone).** The app loads the content into its database, runs the exercises, records the sessions and the results, and sends writing and speaking answers to the cloud LLM for assessment.

## Exam specification

The exam is written down in `docs/`: [exam-spec.md](docs/exam-spec.md) (the structure, the timing, the rules, and how the KEG exam differs), [assessment-criteria.md](docs/assessment-criteria.md), [task-types.md](docs/task-types.md) and [topics.md](docs/topics.md). UCY publishes little, so the gaps are filled from the KEG B1 exam, whose task design UCY shares; each fact names its source.

- **Stable IDs** for the task types, topics, situations, functions and criteria. An ID in use is never renamed or reused, since the user tables keep results and mistakes by ID. The IDs live in the tables of these documents until story 4 makes a machine-readable catalog in `content/` from them; from then on that file is their source.
- **Every content item is in the exam form** (the counts and the shape of its task type), so any item can go into a mock exam. Practice changes how an item is run, not the item.
- **The official sources are part of the repository**, encrypted with git-crypt: `docs/sources/` holds copyrighted sample papers, and the repository is public. Only the index [docs/sources/README.md](docs/sources/README.md) stays readable.

## The app

- **Language and UI:** Kotlin, Jetpack Compose, Material 3 with the phone's dynamic colors.
- **The language of the UI is English.** The tasks are in Greek, as in the exam. Where training shows a translation of Greek (a dictionary, for example), it is Greek to Russian, the user's language.
- **Structure:** one Gradle module, the only project of the build in `app/`; packages by feature (`home`, `reading`, `listening`, `writing`, `speaking`, `progress`, `settings`, `diagnostics`) and a few shared packages (`data`, `theme`; later `content`, `speech`, `ink`, `assessment`).
- **Navigation:** Navigation 3; home is a hub with a tile for each of the other screens, and back returns to it (see [Navigation.kt](app/src/main/kotlin/com/somkov/greek_b1/Navigation.kt)).
- **State:** a `ViewModel` per screen with `StateFlow`; Compose screens observe it. A ViewModel lives as long as its screen is on the back stack.
- **Data:** Room (SQLite) on the phone's own SQLite. Two kinds of tables:
  - content tables, rewritten from the APK's content on each content update;
  - user tables (sessions, attempts, results, assessments, recordings), never rewritten, linked to content by stable IDs.
- **The database is migrated, never dropped**, and every schema version is exported and tested; the rules are in [Migrations.kt](app/src/main/kotlin/com/somkov/greek_b1/data/Migrations.kt).
- **Dependency injection:** manual, with a small app container ([AppContainer.kt](app/src/main/kotlin/com/somkov/greek_b1/AppContainer.kt)), unless the app grows enough to need Hilt.
- **SDK levels:** `minSdk` is the phone's API level, so no compatibility code for older Android versions. The values and their reasons are in [app/build.gradle.kts](app/build.gradle.kts).
- **The signing key is part of the repository**, encrypted with git-crypt, because a new key forces an uninstall. Details in [app/build.gradle.kts](app/build.gradle.kts).
- **Diagnostics:** a screen under the settings (package `diagnostics`) probes what a system update or a new phone changes: the TTS voices, the recognizer languages, the ink model. The probes keep the findings about the phone in their comments.

## Content

- **Source format:** text files in `content/`, one file per item or per small group, with a stable ID, a task type and a situation (see [Exam specification](#exam-specification)). Readable in a review, since the reviewer checks the Greek text.
- **Validation:** a script checks the format, the IDs, the references to the topic catalog and the task types, and reports the coverage per topic and per task type.
- **Delivery:** the content is packaged into the APK. A new batch of content is a new build. A content version number tells the app to reload its content tables.
- **Quality of generated Greek:** text that an LLM writes can contain errors. Measures: the user reviews the content; the KEG sample papers are the reference for the style and the level, and [topics.md](docs/topics.md#rules-for-content-items) sets the rules for the setting and the language; a second check by another model is possible for a large batch.

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

- A session is a period of practice with a start and an end. An attempt is one task done in a session, with its result.
- From these: the time per day, the daily goal, the streak, the results per part, topic and task type over time, and the days left until the exam.
- The daily plan (story 15) and the review of mistakes (story 16) read the same data.

## Development environment

- **The command line only:** JDK, the Android SDK command-line tools and the Gradle wrapper on Linux, no Android Studio; the phone over USB. The setup and its reasons are in [README.md](README.md#development-setup).
- **One script per command** in `tools/`: build, install, start, the log, the tests. They are shell scripts while they only call `gradlew` and `adb`; scripts with logic of their own (the content validator, the audio generation) are in Python.
- **Tests:** JUnit 4. Unit tests run on the JVM; tests on the phone only where the platform is the point (the navigation, the database, later TTS, STT and ink). The tests on the phone run against a copy of the app under its own application ID, never against the installed app (see [app/build.gradle.kts](app/build.gradle.kts)).

## Open questions

### The exam

- [ ] **The listening tasks of a UCY paper:** which of the five listening task types it uses, with how many items and points. The largest gap of the specification; until it is filled, the app trains all five and the mock exam uses the KEG pair. UCY could answer by email (smgcertification@ucy.ac.cy). The other gaps and their assumptions are in [exam-spec.md](docs/exam-spec.md#gaps).

### The design

- [ ] **The source format of the content:** YAML (easier to read in a review) or JSON (no conversion)?
- [ ] **Which cloud TTS** for the pre-generated audio, and its cost and terms?
- [ ] **How to keep the API keys:** in a local build file outside git, compiled into the app, or entered in the app's settings and kept with the Android Keystore?
- [ ] **Which LLM** for the assessment (story 9)? Decided on real samples of writing and speaking.
- [ ] **Assess speaking from audio?** Some cloud models take audio directly. That could assess fluency and pronunciation better than a transcript. To check after story 13.
- [ ] **APK size:** how much audio fits before the content needs a separate download?
- [ ] **Real audio:** where to get authentic listening material (official sample papers, other sources), and how to use it within its terms. The KEG series B sample has published audio, linked in [docs/sources/README.md](docs/sources/README.md).
