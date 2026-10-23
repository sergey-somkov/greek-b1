"""Tests of `tools/content.py validate`: one test per rule, on small fixtures.

A fixture is a valid catalog or item; each test changes one thing in it and expects the
problems that the change makes.
"""

import copy
import os
import re
import shutil
import subprocess
from collections.abc import Callable
from dataclasses import replace
from pathlib import Path
from typing import Any

import pytest
from ruamel.yaml import YAML

from docs_tables import rewrite
from formats import ITEM_MODELS, item_path, load_catalog
from validator import (
    TASK_TYPE_CHECKS,
    FileReport,
    Index,
    Report,
    Unreadable,
    check_catalog,
    check_item,
    load,
    plain,
    validate,
)

REPO_ROOT = Path(__file__).resolve().parents[2]

Change = Callable[[Any], None]


# Changes of a fixture


def node_at(data: Any, path: tuple[Any, ...]) -> Any:
    for key in path:
        data = data[key]
    return data


def setting(*path_and_value: Any) -> Change:
    *path, value = path_and_value
    return lambda data: node_at(data, tuple(path[:-1])).__setitem__(path[-1], copy.deepcopy(value))


def removing(*path: Any) -> Change:
    return lambda data: node_at(data, path[:-1]).__delitem__(path[-1])


def appending(*path_and_value: Any) -> Change:
    *path, value = path_and_value
    return lambda data: node_at(data, tuple(path)).append(copy.deepcopy(value))


def changes(*fns: Change) -> Change:
    def change(data: Any) -> None:
        for fn in fns:
            fn(data)

    return change


# The fixture catalog

ANSWER_FORMATS = {
    "listening.notes": "short_text",
    "listening.comparison": "grid",
    "listening.true_false": "choice",
    "listening.gap_fill": "short_text",
    "listening.multiple_choice": "choice",
    "reading.matching": "assign",
    "reading.comparison": "grid",
    "reading.true_false": "choice",
    "reading.gap_fill": "assign",
    "writing.informal": "text",
    "writing.formal": "text",
    "speaking.interview": "speech",
    "speaking.opinion": "speech",
    "speaking.picture": "speech",
    "speaking.roleplay": "dialogue",
}


def entry(entry_id: str, retired: bool = False, **fields: Any) -> dict[str, Any]:
    return {"id": entry_id, "retired": retired, **fields}


def task_type(type_id: str) -> dict[str, Any]:
    fields = entry(
        type_id,
        part=type_id.split(".")[0],
        greek="Τύπος",
        exam_slot="A slot",
        items="1",
        points="1",
        answer=ANSWER_FORMATS[type_id],
        scored_by="App",
    )
    if type_id.startswith("writing."):
        fields["genres"] = [
            entry("letter", greek="επιστολή", example="A letter."),
            entry("old_genre", True, greek="παλιό", example="An old genre."),
        ]
    if type_id == "speaking.roleplay":
        fields["kinds"] = [entry("disagreement", meaning="Argue."), entry("old_kind", True, meaning="Old.")]
    return fields


def named(entry_id: str, retired: bool = False, **fields: Any) -> dict[str, Any]:
    return entry(entry_id, retired, name="A name", greek="Όνομα", **fields)


def catalog() -> dict[str, Any]:
    situations = [named("home.neighbors", examples="Neighbors."), named("home.old", True, examples="Old.")]
    function_refs = ["speaking.picture", {"task_type": "writing.formal", "genre": "letter"}, "writing"]
    return {
        "task_types": {
            "answer_formats": [
                entry(f, user_does="Does.", recorded_answer="The answer.")
                for f in dict.fromkeys(ANSWER_FORMATS.values())
            ],
            "task_types": [task_type(t) for t in ANSWER_FORMATS],
        },
        "topics": {
            "topics": [
                named("home", basis="KEG", situations=situations),
                named("past", True, basis="KEG", situations=[named("past.memories", examples="Memories.")]),
            ],
            "samples": [{"item": "A R1", "content": "Neighbors.", "topic": "home"}],
        },
        "functions": {
            "functions": [
                named("describe", exponents=["Βλέπω…"], mostly_in=function_refs),
                *[
                    named(f, exponents=["Λέω…"], mostly_in=["writing"])
                    for f in ("complain", "request", "advise", "suggest")
                ],
                named("old_function", True, exponents=["Λέω…"], mostly_in=["writing"]),
            ]
        },
        "criteria": {
            "writing": [named("task", covers="All.", bands={n: f"Band {n}." for n in range(1, 6)})],
            "speaking": [named("grammar", covers="All.", points={"1": "Weak.", "2-3": "Fair.", "4-5": "Good."})],
        },
    }


