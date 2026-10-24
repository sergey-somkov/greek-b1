"""The files of content/: where they are, how they are read, and their formats.

The format of each catalog file and of the items of each task type is a
Pydantic model. A model checks the shape of a file: the fields, their types,
which are required, and no others. The counts, the references to the catalog
and the other rules of docs/task-types.md are checks of validator.py.

The models are strict: a string field takes a string only, so the implicit
types of YAML (`1935` as a number, `2026-01-02` as a date) are errors.
"""

from dataclasses import dataclass
from pathlib import Path
from typing import Annotated, Any, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator
from ruamel.yaml import YAML

PARTS = ("listening", "reading", "writing", "speaking")
CATALOG_FILES = ("task_types", "topics", "functions", "criteria")


def catalog_path(root: Path, name: str) -> Path:
    return root / "content" / "catalog" / f"{name}.yaml"


def items_dir(root: Path) -> Path:
    return root / "content" / "items"


def item_path(root: Path, item_id: str) -> Path:
    """The file of an item: items/<part>/<type>/<part>.<type>.<NNN>.yaml."""
    part, task_type, _ = item_id.split(".")
    return items_dir(root) / part / task_type / f"{item_id}.yaml"


def read_yaml(path: Path) -> Any:
    """The data of a YAML 1.2 file, with the line of each value (see validator.line_of).

    Raises ruamel.yaml.YAMLError, also on a duplicate key.
    """
    return YAML().load(path)


def load_catalog(root: Path) -> dict[str, Any]:
    """The catalog files, by their name without .yaml."""
    return {name: read_yaml(catalog_path(root, name)) for name in CATALOG_FILES}


class Model(BaseModel):
    model_config = ConfigDict(strict=True, extra="forbid")


Text = Annotated[str, Field(min_length=1)]
Texts = Annotated[list[Text], Field(min_length=1)]
Part = Literal["listening", "reading", "writing", "speaking"]


# The catalog, as the comments at the top of each catalog file describe it.


class Entry(Model):
    id: Text
    retired: bool


class AnswerFormat(Entry):
    user_does: Text
    recorded_answer: Text


class Genre(Entry):
    greek: Text
    example: Text


class Kind(Entry):
    meaning: Text


class TaskType(Entry):
    part: Part
    greek: Text
    exam_slot: Text
    items: Text
    points: Text
    minutes: Annotated[float, Field(gt=0)]
    answer: Text
    scored_by: Text
    genres: list[Genre] | None = None
    kinds: list[Kind] | None = None


class TaskTypesFile(Model):
    answer_formats: list[AnswerFormat]
    task_types: list[TaskType]


class Situation(Entry):
    name: Text
    greek: Text
    examples: Text


class Topic(Entry):
    name: Text
    greek: Text
    basis: Text
    situations: Annotated[list[Situation], Field(min_length=1)]


class Sample(Model):
    item: Text
    content: Text
    topic: Text


class TopicsFile(Model):
    topics: list[Topic]
    samples: list[Sample]


class GenreRef(Model):
    task_type: Text
    genre: Text


class Function(Entry):
    name: Text
    greek: Text
    exponents: Texts
    mostly_in: Annotated[list[Text | GenreRef], Field(min_length=1)]


class FunctionsFile(Model):
    functions: list[Function]


class WritingCriterion(Entry):
    name: Text
    greek: Text
    covers: Text
    bands: dict[int, Text]

    @field_validator("bands")
    @classmethod
    def bands_1_to_5(cls, bands: dict[int, str]) -> dict[int, str]:
        if list(bands) != [1, 2, 3, 4, 5]:
            raise ValueError("the bands are 1, 2, 3, 4 and 5, in this order")
        return bands


class SpeakingCriterion(Entry):
    name: Text
    greek: Text
    covers: Text
    points: dict[str, Text]

    @field_validator("points")
    @classmethod
    def three_levels(cls, points: dict[str, str]) -> dict[str, str]:
        if list(points) != ["1", "2-3", "4-5"]:
            raise ValueError("the points are '1', '2-3' and '4-5', in this order")
        return points


class CriteriaFile(Model):
    writing: list[WritingCriterion]
    speaking: list[SpeakingCriterion]


CATALOG_MODELS: dict[str, type[Model]] = {
    "task_types": TaskTypesFile,
    "topics": TopicsFile,
    "functions": FunctionsFile,
    "criteria": CriteriaFile,
}


@dataclass(frozen=True)
class Catalog:
    """The catalog files in their format."""

    task_types: TaskTypesFile
    topics: TopicsFile
    functions: FunctionsFile
    criteria: CriteriaFile


# The items, as content/README.md describes them.


