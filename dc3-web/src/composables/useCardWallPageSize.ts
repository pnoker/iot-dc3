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

// Wall-driven page sizing for the entity card walls (.entity-card-wall).
// The wall's column count changes continuously with the available width
// (repeat(auto-fill, minmax(min(NNNpx,100%),1fr))), so the list page size
// is derived from the browser-resolved grid tracks and snapped to a whole
// row multiple of the column count: full pages always fill complete rows.
// The residual (last page / small datasets) is covered by ghost slots
// (CardWallGhosts), so a wall never shows an accidental hole.

import {computed, getCurrentInstance, onScopeDispose, ref, watch, type ComputedRef, type Ref} from 'vue';

import {
  CARD_WALL_APPLY_MIN_INTERVAL_MS,
  CARD_WALL_MIN_ROWS,
  CARD_WALL_PAGE_SIZE_MAX,
  CARD_WALL_RESIZE_DEBOUNCE_MS,
  CARD_WALL_RESOLVE_BACKSTOP_MS,
  CARD_WALL_TARGET_PAGE_SIZE,
} from '@/config/constant/cardWall';

/**
 * Page size closest to the target that fills whole wall rows.
 *
 * Row count is the nearest multiple of `cols` to `target`, clamped to a
 * 2-row floor (a one-row wall reads as a sliver) and to the ceiling that
 * still fits the target; a single row is allowed when the target itself
 * fits one row (cols >= target). An unmeasurable wall (`cols <= 0`) falls
 * back to the target so the first load never waits on layout.
 *
 * @param cols wall column count (0 when hidden/unmeasured)
 * @param target density target in records (default CARD_WALL_TARGET_PAGE_SIZE)
 * @param max hard cap mirroring the backend limit (default CARD_WALL_PAGE_SIZE_MAX)
 * @returns the derived page size
 */
export const derivePageSize = (
  cols: number,
  target: number = CARD_WALL_TARGET_PAGE_SIZE,
  max: number = CARD_WALL_PAGE_SIZE_MAX,
): number => {
  if (cols <= 0) return Math.min(target, max);
  const ceiling = Math.max(1, Math.ceil(target / cols));
  const floorRows = Math.min(CARD_WALL_MIN_ROWS, ceiling);
  const rows = Math.min(Math.max(Math.round(target / cols), floorRows), ceiling);
  return Math.min(max, rows * cols);
};

/**
 * Column count of a resolved auto-fill grid.
 *
 * Reads the browser's own `gridTemplateColumns` resolution instead of
 * replaying the minmax() formula in JS, so the derived page size can never
 * drift from the wall that is actually painted (fractional dock/aside
 * widths included).
 *
 * @param el the .entity-card-wall element
 * @returns track count, or 0 when hidden/unmeasurable
 */
export const wallColumnCount = (el: HTMLElement): number => {
  const tracks = getComputedStyle(el).gridTemplateColumns;
  if (!tracks || tracks === 'none') return 0;
  const parts = tracks.trim().split(/\s+/).filter(Boolean);
  return parts.length;
};

/**
 * Ghost slot count completing the wall's last row.
 *
 * Whole-row multiples make full pages ragged-free; the final page holds
 * whatever the dataset ends with, and the empty tracks of its last row are
 * filled with decorative ghost slots so the wall always reads complete.
 *
 * @param cols wall column count
 * @param listLength records rendered on the current page
 * @returns ghost count in [0, cols)
 */
export const wallGhostCount = (cols: number, listLength: number): number => {
  if (cols <= 0 || listLength <= 0) return 0;
  return (cols - (listLength % cols)) % cols;
};

/**
 * Wall observation options.
 */
export interface CardWallObserveOptions {
  /** Raw column-count listener (ghost counts track this even in manual mode). */
  onCols?: (cols: number) => void;
  /** Trailing debounce for resize-driven runs. */
  debounceMs?: number;
  /** Minimum interval between applied runs. */
  minIntervalMs?: number;
}

