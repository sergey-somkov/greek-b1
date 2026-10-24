#!/usr/bin/env python3
"""A page that shows the vector drawables of the app, for a look at them outside Android Studio.

    tools/drawables.py [OUT]

Converts each vector drawable in app/src/main/res/drawable*/ to SVG and writes them all to one
HTML page, OUT (by default app/build/drawables.html, a build output), each icon on a light and a
dark background. Open the page in a browser. A drawable that is not a vector is listed as skipped.

A vector drawable is close to SVG: `android:pathData` is the `d` of an SVG path. The conversion
covers what the Material Symbols use and a bit more: paths with fill and stroke, groups with their
transforms, clip paths. A color that refers to a resource or a theme attribute (`@color/…`,
`?attr/…`) cannot be resolved here and is drawn in the color of the text; so is every color of a
drawable with `android:tint`, since the tint replaces them in the app too.

Only the standard library: the script runs with any Python 3.12.
"""

import argparse
import html
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
RES = REPO_ROOT / "app" / "src" / "main" / "res"
DEFAULT_OUT = REPO_ROOT / "app" / "build" / "drawables.html"

ANDROID = "{http://schemas.android.com/apk/res/android}"

# The icons are drawn at this many CSS pixels per dp, so that a 24 dp icon is large enough to see.
SCALE = 4


class NotAVector(Exception):
    """The drawable is not a <vector> (a shape, a selector, a bitmap…)."""


def attr(element: ET.Element, name: str, default: str | None = None) -> str | None:
    return element.get(ANDROID + name, default)


def number(element: ET.Element, name: str, default: float) -> float:
    value = attr(element, name)
    return default if value is None else float(value.removesuffix("dp"))


def color(value: str | None, tinted: bool) -> str:
    """An Android color as CSS: #RGB, #ARGB, #RRGGBB or #AARRGGBB; a reference is the text color."""
    if value is None:
        return "none"
    if tinted or not value.startswith("#"):
        return "currentColor"
    digits = value[1:]
    if len(digits) in (3, 4):
        digits = "".join(c * 2 for c in digits)
    if len(digits) == 6:
        return "#" + digits
    if len(digits) == 8:
        alpha, rgb = int(digits[:2], 16) / 255, digits[2:]
        return f"rgba({int(rgb[0:2], 16)}, {int(rgb[2:4], 16)}, {int(rgb[4:6], 16)}, {alpha:.3f})"
    raise ValueError(f"not a color: {value}")


def path_svg(element: ET.Element, tinted: bool) -> str:
    fill = color(attr(element, "fillColor"), tinted)
    stroke = color(attr(element, "strokeColor"), tinted)
    parts = [f'd="{html.escape(attr(element, "pathData") or "")}"', f'fill="{fill}"']
    if attr(element, "fillType") == "evenOdd":
        parts.append('fill-rule="evenodd"')
    if attr(element, "fillAlpha") is not None:
        parts.append(f'fill-opacity="{number(element, "fillAlpha", 1)}"')
    if stroke != "none":
        parts.append(f'stroke="{stroke}"')
        parts.append(f'stroke-width="{number(element, "strokeWidth", 0)}"')
        if attr(element, "strokeAlpha") is not None:
            parts.append(f'stroke-opacity="{number(element, "strokeAlpha", 1)}"')
        for android_name, svg_name in (("strokeLineCap", "stroke-linecap"), ("strokeLineJoin", "stroke-linejoin")):
            value = attr(element, android_name)
            if value is not None:
                parts.append(f'{svg_name}="{value}"')
    return f"<path {' '.join(parts)}/>"


def group_transform(element: ET.Element) -> str:
    """The transform of a <group>, in the order of Android: pivot, scale, rotate, translate."""
    px, py = number(element, "pivotX", 0), number(element, "pivotY", 0)
    tx, ty = number(element, "translateX", 0), number(element, "translateY", 0)
    sx, sy = number(element, "scaleX", 1), number(element, "scaleY", 1)
    rotation = number(element, "rotation", 0)
    # SVG applies the rightmost transform first.
    return f"translate({tx + px:g} {ty + py:g}) rotate({rotation:g}) scale({sx:g} {sy:g}) translate({-px:g} {-py:g})"


