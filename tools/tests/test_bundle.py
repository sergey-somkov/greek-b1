"""Tests of `tools/content.py bundle`, on the repository of the validator tests: a catalog and a
valid item of each task type."""

import hashlib
import json
from pathlib import Path
from typing import Any

from bundle import BUNDLE, VERSION, build, write
from formats import Catalog, Item, item_path
from test_validator import ITEMS, repo, write_yaml  # noqa: F401  (repo is a fixture)
from validator import validate


def content(root: Path) -> tuple[Catalog, dict[str, Item]]:
    """The catalog and the items of a repository with no problems."""
    result = validate(root)
    assert result.problems == [] and result.catalog is not None
    return result.catalog, {item_id: item for item_id, item in result.items.items() if item is not None}


def bundle_data(root: Path) -> dict[str, Any]:
    return json.loads(build(*content(root)))


def test_the_bundle_has_every_item_with_its_columns_and_body(repo: Path):
    items = bundle_data(repo)["items"]
    assert [item["id"] for item in items] == sorted(f"{type_id}.001" for type_id in ITEMS)
    gap_fill = next(item for item in items if item["id"] == "reading.gap_fill.001")
    source = ITEMS["reading.gap_fill"]()
    assert {name: value for name, value in gap_fill.items() if name != "body"} == {
        "id": "reading.gap_fill.001",
        "part": "reading",
        "task_type": "reading.gap_fill",
        "situation": "home.neighbors",
        "topic": "home",
        "title": source["title"],
        "retired": False,
    }
    assert list(gap_fill["body"]) == ["functions", "framing", "instruction", "text", "example", "gaps", "distractors"]
    assert gap_fill["body"]["gaps"] == source["gaps"]


def test_the_body_names_the_fields_as_the_file_does_and_leaves_out_the_missing_ones(repo: Path):
    items = {item["id"]: item for item in bundle_data(repo)["items"]}
    assert items["speaking.roleplay.001"]["body"]["register"] == "informal"
    assert "model_answer" not in items["writing.formal.001"]["body"]
    assert "follow_up" not in items["speaking.interview.001"]["body"]["questions"][0]


def test_a_retired_item_is_in_the_bundle(repo: Path):
    write_yaml(item_path(repo, "reading.true_false.002"), ITEMS["reading.true_false"]() | {"retired": True})
    items = {item["id"]: item for item in bundle_data(repo)["items"]}
    assert items["reading.true_false.002"]["retired"] is True


def test_the_bundle_has_the_catalog_in_its_format(repo: Path):
    catalog = bundle_data(repo)["catalog"]
    assert list(catalog) == ["task_types", "topics", "functions", "criteria"]
    assert [topic["id"] for topic in catalog["topics"]["topics"]] == ["home", "past"]
    assert catalog["topics"]["topics"][1]["retired"] is True
    assert catalog["criteria"]["writing"][0]["bands"]["1"] == "Band 1."
    assert catalog["task_types"]["task_types"][0]["minutes"] == 12.5


def test_the_same_content_gives_the_same_version(repo: Path, tmp_path: Path):
    first = write(tmp_path / "first", build(*content(repo)))
    second = write(tmp_path / "second", build(*content(repo)))
    assert first == second
    assert (tmp_path / "first" / BUNDLE).read_bytes() == (tmp_path / "second" / BUNDLE).read_bytes()


def test_the_version_is_the_sha256_of_the_bundle(repo: Path, tmp_path: Path):
    version = write(tmp_path, build(*content(repo)))
    assert version == hashlib.sha256((tmp_path / BUNDLE).read_bytes()).hexdigest()
    assert (tmp_path / VERSION).read_text() == version + "\n"


def test_a_change_of_an_item_changes_the_version(repo: Path, tmp_path: Path):
    before = write(tmp_path, build(*content(repo)))
    write_yaml(item_path(repo, "reading.true_false.001"), ITEMS["reading.true_false"]() | {"title": "Άλλος"})
    assert write(tmp_path, build(*content(repo))) != before


def test_the_bundle_is_utf8_without_escapes(repo: Path):
    assert "Τίτλος" in build(*content(repo)).decode("utf-8")
