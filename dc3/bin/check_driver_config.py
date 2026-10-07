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
"""Driver broker-configuration gate.

The RabbitMQ connection placeholders live in exactly one place: the
``application-rabbitmq.yml`` profile file shipped inside the
``dc3-mq-rabbitmq`` jar. The ``rabbitmq`` profile is auto-activated in every
service embedding the adapter (drivers, manager, data), so each of them
inherits ``spring.rabbitmq.*`` connection defaults without copying the block
into per-module ``application-[dev|test|pre|pro].yml`` files. Before that
single source of truth existed, 23 of 36 driver modules were scaffolded
without ``application-test.yml``/``application-pre.yml``/``application-pro.yml``,
fell back to ``localhost:5672`` under the compose ``NODE_ENV=test`` default,
and crash-looped in ``RabbitMqAdapter`` bean creation.

The gate fails when: (a) the SDK carrier file loses a connection placeholder
binding; (b) any consumer module (``dc3-driver-*``, ``dc3-center-*``) re-declares
a ``spring.rabbitmq`` block instead of inheriting the SDK base; (c) a driver
module lacks its own ``application.yml``; or (d) ``dc3-common-driver`` ships a
plain ``application.yml``, which Spring Boot fully shadows with the driver's
own ``application.yml`` (first classpath match wins) and therefore can never
act as a shared carrier.
"""

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DRIVER_ROOT = REPO_ROOT / "dc3-driver"
CENTER_ROOT = REPO_ROOT / "dc3-center"
SDK_CARRIER = REPO_ROOT / "dc3-mq" / "dc3-mq-rabbitmq" / "src" / "main" / "resources" / "application-rabbitmq.yml"
DRIVER_SDK_RESOURCES = REPO_ROOT / "dc3-common" / "dc3-common-driver" / "src" / "main" / "resources"

CONNECTION_KEYS = ("virtual-host", "host", "port", "username", "password")
PLACEHOLDER = re.compile(r"\$\{RABBITMQ_[A-Z_]+:")
TOP_LEVEL_KEY = re.compile(r"^[^\s#-]")


def driver_modules() -> list[Path]:
    return sorted(
        child for child in DRIVER_ROOT.iterdir() if child.is_dir() and child.name.startswith("dc3-driver-")
    )


def spring_rabbitmq_lines(yml_path: Path) -> list[str]:
    """Return the lines of the ``rabbitmq`` child block of the ``spring`` section."""

    lines = yml_path.read_text(encoding="utf-8").splitlines()
    tops = [index for index, line in enumerate(lines) if TOP_LEVEL_KEY.match(line)]
    for index, line in enumerate(lines):
        if line == "spring:" and index in tops:
            section_end = next((top for top in tops if top > index), len(lines))
            section = lines[index + 1 : section_end]
            for position, child in enumerate(section):
                if re.match(r"^\s{2}rabbitmq:", child):
                    block_end = next(
                        (
                            offset
                            for offset in range(position + 1, len(section))
                            if re.match(r"^\s{0,2}\S", section[offset]) and not section[offset].strip().startswith("#")
                        ),
                        len(section),
                    )
                    return section[position:block_end]
    joined = [line for line in lines if re.match(r"^\s*spring\.rabbitmq\b", line)]
    return joined


def check_sdk_carrier(errors: list[str]) -> None:
    if not SDK_CARRIER.is_file():
        errors.append(f"SDK broker config check failed: missing carrier {SDK_CARRIER.relative_to(REPO_ROOT).as_posix()}")
        return
    block = spring_rabbitmq_lines(SDK_CARRIER)
    for key in CONNECTION_KEYS:
        binding = next((line for line in block if re.match(rf"^\s{{4}}{re.escape(key)}:\s*\$", line)), None)
        if binding is None or not PLACEHOLDER.search(binding):
            errors.append(
                f"{SDK_CARRIER.relative_to(REPO_ROOT).as_posix()}: spring.rabbitmq.{key} must stay bound to a "
                "${RABBITMQ_*:default} placeholder (single source of truth for the broker connection)"
            )


def check_no_module_duplicates(errors: list[str]) -> int:
    scanned = 0
    for root in (DRIVER_ROOT, CENTER_ROOT):
        if not root.is_dir():
            continue
        for yml in sorted(root.glob("*/src/main/resources/application*.yml")):
            scanned += 1
            block = spring_rabbitmq_lines(yml)
            if block:
                errors.append(
                    f"{yml.relative_to(REPO_ROOT).as_posix()}: declares its own spring.rabbitmq block "
                    "(remove it; the dc3-mq-rabbitmq SDK profile file already provides the connection defaults)"
                )
    return scanned


def check_driver_application_yml(modules: list[Path], errors: list[str]) -> None:
    for module in modules:
        if not (module / "src" / "main" / "resources" / "application.yml").is_file():
            errors.append(f"{module.name}: missing src/main/resources/application.yml (driver metadata and profile activation)")


def check_driver_sdk_has_no_plain_application_yml(errors: list[str]) -> None:
    shadowed = DRIVER_SDK_RESOURCES / "application.yml"
    if shadowed.is_file():
        errors.append(
            f"{shadowed.relative_to(REPO_ROOT).as_posix()}: a plain application.yml inside the SDK jar is fully "
            "shadowed by each driver's own application.yml and can never apply; use the application-driver.yml "
            "profile file instead"
        )


def main() -> int:
    modules = driver_modules()
    if not modules:
        print("Driver broker-config check failed: no driver modules found", file=sys.stderr)
        return 1

    errors: list[str] = []
    check_sdk_carrier(errors)
    scanned = check_no_module_duplicates(errors)
    check_driver_application_yml(modules, errors)
    check_driver_sdk_has_no_plain_application_yml(errors)

    if errors:
        print(f"Driver broker-config check failed: {len(errors)} violation(s)", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        print(
            "Broker connection settings belong only in dc3-mq/dc3-mq-rabbitmq/src/main/resources/application-rabbitmq.yml.",
            file=sys.stderr,
        )
        return 1

    print(
        "Driver broker-config check passed: "
        f"rule=single rabbitmq source in dc3-mq-rabbitmq SDK, modules={len(modules)}, scanned-yml={scanned}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