class Item(Model):
    """The fields of every item."""

    task_type: Text
    situation: Text
    functions: list[Text]
    retired: bool
    title: Text
    framing: Text
    instruction: Text


class ReceptiveItem(Item):
    """A reading or listening item, which may leave out its functions."""

    functions: list[Text] = []


class ProductiveItem(Item):
    """A writing or speaking item, which must list its functions."""


class Speaker(Model):
    name: Text
    gender: Literal["female", "male"]
    age: Literal["child", "young", "adult", "senior"]


class Turn(Model):
    speaker: Text
    text: Text


class ListeningItem(ReceptiveItem):
    speakers: Annotated[list[Speaker], Field(min_length=1)]
    script: Annotated[list[Turn], Field(min_length=1)]


class Statement(Model):
    statement: Text
    answer: bool
    explanation: Text


class ShortAnswer(Model):
    answers: Texts
    explanation: Text


class NoteExample(ShortAnswer):
    label: Text


class NoteLine(Model):
    label: Text
    answers: Texts | None = None
    any_order: Annotated[list[Texts], Field(min_length=2)] | None = None
    explanation: Text

    @model_validator(mode="after")
    def one_key(self) -> "NoteLine":
        if (self.answers is None) == (self.any_order is None):
            raise ValueError("a line has either `answers` or `any_order`")
        return self

    def groups(self) -> list[list[str]]:
        """The alternatives of each line that the entry stands for."""
        return self.any_order or [self.answers or []]


class Row(Model):
    """A row of a grid."""

    heading: Text | None = None
    statement: Text
    example: Text | None = None
    ticks: list[Text]
    explanation: Text


class ListeningNotes(ListeningItem):
    max_words: Literal[3, 4]
    example: NoteExample
    lines: list[NoteLine]


class ListeningComparison(ListeningItem):
    columns: list[Text]
    rows: list[Row]


class ListeningTrueFalse(ListeningItem):
    example: Statement
    statements: list[Statement]


class ListeningGapFill(ListeningItem):
    text: Text
    example: ShortAnswer
    gaps: list[ShortAnswer]


class Question(Model):
    question: Text
    options: list[Text]
    answer: Literal["α", "β", "γ"]
    explanation: Text


class ListeningMultipleChoice(ListeningItem):
    example: Question
    questions: list[Question]


class Sentence(Model):
    first: Text
    second: Text
    explanation: Text


class ReadingMatching(ReceptiveItem):
    intro: Text | None = None
    example: Sentence
    sentences: list[Sentence]
    distractors: list[Text]


class NamedText(Model):
    name: Text
    text: Text


class ReadingComparison(ReceptiveItem):
    texts: list[NamedText]
    rows: list[Row]


class ReadingTrueFalse(ReceptiveItem):
    text: Text
    example: Statement
    statements: list[Statement]


class BankAnswer(Model):
    answer: Text
    explanation: Text


class ReadingGapFill(ReceptiveItem):
    text: Text
    example: BankAnswer
    gaps: list[BankAnswer]
    distractors: list[Text]


class WritingItem(ProductiveItem):
    genre: Text
    points_to_cover: list[Text]
    opening: Text
    signature: Text
    model_answer: Text | None = None


class InterviewQuestion(Model):
    question: Text
    follow_up: Text | None = None


class SpeakingInterview(ProductiveItem):
    questions: list[InterviewQuestion]


class SpeakingOpinion(ProductiveItem):
    question: Text
    follow_ups: list[Text]


class SpeakingPicture(ProductiveItem):
    description: Text
    picture_questions: Texts
    personal_questions: list[Text]


class Role(Model):
    card: Text
    arguments: list[Text]
    gives_way: Text


class SpeakingRoleplay(ProductiveItem):
    kind: Text
    # `register` would shadow BaseModel.register
    register_: Literal["informal", "formal"] = Field(alias="register")
    role_a: Role
    role_b: Role


ITEM_MODELS: dict[str, type[Item]] = {
    "listening.notes": ListeningNotes,
    "listening.comparison": ListeningComparison,
    "listening.true_false": ListeningTrueFalse,
    "listening.gap_fill": ListeningGapFill,
    "listening.multiple_choice": ListeningMultipleChoice,
    "reading.matching": ReadingMatching,
    "reading.comparison": ReadingComparison,
    "reading.true_false": ReadingTrueFalse,
    "reading.gap_fill": ReadingGapFill,
    "writing.informal": WritingItem,
    "writing.formal": WritingItem,
    "speaking.interview": SpeakingInterview,
    "speaking.opinion": SpeakingOpinion,
    "speaking.picture": SpeakingPicture,
    "speaking.roleplay": SpeakingRoleplay,
}
