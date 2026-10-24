"""The validator of the content: `tools/content.py validate`.

It checks:

- that each catalog file and each item file can be read, and its format
  (formats.py);
- the catalog: unique IDs, the references between its entries, and the
  time of each task type;
- the tables of the docs: equal to what `tools/content.py docs` writes;
- the path of each item file, items/<part>/<type>/<part>.<type>.<NNN>.yaml,
  and that the task type of the file is that of its name;
- the references of each item to the catalog: present, and not retired
  unless the item is retired itself;
- the rules of each task type of docs/task-types.md;
- the audio and the pictures: at most one file per item, a picture for each
  speaking.picture item, no file without its item;
- that no item file is missing that git ever had in the history of HEAD.

An error makes the content unfit for the app: a wrong count or shape, a
missing or wrong reference. A warning asks the author to look, and may stay:
a length or a balance of answers out of range, a key that the script does not
seem to contain.
"""

import os
import re
import subprocess
import unicodedata
from collections.abc import Callable, Iterable
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal, cast

from pydantic import ValidationError
from ruamel.yaml import YAMLError
from ruamel.yaml.comments import CommentedMap, CommentedSeq
from ruamel.yaml.error import MarkedYAMLError

from docs_tables import DocsError, rewrite
from formats import (
    CATALOG_FILES,
    CATALOG_MODELS,
    ITEM_MODELS,
    PARTS,
    Catalog,
    CriteriaFile,
    Entry,
    FunctionsFile,
    GenreRef,
    Item,
    ListeningComparison,
    ListeningGapFill,
    ListeningItem,
    ListeningMultipleChoice,
    ListeningNotes,
    ListeningTrueFalse,
    Model,
    ProductiveItem,
    ReadingComparison,
    ReadingGapFill,
    ReadingMatching,
    ReadingTrueFalse,
    Row,
    SpeakingInterview,
    SpeakingOpinion,
    SpeakingPicture,
    SpeakingRoleplay,
    Statement,
    TaskType,
    TaskTypesFile,
    TopicsFile,
    WritingItem,
    catalog_path,
    item_path,
    items_dir,
    read_yaml,
)

Loc = tuple[str | int, ...]
Severity = Literal["error", "warning"]

PART = "(" + "|".join(PARTS) + ")"
TASK_TYPE_ID = re.compile(rf"^{PART}\.[a-z_]+$")
ITEM_ID = re.compile(rf"^{PART}\.[a-z_]+\.\d{{3}}$")
GAP = re.compile(r"\{(\d+)\}")


# The problems and where they are


@dataclass(frozen=True)
class Problem:
    path: str
    line: int | None
    severity: Severity
    message: str

    def __str__(self) -> str:
        where = self.path if self.line is None else f"{self.path}:{self.line}"
        return f"{where}: {self.severity}: {self.message}"


class Report:
    """The problems found, with the paths of their files relative to the root."""

    def __init__(self, root: Path):
        self.root = root
        self.problems: list[Problem] = []

    def file(self, path: Path, data: Any = None) -> "FileReport":
        return FileReport(self, path.relative_to(self.root).as_posix(), data)


class FileReport:
    """The problems of one file.

    `loc` is the path of a value in the data of the file, such as ("statements", 2, "answer").
    The message is prefixed with it, and the problem gets the line of the value when the data
    comes from read_yaml.
    """

    def __init__(self, report: Report, path: str, data: Any = None):
        self.report = report
        self.path = path
        self.data = data

    def error(self, loc: Loc, message: str, line: int | None = None) -> None:
        self.add("error", loc, message, line)

    def warning(self, loc: Loc, message: str) -> None:
        self.add("warning", loc, message)

    def add(self, severity: Severity, loc: Loc, message: str, line: int | None = None) -> None:
        if line is None:
            line = line_of(self.data, loc)
        if loc:
            message = f"{loc_text(loc)}: {message}"
        self.report.problems.append(Problem(self.path, line, severity, message))

    def validation_errors(self, error: ValidationError) -> None:
        for e in error.errors():
            message = e["msg"].removeprefix("Value error, ")
            self.error(e["loc"], message[0].lower() + message[1:])


