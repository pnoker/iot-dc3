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

// Card-wall auto paging contract (A5). The wall grid derives its column
// count from repeat(auto-fill, minmax(min(NNNpx,100%),1fr)) at runtime
// (useCardWallPageSize reads the browser-resolved tracks — the wall's
// minmax floor is deliberately NOT duplicated here as a TS constant), and
// the list page size snaps to whole-row multiples of that count so a full
// page never ends ragged. Keep in sync with the guardrail contract test
// tests/guardrails/card-wall-contract.test.ts.

/** Target page size the row-multiple rounding aims at. */
export const CARD_WALL_TARGET_PAGE_SIZE = 12;

/** Full pages hold at least this many rows (never a 1-row sliver wall). */
export const CARD_WALL_MIN_ROWS = 2;

/** Mirror of dc3-db PageRequest.MAX_LIMIT and the mock readPage cap. */
export const CARD_WALL_PAGE_SIZE_MAX = 200;

/** Trailing debounce for resize-driven re-derivation. */
export const CARD_WALL_RESIZE_DEBOUNCE_MS = 300;

/** Minimum gap between two applied size changes (resize storm bound). */
export const CARD_WALL_APPLY_MIN_INTERVAL_MS = 800;

/** Backstop resolving the first load if the wall element never arrives. */
export const CARD_WALL_RESOLVE_BACKSTOP_MS = 100;
