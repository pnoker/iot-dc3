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

import {domainCases} from './families/domain';
import {conversationCases} from './families/conversation';
import {robustnessCases} from './families/robustness';
import type {DialogueCase} from './types';

/**
 * The complete dialogue corpus shared by the offline deterministic suites and
 * the live evaluation runner. Cases are generated deterministically from the
 * family modules, so counts and ids are stable across runs.
 */

const assemble = (): DialogueCase[] => [...domainCases(), ...conversationCases(), ...robustnessCases()];

/** Every dialogue case in the corpus. */
export const CORPUS: DialogueCase[] = assemble();

export interface CorpusStats {
  total: number;
  multiTurn: number;
  turns: number;
  families: Record<string, number>;
}

/**
 * Summarize corpus shape for reports and self-checks.
 * @param corpus - corpus to summarize
 * @returns counts by family plus totals
 */
export const corpusStats = (corpus: DialogueCase[] = CORPUS): CorpusStats => {
  const families: Record<string, number> = {};
  let turns = 0;
  let multiTurn = 0;
  for (const item of corpus) {
    families[item.family] = (families[item.family] ?? 0) + 1;
    turns += item.turns.length;
    if (item.turns.length > 1) multiTurn += 1;
  }
  return {total: corpus.length, multiTurn, turns, families};
};

/**
 * Find structural problems in the corpus (duplicate ids, empty turns, broken
 * expectations). Used by the corpus self-check suite.
 * @param corpus - corpus to validate
 * @returns human-readable problem descriptions; empty when the corpus is sound
 */
export const validateCorpus = (corpus: DialogueCase[] = CORPUS): string[] => {
  const problems: string[] = [];
  const seen = new Set<string>();
  for (const item of corpus) {
    if (seen.has(item.id)) problems.push(`duplicate id: ${item.id}`);
    seen.add(item.id);
    if (item.turns.length === 0) problems.push(`${item.id}: no turns`);
    if (item.turns.some((turn) => turn === undefined)) problems.push(`${item.id}: undefined turn`);
    const {scenario, scenarioAccept} = item.expectations;
    if (scenarioAccept && !scenarioAccept.includes(scenario)) {
      problems.push(`${item.id}: scenario ${scenario} missing from scenarioAccept`);
    }
    if (item.expectations.strictScenario && scenarioAccept && scenarioAccept.length > 1) {
      problems.push(`${item.id}: strict scenario must accept only ${scenario}`);
    }
  }
  return problems;
};

export type {DialogueCase} from './types';