def line_of(data: Any, loc: Loc) -> int | None:
    """The line, from 1, of the value at `loc`, or of the deepest part of `loc` that is in the
    data; None for data that does not come from read_yaml."""
    line = None
    node = data
    for key in loc:
        if isinstance(node, CommentedMap) and key in node:
            line = node.lc.key(key)[0] + 1
        elif isinstance(node, CommentedSeq) and isinstance(key, int) and 0 <= key < len(node):
            line = node.lc.item(key)[0] + 1
        else:
            break
        node = node[key]
    return line


def loc_text(loc: Loc) -> str:
    """("statements", 2, "answer") as `statements[2].answer`."""
    text = ""
    for key in loc:
        text += f"[{key}]" if isinstance(key, int) else f".{key}" if text else key
    return text


# Reading the files


class Unreadable(Exception):
    """A file that cannot be read; the reason is reported."""


def load(path: Path, report: Report) -> Any:
    """The data of a YAML file. Raises Unreadable."""
    file = report.file(path)
    try:
        return read_yaml(path)
    except MarkedYAMLError as e:
        mark = e.problem_mark or e.context_mark
        file.error((), f"YAML: {e.problem or e.context}", line=mark.line + 1 if mark else None)
    except YAMLError as e:
        file.error((), f"YAML: {e}")
    except OSError as e:
        file.error((), f"cannot read the file: {e.strerror}")
    raise Unreadable


def plain(data: Any) -> Any:
    """The data of read_yaml as plain dicts, lists, strings and numbers."""
    if isinstance(data, dict):
        return {plain(k): plain(v) for k, v in cast(dict[Any, Any], data).items()}
    if isinstance(data, list):
        return [plain(v) for v in cast(list[Any], data)]
    for t in (bool, str, int, float):
        if isinstance(data, t):
            return t(data)
    return data


def parse[M: Model](model: type[M], data: Any, file: FileReport) -> M | None:
    """The data in the format of the model, or None after reporting why it is not."""
    try:
        return model.model_validate(plain(data))
    except ValidationError as e:
        file.validation_errors(e)
        return None


# Checks used by several rules


def check_count(file: FileReport, loc: Loc, what: str, count: int, low: int, high: int | None = None) -> None:
    """An error unless the count is `low`, or in `low`–`high`."""
    high = low if high is None else high
    if not low <= count <= high:
        expected = str(low) if low == high else f"{low}–{high}"
        file.error(loc, f"{count} {what}, expected {expected}")


def check_unique(file: FileReport, values: Iterable[tuple[Loc, str]]) -> None:
    """An error at each value that occurs earlier in the list."""
    seen: set[str] = set()
    for loc, value in values:
        if value in seen:
            file.error(loc, f"`{value}` occurs twice")
        seen.add(value)


def at(loc: Loc, values: Iterable[str]) -> list[tuple[Loc, str]]:
    """The values of the list at `loc`, each with its own loc."""
    return [((*loc, i), value) for i, value in enumerate(values)]


def ids_at(loc: Loc, entries: Iterable[Entry]) -> list[tuple[Loc, str]]:
    return [((*loc, i, "id"), entry.id) for i, entry in enumerate(entries)]


def retired_by_id(entries: Iterable[Entry]) -> dict[str, bool]:
    return {entry.id: entry.retired for entry in entries}


def check_ref(
    file: FileReport, loc: Loc, kind: str, ref: str, entries: dict[str, bool], referrer_retired: bool
) -> None:
    """A reference to a catalog entry: it must exist, and must not be retired unless the
    referrer is retired too. `entries` tells for each ID whether it is retired."""
    if ref not in entries:
        file.error(loc, f"no {kind} `{ref}` in the catalog")
    elif entries[ref] and not referrer_retired:
        file.error(loc, f"the {kind} `{ref}` is retired")


def word_count(text: str) -> int:
    """The words of a text, separated by spaces; a dash alone is no word."""
    return sum(any(c.isalnum() for c in word) for word in text.split())


