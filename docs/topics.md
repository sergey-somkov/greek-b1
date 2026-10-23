# B1 topic catalog

The topics of the B1 exam, each with a stable ID, its typical situations, its vocabulary areas and the communicative functions it needs. Every content item names one situation of this catalog, and so one topic; the validator (story 4), the progress screen (story 5), the daily plan (story 15) and the coverage report (story 17) count by these IDs.

The task types are in [task-types.md](task-types.md); a content item has one task type and one situation, and the two are independent: any situation can be practised in any task type whose text type fits it.

## How to read this document

UCY publishes no topic list and no course syllabus (see [docs/sources/README.md](sources/README.md)). Its exam structure says only that the listening texts are about "the situations taught at this level", and names the subjects of the speaking interview ([ucy-structure]). The catalog is built from three other sources:

- **KEG, the B1 level page** ([keg-level]): the communicative situations of B1, in two groups (personal life; daily life), with what the candidate must be able to do in each. The public summary of the KEG syllabus, which is not online.
- **INEK, the Cyprus adult B1 syllabus** ([inek]): six themes, each with communicative goals, grammar and vocabulary. It names the UCY B1 syllabus as one of its sources, so it is the closest available view of what UCY teaches at B1.
- **The KEG sample papers** ([keg-a-reading] … [keg-b-speaking]): the topics that the exam really uses, item by item (see [Topics in the sample papers](#topics-in-the-sample-papers)).

Every topic and situation has a basis, marked as in [exam-spec.md](exam-spec.md#how-to-read-this-document), with one more mark:

| Basis | Meaning |
|---|---|
| **UCY** | A UCY document names it. |
| **KEG** | The KEG level page names it. |
| **INEK** | The INEK syllabus names it. |
| **Samples** | A KEG sample paper uses it. |
| **Estimate** | Ours; the reason is given. |

The vocabulary areas and the functions of each topic are drawn from the same sources; they are marked only where they are ours. The Greek words in the vocabulary areas are examples that show the area, not a word list to learn.

**How the topics were cut.** The KEG situations and the INEK themes overlap; the catalog takes their union, with these changes:

- KEG's "Education/workplace" is split into `education` and `work`, as INEK does.
- KEG's "Free time" is split into `leisure` and `sport` (Estimate): sport and exercise are 5 of the 47 sample items, as many as any topic but two.
- INEK's "Health – road safety" is split: health goes to `health`, road safety to `travel`, where KEG puts roads, signs and garages.
- KEG's "Activities at home" is merged into `home`; KEG's sociocultural competence (invitations, politeness) into `relations`.
- INEK's themes "Media" and "Then and now" become `media` and `past_present`; `media` also takes computers and the internet, which the samples use often.
- INEK's grammar per theme is not taken: B1 grammar spans all topics. The grammar points for the review of mistakes are for story 16.

## IDs

- **Topic:** one or more lowercase English words joined by `_`. Example: `past_present`.
- **Situation:** `<topic>.<situation>`, in the same form. Example: `work.interview`.
- **Function:** one or more lowercase English words joined by `_`, without a prefix, since a function serves many topics. Example: `give_reasons`.
- **Stable:** an ID in use is never renamed and never reused. The app keeps the results by ID, so a renamed ID would cut the history. An ID no longer needed stays in the catalog, marked as retired.
- **The catalog grows:** a content item that fits no situation gets a new situation in the catalog first, under the topic it fits best.

## Overview

| ID | Topic | Greek | Basis | Sample items |
|---|---|---|---|---|
| `personal` | Personal identity | Ταυτότητα, προσωπικά στοιχεία | UCY, KEG | 2 |
| `relations` | Family, friends and social life | Οικογένεια, φίλοι, κοινωνικές σχέσεις | KEG | 3 |
| `home` | Home and residence | Κατοικία, διαμονή | KEG | 0 |
| `environment` | Nature, environment and weather | Περιβάλλον, φύση, καιρός | KEG | 9 |
| `leisure` | Free time, entertainment and culture | Ελεύθερος χρόνος, διασκέδαση, πολιτισμός | KEG | 3 |
| `sport` | Sport and exercise | Αθλητισμός, άσκηση | KEG, Samples | 5 |
| `shopping` | Shopping and consumer goods | Αγορές, καταναλωτικά αγαθά | KEG | 5 |
| `food` | Food and eating | Διατροφή, φαγητό | KEG, INEK 5 | 1 |
| `health` | Health and the body | Υγεία | KEG, INEK 4 | 1 |
| `travel` | Travel and transport | Μετακίνηση, ταξίδια | KEG, INEK 4 | 2 |
| `education` | Education and learning | Εκπαίδευση | KEG, INEK 3 | 5 |
| `work` | Work and employment | Εργασία | KEG, INEK 2 | 3 |
| `services` | Public services and everyday business | Δημόσιες υπηρεσίες, συναλλαγές | KEG | 1 |
| `media` | Media and technology | ΜΜΕ, τεχνολογία | INEK 6, Samples | 6 |
| `past_present` | Then and now | Τότε και τώρα | INEK 1 | 1 |

- **Basis:** KEG for a section of the level page ([keg-level], "Καταστάσεις Επικοινωνίας"); INEK with the number of its theme ([inek]); UCY for the speaking interview ([ucy-structure]).
- **Sample items:** the number of items of the two KEG sample papers on the topic, out of 47 (see [Topics in the sample papers](#topics-in-the-sample-papers)).
- **The samples and the syllabi lean different ways.** The samples favour topics to argue about (the environment, the media, school, buying choices); the syllabi favour transactions (public services, the doctor, the home), which the samples hardly use. The exam can draw on both, and the two kinds train different things: opinions for speaking part B and articles, transactions for the role play and the formal letter. The catalog keeps both.

## Communicative functions

What the user must be able to do with the language, across topics. The list follows the KEG speaking and writing aims ([keg-level], "Ειδικοί Εκπαιδευτικοί Στόχοι"), the INEK goals per theme ([inek]), and the UCY writing tasks ([ucy-structure]: a reasoned request for leave, a reasoned complaint). The Greek phrases are typical exponents at B1, as a guide for the content and for the LLM; they are ours (Estimate).

| ID | Function | Greek | Typical exponents | Mostly in |
|---|---|---|---|---|
| `give_information` | Give information about oneself or a matter | Δίνω πληροφορίες | «Μένω στη Λεμεσό εδώ και τρία χρόνια.» «Δουλεύω ως…» | `speaking.interview`, `writing.formal` |
| `ask_information` | Ask for information | Ζητώ πληροφορίες | «Θα ήθελα να μάθω…» «Μπορείτε να μου πείτε πότε…;» | `speaking.roleplay`, `writing.formal` |
| `describe` | Describe people, things, places, a picture, a problem | Περιγράφω | «Στην εικόνα βλέπω…» «Είναι ψηλός και έχει καστανά μαλλιά.» | `speaking.picture`, `writing.informal` |
| `narrate` | Tell events and experiences, the plot of a book or a film | Αφηγούμαι | «Πριν από δύο χρόνια…» «Στην αρχή…, μετά…, στο τέλος…» | `writing.informal`, `speaking.opinion` |
| `compare` | Compare things, places, the past and the present | Συγκρίνω | «πιο οικονομικό από…» «ενώ…» «σε αντίθεση με…» | `speaking.opinion`, `writing.formal` (article) |
| `instruct` | Give instructions or directions | Δίνω οδηγίες | «Πρώτα κόβετε…, μετά…» «Μην ξεχάσετε να…» «Στρίψτε δεξιά.» | `writing.informal`, `speaking.roleplay` |
| `express_opinion` | Express and ask for an opinion | Εκφράζω γνώμη | «Νομίζω / πιστεύω / θεωρώ ότι…» «Κατά τη γνώμη μου…» «Εσύ τι λες;» | `speaking.opinion`, `writing.formal` (article) |
| `give_reasons` | Give reasons for a view, a choice or a request | Αιτιολογώ | «γιατί / επειδή…» «γι' αυτό…» «αφού…» «Ο λόγος είναι ότι…» | all writing and speaking |
| `express_preference` | Say what one prefers | Εκφράζω προτίμηση | «Προτιμώ… από…» «Μου αρέσει περισσότερο να…» | `speaking.opinion`, `speaking.roleplay` |
| `agree_disagree` | Agree, disagree, concede | Συμφωνώ, διαφωνώ | «Συμφωνώ μαζί σου.» «Έχεις δίκιο, αλλά…» «Δεν συμφωνώ καθόλου.» | `speaking.roleplay` |
| `persuade` | Try to persuade, insist | Πείθω, επιμένω | «Σκέψου ότι…» «Δεν είναι καλύτερα να…;» «Επιμένω ότι…» | `speaking.roleplay`, `writing.informal` |
| `express_feelings` | Express feelings: joy, regret, fear, worry, surprise | Εκφράζω συναισθήματα | «Χαίρομαι που…» «Λυπάμαι που…» «Φοβάμαι ότι / μήπως…» | `speaking.picture`, `writing.informal` |
| `advise` | Ask for and give advice | Ζητώ και δίνω συμβουλές | «Τι να κάνω;» «Σου προτείνω να…» «Αν ήμουν στη θέση σου, θα…» | `writing.informal`, `writing.formal` (article) |
| `suggest` | Suggest, make plans together | Προτείνω | «Τι λες να…;» «Πάμε να…» «Γιατί δεν…;» | `speaking.roleplay`, `writing.informal` |
| `invite` | Invite, accept, decline politely | Προσκαλώ, δέχομαι, αρνούμαι | «Θα ήθελες να έρθεις…;» «Ευχαρίστως!» «Δυστυχώς δεν μπορώ, γιατί…» | `writing.informal` |
| `plan` | Talk about plans, intentions, hopes | Μιλώ για σχέδια | «Σκοπεύω να…» «Σκέφτομαι να…» «Ελπίζω να…» | `speaking.interview`, `writing.informal` |
| `request` | Ask for something or for a service | Ζητώ κάτι | «Θα μπορούσατε να…;» «Θα ήθελα…, παρακαλώ.» | `speaking.roleplay`, `writing.formal` |
| `ask_permission` | Ask for permission or leave, with reasons | Ζητώ άδεια | «Μπορώ να…;» «Θα ήθελα να ζητήσω άδεια για…» | `writing.formal` |
| `complain` | Complain, with reasons, and ask for a remedy | Παραπονιέμαι | «Θα ήθελα να εκφράσω τα παράπονά μου για…» «Σας παρακαλώ να…» | `writing.formal`, `speaking.roleplay` |
| `social_courtesy` | Greet, thank, congratulate, wish, apologise | Χαιρετώ, ευχαριστώ, εύχομαι | «Συγχαρητήρια!» «Σ' ευχαριστώ πολύ για…» «Χρόνια πολλά!» «Ζητώ συγγνώμη για…» | `writing.informal`, all speaking |
| `manage_conversation` | Open and close a talk, take the turn, ask for repetition or clarification | Διαχειρίζομαι τη συζήτηση | «Συγγνώμη, δεν κατάλαβα.» «Μπορείτε να το επαναλάβετε;» «Δηλαδή;» | all speaking |

- **Register:** most functions exist in a friendly form (εσύ) and a formal one (εσείς); the task type and the role decide which (see [task-types.md](task-types.md)). The choice of the polite plural is itself part of B1 ([keg-level], "Κοινωνιοπολιτισμική Ικανότητα").
- **Mostly in** is a guide for the content, not a rule.

## Topics

Each topic gives: its situations, with the ID, the Greek name and examples of content; its vocabulary areas; its functions; and its items in the samples, in the notation of [Topics in the sample papers](#topics-in-the-sample-papers).

### `personal`: Personal identity

Who the user is: personal details, appearance and character, the daily routine, plans. The subjects of the speaking interview (UCY: name, residence, school or work, family, hobbies [ucy-structure]); details of the person, their character and their job (KEG, "Αναγνώριση ταυτότητας").

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `personal.details` | Personal details: name, age, origin, residence, family, languages | Προσωπικά στοιχεία | The interview questions; a form to fill in; introducing oneself in a letter to a new contact. |
| `personal.appearance_character` | What a person looks like and what they are like | Εμφάνιση και χαρακτήρας | Describing a friend or a new colleague in a letter; the people in a picture. |
| `personal.daily_routine` | A normal day, a week, the weekend | Η καθημερινότητά μου | "Πώς περνάτε μια συνηθισμένη μέρα σας;"; a listening text on a day in someone's life. |
| `personal.plans` | Plans, dreams, hopes | Σχέδια, όνειρα, φιλοδοξίες | "Τι σχέδια έχετε για το μέλλον;"; a letter about a decision. (INEK, general aims.) |

- **Vocabulary:** personal details (όνομα, επώνυμο, ηλικία, τόπος γέννησης, υπηκοότητα, διεύθυνση, παντρεμένος, ανύπαντρος); appearance (ψηλός, αδύνατος, ξανθός, γένια, γυαλιά); character (ήρεμος, κοινωνικός, ντροπαλός, υπομονετικός, εργατικός, αστείος); the day (ξυπνάω, ετοιμάζομαι, γυρίζω, ξεκουράζομαι); plans (σχέδιο, όνειρο, στόχος, σκοπεύω, ελπίζω).
- **Functions:** `give_information`, `describe`, `plan`, `ask_information`.
- **In the samples:** A S-A, B S-A.

### `relations`: Family, friends and social life

Family and friends, clubs and groups, invitations and celebrations, and the customs around them (KEG, "Κοινωνικές σχέσεις" and "Κοινωνιοπολιτισμική Ικανότητα").

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `relations.family` | Family life: roles, children, parents and grandparents, sharing tasks | Οικογενειακή ζωή | Tips for parents in a magazine (B R1); a role play on how to spend a family weekend. |
| `relations.friends` | Friendship: keeping in touch, helping, a quarrel | Φιλία | A letter to a friend with news or advice; "Τι μπορούμε να κάνουμε για να έχουμε καλές σχέσεις με τους άλλους;" (B S-B3). |
| `relations.feelings` | Feelings in relationships: love, loneliness, jealousy, trust | Συναισθήματα, σχέσεις | A literary extract about a girl who looks for love (B R3); the feelings of the people in a picture. |
| `relations.invitations` | Invitations and visits: inviting, accepting, declining, what to bring, how long to stay | Προσκλήσεις, επισκέψεις | A letter that invites a friend; a reply that declines politely. (KEG: the conventions of a visit.) |
| `relations.celebrations` | Celebrations and customs: name days, birthdays, weddings, Easter, Christmas | Γιορτές, έθιμα | A letter about a celebration in the user's country; a text on Easter in a Cypriot village. |
| `relations.clubs` | Clubs, associations, volunteering | Σύλλογοι, ομάδες, εθελοντισμός | An announcement of a club; a listening text on a volunteer group. (KEG: membership of clubs and groups.) |

- **Vocabulary:** family (σύζυγος, γονείς, παππούς, γιαγιά, εγγόνια, συγγενείς, πεθερά); relationships (φιλία, εμπιστοσύνη, ζήλια, μοναξιά, μαλώνω, τα βρίσκω με); celebrations (γιορτή, ονομαστική γιορτή, γενέθλια, γάμος, βάφτιση, δώρο, «Χρόνια πολλά»); groups (σύλλογος, μέλος, εθελοντής, συνάντηση).
- **Functions:** `narrate`, `describe`, `express_feelings`, `advise`, `invite`, `social_courtesy`, `agree_disagree`.
- **In the samples:** B R1, B R3, B S-B3.

### `home`: Home and residence

The home and the neighbourhood, renting and moving, repairs, housework, neighbours (KEG, "Κατοικία, διαμονή" and "Δραστηριότητες μέσα στο σπίτι"; INEK 3 has the passive forms of ads: ενοικιάζεται, πωλείται).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `home.description` | The home and the area: type, rooms, furniture, appliances | Το σπίτι μου, η γειτονιά μου | A letter describing a new flat; a comparison of flats for rent (`reading.comparison`). |
| `home.renting` | Renting, buying, moving | Ενοικίαση, μετακόμιση | A small ad for a room (`writing.informal`, `advertisement`); an email to a landlord. (KEG: letters to landlords.) |
| `home.repairs` | Something broken: a repair, a technician, the landlord | Βλάβες, επισκευές | A formal complaint to the landlord about a repair not done; a phone call to a plumber (role play). |
| `home.housework` | Housework and sharing it | Δουλειές του σπιτιού | A role play between flatmates on who does what. |
| `home.neighbours` | Neighbours and the building: noise, pets, shared costs | Γείτονες, πολυκατοικία | An announcement for the neighbours (`writing.informal`, `announcement`). |

- **Vocabulary:** types and parts (διαμέρισμα, μονοκατοικία, πολυκατοικία, όροφος, μπαλκόνι, αυλή); rooms and furniture (σαλόνι, υπνοδωμάτιο, καναπές, ντουλάπα); appliances (πλυντήριο, ψυγείο, κλιματιστικό, θερμοσίφωνας); renting (ενοίκιο, ιδιοκτήτης, ενοικιαστής, συμβόλαιο, εγγύηση, επιπλωμένο, μετακομίζω); repairs (βλάβη, χαλάει, υδραυλικός, ηλεκτρολόγος); the building (κοινόχρηστα, θόρυβος, διαχειριστής); housework (σκουπίζω, σιδερώνω, πλένω τα πιάτα).
- **Functions:** `describe`, `ask_information`, `request`, `complain`, `compare`.
- **In the samples:** none.

### `environment`: Nature, environment and weather

The weather and the climate, landscapes, the protection of the environment, animals (KEG, "Τοποθεσία, περιβάλλον, χλωρίδα, πανίδα, καιρικές συνθήκες"). The most used topic of the samples.

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `environment.weather` | Weather, seasons, climate, the forecast | Καιρός, κλίμα | A weather forecast on the radio (listening; KEG text type); "Νομίζετε ότι το κλίμα επηρεάζει τον χαρακτήρα των ανθρώπων;" (A S-B6). |
| `environment.places` | Landscapes and places: sea and mountains, town and village, the home country and Cyprus | Τόποι, τοπία | A letter comparing the user's home country with Cyprus. (KEG: compare the landscape of the home country.) |
| `environment.protection` | Pollution, litter, recycling, saving water and energy, forest fires | Προστασία του περιβάλλοντος | Two environmental groups on the radio (A L2); "Ανακυκλώνετε υλικά;" (A S-B5); fires in the forests (A S-B4). |
| `environment.animals` | Pets, wild animals and their protection, animals that help people | Ζώα | A programme to protect bears (B L1); guide dogs for the blind (B R4). |

- **Vocabulary:** weather (θερμοκρασία, βροχή, υγρασία, συννεφιά, ζέστη, δελτίο καιρού, εποχή); places (ακτή, νησί, ύπαιθρος, τοπίο, χωριό); the environment (ρύπανση, ανακύκλωση, σκουπίδια, πλαστικό, πυρκαγιά, δάσος, εξοικονόμηση ενέργειας, λειψυδρία); animals (κατοικίδιο, άγρια ζώα, εξαφάνιση, προστασία).
- **Functions:** `express_opinion`, `give_reasons`, `suggest`, `describe`, `compare`.
- **In the samples:** A L2, B L1, B R4, A S-B3, A S-B4, A S-B5, A S-B6, B S-B4, B S-B5.

### `leisure`: Free time, entertainment and culture

Hobbies, going out, culture and the arts, events (KEG, "Ελεύθερος χρόνος, διασκέδαση": interests, cultural events, opening hours, tickets, seats).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `leisure.hobbies` | Hobbies and interests | Χόμπι, ενδιαφέροντα | "Πώς περνάτε τον ελεύθερο χρόνο σας;" (interview); a letter about a new hobby. |
| `leisure.going_out` | Going out: cinema, theatre, concerts, cafés; choosing what to see | Έξοδος, διασκέδαση | Comedy or historical film (A S-rp1); booking seats by phone. |
| `leisure.culture` | Culture and the arts: museums, monuments, books, music | Πολιτισμός, τέχνες | The ancient theatre (A R3); "Νομίζετε ότι η μουσική επηρεάζει τη διάθεση των ανθρώπων;" (B S-B6). |
| `leisure.events` | Events and their programmes: festivals, exhibitions, opening hours, prices | Εκδηλώσεις, προγράμματα | A comparison of five events in a city guide (`reading.comparison`); an announcement of a festival. |

- **Vocabulary:** hobbies (ζωγραφική, φωτογραφία, κηπουρική, ασχολούμαι με); going out (βόλτα, ταινία, κωμωδία, παράσταση, συναυλία, εισιτήριο, θέση, πλατεία, εξώστης); culture (μουσείο, έκθεση, μνημείο, συγγραφέας, τραγούδι); programmes (ώρες λειτουργίας, τιμή εισόδου, είσοδος ελεύθερη, εκδήλωση).
- **Functions:** `express_preference`, `suggest`, `invite`, `persuade`, `ask_information`, `narrate`.
- **In the samples:** A R3, A S-rp1, B S-B6.

### `sport`: Sport and exercise

Doing and watching sport, keeping fit, signing up for lessons, outdoor activities. Part of KEG's free time; a topic of its own because the samples use it often (Estimate, see [How to read this document](#how-to-read-this-document)).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `sport.sports` | Sports and teams, doing and watching | Αθλήματα | A short biography of an athlete (`reading.comparison`); "Ασχολείστε με κάποιο άθλημα;" |
| `sport.fitness` | Keeping fit: the gym, running, exercise for health | Γυμναστική, άσκηση | People at a gym (B S-pic1); park or gym (B S-rp4). |
| `sport.lessons` | Signing up for lessons: times, fees, what to buy, a medical check | Μαθήματα, εγγραφή | Swimming lessons at the municipal pool (B S-rp1); tips on teaching children to ski (A R1). |
| `sport.outdoors` | Outdoor activities: hiking, cycling, the sea, the snow | Δραστηριότητες στη φύση | A letter about a new hiking group (B W1). |

- **Vocabulary:** sports (ποδόσφαιρο, κολύμβηση, τένις, σκι, αγώνας, ομάδα, προπονητής, προπόνηση); fitness (γυμναστήριο, γυμνάζομαι, τρέξιμο, πρόγραμμα γυμναστικής, είμαι σε φόρμα); lessons (εγγραφή, συνδρομή, εξοπλισμός, ιατρικές εξετάσεις); outdoors (πεζοπορία, ορειβασία, ποδήλατο, εκδρομή, κατασκήνωση).
- **Functions:** `ask_information`, `express_preference`, `persuade`, `advise`, `give_reasons`.
- **In the samples:** A R1, B W1, B S-pic1, B S-rp1, B S-rp4.

### `shopping`: Shopping and consumer goods

Shops, choosing and buying a product, returns and complaints, buying online (KEG, "Αγορά": price, discount, payment, ads, terms, instructions on the product).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `shopping.shops` | Kinds of shop: malls, small shops, the market, the supermarket | Καταστήματα | "Πού προτιμάτε να ψωνίζετε; Σε εμπορικά κέντρα ή σε μικρά μαγαζιά;" (A S-B2, B S-B2). |
| `shopping.buying` | Choosing and buying: price, sales, payment, guarantee, the advice of the assistant | Αγορά προϊόντος | A mobile phone (B S-rp5); a new computer or an upgrade (B S-rp6); a family car or a sports car (B S-rp3). |
| `shopping.clothes` | Clothes and sizes | Ρούχα | A role play in a clothes shop; a letter about what to wear at an event. |
| `shopping.returns` | A faulty product: return, exchange, refund | Επιστροφή, παράπονα | A formal complaint to a company (KEG: complaints to a company). |
| `shopping.online` | Buying online and delivery | Ηλεκτρονικές αγορές | An email about an order that did not arrive; a discussion on the pros and cons. |

- **Vocabulary:** shops (εμπορικό κέντρο, λαϊκή αγορά, περίπτερο, ωράριο); buying (τιμή, έκπτωση, προσφορά, οικονομικό, ακριβό, μετρητά, κάρτα, δόσεις, εγγύηση, απόδειξη); clothes (μέγεθος, νούμερο, δοκιμάζω, μου πάει, στενό, φαρδύ); returns (ελαττωματικό, αλλαγή, επιστροφή χρημάτων); online (παραγγελία, παράδοση, μεταφορικά).
- **Functions:** `ask_information`, `compare`, `express_preference`, `persuade`, `request`, `complain`.
- **In the samples:** A S-B2, B S-B2, B S-rp3, B S-rp5, B S-rp6.

### `food`: Food and eating

Eating habits and diet, cooking, eating out (INEK 5, "Διατροφή και υγεία"; KEG, "Διατροφή": restaurants, menus, delivery).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `food.habits` | Eating habits and diet: healthy, Mediterranean, vegetarian; the habits of the home country | Διατροφικές συνήθειες | A magazine article with diet tips (`reading.gap_fill`); "Τι τρώνε στη χώρα σας;" |
| `food.cooking` | Cooking and recipes; the cuisine of the home country | Μαγειρική, συνταγές | A recipe for a friend (`writing.informal`); a listening text on a traditional dish. (INEK.) |
| `food.eating_out` | Restaurants, taverns, cafés: menus, booking, ordering, the bill, delivery | Φαγητό εκτός σπιτιού | New restaurants found online (A R2); a complaint to a restaurant. |

- **Vocabulary:** meals and foods (πρωινό, μεσημεριανό, βραδινό, όσπρια, λαχανικά, θαλασσινά, γλυκά); diet (υγιεινή / μεσογειακή διατροφή, χορτοφάγος, βίγκαν, θερμίδες, δίαιτα); cooking (υλικά, βράζω, ψήνω, τηγανίζω, ψιλοκόβω, κατσαρόλα, τηγάνι, μπαχαρικά); eating out (κατάλογος, παραγγελία, σερβιτόρος, λογαριασμός, κράτηση τραπεζιού, πιάτο ημέρας).
- **Functions:** `describe`, `instruct`, `advise`, `express_preference`, `request`.
- **In the samples:** A R2.

### `health`: Health and the body

The doctor, the hospital and the pharmacy, emergencies, a healthy life (INEK 4; KEG, "Υγεία, κατάσταση του οργανισμού": medical help, emergency services, on-duty pharmacies, insurance).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `health.doctor` | At the doctor: an appointment, the symptoms, the specialists | Στον γιατρό | Booking an appointment by phone (role play); describing a problem to the doctor. (INEK.) |
| `health.hospital_pharmacy` | The hospital and the pharmacy: departments, medicines, prescriptions | Νοσοκομείο, φαρμακείο | The announcement of the on-duty pharmacies; the instructions on a medicine (`reading.true_false`). |
| `health.emergency` | Emergencies and accidents: the ambulance, first aid | Επείγοντα περιστατικά | A listening text in which someone calls for help; a short report on an accident at work. |
| `health.lifestyle` | A healthy life: sleep, stress, water, smoking | Υγιεινός τρόπος ζωής | Water as a source of life (A R4); "Τι άλλο κάνετε για να είστε υγιής;" (B S-pic1). |

- **Vocabulary:** symptoms (πονάει, πονοκέφαλος, πυρετός, βήχας, κρύωμα, γρίπη, αλλεργία, ζαλάδα); doctors (παθολόγος, οδοντίατρος, παιδίατρος, ραντεβού, εξέταση, ασθενής, προσωπικός γιατρός); the hospital and the pharmacy (τμήμα επειγόντων περιστατικών, εφημερεύον φαρμακείο, φάρμακο, συνταγή, χάπι, σιρόπι); emergencies (ασθενοφόρο, ατύχημα, πρώτες βοήθειες, τραυματίζομαι); a healthy life (ύπνος, άγχος, κάπνισμα, ξεκούραση); fears (φοβάμαι μήπως, ανησυχώ; INEK 4).
- **Functions:** `describe`, `ask_information`, `request`, `advise`, `express_feelings`.
- **In the samples:** A R4.

### `travel`: Travel and transport

Holidays and trips, bookings, getting around, driving and road safety, directions (KEG, "Μετακίνηση/ταξίδια"; INEK 4 for road safety).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `travel.holidays` | Holidays and trips: destinations, organised or independent travel | Διακοπές, εκδρομές | Leaflets of travel agencies (B R2); a group trip or a trip of one's own (A S-rp2). |
| `travel.booking` | Bookings: hotel rooms, tickets, timetables, delays, luggage | Κρατήσεις, εισιτήρια | An email to a hotel (KEG writing aims); a complaint about a cancelled flight. |
| `travel.public_transport` | Getting around: bus, taxi, airport, port; announcements | Συγκοινωνίες | Announcements at an airport or a bus station (listening; KEG text type). |
| `travel.driving` | Driving: the car, petrol, the garage, traffic, parking, car hire | Οδήγηση | Traffic news on the radio (listening; KEG text type); a role play at a garage. |
| `travel.road_safety` | Road safety and accidents: seat belt, helmet, speed, the traffic police | Οδική ασφάλεια | A short report on a road accident (KEG writing aims); a leaflet on road safety. (INEK.) |
| `travel.directions` | Asking for and giving directions | Οδηγίες στον δρόμο | A listening text with directions in the street (KEG text type). |

- **Vocabulary:** holidays (προορισμός, τουριστικό γραφείο, οργανωμένη εκδρομή, ξενάγηση, αξιοθέατα); bookings (κράτηση, μονόκλινο, δίκλινο, εισιτήριο μετ' επιστροφής, πτήση, αναχώρηση, άφιξη, καθυστέρηση, ακύρωση, αποσκευές); transport (στάση, δρομολόγιο, λιμάνι, πλοίο); driving (δίπλωμα οδήγησης, πρατήριο καυσίμων, συνεργείο, κίνηση, στάθμευση, ενοικίαση αυτοκινήτου); road safety (ζώνη ασφαλείας, κράνος, όριο ταχύτητας, σήματα τροχαίας, τροχαία, πρόστιμο, διάβαση πεζών); directions (ευθεία, στρίβω, στη γωνία, απέναντι).
- **Functions:** `ask_information`, `request`, `complain`, `compare`, `narrate`, `instruct`, `express_preference`.
- **In the samples:** B R2, A S-rp2.

### `education`: Education and learning

Schools and the school life of children, studies, courses for adults, learning languages, choosing a career (INEK 3; KEG, "Εκπαίδευση/επαγγελματικός χώρος": types of schools, exams, the duties of a student).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `education.school` | School: kinds of school, enrolling a child, the teachers and the parents' association, school events | Σχολείο | A school party at the end of the year (B S-rp2); computers at school (A S-pic2); "Θεωρείτε ότι οι νέοι μαθαίνουν αρκετά πράγματα στα σχολεία;" (A S-B7). |
| `education.university` | Studies: the university, degrees, fees, enrolment papers, exams | Σπουδές | A comparison of study programmes (`reading.comparison`); an email asking what papers the enrolment needs. (INEK.) |
| `education.adult_courses` | Courses for adults: language and training courses, enrolling | Μαθήματα για ενήλικες | Signing up for a Greek course (role play); a formal request for information. (INEK.) |
| `education.languages` | Learning a language: ways of learning, why learn Greek | Εκμάθηση γλωσσών | An online language course or a teacher (B S-rp7). |
| `education.career_choice` | Choosing studies and a profession | Επιλογή σπουδών, επαγγέλματος | An article with advice to parents on choosing a profession after school (B W2). |

- **Vocabulary:** school (δημοτικό, γυμνάσιο, λύκειο, δημόσιο / ιδιωτικό σχολείο, τάξη, δάσκαλος, καθηγητής, Σύνδεσμος Γονέων και Κηδεμόνων, σχολική γιορτή); studies (σχολή, πτυχίο, μεταπτυχιακό, εξάμηνο, δίδακτρα, υποτροφία, εγγραφή, δικαιολογητικά); courses (τμήμα ενηλίκων, σεμινάριο, πιστοποιητικό, εξετάσεις, βαθμός); languages (ξένη γλώσσα, διαδικτυακό μάθημα, ιδιαίτερα μαθήματα); careers (επάγγελμα, ειδικότητα, προσόντα, αγορά εργασίας).
- **Functions:** `ask_information`, `give_information`, `express_opinion`, `advise`, `plan`, `compare`.
- **In the samples:** B W2, A S-B7, A S-pic2, B S-rp2, B S-rp7.

### `work`: Work and employment

Looking for a job, the interview, the conditions of work, the workplace (INEK 2; KEG, "Εκπαίδευση/επαγγελματικός χώρος": job ads, duties, hours, pay, leave, social benefits, safety rules).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `work.job_search` | Looking for a job: ads, the CV, the cover letter, the employment office | Αναζήτηση εργασίας | Advice to a friend who looks for a job (A W1); a call about a job ad. (INEK.) |
| `work.interview` | The job interview: experience, qualifications, the questions of the employer | Συνέντευξη εργασίας | Four candidates at job interviews (B L2). |
| `work.conditions` | Conditions of work: hours, pay, leave, overtime, the contract, benefits | Συνθήκες εργασίας | A reasoned request for leave to the employer (`writing.formal`, UCY part B); a comparison of job ads (`reading.comparison`). |
| `work.workplace` | The workplace: colleagues, duties, rules, a uniform, safety | Χώρος εργασίας | A uniform for all at work (A S-rp3); an announcement for colleagues. |

- **Vocabulary:** job search (αγγελία, θέση εργασίας, βιογραφικό σημείωμα, συνοδευτική επιστολή, αίτηση, άνεργος, επίδομα ανεργίας); interview (εμπειρία, προϋπηρεσία, προσόντα, εργοδότης, υποψήφιος); conditions (ωράριο, μισθός καθαρός / μικτός, υπερωρίες, άδεια, αργία, σύμβαση εργασίας, κρατήσεις, πλήρης / μερική απασχόληση); workplace (συνάδελφος, προϊστάμενος, καθήκοντα, βάρδια, κανονισμός).
- **Functions:** `give_information`, `ask_information`, `ask_permission`, `plan`, `complain`, `agree_disagree`.
- **In the samples:** A W1, B L2, A S-rp3.

### `services`: Public services and everyday business

The bank, the post office, the utilities, the authorities and their forms, the police (KEG, "Δημόσιες υπηρεσίες"; the INEK speaking aims name the foreigners' office, the police, the traffic police, the Citizen Service Centres, the post office and the bank).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `services.bank` | The bank: account, card, loan, payments | Τράπεζα | A complaint to the bank manager about a call on a late car-loan payment (A W2); instructions of a cash machine (KEG text type). |
| `services.post` | The post office and couriers: letters, parcels | Ταχυδρομείο | A role play about a parcel that did not arrive. |
| `services.utilities` | Electricity, water, phone and internet: contracts, bills, faults | Ηλεκτρισμός, νερό, τηλέφωνο | A complaint about a wrong bill; a call to report a fault. |
| `services.authorities` | Public authorities: forms, certificates, the municipality, the Citizen Service Centre, the migration office | Δημόσιες υπηρεσίες, έντυπα | Filling in a form; asking which papers a residence permit needs. |
| `services.police` | The police: a loss, a theft, a report | Αστυνομία | Reporting a lost wallet (role play). |

- **Vocabulary:** bank (λογαριασμός, κάρτα, δάνειο, δόση, τόκος, κατάθεση, ανάληψη, μεταφορά χρημάτων); post (δέμα, φάκελος, συστημένο, αποστολή, παραλαβή); utilities (ρεύμα, νερό, σύνδεση, βλάβη, λογαριασμός); authorities (έντυπο, αίτηση, συμπληρώνω, δικαιολογητικά, πιστοποιητικό, βεβαίωση, ταυτότητα, διαβατήριο, άδεια παραμονής, δήμος, ΚΕΠ, ουρά, ραντεβού); police (κλοπή, χάνω, καταγγελία).
- **Functions:** `ask_information`, `request`, `complain`, `give_information`, `manage_conversation`.
- **In the samples:** A W2.

### `media`: Media and technology

Television and radio, the press and the news, the internet, computers and phones (INEK 6: headlines, news portals, captions, talking about an event; KEG: TV and radio programmes, simple newspaper articles). The second most used topic of the samples.

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `media.tv_radio` | Television and radio: programmes, TV and children | Τηλεόραση, ραδιόφωνο | "Είναι καλό ή κακό να βλέπουν τα παιδιά τηλεόραση;" (A S-B1, B S-B1); watching TV at home (B S-pic2). |
| `media.press_news` | The press and the news: newspapers in print or online, headlines, journalists | Τύπος, ειδήσεις | A journalist at work (A S-pic1); newspapers online or in print (A S-rp4); a simple news article (`reading.gap_fill`). |
| `media.internet` | The internet and social media: use, pros and cons, safety | Διαδίκτυο | "Ποια θεωρείτε ότι είναι τα θετικά και ποια τα αρνητικά στοιχεία του διαδικτύου;" (B S-B7). |
| `media.devices` | Computers and phones in daily life, and their problems | Υπολογιστές, κινητά | A letter to a friend on a new phone that does not work; a magazine survey on phone use (`reading.comparison`). |

- **Vocabulary:** TV and radio (εκπομπή, σειρά, ειδήσεις, ντοκιμαντέρ, κανάλι, παρουσιαστής); press (εφημερίδα, περιοδικό, άρθρο, τίτλος, πρωτοσέλιδο, δημοσιογράφος, έντυπη / ηλεκτρονική έκδοση, επικαιρότητα, λεζάντα); internet (ιστοσελίδα, μέσα κοινωνικής δικτύωσης, ανάρτηση, κωδικός, εξάρτηση); devices (εφαρμογή, κατεβάζω, μνήμη, οθόνη, μπαταρία); verbs of opinion (ξέρω, νομίζω, πιστεύω, θεωρώ, υποστηρίζω, μου φαίνεται; INEK 6).
- **Functions:** `express_opinion`, `give_reasons`, `agree_disagree`, `compare`, `narrate`, `describe`.
- **In the samples:** A S-B1, A S-pic1, A S-rp4, B S-B1, B S-B7, B S-pic2.

### `past_present`: Then and now

Memories, life stories, how life has changed, simple history (INEK 1: describe past experiences, compare the past with the present; UCY reading texts include simple biographies and simple historical extracts [ucy-structure]).

| ID | Situation | Greek | Examples |
|---|---|---|---|
| `past_present.memories` | Childhood and memories | Αναμνήσεις | A letter about a summer of one's childhood; "Όταν ήσασταν παιδί, τι σας άρεσε να κάνετε;" |
| `past_present.life_stories` | Life stories and biographies | Βιογραφίες, ιστορίες ζωής | The story of a grandfather who came to the city (A L1); short biographies (`reading.comparison`). |
| `past_present.changes` | How life has changed: family, work, entertainment, relations | Αλλαγές στη ζωή | "Πώς ήταν η ζωή των παππούδων σας;"; a radio talk on how shopping has changed. (INEK.) |
| `past_present.history` | The past of a place: a town then and now, a monument, an event | Ιστορία ενός τόπου | A simple historical text on a Cypriot town (`reading.true_false`). |

- **Vocabulary:** time (τότε, παλιά, κάποτε, εκείνα τα χρόνια, στην εποχή μου, όταν ήμουν μικρός, σήμερα, πια, πλέον; INEK 1); a life (γεννιέμαι, μεγαλώνω, μετακομίζω, μεταναστεύω, παντρεύομαι, σύνταξη); change (αλλαγή, συνήθειες, παράδοση); history (αιώνας, αρχαίος, γεγονός, μνημείο).
- **Functions:** `narrate`, `compare`, `describe`, `express_opinion`.
- **In the samples:** A L1.

## Topics in the sample papers

Each item of the two KEG B1 sample papers ([keg-a-reading] … [keg-b-speaking]), with its topic. An item that fits two topics is counted under the one that fits better.

**Notation:** the series (A or B), then the part and the item: R1–R4 the reading tasks, L1–L2 the listening tasks, W1 and W2 the writing parts A and B, S-A the speaking interview, S-B1–S-B7 the questions of speaking part B, S-pic1–2 the pictures, S-rp1–7 the role plays.

| Item | Content | Topic |
|---|---|---|
| A R1 | Tips on teaching children to ski | `sport` |
| A R2 | New restaurants in Athens | `food` |
| A R3 | The theatre in ancient Greece | `leisure` |
| A R4 | Water is a source of life | `health` |
| A L1 | The story of my grandfather | `past_present` |
| A L2 | Two environmental groups on the radio | `environment` |
| A W1 | Advice to a friend who looks for a job | `work` |
| A W2 | Complaint to the bank about a car loan | `services` |
| A S-A | Interview: name, residence, school or work, free time, Greece, languages | `personal` |
| A S-B1 | TV and children | `media` |
| A S-B2 | Malls or small shops | `shopping` |
| A S-B3 | Protecting the environment | `environment` |
| A S-B4 | Forest fires | `environment` |
| A S-B5 | Recycling | `environment` |
| A S-B6 | Climate and character | `environment` |
| A S-B7 | What young people learn at school | `education` |
| A S-pic1 | A journalist | `media` |
| A S-pic2 | Computers at school | `education` |
| A S-rp1 | Comedy or historical film | `leisure` |
| A S-rp2 | Group trip or independent trip | `travel` |
| A S-rp3 | A uniform at school or at work | `work` |
| A S-rp4 | Newspapers online or in print | `media` |
| B R1 | Tips for mothers and spouses before the holidays | `relations` |
| B R2 | Travel agencies | `travel` |
| B R3 | Literary extract: a girl looks for love | `relations` |
| B R4 | Guide dogs for the blind | `environment` |
| B L1 | A programme to protect bears | `environment` |
| B L2 | Four job interviews | `work` |
| B W1 | A new hiking group | `sport` |
| B W2 | Article: choosing a profession after school | `education` |
| B S-A | Interview, as in A, plus the favourite food | `personal` |
| B S-B1 | TV and children | `media` |
| B S-B2 | Malls or small shops | `shopping` |
| B S-B3 | Good relations with others | `relations` |
| B S-B4 | Litter in the streets | `environment` |
| B S-B5 | Recycling | `environment` |
| B S-B6 | Music and mood | `leisure` |
| B S-B7 | The internet, pros and cons | `media` |
| B S-pic1 | At the gym | `sport` |
| B S-pic2 | Watching TV | `media` |
| B S-rp1 | Swimming lessons at the municipal pool | `sport` |
| B S-rp2 | Planning a school party | `education` |
| B S-rp3 | Family car or sports car | `shopping` |
| B S-rp4 | Exercise in the park or at the gym | `sport` |
| B S-rp5 | Buying a mobile phone | `shopping` |
| B S-rp6 | A new computer or an upgrade | `shopping` |
| B S-rp7 | Learning a language online | `education` |

- **The two speaking sets share** questions 1, 2 and 5 of part B (TV, shopping, recycling), and the KEG guide repeats the set of series A as its B1 example ([keg-guide-en] p. 29). These questions are counted in each series.
- **What the samples say about UCY:** nothing directly. They are KEG papers of 2014, and the UCY task design is the KEG one (see [exam-spec.md](exam-spec.md#how-to-read-this-document)); UCY may choose other topics. That is why the catalog keeps the topics of the syllabi that the samples do not use.

## Rules for content items

- **One situation per item.** Each content item names exactly one situation ID; its topic is the first part of the ID (`work.interview` is in `work`). The item takes the situation that fits its main subject. The field names are for story 4.
- **Functions:** a writing or speaking item lists the 1–4 functions that its task asks for (for example `complain`, `give_reasons`, `request` for a complaint). The assessment prompt (story 9) gets them with the points to cover, as a hint to the language it should expect. A reading or listening item may list them; it does not have to.
- **The validator** (story 4) checks that each situation and function ID is in the catalog and not retired, and reports the coverage per topic and per situation.
- **The setting:** Cyprus by default, and Greece as well (Estimate: UCY is in Cyprus and the user lives there; the KEG samples are set in Greece). Names of real public bodies may appear as the setting (the municipality, a Citizen Service Centre, the electricity authority), but without facts about them, such as fees, opening hours or rules, that could be wrong or go out of date. Businesses, schools and people are invented.
- **The language:** standard Modern Greek, as in the KEG listening aims ([keg-level]); no Cypriot dialect in the texts (Estimate: UCY says nothing; INEK aims to make its learners familiar with the dialect, but as a course aim, not an exam task).
- **Avoided topics:** none that may offend a candidate, and none that need specialised knowledge or terminology ([keg-guide-en] p. 8). By the same rule (Estimate): no party politics, no religion beyond customs and holidays, no national disputes such as the Cyprus problem, no disasters or crimes with victims as the main subject.
- **Adult life:** the user is an adult who lives and works in Cyprus. Items are written for an adult; school topics are about the school of one's children, one's own school days, or education in general. The samples' questions for pupils are not used.

## Coverage

The targets for the content (story 17) and for the daily plan (story 15). All are Estimate.

- **Every topic in every part:** at least one reading or listening item and one writing or speaking item per topic, so that no topic of the exam is met for the first time on the day.
- **Every situation in at least one item.**
- **Weight by the samples:** the topics with 5 or more sample items (`environment`, `media`, `sport`, `shopping`, `education`) get about twice the share of content of the others. `home`, `health` and `services` get no less than the others, although the samples hardly use them: they are core B1 situations in both syllabi, and the user meets them in daily life.
- **Spread of the task types over the topics:** a topic should not be practised in one task type only; the coverage report shows the topic × task type matrix.

## Gaps

| Gap | Assumption |
|---|---|
| The topics of the UCY exam and of the UCY B1 course | The union of the KEG situations and the INEK themes, weighted by the KEG samples. |
| The full KEG syllabus (sold as a book, see [docs/sources/README.md](sources/README.md)) | The KEG level page, its public summary. |
| How often a topic comes up in real papers | The 47 items of the two samples; a small base, so the weights are coarse. |

## Sources

All in [docs/sources/](sources/README.md), which also gives the origin and the retrieval date of each file.

- [ucy-structure]: UCY, B1 exam structure and task types (2025).
- [keg-level]: KEG, the B1 level page (2020), with the section named.
- [inek]: INEK, B1 syllabus of the Cyprus Greek programme for adults (2020), with the number of the theme.
- [keg-guide-en]: KEG, exam guide in English (2024); page numbers are the printed ones.
- [keg-a-reading], [keg-a-listening], [keg-a-writing], [keg-a-speaking]: KEG B1 sample paper, series A (2014).
- [keg-b-reading], [keg-b-listening], [keg-b-writing], [keg-b-speaking]: KEG B1 sample paper, series B (2014).

[ucy-structure]: sources/ucy-b1-exam-structure.pdf
[keg-level]: sources/keg-b1-level-description.pdf
[inek]: sources/inek-b1-syllabus-2020.pdf
[keg-guide-en]: sources/keg-exam-guide-en-2024.pdf
[keg-a-reading]: sources/keg-b1-sample-a-reading.pdf
[keg-a-listening]: sources/keg-b1-sample-a-listening.pdf
[keg-a-writing]: sources/keg-b1-sample-a-writing.pdf
[keg-a-speaking]: sources/keg-b1-sample-a-speaking.pdf
[keg-b-reading]: sources/keg-b1-sample-b-reading.pdf
[keg-b-listening]: sources/keg-b1-sample-b-listening.pdf
[keg-b-writing]: sources/keg-b1-sample-b-writing.pdf
[keg-b-speaking]: sources/keg-b1-sample-b-speaking.pdf
