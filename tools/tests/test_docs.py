"""Tests of `tools/content.py docs`: the tables of the docs from the catalog."""

import re

import pytest

from docs_tables import DocsError, mostly_in, rewrite

CATALOG = {
    "task_types": {
        "answer_formats": [
            {"id": "choice", "retired": False, "user_does": "Picks one.", "recorded_answer": "The option."},
            {"id": "grid", "retired": True, "user_does": "Ticks | cells.", "recorded_answer": "The cells."},
        ],
        "task_types": [
            {
                "id": "speaking.roleplay",
                "retired": False,
                "kinds": [{"id": "service", "retired": False, "meaning": "A customer and a clerk."}],
            },
        ],
    },
}

FORMATS_TABLE = [
    "| Format | What the user does | Recorded answer |",
    "|---|---|---|",
    "| `choice` | Picks one. | The option. |",
    "| `grid` (retired) | Ticks \\| cells. | The cells. |",
]


def doc(*lines):
    return "\n".join(lines) + "\n"


def test_rewrites_the_block_and_leaves_the_rest_alone():
    text = doc("# Title", "", "Prose.", "<!-- catalog: answer_formats -->", "| old |", "<!-- /catalog -->", "More prose.")
    assert rewrite(text, CATALOG) == doc(
        "# Title", "", "Prose.", "<!-- catalog: answer_formats -->", "", *FORMATS_TABLE, "", "<!-- /catalog -->",
        "More prose.",
    )


def test_a_rewritten_doc_stays_the_same():
    text = rewrite(doc("<!-- catalog: answer_formats -->", "<!-- /catalog -->"), CATALOG)
    assert rewrite(text, CATALOG) == text


def test_an_indented_block_gets_its_indent():
    text = doc("- **Kind:**", "", "  <!-- catalog: kinds speaking.roleplay -->", "  <!-- /catalog -->")
    assert rewrite(text, CATALOG) == doc(
        "- **Kind:**",
        "",
        "  <!-- catalog: kinds speaking.roleplay -->",
        "",
        "  | Kind | Meaning |",
        "  |---|---|",
        "  | `service` | A customer and a clerk. |",
        "",
        "  <!-- /catalog -->",
    )


@pytest.mark.parametrize(
    "lines, error",
    [
        (["<!-- catalog: nothing -->", "<!-- /catalog -->"], "line 1: unknown table `nothing`"),
        (["<!-- catalog: kinds -->", "<!-- /catalog -->"], "line 1: wrong arguments for the table `kinds`: none"),
        (["<!-- catalog: answer_formats x -->", "<!-- /catalog -->"], "line 1: wrong arguments"),
        (["<!-- catalog: kinds speaking.picture -->", "<!-- /catalog -->"], "line 1: no task type `speaking.picture`"),
        (["<!-- catalog: answer_formats -->", "text"], "line 1: the block has no closing marker"),
        (["text", "<!-- /catalog -->"], "line 2: a closing marker without an opening one"),
        (
            ["<!-- catalog: answer_formats -->", "<!-- catalog: answer_formats -->", "<!-- /catalog -->"],
            "line 2: a marker inside the block of line 1",
        ),
        (["<!-- catalog: answer_formats -->", "  <!-- /catalog -->"], "line 2: the closing marker is not indented"),
    ],
)
def test_a_wrong_marker_is_an_error(lines, error):
    with pytest.raises(DocsError, match=re.escape(error)):
        rewrite(doc(*lines), CATALOG)


def test_mostly_in_names_the_parts_and_the_genres():
    refs = ["speaking.opinion", {"task_type": "writing.formal", "genre": "article"}, "writing", "speaking"]
    assert mostly_in(refs) == "`speaking.opinion`, `writing.formal` (article), all writing and speaking"
    assert mostly_in(["writing.informal", "speaking"]) == "`writing.informal`, all speaking"