def check_length(file: FileReport, loc: Loc, what: str, text: str, low: int, high: int) -> None:
    count = word_count(text)
    if not low <= count <= high:
        file.warning(loc, f"{count} words in {what}, expected {low}–{high}")


# The catalog


@dataclass
class Index:
    """What the items refer to in the catalog. A dict of IDs tells whether each one is retired;
    a situation is retired with its topic."""

    task_types: dict[str, TaskType]
    situations: dict[str, bool]
    functions: dict[str, bool]


def check_catalog(root: Path, report: Report) -> tuple[dict[str, Any], Catalog, Index] | None:
    """The catalog files as read_yaml reads them, by their name, the catalog in its format, and
    its index; None if a file cannot be read or is not in the format."""
    raw: dict[str, Any] = {}
    files: dict[str, FileReport] = {}
    parsed: dict[str, Model | None] = {}
    for name in CATALOG_FILES:
        path = catalog_path(root, name)
        try:
            raw[name] = load(path, report)
        except Unreadable:
            continue
        files[name] = report.file(path, raw[name])
        parsed[name] = parse(CATALOG_MODELS[name], raw[name], files[name])
    if any(parsed.get(name) is None for name in CATALOG_FILES):
        return None
    catalog = Catalog(
        task_types=cast(TaskTypesFile, parsed["task_types"]),
        topics=cast(TopicsFile, parsed["topics"]),
        functions=cast(FunctionsFile, parsed["functions"]),
        criteria=cast(CriteriaFile, parsed["criteria"]),
    )

    check_task_types(catalog.task_types, files["task_types"])
    check_topics(catalog.topics, files["topics"])
    check_functions(catalog.functions, catalog.task_types, files["functions"])
    check_unique(files["criteria"], ids_at(("writing",), catalog.criteria.writing))
    check_unique(files["criteria"], ids_at(("speaking",), catalog.criteria.speaking))

    index = Index(
        task_types={t.id: t for t in catalog.task_types.task_types},
        situations={s.id: s.retired or topic.retired for topic in catalog.topics.topics for s in topic.situations},
        functions=retired_by_id(catalog.functions.functions),
    )
    return raw, catalog, index


def check_task_types(catalog: TaskTypesFile, file: FileReport) -> None:
    check_unique(file, ids_at(("answer_formats",), catalog.answer_formats))
    check_unique(file, ids_at(("task_types",), catalog.task_types))
    formats = retired_by_id(catalog.answer_formats)
    for i, task_type in enumerate(catalog.task_types):
        loc = ("task_types", i)
        if not TASK_TYPE_ID.match(task_type.id) or not task_type.id.startswith(task_type.part + "."):
            file.error((*loc, "id"), "a task type ID is <part>.<type>, with the part of `part`")
        if task_type.minutes * 2 != int(task_type.minutes * 2):
            file.error((*loc, "minutes"), "the time of a task is in whole or half minutes")
        check_ref(file, (*loc, "answer"), "answer format", task_type.answer, formats, task_type.retired)
        check_unique(file, ids_at((*loc, "genres"), task_type.genres or []))
        check_unique(file, ids_at((*loc, "kinds"), task_type.kinds or []))


def check_topics(catalog: TopicsFile, file: FileReport) -> None:
    check_unique(file, ids_at(("topics",), catalog.topics))
    situations = []
    for i, topic in enumerate(catalog.topics):
        for j, situation in enumerate(topic.situations):
            loc = ("topics", i, "situations", j, "id")
            if not re.fullmatch(rf"{re.escape(topic.id)}\.[a-z_]+", situation.id):
                file.error(loc, f"a situation ID is <topic>.<situation>, here `{topic.id}.<situation>`")
            situations.append((loc, situation.id))
    check_unique(file, situations)
    check_unique(file, [(("samples", i, "item"), s.item) for i, s in enumerate(catalog.samples)])
    topics = retired_by_id(catalog.topics)
    for i, sample in enumerate(catalog.samples):
        check_ref(file, ("samples", i, "topic"), "topic", sample.topic, topics, referrer_retired=True)


