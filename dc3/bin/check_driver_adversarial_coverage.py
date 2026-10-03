#!/usr/bin/env python3
#
# Copyright 2016-present the IoT DC3 original author or authors.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
"""Driver adversarial-coverage gate.

Every protocol driver module must carry an adversarial contract test
(``*DriverAdversarialTest.java`` extending the shared
``io.github.pnoker.test.driver.DriverAdversarialContract``), so the black-box,
fuzz, and stress coverage introduced in the 2026-09-30 driver hardening cannot
silently skip a new driver. The shared harness is provided to every module by
the ``dc3-driver`` parent pom (dc3-common-test, test scope).
"""

import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DRIVER_ROOT = REPO_ROOT / "dc3-driver"


def main() -> int:
    modules = sorted(
        child for child in DRIVER_ROOT.iterdir() if child.is_dir() and child.name.startswith("dc3-driver-")
    )
    if not modules:
        print("Driver adversarial-coverage check failed: no driver modules found", file=sys.stderr)
        return 1

    missing = []
    for module in modules:
        tests = list((module / "src" / "test").rglob("*DriverAdversarialTest.java"))
        if not tests:
            missing.append(module.name)

    if missing:
        print(
            f"Driver adversarial-coverage check failed: {len(missing)} of {len(missing) + (len(modules) - len(missing))} "
            f"driver modules lack a *DriverAdversarialTest: {', '.join(missing)}",
            file=sys.stderr,
        )
        print(
            "Add one test class per driver extending DriverAdversarialContract "
            "(see dc3-common-test, io.github.pnoker.test.driver).",
            file=sys.stderr,
        )
        return 1

    print(
        "Driver adversarial-coverage check passed: "
        f"rule=every dc3-driver-* module carries a *DriverAdversarialTest, modules={len(modules)}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
