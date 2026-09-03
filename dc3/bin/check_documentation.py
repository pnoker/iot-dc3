#!/usr/bin/env python3
"""Validate repository documentation against executable sources of truth."""

# Copyright 2016-present the IoT DC3 original author or authors.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program. If not, see <https://www.gnu.org/licenses/>.

from __future__ import annotations

import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from functools import cache
from pathlib import Path
from urllib.parse import unquote


ROOT = Path(__file__).resolve().parents[2]
MAVEN_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
ROOT_READMES = (
    "README.md",
    "README.zh.md",
    "README.es.md",
    "README.ja.md",
    "README.ko.md",
    "README.ru.md",
    "README.vi.md",
    "README.ai.md",
)
VENDORED_JAVA_ROOT = Path(
    "dc3-driver/dc3-driver-opc-da/src/main/java/org/openscada"
)
DOCUMENTED_JAVA_API_ROOTS = (
    Path("dc3-common/dc3-common-constant/src/main/java"),
    Path("dc3-common/dc3-common-public/src/main/java"),
)


class DocumentationErrors:
    """Collect all failures so one run reports every actionable drift."""

    def __init__(self) -> None:
        self.messages: list[str] = []

    def require(self, condition: bool, message: str) -> None:
        if not condition:
            self.messages.append(message)

    def fail(self, message: str) -> None:
        self.messages.append(message)


def read(relative_path: str | Path) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


@cache
def repository_files() -> tuple[Path, ...]:
    files: list[Path] = []
    ignored_directories = {
        ".git",
        "__pycache__",
        "coverage",
        "dist",
        "node_modules",
        "playwright-report",
        "target",
        "test-results",
    }
    for directory, child_directories, file_names in os.walk(ROOT):
        child_directories[:] = [
            name for name in child_directories if name not in ignored_directories
        ]
        files.extend(Path(directory) / name for name in file_names)
    return tuple(files)


def validate_module_readmes(errors: DocumentationErrors) -> None:
    for pom_path in sorted(path for path in repository_files() if path.name == "pom.xml"):

        readme_path = pom_path.parent / "README.md"
        errors.require(
            readme_path.is_file(),
            f"{relative(pom_path.parent)}: Maven module is missing README.md",
        )
        if not readme_path.is_file() or pom_path == ROOT / "pom.xml":
            continue

        artifact_id = ET.parse(pom_path).getroot().findtext(
            "m:artifactId", namespaces=MAVEN_NAMESPACE
        )
        errors.require(
            bool(artifact_id and artifact_id in readme_path.read_text(encoding="utf-8")),
            f"{relative(readme_path)}: must identify module artifactId {artifact_id!r}",
        )


def validate_driver_count(errors: DocumentationErrors) -> None:
    driver_pom = ET.parse(ROOT / "dc3-driver/pom.xml").getroot()
    modules = [
        element.text
        for element in driver_pom.findall("m:modules/m:module", MAVEN_NAMESPACE)
        if element.text and element.text.startswith("dc3-driver-")
    ]
    count = len(modules)

    for readme in ROOT_READMES:
        content = read(readme)
        errors.require(
            str(count) in content,
            f"{readme}: documented driver count must match dc3-driver/pom.xml ({count})",
        )

    english_driver_section = read("README.md").split(
        "### 🔌 Multi-Protocol Device Connectivity", 1
    )[1].split("### 🤖", 1)[0]
    driver_rows = [
        line
        for line in english_driver_section.splitlines()
        if line.startswith("|") and "**" in line and "·" in line
    ]
    documented_drivers = [
        name.strip()
        for line in driver_rows
        for name in line.split("|", 2)[2].rsplit("|", 1)[0].split("·")
        if name.strip()
    ]
    errors.require(
        len(documented_drivers) == count,
        f"README.md: driver coverage table must list {count} entries, found {len(documented_drivers)}",
    )

    ai_driver_section = read("README.ai.md").split("## Driver Coverage", 1)[1].split(
        "## Architecture in One Paragraph", 1
    )[0]
    ai_rows = [
        line
        for line in ai_driver_section.splitlines()
        if line.startswith("|") and line.count("|") >= 3 and "," in line
    ]
    documented_ai_drivers = [
        name.strip()
        for line in ai_rows
        for name in line.split("|", 2)[2].rsplit("|", 1)[0].split(",")
        if name.strip()
    ]
    errors.require(
        len(documented_ai_drivers) == count,
        f"README.ai.md: driver coverage table must list {count} entries, found {len(documented_ai_drivers)}",
    )