def check_functions(catalog: FunctionsFile, task_types: TaskTypesFile, file: FileReport) -> None:
    check_unique(file, ids_at(("functions",), catalog.functions))
    types = {t.id: t for t in task_types.task_types}
    type_ids = retired_by_id(task_types.task_types)
    for i, function in enumerate(catalog.functions):
        for j, ref in enumerate(function.mostly_in):
            loc = ("functions", i, "mostly_in", j)
            if isinstance(ref, GenreRef):
                check_ref(file, (*loc, "task_type"), "task type", ref.task_type, type_ids, function.retired)
                if ref.task_type in types:
                    genres = retired_by_id(types[ref.task_type].genres or [])
                    kind = f"`{ref.task_type}` genre"
                    check_ref(file, (*loc, "genre"), kind, ref.genre, genres, function.retired)
            elif ref not in PARTS:
                check_ref(file, loc, "task type or part", ref, type_ids, function.retired)


# The docs


def check_docs(root: Path, catalog: dict[str, Any], report: Report) -> None:
    """An error for each doc whose tables differ from what `tools/content.py docs` writes."""
    for path in sorted((root / "docs").glob("*.md")):
        file = report.file(path)
        text = path.read_text(encoding="utf-8")
        try:
            new_text = rewrite(text, catalog)
        except DocsError as e:
            file.error((), str(e))
            continue
        if new_text != text:
            old_lines, new_lines = text.split("\n"), new_text.split("\n")
            line = next((n for n, (a, b) in enumerate(zip(old_lines, new_lines), 1) if a != b), None)
            file.error((), "the tables differ from the catalog; run tools/content.py docs", line=line)


# The items


def check_items(root: Path, index: Index, report: Report) -> dict[str, Item | None]:
    """The items by their ID: the model of each, or None if the item is not in the format."""
    items: dict[str, Item | None] = {}
    directory = items_dir(root)
    if not directory.exists():
        return items
    for path in sorted(directory.rglob("*")):
        if path.is_dir() or path.name.startswith("."):
            continue
        item_id = path.stem
        if not ITEM_ID.match(item_id) or path != item_path(root, item_id):
            report.file(path).error((), "an item file is content/items/<part>/<type>/<part>.<type>.<NNN>.yaml")
            continue
        try:
            data = load(path, report)
        except Unreadable:
            items[item_id] = None
            continue
        items[item_id] = check_item(item_id, data, index, report.file(path, data))
    return items


def check_item(item_id: str, data: Any, index: Index, file: FileReport) -> Item | None:
    """Checks an item, and returns its model, or None if it is not in the format."""
    type_id = item_id.rsplit(".", 1)[0]
    task_type = index.task_types.get(type_id)
    if task_type is None:
        file.error((), f"no task type `{type_id}` in the catalog")
        return None
    if type_id not in ITEM_MODELS:
        file.error((), f"the validator has no format for the task type `{type_id}`")
        return None
    item = parse(ITEM_MODELS[type_id], data, file)
    if item is None:
        return None
    if item.task_type != type_id:
        file.error(("task_type",), f"`{item.task_type}`, but the file name is of the task type `{type_id}`")
    check_references(item, task_type, index, file)
    TASK_TYPE_CHECKS[type_id](item, file)
    return item


def check_references(item: Item, task_type: TaskType, index: Index, file: FileReport) -> None:
    if task_type.retired and not item.retired:
        file.error(("task_type",), f"the task type `{task_type.id}` is retired")
    check_ref(file, ("situation",), "situation", item.situation, index.situations, item.retired)
    for i, function in enumerate(item.functions):
        check_ref(file, ("functions", i), "function", function, index.functions, item.retired)
    check_unique(file, at(("functions",), item.functions))
    if isinstance(item, ProductiveItem) or item.functions:
        check_count(file, ("functions",), "functions", len(item.functions), 1, 4)
    if isinstance(item, WritingItem):
        genres = retired_by_id(task_type.genres or [])
        check_ref(file, ("genre",), f"`{task_type.id}` genre", item.genre, genres, item.retired)
    if isinstance(item, SpeakingRoleplay):
        kinds = retired_by_id(task_type.kinds or [])
        check_ref(file, ("kind",), f"`{task_type.id}` kind", item.kind, kinds, item.retired)


