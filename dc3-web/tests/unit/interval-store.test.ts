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
