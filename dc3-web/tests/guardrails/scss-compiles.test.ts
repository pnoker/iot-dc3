/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Blind-spot guardrail: vitest's jsdom environment never compiles SFC
// `<style lang="scss">` blocks, so SASS syntax damage ships green tests and
// only explodes as a dev-server 500 overlay in the browser. This suite
// compiles every SCSS block the way vite does (element-variables injection +
// `@/` alias) and fails on the first syntax error.

import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, resolve } from "node:path";

import * as sass from "sass";
import { describe, expect, it } from "vitest";

const srcRoot = resolve(process.cwd(), "src");

const toFileUrl = (path: string) => new URL(`file:///${path.split("\\").join("/")}`);

const aliasImporter = {
  findFileUrl(url: string) {
    if (!url.startsWith("@/")) return null;
    return toFileUrl(join(srcRoot, url.slice(2)));
  },
};

const walk = (dir: string): string[] =>
  readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry);
    return statSync(path).isDirectory() ? walk(path) : /\.(vue|scss)$/.test(entry) ? [path] : [];
  });

interface StyleBlock {
  file: string;
  open: number;
  body: string;
}

const collectBlocks = (): StyleBlock[] => {
  const blocks: StyleBlock[] = [];
  for (const file of walk(srcRoot)) {
    const text = readFileSync(file, "utf8");
    if (file.endsWith(".scss")) {
      blocks.push({ file, open: 1, body: text });
      continue;
    }
    for (const match of text.matchAll(/<style([^>]*)>([\s\S]*?)<\/style>/g)) {
      if (!/lang=["']scss["']/.test(match[1])) continue;
      const open = text.slice(0, match.index).split("\n").length;
      blocks.push({ file, open, body: match[2] });
    }
  }
  return blocks;
};

describe("SCSS compile contract", () => {
  it("every SCSS block in src/ compiles with the vite injection and alias", () => {
    const failures: string[] = [];
    for (const block of collectBlocks()) {
      // Mirror vite's additionalData — skipped for the injection chain itself
      // (element-variables.scss forwards tokens.scss, so either as a root
      // would self-loop under the injection).
      const inject = /element-variables\.scss$|[\\/]styles[\\/]tokens\.scss$/.test(block.file)
        ? ""
        : '@use "@/config/plugins/element/element-variables.scss" as *;\n';
      try {
        sass.compileString(inject + block.body, {
          importers: [aliasImporter],
          url: toFileUrl(block.file),
        });
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        failures.push(`${block.file}:${block.open} — ${message.split("\n")[0]}`);
      }
    }
    expect(failures, failures.join("\n")).toEqual([]);
  });
});
