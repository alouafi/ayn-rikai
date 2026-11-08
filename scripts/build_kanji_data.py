#!/usr/bin/env python3
"""Build the offline kanji dictionary asset from KANJIDIC2.

Usage:
    python3 scripts/build_kanji_data.py [path/to/kanjidic2.xml.gz]

Without an argument the latest KANJIDIC2 is downloaded from the EDRDG.
The output is a tab-separated file read by `KanjiDictionary` on the device:

    literal  strokes  grade  freq  radical  on  kun  meanings_en  meanings_fr

List fields are joined with `|`. Empty fields are left blank.
KANJIDIC2 is © the Electronic Dictionary Research and Development Group,
licensed under CC BY-SA 4.0 (https://www.edrdg.org/edrdg/licence.html).
"""

from __future__ import annotations

import gzip
import io
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

SOURCE_URL = "http://www.edrdg.org/kanjidic/kanjidic2.xml.gz"
OUTPUT = Path(__file__).resolve().parent.parent / "app/src/main/assets/kanji.tsv"
LIST_SEPARATOR = "|"


def load_xml(path: str | None) -> bytes:
    if path:
        return gzip.decompress(Path(path).read_bytes())
    with urllib.request.urlopen(SOURCE_URL) as response:
        return gzip.decompress(response.read())


def clean(value: str) -> str:
    if "\t" in value or LIST_SEPARATOR in value or "\n" in value:
        raise ValueError(f"Unexpected separator in value: {value!r}")
    return value


def join(values: list[str]) -> str:
    return LIST_SEPARATOR.join(clean(v) for v in values)


def text(element: ET.Element | None) -> str:
    return element.text.strip() if element is not None and element.text else ""


def build_rows(root: ET.Element) -> list[str]:
    rows = []
    for character in root.iter("character"):
        literal = text(character.find("literal"))
        misc = character.find("misc")
        radical = character.find("radical/rad_value[@rad_type='classical']")
        group = character.find("reading_meaning/rmgroup")

        on_readings: list[str] = []
        kun_readings: list[str] = []
        meanings_en: list[str] = []
        meanings_fr: list[str] = []
        if group is not None:
            for reading in group.findall("reading"):
                if reading.get("r_type") == "ja_on":
                    on_readings.append(text(reading))
                elif reading.get("r_type") == "ja_kun":
                    kun_readings.append(text(reading))
            for meaning in group.findall("meaning"):
                language = meaning.get("m_lang")
                if language is None:
                    meanings_en.append(text(meaning))
                elif language == "fr":
                    meanings_fr.append(text(meaning))

        rows.append(
            "\t".join(
                [
                    clean(literal),
                    text(misc.find("stroke_count")) if misc is not None else "",
                    text(misc.find("grade")) if misc is not None else "",
                    text(misc.find("freq")) if misc is not None else "",
                    text(radical),
                    join(on_readings),
                    join(kun_readings),
                    join(meanings_en),
                    join(meanings_fr),
                ]
            )
        )
    return rows


def main() -> None:
    root = ET.parse(io.BytesIO(load_xml(sys.argv[1] if len(sys.argv) > 1 else None))).getroot()
    header = root.find("header")
    version = text(header.find("file_version")) if header is not None else "?"
    created = text(header.find("date_of_creation")) if header is not None else "?"
    rows = build_rows(root)

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8", newline="\n") as output:
        output.write(
            f"# KANJIDIC2 file_version={version} date={created} "
            "(c) EDRDG, CC BY-SA 4.0, https://www.edrdg.org/edrdg/licence.html\n"
        )
        output.write("\n".join(rows))
        output.write("\n")
    print(f"Wrote {len(rows)} kanji to {OUTPUT}")


if __name__ == "__main__":
    main()
