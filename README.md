# Greek B1

A personal Android app to prepare for a Greek language exam at level B1.

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

## How the project is built

- Claude Code writes the code and generates the learning content (texts, tasks, audio scripts).
- The user sets the direction, reviews every change, and uses the app every day.
- The work goes story by story, one git branch per story.

## Documents

- [PLAN.md](PLAN.md): the plan of the MVP as a list of stories, with their status.
- [DESIGN.md](DESIGN.md): the architecture, the decisions made, and the open questions.
- [CLAUDE.md](CLAUDE.md): the rules for Claude Code in this repository.
