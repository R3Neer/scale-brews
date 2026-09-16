#!/usr/bin/env python3
"""S23 adversarial source gate: keep DisplayRig/display-composite knowledge out of production."""

from __future__ import annotations

import re
from pathlib import Path

ROOTS = (Path("src/main/java"), Path("src/main/resources"))
TEXT_SUFFIXES = {".java", ".json", ".mcmeta", ".properties", ".txt"}
FORBIDDEN = (
    re.compile(r"\bDisplayRig\b", re.IGNORECASE),
    re.compile(r"display[_-]?rig", re.IGNORECASE),
    re.compile(r"display[_-]?composite", re.IGNORECASE),
    re.compile(r"s23[_-]?display", re.IGNORECASE),
)

hits: list[str] = []
for root in ROOTS:
    if not root.exists():
        continue
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        if path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        text = path.read_text(encoding="utf-8")
        for line_number, line in enumerate(text.splitlines(), 1):
            if any(pattern.search(line) for pattern in FORBIDDEN):
                hits.append(f"{path}:{line_number}: {line.strip()}")

if hits:
    print("S23 SPI-only gate FAILED: production contains DisplayRig/display-composite-specific knowledge:")
    for hit in hits:
        print(f"  {hit}")
    raise SystemExit(1)

print("S23 SPI-only gate passed: no DisplayRig/display-composite-specific production knowledge found.")
