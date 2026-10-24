"""The tables of the docs, written from the catalog: `tools/content.py docs`.

Each table of the docs is between the markers `<!-- catalog: NAME [ARG] -->`
and `<!-- /catalog -->`. NAME is one of the keys of RENDERERS below; ARG is
the ID that some of them need. A marker may be indented, inside a list item;
the table gets the same indent.
"""

import inspect
import re

from formats import PARTS

OPEN_MARKER = re.compile(r"^(\s*)<!-- catalog: (.+?) -->$")
CLOSE_MARKER = re.compile(r"^(\s*)<!-- /catalog -->$")


class DocsError(Exception):
    pass


# Markdown


def table(header, rows):
    lines = [table_row(header), "|" + "---|" * len(header)]
    return lines + [table_row(row) for row in rows]


def table_row(cells):
    return "| " + " | ".join(str(c).replace("\n", " ").replace("|", "\\|") for c in cells) + " |"


def code(text):
    return f"`{text}`"


def id_cell(entry):
    """The ID of a catalog entry, marked when it is retired."""
    return code(entry["id"]) + (" (retired)" if entry["retired"] else "")


def find(entries, entry_id, kind):
    for entry in entries:
        if entry["id"] == entry_id:
            return entry
    raise DocsError(f"no {kind} `{entry_id}` in the catalog")


# The tables. Each renderer takes the catalog and the ARG of its marker, if any,
# and returns the lines between the markers.


def render_task_types(catalog):
    return table(
        ["ID", "Greek name", "Exam slot", "Items", "Points", "Time", "Answer", "Scored by"],
        [
            [
                id_cell(t),
                t["greek"],
                t["exam_slot"],
                t["items"],
                t["points"],
                f"{t['minutes']:g} min",
                code(t["answer"]),
                t["scored_by"],
            ]
            for t in catalog["task_types"]["task_types"]
        ],
    )


def render_answer_formats(catalog):
    return table(
        ["Format", "What the user does", "Recorded answer"],
        [[id_cell(f), f["user_does"], f["recorded_answer"]] for f in catalog["task_types"]["answer_formats"]],
    )


def render_genres(catalog, task_type_id):
    task_type = find(catalog["task_types"]["task_types"], task_type_id, "task type")
    if "genres" not in task_type:
        raise DocsError(f"the task type `{task_type_id}` has no genres")
    return table(["Genre", "Greek", "Example"], [[id_cell(g), g["greek"], g["example"]] for g in task_type["genres"]])


def render_kinds(catalog, task_type_id):
    task_type = find(catalog["task_types"]["task_types"], task_type_id, "task type")
    if "kinds" not in task_type:
        raise DocsError(f"the task type `{task_type_id}` has no kinds")
    return table(["Kind", "Meaning"], [[id_cell(k), k["meaning"]] for k in task_type["kinds"]])


def render_topics(catalog):
    samples = catalog["topics"]["samples"]
    return table(
        ["ID", "Topic", "Greek", "Basis", "Sample items"],
        [
            [id_cell(t), t["name"], t["greek"], t["basis"], sum(s["topic"] == t["id"] for s in samples)]
            for t in catalog["topics"]["topics"]
        ],
    )


def render_situations(catalog, topic_id):
    """The situations of a topic, and the sample items of the topic in the order of the papers."""
    topic = find(catalog["topics"]["topics"], topic_id, "topic")
    samples = [s["item"] for s in catalog["topics"]["samples"] if s["topic"] == topic_id]
    return table(
        ["ID", "Situation", "Greek", "Examples"],
        [[id_cell(s), s["name"], s["greek"], s["examples"]] for s in topic["situations"]],
    ) + ["", f"**In the samples:** {', '.join(samples) or 'none'}."]


def render_samples(catalog):
    return table(
        ["Item", "Content", "Topic"],
        [[s["item"], s["content"], code(s["topic"])] for s in catalog["topics"]["samples"]],
    )


def render_functions(catalog):
    return table(
        ["ID", "Function", "Greek", "Typical exponents", "Mostly in"],
        [
            [
                id_cell(f),
                f["name"],
                f["greek"],
                " ".join(f"«{e}»" for e in f["exponents"]),
                mostly_in(f["mostly_in"]),
            ]
            for f in catalog["functions"]["functions"]
        ],
    )


