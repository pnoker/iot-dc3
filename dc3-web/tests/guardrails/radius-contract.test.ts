/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Axiom A5 guardrail: every border-radius must resolve to the
// --dc3-radius-* scale in src/styles/theme.scss (directly or through an
// Element Plus radius variable). Geometric circles (50%/100%), `inherit`
// and single-corner `0` resets are shape semantics, not radii, so they stay
// literal. Hand-rolled px values reintroduce per-component drift one patch
// at a time — the exact debt this contract cleared once.

import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";

import { describe, expect, it } from "vitest";

const root = process.cwd();
const srcDir = join(root, "src");

// Any corner variant: border-radius, border-top-left-radius, ...
const RADIUS_DECL = /border(?:-[a-z]+)*-radius\s*:\s*([^;{}]+?)(?=\s*[;}]|\s*$)/g;

// Whitelisted atoms. Multi-corner shorthands ("0 0 var(--dc3-radius-full) var(--dc3-radius-full)")
// are split on whitespace and every segment must match on its own.
const TOKEN_ATOM = [
  /^var\(--dc3-radius-(?:sm|md|lg|xl|2xl|full)\)$/,
  /^var\(--el-[a-z-]*radius[a-z-]*\)$/,
  /^(?:50%|100%|inherit|0)$/,
];

function stripComments(content: string): string {
  return content
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/(^|\s)\/\/.*$/gm, "$1");
}

function walk(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) return walk(path);
    return /\.(vue|scss)$/.test(path) ? [path] : [];
  });
}

function findViolations(): string[] {
  const violations: string[] = [];
  for (const path of walk(srcDir)) {
    const content = stripComments(readFileSync(path, "utf8"));
    for (const match of content.matchAll(RADIUS_DECL)) {
      const value = match[1].trim();
      const segments = value.split(/\s+/);
      if (segments.every((segment) => TOKEN_ATOM.some((atom) => atom.test(segment)))) continue;
      const line = content.slice(0, match.index).split("\n").length;
      violations.push(
        `${relative(root, path)}:${line} — border-radius "${value}" is not a radius token`,
      );
    }
  }
  return violations;
}

describe("radius scale contract (A5)", () => {
  it("all border-radius declarations resolve to --dc3-radius-* or shape literals", () => {
    const violations = findViolations();
    expect(violations, violations.join("\n")).toEqual([]);
  });

  it("keeps the Element Plus radius mapping as the single source of truth", () => {
    const theme = readFileSync(join(srcDir, "styles/theme.scss"), "utf8");
    // :root variable-level mapping (see comments there for why these win).
    expect(theme).toContain("--el-border-radius-base: var(--dc3-radius-md)");
    expect(theme).toContain("--el-popover-border-radius: var(--dc3-radius-lg)");
    // Dead variable Element Plus never consumes — must not come back.
    expect(theme).not.toContain("--el-popper-radius");
  });
});
