# B1 assessment criteria

How each part of the UCY B1 exam is scored: the item scoring of reading and listening, and the criteria, weights and band descriptors of writing and speaking. The parts and tasks themselves are in [exam-spec.md](exam-spec.md). This document is the reference for the automatic scoring of reading and listening (stories 6, 7) and for the assessment prompts of the LLM (story 9).

The IDs and the data of the tables of the writing and speaking criteria and of their descriptors come from [content/catalog/criteria.yaml](../content/catalog/criteria.yaml), their one source. `tools/content.py docs` writes each table between its `<!-- catalog: … -->` markers; change the catalog and run it, never the tables. The rest of this document is written by hand.

## How to read this document

UCY publishes no assessment criteria (see [docs/sources/README.md](sources/README.md)). KEG publishes a holistic scale for B1 writing ([keg-writing]), five analytic criteria and a holistic scale for B1 speaking ([keg-speaking]), and the points per item for reading and listening ([keg-structure]). The UCY task design is the KEG one (see [exam-spec.md](exam-spec.md#how-to-read-this-document)), so the KEG criteria are used.

The basis of each fact is marked as in the exam specification, with one more mark:

| Basis | Meaning |
|---|---|
| **UCY** | A UCY document says so. |
| **KEG** | A KEG document says so, and UCY says nothing on the point. We assume UCY does the same. |
| **Samples** | Seen in the two KEG B1 sample papers. |
| **Derived** | Our own restructuring of a KEG text: the content is KEG's, the arrangement is ours (for example, a holistic band split into criteria). |
| **Estimate** | Our rule where no source gives one; the reason is given. |

The descriptors are given in our own English wording, not as a copy of the KEG text, since the repository is public and the KEG documents are under copyright. The Greek originals are in [docs/sources/](sources/README.md). Greek terms are kept where the exam uses them.

## Pass mark and the total

| Rule | Basis |
|---|---|
| Each part is scored out of 25. | UCY ([ucy-structure]) |
| A part is passed with at least 60%, that is 15 of 25. Failing one part fails the whole exam. | UCY ([ucy-regs] C.1–2) |
| The final score, on the certificate, is 0–100: the sum of the four parts. | UCY for the scale ([ucy-regs] C.3); the sum Estimate, since four parts of 25 make 100 |
| Re-evaluation: on request within 10 days, by a review committee, without the candidate; one request only. | UCY ([ucy-regs] D) |
| How the parts are rounded (reading can give half points): unknown. The app keeps half points and shows the total with one decimal. | Estimate |

## Reading and listening: item scoring

The answers are closed (numbers, ticks) except the listening notes. All are scored by the app, without the LLM.

### General rules

| Rule | Basis |
|---|---|
| Item 0, the example, earns no points. | KEG ([keg-structure]: "+ 1 παράδειγμα"); Samples |
| A right answer earns the points of its item; a wrong or missing answer earns 0. There are no negative points. | KEG ([keg-structure]) |
| The instructions give the number of answers. More answers than that can make the whole task score 0. | KEG ([keg-guide] p. 27) |
| The app does not let the user give more answers than the task asks for, so the zero rule never applies in the app. A grid with a fixed number of ticks needs no penalty for a wrong tick: each wrong tick is a right one missing. | Estimate |
| Answers in the notes task are written in small letters, not only in capitals. | KEG ([keg-guide] p. 22) |

### Points per task

| Part | Task | Answer unit | Points | Basis |
|---|---|---|---|---|
| Reading | Matching sentence halves | 1 number per first half (6) | 1 each, 6 in total | KEG ([keg-structure]) |
| Reading | Comparison, table completion | 1 tick (12) | 0.5 each, 6 in total | KEG ([keg-structure]) |
| Reading | True/false | 1 tick per statement (6) | 1 each, 6 in total | KEG ([keg-structure]) |
| Reading | Gap fill from a word bank | 1 gap number per word (7) | 1 each, 7 in total | KEG ([keg-structure]) |
| Listening | Notes | 1 short note (12) | 1 each, 12 in total | KEG ([keg-structure]) |
| Listening | Comparison, table completion | 1 tick (13) | 1 each, 13 in total | KEG ([keg-structure]) |
| Listening | True/false | 1 tick per statement | 1 each | Estimate: as in reading |
| Listening | Gap fill in a text | 1 word or phrase per gap | 1 each | Estimate: as in reading |
| Listening | Multiple choice, 3 options | 1 option per question | 1 each | Estimate |

For the three UCY listening types that KEG does not use, the number of items, and so the share of the 25 points, is unknown (see [exam-spec.md](exam-spec.md#gaps)). Practice sets of these types report the share of right items; the mock exam uses the KEG pair, notes 12 + comparison 13.

### Matching the notes against the key

The notes are the only open answers. The rules of the key, from the samples ([keg-a-listening], [keg-b-listening]):

| Key notation | Meaning | Example |
|---|---|---|
| `a / b` | Either answer is right. | "πήγαν σχολείο / σπούδασαν" |
| `(…)` | The words in brackets may be left out. | "εργοστάσιο (σταφίδας)", "η (βόρεια) Πίνδος" |
| Two items with one heading | The two answers may come in either order. | "Τα μέρη όπου συναντάμε αρκούδες: 3. … 4. …" |

What else counts as right is not published. KEG says only that spelling counts little at the lower levels, but fully phonetic spelling is not accepted ([keg-guide] p. 26). The app's rule (Estimate):

1. Normalize both sides: small letters, single spaces, no punctuation around the answer, the optional words of the key dropped or kept.
2. **Right** if it equals one of the alternatives of the key. Numbers count in digits or in words ("12" = "δώδεκα").
3. **Right, with a spelling note** if it differs from an alternative only in the accent marks, or by at most 1 letter in a word of up to 6 letters and 2 letters in a longer word.
4. **Wrong** otherwise. The app shows the key, and the user can mark the answer as right when it says the same in other words; the result keeps that the user overrode it. When online, the LLM can be asked instead, with the transcript and the key (story 7 decides).

## Writing

### What B1 writing covers

Besides the texts of A1 and A2, a B1 candidate writes (KEG: [keg-writing]):

- short personal messages to friends or acquaintances: congratulations, invitations;
- formal letters: a job application, a complaint to a company, a hotel booking, a letter to a landlord;
- a short report of an accident;
- simple descriptions of people and objects;
- information, impressions and personal opinions.

UCY part A is an informal text (a friendly letter, an announcement, an advertisement), part B a formal one (personal information, a reasoned request for leave, a reasoned complaint) (UCY: [ucy-structure]).

### The score

| Rule | Basis |
|---|---|
| Part A earns 12 points, part B 13; 25 in total. | KEG ([keg-structure]) |
| The B1 writing scale is holistic: 25 points in five bands of five points. The rater chooses the band whose description fits the text best, then a point within it. | KEG ([keg-writing]) |
| How the 25-point scale gives the 12 and 13 points of the two parts is not published. The app rates each text on the 25-point scale and takes 12/25 of it for part A and 13/25 for part B, rounded to 0.5. So the writing total is the weighted mean of the two ratings: two texts rated 16 give 16 of 25. | Estimate |
| The pass mark of 15 is the top point of band 3. A text that fits band 3 passes only at its top; the target is band 4. | Derived from the pass mark and the scale |

### Criteria

The KEG scale describes each band in one paragraph that covers six aspects. The app rates each aspect on its own (1–5, one value per band), to give feedback per criterion, and then gives the holistic score. The aspects are taken from the scale; the split is ours (Derived).

<!-- catalog: writing_criteria -->

| ID | Criterion | Greek terms of the scale | What it covers |
|---|---|---|---|
| `task` | Content and task | θεματικό κέντρο, ανάπτυξη του θέματος, αριθμός λέξεων | The text is on the topic of the task, covers all the points it asks for, develops them, and has the required length. |
| `organization` | Organization and cohesion | οργάνωση, παραγραφοποίηση, συνοχή, συνεκτικότητα, μεταβατικοί μηχανισμοί, αλληλουχία νοημάτων | Paragraphs, the order of the ideas, connectors and other links between sentences. |
| `register` | Register | ύφος (τυπικό, φιλικό) | Formal or friendly style as the task needs: salutation, forms of address (εσύ / εσείς), set phrases of the text type, closing. |
| `vocabulary` | Vocabulary | λεξιλόγιο, ολοφράσεις | Range, accuracy and fitness of the words; the set phrases (ολοφράσεις) expected for the text type. |
| `grammar` | Grammar | μορφοσυντακτικά λάθη, γλωσσικές δομές | Morphology and syntax: forms of nouns and verbs, agreement, the structures of the level. |
| `mechanics` | Spelling, accents and punctuation | ορθογραφία, τονισμός, στίξη, κεφαλαιογράμματη γραφή | Spelling, the accent marks, punctuation; writing in capitals only. |

<!-- /catalog -->

### Band descriptors

Our rendering of the KEG B1 writing scale ([keg-writing]), split by criterion (Derived). Each band applies to the points shown.

<!-- catalog: writing_bands -->

**Band 1 (1–5 points)**

| Criterion | Descriptor |
|---|---|
| `task` | The content has nothing to do with the topic. |
| `organization` | No organization: sentences and phrases in no order; no sequence of ideas. |
| `register` | No difference between formal and friendly style. |
| `vocabulary` | Extremely poor, with very many errors. |
| `grammar` | So many errors of morphology and syntax that the message does not get across. |
| `mechanics` | Very many spelling errors; no punctuation; words divided wrongly; written in capitals; accent marks missing or wrong. |

**Band 2 (6–10 points)**

| Criterion | Descriptor |
|---|---|
| `task` | The content is related to the topic, though the link is hard to see; the ideas are not developed at all. |
| `organization` | Poorly organized, with many errors in paragraphing; neither cohesion nor coherence. |
| `register` | A few changes of style, mostly unsuccessful. |
| `vocabulary` | Very poor, and fairly often wrong or unfit. |
| `grammar` | Very many errors of morphology and syntax. |
| `mechanics` | Spelling errors make the text hard to understand; punctuation and accents are very weak. |

**Band 3 (11–15 points)**

| Criterion | Descriptor |
|---|---|
| `task` | The main point is partly clear; the development is limited or weak in some respects, but acceptable. The text is fairly long but somewhat short of the required number of words. |
| `organization` | The organization is visible, though cohesion or coherence fails in places. Paragraphing is attempted, not always with success. |
| `register` | The style fits only where a familiar style is needed: the friendly text works, the formal one does not. |
| `vocabulary` | Correct, but very limited in range and often repeated. The set phrases of the text type are attempted, clearly without success. |
| `grammar` | Many errors that are not negligible, though the text attempts structures that are complex for the level; only some of the errors are serious. |
| `mechanics` | Spelling, accent and punctuation errors, which together with the grammar errors can get in the way of understanding. |

**Band 4 (16–20 points)**

| Criterion | Descriptor |
|---|---|
| `task` | The main idea is very clear, but some aspects are not developed enough, so the content does not use all that the topic offers. |
| `organization` | Fairly well organized, with correct paragraphing and the successful use of some linking devices, basic ones for the level. |
| `register` | The right style in a good number of places. |
| `vocabulary` | Good, though not excellent; frequent use of the vocabulary specific to the topic; few errors. |
| `grammar` | The structures of the level are used effectively, but not always accurately. |
| `mechanics` | Very few spelling, accent and punctuation errors, none of them serious. |

**Band 5 (21–25 points)**

| Criterion | Descriptor |
|---|---|
| `task` | The main point is clear, and the arguments on the topic are fully developed. |
| `organization` | Faultless organization; the linking devices hold the text together very well; an excellent sequence of ideas. |
| `register` | The best-fitting choices of style. |
| `vocabulary` | Very precise use of the vocabulary of the level. |
| `grammar` | The structures are correct and, though simple, cover all that is taught at the level. |
| `mechanics` | No spelling errors; faultless accents; the punctuation serves the text. |

<!-- /catalog -->

### What the level expects

The KEG scale names the language that a B1 text is measured against ([keg-writing], footnotes 1–4). These lists are what "the structures of the level" and "the vocabulary of the level" mean in the descriptors, for writing and for speaking.

- **Connectors (cohesion):** those of A2, plus ούτε … ούτε, ενώ, ή … ή, είτε … είτε, καθώς, επειδή, μη(ν), ίσως, δηλαδή, αν και, ότι, πως, and που as a conjunction (that), in a causal sense and as a relative.
- **Vocabulary:** all of A2, plus the verbs that express knowledge, opinions, views and feelings; enough range to express oneself, be understood and meet one's needs, if necessary by paraphrase. Pronouns get particular weight.
- **Morphology:** all of A1, plus the periphrastic comparative (πιο + adjective), and the present, imperfect, future and aorist of verbs of the first and second conjugations, active and passive, in the subjunctive and in the perfective imperative, formed correctly for the subject, the time and the voice.
- **Syntax:** the genitive, some auxiliary and impersonal verbs, the passive, reported speech, and subordinate clauses: concessive, relative, adverbial, and the first type of conditional.
- **Punctuation:** the apostrophe, the dash, the ano teleia (·), the colon, parentheses, and the rules for dropping or keeping final -ν.

### Rules that touch the score

| Rule | Effect | Basis |
|---|---|---|
| A text off the topic | Band 1 for `task`, and so for the whole text. | KEG ([keg-writing]) |
| Fewer words than required | The scale puts "somewhat short of the number of words" in band 3. The app counts the words itself and gives the count to the LLM: under 120 words (80% of 150), `task` and the holistic score cannot go above band 3; under 75 words, not above band 2. No limit above 150 words. | KEG for band 3; the word limits Estimate |
| Writing in capitals only | A feature of band 1 for `mechanics`. Matters only for handwriting (story 11). | KEG ([keg-writing], [keg-guide] p. 22) |
| The wrong style for the task | Formal text in friendly style, or the reverse: `register` no higher than band 3. | Derived from band 3 of [keg-writing] |
| The candidate's real name in the text | Forbidden; the effect on the score is not published. The app gives a name to sign with, and warns if the user's own name appears. | UCY ([ucy-regs] B.13) |

## Speaking

### The score

| Rule | Basis |
|---|---|
| Five criteria, 1–5 points each, 25 in total. So the lowest possible score is 5. | KEG ([keg-speaking], grade sheets of May 2026) |
| Each rater writes a value for each criterion on the grade sheet and adds them up. | KEG ([keg-speaking]) |
| The criteria apply to the whole performance of the candidate in the three parts; the grade sheet has no columns per part. | KEG (grade sheets) |
| Two raters, an examiner who leads the talk and an assessor, rate each candidate separately, without talking it over. They should be neither lenient nor strict. | KEG ([keg-speaking-instr]); UCY also has two examiners ([ucy-course]) |
| How the two ratings give the final score (mean, or a third rater on disagreement) is not published. | Unknown |
| The general descriptors are the same for all levels; the raters judge them against what the level requires. For that, KEG gives a holistic B1 scale of 25 points in five bands. | KEG ([keg-speaking]) |
| The pass mark of 15 is a mean of 3 per criterion. | Derived from the pass mark |

### Criteria

<!-- catalog: speaking_criteria -->

| ID | Criterion | Greek name | What it covers |
|---|---|---|---|
| `grammar` | Grammatical accuracy | Γραμματική ακρίβεια (μορφολογία, σύνταξη) | Morphology and syntax. The KEG heading also names spelling and punctuation; the grid is shared with writing, and they do not apply to speech. |
| `pragmatics` | Pragmatic appropriateness | Πραγματολογική καταλληλότητα (περιεχόμενο, είδος, ύφος) | The content and its organization, fitness to the kind of talk, register (εσύ / εσείς). |
| `pronunciation` | Pronunciation and intonation | Προφορά και επιτονισμός | Sounds, word stress, intonation, flow of speech. |
| `vocabulary` | Vocabulary | Λεξιλόγιο (εύρος, επιλογή) | Range and choice of words. |
| `communication` | Communicative effectiveness, completing the task | Αποτελεσματικότητα στην επικοινωνία / ολοκλήρωση του θέματος | Answers what is asked, completes the task, keeps the talk going; fluency. |

<!-- /catalog -->

### Descriptors per criterion

KEG describes three levels for each criterion: 1 point, 2–3 points and 4–5 points ([keg-speaking], our rendering):

<!-- catalog: speaking_points -->

| Criterion | 1 point | 2–3 points | 4–5 points |
|---|---|---|---|
| `grammar` | Very limited use of structures; many inaccuracies, or the needed forms and syntax are not known. | The structures of the level are used well enough; some inaccuracies, which hardly affect communication. | The structures needed are used fully effectively. |
| `pragmatics` | Content poorly presented or organized, and/or almost no fitting style. | Content of the talk adequately organized and presented; limited use of fitting style. | Content very effectively organized and presented; style fits the topic. |
| `pronunciation` | Unnatural flow; strong and frequent features of the native language's pronunciation that hinder understanding at once. | Frequent weaknesses of pronunciation from the native or another foreign language; the flow is not always normal. | Very good use of sounds and prosody. |
| `vocabulary` | Very limited vocabulary; many wrong words; frequent repetition. | Words chosen and used fairly well, with few errors that do not seriously affect communication; a satisfactory range. | Fully satisfactory knowledge and use of the vocabulary needed. |
| `communication` | Cannot complete the task; wrong elements make understanding impossible; a serious lack of fluency. | Satisfactory communication with the minimum content needed; anything irrelevant hardly affects communication. | Responds to the task very effectively. |

<!-- /catalog -->

KEG does not say how to choose between 2 and 3, or between 4 and 5. The app's rule (Estimate): the lower value when the description fits with clear weaknesses, the higher when it fits fully.

### The B1 holistic scale

Our rendering of the KEG B1 speaking scale ([keg-speaking]). The sum of the five criteria falls into one of these bands, and the band's description must fit the performance; if it does not, the criteria are reviewed (Derived: KEG gives both the criteria and the scale but not how they meet).

| Band | Descriptor |
|---|---|
| 1–5 | The content has nothing to do with the topic, and the meaning cannot be understood. Very poor and wrongly chosen vocabulary with constant repetition. Pronunciation and word stress often make the speech unintelligible; constant features of the native language. Poor content, very badly organized, with no sequence of ideas; wrong style. Cannot use even the most basic structures; so many inaccuracies that the speech cannot be understood. |
| 6–10 | Cannot complete the task: the content is related to the topic, though hard to see, but the ideas are not developed at all. The meaning is hard or impossible to follow; answers may be learned by heart or off the point. Very poor vocabulary, wrong words, frequent repetition. Pronunciation often unintelligible; word stress very weak. Poor content, badly organized in coherence and cohesion; wrong style. Weak in even the basic structures; many inaccuracies that harm understanding. |
| 11–15 | Partly adequate, not very effective communication. The content is often the minimum, so that important aspects of the task are left out. Some hesitation is accepted, but in places it gets in the way badly. The vocabulary of the level is lacking; repetition and wrong choices may affect communication. The sounds are just about right; the native language makes the speech hard to understand at times; intonation is not used systematically. Incomplete organization; signs of simple style features, but the style fits only where a familiar one is needed. Limited use of the structures of the level; some inaccuracies, which do not really affect communication. |
| 16–20 | Completes the task in part, fairly effectively and at ease. The main idea is very clear, but some aspects are not developed enough. Very little hesitation. A fairly good range of vocabulary, mostly used correctly; limited use of idioms and specialized words, as expected at the level. Few errors in sounds and intonation; good word stress. Ideas and content fairly well organized and presented, with the successful use of some basic linking devices; the basic style features of the level used effectively, though not always correctly; a good sequence of ideas. The structures of the level used fairly accurately and effectively, with few errors. |
| 21–25 | Completes the task fully, effectively and at ease. Very little hesitation. A fully satisfactory range of vocabulary, used correctly; some idioms and specialized words, as expected at the level. Correct sounds and intonation; faultless word stress. Ideas and content very well organized and presented, with the correct basic style features of the level; an excellent sequence of ideas. The structures of the level used very accurately and effectively. |

### What the app can and cannot judge

- **Pronunciation** cannot be judged from a transcript. When the LLM gets only the text of the speech recognition, `pronunciation` is not rated: the result shows the other four criteria out of 20, and the mock exam scales it to 25 and marks it as partial. When the model gets the audio, all five are rated. This is the open question "Assess speaking from audio?" of [DESIGN.md](../DESIGN.md#open-questions) (Estimate).
- **Hesitation and flow** can be judged in part from the timing of the transcript (pauses, words per minute); the app gives the timing to the LLM (Estimate).
- **One part at a time:** in practice of one part, the app rates that part alone on the five criteria, and says that the exam rates the three parts together (Estimate).

## For the LLM prompts (story 9)

The prompts of story 9 use this document as follows. The exact format of the input and the output is for story 9 to fix.

**Input, writing:** the task type (`writing.informal` or `writing.formal`, see [task-types.md](task-types.md)), the task text (situation, points to cover, required style, 150 words), the user's text, the word count made by the app, the band descriptors of this document, and the level lists of [What the level expects](#what-the-level-expects).

**Input, speaking:** the part or parts, the examiner's questions or the role cards (both roles), the picture's description, the transcript with the speakers and the timing, the audio when the model takes it, the criteria and descriptors of this document.

**Procedure:**

1. Check the rules that cap the score: off the topic; the word count; the wrong style.
2. Rate each criterion 1–5, with a short quote from the answer as evidence for each.
3. Writing: choose the holistic band that fits best, and the point within it: the first point when the text barely fits the band, the middle one when it fits, the top one when it also shows features of the next band (Estimate). The band should agree with most of the criteria; if it is more than one band away from them, review.
4. Speaking: add up the criteria, and check the sum against the holistic band.
5. List the errors: the quote, the correction, the criterion, a short explanation in English.
6. Give advice: two or three points, the ones that would raise the score most, given the pass mark of 15.

**Output sketch:**

```json
{
  "task_type": "writing.formal",
  "criteria": [
    { "id": "register", "value": 2, "evidence": "Γεια σου, κύριε Διευθυντά" }
  ],
  "score25": 14,
  "points": 7.5,
  "errors": [
    { "quote": "την τράπεζα σας", "correction": "την τράπεζά σας", "criterion": "mechanics", "note": "…" }
  ],
  "advice": ["…"]
}
```

**Two raters:** the exam has two independent raters. The app can do the same with two independent LLM calls and take the mean, and show a disagreement of more than one band as uncertain. Whether that is worth the cost is for story 9 to decide on real samples.

## Sources

All in [docs/sources/](sources/README.md), which also gives the origin and the retrieval date of each file.

- [ucy-structure]: UCY, B1 exam structure and task types (2025).
- [ucy-regs]: UCY, exam regulations (updated 2026-08-26); section and item numbers given.
- [ucy-course]: UCY School of Modern Greek, course rules (2025-02-04).
- [keg-structure]: KEG, B1 exam structure (2012).
- [keg-guide]: KEG, exam guide in Greek (2022); page numbers are the printed ones.
- [keg-writing]: KEG, B1 writing assessment criteria (2013): the scope of B1 writing, the holistic scale, and footnotes 1–4 on connectors, vocabulary, grammar and punctuation.
- [keg-speaking]: KEG, speaking assessment page (2026), with the general criteria, the holistic scale per level and the grade sheets of May 2026 (also saved as separate files); [keg-speaking-instr]: its instructions for the speaking exam.
- [keg-a-listening], [keg-b-listening]: KEG B1 sample papers, series A and B (2014), listening with the answer keys.

[ucy-structure]: sources/ucy-b1-exam-structure.pdf
[ucy-regs]: sources/ucy-exam-regulations-2026-08-26.pdf
[ucy-course]: sources/ucy-course-rules-2025-02-04.pdf
[keg-structure]: sources/keg-b1-exam-structure.pdf
[keg-guide]: sources/keg-exam-guide-el-2022.pdf
[keg-writing]: sources/keg-b1-writing-criteria.pdf
[keg-speaking]: sources/keg-speaking-assessment.pdf
[keg-speaking-instr]: sources/keg-speaking-instructions.docx
[keg-a-listening]: sources/keg-b1-sample-a-listening.pdf
[keg-b-listening]: sources/keg-b1-sample-b-listening.pdf
