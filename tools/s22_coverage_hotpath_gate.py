#!/usr/bin/env python3
"""S22 adversarial structural gate: coverage discovery/scanning must remain tooling-only."""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path("src/main/java")
ALLOWED = {
    Path("src/main/java/io/github/r3neer/scalebrews/collision/catalog/CollisionCoverageDiscovery.java"),
    Path("src/main/java/io/github/r3neer/scalebrews/collision/catalog/CollisionCoverageScanner.java"),
}
PATTERN = re.compile(r"\bCollisionCoverage(?:Discovery|Scanner)\b")

hits: list[str] = []
for path in sorted(ROOT.rglob("*.java")):
    if path in ALLOWED:
        continue
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if PATTERN.search(line):
            hits.append(f"{path}:{line_number}: {line.strip()}")

if hits:
    print("S22 coverage hot-path gate FAILED: coverage tooling leaked outside its tooling implementation files:")
    for hit in hits:
        print(f"  {hit}")
    raise SystemExit(1)

print("S22 coverage hot-path gate passed: no production caller references coverage discovery/scanner.")
