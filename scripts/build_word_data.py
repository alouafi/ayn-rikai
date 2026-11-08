#!/usr/bin/env python3
"""Build the offline word dictionary asset from JMdict (English).

Usage:
    python3 scripts/build_word_data.py [path/to/JMdict_e.gz]

Without an argument the latest JMdict_e is downloaded from the EDRDG.
It writes two tab-separated files read by `WordDictionary` on the device.

`words.tsv` has one row per word:

    spellings  readings  classes  senses

Spellings, readings and classes are joined with `|`. Senses are joined with `|` and the
glosses of one sense with `; `. Classes are the conjugation classes the deinflector needs
(v1, v5, v5k-s, v5aru, vk, vs-i, vs, adj-i). Common words come first, so each word's rank is its row.

`word_index.tsv` maps every spelling and reading, with katakana folded to hiragana, to the rows
of `words.tsv` (0-based, header excluded):

    key  row,row,...

Its lines are sorted by key in code point order, which is also UTF-8 byte order, so the app
can binary-search the file without parsing it.

JMdict is © the Electronic Dictionary Research and Development Group,
licensed under CC BY-SA 4.0 (https://www.edrdg.org/edrdg/licence.html).
"""

from __future__ import annotations

import gzip
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

SOURCE_URL = "http://ftp.edrdg.org/pub/Nihongo/JMdict_e.gz"
ASSETS = Path(__file__).resolve().parent.parent / "app/src/main/assets"
LIST_SEPARATOR = "|"
GLOSS_SEPARATOR = "; "
MAX_SENSES = 6
MAX_GLOSSES = 4

# Priority tags that JMdict uses to mark common words.
COMMON = {"news1", "ichi1", "spec1", "spec2", "gai1"}

# JMdict part-of-speech codes, mapped to the classes the deinflector knows.
CLASSES = {
    "v1": ["v1"],
    "v1-s": ["v1"],
    "v5k-s": ["v5", "v5k-s"],
    "v5aru": ["v5", "v5aru"],
    "vk": ["vk"],
    "vs-i": ["vs-i"],
    "vs-s": ["vs-i"],
    "vs": ["vs"],
    "adj-i": ["adj-i"],
    "adj-ix": ["adj-i"],
}


def load_xml(path: str | None) -> str:
    if path:
        data = Path(path).read_bytes()
    else:
        with urllib.request.urlopen(SOURCE_URL) as response:
            data = response.read()
    return gzip.decompress(data).decode("utf-8")


def strip_entities(xml: str) -> str:
    """Keeps the codes of JMdict's entities (`&v5k;` becomes `v5k`) instead of their long descriptions."""
    body = xml[xml.index("<JMdict>") :]
    return re.sub(r"&(?!amp;|lt;|gt;|quot;|apos;)([\w-]+);", r"\1", body)


def clean(value: str) -> str:
    if "\t" in value or LIST_SEPARATOR in value or "\n" in value:
        raise ValueError(f"Unexpected separator in value: {value!r}")
    return value


def classes_of(pos: str) -> list[str]:
    if pos.startswith("v5"):
        return CLASSES.get(pos, ["v5"])
    return CLASSES.get(pos, [])


def fold_katakana(text: str) -> str:
    """Katakana to hiragana, the same folding as `toHiragana` in the app."""
    return "".join(chr(ord(c) - 0x60) if "ァ" <= c <= "ヶ" else c for c in text)


def build_rows(root: ET.Element) -> list[tuple[list[str], str]]:
    """Each word's keys and row, common words first."""
    common_rows = []
    other_rows = []
    for entry in root.iter("entry"):
        spellings = [k.findtext("keb") for k in entry.findall("k_ele")]
        readings = [r.findtext("reb") for r in entry.findall("r_ele")]
        priorities = {p.text for p in entry.iter() if p.tag in ("ke_pri", "re_pri")}

        classes: list[str] = []
        senses: list[str] = []
        for sense in entry.findall("sense"):
            for pos in sense.findall("pos"):
                classes.extend(c for c in classes_of(pos.text or "") if c not in classes)
            glosses = [g.text for g in sense.findall("gloss") if g.text][:MAX_GLOSSES]
            if glosses and len(senses) < MAX_SENSES:
                senses.append(GLOSS_SEPARATOR.join(clean(g) for g in glosses))

        row = "\t".join(
            [
                LIST_SEPARATOR.join(clean(s) for s in spellings),
                LIST_SEPARATOR.join(clean(r) for r in readings),
                LIST_SEPARATOR.join(classes),
                LIST_SEPARATOR.join(senses),
            ]
        )
        keys = {fold_katakana(key) for key in spellings + readings}
        (common_rows if priorities & COMMON else other_rows).append((sorted(keys), row))
    return common_rows + other_rows


def build_index(rows: list[tuple[list[str], str]]) -> list[str]:
    index: dict[str, list[int]] = {}
    for number, (keys, _) in enumerate(rows):
        for key in keys:
            index.setdefault(key, []).append(number)
    return [f"{key}\t{','.join(map(str, numbers))}" for key, numbers in sorted(index.items())]


def write(name: str, header: str, lines: list[str]) -> None:
    # The app skips empty lines and lines starting with "#" as comments, which would shift the rows.
    skipped = [line for line in lines if not line or line.startswith("#")]
    if skipped:
        sys.exit(f"{name}: rows the app would skip as comments: {skipped[:3]}")
    path = ASSETS / name
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as output:
        output.write(header)
        output.write("\n".join(lines))
        output.write("\n")
    print(f"Wrote {len(lines)} lines to {path}")


def main() -> None:
    xml = load_xml(sys.argv[1] if len(sys.argv) > 1 else None)
    created = re.search(r"<!-- JMdict created: (\S+) -->", xml)
    root = ET.fromstring(strip_entities(xml))
    rows = build_rows(root)

    header = (
        f"# JMdict_e date={created.group(1) if created else '?'} "
        "(c) EDRDG, CC BY-SA 4.0, https://www.edrdg.org/edrdg/licence.html\n"
    )
    write("words.tsv", header, [row for _, row in rows])
    write("word_index.tsv", header, build_index(rows))


if __name__ == "__main__":
    main()