def mostly_in(refs):
    """`mostly_in` of a function: task types, task types with a genre, and parts.

    Parts next to each other are joined: [writing, speaking] is "all writing and speaking".
    """
    cells, parts = [], []
    for ref in refs + [None]:
        if ref in PARTS:
            parts.append(ref)
            continue
        if parts:
            cells.append("all " + " and ".join(parts))
            parts = []
        if isinstance(ref, str):
            cells.append(code(ref))
        elif ref is not None:
            cells.append(f"{code(ref['task_type'])} ({ref['genre']})")
    return ", ".join(cells)


def render_writing_criteria(catalog):
    return table(
        ["ID", "Criterion", "Greek terms of the scale", "What it covers"],
        [[id_cell(c), c["name"], c["greek"], c["covers"]] for c in catalog["criteria"]["writing"]],
    )


def render_writing_bands(catalog):
    """A table per band of the 25-point scale; band N covers the points 5N-4 to 5N."""
    criteria = catalog["criteria"]["writing"]
    lines = []
    for band in range(1, 6):
        if lines:
            lines.append("")
        lines += [f"**Band {band} ({5 * band - 4}–{5 * band} points)**", ""]
        lines += table(["Criterion", "Descriptor"], [[id_cell(c), c["bands"][band]] for c in criteria])
    return lines


def render_speaking_criteria(catalog):
    return table(
        ["ID", "Criterion", "Greek name", "What it covers"],
        [[id_cell(c), c["name"], c["greek"], c["covers"]] for c in catalog["criteria"]["speaking"]],
    )


def render_speaking_points(catalog):
    criteria = catalog["criteria"]["speaking"]
    levels = list(criteria[0]["points"])
    return table(
        ["Criterion"] + [level.replace("-", "–") + (" point" if level == "1" else " points") for level in levels],
        [[id_cell(c)] + [c["points"][level] for level in levels] for c in criteria],
    )


RENDERERS = {
    "task_types": render_task_types,
    "answer_formats": render_answer_formats,
    "genres": render_genres,
    "kinds": render_kinds,
    "topics": render_topics,
    "situations": render_situations,
    "samples": render_samples,
    "functions": render_functions,
    "writing_criteria": render_writing_criteria,
    "writing_bands": render_writing_bands,
    "speaking_criteria": render_speaking_criteria,
    "speaking_points": render_speaking_points,
}


def render(catalog, marker):
    """The lines of the table of a marker, `NAME` or `NAME ARG`."""
    name, *args = marker.split()
    renderer = RENDERERS.get(name)
    if renderer is None:
        raise DocsError(f"unknown table `{name}`")
    try:
        inspect.signature(renderer).bind(catalog, *args)
    except TypeError:
        raise DocsError(f"wrong arguments for the table `{name}`: {' '.join(args) or 'none'}") from None
    return renderer(catalog, *args)


def rewrite(text, catalog):
    """The text with each block between markers rewritten from the catalog.

    Raises DocsError with the line number of the marker at fault.
    """
    lines = text.split("\n")
    out = []
    i = 0
    while i < len(lines):
        out.append(lines[i])
        if CLOSE_MARKER.match(lines[i]):
            raise DocsError(f"line {i + 1}: a closing marker without an opening one")
        opening = OPEN_MARKER.match(lines[i])
        if not opening:
            i += 1
            continue
        indent, marker = opening.groups()
        end = i + 1
        while end < len(lines) and not CLOSE_MARKER.match(lines[end]):
            if OPEN_MARKER.match(lines[end]):
                raise DocsError(f"line {end + 1}: a marker inside the block of line {i + 1}")
            end += 1
        if end == len(lines):
            raise DocsError(f"line {i + 1}: the block has no closing marker")
        if lines[end] != indent + "<!-- /catalog -->":
            raise DocsError(f"line {end + 1}: the closing marker is not indented as the opening one")
        try:
            block = render(catalog, marker)
        except DocsError as e:
            raise DocsError(f"line {i + 1}: {e}") from None
        out += [""] + [indent + line if line else "" for line in block] + ["", lines[end]]
        i = end + 1
    return "\n".join(out)
