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

import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';

import {useIntervalStore} from '@/store'; // Arbitrary non-zero handles. Names express ordering rather than the

// Arbitrary non-zero handles. Names express ordering rather than the
// values themselves so the assertions stay readable.
const FIRST_TIMER_ID = 101;
const SECOND_TIMER_ID = 202;

describe('interval store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('stores the latest interval id without clearing when none is active', () => {
    const clearSpy = vi.spyOn(globalThis, 'clearInterval');
    const store = useIntervalStore();

    expect(store.pointValueInterval).toBeNull();
    store.clearPointValueInterval(FIRST_TIMER_ID);

    expect(clearSpy).not.toHaveBeenCalled();
    expect(store.pointValueInterval).toBe(FIRST_TIMER_ID);
  });

  it('clears the previous interval before recording a new one', () => {
    const clearSpy = vi.spyOn(globalThis, 'clearInterval');
    const store = useIntervalStore();

    store.clearPointValueInterval(FIRST_TIMER_ID);
    store.clearPointValueInterval(SECOND_TIMER_ID);

    // Replacing the first id with the second must call clearInterval on the
    // first — otherwise the old polling timer keeps firing in parallel and
    // PointValue page double-fetches every tick.
    expect(clearSpy).toHaveBeenCalledTimes(1);
    expect(clearSpy).toHaveBeenCalledWith(FIRST_TIMER_ID);
    expect(store.pointValueInterval).toBe(SECOND_TIMER_ID);
  });
});
