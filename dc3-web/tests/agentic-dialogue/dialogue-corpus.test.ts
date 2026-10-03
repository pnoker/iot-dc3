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

import {describe, expect, it} from 'vitest';

import {CORPUS, corpusStats, validateCorpus} from './corpus';

/**
 * Corpus self-check: the dialogue suite is only as good as the corpus, so its
 * shape is asserted before any engine runs — size, unique ids, family spread,
 * and expectation consistency.
 */

describe('dialogue corpus integrity', () => {
  const stats = corpusStats();

  it('holds at least 1000 distinct dialogue cases', () => {
    expect(CORPUS.length).toBeGreaterThanOrEqual(1000);
  });

  it('has no structural problems', () => {
    expect(validateCorpus()).toEqual([]);
  });

  it('uses unique case ids', () => {
    expect(new Set(CORPUS.map((item) => item.id)).size).toBe(CORPUS.length);
  });

  it('covers every expected family group', () => {
    const families = Object.keys(stats.families);
    for (const group of ['domain-', 'conv-', 'edge-']) {
      expect(families.some((family) => family.startsWith(group))).toBe(true);
    }
    expect(families.length).toBeGreaterThanOrEqual(25);
  });

  it('includes multi-turn dialogue scripts', () => {
    expect(stats.multiTurn).toBeGreaterThanOrEqual(80);
    expect(Math.max(...CORPUS.map((item) => item.turns.length))).toBeGreaterThanOrEqual(3);
  });

  it('covers Chinese, English, and mixed-language phrasings', () => {
    const languages = new Set(CORPUS.map((item) => item.language));
    expect(languages.has('zh')).toBe(true);
    expect(languages.has('en')).toBe(true);
    expect(languages.has('mixed')).toBe(true);
  });

  it('carries live-eval expectations on every case', () => {
    for (const item of CORPUS) {
      expect(Array.isArray(item.expectations.live.keywords)).toBe(true);
    }
  });

  it('keeps strict routing cases free of competing expectations', () => {
    const strict = CORPUS.filter((item) => item.expectations.strictScenario);
    expect(strict.length).toBeGreaterThanOrEqual(200);
    for (const item of strict) {
      expect(item.expectations.scenarioAccept).toEqual([item.expectations.scenario]);
    }
  });
});
