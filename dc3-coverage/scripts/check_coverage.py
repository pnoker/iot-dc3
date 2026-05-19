#!/usr/bin/env python3
"""Validate the aggregate JaCoCo XML report against repository thresholds."""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Counter:
    name: str
    missed: int
    covered: int

    @property
    def total(self) -> int:
        return self.missed + self.covered

    @property
    def ratio(self) -> float:
        if self.total == 0:
            return 1.0
        return self.covered / self.total


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Fail the build if aggregate JaCoCo coverage is below threshold.",
    )
    parser.add_argument("xml_report", type=Path, help="Path to jacoco.xml")
    parser.add_argument("--minimum-line", type=float, required=True)
    parser.add_argument("--minimum-branch", type=float, required=True)
    return parser.parse_args()


def read_counters(xml_report: Path) -> dict[str, Counter]:
    if not xml_report.is_file():
        raise FileNotFoundError(f"JaCoCo XML report does not exist: {xml_report}")

    root = ET.parse(xml_report).getroot()
    counters: dict[str, Counter] = {}
    for element in root.findall("counter"):
        name = element.attrib["type"]
        counters[name] = Counter(
            name=name,
            missed=int(element.attrib["missed"]),
            covered=int(element.attrib["covered"]),
        )
    return counters


def format_percent(value: float) -> str:
    return f"{value * 100:.2f}%"


def check(counter: Counter, minimum: float) -> bool:
    print(
        f"Aggregate {counter.name.lower()} coverage: "
        f"{format_percent(counter.ratio)} "
        f"({counter.covered}/{counter.total}), "
        f"minimum {format_percent(minimum)}"
    )
    return counter.ratio >= minimum


def main() -> int:
    args = parse_args()
    try:
        counters = read_counters(args.xml_report)
    except Exception as exc:  # noqa: BLE001 - build tool should show the exact cause.
        print(f"Coverage check failed: {exc}", file=sys.stderr)
        return 1

    required = {
        "LINE": args.minimum_line,
        "BRANCH": args.minimum_branch,
    }
    missing = [name for name in required if name not in counters]
    if missing:
        print(
            f"Coverage check failed: missing counters {', '.join(missing)}",
            file=sys.stderr,
        )
        return 1

    passed = True
    for name, minimum in required.items():
        passed = check(counters[name], minimum) and passed

    if not passed:
        print("Coverage check failed: aggregate coverage is below threshold.", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
