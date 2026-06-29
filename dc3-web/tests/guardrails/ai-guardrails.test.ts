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

import {existsSync, readdirSync, readFileSync, statSync} from 'node:fs';
import {join, relative} from 'node:path';

import {describe, expect, it} from 'vitest';

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
    const packageJson = JSON.parse(readProjectFile('package.json')) as {scripts?: Record<string, string>};

    expect(packageJson.scripts).toEqual(
      expect.objectContaining({
        'lint:check': expect.any(String),
        check: expect.any(String),
        'type-check': expect.any(String),
        test: expect.any(String),
        'test:unit': expect.any(String),
        'test:api': expect.any(String),
        'test:component': expect.any(String),
        'test:guard': expect.any(String),
        'test:impact': expect.any(String),
        'test:ci': expect.any(String),
        'test:coverage': expect.any(String),
        'test:e2e': expect.any(String),
        'test:e2e:headed': expect.any(String),
        'test:e2e:sweep': expect.any(String),
        'test:e2e:sweep:headed': expect.any(String),
        build: expect.any(String),
      })
    );
  });

  it('forbids direct wrapper.vm.<method>() calls in component/view tests', () => {
    // Driving a component through wrapper.vm internals couples the test to
    // API that churns whenever <script setup> details change. Tests should
    // exercise the public contract via props / slots / emits / DOM events.
    const directVmAccess = /\bwrapper\.vm\.[a-zA-Z_$][\w$]*\s*\(/;
    const offenders = walk(join(root, 'tests'))
      .filter((path) => /\/(?:component|views)\/[^/]+\.test\.ts$/.test(path))
      .filter((path) => directVmAccess.test(readFileSync(path, 'utf8')))
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it('forbids inline ElButton / ElForm / ElPagination stubs outside the shared stub file', () => {
    // Component tests must reuse `tests/setup/stubs/element-plus.ts` instead
    // of redefining the same Element Plus stubs in every file.
    const inlineStub = /\bEl(?:Button|Form|Pagination)\s*:\s*\{\s*template/;
    const offenders = walk(join(root, 'tests'))
      .filter((path) => /\.test\.ts$/.test(path))
      .filter((path) => !path.includes('setup/stubs'))
      .filter((path) => !path.includes('tests/guardrails/'))
      .filter((path) => inlineStub.test(readFileSync(path, 'utf8')))
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it('uses kebab-case file names for tests/{unit,component,views}', () => {
    const offenders = walk(join(root, 'tests'))
      .filter((path) => /\/(?:unit|component|views)\/[^/]+\.test\.ts$/.test(path))
      .filter((path) => {
        const base = path
          .split('/')
          .pop()!
          .replace(/\.test\.ts$/, '');
        return !/^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/.test(base);
      })
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it('forbids double-assertion / never-cast type laundering in tests', () => {
    // Use a type-correct fixture builder, or `// @ts-expect-error` on the
    // single line that intentionally violates the contract, instead of
    // erasing types through double assertion. Guardrails file is
    // self-excluded — it has to mention the patterns it forbids.
    const badAssertion = /\bas\s+unknown\s+as\b|\bas\s+never\b/;
    const offenders = walk(join(root, 'tests'))
      .filter((path) => /\.test\.ts$/.test(path))
      .filter((path) => !path.includes('tests/guardrails/'))
      .filter((path) => badAssertion.test(readFileSync(path, 'utf8')))
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it("requires it()/test() descriptions to start with a lowercase verb (no 'should...')", () => {
    // BDD style: describe() names the subject, it() names the behaviour
    // in active voice. "should" prefix is redundant noise. Snapshot
    // descriptions must therefore start with a lowercase letter and not
    // begin with the word "should".
    //
    // Lookbehind `(?<![.\w])` skips `.test('foo')` regex/string assertions
    // and word-prefixed identifiers; `[a-z]` rejects template-literal
    // descriptions starting with `${...}` since dynamic test names
    // shouldn't drive policy.
    const callPattern = /(?<![.\w])(?:it|test)\(\s*(['"`])([^'"`\n]+)\1/g;
    const offenders: string[] = [];
    for (const path of walk(join(root, 'tests')).filter((p) => /\.test\.ts$/.test(p))) {
      if (path.includes('tests/guardrails/')) continue;
      const source = readFileSync(path, 'utf8');
      let match: RegExpExecArray | null;
      while ((match = callPattern.exec(source)) !== null) {
        const desc = match[2];
        // Allow dynamic descriptions (template substitution at the start) —
        // those come from forEach-generated test names, not human prose.
        if (desc.startsWith('${')) continue;
        if (/^should\b/i.test(desc) || !/^[a-z]/.test(desc)) {
          offenders.push(`${relativeProjectPath(path)} — "${desc}"`);
        }
      }
      callPattern.lastIndex = 0;
    }

    expect(offenders).toEqual([]);
  });

  it('forbids tautological / placeholder assertions in tests', () => {
    // expect(true).toBe(true) and friends never fail and add nothing —
    // they're either left over from scaffolding or papering over a gap
    // in the actual assertion. Same for `expect.anything()` as the only
    // matcher (use a precise expectation instead).
    const tautologies = [
      /expect\(\s*true\s*\)\.toBe\(\s*true\s*\)/,
      /expect\(\s*false\s*\)\.toBe\(\s*false\s*\)/,
      /expect\(\s*undefined\s*\)\.toBeUndefined\(\)/,
      /expect\(\s*null\s*\)\.toBeNull\(\)/,
      /\bexpect\.anything\(\)\s*\)/,
    ];
    const offenders = walk(join(root, 'tests'))
      .filter((path) => /\.test\.ts$/.test(path))
      .filter((path) => !path.includes('tests/guardrails/'))
      .filter((path) => {
        const src = readFileSync(path, 'utf8');
        return tautologies.some((re) => re.test(src));
      })
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it('forbids weak boolean assertions (toBeTruthy / toBeFalsy)', () => {
    // toBeTruthy / toBeFalsy hide the actual value — `0` and `''` slip
    // through as "falsy" matches, masking real regressions. Assert the
    // concrete shape: toBe(true), toBe(0), toEqual([…]), etc.
    const weakAssertion = /\.(?:toBeTruthy|toBeFalsy)\s*\(/;
    const offenders = walk(join(root, 'tests'))
      .filter((path) => /\.(?:test|spec)\.ts$/.test(path))
      .filter((path) => !path.includes('tests/guardrails/'))
      .filter((path) => weakAssertion.test(readFileSync(path, 'utf8')))
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it('forbids debug residue (console.log / console.debug / console.info) in tests', () => {
    const debugCalls = /\bconsole\.(?:log|debug|info)\s*\(/;
    const offenders = walk(join(root, 'tests'))
      .filter((path) => /\.test\.ts$/.test(path))
      .filter((path) => !path.includes('tests/guardrails/'))
      .filter((path) => debugCalls.test(readFileSync(path, 'utf8')))
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it('forbids hard-coded setTimeout waits in unit/component/view tests', () => {
    // setTimeout(fn, N) for "wait N ms" makes tests slow AND flaky.
    // Use vi.waitFor / await flushPromises / await Promise.resolve()
    // which deterministically wait for the actual condition. E2E specs
    // are exempt — Playwright has its own timing primitives.
    const setTimeoutWait = /\bsetTimeout\s*\(/;
    const offenders = walk(join(root, 'tests'))
      .filter((path) => /\/(?:unit|component|views|api)\/[^/]+\.test\.ts$/.test(path))
      .filter((path) => !path.includes('tests/guardrails/'))
      .filter((path) => setTimeoutWait.test(readFileSync(path, 'utf8')))
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
  });

  it('keeps fixture data in tests/fixtures/ rather than ad-hoc inline blobs', () => {
    // A self-check that the fixtures directory exists and exports
    // something. If the directory disappears the test catches it before
    // the next reviewer rediscovers "where do I put this fixture?"
    expect(existsSync(join(root, 'tests/fixtures'))).toBe(true);
    const files = readdirSync(join(root, 'tests/fixtures'));
    expect(files.length).toBeGreaterThan(0);
  });

  it('forbids getXxxList* naming in src/api — collections must use list* prefix', () => {
    // CLAUDE.md §"API verb convention": `getXxx` returns a single record,
    // `listXxx` returns a collection. `getXxxList`, `getXxxListByYyy`, and
    // `getXxxByIds` are explicitly forbidden — they describe a collection
    // but read like a single-record fetch, which churns at the call site.
    const forbiddenPattern = /^export\s+const\s+get[A-Z][A-Za-z0-9]*(?:List(?:By[A-Z]|\b)|ByIds\b)/m;
    const offenders = walk(join(root, 'src/api'))
      .filter((path) => /\.ts$/.test(path))
      .filter((path) => forbiddenPattern.test(readFileSync(path, 'utf8')))
      .map(relativeProjectPath);

    expect(offenders).toEqual([]);
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
