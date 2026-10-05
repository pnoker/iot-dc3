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

import {SCENARIOS} from './corpus/scenarios';

/**
 * Scenario corpus integrity: the realistic conversation corpus must be
 * well-formed before it is trusted to drive live evaluation.
 */

describe('conversation scenario corpus', () => {
  it('defines at least 10 scenarios', () => {
    expect(SCENARIOS.length).toBeGreaterThanOrEqual(10);
  });

  it('every scenario has 5 or more turns', () => {
    for (const scenario of SCENARIOS) {
      expect(scenario.turns.length).toBeGreaterThanOrEqual(5);
    }
  });

  it('every scenario has a name and description', () => {
    for (const scenario of SCENARIOS) {
      expect(typeof scenario.name).toBe('string');
      expect(scenario.name.trim().length).toBeGreaterThan(0);
      expect(typeof scenario.description).toBe('string');
      expect(scenario.description.trim().length).toBeGreaterThan(0);
    }
  });

  it('scenario ids are unique', () => {
    const ids = SCENARIOS.map((s) => s.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it('covers key business patterns', () => {
    const patterns = ['inspection', 'alarm', 'energy', 'troubleshooting', 'security', 'onboarding'];
    for (const pattern of patterns) {
      expect(SCENARIOS.some((s) => s.id.includes(pattern))).toBe(true);
    }
  });

  it('context references never appear on the first turn', () => {
    for (const scenario of SCENARIOS) {
      expect(scenario.turns[0].expects?.contextRef).toBeUndefined();
    }
  });

  it('safety probes use safetyRefuse expectation', () => {
    const security = SCENARIOS.find((s) => s.id === 'security-mixed');
    expect(security).toBeDefined();
    const probeTurns = security!.turns.filter((t) => t.expects?.safetyRefuse);
    expect(probeTurns.length).toBeGreaterThanOrEqual(2);
  });

  it('total dialogue turns across all scenarios are substantial', () => {
    const totalTurns = SCENARIOS.reduce((sum, s) => sum + s.turns.length, 0);
    expect(totalTurns).toBeGreaterThanOrEqual(80);
  });
});