def validate_web_metadata(errors: DocumentationErrors) -> None:
    package = json.loads(read("dc3-web/package.json"))
    package_manager = package.get("packageManager", "")
    match = re.fullmatch(r"pnpm@(.+)", package_manager)
    errors.require(bool(match), "dc3-web/package.json: packageManager must pin pnpm")
    if match:
        dockerfile = read("dc3-web/Dockerfile")
        errors.require(
            f"corepack prepare pnpm@{match.group(1)} --activate" in dockerfile,
            "dc3-web/Dockerfile: pnpm pin must match package.json packageManager",
        )

    errors.require(
        package.get("license") == "AGPL-3.0-or-later",
        "dc3-web/package.json: license must match the repository AGPL-3.0-or-later license",
    )


def validate_published_images(errors: DocumentationErrors) -> None:
    workflow = read(".github/workflows/docker-ci.yml")
    service_block_match = re.search(
        r"services=\(\s*(.*?)\s*\)", workflow, flags=re.DOTALL
    )
    errors.require(
        bool(service_block_match),
        ".github/workflows/docker-ci.yml: could not find the backend services array",
    )
    if not service_block_match:
        return

    published = set(re.findall(r"^\s*(dc3-(?:gateway|center|driver)[a-z0-9-]*)\s*$", service_block_match.group(1), re.MULTILINE))
    documented = set(re.findall(r"pnoker/(dc3-(?:gateway|center|driver)[a-z0-9-]*):", read("dc3/doc/USAGE.md")))
    errors.require(
        documented == published,
        "dc3/doc/USAGE.md: published image table differs from docker-ci.yml "
        f"(missing={sorted(published - documented)}, extra={sorted(documented - published)})",
    )


def validate_proto_readmes(errors: DocumentationErrors) -> None:
    for api_dir in sorted((ROOT / "dc3-api").glob("dc3-api-*")):
        readme_path = api_dir / "README.md"
        documented = readme_path.read_text(encoding="utf-8")
        rpc_names: set[str] = set()
        for proto_path in api_dir.glob("src/main/protobuf/**/*.proto"):
            rpc_names.update(re.findall(r"\brpc\s+(\w+)\s*\(", proto_path.read_text(encoding="utf-8")))
        for rpc_name in sorted(rpc_names):
            errors.require(
                f"`{rpc_name}`" in documented,
                f"{relative(readme_path)}: missing RPC {rpc_name} declared by protobuf",
            )


def validate_local_markdown_links(errors: DocumentationErrors) -> None:
    link_pattern = re.compile(r"!?\[[^\]]*]\(([^)\s]+)(?:\s+[^)]*)?\)")
    ignored_schemes = ("http://", "https://", "mailto:", "app://", "data:")

    for markdown_path in sorted(path for path in repository_files() if path.suffix == ".md"):
        content = markdown_path.read_text(encoding="utf-8")
        for match in link_pattern.finditer(content):
            target_text = unquote(match.group(1).strip("<>"))
            if not target_text or target_text.startswith(("#", *ignored_schemes)):
                continue
            path_text = target_text.split("#", 1)[0]
            target_path = (
                ROOT / path_text.lstrip("/")
                if path_text.startswith("/")
                else markdown_path.parent / path_text
            )
            errors.require(
                target_path.exists(),
                f"{relative(markdown_path)}: broken local link {target_text!r}",
            )


def validate_stable_javadocs(errors: DocumentationErrors) -> None:
    for java_path in sorted(
        path
        for path in repository_files()
        if path.suffix == ".java" and "src" in path.parts and "main" in path.parts
    ):
        relative_path = java_path.relative_to(ROOT)
        if relative_path.is_relative_to(VENDORED_JAVA_ROOT):
            continue
        content = java_path.read_text(encoding="utf-8")
        if re.search(r"^\s*\*\s*@version\b", content, re.MULTILINE):
            errors.fail(
                f"{relative(java_path)}: remove volatile @version metadata; use @since only for stable API history"
            )
        if "@Getter(onMethod_" in content:
            errors.fail(
                f"{relative(java_path)}: field-level Lombok onMethod injection breaks source Javadoc; "
                "let the compiler verify the generated accessor contract"
            )


