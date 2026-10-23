"""The bundle of the content for the app: `tools/content.py bundle DIR`.

The build of the app runs it before the assets are merged (app/build.gradle.kts),
so the bundle is a build output, not a file in git. It writes two files under
DIR, which become the paths of the APK's assets:

- content/bundle.json: the catalog and the items, as JSON in UTF-8;
- content/version.txt: the content version, the SHA-256 of bundle.json in hex.
  The app compares it with the version it loaded, and reads the bundle only
  when they differ.

The bundle is a JSON object:

- `catalog`: the four catalog files in their format, by their name
  (`task_types`, `topics`, `functions`, `criteria`), retired entries included.
- `items`: every item, retired ones included, ordered by ID. An item has the
  columns of the content table of the app (`id`, `part`, `task_type`,
  `situation`, `topic`, `title`, `retired`) and the rest of its fields in
  `body`, in the order of the format. `part` and `topic` follow from the task
  type and the situation.

A field that an item leaves out is not in the bundle. The JSON is written the
same way each time, so the same content gives the same version.
"""

import hashlib
import json
from pathlib import Path
from typing import Any

from formats import Catalog, Item

BUNDLE = Path("content") / "bundle.json"
VERSION = Path("content") / "version.txt"

COLUMNS = ("task_type", "situation", "title", "retired")
"""The fields of an item that are columns of the content table, not in `body`."""


def build(catalog: Catalog, items: dict[str, Item]) -> bytes:
    """The bundle of the catalog and of the items by their ID, all in their format."""
    topic_of = {situation.id: topic.id for topic in catalog.topics.topics for situation in topic.situations}
    data = {
        "catalog": {
            "task_types": dump(catalog.task_types),
            "topics": dump(catalog.topics),
            "functions": dump(catalog.functions),
            "criteria": dump(catalog.criteria),
        },
        "items": [row(item_id, items[item_id], topic_of) for item_id in sorted(items)],
    }
    return json.dumps(data, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def dump(model: Any) -> Any:
    """The fields of a model as the YAML files name them, without the ones left out."""
    return model.model_dump(by_alias=True, exclude_none=True)


def row(item_id: str, item: Item, topic_of: dict[str, str]) -> dict[str, Any]:
    fields = dump(item)
    return {
        "id": item_id,
        "part": item.task_type.split(".")[0],
        "task_type": item.task_type,
        "situation": item.situation,
        "topic": topic_of[item.situation],
        "title": item.title,
        "retired": item.retired,
        "body": {name: value for name, value in fields.items() if name not in COLUMNS},
    }


def version(bundle: bytes) -> str:
    return hashlib.sha256(bundle).hexdigest()


def write(directory: Path, bundle: bytes) -> str:
    """Writes the bundle and its version under the directory, and returns the version."""
    content_version = version(bundle)
    (directory / BUNDLE).parent.mkdir(parents=True, exist_ok=True)
    (directory / BUNDLE).write_bytes(bundle)
    (directory / VERSION).write_text(content_version + "\n", encoding="utf-8")
    return content_version
