/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import {defaultScenario, pickScenario, scenarios} from '@/mock/fetch';

/**
 * Scenario routing is declared through the priority field — array order
 * must not decide which scenario wins when several regexes match.
 */
describe('pickScenario', () => {
  const byPriority = (priority: number) => scenarios.find((scenario) => scenario.priority === priority)!;

  it('routes a prompt mixing point and temperature keywords to the point scenario', () => {
    // The prompt matches both the temperature regex (priority 20) and the
    // point regex (priority 10); the lower priority number must win
    // regardless of array order.
    expect(pickScenario('温度位号现在是多少')).toBe(byPriority(10));
  });

  it('routes temperature-only prompts to the temperature scenario', () => {
    expect(pickScenario('风机温度趋势如何')).toBe(byPriority(20));
  });

  it('routes energy prompts to the energy scenario', () => {
    expect(pickScenario('近一周能耗怎么样')).toBe(byPriority(30));
  });

  it('routes driver prompts to the driver scenario', () => {
    expect(pickScenario('采集驱动负载高吗')).toBe(byPriority(40));
  });

  it('falls back to the default scenario when nothing matches', () => {
    expect(pickScenario('随便聊聊天气')).toBe(defaultScenario);
  });
});
