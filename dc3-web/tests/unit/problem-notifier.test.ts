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

import {describe, expect, it, vi, beforeEach} from 'vitest';

import {notifyAggregatedFailure, resetFailureAggregator} from '@/utils/problemNotifier';

const lastCall = () => notify.mock.calls[notify.mock.calls.length - 1] as [string, string, unknown?];
const notify = vi.fn();

describe('problemNotifier aggregation', () => {
  beforeEach(() => {
    resetFailureAggregator();
    notify.mockClear();
  });

  it('notifies once for a burst of identical errors', () => {
    for (let i = 0; i < 20; i++) {
      notifyAggregatedFailure('network', notify, 'Network error', 'Unable to reach the server');
    }
    expect(notify).toHaveBeenCalledTimes(1);
  });

  it('counts suppressed repeats and appends them after the window expires', () => {
    vi.useFakeTimers();
    try {
      // Wave 1: 1 shown, 4 suppressed inside the window.
      for (let i = 0; i < 5; i++) {
        notifyAggregatedFailure('network', notify, 'Network error', 'Unable to reach the server');
      }
      expect(notify).toHaveBeenCalledTimes(1);
      expect(lastCall()[0]).not.toMatch(/suppressed|抑制/);

      // Past the window the next identical failure notifies again, carrying
      // the suppressed count from the previous window.
      vi.advanceTimersByTime(6000);
      notifyAggregatedFailure('network', notify, 'Network error', 'Unable to reach the server');
      expect(notify).toHaveBeenCalledTimes(2);
      expect(lastCall()[0]).toMatch(/suppressed|抑制/);
    } finally {
      vi.useRealTimers();
    }
  });

  it('keeps distinct error keys separate', () => {
    notifyAggregatedFailure('network', notify, 'Network error', 'msg');
    notifyAggregatedFailure('http-500', notify, 'Server error', 'msg');
    notifyAggregatedFailure('code-R4041', notify, 'Request error', 'msg');
    expect(notify).toHaveBeenCalledTimes(3);
  });

  it('caps simultaneous distinct errors at three and counts the overflow', () => {
    notifyAggregatedFailure('k1', notify, 'T', 'm');
    notifyAggregatedFailure('k2', notify, 'T', 'm');
    notifyAggregatedFailure('k3', notify, 'T', 'm');
    notifyAggregatedFailure('k4', notify, 'T', 'm');
    expect(notify).toHaveBeenCalledTimes(3);
  });

  it('passes the problem payload through to the toast', () => {
    const problem = {status: 422, code: 'R422', title: 'validation'};
    notifyAggregatedFailure('code-R422', notify, 'Request error', 'R422', problem);
    expect(lastCall()[2]).toBe(problem);
  });
});