def children_svg(element: ET.Element, tinted: bool, ids: list[int]) -> str:
    """The children of a <vector> or a <group>. A <clip-path> clips the siblings after it."""
    out: list[str] = []
    clips: list[str] = []
    for child in element:
        if child.tag == "path":
            out.append(path_svg(child, tinted))
        elif child.tag == "group":
            out.append(f'<g transform="{group_transform(child)}">{children_svg(child, tinted, ids)}</g>')
        elif child.tag == "clip-path":
            ids.append(len(ids) + 1)
            clip_id = f"clip{ids[-1]}"
            out.append(f'<clipPath id="{clip_id}"><path d="{html.escape(attr(child, "pathData") or "")}"/></clipPath>')
            out.append(f'<g clip-path="url(#{clip_id})">')
            clips.append("</g>")
    return "".join(out) + "".join(reversed(clips))


def vector_to_svg(xml: str) -> str:
    """The SVG of a vector drawable, at SCALE pixels per dp."""
    root = ET.fromstring(xml)
    if root.tag != "vector":
        raise NotAVector(root.tag)
    width, height = number(root, "width", 24) * SCALE, number(root, "height", 24) * SCALE
    viewport = f"0 0 {number(root, 'viewportWidth', 24):g} {number(root, 'viewportHeight', 24):g}"
    tinted = attr(root, "tint") is not None
    body = children_svg(root, tinted, [])
    if attr(root, "alpha") is not None:
        body = f'<g opacity="{number(root, "alpha", 1)}">{body}</g>'
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{width:g}" height="{height:g}" viewBox="{viewport}">{body}</svg>'


PAGE = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Drawables</title>
<style>
  body {{ font-family: sans-serif; margin: 24px; background: #fafafa; color: #222; }}
  .grid {{ display: flex; flex-wrap: wrap; gap: 16px; }}
  .card {{ border: 1px solid #ddd; border-radius: 8px; background: #fff; padding: 12px; }}
  .pair {{ display: flex; gap: 8px; }}
  .light, .dark {{ padding: 8px; border-radius: 4px; line-height: 0; }}
  .light {{ background: #fff; color: #000; }}
  .dark {{ background: #222; color: #fff; }}
  .light svg {{ outline: 1px dashed #ccc; }}
  .name {{ margin-top: 8px; font-size: 14px; }}
  .note {{ color: #777; font-size: 12px; }}
</style>
</head>
<body>
<h1>Drawables</h1>
<p class="note">{count} vector drawables from app/src/main/res. The dashed outline is the size of the icon.</p>
<div class="grid">
{cards}
</div>
{skipped}
</body>
</html>
"""

CARD = """<div class="card">
  <div class="pair"><div class="light">{svg}</div><div class="dark">{svg}</div></div>
  <div class="name">{name}</div>
  <div class="note">{size}</div>
</div>"""


def page(res: Path) -> str:
    cards: list[str] = []
    skipped: list[str] = []
    for path in sorted(res.glob("drawable*/*.xml")):
        name = str(path.relative_to(res))
        try:
            svg = vector_to_svg(path.read_text(encoding="utf-8"))
        except NotAVector as e:
            skipped.append(f"<li>{html.escape(name)}: &lt;{html.escape(str(e))}&gt;</li>")
            continue
        root = ET.fromstring(path.read_text(encoding="utf-8"))
        size = f"{number(root, 'width', 24):g} × {number(root, 'height', 24):g} dp"
        cards.append(CARD.format(svg=svg, name=html.escape(name), size=size))
    skipped_html = f"<h2>Skipped, not vectors</h2><ul>{''.join(skipped)}</ul>" if skipped else ""
    return PAGE.format(count=len(cards), cards="\n".join(cards), skipped=skipped_html)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("out", nargs="?", type=Path, default=DEFAULT_OUT, help="the HTML page to write")
    args = parser.parse_args()
    out: Path = args.out
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(page(RES), encoding="utf-8")
    print(out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
