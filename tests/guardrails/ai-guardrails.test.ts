/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

import { describe, expect, it } from 'vitest';

const root = process.cwd();

function walk(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry);
    const stat = statSync(path);
    if (stat.isDirectory()) return walk(path);
    return [path];
  });
}

function readProjectFile(path: string) {
  return readFileSync(join(root, path), 'utf8');
}

function relativeProjectPath(path: string) {
  return relative(root, path).replaceAll('\\', '/');
}

describe('AI coding guardrails', () => {
  it('does not commit focused or disabled tests', () => {
    const focusedOrDisabledTest = /\b(?:describe|it|test)\.(?:only|skip|todo)\s*\(/;
    const offenders = walk(join(root, 'tests'))
      .filter((path) => /\.(?:test|spec)\.ts$|\.mjs$/.test(path))
      .filter((path) => focusedOrDisabledTest.test(readFileSync(path, 'utf8')))
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it('keeps e2e data dynamic instead of hard-coding business IDs', () => {
    const fixedIdPattern =
      /\b(?:driver|profile|device|point|pointProfile|api|resource|menu|user|role)Id:\s*['"]\d{3,}['"]/;
    const offenders = walk(join(root, 'tests/e2e'))
      .filter((path) => /\.(?:ts|mjs)$/.test(path))
      .filter((path) => fixedIdPattern.test(readFileSync(path, 'utf8')))
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it('keeps the browser sweep entrypoint thin and removes old full-ui aliases', () => {
    const browserSweepEntrypoint = readProjectFile('tests/e2e/browser-sweep.mjs');

    expect(browserSweepEntrypoint.split('\n').length).toBeLessThanOrEqual(40);
    expect(browserSweepEntrypoint).toContain("import './browser-sweep/runner.mjs'");
    expect(existsSync(join(root, 'tests/e2e/full-ui.mjs'))).toBe(false);
  });

  it('exposes the required quality scripts for local and CI use', () => {
    const packageJson = JSON.parse(readProjectFile('package.json')) as { scripts?: Record<string, string> };

    expect(packageJson.scripts).toEqual(
      expect.objectContaining({
        'lint:check': expect.any(String),
        'type-check': expect.any(String),
        test: expect.any(String),
        'test:unit': expect.any(String),
        'test:api': expect.any(String),
        'test:component': expect.any(String),
        'test:guard': expect.any(String),
        'test:impact': expect.any(String),
        'test:ci': expect.any(String),
        'test:e2e': expect.any(String),
        'test:e2e:sweep': expect.any(String),
        build: expect.any(String),
      })
    );
  });

  it('documents the mandatory AI testing policy', () => {
    const policy = readProjectFile('docs/frontend-testing-guardrails.md');

    for (const section of [
      'Required Test Mapping',
      'AI Change Rules',
      'E2E Data Rules',
      'CI Gates',
      'Adding New Features',
    ]) {
      expect(policy).toContain(`## ${section}`);
    }
  });
});
