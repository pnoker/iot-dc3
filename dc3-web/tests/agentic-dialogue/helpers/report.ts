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

import {mkdirSync, writeFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';

/**
 * Report helpers for the dialogue suites. Each suite writes its own JSON file
 * (no cross-suite write races) and the routing suite renders the combined
 * Markdown report that becomes the human-readable deliverable.
 */

const reportsDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'reports');

/**
 * Write a JSON report artifact.
 * @param name - file base name without extension
 * @param payload - JSON-serializable payload
 * @returns the written file path
 */
export const writeJsonReport = (name: string, payload: unknown): string => {
  mkdirSync(reportsDir, {recursive: true});
  const target = join(reportsDir, `${name}.json`);
  writeFileSync(target, `${JSON.stringify(payload, null, 2)}\n`, 'utf-8');
  return target;
};

/**
 * Write the combined Markdown dialogue report.
 * @param name - file base name without extension
 * @param markdown - report body
 * @returns the written file path
 */
export const writeMarkdownReport = (name: string, markdown: string): string => {
  mkdirSync(reportsDir, {recursive: true});
  const target = join(reportsDir, `${name}.md`);
  writeFileSync(target, `${markdown}\n`, 'utf-8');
  return target;
};

export interface FamilyMetric {
  family: string;
  cases: number;
  idealRoutes: number;
  acceptableRoutes: number;
  fallbackRoutes: number;
  languagePass: number;
  structurePass: number;
}

/**
 * Render the offline dialogue Markdown report.
 * @param stats - corpus statistics
 * @param metrics - per-family routing/language/structure metrics
 * @param contract - contract-tier summary
 * @returns markdown body
 */
export const renderOfflineReport = (
  stats: {total: number; multiTurn: number; turns: number; families: Record<string, number>},
  metrics: FamilyMetric[],
  contract: {streamPass: number; streamFail: number; blockingPass: number; blockingFail: number; p95Ms: number}
): string => {
  const ideal = metrics.reduce((sum, m) => sum + m.idealRoutes, 0);
  const acceptable = metrics.reduce((sum, m) => sum + m.acceptableRoutes, 0);
  const fallback = metrics.reduce((sum, m) => sum + m.fallbackRoutes, 0);
  const pct = (value: number, total: number) => (total === 0 ? 'n/a' : `${((value / total) * 100).toFixed(1)}%`);
  const lines: string[] = [
    '# Agentic Assistant Dialogue Test Report (offline)',
    '',
    `Generated: ${new Date().toISOString()}`,
    '',
    '## Corpus',
    '',
    `- Dialogue cases: **${stats.total}** (target ≥ 1000)`,
    `- Multi-turn scripts: **${stats.multiTurn}**, total turns: **${stats.turns}**`,
    `- Families: **${Object.keys(stats.families).length}**`,
    '',
    '## Contract tier (every case, streaming + blocking)',
    '',
    `- Streaming pass/fail: **${contract.streamPass}/${contract.streamFail}**`,
    `- Blocking pass/fail: **${contract.blockingPass}/${contract.blockingFail}**`,
    `- Streaming p95 latency: **${contract.p95Ms} ms** (mock engine, includes SSE parse)`,
    '',
    '## Topic-routing fidelity (mock engine)',
    '',
    `- Ideal-route fidelity: **${ideal}/${metrics.reduce((s, m) => s + m.cases, 0)} (${pct(
      ideal,
      metrics.reduce((s, m) => s + m.cases, 0)
    )})**`,
    `- Acceptable-route rate: **${acceptable}**, fallback-to-summary rate: **${fallback}**`,
    '',
    '| Family | Cases | Ideal route | Acceptable | Fallback | Language OK | Structure OK |',
    '|---|---:|---:|---:|---:|---:|---:|',
    ...metrics.map(
      (m) =>
        `| ${m.family} | ${m.cases} | ${m.idealRoutes} | ${m.acceptableRoutes} | ${m.fallbackRoutes} | ${m.languagePass} | ${m.structurePass} |`
    ),
    '',
    '## How to read this',
    '',
    '- **Contract tier** is the hard gate: every utterance must terminate cleanly,',
    '  produce a parseable SSE stream, and a non-empty structured reply.',
    '- **Ideal route** counts cases answered by the topic-matched scenario;',
    '  **Fallback** counts cases answered by the generic platform summary. Fallback',
    '  rows on keyword-bearing families are routing-coverage gaps worth fixing in',
    '  the engine (or prompts, on the live backend).',
    '- Live LLM evaluation is a separate report (`live-eval`), produced by the',
    '  env-gated runner against a real backend and model.',
  ];
  return lines.join('\n');
};
