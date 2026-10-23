# Content

The learning content of the app: the catalog of the IDs, and the items, that is, the exam tasks that the user practices. This document is the format of an item file. What an item of each task type must contain (the text types, the counts, the lengths, the checks) is in [docs/task-types.md](../docs/task-types.md), and the rules for the subject of an item are in [docs/topics.md](../docs/topics.md#rules-for-content-items). The validator (`tools/content.py validate`) checks the items against all three.

## Layout

```
content/
├── catalog/      the IDs of the task types, topics, situations, functions and criteria
├── items/        one file per item: items/<part>/<type>/<id>.yaml
├── audio/        the audio of a listening item: audio/<id>.<ext>, optional
└── pictures/     the picture of a speaking.picture item: pictures/<id>.<ext>, required
```

The catalog is described in the comments at the top of each of its files.

## Item IDs

- **The ID is the file name,** of the form `<task type>.<NNN>`: `content/items/reading/true_false/reading.true_false.001.yaml` has the ID `reading.true_false.001`. It is not a field of the file; the build adds it to the bundle. A new item takes the next free number of its type.
- **An item is never deleted or renamed.** The results in the app refer to it by its ID. An item that is no longer wanted gets `retired: true`: the app stops offering it, and keeps it for the old results. The validator fails if a file that git ever had under `content/items/` is missing.
- **Fix in place** the typos, the wording and the explanations.
- **Make a new item** when the questions, their order or the key of an item change, once it has reached the phone, and retire the old one: the review of mistakes refers to a question by its number, and the results keep the user's answers against the old key.
- **Audio and pictures** are found by the ID of their item; there is no field for them. A listening item without audio is spoken by the system TTS.

## YAML

The files are YAML 1.2, read with ruamel.yaml.

- **Prose in folded block scalars** (`>-`): the lines of a paragraph are joined with spaces, and a blank line separates paragraphs. In the bundle, a line break in a text is a paragraph break.
- **Lines kept as they are in literal block scalars** (`|-`): only the opening of a writing item.
- **A number where the format has a string** must be quoted (`'1935'`); the validator requires a string. `true` and `false` are booleans, and only the `answer` of a true/false statement takes them. `no` and `yes` stay strings.
- **No colon at the end of a plain value:** YAML reads `ήταν:` as the start of a mapping. The labels of the notes are written without their colon, and the app adds it.
- **Greek punctuation:** the question mark is `;` (U+003B), the apostrophe of elision `’` (U+2019), the quotation marks `«»`.
- **Comments** (`#`) are for the author; they are not in the bundle. A comment after a block scalar must be less indented than its text, or it becomes part of the text.

## Fields of every item

Each item has these fields, then the body of its task type. Greek fields are in Greek, the others in English.

| Field | Value |
|---|---|
| `task_type` | The ID of the task type, from [task_types.yaml](catalog/task_types.yaml); it matches the directories of the path. |
| `situation` | The ID of one situation, from [topics.yaml](catalog/topics.yaml): the one that fits the main subject. The topic is the first part of the ID. |
| `functions` | A list of the IDs of the functions that the task asks for, from [functions.yaml](catalog/functions.yaml). Writing and speaking: 1–4, required. Reading and listening: optional. |
| `retired` | `false`, or `true` for an item that the app no longer offers. |
| `title` | Greek. A short title: the title of the text, or of the task when the text has none. The app shows it in lists. |
| `framing` | Greek, second person plural. The paragraph that opens the task in the exam: the situation, and what to do ("…, όπως στο παράδειγμα"). In writing, the whole prompt with the length "(150 λέξεις)". |
| `instruction` | Greek. The note that the exam prints in the ΠΡΟΣΕΞΤΕ box, without that word: the number of answers and what else to watch ("Υπάρχουν τρεις φράσεις που δεν πρέπει να χρησιμοποιήσετε."). In writing, the name to sign with. In speaking, what the user does. One standard wording per type, modeled on the samples. |

A field that is not in the format is an error, and so is a missing one, except where a field is marked optional.

## Parts that several types share

### The example and the questions

A reading or listening item gives its example in `example`, solved, with the same fields as a question, and its questions in a list (`statements`, `questions`, `sentences`, `gaps`, `lines`). The example is number 0, and the questions are numbered from 1 in the order of the list. A grid has no separate example: the example is a tick in its first row.

### Explanations

Each question of a reading or listening item, the example included, has an `explanation`: one sentence in English that says why the answer is right, quoting the part of the text that decides it. A grid has one per row, for all the ticks of the row.

### Short answers and their key

The key of a note or of a gap of `listening.gap_fill` is `answers`: a list of the right answers, the notation `a / b` of [assessment-criteria.md](../docs/assessment-criteria.md#matching-the-notes-against-the-key). Words in brackets may be left out: `εργοστάσιο (σταφίδας)`. The app shows the list joined by ` / `.

### Gaps in a text

A gap is `{N}` in the text, where N is its number: `{0}` is the example, and every number occurs once, in order. The list `gaps` gives the key of the gaps 1, 2, …

### Banks

In `reading.matching` and `reading.gap_fill`, each question carries its right answer, and `distractors` lists the wrong entries. The app makes the bank (the right answers, the example's included, and the distractors) and shows it in a mixed order of its own. The key cannot point to the wrong entry, and the recorded answer is the text of the entry, whatever the order.

### Grids

| Field | Value |
|---|---|
| `rows` | The rows, in order. Each row: |
| `rows[].heading` | Optional. Greek. Starts a group of rows under a short heading (`Ιστορία`, `Επικοινωνία`); the rows that follow belong to it up to the next heading. |
| `rows[].statement` | Greek. The statement or the feature. |
| `rows[].example` | First row only, and required there: the column of the example tick. |
| `rows[].ticks` | The columns of the scored ticks of the row; may be empty in the first row. |
| `rows[].explanation` | English. Why these columns. |

The columns are referred to by their names. A name is written in normal case (`Άγρια Φύση`, not `ΑΓΡΙΑ ΦΥΣΗ`); the app chooses the style.

### The script of a listening item

| Field | Value |
|---|---|
| `speakers` | The speakers. Each: `name` (Greek, as the transcript shows it: `Δημοσιογράφος`, `Κώστας`), `gender` (`female`, `male`) and `age` (`child`, `young`, `adult`, `senior`), so that the audio gives each one a voice. |
| `script` | The turns in order. Each: `speaker` (a `name` of `speakers`) and `text` (Greek). A monologue is one speaker with one turn per paragraph. |

The audio is made from the script, and the app shows the script as the transcript after the check.

## The body of each task type

### `listening.notes`

`speakers`, `script`, and:

| Field | Value |
|---|---|
| `max_words` | `3` or `4`: the limit of a note, as the instruction states it. |
| `example` | `label` and `answers` of the example line, and its `explanation`. |
| `lines` | The labeled lines. Each: `label` (Greek, without the colon: `Ηλικία του παππού, όταν ήρθε στην πόλη`), `answers`, `explanation`. A group of lines whose answers may come in either order is one entry with `any_order` in place of `answers`: a list with the `answers` of each line of the group. It counts as that many lines. |

### `listening.comparison`

`speakers`, `script`, and:

| Field | Value |
|---|---|
| `columns` | Greek. The names of the people, organizations or places compared, as the script names them. |
| `rows` | The grid, see [Grids](#grids). |

### `listening.true_false`

`speakers`, `script`, and:

| Field | Value |
|---|---|
| `example` | As a statement. |
| `statements` | Each: `statement` (Greek), `answer` (`true` for ΣΩΣΤΟ, `false` for ΛΑΘΟΣ), `explanation`. |

### `listening.gap_fill`

`speakers`, `script`, and:

| Field | Value |
|---|---|
| `text` | Greek. The short text with the gaps `{0}`, `{1}`, … |
| `example` | `answers` and `explanation` of gap 0. |
| `gaps` | Each: `answers`, `explanation`. |

### `listening.multiple_choice`

`speakers`, `script`, and:

| Field | Value |
|---|---|
| `example` | As a question. |
| `questions` | Each: `question` (Greek: a question or the start of a sentence), `options` (Greek: the three options in the order α, β, γ), `answer` (`α`, `β` or `γ`), `explanation`. |

### `reading.matching`

| Field | Value |
|---|---|
| `intro` | Optional. Greek. The paragraph before the sentences. |
| `example` | As a sentence: sentence 0. |
| `sentences` | Each: `first` (Greek: the first part, where the exam breaks it off; the app adds "…"), `second` (the right second part, with the punctuation at its end), `explanation`. |
| `distractors` | Greek. The second parts that belong to no sentence. |

The text is `intro`, then each sentence with its right second part.

### `reading.comparison`

| Field | Value |
|---|---|
| `texts` | The short texts in order; they are the columns of the grid. Each: `name` (Greek: the name of the shop, the person, …) and `text` (Greek). |
| `rows` | The grid, see [Grids](#grids). |

### `reading.true_false`

| Field | Value |
|---|---|
| `text` | Greek. The text. |
| `example`, `statements` | As in `listening.true_false`. |

### `reading.gap_fill`

| Field | Value |
|---|---|
| `text` | Greek. The text with the gaps `{0}`, `{1}`, … |
| `example` | `answer` (the right bank entry of gap 0) and `explanation`. |
| `gaps` | Each: `answer`, `explanation`. |
| `distractors` | Greek. The bank entries that fit no gap. |

### `writing.informal`, `writing.formal`

| Field | Value |
|---|---|
| `genre` | One of the genres of the task type, from [task_types.yaml](catalog/task_types.yaml). |
| `points_to_cover` | Greek, second person plural. The points that the text must cover, also when the framing states them. |
| `opening` | Greek, a literal block. The start of the text as the exam prints it: the place and date line, the salutation and the first words; for an article, its title. |
| `signature` | Greek. The name to sign with, never the user's own; `instruction` names it too. |
| `model_answer` | Optional. Greek. A model text of about 150 words, from the opening to the signature, shown after the assessment. |

### `speaking.interview`

| Field | Value |
|---|---|
| `questions` | Each: `question` (Greek), and optionally `follow_up` (Greek), asked when the answer is short. |

### `speaking.opinion`

| Field | Value |
|---|---|
| `question` | Greek. The question that asks for a view with reasons. |
| `follow_ups` | Greek. The questions that the examiner asks when the answer is short. |

### `speaking.picture`

The picture is `content/pictures/<id>.<ext>`.

| Field | Value |
|---|---|
| `description` | English. What the picture shows, where the people are and how they feel, for the LLM, which does not see the picture. |
| `picture_questions` | Greek. The examiner's questions on the picture: what you see, where the people are, how they feel and why. |
| `personal_questions` | Greek. The personal questions on the topic that follow. |

### `speaking.roleplay`

| Field | Value |
|---|---|
| `kind` | One of the kinds of role play, from [task_types.yaml](catalog/task_types.yaml). |
| `register` | `informal` (εσύ) or `formal` (εσείς), as the relation of the roles needs. |
| `role_a`, `role_b` | The two roles. The user plays either, and the app the other. Each: |
| `role_*.card` | Greek, second person plural. The role card, complete without the other one. |
| `role_*.arguments` | Greek. 2–4 short sentences that the app uses when it plays this role. |
| `role_*.gives_way` | English. When the app, playing this role, gives way or agrees, so that the talk comes to an end. |

## Validation

`tools/content.py validate` checks the catalog, every item, the audio and the pictures, and that the tables of the docs equal the catalog. It prints each problem as `path:line: error: field: message`, or `warning`, and fails if there is an error.

- **Errors:** a file that YAML cannot read; a field missing, unknown or of the wrong type; a path that does not match the ID; a reference to an ID that is not in the catalog, or is retired while the item is not; a wrong count or shape from the checks of [docs/task-types.md](../docs/task-types.md); a missing picture, or an audio or picture file of no item; a file that git had under `items/` and that is gone.
- **Warnings:** a length or a balance of answers outside the range of [docs/task-types.md](../docs/task-types.md), a key answer that the script does not seem to contain or that comes out of order, and the other checks marked "(warning)" there. They ask the author to look; an item may keep a warning when the text needs it.
- **Beyond docs/task-types.md,** the validator checks the shape that this format gives: the speakers of a script have unique names and each one speaks, and each turn names one of them; the example tick of a grid is in its first row only, every tick names a column, and no cell is ticked twice; the gaps in a text are `{0}` to `{N}`, once each and in order, for the N questions; the entries of a bank, the right answers and the distractors, are all different.
- **A key answer in the script:** each word of more than 3 letters of the answer, without its optional words, must occur in the script, the same or with an ending changed (up to its last 3 letters), with accents and capitals ignored.

## Examples

One per answer format, shortened: a comment marks what is left out. The examples of `choice`, `short_text`, `text` and `dialogue` come from the sample set in `items/`, the items that tested the pipeline (story 4); the others have no item yet.

### `choice`: `reading.true_false.001`

```yaml
task_type: reading.true_false
situation: past_present.memories
retired: false
title: Το πρώτο μου ποδήλατο
framing: >-
  Διαβάζετε σε ένα περιοδικό την ιστορία ενός αναγνώστη. Καθώς διαβάζετε, σημειώνετε
  ΣΩΣΤΟ για τις προτάσεις που συμφωνούν με το κείμενο ή ΛΑΘΟΣ για τις προτάσεις που
  δεν συμφωνούν, όπως στο παράδειγμα.
instruction: Πρέπει να σημειώσετε συνολικά ΕΞΙ (6) απαντήσεις χωρίς το παράδειγμα.
text: >-
  Ήμουν δέκα χρονών, όταν ο πατέρας μου μού έφερε το πρώτο μου ποδήλατο. Δεν ήταν
  καινούργιο: το είχε αγοράσει από έναν γείτονα και το είχε βάψει κόκκινο.

  Κάθε απόγευμα, μετά το σχολείο, έκανα βόλτες στη γειτονιά με τους φίλους μου.
# … the rest of the text
example:
  statement: Ο αφηγητής πήρε το πρώτο του ποδήλατο από τον πατέρα του.
  answer: true
  explanation: '"ο πατέρας μου μού έφερε το πρώτο μου ποδήλατο": his father brought it.'
statements:
  - statement: Ο πατέρας αγόρασε το ποδήλατο από ένα κατάστημα.
    answer: false
    explanation: '"το είχε αγοράσει από έναν γείτονα": from a neighbor, not from a shop.'
  # … 5 more statements
```

### `assign`: `reading.gap_fill.001`

```yaml
task_type: reading.gap_fill
situation: health.lifestyle
retired: false
title: Περπάτημα για όλους
framing: >-
  Βρήκατε στο διαδίκτυο το πιο κάτω κείμενο για το περπάτημα. Μερικές λέξεις όμως δε
  φαίνονται καλά. Στον πίνακα υπάρχουν οι λέξεις που ταιριάζουν στα κενά του κειμένου.
  Συμπληρώνετε δίπλα σε κάθε λέξη τον αριθμό του κενού στο οποίο ταιριάζει, όπως στο
  παράδειγμα.
instruction: >-
  Οι αριθμοί που πρέπει να συμπληρώσετε είναι ΕΠΤΑ (7) χωρίς το παράδειγμα. Υπάρχουν
  έξι λέξεις που δεν ταιριάζουν σε κανένα κενό.
text: >-
  Το περπάτημα είναι {0} πολύ απλός τρόπος άσκησης. Δε χρειάζεται ακριβά
  ρούχα ούτε {1} εξοπλισμό.
# … the gaps 2–7
example:
  answer: ένας
  explanation: '"απλός τρόπος" is masculine nominative singular, so the article is "ένας".'
gaps:
  - answer: ειδικό
    explanation: '"εξοπλισμό" is masculine accusative singular, and so is "ειδικό".'
  # … 6 more gaps
distractors:
  - μία
  - ειδικής
  # … 4 more distractors
```

### `grid`: `reading.comparison.001`

```yaml
task_type: reading.comparison
situation: education.adult_courses
retired: false
title: Μαθήματα για ενήλικες
framing: >-
  Θέλετε να παρακολουθήσετε ένα μάθημα στον ελεύθερο χρόνο σας. Διαβάζετε στην
  εφημερίδα τις αγγελίες πέντε σχολών και στον πίνακα σημειώνετε με ✓ τι προσφέρει
  κάθε σχολή, όπως στο παράδειγμα.
instruction: >-
  Πρέπει να σημειώσετε συνολικά ΔΩΔΕΚΑ (12) ✓ χωρίς το παράδειγμα. Μια πληροφορία
  μπορεί να ταιριάζει σε περισσότερες από μία σχολές.
texts:
  - name: Ελληνικά για όλους
    text: >-
      Μαθήματα ελληνικών σε μικρές ομάδες, δύο φορές την εβδομάδα, το απόγευμα. Οι
      δάσκαλοι έχουν μεγάλη πείρα.
  - name: Η κουζίνα της γιαγιάς
    text: >-
      Μάθετε να μαγειρεύετε παραδοσιακά φαγητά. Τα μαθήματα γίνονται κάθε Σάββατο
      το πρωί. Έκπτωση 20% για ζευγάρια.
  # … 3 more texts
rows:
  - statement: Μαθήματα το Σαββατοκύριακο
    example: Η κουζίνα της γιαγιάς
    ticks: []
    explanation: '"κάθε Σάββατο το πρωί": the only school with lessons at the weekend.'
  - statement: Έκπτωση στην τιμή
    ticks: [Η κουζίνα της γιαγιάς]
    explanation: '"Έκπτωση 20% για ζευγάρια".'
  # … 5 more rows
```

### `short_text`: `listening.notes.001`

```yaml
task_type: listening.notes
situation: past_present.life_stories
retired: false
title: Ο φούρνος της γειτονιάς
framing: >-
  Θα ακούσετε δύο (2) φορές μια συνέντευξη με τον ιδιοκτήτη ενός φούρνου. Κρατάτε
  πολύ σύντομες σημειώσεις (1-3 λέξεις), όπως στο παράδειγμα.
instruction: Οι σημειώσεις που πρέπει να κρατήσετε είναι ΔΩΔΕΚΑ (12) χωρίς το παράδειγμα.
speakers:
  - name: Δημοσιογράφος
    gender: female
    age: adult
  - name: Κώστας
    gender: male
    age: senior
script:
  - speaker: Δημοσιογράφος
    text: >-
      Καλημέρα σας. Σήμερα βρισκόμαστε σε έναν παλιό φούρνο της Λευκωσίας. Κύριε Κώστα,
      πότε ανοίξατε τον φούρνο σας;
  - speaker: Κώστας
    text: >-
      Τον άνοιξα το 1987, μαζί με τη γυναίκα μου. Τότε η γειτονιά ήταν ακόμα μικρή και
      είχε λίγα σπίτια.
  # … 4 more turns
  - speaker: Δημοσιογράφος
    text: Και τι πουλάτε περισσότερο;
  - speaker: Κώστας
    text: >-
      Το χωριάτικο ψωμί, φυσικά, και τα κουλούρια με σουσάμι. Τα παιδιά τα παίρνουν
      κάθε πρωί για το σχολείο. Το Πάσχα φτιάχνουμε και φλαούνες, και τότε δεν προλαβαίνουμε.
  # … the rest of the script
max_words: 3
example:
  label: Χρονολογία που άνοιξε ο φούρνος
  answers: ['1987']
  explanation: '"Τον άνοιξα το 1987".'
lines:
  - label: Άνοιξε τον φούρνο μαζί με
    answers: [(τη) γυναίκα (του), (τη) γυναίκα μου]
    explanation: '"μαζί με τη γυναίκα μου".'
  - label: Τα προϊόντα που πουλά περισσότερο
    any_order:
      - [(χωριάτικο) ψωμί]
      - [κουλούρια (με σουσάμι)]
    explanation: '"Το χωριάτικο ψωμί, φυσικά, και τα κουλούρια με σουσάμι".'
  # … 9 more lines
```

### `text`: `writing.formal.001`

```yaml
task_type: writing.formal
situation: services.authorities
functions: [complain, give_reasons, request]
retired: false
title: Σκοτεινός δρόμος
framing: >-
  Τα φώτα στον δρόμο όπου μένετε, στη Λεμεσό, δεν ανάβουν εδώ και έναν μήνα. Το βράδυ ο δρόμος
  είναι πολύ σκοτεινός και οι γείτονές σας φοβούνται να περπατούν εκεί. Έχετε
  τηλεφωνήσει δύο φορές στον δήμο, αλλά κανείς δεν ήρθε. Γράφετε μια επιστολή στον
  δήμαρχο, για να πείτε τα παράπονά σας και να ζητήσετε να λυθεί το πρόβλημα.
  (150 λέξεις)
instruction: >-
  Στο τέλος της επιστολής μην υπογράψετε με το όνομά σας. Υπογράψτε ως Ελένη
  Κωνσταντίνου.
genre: formal_letter
points_to_cover:
  - Περιγράψτε το πρόβλημα και πείτε από πότε υπάρχει.
  - Εξηγήστε γιατί είναι επικίνδυνο για τους κατοίκους.
  - Ζητήστε να διορθωθεί σύντομα.
opening: |-
  Λεμεσός, 3 Νοεμβρίου
  Αξιότιμε κύριε Δήμαρχε,
  Σας γράφω για να
signature: Ελένη Κωνσταντίνου
# … the model answer
```

### `speech`: `speaking.picture.001`

With the picture `content/pictures/speaking.picture.001.jpg`.

```yaml
task_type: speaking.picture
situation: relations.celebrations
functions: [describe, express_feelings]
retired: false
title: Γενέθλια σε ταβέρνα
framing: >-
  Κοιτάξτε την εικόνα για ένα λεπτό. Μετά ο εξεταστής θα σας κάνει μερικές ερωτήσεις.
instruction: Περιγράψτε την εικόνα και απαντήστε στις ερωτήσεις.
description: >-
  A family of five at a table in a small taverna in the evening. A grandmother blows
  out the candles on a birthday cake, two children laugh and clap, and a waiter brings
  plates. Everyone looks happy.
picture_questions:
  - Τι βλέπετε στην εικόνα;
  - Πού βρίσκονται αυτοί οι άνθρωποι;
  - Πώς νιώθουν; Γιατί;
personal_questions:
  - Πώς γιορτάζετε εσείς τα γενέθλιά σας;
  - Σας αρέσει να τρώτε έξω με την οικογένειά σας; Γιατί;
```

### `dialogue`: `speaking.roleplay.001`

```yaml
task_type: speaking.roleplay
situation: leisure.going_out
functions: [express_preference, give_reasons, persuade, agree_disagree]
retired: false
title: Σινεμά ή θέατρο;
framing: Εσείς και ένας φίλος σας θέλετε να βγείτε το Σάββατο το βράδυ.
instruction: >-
  Διαβάστε τον ρόλο σας και συζητήστε με τον φίλο σας. Στο τέλος πρέπει να αποφασίσετε
  τι θα κάνετε.
kind: disagreement
register: informal
role_a:
  card: >-
    Θέλετε να βγείτε το Σάββατο το βράδυ με έναν φίλο σας. Προτείνετε να πάτε σινεμά,
    γιατί παίζει μια καινούργια κωμωδία και θέλετε να γελάσετε. Ο φίλος σας προτιμά το
    θέατρο. Του εξηγείτε γιατί το σινεμά είναι καλύτερη ιδέα και προσπαθείτε να τον
    πείσετε. Αυτός όμως επιμένει.
  arguments:
    - Τα εισιτήρια του σινεμά είναι πιο φτηνά.
    - Η ταινία κρατάει λιγότερο, έτσι δεν θα γυρίσουμε αργά.
    - Μετά από μια δύσκολη εβδομάδα θέλω κάτι ελαφρύ.
  gives_way: Agrees to the theater if B promises to go to the cinema next Saturday.
role_b:
  card: >-
    Θέλετε να βγείτε το Σάββατο το βράδυ με έναν φίλο σας. Ο φίλος σας προτείνει να
    πάτε σινεμά, αλλά εσείς θέλετε να δείτε μια παράσταση στο θέατρο της πόλης σας.
    Του εξηγείτε γιατί προτιμάτε το θέατρο και προσπαθείτε να τον πείσετε. Αυτός όμως
    επιμένει.
  arguments:
    - Ταινίες βλέπουμε κάθε εβδομάδα στο σπίτι.
    - Η παράσταση παίζεται μόνο αυτό το Σαββατοκύριακο.
    - Η παράσταση είναι μια καλή ευκαιρία να δούμε κάτι διαφορετικό.
  gives_way: Agrees to the cinema if A promises to come to the theater another time.
```
