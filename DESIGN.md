# Technical Design

The architecture of the app: the main ideas, the decisions made, and the open questions. See [README.md](README.md) for the purpose and [PLAN.md](PLAN.md) for the plan.

## Status

A first draft, written before any code. Story 1 answers the main open questions and updates this document.

## Constraints

- **One user, one phone:** Samsung Galaxy S22 Ultra. No backend of our own, no accounts.
- **Offline first.** Practice works without a network. Only the assessment of writing and speaking needs the network, and it can wait until the network is back.
- **The exam is the scope.** Each piece of content refers to a part of the exam, a task type and a B1 topic.
- **Claude Code writes the code and the content; the user reviews.** The code must be easy to understand: few layers, no clever abstractions.
- **Use libraries, do not reinvent them.** A well-known library is preferred to code of our own that does the same work.

## Overview

```
repository
├── app/        Android app (Kotlin, Jetpack Compose)
├── content/    source of the learning content: topics, tasks, dialogue scripts
├── tools/      scripts: content validator, audio generation
└── docs/       exam specification and other reference material
```

Two flows:

1. **Content flow (on the development machine).** Claude Code writes the content into `content/`. The validator checks it. The audio script makes the audio files. The build packages the content and the audio into the APK.
2. **Practice flow (on the phone).** The app loads the content into its database, runs the exercises, records the sessions and the results, and sends writing and speaking answers to the cloud LLM for assessment.

## The app

- **Language and UI:** Kotlin, Jetpack Compose, Material 3.
- **Structure:** one Gradle module, packages by feature (`reading`, `listening`, `writing`, `speaking`, `progress`, …) and a few shared packages (`content`, `data`, `speech`, `ink`, `assessment`).
- **State:** a `ViewModel` per screen with `StateFlow`; Compose screens observe it.
- **Data:** Room (SQLite). Two kinds of tables:
  - content tables, rewritten from the APK's content on each content update;
  - user tables (sessions, attempts, results, assessments, recordings), never rewritten, linked to content by stable IDs.
- **Dependency injection:** manual (a small app container) unless the app grows enough to need Hilt.

## Content

- **Source format:** text files in `content/`, one file per item or per small group, with a stable ID, a topic, an exam part and a task type. Readable in a review, since the reviewer checks the Greek text.
- **Validation:** a script checks the format, the IDs, the references to the topic catalog and the task types, and reports the coverage per topic and per task type.
- **Delivery:** the content is packaged into the APK. A new batch of content is a new build. A content version number tells the app to reload its content tables.
- **Quality of generated Greek:** text that an LLM writes can contain errors. Measures: the user reviews the content; official sample papers are the reference for the style and the level; a second check by another model is possible for a large batch.

## Audio (listening)

- **Pre-generated neural audio** for the listening content: dialogue scripts with speakers, turned into audio files by a cloud neural TTS on the development machine. Real male and female voices; plays offline.
- **System TTS** (Android `TextToSpeech`) for text that exists only at run time: single words, the prompts of the speaking part, or as a fallback. Different speakers get different voices if the phone has them, otherwise a different pitch and rate.

## Speech (speaking)

- An interface `SpeechToText` with one implementation chosen in story 12 from: the Android on-device recognizer, Vosk, whisper.cpp.
- The app keeps the recording with its transcript.
- What is assessed: content, vocabulary, grammar as far as the transcript shows it, and fluency (words per minute, pauses). Pronunciation is not assessed. Speech recognizers tend to correct errors, so the transcript can look better than the speech was. This is a known limit.

## Handwriting (writing)

- A drawing canvas that takes S Pen input (`MotionEvent` with the stylus tool type).
- ML Kit Digital Ink Recognition with the Greek model, offline after a one-time model download.
- The recognized text is shown for correction before it is submitted. Keyboard input stays available.

## Assessment

- An interface `Assessor` with a cloud LLM client behind it, so that the model and the provider can change.
- Each part has a prompt that states the exam's criteria for the task type (from story 2) and asks for structured output (JSON): a score per criterion, the errors with corrections, and advice.
- The results are stored with the model name and the date, so that results from different models can be told apart.
- Answers made while offline wait in a queue and are sent when the network is back.

## Progress

- A session is a period of practice with a start and an end. An attempt is one task done in a session, with its result.
- From these: the time per day, the daily goal, the streak, the results per part, topic and task type over time, and the days left until the exam.
- The daily plan (story 15) and the review of mistakes (story 16) read the same data.

## Development environment

- JDK, Android SDK command-line tools and the Gradle wrapper on the development machine (Linux). No Android Studio needed.
- Claude Code builds, installs (`adb install`) and reads the logs (`adb logcat`) from the command line.
- Unit tests run on the JVM. Tests on the device only where the platform is the point (TTS, STT, ink).
- The scripts in `tools/` are in Python.

## Open questions

### The exam

- [ ] **Which exam?** Πιστοποίηση Ελληνομάθειας B1 (Κέντρο Ελληνικής Γλώσσας), or the exam for naturalization (ΠΕΓΠ), which has a B1 language part? The formats, the topics and the timing differ.
- [ ] **The date of the exam?** It sets the order of the stories and the amount of content needed.
- [ ] **The current format** of the exam: the task types, the timing and the criteria per part, from the official sources (story 2).

### The phone

- [ ] **Which Android version and One UI version** does the phone run? It sets the `minSdk` and which on-device speech APIs are available.
- [ ] **Which Greek TTS voices** are installed or can be installed (Google, Samsung)? Is there more than one?
- [ ] **Is Greek offline speech recognition available** on the phone through the Android recognizer?

### The design

- [ ] **The source format of the content:** YAML (easier to read in a review) or JSON (no conversion)?
- [ ] **Which cloud TTS** for the pre-generated audio, and its cost and terms?
- [ ] **How to keep the API keys:** in a local build file outside git, compiled into the app, or entered in the app's settings and kept with the Android Keystore?
- [ ] **Which LLM** for the assessment (story 9)? Decided on real samples of writing and speaking.
- [ ] **Assess speaking from audio?** Some cloud models take audio directly. That could assess fluency and pronunciation better than a transcript. To check after story 13.
- [ ] **APK size:** how much audio fits before the content needs a separate download?
- [ ] **Real audio:** where to get authentic listening material (official sample papers, other sources), and how to use it within its terms.