# The rules of each task type, from docs/task-types.md


def check_script(item: ListeningItem, file: FileReport, low: int, high: int) -> list[str]:
    """Checks the speakers, the script and its length; returns the words of the script, as
    `normalized_words` gives them."""
    names = [speaker.name for speaker in item.speakers]
    check_unique(file, [(("speakers", i, "name"), name) for i, name in enumerate(names)])
    for i, turn in enumerate(item.script):
        if turn.speaker not in names:
            file.error(("script", i, "speaker"), f"no speaker `{turn.speaker}` in `speakers`")
    for i, name in enumerate(names):
        if all(turn.speaker != name for turn in item.script):
            file.error(("speakers", i), f"`{name}` has no turn in the script")
    text = "\n".join(turn.text for turn in item.script)
    check_length(file, ("script",), "the script", text, low, high)
    return normalized_words(text)


def normalized_words(text: str) -> list[str]:
    """The words of a text in small letters, without accents and with σ for ς, to compare forms."""
    text = unicodedata.normalize("NFD", text.lower()).replace("ς", "σ")
    return re.findall(r"\w+", "".join(c for c in text if not unicodedata.combining(c)))


def key_words(answer: str) -> list[str]:
    """The words of a key answer that the script must contain: without the optional words in
    brackets, and without the words of up to 3 letters (articles, pronouns, prepositions),
    whose forms differ too much; numbers are kept."""
    words = normalized_words(re.sub(r"\(.*?\)", " ", answer))
    return [word for word in words if len(word) > 3 or word.isdigit()]


def same_word(a: str, b: str) -> bool:
    """Whether two normalized words are one word, perhaps with another ending: they share at least
    3 letters at the start and differ at most in the last 3 letters of the longer one."""
    if a == b:
        return True
    if a.isdigit() or b.isdigit():
        return False
    common = len(os.path.commonprefix([a, b]))
    return common >= 3 and common >= max(len(a), len(b)) - 3


def heard(answer: str, script: list[str]) -> bool:
    """Whether the script contains each key word of the answer, in some form."""
    return all(any(same_word(word, s) for s in script) for word in key_words(answer))


def position(answer: str, script: list[str], start: int) -> int | None:
    """The first position from `start` of the longest key word of the answer in the script."""
    words = key_words(answer)
    if not words:
        return start
    longest = max(words, key=len)
    return next((i for i in range(start, len(script)) if same_word(longest, script[i])), None)


def check_heard(file: FileReport, keys: list[tuple[Loc, list[list[str]]]], script: list[str], in_order: bool) -> None:
    """Warns of each key answer that the script does not seem to contain and, if `in_order`, of
    each one heard only before the key answer of the question before it.

    `keys` has the loc of each question and its groups of alternatives: one group, or one per
    line of an `any_order` entry, which may come in either order.
    """
    start = 0
    for loc, groups in keys:
        positions: list[int] = []
        for alternatives in groups:
            key = " / ".join(alternatives)
            if not any(heard(answer, script) for answer in alternatives):
                file.warning(loc, f"`{key}` is not in the script, in this or a close form")
                continue
            found = [p for answer in alternatives if (p := position(answer, script, start)) is not None]
            if in_order and not found:
                file.warning(loc, f"`{key}` is heard only before the answer of the question before it")
            positions += found
        start = max(positions, default=start)


def check_key_length(file: FileReport, loc: Loc, alternatives: list[str], max_words: int) -> None:
    for answer in alternatives:
        count = word_count(answer.replace("(", "").replace(")", ""))
        if count > max_words:
            file.error(loc, f"`{answer}` has {count} words; a key answer has at most {max_words}")


