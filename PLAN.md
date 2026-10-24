# Plan

The implementation plan of the MVP. See [README.md](README.md) for the purpose of the project and [DESIGN.md](DESIGN.md) for the architecture.

## How to use this document

- The plan is a list of stories. Each story is done in its own branch, `story/<name>`, and is split into small tasks there.
- The stories are worked top to bottom, but the order can change as we learn. This is a living plan.
- Each story has a status: **Not started**, **In progress**, **Done** or **Dropped** (with the reason). Update it when the branch is created and when it is merged.
- A design decision goes into DESIGN.md, not here. This file tracks the order and the status of the work.
- Add, split, merge or reorder stories as needed. Git history keeps the old versions.
- The app is used every day from milestone 2. Problems found in use are fixed as they come, and the plan is revised as soon as it is clear that it must change, not at the end of the MVP.

## Working a story

For a story large enough to need review checkpoints, create a temporary `STORY.md` in the repository root and work from it. Delete it before the merge; first move anything worth keeping into DESIGN.md, PLAN.md or code comments.

The last task of a story that changes the app raises `versionCode` (see [app/build.gradle.kts](app/build.gradle.kts)), so that the phone shows which version it runs.

```markdown
# Story: <name>

**Status:** <e.g. "Step 2 of 5, in progress">

## Description (from PLAN.md)

> <the text of the story>

## Tasks

- [ ] **1. <task>.** <what it does>
- [ ] **2. <task>.** <what it does>

## Decisions and open questions

- [ ] **<Open question>?** <options>
- [x] **<Decision>.** <answer> — <why, in one line>
```

## Milestones

1. **Foundation** (stories 1–4): the toolchain, the exam specification, the app skeleton and the content pipeline.
2. **Daily practice of the receptive skills** (stories 5–8): the tracking of the sessions and the readiness, reading and listening. From here on the app is used every day.
3. **Productive skills** (stories 9–13): writing, handwriting, speaking, and their assessment by the LLM.
4. **Exam readiness** (stories 14–18): the mock exam, the daily plan, the review of mistakes, full content coverage, and backup.

