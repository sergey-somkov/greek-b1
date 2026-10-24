# B1 task type catalog

One stable ID for each task type of each part of the UCY B1 exam, with what a content item of the type must give, what the user answers, and how the answer is scored. The content validator (story 4), every content item, and the results in the app refer to these IDs.

The exam facts behind each type, with their sources, are in [exam-spec.md](exam-spec.md); the scoring rules in full are in [assessment-criteria.md](assessment-criteria.md). This catalog does not repeat the sources. It marks as **Estimate** only the rules that are ours: the formats of the three listening types that KEG does not use, and the rules that the app adds for its content.

The IDs and the data of the tables of this document (the overview, the answer formats, the genres of writing and the kinds of role play) come from [content/catalog/task_types.yaml](../content/catalog/task_types.yaml), their one source. `tools/content.py docs` writes each table between its `<!-- catalog: … -->` markers; change the catalog and run it, never the tables. The prose is written by hand.

## IDs

- **Form:** `<part>.<type>`. The part is one of `listening`, `reading`, `writing`, `speaking`. The type is one or more lowercase English words joined by `_`. Example: `reading.true_false`.
- **Stable:** an ID in use is never renamed and never reused for another type. The app keeps the results and the mistakes of the user by ID (see [DESIGN.md](../DESIGN.md#the-app)), so a renamed ID would cut the history. A type that is no longer needed stays in the catalog, marked as retired.
- **Only exam formats.** Each type is a task of the exam. Drills that the exam does not have (vocabulary cards, grammar exercises) are not in this catalog; if the app gets them later, they get IDs of the same form.
- **The same name in two parts** (`comparison`, `true_false`, `gap_fill`) means the same idea, not the same format: the input and the scoring differ between reading and listening.

## Overview

In the order of the exam: listening first, then reading and writing, then the speaking session.

<!-- catalog: task_types -->

| ID | Greek name | Exam slot | Items | Points | Time | Answer | Scored by |
|---|---|---|---|---|---|---|---|
| `listening.notes` | Σημειώσεις | Listening, KEG task 1 | 12 + example | 12 | 15 min | `short_text` | App, key with matching rule |
| `listening.comparison` | Σύγκριση (συμπλήρωση πίνακα) | Listening, KEG task 2 | 13 ticks + example | 13 | 15 min | `grid` | App, key |
| `listening.true_false` | Σωστό/Λάθος | Listening, UCY only | 6–12 + example (Estimate) | 1 each | 15 min | `choice` | App, key |
| `listening.gap_fill` | Συμπλήρωση κενών σε κείμενο | Listening, UCY only | 6–12 + example (Estimate) | 1 each | 15 min | `short_text` | App, key with matching rule |
| `listening.multiple_choice` | Πολλαπλή επιλογή με 3 επιλογές | Listening, UCY only | 6–12 + example (Estimate) | 1 each | 15 min | `choice` | App, key |
| `reading.matching` | Σύνδεση/Αντιστοίχιση προτάσεων/ενοτήτων | Reading, task 1 | 6 + example, 3 distractors | 6 | 12.5 min | `assign` | App, key |
| `reading.comparison` | Σύγκριση (συμπλήρωση πίνακα) | Reading, task 2 | 12 ticks + example | 6 (0.5 each) | 12.5 min | `grid` | App, key |
| `reading.true_false` | Σωστό/Λάθος | Reading, task 3 | 6 + example | 6 | 12.5 min | `choice` | App, key |
| `reading.gap_fill` | Συμπλήρωση κενών | Reading, task 4 | 7 + example, 6 distractors | 7 | 12.5 min | `assign` | App, key |
| `writing.informal` | Α΄ μέρος: φιλικό κείμενο | Writing, part A | 1 text, 150 words | 12 | 35 min | `text` | LLM, writing scale |
| `writing.formal` | Β΄ μέρος: τυπικό κείμενο | Writing, part B | 1 text, 150 words | 13 | 35 min | `text` | LLM, writing scale |
| `speaking.interview` | Α΄ μέρος: απλές ερωτήσεις | Speaking, part A | 6–8 questions | — | 2 min | `speech` | LLM, speaking criteria |
| `speaking.opinion` | Β΄ μέρος: σύντομος μονόλογος | Speaking, part B, first half | 1 question | — | 1.5 min | `speech` | LLM, speaking criteria |
| `speaking.picture` | Β΄ μέρος: φωτογραφία | Speaking, part B, second half | 1 picture | — | 3 min | `speech` | LLM, speaking criteria |
| `speaking.roleplay` | Γ΄ μέρος: παιχνίδι ρόλων | Speaking, part C | 2 role cards | — | 6 min | `dialogue` | LLM, speaking criteria |

<!-- /catalog -->

- **Speaking has no points per type:** the five criteria rate the whole performance over the three parts (see [assessment-criteria.md](assessment-criteria.md#speaking)). In practice the app rates one type at a time on the same criteria.
- **The time** is the time of the task: the time limit of a trial, the first sight of an item under exam conditions (see [DESIGN.md](../DESIGN.md#progress)). It is derived from the time of the part in the exam, as the section of each part says; past it the user may finish, and the time over it reduces the readiness that the trial shows.
- **Part B of speaking is two types**, because KEG splits it into two halves with different input: a question, and a picture (see [exam-spec.md](exam-spec.md#part-b-a-short-monologue-β-μέρος)). UCY names only the monologue and the photos.
- **The mock exam** (story 14) uses: listening `notes` + `comparison` (the KEG pair, see [exam-spec.md](exam-spec.md#gaps)); the four reading types in their order; `writing.informal` + `writing.formal`; and the speaking types `interview`, `opinion`, `picture`, `roleplay` in a row.

## Answer formats

The app has one input component for each format. The recorded answer is what the user tables keep for the results (story 5) and the review of mistakes (story 16).

<!-- catalog: answer_formats -->

| Format | What the user does | Recorded answer |
|---|---|---|
| `choice` | Picks one of a fixed set of options for each item: ΣΩΣΤΟ / ΛΑΘΟΣ, or α / β / γ. | The option of each item. |
| `assign` | Matches each item to one entry of a list that has more entries than items; an entry is used at most once. | The entry of each item. |
| `grid` | Ticks cells of a grid; the task states the total number of ticks, and the app accepts no more. | The set of ticked cells. |
| `short_text` | Writes a short answer of a few words for each item. | The text of each item, the result of the match, and whether the user overrode it. |
| `text` | Writes a text, with the keyboard (story 10) or the S Pen (story 11). | The text, the word count, the input method, the time taken. |
| `speech` | Speaks, after a question or a picture; one recording per prompt. | The audio and the transcript with timing, per prompt. |
| `dialogue` | Speaks in turns with the app, which plays the partner. | The turns of both sides, with the audio and the transcript of the user's turns. |

<!-- /catalog -->

## Rules for all content items

The format of an item file, with the names of its fields, is in [content/README.md](../content/README.md).

- **Task type and topic:** each item has exactly one task type ID and names one situation of the topic catalog, [topics.md](topics.md#rules-for-content-items).
- **Exam form only:** each item has the counts and the shape that the exam has, so that any item can go into a mock exam. Practice changes how an item is run (timer, replays, hints), not the item.
- **The validator** (`tools/content.py validate`, see [content/README.md](../content/README.md#validation)) treats a wrong count or shape (items, distractors, ticks, options) as an error, and a text length or a balance of answers outside the given range as a warning. The KEG lengths are guides, and the samples themselves run outside them.
- **Lengths** are in words of the Greek text, separated by spaces. A reading or listening length counts the text only, not the items or the options.
- **Reading and listening items** have: a title; a short framing situation in Greek, in the second person plural, as in the exam ("Διαβάζετε σε ένα περιοδικό …"); an instruction that states the number of answers; and the example, item 0, with its answer, shown solved. One standard instruction per type is written with the first content of the type (stories 6, 7), modeled on the samples.
- **No made-up source line.** The exam names the source of each reading text. The app's texts are written for the app and say so; they do not name a newspaper or a website.
- **An explanation per item** (reading, listening): one sentence in English that says why the answer is right, quoting the part of the text that decides it. The app shows it after the check (story 6). Estimate: the exam has no explanations; they are for practice.
- **Writing and speaking items** have no key. They give what the LLM needs for the assessment (see [assessment-criteria.md](assessment-criteria.md#for-the-llm-prompts-story-9)).

## Listening

Common to all listening types:

- **Input:** a script, that is, the turns of the text in order, each with its speaker, and the speakers with their gender and age group, so that the audio of stories 7 and 8 can give each one a voice. The audio is made from the script. The script is also the transcript that the app shows after the check.
- **Text types** (UCY): friendly to fairly formal texts, informative texts, extracts from interviews or literary works, dialogues, on the situations of B1.
- **Exam form:** the audio plays twice, with a pause before the first play to read the task and between the plays; no pause or seeking. The length of the pauses is for story 7.
- **Items in the order of the text.** The answers come in the order in which they are heard.
- **Time:** in the exam, 30 minutes for the part; the time of a single task is 15 minutes, half of 30, as the KEG pair notes + comparison splits it. The three types that KEG does not use get the same (Estimate).

### `listening.notes`

- **Text:** a monologue, a narration of events, or an extract from an interview or a literary work; friendly or informative; about 300 words (250–350).
- **Task:** a title and 12 labeled lines plus the example. Each label is a short Greek phrase that names the fact to note ("Ηλικία του παππού, όταν ήρθε στην πόλη:").
- **Key:** for each line, one or more right answers in the notation of [assessment-criteria.md](assessment-criteria.md#matching-the-notes-against-the-key): `a / b` for alternatives, `(…)` for words that may be left out, and a group of lines whose answers may come in either order.
- **Answer:** `short_text`, a note of 1–4 words; the instruction states the limit (1–3 or 1–4 words).
- **Scoring:** 1 point per line, 12 in total, by the matching rule of [assessment-criteria.md](assessment-criteria.md#matching-the-notes-against-the-key).
- **Checks:** 12 lines + the example; each key answer at most the limit of the instruction, 3 or 4 words; each key answer found in the script, in the same or a close form (warning); the answers in the order of the script (warning).

### `listening.comparison`

- **Text:** a fairly formal dialogue, a discussion between 2–3 people (a radio discussion, an interview), or a series of short interviews; 350–400 words.
- **Task:** a grid with 2–4 columns (the people, organizations or places compared) and 6–11 rows (statements). The rows may be grouped under short headings. The example is one tick in the first row.
- **Key:** the set of right cells: 13 ticks plus the example. A statement can fit more than one column, and the instruction says so. Each row fits at least one column (Estimate).
- **Answer:** `grid`, exactly 13 ticks.
- **Scoring:** 1 point per right tick, 13 in total.
- **Checks:** 2–4 columns; 6–11 rows; 13 key ticks + 1 example tick; no empty row; each column name occurs in the script (warning).

### `listening.true_false`

Estimate: by analogy with `reading.true_false`; there is no sample.

- **Text:** any listening text type; about 300 words (250–350).
- **Task:** 6–12 statements plus the example.
- **Key:** ΣΩΣΤΟ or ΛΑΘΟΣ for each statement. A false statement changes a detail of the text or draws a wrong conclusion from it. There is no "not given": every statement is decided by the text.
- **Answer:** `choice`, ΣΩΣΤΟ / ΛΑΘΟΣ.
- **Scoring:** 1 point per statement.
- **Checks:** 6–12 statements + the example; at least 2 true and 2 false (warning).

### `listening.gap_fill`

Estimate: UCY names the type only; there is no sample.

- **Text:** any listening text type; about 300 words (250–350).
- **Task:** a short text in Greek with 6–12 gaps plus the example: a summary of what is heard, or a note, a notice or a form about it. Each gap takes 1–3 words that are heard in the audio.
- **Key:** as in `listening.notes`.
- **Answer:** `short_text`, written into the gap. No word bank: writing the words is harder than choosing them, which is the safe side for practice. If a UCY paper turns out to give a bank, the type takes one as in `reading.gap_fill`.
- **Scoring:** 1 point per gap, by the matching rule of the notes.
- **Checks:** 6–12 gaps + the example; each key answer at most 3 words; each key answer found in the script (warning).

### `listening.multiple_choice`

Estimate: UCY names the type only; there is no sample.

- **Text:** any listening text type; about 300 words (250–350).
- **Task:** 6–12 questions plus the example; each a question or the start of a sentence, with three options α, β, γ.
- **Key:** one right option per question. The wrong options are mentioned or suggested in the audio but do not answer the question.
- **Answer:** `choice`, α / β / γ.
- **Scoring:** 1 point per question.
- **Checks:** 6–12 questions + the example; exactly 3 options each; the right option not always in the same position (warning).

## Reading

Common to all reading types: the text and the task are shown together; the answers are numbers or ticks, never written words. In the exam form the four tasks share one timer of 50 minutes; the time of a single task is 12.5 minutes (Estimate: a quarter of 50).

### `reading.matching`

- **Text:** fairly formal, informative (tips, instructions, information); 150–180 words, counted with the right second parts in place.
- **Task:** the text has the sentences 0–6, each broken off after its first part. A list holds 10 second parts in mixed order: that of the example, the 6 right ones and 3 distractors.
- **Key:** for each first part 1–6, its second part.
- **Distractors:** they fit the grammar of a first part but not its meaning or the context.
- **Answer:** `assign`: each first part gets one second part; each second part is used at most once. The exam asks for the number of the first part next to the second part; the app may show it the other way, the answer is the same.
- **Scoring:** 1 point per first part, 6 in total.
- **Checks:** 6 first parts + the example; 10 second parts; exactly 3 left unused by the key; no second part used twice.

### `reading.comparison`

- **Texts:** 5 short texts of one kind: adverts, descriptions, short biographies, results of a survey; formal; 150–180 words in total.
- **Task:** a grid with one column per text (5) and 7 rows (features). The example is one tick in the first row.
- **Key:** the set of right cells: 12 ticks plus the example. Each feature fits 1–3 texts. The first row can hold a scored tick besides the example.
- **Answer:** `grid`, exactly 12 ticks.
- **Scoring:** 0.5 point per right tick, 6 in total.
- **Checks:** 5 texts; 7 rows; 12 key ticks + 1 example tick; each row 1–3 ticks, the example included.

### `reading.true_false`

- **Text:** less formal: an extract from a narrative or a literary work; 180–200 words.
- **Task:** 6 statements plus the example, in the order of the text.
- **Key:** ΣΩΣΤΟ or ΛΑΘΟΣ for each statement. A false statement changes a detail of the text or draws a wrong conclusion from it. There is no "not given".
- **Answer:** `choice`, ΣΩΣΤΟ / ΛΑΘΟΣ.
- **Scoring:** 1 point per statement, 6 in total.
- **Checks:** 6 statements + the example; at least 2 true and 2 false (warning).

### `reading.gap_fill`

- **Text:** a friendly text or a simple newspaper or magazine article that informs on a topic; 180–200 words, counted with the right words in the gaps.
- **Task:** the gaps 0–7 in the text, and a bank of 14 words or short phrases in mixed order: that of the example, the 7 right ones and 6 distractors.
- **Distractors:** mostly other forms of the right answers or of the words around the gap (πολλή / πολλά, να δούμε / να δουν), so the task tests grammar as well as meaning.
- **Key:** for each gap, its bank entry.
- **Answer:** `assign`: each gap gets one bank entry; each entry is used at most once. As in matching, the exam writes the gap number next to the entry.
- **Scoring:** 1 point per gap, 7 in total.
- **Checks:** 7 gaps + the example; 14 bank entries, all different; exactly 6 left unused by the key.

## Writing

Common to both writing types:

- **Prompt:** a situation in Greek, in 3–5 sentences, in the second person plural: who you are, what happened, to whom you write and why.
- **The points to cover,** as a list of 2–4 points, also when the prompt states them in its text. The LLM checks them for the criterion `task` (see [assessment-criteria.md](assessment-criteria.md#criteria)).
- **Genre:** one of the values below; it tells the LLM which set phrases and layout to expect.
- **Length:** 150 words, stated in the prompt.
- **The start of the text,** as the exam prints it: the date line, the salutation and the first words; or, for an article, the title.
- **A name to sign with,** never the user's own.
- **A model answer** of about 150 words (optional): shown after the assessment, for comparison. Estimate: the exam has none.
- **Answer:** `text`.
- **Scoring:** the LLM rates the text on the 25-point writing scale, with the six criteria and the caps of [assessment-criteria.md](assessment-criteria.md#writing); the points of the part are 12/25 or 13/25 of the rating.
- **Time:** in the exam, 70 minutes for both texts; the time of a single text is 35 minutes (Estimate: half of 70).
- **Checks:** the genre is one of the type's values; 2–4 points to cover; the start of the text is given; the signature name is given, and the instruction names it (warning).

### `writing.informal`

Part A, 12 points. Friendly register (εσύ, or εσείς to a group).

<!-- catalog: genres writing.informal -->

| Genre | Greek | Example |
|---|---|---|
| `friendly_letter` | φιλική επιστολή | A letter or an email to a friend: news, advice, an invitation, persuading them to join something. |
| `announcement` | εκτεταμένη ανακοίνωση | A notice for neighbors, classmates or a club: an event, a change, a call for help. |
| `advertisement` | εκτεταμένη διαφήμιση | A small ad: something to sell, a room to rent, a lost pet, lessons offered. |

<!-- /catalog -->

### `writing.formal`

Part B, 13 points. Formal register (εσείς).

<!-- catalog: genres writing.formal -->

| Genre | Greek | Example |
|---|---|---|
| `formal_letter` | τυπική επιστολή | A letter or an email to an organization or an official: a reasoned complaint, a reasoned request for leave, a request for information, giving personal or other information, an application. |
| `article` | άρθρο | A short article for a magazine or a website that informs or advises on a topic. |

<!-- /catalog -->

## Speaking

Common to all speaking types:

- **Input:** the Greek text that the examiner or the partner says, read by the system TTS (see [DESIGN.md](../DESIGN.md#audio-listening)).
- **Answer:** the recording and its transcript (story 12); `speech` or `dialogue`.
- **Scoring:** the LLM rates the five speaking criteria of [assessment-criteria.md](assessment-criteria.md#speaking); `pronunciation` only when it gets the audio.
- **Time:** the exam gives 12 minutes per pair for the three parts. The times below are the estimate of [exam-spec.md](exam-spec.md#speaking-παραγωγή-προφορικού-λόγου). The time of a task is the upper end of its estimate, with the time to look at the picture or to read the role: 2 minutes for the interview, 1.5 for the opinion, 3 for the picture and 6 for the role play, 12.5 in all (Estimate).

### `speaking.interview`

- **Task:** 6–8 questions that the examiner asks one candidate about themselves: name, residence, school or work, family, free time, languages, travel, food. One optional follow-up question each.
- **Answer:** `speech`, one recording per question; about 2 minutes in total.
- **Key:** none. The answers are the user's own; the LLM judges the language and whether each question is answered.
- **Checks:** 6–8 questions.

### `speaking.opinion`

- **Task:** one question that asks for a view with reasons ("Είναι καλή ή κακή η τηλεόραση για τα παιδιά;"), and 1–3 follow-up questions that the examiner asks when the answer is short.
- **Answer:** `speech`; about 1–1.5 minutes.
- **Checks:** one question; 1–3 follow-ups.

### `speaking.picture`

- **Task:** a picture of people in a situation, with feelings that show; a description of the picture for the LLM (what it shows, where the people are, how they feel), since the LLM does not see it; and the examiner's questions: what you see, where the people are, how they feel and why, then 2–3 personal questions on the topic.
- **Answer:** `speech`, after 1 minute to look at the picture; about 1.5–2 minutes.
- **Where the pictures come from** (photos under a free license, or generated images) is for story 13.
- **Checks:** the image file exists; the description is given; 2–3 personal questions.

### `speaking.roleplay`

- **Task:** a situation and two role cards, A and B, each 3–7 sentences in Greek in the second person plural: the situation, your position and your reasons, the partner's position, and often "you insist". Each card stands alone: a candidate does not see the partner's card.
- **Kind** (from the samples):

  <!-- catalog: kinds speaking.roleplay -->

  | Kind | Meaning |
  |---|---|
  | `disagreement` | Friends, relatives or colleagues argue out a choice. The most common kind. |
  | `service` | A customer and a clerk or a shop assistant who recommends something else. |
  | `information` | A request for information at a service. |
  | `planning` | Two people plan something together. |

  <!-- /catalog -->

- **Register:** informal (εσύ) or formal (εσείς), as the relation of the roles needs.
- **For the app's role:** the arguments it uses, and when it gives way or agrees, so that the talk comes to an end.
- **The user can play either role,** so both cards are complete; the app plays the other one.
- **Answer:** `dialogue`; 1–2 minutes to read the role, then about 4 minutes of talk. Only the user's turns are rated.
- **How the app plays the partner** (a script, or the LLM at run time, which needs the network) is for story 13.
- **Checks:** both cards given, each 3–7 sentences (warning); the kind and the register are one of the values; 2–4 arguments for each role.