def check_grid(
    file: FileReport,
    rows: list[Row],
    columns: list[str],
    row_count: tuple[int, int],
    ticks: int,
    per_row: tuple[int, int | None],
) -> None:
    """The rows of a grid: the example tick in the first row only, the ticks in the columns, at
    most one per cell, `per_row` ticks in each row with the example, and `ticks` in total
    without it."""
    check_count(file, ("rows",), "rows", len(rows), *row_count)
    low, high = per_row
    for i, row in enumerate(rows):
        loc = ("rows", i)
        if i == 0 and row.example is None:
            file.error(loc, "the first row has the example tick, in `example`")
        if i > 0 and row.example is not None:
            file.error((*loc, "example"), "only the first row has the example tick")
        cells = at((*loc, "ticks"), row.ticks)
        if row.example is not None:
            cells.insert(0, ((*loc, "example"), row.example))
        for cell_loc, column in cells:
            if column not in columns:
                file.error(cell_loc, f"no column `{column}`")
        check_unique(file, cells)
        if len(cells) < low or high is not None and len(cells) > high:
            expected = f"at least {low}" if high is None else f"{low}–{high}"
            file.error(loc, f"{len(cells)} ticks with the example, expected {expected}")
    check_count(file, ("rows",), "ticks without the example", sum(len(row.ticks) for row in rows), ticks)


def check_statements(file: FileReport, statements: list[Statement], low: int, high: int) -> None:
    check_count(file, ("statements",), "statements", len(statements), low, high)
    true = sum(statement.answer for statement in statements)
    false = len(statements) - true
    if true < 2 or false < 2:
        file.warning(("statements",), f"{true} true and {false} false, expected at least 2 of each")


def check_gaps(file: FileReport, text: str, count: int) -> bool:
    """An error unless the gaps in the text are {0} to {count}, each once and in order; returns
    whether they are."""
    numbers = [int(n) for n in GAP.findall(text)]
    if numbers != list(range(count + 1)):
        found = ", ".join(f"{{{n}}}" for n in numbers) or "none"
        file.error(("text",), f"the gaps are {found}; expected {{0}} to {{{count}}}, each once and in order")
    rest = GAP.sub("", text)
    if "{" in rest or "}" in rest:
        file.error(("text",), "a brace that is not part of a gap {N}")
    return numbers == list(range(count + 1))


def sentence_count(text: str) -> int:
    return len(re.findall(r"[.;!…]+(?=\s|$)", text.strip()))


def check_listening_notes(item: ListeningNotes, file: FileReport) -> None:
    script = check_script(item, file, 250, 350)
    lines = sum(len(line.groups()) for line in item.lines)
    check_count(file, ("lines",), "lines (an `any_order` entry counts one per group)", lines, 12)
    keys: list[tuple[Loc, list[list[str]]]] = [(("example",), [item.example.answers])]
    keys += [(("lines", i), line.groups()) for i, line in enumerate(item.lines)]
    for loc, groups in keys:
        for alternatives in groups:
            check_key_length(file, loc, alternatives, item.max_words)
    check_heard(file, keys, script, in_order=True)


def check_listening_comparison(item: ListeningComparison, file: FileReport) -> None:
    script = check_script(item, file, 350, 400)
    check_count(file, ("columns",), "columns", len(item.columns), 2, 4)
    check_unique(file, at(("columns",), item.columns))
    for i, column in enumerate(item.columns):
        if not heard(column, script):
            file.warning(("columns", i), f"`{column}` is not named in the script")
    check_grid(file, item.rows, item.columns, row_count=(6, 11), ticks=13, per_row=(1, None))


def check_listening_true_false(item: ListeningTrueFalse, file: FileReport) -> None:
    check_script(item, file, 250, 350)
    check_statements(file, item.statements, 6, 12)


def check_listening_gap_fill(item: ListeningGapFill, file: FileReport) -> None:
    script = check_script(item, file, 250, 350)
    check_count(file, ("gaps",), "gaps", len(item.gaps), 6, 12)
    check_gaps(file, item.text, len(item.gaps))
    keys: list[tuple[Loc, list[list[str]]]] = [(("example",), [item.example.answers])]
    keys += [(("gaps", i), [gap.answers]) for i, gap in enumerate(item.gaps)]
    for loc, groups in keys:
        check_key_length(file, loc, groups[0], 3)
    check_heard(file, keys, script, in_order=False)


