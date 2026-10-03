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

import {afterAll, describe, expect, it} from 'vitest';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';

import {CORPUS, corpusStats} from './corpus';
import type {ScenarioKey} from './corpus/types';
import {containsCjk, runStreaming} from './helpers/chatHarness';
import type {FamilyMetric} from './helpers/report';
import {renderOfflineReport, writeJsonReport, writeMarkdownReport} from './helpers/report';

/**
 * Reply-quality tier against the scripted engine: topic-routing fidelity for
 * every utterance (strict where the wording carries engine keywords, tolerant
 * elsewhere), reply-language correctness per mock locale, and structural
 * richness (charts / trace events) for domain topics. Metrics feed the offline
 * dialogue report; strict failures fail the build, tolerant mismatches count as
 * routing gaps in the report.
 */

const SCENARIO_MARKERS: Array<[ScenarioKey, RegExp]> = [
  ['temperature', /85\.4/],
  ['energy', /340\.9/],
  ['driver', /S7 PLC/],
  ['point', /24\.5/],
  ['dashboard', /16 台设备|16 assets/],
];

/**
 * Identify which canned scenario answered a reply.
 * @param content - assistant reply text
 * @returns the detected scenario key, or undefined when no marker matched
 */
export const detectScenario = (content: string): ScenarioKey | undefined =>
  SCENARIO_MARKERS.find(([, marker]) => marker.test(content))?.[0];

const metrics = new Map<string, FamilyMetric>();
const fallbackExamples: string[] = [];

const metricOf = (family: string): FamilyMetric => {
  const existing = metrics.get(family);
  if (existing) return existing;
  const created: FamilyMetric = {
    family,
    cases: 0,
    idealRoutes: 0,
    acceptableRoutes: 0,
    fallbackRoutes: 0,
    languagePass: 0,
    structurePass: 0,
  };
  metrics.set(family, created);
  return created;
};

describe('dialogue reply quality · routing and language', () => {
  it.each(CORPUS.map((item) => [item.id, item] as const))('%s answers on the right topic', async (_id, item) => {
    const outcome = await runStreaming(item, item.turns[item.turns.length - 1]);
    const metric = metricOf(item.family);
    metric.cases += 1;

    expect(outcome.error).toBeUndefined();
    const content = outcome.content;
    const detected = detectScenario(content);
    const {scenario, scenarioAccept, strictScenario, language} = item.expectations;
    const acceptable = scenarioAccept ?? [scenario];

    if (detected === scenario) metric.idealRoutes += 1;
    else if (detected === 'dashboard') metric.fallbackRoutes += 1;
    else if (detected && acceptable.includes(detected)) metric.acceptableRoutes += 1;
    if (detected && detected !== scenario && detected === 'dashboard' && fallbackExamples.length < 40) {
      fallbackExamples.push(`${item.id}: ${JSON.stringify(item.turns[item.turns.length - 1].slice(0, 60))}`);
    }

    // Reply language follows the mock locale pipeline (zh reply ↔ CJK text).
    const languageOk = language === 'zh' ? containsCjk(content) : !containsCjk(content);
    if (languageOk) metric.languagePass += 1;

    // Domain topics must arrive with charts and tool/reasoning traces.
    const structureOk =
      (!item.expectations.charts || outcome.visualizations.length > 0) &&
      (!item.expectations.events || outcome.events.length > 0);
    if (structureOk) metric.structurePass += 1;

    if (strictScenario) {
      expect(detected).toBe(scenario);
    } else {
      expect(acceptable).toContain(detected ?? 'dashboard');
    }
    expect(languageOk).toBe(true);
    expect(structureOk).toBe(true);
  });
});

afterAll(() => {
  const ordered = [...metrics.values()].sort((a, b) => a.family.localeCompare(b.family));
  const contract = {streamPass: 0, streamFail: 0, blockingPass: 0, blockingFail: 0, p95Ms: 0};
  writeJsonReport('routing-metrics', {
    generated: new Date().toISOString(),
    families: ordered,
    fallbackExamples,
  });

  // The contract suite writes its own metrics; pick them up when present so the
  // combined report carries both tiers (it may run in another worker).
  let contractWithFile = contract;
  try {
    const dir = join(dirname(fileURLToPath(import.meta.url)), 'reports');
    contractWithFile = JSON.parse(readFileSync(join(dir, 'contract-metrics.json'), 'utf-8'));
  } catch {
    /* contract tier not run (or not finished) in this invocation */
  }

  writeMarkdownReport(
    'offline-dialogue-report',
    renderOfflineReport(corpusStats(), ordered, {
      streamPass: contractWithFile.streamPass,
      streamFail: contractWithFile.streamFail,
      blockingPass: contractWithFile.blockingPass,
      blockingFail: contractWithFile.blockingFail,
      p95Ms: contractWithFile.p95Ms ?? 0,
    })
  );
});