// The pager is an active interaction: re-paging under the user's cursor
// while they read or click through it would yank the list away. Runs are
// deferred while focus lives in the pager footer and retried afterwards.
const PAGER_FOOTER_SELECTOR = '.tool-card-footer-page';

const pagerFocused = (): boolean => {
  const active = document.activeElement as HTMLElement | null;
  return Boolean(active?.closest(PAGER_FOOTER_SELECTOR));
};

/**
 * Observe a card wall and report its column count.
 *
 * The first measurement runs synchronously so the initial page size is
 * settled before the first request matters; later measurements are width-
 * filtered (height-only growth from data must not re-page), debounced,
 * bounded by a minimum apply interval, and deferred while the pager is
 * focused. When ResizeObserver is unavailable (test environments) only the
 * synchronous first measurement runs.
 *
 * @param el the .entity-card-wall element
 * @param apply column-count sink for settled measurements
 * @param options observation options
 * @returns a stop handle clearing timers and the observer
 */
export const observeCardWall = (
  el: HTMLElement,
  apply: (cols: number) => void,
  options: CardWallObserveOptions = {},
): (() => void) => {
  const {
    onCols,
    debounceMs = CARD_WALL_RESIZE_DEBOUNCE_MS,
    minIntervalMs = CARD_WALL_APPLY_MIN_INTERVAL_MS,
  } = options;

  let width = el.clientWidth;
  let appliedCols = 0;
  let lastApplyAt = 0;
  let timer: ReturnType<typeof setTimeout> | undefined;
  let retryTimer: ReturnType<typeof setTimeout> | undefined;
  let stopped = false;

  const applyNow = (cols: number) => {
    if (stopped) return;
    appliedCols = cols;
    lastApplyAt = Date.now();
    apply(cols);
  };

  const run = (initial: boolean) => {
    if (stopped) return;
    const cols = wallColumnCount(el);
    onCols?.(cols);
    if (initial) {
      // First resolution must always land — never deferred, never gated.
      applyNow(cols);
      return;
    }
    if (cols <= 0 || cols === appliedCols) return;
    if (pagerFocused()) {
      clearTimeout(retryTimer);
      retryTimer = setTimeout(() => run(false), debounceMs);
      return;
    }
    const since = Date.now() - lastApplyAt;
    if (since < minIntervalMs) {
      clearTimeout(timer);
      timer = setTimeout(() => run(false), minIntervalMs - since);
      return;
    }
    applyNow(cols);
  };

  const scheduleRun = () => {
    clearTimeout(timer);
    timer = setTimeout(() => run(false), debounceMs);
  };

  const observer =
    typeof ResizeObserver === 'undefined'
      ? null
      : new ResizeObserver(() => {
        const nextWidth = el.clientWidth;
        // Height-only entries (data growth, skeleton swaps) must not re-page.
        if (nextWidth === width) return;
        width = nextWidth;
        scheduleRun();
      });

  observer?.observe(el);
  run(true);

  return () => {
    stopped = true;
    clearTimeout(timer);
    clearTimeout(retryTimer);
    observer?.disconnect();
  };
};

/**
 * Card-wall auto paging options.
 */
export interface CardWallAutoOptions {
  /** Density target in records (page size snaps around it). */
  target?: number;
  /** Loads wait for the first wall measurement (set on wall-managed lists). */
  gateLoads?: boolean;
  /** Current page size of the owning list. */
  getCurrentSize: () => number;
  /** Apply a derived size (change + reload is the callee's concern). */
  applySize: (size: number, ctx: {preservePosition: boolean}) => void | Promise<void>;
  /** First-resolution load used when the derived size equals the current one. */
  requestLoad: () => void | Promise<void>;
  /** Records rendered on the current page (drives ghost slots). */
  listLength: () => number;
}

/**
 * Card-wall auto paging handle.
 */