def check_listening_multiple_choice(item: ListeningMultipleChoice, file: FileReport) -> None:
    check_script(item, file, 250, 350)
    check_count(file, ("questions",), "questions", len(item.questions), 6, 12)
    questions = [(("example",), item.example)] + [(("questions", i), q) for i, q in enumerate(item.questions)]
    for loc, question in questions:
        check_count(file, (*loc, "options"), "options", len(question.options), 3)
        check_unique(file, at((*loc, "options"), question.options))
    answers = {question.answer for question in item.questions}
    if len(answers) == 1:
        file.warning(("questions",), f"the right option is always {answers.pop()}")


def check_reading_matching(item: ReadingMatching, file: FileReport) -> None:
    check_count(file, ("sentences",), "sentences", len(item.sentences), 6)
    check_count(file, ("distractors",), "distractors", len(item.distractors), 3)
    seconds = [(("example", "second"), item.example.second)]
    seconds += [(("sentences", i, "second"), s.second) for i, s in enumerate(item.sentences)]
    check_unique(file, seconds + at(("distractors",), item.distractors))
    sentences = [item.example] + item.sentences
    text = " ".join([item.intro or ""] + [f"{s.first} {s.second}" for s in sentences])
    check_length(file, (), "the text with the right second parts", text, 150, 180)


def check_reading_comparison(item: ReadingComparison, file: FileReport) -> None:
    check_count(file, ("texts",), "texts", len(item.texts), 5)
    names = [text.name for text in item.texts]
    check_unique(file, [(("texts", i, "name"), name) for i, name in enumerate(names)])
    check_length(file, ("texts",), "the texts", "\n".join(text.text for text in item.texts), 150, 180)
    check_grid(file, item.rows, names, row_count=(7, 7), ticks=12, per_row=(1, 3))


def check_reading_true_false(item: ReadingTrueFalse, file: FileReport) -> None:
    check_length(file, ("text",), "the text", item.text, 180, 200)
    check_statements(file, item.statements, 6, 6)


def check_reading_gap_fill(item: ReadingGapFill, file: FileReport) -> None:
    check_count(file, ("gaps",), "gaps", len(item.gaps), 7)
    check_count(file, ("distractors",), "distractors", len(item.distractors), 6)
    bank = [(("example", "answer"), item.example.answer)]
    bank += [(("gaps", i, "answer"), gap.answer) for i, gap in enumerate(item.gaps)]
    check_unique(file, bank + at(("distractors",), item.distractors))
    if check_gaps(file, item.text, len(item.gaps)):
        answers = [item.example.answer] + [gap.answer for gap in item.gaps]
        text = GAP.sub(lambda m: answers[int(m[1])], item.text)
        check_length(file, ("text",), "the text with the right words in the gaps", text, 180, 200)


def check_writing(item: WritingItem, file: FileReport) -> None:
    check_count(file, ("points_to_cover",), "points", len(item.points_to_cover), 2, 4)
    if item.signature not in item.instruction:
        file.warning(("instruction",), f"does not name the signature `{item.signature}`")


def check_speaking_interview(item: SpeakingInterview, file: FileReport) -> None:
    check_count(file, ("questions",), "questions", len(item.questions), 6, 8)


def check_speaking_opinion(item: SpeakingOpinion, file: FileReport) -> None:
    check_count(file, ("follow_ups",), "follow-up questions", len(item.follow_ups), 1, 3)


def check_speaking_picture(item: SpeakingPicture, file: FileReport) -> None:
    check_count(file, ("personal_questions",), "personal questions", len(item.personal_questions), 2, 3)


def check_speaking_roleplay(item: SpeakingRoleplay, file: FileReport) -> None:
    for name, role in (("role_a", item.role_a), ("role_b", item.role_b)):
        check_count(file, (name, "arguments"), "arguments", len(role.arguments), 2, 4)
        sentences = sentence_count(role.card)
        if not 3 <= sentences <= 7:
            file.warning((name, "card"), f"{sentences} sentences, expected 3–7")


