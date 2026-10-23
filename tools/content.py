#!/usr/bin/env python3
"""The scripts of the learning content in content/.

    tools/content.py docs
    tools/content.py validate
    tools/content.py coverage
    tools/content.py bundle DIR

docs: rewrites each table of the docs between the markers
`<!-- catalog: NAME [ARG] -->` and `<!-- /catalog -->` from the catalog in
content/catalog/, and leaves the rest of the docs alone (docs_tables.py).

validate: checks the catalog, the items, their audio and pictures, and the
tables of the docs (validator.py). It prints each error and warning as
`path:line: error: message`, and fails if there is an error; a warning asks
the author to look, and may stay.

coverage: validates the content, and counts the items that are in the format
and not retired: per topic and part, per situation, and per topic and task
type (coverage.py). It prints the number of errors, if any, but not the
problems themselves; validate does.

bundle: validates the content, prints the problems as validate does, and, if
there is no error, writes the bundle of the content and its version under DIR
(bundle.py). The build of the app runs it, so a validation error fails the
build.

The script runs in the virtual environment of the repository (see
pyproject.toml), whatever Python starts it.
"""

import argparse
import os
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
VENV = REPO_ROOT / ".venv"

if __name__ == "__main__" and Path(sys.prefix).resolve() != VENV.resolve():
    venv_python = VENV / "bin" / "python"
    if not venv_python.exists():
        sys.exit("The Python environment .venv/ is missing: set it up with the commands at the top of pyproject.toml.")
    os.execv(venv_python, [str(venv_python), __file__, *sys.argv[1:]])

import bundle  # noqa: E402
import coverage  # noqa: E402
import validator  # noqa: E402
from docs_tables import DocsError, rewrite  # noqa: E402
from formats import load_catalog  # noqa: E402


def docs():
    catalog = load_catalog(REPO_ROOT)
    failed = False
    for path in sorted((REPO_ROOT / "docs").glob("*.md")):
        name = path.relative_to(REPO_ROOT)
        text = path.read_text(encoding="utf-8")
        try:
            new_text = rewrite(text, catalog)
        except DocsError as e:
            print(f"{name}: {e}", file=sys.stderr)
            failed = True
            continue
        if new_text != text:
            path.write_text(new_text, encoding="utf-8")
            print(f"{name}: rewritten")
    return 1 if failed else 0


def validate():
    result = validator.validate(REPO_ROOT)
    for problem in result.problems:
        print(problem, file=sys.stderr)
    errors, warnings = result.count("error"), result.count("warning")
    print(f"{len(result.items)} items: {errors} errors, {warnings} warnings")
    return 1 if errors else 0


def coverage_report():
    result = validator.validate(REPO_ROOT)
    if result.catalog is None:
        for problem in result.problems:
            print(problem, file=sys.stderr)
        print("The catalog is not in its format: no coverage.", file=sys.stderr)
        return 1
    print("\n".join(coverage.report(coverage.count(result.catalog, result.items))), flush=True)
    errors = result.count("error")
    if errors:
        message = f"The content has {errors} errors, so the counts may be off; tools/content.py validate prints them."
        print(message, file=sys.stderr)
    return 0


def make_bundle(directory: Path):
    result = validator.validate(REPO_ROOT)
    for problem in result.problems:
        print(problem, file=sys.stderr)
    errors, warnings = result.count("error"), result.count("warning")
    if errors or result.catalog is None:
        print(f"{len(result.items)} items: {errors} errors, {warnings} warnings; no bundle", file=sys.stderr)
        return 1
    items = {item_id: item for item_id, item in result.items.items() if item is not None}
    version = bundle.write(directory, bundle.build(result.catalog, items))
    print(f"{len(items)} items: {warnings} warnings; content version {version[:8]} in {directory}")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("docs", help="rewrite the tables of the docs from the catalog")
    commands.add_parser("validate", help="check the content and the tables of the docs")
    commands.add_parser("coverage", help="count the items per topic, situation and task type")
    bundle_parser = commands.add_parser("bundle", help="validate the content and write its bundle for the app")
    bundle_parser.add_argument("directory", type=Path, help="where to write content/bundle.json and content/version.txt")
    args = parser.parse_args()
    if args.command == "docs":
        return docs()
    if args.command == "validate":
        return validate()
    if args.command == "coverage":
        return coverage_report()
    if args.command == "bundle":
        return make_bundle(args.directory)


if __name__ == "__main__":
    sys.exit(main())