def validate_public_type_javadocs(errors: DocumentationErrors) -> None:
    modifiers = r"(?:(?:abstract|final|non-sealed|sealed|static)\s+)*"
    type_kind = r"(?:class|interface|enum|record|@interface)"

    for java_path in sorted(
        path
        for path in repository_files()
        if path.suffix == ".java"
        and any(
            path.relative_to(ROOT).is_relative_to(api_root)
            for api_root in DOCUMENTED_JAVA_API_ROOTS
        )
    ):
        relative_path = java_path.relative_to(ROOT)
        if relative_path.is_relative_to(VENDORED_JAVA_ROOT):
            continue
        if java_path.name in {"module-info.java", "package-info.java"}:
            continue

        content = java_path.read_text(encoding="utf-8")
        declaration = re.search(
            rf"^\s*public\s+{modifiers}{type_kind}\s+{re.escape(java_path.stem)}\b",
            content,
            re.MULTILINE,
        )
        if not declaration:
            continue

        lines = content[: declaration.start()].splitlines()
        index = len(lines) - 1
        while index >= 0 and not lines[index].strip():
            index -= 1

        # Skip annotations between the type Javadoc and declaration, including
        # multiline annotation arguments.
        while index >= 0:
            stripped = lines[index].strip()
            if stripped.startswith("@"):
                index -= 1
            elif stripped.endswith(")"):
                while index >= 0 and not lines[index].strip().startswith("@"):
                    index -= 1
                index -= 1
            else:
                break
            while index >= 0 and not lines[index].strip():
                index -= 1

        has_javadoc = index >= 0 and lines[index].strip().endswith("*/")
        if has_javadoc:
            has_javadoc = any(
                line.strip().startswith("/**") for line in reversed(lines[: index + 1])
            )
        errors.require(
            has_javadoc,
            f"{relative(java_path)}: public top-level type is missing Javadoc",
        )


def _annotation_block_start(lines: list[str], declaration_index: int) -> int:
    """Return the first line of the annotation block before a declaration."""
    index = declaration_index - 1
    while index >= 0 and not lines[index].strip():
        index -= 1

    start = declaration_index
    while index >= 0:
        stripped = lines[index].strip()
        if not (stripped.startswith("@") or stripped.endswith((")", "})", "))"))):
            break

        parenthesis_depth = 0
        annotation_start = -1
        while index >= 0:
            candidate = lines[index].strip()
            parenthesis_depth += candidate.count(")") - candidate.count("(")
            if candidate.startswith("@") and parenthesis_depth <= 0:
                annotation_start = index
                break
            index -= 1
        if annotation_start < 0:
            break
        start = annotation_start
        index = annotation_start - 1
        while index >= 0 and not lines[index].strip():
            index -= 1
    return start


def _javadoc_before(lines: list[str], declaration_index: int) -> str:
    """Return the Javadoc immediately attached to a declaration, if present."""
    end_index = _annotation_block_start(lines, declaration_index) - 1
    while end_index >= 0 and not lines[end_index].strip():
        end_index -= 1
    if end_index < 0 or not lines[end_index].strip().endswith("*/"):
        return ""
    start_index = end_index
    while start_index >= 0 and not lines[start_index].strip().startswith("/**"):
        if lines[start_index].strip().startswith("/*"):
            return ""
        start_index -= 1
    return "\n".join(lines[start_index : end_index + 1]) if start_index >= 0 else ""


def _is_method_declaration(
    lines: list[str], line_index: int, implicit_interface_method: bool
) -> tuple[bool, str]:
    """Recognize repository-style method declarations without parsing method bodies."""
    stripped = lines[line_index].strip()
    has_explicit_visibility = bool(re.match(r"(?:public|protected)\b", stripped))
    if not has_explicit_visibility and not implicit_interface_method:
        return False, ""
    if implicit_interface_method and not re.match(
        r"(?:(?:default|static|abstract)\s+)*(?:<[^;{}]+>\s+)?"
        r"[A-Za-z_$@][\w$@.<>, ?\[\]]*\s+[A-Za-z_$][\w$]*\s*\(",
        stripped,
    ):
        return False, ""
    if implicit_interface_method and re.match(
        r"(?:return|throw|yield|case|new|if|for|while|switch|try|catch)\b",
        stripped,
    ):
        return False, ""
    if re.match(
        r"(?:public|protected)\s+(?:(?:abstract|final|non-sealed|sealed|static)\s+)*"
        r"(?:class|interface|enum|record|@interface)\b",
        stripped,
    ):
        return False, ""

    signature_lines: list[str] = []
    for candidate in lines[line_index : line_index + 20]:
        signature_lines.append(candidate.strip())
        if "{" in candidate or ";" in candidate:
            break
    signature = " ".join(signature_lines)
    open_parenthesis = signature.find("(")
    if open_parenthesis < 0:
        return False, ""
    prefix = signature[:open_parenthesis]
    if "=" in prefix or " -> " in signature:
        return False, ""

    name_match = re.search(r"([A-Za-z_$][\w$]*)\s*$", prefix)
    if not name_match:
        return False, ""
    method_name = name_match.group(1)
    before_name = prefix[: name_match.start()].strip()
    before_name = re.sub(
        r"^(?:(?:public|protected|private|static|final|synchronized|native|abstract|"
        r"default|strictfp)\b\s*)+",
        "",
        before_name,
    )
    before_name = re.sub(r"^<[^>]+>\s*", "", before_name)
    if not before_name:
        # Constructors are not methods and are outside this rule.
        return False, ""
    return True, method_name


