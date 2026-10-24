"""Tests of `tools/content.py coverage`, on a small catalog and items that have only the fields
that the report reads."""

from typing import Any

from coverage import count, report
from formats import Catalog, CriteriaFile, FunctionsFile, Item, TaskTypesFile, TopicsFile


def entry(entry_id: str, retired: bool = False, **fields: Any) -> dict[str, Any]:
    return {"id": entry_id, "retired": retired, "greek": "Όνομα", **fields}


def task_type(type_id: str, retired: bool = False) -> dict[str, Any]:
    return entry(
        type_id,
        retired,
        part=type_id.split(".")[0],
        exam_slot="A slot",
        items="1",
        points="1",
        minutes=12.5,
        answer="choice",
        scored_by="App",
    )


def situation(situation_id: str, retired: bool = False) -> dict[str, Any]:
    return entry(situation_id, retired, name="A name", examples="Some.")


def topic(topic_id: str, situations: list[dict[str, Any]], retired: bool = False) -> dict[str, Any]:
    return entry(topic_id, retired, name="A name", basis="KEG", situations=situations)


def catalog() -> Catalog:
    return Catalog(
        task_types=TaskTypesFile.model_validate(
            {
                "answer_formats": [],
                "task_types": [
                    task_type("listening.notes"),
                    task_type("reading.matching"),
                    task_type("reading.true_false"),
                    task_type("reading.old", retired=True),
                    task_type("writing.formal"),
                ],
            }
        ),
        topics=TopicsFile.model_validate(
            {
                "topics": [
                    topic("home", [situation("home.renting"), situation("home.neighbors"), situation("home.old", True)]),
                    topic("past", [situation("past.memories")], retired=True),
                    topic("work", [situation("work.interview")]),
                ],
                "samples": [],
            }
        ),
        functions=FunctionsFile(functions=[]),
        criteria=CriteriaFile(writing=[], speaking=[]),
    )


def item(task_type: str, situation: str, retired: bool = False) -> Item:
    return Item.model_construct(task_type=task_type, situation=situation, retired=retired)


def items(*models: Item | None) -> dict[str, Item | None]:
    return {f"item.{n:03}": model for n, model in enumerate(models)}


def test_counts_the_items_per_topic_and_task_type_and_per_situation():
    coverage = count(
        catalog(),
        items(
            item("reading.matching", "home.renting"),
            item("reading.matching", "home.neighbors"),
            item("reading.true_false", "home.renting"),
            item("writing.formal", "work.interview"),
        ),
    )
    assert coverage.by_cell == {
        ("home", "reading.matching"): 2,
        ("home", "reading.true_false"): 1,
        ("work", "writing.formal"): 1,
    }
    assert coverage.by_situation == {"home.renting": 2, "home.neighbors": 1, "work.interview": 1}
    assert coverage.by_topic_and_part("home", "reading") == 3
    assert coverage.by_topic_and_part("home", "writing") == 0


def test_retired_items_and_items_not_in_the_format_are_not_counted():
    coverage = count(catalog(), items(item("reading.matching", "home.renting", retired=True), None, None))
    assert coverage.by_cell.total() == 0
    assert (coverage.retired, coverage.not_in_format) == (1, 2)


def test_retired_entries_of_the_catalog_are_left_out():
    coverage = count(
        catalog(),
        items(
            item("reading.old", "home.renting"),
            item("reading.matching", "home.old"),
            item("reading.matching", "past.memories"),
            item("reading.matching", "garden.flowers"),
        ),
    )
    assert coverage.topics == ["home", "work"]
    assert coverage.situations == ["home.renting", "home.neighbors", "work.interview"]
    assert coverage.task_types == ["listening.notes", "reading.matching", "reading.true_false", "writing.formal"]
    assert coverage.by_cell.total() == 0


def test_the_report():
    coverage = count(
        catalog(),
        items(
            item("reading.matching", "home.renting"),
            item("reading.matching", "home.neighbors"),
            item("writing.formal", "work.interview"),
            item("writing.formal", "work.interview", retired=True),
        ),
    )
    assert report(coverage) == [
        "Items per topic and part",
        "",
        "topic  items  listening  reading  writing  speaking",
        "home       2          0        2        0         0",
        "work       1          0        0        1         0",
        "all        3          0        2        1         0",
        "",
        "Items per situation",
        "",
        "home.renting    1",
        "home.neighbors  1",
        "work.interview  1",
        "",
        "Items per topic and task type",
        "",
        "topic  L1  R1  R2  W1",
        "home    .   2   .   .",
        "work    .   .   .   1",
        "all     .   2   .   1",
        "",
        "L1 listening: notes",
        "R1–R2 reading: matching, true_false",
        "W1 writing: formal",
        "",
        "3 items counted; 1 retired and 0 not in the format are not counted.",
    ]
