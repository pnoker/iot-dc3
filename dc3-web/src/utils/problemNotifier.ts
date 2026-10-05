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

import i18n from '@/config/i18n';

/**
 * Failure-notification aggregator: bursting request failures (a dev stack
 * without its backend, a gateway restart, a polling loop erroring every
 * interval) must not stack one toast per request.
 *
 * Rules:
 * - identical error keys (problem code / HTTP status / network) notify at
 *   most once per throttle window; repeats are silently counted and the
 *   count is appended to the next notification for that key;
 * - at most {@link MAX_VISIBLE} distinct error keys notify at the same
 *   time, so a multi-fault burst still fits on screen.
 */
const THROTTLE_WINDOW_MS = 5000;
const MAX_VISIBLE = 3;

type Notify = (message: string, title: string, problem?: unknown) => void;

type KeyState = {
  lastShownAt: number;
  suppressed: number;
};

const keyStates = new Map<string, KeyState>();

/**
 * Render one aggregated failure notification.
 *
 * @param key stable error identity (problem code, HTTP status, or 'network')
 * @param notify toast callback (title-first signature of notificationUtil)
 * @param title toast title
 * @param message toast message
 * @param problem raw problem payload passed through to the toast detail
 */
export const notifyAggregatedFailure = (
    key: string,
    notify: Notify,
    title: string,
    message: string,
    problem?: unknown
): void => {
  const now = Date.now();
  const state = keyStates.get(key);
  if (state && now - state.lastShownAt < THROTTLE_WINDOW_MS) {
    state.suppressed += 1;
    return;
  }
  const suppressed = state?.suppressed ?? 0;
  if (state) keyStates.delete(key);
  if (keyStates.size >= MAX_VISIBLE) {
    // Screen budget exhausted by other distinct keys: count this one too.
    const budget = keyStates.values().next().value as KeyState | undefined;
    if (budget) budget.suppressed += 1;
    return;
  }
  keyStates.set(key, {lastShownAt: now, suppressed: 0});
  const suffix = suppressed
    ? ` ${i18n.global.t('common.axios.suppressedSuffix', {n: suppressed})}`
    : '';
  notify(`${message}${suffix}`, title, problem);
};

/** Test seam: reset aggregator state between cases. */
export const resetFailureAggregator = (): void => {
  keyStates.clear();
};
