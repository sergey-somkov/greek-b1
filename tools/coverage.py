"""The coverage report of the content: `tools/content.py coverage`.

It counts the items that the app offers, those in the format and not retired:
per topic and part, per situation, and per topic and task type, the matrix of
docs/topics.md#coverage. The rows and the columns are the entries of the
catalog that are not retired, in its order, so a topic, a situation or a task
type without items shows as a gap. An item that refers to an entry missing
from the catalog or retired is not counted; the validator reports it.
"""

from collections import Counter
from dataclasses import dataclass

from formats import PARTS, Catalog, Item

ZERO = "."
"""An empty cell of the matrix, so that the gaps stand out."""


@dataclass
class Coverage:
    topics: list[str]
    situations: list[str]
    task_types: list[str]
    """The entries of the catalog that are not retired, in its order."""
    by_cell: Counter[tuple[str, str]]
    """The items per topic and task type."""
    by_situation: Counter[str]
    retired: int
    """The retired items, not counted."""
    not_in_format: int
    """The items not in the format, not counted."""

    def by_topic_and_part(self, topic: str, part: str) -> int:
        return sum(n for (t, task_type), n in self.by_cell.items() if t == topic and part_of(task_type) == part)


def part_of(task_type: str) -> str:
    return task_type.split(".")[0]


def count(catalog: Catalog, items: dict[str, Item | None]) -> Coverage:
    """The coverage of the items, by their ID, as validator.validate returns them."""
    topic_of = {
        situation.id: topic.id
        for topic in catalog.topics.topics
        if not topic.retired
        for situation in topic.situations
        if not situation.retired
    }
    task_types = [t.id for t in catalog.task_types.task_types if not t.retired]
    coverage = Coverage(
        topics=list(dict.fromkeys(topic_of.values())),
        situations=list(topic_of),
        task_types=task_types,
        by_cell=Counter(),
        by_situation=Counter(),
        retired=0,
        not_in_format=0,
    )
    for item in items.values():
        if item is None:
            coverage.not_in_format += 1
        elif item.retired:
            coverage.retired += 1
        elif item.situation in topic_of and item.task_type in task_types:
            coverage.by_cell[topic_of[item.situation], item.task_type] += 1
            coverage.by_situation[item.situation] += 1
    return coverage


def report(coverage: Coverage) -> list[str]:
    """The lines of the report."""
    return [
        "Items per topic and part",
        "",
        *topics_table(coverage),
        "",
        "Items per situation",
        "",
        *aligned([[s, str(coverage.by_situation[s])] for s in coverage.situations]),
        "",
        "Items per topic and task type",
        "",
        *matrix(coverage),
        "",
        f"{coverage.by_cell.total()} items counted; {coverage.retired} retired"
        f" and {coverage.not_in_format} not in the format are not counted.",
    ]


def topics_table(coverage: Coverage) -> list[str]:
    def row(name: str, counts: list[int]) -> list[str]:
        return [name, str(sum(counts)), *map(str, counts)]

    rows = [["topic", "items", *PARTS]]
    for topic in coverage.topics:
        rows.append(row(topic, [coverage.by_topic_and_part(topic, part) for part in PARTS]))
    rows.append(row("all", [sum(coverage.by_topic_and_part(t, part) for t in coverage.topics) for part in PARTS]))
    return aligned(rows)


def matrix(coverage: Coverage) -> list[str]:
    """The topic × task type matrix; a column is named by the initial of its part and the number of
    the task type in the part, explained below the matrix."""
    labels: dict[str, str] = {}
    legend: list[str] = []
    for part in PARTS:
        task_types = [t for t in coverage.task_types if part_of(t) == part]
        if not task_types:
            continue
        initial = part[0].upper()
        numbers = f"{initial}1" if len(task_types) == 1 else f"{initial}1–{initial}{len(task_types)}"
        legend.append(f"{numbers} {part}: " + ", ".join(t.split(".")[1] for t in task_types))
        for number, task_type in enumerate(task_types, 1):
            labels[task_type] = f"{initial}{number}"

    def cell(n: int) -> str:
        return str(n) if n else ZERO

    rows = [["topic", *labels.values()]]
    for topic in coverage.topics:
        rows.append([topic, *(cell(coverage.by_cell[topic, t]) for t in labels)])
    rows.append(["all", *(cell(sum(coverage.by_cell[topic, t] for topic in coverage.topics)) for t in labels)])
    return [*aligned(rows), "", *legend]


def aligned(rows: list[list[str]]) -> list[str]:
    """The rows as lines, the first column aligned left and the others right."""
    widths = [max(len(row[i]) for row in rows) for i in range(len(rows[0]))]
    return [
        "  ".join([row[0].ljust(widths[0]), *(value.rjust(width) for value, width in zip(row[1:], widths[1:]))])
        for row in rows
    ]