def write_yaml(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as f:
        YAML().dump(data, f)


def write_catalog(root: Path, data: dict[str, Any]) -> None:
    for name, content in data.items():
        write_yaml(root / "content" / "catalog" / f"{name}.yaml", content)


@pytest.fixture(scope="module")
def index(tmp_path_factory: pytest.TempPathFactory) -> Index:
    root = tmp_path_factory.mktemp("catalog")
    write_catalog(root, catalog())
    report = Report(root)
    result = check_catalog(root, report)
    assert report.problems == [] and result is not None
    return result[2]


# The fixture items

# Words that are not forms of each other, as the key answers of the listening items.
NOUNS = ["ψωμί", "κουλούρια", "γυναίκα", "θάλασσα", "βιβλίο", "τραπέζι", "παράθυρο"]
NOUNS += ["λεμόνι", "ντομάτα", "ζάχαρη", "φεγγάρι", "χειμώνας", "ουρανός", "ποτάμι"]


def filler(count: int) -> str:
    """`count` words that no key answer contains."""
    return " ".join(["λα"] * count)


def common(type_id: str, **body: Any) -> dict[str, Any]:
    fields: dict[str, Any] = {
        "task_type": type_id,
        "situation": "home.neighbors",
        "retired": False,
        "title": "Τίτλος",
        "framing": "Διαβάζετε ένα κείμενο.",
        "instruction": "Απαντήστε στις ερωτήσεις.",
    }
    if type_id.split(".")[0] in ("writing", "speaking"):
        fields["functions"] = ["describe"]
    return fields | body


def listening(type_id: str, words: list[str], total: int, **body: Any) -> dict[str, Any]:
    """A listening item with a script of `total` words, which has the words in order."""
    gap = (total - len(words)) // (len(words) + 1)
    text = " ".join([filler(gap)] + [f"{word} {filler(gap)}" for word in words])
    return common(
        type_id,
        speakers=[{"name": "Κώστας", "gender": "male", "age": "senior"}],
        script=[{"speaker": "Κώστας", "text": text}],
        **body,
    )


def statement(answer: bool) -> dict[str, Any]:
    return {"statement": "Ο Κώστας μένει εδώ.", "answer": answer, "explanation": "Because."}


def statements() -> list[dict[str, Any]]:
    return [statement(i % 2 == 0) for i in range(6)]


def question(answer: str) -> dict[str, Any]:
    options = ["Τρώει.", "Πίνει.", "Κοιμάται."]
    return {"question": "Τι κάνει ο Κώστας;", "options": options, "answer": answer, "explanation": "Because."}


def heard(*answers: str) -> dict[str, Any]:
    return {"answers": list(answers), "explanation": "Heard."}


def gaps_text(count: int) -> str:
    return " ".join(f"{{{i}}}" for i in range(count + 1))


def row(statement_text: str, ticks: list[str], example: str | None = None) -> dict[str, Any]:
    fields: dict[str, Any] = {"statement": statement_text}
    if example is not None:
        fields["example"] = example
    return fields | {"ticks": ticks, "explanation": "Says so."}


def sentence(i: int) -> dict[str, Any]:
    return {"first": f"Η πρόταση {i} λέει ότι", "second": f"{NOUNS[i]}.", "explanation": "Fits."}


def role() -> dict[str, Any]:
    card = "Θέλετε να πάτε σινεμά. Ο φίλος σας θέλει θέατρο. Επιμένετε."
    return {"card": card, "arguments": ["Είναι φτηνό.", "Είναι κοντά."], "gives_way": "Agrees at the end."}


COLUMNS = ["Άννα", "Νίκος"]
SHOPS = ["Άλφα", "Βήτα", "Γάμμα", "Δέλτα", "Έψιλον"]

ITEMS: dict[str, Callable[[], dict[str, Any]]] = {
    "listening.notes": lambda: listening(
        "listening.notes",
        NOUNS[:13],
        300,
        max_words=3,
        example={"label": "Τι πουλά", **heard(NOUNS[0])},
        lines=[{"label": "Τι πουλά", **heard(noun)} for noun in NOUNS[1:11]]
        + [{"label": "Τι αγαπά", "any_order": [[NOUNS[11]], [NOUNS[12]]], "explanation": "Heard."}],
    ),
    "listening.comparison": lambda: listening(
        "listening.comparison",
        COLUMNS,
        375,
        columns=list(COLUMNS),
        rows=[row("Δουλεύει.", ["Νίκος"], example="Άννα")] + [row("Μένει εδώ.", list(COLUMNS)) for _ in range(6)],
    ),
    "listening.true_false": lambda: listening(
        "listening.true_false", [], 300, example=statement(True), statements=statements()
    ),
    "listening.gap_fill": lambda: listening(
        "listening.gap_fill",
        NOUNS[:7],
        300,
        text=gaps_text(6),
        example=heard(NOUNS[0]),
        gaps=[heard(noun) for noun in NOUNS[1:7]],
    ),
    "listening.multiple_choice": lambda: listening(
        "listening.multiple_choice",
        [],
        300,
        example=question("β"),
        questions=[question("αβγ"[i % 3]) for i in range(6)],
    ),
    "reading.matching": lambda: common(
        "reading.matching",
        intro=filler(130),
        example=sentence(0),
        sentences=[sentence(i) for i in range(1, 7)],
        distractors=[f"{noun}." for noun in NOUNS[7:10]],
    ),
    "reading.comparison": lambda: common(
        "reading.comparison",
        texts=[{"name": name, "text": filler(33)} for name in SHOPS],
        rows=[row("Ανοιχτό.", ["Βήτα"], example="Άλφα")]
        + [row("Φτηνό.", ["Γάμμα", "Δέλτα"]) for _ in range(5)]
        + [row("Κοντά.", ["Έψιλον"])],
    ),
    "reading.true_false": lambda: common(
        "reading.true_false", text=filler(190), example=statement(True), statements=statements()
    ),
    "reading.gap_fill": lambda: common(
        "reading.gap_fill",
        text=f"{gaps_text(7)} {filler(182)}",
        example={"answer": NOUNS[0], "explanation": "Fits."},
        gaps=[{"answer": noun, "explanation": "Fits."} for noun in NOUNS[1:8]],
        distractors=NOUNS[8:14],
    ),
    **{
        type_id: lambda type_id=type_id: common(
            type_id,
            instruction="Υπογράψτε ως Ελένη.",
            genre="letter",
            points_to_cover=["Περιγράψτε το πρόβλημα.", "Ζητήστε βοήθεια."],
            opening="Αγαπητέ Νίκο,",
            signature="Ελένη",
        )
        for type_id in ("writing.informal", "writing.formal")
    },
    "speaking.interview": lambda: common(
        "speaking.interview", questions=[{"question": "Πώς σας λένε;"} for _ in range(6)]
    ),
    "speaking.opinion": lambda: common("speaking.opinion", question="Σας αρέσει η τηλεόραση;", follow_ups=["Γιατί;"]),
    "speaking.picture": lambda: common(
        "speaking.picture",
        description="A family at a table.",
        picture_questions=["Τι βλέπετε;"],
        personal_questions=["Πού μένετε;", "Τι σας αρέσει;"],
    ),
    "speaking.roleplay": lambda: common(
        "speaking.roleplay", kind="disagreement", register="informal", role_a=role(), role_b=role()
    ),
}


def problems(index: Index, item_id: str, data: Any) -> list[str]:
    report = Report(Path("."))
    check_item(item_id, data, index, FileReport(report, "item.yaml", data))
    return [f"{p.severity}: {p.message}" for p in report.problems]


# The format and the references of an item


def test_each_task_type_has_a_format_and_checks():
    task_types = {t["id"] for t in load_catalog(REPO_ROOT)["task_types"]["task_types"]}
    assert task_types == set(ITEM_MODELS) == set(TASK_TYPE_CHECKS) == set(ANSWER_FORMATS) == set(ITEMS)


README_EXAMPLES = re.findall(r"```yaml\n(.*?)```", (REPO_ROOT / "content" / "README.md").read_text(), re.S)


@pytest.mark.parametrize("example", README_EXAMPLES)
def test_the_examples_of_the_readme_are_in_the_format(example: str):
    data = YAML().load(example)
    ITEM_MODELS[data["task_type"]].model_validate(plain(data))


@pytest.mark.parametrize("type_id", ITEMS)
def test_a_valid_item_has_no_problems(index: Index, type_id: str):
    assert problems(index, f"{type_id}.001", ITEMS[type_id]()) == []


RULES = [
    # The format
    ("reading.true_false", removing("title"), "error: title: field required"),
    ("reading.true_false", setting("source", "A magazine"), "error: source: extra inputs are not permitted"),
    ("reading.true_false", setting("title", 1935), "error: title: input should be a valid string"),
    ("reading.true_false", setting("title", ""), "error: title: string should have at least 1 character"),
    (
        "reading.true_false",
        setting("statements", 0, "answer", "no"),
        "error: statements[0].answer: input should be a valid boolean",
    ),
    (
        "listening.notes",
        setting("lines", 0, "any_order", [["α"], ["β"]]),
        "error: lines[0]: a line has either `answers` or `any_order`",
    ),
    (
        "listening.true_false",
        setting("speakers", 0, "age", "old"),
        "error: speakers[0].age: input should be 'child', 'young', 'adult' or 'senior'",
    ),
    ("speaking.opinion", removing("functions"), "error: functions: field required"),
    (
        "listening.true_false",
        setting("task_type", "reading.true_false"),
        "error: task_type: `reading.true_false`, but the file name is of the task type `listening.true_false`",
    ),
    # The references to the catalog
    (
        "reading.true_false",
        setting("situation", "home.garden"),
        "error: situation: no situation `home.garden` in the catalog",
    ),
    ("reading.true_false", setting("situation", "home.old"), "error: situation: the situation `home.old` is retired"),
    (
        "reading.true_false",
        setting("situation", "past.memories"),
        "error: situation: the situation `past.memories` is retired",
    ),
    (
        "speaking.opinion",
        setting("functions", ["describe", "shout"]),
        "error: functions[1]: no function `shout` in the catalog",
    ),
    (
        "speaking.opinion",
        setting("functions", ["old_function"]),
        "error: functions[0]: the function `old_function` is retired",
    ),
    (
        "speaking.opinion",
        setting("functions", ["describe", "describe"]),
        "error: functions[1]: `describe` occurs twice",
    ),
    ("speaking.opinion", setting("functions", []), "error: functions: 0 functions, expected 1–4"),
    (
        "reading.true_false",
        setting("functions", ["describe", "complain", "request", "advise", "suggest"]),
        "error: functions: 5 functions, expected 1–4",
    ),
    ("writing.formal", setting("genre", "poem"), "error: genre: no `writing.formal` genre `poem` in the catalog"),
    (
        "writing.formal",
        setting("genre", "old_genre"),
        "error: genre: the `writing.formal` genre `old_genre` is retired",
    ),
    ("speaking.roleplay", setting("kind", "chat"), "error: kind: no `speaking.roleplay` kind `chat` in the catalog"),
    # The script of a listening item
    (
        "listening.true_false",
        appending("script", {"speaker": "Μαρία", "text": "Γεια."}),
        "error: script[1].speaker: no speaker `Μαρία` in `speakers`",
    ),
    (
        "listening.true_false",
        appending("speakers", {"name": "Κώστας", "gender": "male", "age": "adult"}),
        "error: speakers[1].name: `Κώστας` occurs twice",
    ),
    (
        "listening.true_false",
        appending("speakers", {"name": "Μαρία", "gender": "female", "age": "young"}),
        "error: speakers[1]: `Μαρία` has no turn in the script",
    ),
    (
        "listening.true_false",
        setting("script", 0, "text", filler(200)),
        "warning: script: 200 words in the script, expected 250–350",
    ),
    # listening.notes
    (
        "listening.notes",
        removing("lines", 0),
        "error: lines: 11 lines (an `any_order` entry counts one per group), expected 12",
    ),
    (
        "listening.notes",
        setting("lines", 0, "answers", ["κουλούρια (λα λα) λα"]),
        "error: lines[0]: `κουλούρια (λα λα) λα` has 4 words; a key answer has at most 3",
    ),
    (
        "listening.notes",
        setting("lines", 0, "answers", ["σοκολάτα"]),
        "warning: lines[0]: `σοκολάτα` is not in the script, in this or a close form",
    ),
    (
        "listening.notes",
        changes(setting("lines", 0, "answers", [NOUNS[2]]), setting("lines", 1, "answers", [NOUNS[1]])),
        "warning: lines[1]: `κουλούρια` is heard only before the answer of the question before it",
    ),
    # Grids
    (
        "listening.comparison",
        setting("columns", COLUMNS + ["Ζωή", "Ρία", "Λία"]),
        "error: columns: 5 columns, expected 2–4",
    ),
    ("listening.comparison", setting("columns", COLUMNS + ["Άννα"]), "error: columns[2]: `Άννα` occurs twice"),
    ("listening.comparison", appending("columns", "Μαρία"), "warning: columns[2]: `Μαρία` is not named in the script"),
    ("listening.comparison", removing("rows", 1, "ticks", 0), "error: rows: 12 ticks without the example, expected 13"),
    (
        "listening.comparison",
        removing("rows", 0, "example"),
        "error: rows[0]: the first row has the example tick, in `example`",
    ),
    (
        "reading.comparison",
        setting("rows", 6, "example", "Άλφα"),
        "error: rows[6].example: only the first row has the example tick",
    ),
    (
        "listening.comparison",
        setting("rows", 1, "ticks", ["Άννα", "Μαρία"]),
        "error: rows[1].ticks[1]: no column `Μαρία`",
    ),
    (
        "listening.comparison",
        setting("rows", 1, "ticks", ["Άννα", "Άννα"]),
        "error: rows[1].ticks[1]: `Άννα` occurs twice",
    ),
    (
        "reading.comparison",
        changes(
            setting("rows", 0, "ticks", ["Βήτα", "Γάμμα", "Δέλτα"]),
            setting("rows", 1, "ticks", ["Γάμμα"]),
            setting("rows", 2, "ticks", ["Γάμμα"]),
        ),
        "error: rows[0]: 4 ticks with the example, expected 1–3",
    ),
    # True or false
    (
        "listening.true_false",
        setting("statements", [statement(i % 2 == 0) for i in range(13)]),
        "error: statements: 13 statements, expected 6–12",
    ),
    ("reading.true_false", removing("statements", 0), "error: statements: 5 statements, expected 6"),
    (
        "reading.true_false",
        setting("statements", [statement(True) for _ in range(6)]),
        "warning: statements: 6 true and 0 false, expected at least 2 of each",
    ),
    ("reading.true_false", setting("text", filler(150)), "warning: text: 150 words in the text, expected 180–200"),
    # Gaps
    (
        "listening.gap_fill",
        changes(removing("gaps", 5), setting("text", gaps_text(5))),
        "error: gaps: 5 gaps, expected 6–12",
    ),
    (
        "listening.gap_fill",
        setting("text", "{0} {1} {2} {4} {3} {5} {6}"),
        "error: text: the gaps are {0}, {1}, {2}, {4}, {3}, {5}, {6}; expected {0} to {6}, each once and in order",
    ),
    (
        "listening.gap_fill",
        setting("text", gaps_text(6) + " {x}"),
        "error: text: a brace that is not part of a gap {N}",
    ),
    (
        "listening.gap_fill",
        setting("gaps", 0, "answers", ["κουλούρια λα λα λα"]),
        "error: gaps[0]: `κουλούρια λα λα λα` has 4 words; a key answer has at most 3",
    ),
    # Multiple choice
    ("listening.multiple_choice", removing("questions", 0), "error: questions: 5 questions, expected 6–12"),
    (
        "listening.multiple_choice",
        setting("questions", 0, "options", ["Τρώει.", "Πίνει."]),
        "error: questions[0].options: 2 options, expected 3",
    ),
    (
        "listening.multiple_choice",
        setting("questions", 0, "options", ["Τρώει.", "Τρώει.", "Πίνει."]),
        "error: questions[0].options[1]: `Τρώει.` occurs twice",
    ),
    (
        "listening.multiple_choice",
        setting("questions", [question("α") for _ in range(6)]),
        "warning: questions: the right option is always α",
    ),
    # Banks
    ("reading.matching", removing("sentences", 0), "error: sentences: 5 sentences, expected 6"),
    ("reading.matching", appending("distractors", "τυρί."), "error: distractors: 4 distractors, expected 3"),
    ("reading.matching", setting("distractors", 0, "ψωμί."), "error: distractors[0]: `ψωμί.` occurs twice"),
    (
        "reading.matching",
        setting("intro", filler(10)),
        "warning: 52 words in the text with the right second parts, expected 150–180",
    ),
    (
        "reading.gap_fill",
        changes(removing("gaps", 6), setting("text", f"{gaps_text(6)} {filler(182)}")),
        "error: gaps: 6 gaps, expected 7",
    ),
    ("reading.gap_fill", removing("distractors", 0), "error: distractors: 5 distractors, expected 6"),
    ("reading.gap_fill", setting("distractors", 0, NOUNS[0]), "error: distractors[0]: `ψωμί` occurs twice"),
    (
        "reading.gap_fill",
        setting("text", f"{gaps_text(7)} {filler(100)}"),
        "warning: text: 108 words in the text with the right words in the gaps, expected 180–200",
    ),
    # reading.comparison
    ("reading.comparison", appending("texts", {"name": "Ζήτα", "text": "λα"}), "error: texts: 6 texts, expected 5"),
    (
        "reading.comparison",
        changes(setting("texts", 4, "name", "Άλφα"), setting("rows", 6, "ticks", ["Άλφα"])),
        "error: texts[4].name: `Άλφα` occurs twice",
    ),
    (
        "reading.comparison",
        setting("texts", 0, "text", "λα"),
        "warning: texts: 133 words in the texts, expected 150–180",
    ),
    # Writing
    ("writing.formal", setting("points_to_cover", ["Περιγράψτε."]), "error: points_to_cover: 1 points, expected 2–4"),
    ("writing.informal", setting("signature", "Μαρία"), "warning: instruction: does not name the signature `Μαρία`"),
    # Speaking
    ("speaking.interview", removing("questions", 0), "error: questions: 5 questions, expected 6–8"),
    ("speaking.opinion", setting("follow_ups", []), "error: follow_ups: 0 follow-up questions, expected 1–3"),
    (
        "speaking.picture",
        setting("personal_questions", ["Πού μένετε;", "Τι σας αρέσει;", "Γιατί;", "Πότε;"]),
        "error: personal_questions: 4 personal questions, expected 2–3",
    ),
    (
        "speaking.roleplay",
        setting("role_b", "arguments", ["Ένα."]),
        "error: role_b.arguments: 1 arguments, expected 2–4",
    ),
    (
        "speaking.roleplay",
        setting("role_a", "card", "Θέλετε σινεμά."),
        "warning: role_a.card: 1 sentences, expected 3–7",
    ),
    ("speaking.roleplay", setting("register", "polite"), "error: register: input should be 'informal' or 'formal'"),
]


@pytest.mark.parametrize("type_id, change, problem", RULES, ids=[p.split(": ", 1)[1] for _, _, p in RULES])
def test_a_rule(index: Index, type_id: str, change: Change, problem: str):
    data = ITEMS[type_id]()
    change(data)
    assert problems(index, f"{type_id}.001", data) == [problem]


def test_a_task_type_not_in_the_catalog_is_an_error(index: Index):
    data = ITEMS["reading.true_false"]()
    assert problems(index, "reading.essay.001", data) == ["error: no task type `reading.essay` in the catalog"]


def test_an_item_of_a_retired_task_type_is_an_error(index: Index):
    retired = index.task_types["speaking.opinion"].model_copy(update={"retired": True})
    index = replace(index, task_types=index.task_types | {"speaking.opinion": retired})
    data = ITEMS["speaking.opinion"]()
    assert problems(index, "speaking.opinion.001", data) == [
        "error: task_type: the task type `speaking.opinion` is retired"
    ]


def test_a_retired_item_may_refer_to_retired_entries(index: Index):
    data = ITEMS["writing.formal"]()
    changes(
        setting("retired", True),
        setting("situation", "home.old"),
        setting("functions", ["old_function"]),
        setting("genre", "old_genre"),
    )(data)
    assert problems(index, "writing.formal.001", data) == []


def test_a_key_in_a_close_form_is_heard(index: Index):
    data = ITEMS["listening.notes"]()
    setting("lines", 0, "answers", ["κουλούρι", "σοκολάτα"])(data)
    assert problems(index, "listening.notes.001", data) == []


def test_a_problem_has_the_line_of_its_value(tmp_path: Path, index: Index):
    path = tmp_path / "item.yaml"
    path.write_text(
        "task_type: reading.true_false\n"
        "situation: home.neighbors\n"
        "retired: false\n"
        "title: Τίτλος\n"
        "framing: Διαβάζετε.\n"
        "instruction: Απαντήστε.\n"
        "text: >-\n"
        "  Κείμενο.\n"
        "example:\n"
        "  statement: Σωστό.\n"
        "  answer: true\n"
        "  explanation: Because.\n"
        "statements:\n"
        "  - statement: Λάθος.\n"
        "    answer: no\n"
        "    explanation: Because.\n"
    )
    report = Report(tmp_path)
    data = load(path, report)
    check_item("reading.true_false.001", data, index, report.file(path, data))
    assert [str(p) for p in report.problems] == [
        "item.yaml:15: error: statements[0].answer: input should be a valid boolean"
    ]


def test_a_yaml_error_has_its_line(tmp_path: Path):
    path = tmp_path / "item.yaml"
    path.write_text("title: Ένα\ntitle: Δύο\n")
    report = Report(tmp_path)
    with pytest.raises(Unreadable):
        load(path, report)
    assert [str(p) for p in report.problems] == [
        'item.yaml:2: error: YAML: found duplicate key "title" with value "Δύο" (original value: "Ένα")'
    ]


# The catalog

CATALOG_RULES = [
    (removing("topics", "topics", 0, "greek"), "error: topics[0].greek: field required"),
    (
        setting("criteria", "writing", 0, "bands", {1: "One."}),
        "error: writing[0].bands: the bands are 1, 2, 3, 4 and 5, in this order",
    ),
    (
        appending("task_types", "task_types", task_type("listening.notes")),
        "error: task_types[15].id: `listening.notes` occurs twice",
    ),
    (
        setting("task_types", "task_types", 0, "part", "reading"),
        "error: task_types[0].id: a task type ID is <part>.<type>, with the part of `part`",
    ),
    (
        setting("task_types", "task_types", 0, "answer", "draw"),
        "error: task_types[0].answer: no answer format `draw` in the catalog",
    ),
    (
        setting("topics", "topics", 0, "situations", 0, "id", "house.neighbors"),
        "error: topics[0].situations[0].id: a situation ID is <topic>.<situation>, here `home.<situation>`",
    ),
    (setting("topics", "samples", 0, "topic", "garden"), "error: samples[0].topic: no topic `garden` in the catalog"),
    (
        setting("functions", "functions", 0, "mostly_in", 0, "drawing"),
        "error: functions[0].mostly_in[0]: no task type or part `drawing` in the catalog",
    ),
    (
        setting("functions", "functions", 0, "mostly_in", 1, "genre", "poem"),
        "error: functions[0].mostly_in[1].genre: no `writing.formal` genre `poem` in the catalog",
    ),
]


@pytest.mark.parametrize("change, problem", CATALOG_RULES, ids=[p.split(": ", 1)[1] for _, p in CATALOG_RULES])
def test_a_rule_of_the_catalog(tmp_path: Path, change: Change, problem: str):
    data = catalog()
    change(data)
    write_catalog(tmp_path, data)
    report = Report(tmp_path)
    check_catalog(tmp_path, report)
    assert [f"{p.severity}: {p.message}" for p in report.problems] == [problem]


# The whole repository


def git(root: Path, *args: str) -> None:
    """Runs git in the test repository, without the user's settings, such as the signing of commits."""
    env = os.environ | {
        "GIT_CONFIG_GLOBAL": os.devnull,
        "GIT_CONFIG_NOSYSTEM": "1",
        "GIT_AUTHOR_NAME": "Test",
        "GIT_AUTHOR_EMAIL": "test@example.com",
        "GIT_COMMITTER_NAME": "Test",
        "GIT_COMMITTER_EMAIL": "test@example.com",
    }
    subprocess.run(["git", *args], cwd=root, env=env, check=True, capture_output=True)


@pytest.fixture
def repo(tmp_path: Path) -> Path:
    """A git repository with no commit, a catalog, a doc with a table, and an item of each type,
    with the picture of the picture item."""
    git(tmp_path, "init")
    write_catalog(tmp_path, catalog())
    doc = "# Formats\n\n<!-- catalog: answer_formats -->\n<!-- /catalog -->\n"
    (tmp_path / "docs").mkdir()
    (tmp_path / "docs" / "formats.md").write_text(rewrite(doc, load_catalog(tmp_path)))
    for type_id, make in ITEMS.items():
        write_yaml(item_path(tmp_path, f"{type_id}.001"), make())
    (tmp_path / "content" / "pictures").mkdir()
    (tmp_path / "content" / "pictures" / "speaking.picture.001.jpg").write_bytes(b"")
    return tmp_path


def repo_problems(root: Path) -> list[str]:
    return [str(p) for p in validate(root).problems]


def test_a_valid_repository_has_no_problems(repo: Path):
    result = validate(repo)
    assert result.problems == []
    assert len(result.items) == len(ITEMS)


def test_the_tables_of_the_docs_equal_the_catalog(repo: Path):
    doc = repo / "docs" / "formats.md"
    doc.write_text(doc.read_text().replace("Does.", "Did."))
    assert repo_problems(repo) == [
        "docs/formats.md:7: error: the tables differ from the catalog; run tools/content.py docs"
    ]


@pytest.mark.parametrize(
    "name",
    [
        "reading/true_false/item.yaml",
        "reading/true_false/reading.true_false.1.yaml",
        "reading/gap_fill/reading.true_false.002.yaml",
        "reading/true_false/reading.true_false.002.yml",
    ],
)
def test_an_item_file_has_the_path_of_its_id(repo: Path, name: str):
    write_yaml(repo / "content" / "items" / name, ITEMS["reading.true_false"]())
    assert repo_problems(repo) == [
        f"content/items/{name}: error: an item file is content/items/<part>/<type>/<part>.<type>.<NNN>.yaml"
    ]


def test_a_picture_item_has_a_picture(repo: Path):
    (repo / "content" / "pictures" / "speaking.picture.001.jpg").unlink()
    assert repo_problems(repo) == [
        "content/items/speaking/picture/speaking.picture.001.yaml: error: "
        "no picture content/pictures/speaking.picture.001.<ext>"
    ]


@pytest.mark.parametrize(
    "path, problem",
    [
        ("audio/listening.notes.002.mp3", "no listening item `listening.notes.002`"),
        ("audio/reading.true_false.001.mp3", "no listening item `reading.true_false.001`"),
        ("pictures/speaking.opinion.001.jpg", "no speaking.picture item `speaking.opinion.001`"),
        ("pictures/speaking.picture.001.png", "a second file of the item `speaking.picture.001`"),
    ],
)
def test_a_media_file_belongs_to_an_item(repo: Path, path: str, problem: str):
    (repo / "content" / path).parent.mkdir(exist_ok=True)
    (repo / "content" / path).write_bytes(b"")
    assert repo_problems(repo) == [f"content/{path}: error: {problem}"]


def test_an_item_file_that_git_had_is_never_missing(repo: Path):
    git(repo, "add", ".")
    git(repo, "commit", "-m", "Items")
    item_path(repo, "reading.true_false.001").rename(item_path(repo, "reading.true_false.002"))
    assert repo_problems(repo) == [
        "content/items/reading/true_false/reading.true_false.001.yaml: error: "
        "missing: git had this item file; an item is never deleted or renamed, it is retired"
    ]


def test_the_content_is_in_a_git_repository(repo: Path):
    shutil.rmtree(repo / ".git")
    assert repo_problems(repo) == [
        "content/items: error: not in a git repository; cannot check that no item file is missing"
    ]