export interface CardWallAuto {
  /** Attach the wall element (one watcher per owner scope). */
  bindWall: (wallRef: Ref<HTMLElement | null | undefined>) => void;
  /** Re-enter auto mode and apply the size the wall currently asks for. */
  enableAuto: () => void;
  /** Leave auto mode and apply an explicit page size (resets to page 1). */
  setManual: (size: number) => void;
  /** Auto mode flag — mirrored onto the page contract for ToolCard. */
  sizeAuto: Ref<boolean>;
  /** True while the first measurement is still pending (gates the first load). */
  pending: ComputedRef<boolean>;
  /** Arm the resolve backstop so a gated load can never hang the list. */
  armBackstop: () => void;
  /** Last measured column count (0 = unmeasured). */
  cols: Ref<number>;
  /** Ghost slots completing the wall's last row right now. */
  ghostCount: ComputedRef<number>;
}

/**
 * Wall-driven page-size manager shared by the paged-list composable and
 * bespoke list machines (PointValue's cursor paging).
 *
 * Owns the auto/manual mode, the resize observation and the first-load
 * gate: the owner's setup-time load is a no-op until the wall settles a
 * page size, so exactly one correctly-sized request is issued per mount
 * (at the fallback size when the wall is hidden or unmeasurable).
 *
 * @param options size application hooks of the owning list
 * @returns the composable handle
 */
export const useCardWallAuto = (options: CardWallAutoOptions): CardWallAuto => {
  const target = options.target ?? CARD_WALL_TARGET_PAGE_SIZE;
  const gateLoads = options.gateLoads ?? true;
  const sizeAuto = ref(true);
  const cols = ref(0);
  const resolved = ref(false);

  let stopObserve: (() => void) | null = null;
  let unwatch: (() => void) | null = null;
  let backstop: ReturnType<typeof setTimeout> | undefined;

  const pending = computed(() => gateLoads && !resolved.value);

  const ghostCount = computed(() => wallGhostCount(cols.value, options.listLength()));

  const settle = (nextCols: number) => {
    cols.value = nextCols;
  };

  const onMeasure = (nextCols: number) => {
    settle(nextCols);
    if (!sizeAuto.value) return;
    const derived = derivePageSize(nextCols, target);
    if (!resolved.value) {
      resolved.value = true;
      if (derived !== options.getCurrentSize()) {
        void options.applySize(derived, {preservePosition: true});
      } else {
        void options.requestLoad();
      }
      return;
    }
    if (derived !== options.getCurrentSize()) {
      void options.applySize(derived, {preservePosition: true});
    }
  };

  const bindWall = (wallRef: Ref<HTMLElement | null | undefined>) => {
    unwatch = watch(
      wallRef,
      (el) => {
        stopObserve?.();
        stopObserve = null;
        if (!el) return;
        stopObserve = observeCardWall(el, onMeasure, {
          onCols: settle,
        });
      },
      {immediate: true, flush: 'post'},
    );
  };

  // Backstop: a wall that never mounts (or a template that forgets the
  // ref) must not leave the list empty — resolve at the fallback size.
  // Armed lazily by the first gated load so plain callers never schedule
  // one, and idempotent so the guarantee holds from the first wait on.
  const armBackstop = () => {
    if (backstop || resolved.value) return;
    backstop = setTimeout(() => {
      backstop = undefined;
      if (resolved.value) return;
      resolved.value = true;
      void options.requestLoad();
    }, CARD_WALL_RESOLVE_BACKSTOP_MS);
  };

  const enableAuto = () => {
    sizeAuto.value = true;
    const derived = derivePageSize(cols.value, target);
    if (derived !== options.getCurrentSize()) {
      void options.applySize(derived, {preservePosition: true});
    }
  };

  const setManual = (size: number) => {
    sizeAuto.value = false;
    void options.applySize(size, {preservePosition: false});
  };

  // Match usePagedList: plain-callers (unit tests, stores) own their own
  // cleanup; only component scopes register disposal.
  if (getCurrentInstance()) {
    onScopeDispose(() => {
      unwatch?.();
      unwatch = null;
      stopObserve?.();
      stopObserve = null;
      clearTimeout(backstop);
    });
  }

  return {
    bindWall,
    enableAuto,
    setManual,
    sizeAuto,
    pending,
    armBackstop,
    cols,
    ghostCount,
  };
};