TASK_TYPE_CHECKS: dict[str, Callable[[Any, FileReport], None]] = {
    "listening.notes": check_listening_notes,
    "listening.comparison": check_listening_comparison,
    "listening.true_false": check_listening_true_false,
    "listening.gap_fill": check_listening_gap_fill,
    "listening.multiple_choice": check_listening_multiple_choice,
    "reading.matching": check_reading_matching,
    "reading.comparison": check_reading_comparison,
    "reading.true_false": check_reading_true_false,
    "reading.gap_fill": check_reading_gap_fill,
    "writing.informal": check_writing,
    "writing.formal": check_writing,
    "speaking.interview": check_speaking_interview,
    "speaking.opinion": check_speaking_opinion,
    "speaking.picture": check_speaking_picture,
    "speaking.roleplay": check_speaking_roleplay,
}


# Audio and pictures


def check_media(root: Path, items: dict[str, Item | None], report: Report) -> None:
    def is_listening(item_id: str) -> bool:
        return item_id.startswith("listening.")

    def is_picture(item_id: str) -> bool:
        return item_id.startswith("speaking.picture.")

    content = root / "content"
    check_media_files(content / "audio", "listening item", is_listening, items, report)
    pictures = check_media_files(content / "pictures", "speaking.picture item", is_picture, items, report)
    for item_id in items:
        if is_picture(item_id) and item_id not in pictures:
            report.file(item_path(root, item_id)).error((), f"no picture content/pictures/{item_id}.<ext>")


def check_media_files(
    directory: Path, kind: str, belongs: Callable[[str], bool], items: dict[str, Item | None], report: Report
) -> set[str]:
    """The IDs of the items that have a file `<id>.<ext>` in the directory. An error for a file of
    no such item, and for a second file of an item."""
    found: set[str] = set()
    if not directory.exists():
        return found
    for path in sorted(directory.iterdir()):
        if path.name.startswith("."):
            continue
        file = report.file(path)
        item_id = path.stem
        if path.is_dir():
            file.error((), "a directory; the files are <id>.<ext>, directly in " + directory.name)
        elif item_id not in items or not belongs(item_id):
            file.error((), f"no {kind} `{item_id}`")
        elif item_id in found:
            file.error((), f"a second file of the item `{item_id}`")
        else:
            found.add(item_id)
    return found


# The history of the items


def git(root: Path, *args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(["git", *args], cwd=root, capture_output=True, text=True)


def check_history(root: Path, report: Report) -> None:
    """An error for each file missing now that git added under content/items/ in the history of
    HEAD, on any branch that HEAD contains: an item is never deleted or renamed."""
    file = report.file(items_dir(root))
    try:
        if git(root, "rev-parse", "--git-dir").returncode != 0:
            file.error((), "not in a git repository; cannot check that no item file is missing")
            return
        if git(root, "rev-parse", "--verify", "--quiet", "HEAD").returncode != 0:
            return  # no commit yet
        added = ["--no-renames", "--diff-filter=A", "--name-only", "-z", "--format="]
        log = git(root, "log", *added, "HEAD", "--", "content/items")
    except OSError as e:
        file.error((), f"cannot run git: {e.strerror}")
        return
    if log.returncode != 0:
        file.error((), f"git log failed: {log.stderr.strip()}")
        return
    for name in sorted({name.strip("\n") for name in log.stdout.split("\0")} - {""}):
        if not (root / name).exists():
            message = "missing: git had this item file; an item is never deleted or renamed, it is retired"
            report.file(root / name).error((), message)


# All


@dataclass
class Validation:
    catalog: Catalog | None
    """None if a catalog file cannot be read or is not in the format."""
    items: dict[str, Item | None]
    problems: list[Problem]

    def count(self, severity: Severity) -> int:
        return sum(problem.severity == severity for problem in self.problems)


def validate(root: Path) -> Validation:
    """Validates the content in root/content/ and the docs in root/docs/. The items are checked
    only when the catalog is in its format."""
    report = Report(root)
    catalog: Catalog | None = None
    items: dict[str, Item | None] = {}
    checked = check_catalog(root, report)
    if checked is not None:
        raw, catalog, index = checked
        check_docs(root, raw, report)
        items = check_items(root, index, report)
        check_media(root, items, report)
    check_history(root, report)
    return Validation(catalog, items, sorted(report.problems, key=lambda p: (p.path, p.line or 0)))
