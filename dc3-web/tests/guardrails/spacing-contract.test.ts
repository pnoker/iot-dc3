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

// Axiom A5 guardrail: macro spacing (page edges, card walls, section
// rhythm) must resolve to the --dc3-space-* scale in src/styles/theme.scss,
// expressed either directly or through the semantic tokens
// --dc3-page-padding / --dc3-gutter. Three rule families:
//   Rule 1 — the semantic tokens exist exactly once and are only defined
//            in theme.scss (:root), never redefined per component.
//   Rule 2 — a whitelist of layout-bearing containers may only declare
//            margin/padding/gap with tokens (calc(var(--dc3-…)) allowed).
//            Whitelist selectors must stay direct class names — the
//            selector-anchored regex below does not resolve `&__x` nesting.
//   Rule 3 — anti-patterns banned repo-wide: clamp() in spacing values
//            (fluid edges never align with the tiered rhythm), and el-row
//            :gutter values other than "8" (the JS prop cannot read CSS
//            vars, so the literal must mirror --dc3-gutter's desktop value).
// Intra-component micro spacing (~200 call sites: icon-to-text gaps, chip
// padding) is intentionally out of scope this round.
// Exemptions: a `// spacing-exempt` comment on the offending line.

import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";

import { describe, expect, it } from "vitest";

const root = process.cwd();
const srcDir = join(root, "src");
const themePath = join(srcDir, "styles/theme.scss");

function walk(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) return walk(path);
    return /\.(vue|scss)$/.test(path) ? [path] : [];
  });
}

const files = walk(srcDir);

// Strip comments while keeping a per-line map so violations report real
// line numbers. A `// spacing-exempt` marker is read from the RAW line.
function analyze(path: string): { lines: string[]; exempt: Set<number> } {
  const raw = readFileSync(path, "utf8").split("\n");
  const exempt = new Set<number>();
  const lines = raw.map((line, i) => {
    if (line.includes("spacing-exempt")) exempt.add(i);
    return line.replace(/\/\*[\s\S]*?\*\//g, "").replace(/(^|\s)\/\/.*$/, "$1");
  });
  return { lines, exempt };
}

// Layout-bearing containers whose margin/padding/gap are page-level rhythm.
const WHITELIST = [
  ".body",
  ".things-card",
  ".skeleton-card",
  ".tool-card",
  ".home__row",
  ".home__stats",
  ".home__col",
  ".entity-card-wall",
  ".entity-page-error",
  ".detail-page-alert",
  ".event-overview",
  ".event-overview__cards",
  ".event-overview__charts",
  ".event-overview__diagnostic",
  ".event-overview__grid-1",
  ".event-overview__grid-2",
  ".event-overview__error",
  ".settings-container",
];

const SPACING_PROP = /(?:^|[\s;])(margin|padding)(?:-(?:block|inline|top|right|bottom|left|block-start|block-end|inline-start|inline-end))?|(?:^|[\s;])gap\b|(?:^|[\s;])(?:row|column)-gap\b/;

// Token atoms a spacing value may be built from.
const TOKEN_SEGMENT = [
  /^var\(--dc3-(?:space-[0-9]+|page-padding|gutter)\)$/,
  /^var\(--el-[a-z-]+\)$/,
  /^(?:0|auto|inherit)$/,
];

// A whole value wrapped in calc() that references a dc3 token (the inner
// spaces must not be split into segments).
const TOKEN_CALC = /^calc\([^;]*var\(--dc3-[^;]*\)$/;

// Selector + its same-level declaration body. `[^{}]*` stops at nested
// blocks, so SCSS nesting is safe.
const RULE_BLOCK = /(^|[\s&,.])(\.-?[\w-]+)\s*\{([^{}]*)\}/g;

function findViolations(): string[] {
  const violations: string[] = [];
  for (const path of files) {
    const { lines, exempt } = analyze(path);
    const joined = lines.join("\n");
    const rel = relative(root, path);

    // Rule 1 — semantic tokens are defined only in theme.scss.
    if (path !== themePath) {
      for (const match of joined.matchAll(/--dc3-(?:page-padding|gutter)\s*:/g)) {
        const line = joined.slice(0, match.index).split("\n").length;
        if (exempt.has(line - 1)) continue;
        violations.push(`${rel}:${line} — semantic spacing token must only be defined in theme.scss`);
      }
    }

    // Rule 2 — whitelisted containers carry only token spacing.
    for (const match of joined.matchAll(RULE_BLOCK)) {
      const selector = match[2];
      if (!WHITELIST.includes(selector)) continue;
      const blockStart = joined.slice(0, match.index).split("\n").length;
      for (const decl of match[3].split(";")) {
        if (!SPACING_PROP.test(` ${decl.trim().split(":")[0]} `)) continue;
        const value = (decl.split(":").slice(1).join(":") || "").trim();
        if (!value) continue;
        const segments = value.split(/\s+/);
        if (
          TOKEN_CALC.test(value) ||
          segments.every((segment) => TOKEN_SEGMENT.some((atom) => atom.test(segment)))
        ) {
          continue;
        }
        // Attribute the violation to the block's opening line.
        if (exempt.has(blockStart - 1)) continue;
        violations.push(`${rel}:${blockStart} — ${selector} spacing "${value}" is not a spacing token`);
      }
    }

    // Rule 3a — no clamp() in any spacing value.
    for (const match of joined.matchAll(/(?:margin|padding|gap)[a-z-]*\s*:\s*[^;{]*clamp\(/g)) {
      const line = joined.slice(0, match.index).split("\n").length;
      if (exempt.has(line - 1)) continue;
      violations.push(`${rel}:${line} — clamp() in a spacing value (tiered tokens only)`);
    }

    // Rule 3b — el-row :gutter mirrors the desktop --dc3-gutter (8px).
    if (/\.vue$/.test(path)) {
      for (const match of joined.matchAll(/:gutter="(?!8")\d+"/g)) {
        const line = joined.slice(0, match.index).split("\n").length;
        if (exempt.has(line - 1)) continue;
        violations.push(`${rel}:${line} — :gutter must stay "8" to mirror --dc3-gutter`);
      }
    }
  }
  return violations;
}

describe("spacing scale contract (A5)", () => {
  it("macro spacing resolves to --dc3-space-*/semantic tokens without anti-patterns", () => {
    const violations = findViolations();
    expect(violations, violations.join("\n")).toEqual([]);
  });

  it("keeps the semantic spacing tokens as the single source of truth", () => {
    const theme = readFileSync(themePath, "utf8");
    expect(theme).toContain("--dc3-page-padding: var(--dc3-space-2)");
    expect(theme).toContain("--dc3-gutter: var(--dc3-space-2)");
    expect(theme).toContain("@media (min-width: $breakpoint-sm)");
    expect(theme).toContain("@media (min-width: $breakpoint-lg)");
    expect(theme).toContain("@media (max-width: $breakpoint-xs-max)");
  });
});
