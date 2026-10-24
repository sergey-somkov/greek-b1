"""Tests of `tools/drawables.py`: vector drawables as SVG."""

import pytest

from drawables import NotAVector, color, vector_to_svg

NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'


def vector(body: str, extra: str = "") -> str:
    return (
        f'<vector {NS} android:width="24dp" android:height="24dp" '
        f'android:viewportWidth="960" android:viewportHeight="960" {extra}>{body}</vector>'
    )


def test_a_path_keeps_its_data_and_the_viewport():
    svg = vector_to_svg(vector('<path android:fillColor="#FF0000" android:pathData="M0,0L960,960Z"/>'))
    assert 'viewBox="0 0 960 960"' in svg
    assert 'width="96" height="96"' in svg
    assert '<path d="M0,0L960,960Z" fill="#FF0000"/>' in svg


@pytest.mark.parametrize(
    ("android", "css"),
    [
        ("#F00", "#FF0000"),
        ("#80FF0000", "rgba(255, 0, 0, 0.502)"),
        ("@android:color/white", "currentColor"),
        ("?attr/colorPrimary", "currentColor"),
        (None, "none"),
    ],
)
def test_colors(android: str | None, css: str):
    assert color(android, tinted=False) == css


def test_a_tint_draws_every_color_in_the_text_color():
    svg = vector_to_svg(
        vector('<path android:fillColor="#FF0000" android:pathData="M0,0Z"/>', 'android:tint="?attr/colorControlNormal"')
    )
    assert 'fill="currentColor"' in svg


def test_a_group_transform_follows_the_order_of_android():
    svg = vector_to_svg(
        vector(
            '<group android:pivotX="10" android:pivotY="20" android:rotation="90" android:translateX="5">'
            '<path android:pathData="M0,0Z"/></group>'
        )
    )
    assert '<g transform="translate(15 20) rotate(90) scale(1 1) translate(-10 -20)">' in svg


def test_a_clip_path_clips_the_siblings_after_it():
    svg = vector_to_svg(
        vector('<path android:pathData="M1Z"/><clip-path android:pathData="M2Z"/><path android:pathData="M3Z"/>')
    )
    assert svg.index('d="M1Z"') < svg.index('<g clip-path="url(#clip1)">') < svg.index('d="M3Z"')
    assert svg.endswith("</g></svg>")


def test_a_shape_is_not_a_vector():
    with pytest.raises(NotAVector):
        vector_to_svg(f'<shape {NS}><solid android:color="#FFF"/></shape>')