All the stories are to be done before the exam session of January 2027 (see [README.md](README.md#the-exam)).

## MVP plan

### 1. Environment, key questions and architecture
- Branch: `story/foundation`
- Status: Done

Make the development environment work end to end: JDK, Android SDK command-line tools and Gradle on this machine, and `adb` access to the phone (USB or wireless debugging). An empty app is built from the command line and installed on the phone. Answer the main open questions of DESIGN.md: which exam and when, the Android version of the phone, and what the phone supports for Greek — the installed TTS voices, offline speech recognition, and ML Kit Digital Ink. Update DESIGN.md with the answers and with the architecture decisions that follow from them.

### 2. Exam specification and B1 topic catalog
- Branch: `story/exam-spec`
- Status: Done

Collect the official description of the exam and its sample papers. Write down, in the repository, the structure of each part: the task types, the number of items, the timing, the length of texts, and the assessment criteria. Make the catalog of B1 topics that the exam uses, each with its typical situations and vocabulary areas. Every content item and every exercise later refers to this catalog and to these task types. Work from the UCY documents in `docs/sources/`, and check the format of the KEG exam, the fallback, for the differences.

### 3. App skeleton
- Branch: `story/app-skeleton`
- Status: Done

The base structure of the app: Kotlin, Jetpack Compose, navigation between the main screens (home, the four parts, progress, settings), the Room database, and the test setup. Scripts to build, install and run the tests from the command line. No learning features yet. The empty app of story 1 in `app/` grows into the skeleton, and its capability probe (package `probe`) becomes a diagnostics screen in the settings.

### 4. Content format and pipeline
- Branch: `story/content-pipeline`
- Status: Done

The format of the learning content in the repository (texts, tasks, answers, dialogue scripts, references to audio), with stable IDs and references to the task type and the situation of the catalogs of story 2. The machine-readable catalog of the task types, topics, situations and functions, made from the tables of story 2, which from then on is the source of the IDs. A validator script that checks the content against the format and the catalog. The packaging of the content into the APK, and its load into the app's database on the first start and after each update, so that the progress data survives a content update. A small sample set of content to test the pipeline.

### 5. Sessions and readiness tracking
- Branch: `story/progress-tracking`
- Status: Done

Record every session on a content item, of two kinds. A trial is the first sight of a new item under exam conditions: its timer runs without a stop, the user may finish after the time is up, and the real time is recorded. A study session is any work on an item after its trial, any number of times, and its timer counts only the time with the item on the screen. The readiness for each part of the exam on each topic comes from the last trial of that topic and part, its points reduced by the time over the limit; the progress screen shows it as a heat map of 15 topics × 4 parts. Per topic, the parts follow the chain reading → listening → writing → speaking: a part whose previous part is below the pass mark is not advised, though it stays open. A daily goal in minutes, the first setting of the app, which brings in the settings store; the minutes per day against it in a calendar until the exam. This comes before the exercises so that every exercise is recorded from its first use.

### 6. Reading
- Branch: `story/reading`
- Status: Not started

The reading part: the four task types of the exam (matching, comparison, true/false, gap fill from a word bank), the list of the items with the log of each (a new item opens only as a trial), the trial with its exam timer and the study session of story 5, the check of the answers with explanations. A play button reads the text aloud with the system TTS, as a preparation for listening. The first batch of reading content over the topic catalog.

### 7. Listening with the system TTS
- Branch: `story/listening`
- Status: Not started

The listening part: the five task types that UCY lists (notes, comparison, true/false, gap fill, multiple choice), an audio player (play, pause, replay as the exam allows, speed), and the questions. Audio comes from the Android system TTS. The phone has one Greek voice for apps, so dialogues use a different pitch and rate per speaker. The first batch of listening content.

### 8. Pre-generated neural audio
- Branch: `story/neural-audio`
- Status: Not started

A script in the repository that turns the dialogue scripts into audio files with a cloud neural TTS, with a male and a female voice at least. The audio is packaged into the APK and played offline. The system TTS stays for the text that is made at run time. Includes a check of the APK size.

### 9. LLM assessment service
- Branch: `story/llm-assessment`
- Status: Not started

A client for a cloud LLM behind one interface, so that the model can be changed. Prompts that assess an answer against the exam criteria of story 2 and return structured feedback: a score per criterion, the errors with corrections, and short advice. The handling of the API key, network errors, and answers sent while offline (queued until the network is back). The choice of the model is made in this story, on real samples.

### 10. Writing
- Branch: `story/writing`
- Status: Not started

The writing part with keyboard input: the task types of the exam, the word count, a timer, the submission to the assessment service, and a feedback screen that shows the text with the errors marked. The results are recorded by story 5. The first batch of writing tasks.

### 11. S Pen handwriting
- Branch: `story/handwriting`
- Status: Not started

A handwriting canvas for the S Pen. ML Kit Digital Ink recognizes the Greek text offline; the user corrects the result and submits it as in story 10. Decide if the handwriting is recognized line by line or in one pass at the end.

### 12. Speech recognition engine
- Branch: `story/speech-engine`
- Status: Not started

Compare the offline speech-to-text engines for Greek on the phone (Vosk, whisper.cpp; the Android recognizer has no Greek, see DESIGN.md) on real recordings of the user: accuracy, speed, and how much they hide errors. Put the chosen engine behind an interface. The recording is kept with its transcript, for replay and for later analysis.

### 13. Speaking
- Branch: `story/speaking`
- Status: Not started

The speaking part: the task types of the exam (for example interview questions, picture description, role play), the prompts read by TTS where the exam has an examiner or a partner (the exam is taken in pairs, and the role play is between the two candidates), a timed recording, the transcript from story 12, and the assessment of content, vocabulary and fluency (with simple measures such as words per minute and pauses). The first batch of speaking tasks, with the pictures of the picture description: decide where they come from, and package them into the APK.

### 14. Mock exam
- Branch: `story/mock-exam`
- Status: Not started

A full exam under exam conditions: the four parts in the exam's order and timing, no help, and a result per part and in total, with an estimate of pass or fail. A second timing profile for the KEG exam, which is also a harder practice for the UCY one. How the mock exams are recorded, and whether they count for the readiness of story 5, is decided in this story.

### 15. Daily plan
- Branch: `story/daily-plan`
- Status: Not started

The app proposes the practice of the day from the readiness of story 5: the next trials along the chain of the parts of each topic, the study sessions on the weak topics, the daily goal and the days left until the exam. The home screen starts the next exercise in one tap.

### 16. Review of mistakes
- Branch: `story/mistakes-review`
- Status: Not started

Collect the errors from all parts (wrong answers, the corrections from the writing and speaking feedback) into one list, grouped by topic and by grammar point, and repeat them at intervals until they stop. Only the errors that the user made, not a general grammar course.

### 17. Full content coverage
- Branch: `story/content-coverage`
- Status: Not started

Fill the gaps in the content: enough items for every topic of the catalog and every task type of every part, for the planned number of weeks of daily practice and for several different mock exams. Extends the coverage report of story 4 with the targets. Each new measure of the readiness of a topic in a part (story 5) uses up a new item, so the targets count the new items per topic and part, the reading first, since it opens the chain.

### 18. Backup and restore
- Branch: `story/backup`
- Status: Not started

Export the progress data, the results and the recordings to a file, and import them back, so that a reinstall or a new phone does not lose the history.