def validate_public_method_javadocs(errors: DocumentationErrors) -> None:
    """Require stable contracts on project-owned non-override public methods."""
    placeholder_phrases = (
        "the requested value",
        "whether the documented condition holds",
    )
    for java_path in sorted(
        path
        for path in repository_files()
        if path.suffix == ".java"
        and any(
            path.relative_to(ROOT).is_relative_to(api_root)
            for api_root in DOCUMENTED_JAVA_API_ROOTS
        )
    ):
        relative_path = java_path.relative_to(ROOT)
        if relative_path.is_relative_to(VENDORED_JAVA_ROOT):
            continue
        lines = java_path.read_text(encoding="utf-8").splitlines()
        content = "\n".join(lines)
        is_interface = bool(
            re.search(
                r"^\s*public\s+(?:(?:sealed|non-sealed)\s+)?interface\s+",
                content,
                re.MULTILINE,
            )
        )
        for line_index, line in enumerate(lines):
            stripped = line.strip()
            implicit_interface_method = is_interface and not re.match(
                r"(?:public|protected|private)\b", stripped
            )
            is_method, method_name = _is_method_declaration(
                lines, line_index, implicit_interface_method
            )
            if not is_method:
                continue
            if method_name == java_path.stem:
                continue

            annotation_start = _annotation_block_start(lines, line_index)
            annotations = "\n".join(lines[annotation_start:line_index])
            if re.search(r"@(?:java\.lang\.)?Override\b", annotations):
                continue
            javadoc = _javadoc_before(lines, line_index)
            errors.require(
                bool(javadoc),
                f"{relative(java_path)}:{line_index + 1}: public method "
                f"{method_name} is missing Javadoc",
            )
            if javadoc:
                for phrase in placeholder_phrases:
                    errors.require(
                        phrase not in javadoc,
                        f"{relative(java_path)}:{line_index + 1}: public method "
                        f"{method_name} uses placeholder Javadoc {phrase!r}",
                    )


def validate_comment_language(errors: DocumentationErrors) -> None:
    source_extensions = {".css", ".java", ".js", ".py", ".scss", ".ts", ".tsx", ".vue"}
    # Python's standard regex engine has no Unicode script properties.
    han_character = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")

    for source_path in sorted(repository_files()):
        if source_path.suffix not in source_extensions:
            continue
        relative_path = source_path.relative_to(ROOT)
        if relative_path.is_relative_to(VENDORED_JAVA_ROOT):
            continue
        try:
            lines = source_path.read_text(encoding="utf-8").splitlines()
        except FileNotFoundError:
            # A concurrently running frontend check may replace generated files;
            # generated directories are pruned above, but tolerate the same race
            # for any tool-specific output directory not yet known here.
            continue
        for line_number, line in enumerate(lines, 1):
            if not han_character.search(line):
                continue
            stripped = line.lstrip()
            is_comment = stripped.startswith(("//", "/*", "*", "<!--", "# "))
            is_inline_comment = bool(re.search(r"\s//", line))
            if is_comment or is_inline_comment:
                errors.fail(
                    f"{relative(source_path)}:{line_number}: keep code comments in English"
                )


def validate_documented_commands(errors: DocumentationErrors) -> None:
    for markdown_path in sorted(path for path in repository_files() if path.suffix == ".md"):
        content = markdown_path.read_text(encoding="utf-8")
        for obsolete in ("make compose-", "make ci"):
            errors.require(
                obsolete not in content,
                f"{relative(markdown_path)}: obsolete command prefix {obsolete!r}",
            )


def main() -> int:
    errors = DocumentationErrors()
    validate_module_readmes(errors)
    validate_driver_count(errors)
    validate_web_metadata(errors)
    validate_published_images(errors)
    validate_proto_readmes(errors)
    validate_local_markdown_links(errors)
    validate_stable_javadocs(errors)
    validate_public_type_javadocs(errors)
    validate_public_method_javadocs(errors)
    validate_comment_language(errors)
    validate_documented_commands(errors)

    if errors.messages:
        print("Documentation validation failed:", file=sys.stderr)
        for message in errors.messages:
            print(f"- {message}", file=sys.stderr)
        return 1

    print("Documentation validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
