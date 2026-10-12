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

import {nextTick, ref} from 'vue';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {
  derivePageSize,
  observeCardWall,
  useCardWallAuto,
  wallColumnCount,
  wallGhostCount,
} from '@/composables/useCardWallPageSize';
import {
  CARD_WALL_APPLY_MIN_INTERVAL_MS,
  CARD_WALL_PAGE_SIZE_MAX,
  CARD_WALL_RESIZE_DEBOUNCE_MS,
} from '@/config/constant/cardWall';

afterEach(() => {
  vi.unstubAllGlobals();
  vi.useRealTimers();
});

// Type-correct wall fixture: a real element with a writable clientWidth —
// observeCardWall only reads that property and getComputedStyle.
const fakeWall = (clientWidth = 1200) => {
  const el = document.createElement('div');
  let width = clientWidth;
  Object.defineProperty(el, 'clientWidth', {configurable: true, get: () => width});
  return {
    el,
    setWidth: (next: number) => {
      width = next;
    },
  };
};

const stubTracks = (tracks: string) => {
  vi.stubGlobal('getComputedStyle', () => ({gridTemplateColumns: tracks}));
};

describe('derivePageSize', () => {
  it('snaps to whole-row multiples around the 12-record target', () => {
    // cols 1..13 — every value is a multiple of cols, 1/2/3/4/6/12 columns
    // derive exactly 12 so defect-free bands never change behaviour.
    const matrix = [12, 12, 12, 12, 10, 12, 14, 16, 18, 20, 22, 12, 13];
    matrix.forEach((expected, index) => {
      expect(derivePageSize(index + 1), `cols=${index + 1}`).toBe(expected);
      expect((index + 1) > 0 ? expected % (index + 1) : 0).toBe(0);
    });
  });

  it('falls back to the target for an unmeasurable wall', () => {
    expect(derivePageSize(0)).toBe(12);
    expect(derivePageSize(-1)).toBe(12);
  });

  it('honours a numeric target and the hard cap', () => {
    expect(derivePageSize(5, 24)).toBe(25);
    expect(derivePageSize(4, 24)).toBe(24);
    expect(derivePageSize(300)).toBe(CARD_WALL_PAGE_SIZE_MAX);
    expect(derivePageSize(2, 500)).toBe(CARD_WALL_PAGE_SIZE_MAX);
  });
});

describe('wallColumnCount', () => {
  it('counts the resolved auto-fill tracks', () => {
    stubTracks('466px 466px 466px 466px');
    expect(wallColumnCount(fakeWall().el)).toBe(4);
    stubTracks('450.667px 450.667px 450.667px');
    expect(wallColumnCount(fakeWall().el)).toBe(3);
    stubTracks('  100px   100px  ');
    expect(wallColumnCount(fakeWall().el)).toBe(2);
  });

  it('reports 0 for hidden or unmeasurable walls', () => {
    stubTracks('none');
    expect(wallColumnCount(fakeWall().el)).toBe(0);
    stubTracks('');
    expect(wallColumnCount(fakeWall().el)).toBe(0);
  });
});

describe('wallGhostCount', () => {
  it('fills the remainder of the last row', () => {
    expect(wallGhostCount(5, 12)).toBe(3); // 5+5+2 → 3 ghosts
    expect(wallGhostCount(4, 2)).toBe(2);
    expect(wallGhostCount(5, 10)).toBe(0); // full rows stay clean
    expect(wallGhostCount(7, 7)).toBe(0);
    expect(wallGhostCount(5, 0)).toBe(0);
    expect(wallGhostCount(0, 5)).toBe(0);
  });
});

// Controllable ResizeObserver: the shared test-setup mock never fires, so
// observation tests need one whose callback they can trigger.
class ControllableResizeObserver {
  static instances: ControllableResizeObserver[] = [];
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();

  constructor(private readonly callback: () => void) {
    ControllableResizeObserver.instances.push(this);
  }

  trigger() {
    this.callback();
  }

  static reset() {
    ControllableResizeObserver.instances = [];
  }

  static get last() {
    return ControllableResizeObserver.instances[ControllableResizeObserver.instances.length - 1];
  }
}

describe('observeCardWall', () => {
  const installTimers = () => vi.useFakeTimers({toFake: ['setTimeout', 'clearTimeout', 'Date']});

  it('measures synchronously on observe and skips height-only entries', () => {
    ControllableResizeObserver.reset();
    vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
    installTimers();
    stubTracks('1px 1px 1px');
    const apply = vi.fn();
    const {el, setWidth} = fakeWall(1200);

    const stop = observeCardWall(el, apply);
    expect(apply).toHaveBeenCalledTimes(1);
    expect(apply).toHaveBeenCalledWith(3);

    // Same width — a data-driven height change must not re-page.
    ControllableResizeObserver.last.trigger();
    expect(apply).toHaveBeenCalledTimes(1);

    // Past the apply cooldown, a real width change re-pages.
    vi.advanceTimersByTime(CARD_WALL_APPLY_MIN_INTERVAL_MS);
    setWidth(1600);
    stubTracks('1px 1px 1px 1px');
    ControllableResizeObserver.last.trigger();
    expect(apply).toHaveBeenCalledTimes(1); // debounced
    vi.advanceTimersByTime(CARD_WALL_RESIZE_DEBOUNCE_MS);
    expect(apply).toHaveBeenCalledTimes(2);
    expect(apply).toHaveBeenLastCalledWith(4);
    stop();
  });

  it('coalesces a resize storm into one settled apply', () => {
    ControllableResizeObserver.reset();
    vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
    installTimers();
    stubTracks('1px 1px');
    const apply = vi.fn();
    const {el, setWidth} = fakeWall(1000);

    observeCardWall(el, apply);
    apply.mockClear();
    vi.advanceTimersByTime(CARD_WALL_APPLY_MIN_INTERVAL_MS);

    // Storm: five width steps inside the debounce window, dragging across
    // a column band (2 → 3 tracks) — one settled apply, never five.
    for (const step of [1100, 1200, 1300, 1400, 1500]) {
      setWidth(step);
      stubTracks(step >= 1400 ? '1px 1px 1px' : '1px 1px');
      ControllableResizeObserver.last.trigger();
    }
    vi.advanceTimersByTime(CARD_WALL_RESIZE_DEBOUNCE_MS);
    expect(apply).toHaveBeenCalledTimes(1);
    expect(apply).toHaveBeenLastCalledWith(3);
  });

  it('bounds successive applies by the minimum interval', () => {
    ControllableResizeObserver.reset();
    vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
    installTimers();
    stubTracks('1px');
    const apply = vi.fn();
    const {el, setWidth} = fakeWall(600);

    observeCardWall(el, apply);
    apply.mockClear();
    vi.advanceTimersByTime(CARD_WALL_APPLY_MIN_INTERVAL_MS);

    // First band change settles at cooldown+debounce.
    setWidth(1200);
    stubTracks('1px 1px 1px');
    ControllableResizeObserver.last.trigger();
    vi.advanceTimersByTime(CARD_WALL_RESIZE_DEBOUNCE_MS);
    expect(apply).toHaveBeenCalledTimes(1);

    // Second band change immediately afterwards is deferred past the
    // cooldown and lands as a trailing apply.
    setWidth(1900);
    stubTracks('1px 1px 1px 1px');
    ControllableResizeObserver.last.trigger();
    vi.advanceTimersByTime(CARD_WALL_RESIZE_DEBOUNCE_MS);
    expect(apply).toHaveBeenCalledTimes(1);
    vi.advanceTimersByTime(CARD_WALL_APPLY_MIN_INTERVAL_MS);
    expect(apply).toHaveBeenCalledTimes(2);
    expect(apply).toHaveBeenLastCalledWith(4);
  });

  it('stops cleanly', () => {
    ControllableResizeObserver.reset();
    vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
    installTimers();
    stubTracks('1px 1px');
    const apply = vi.fn();
    const {el, setWidth} = fakeWall();

    const stop = observeCardWall(el, apply);
    apply.mockClear();
    stop();

    setWidth(2000);
    ControllableResizeObserver.last.trigger();
    vi.advanceTimersByTime(CARD_WALL_APPLY_MIN_INTERVAL_MS + CARD_WALL_RESIZE_DEBOUNCE_MS * 4);
    expect(apply).not.toHaveBeenCalled();
    expect(ControllableResizeObserver.last.disconnect).toHaveBeenCalled();
  });
});

describe('useCardWallAuto', () => {
  const mountAuto = (currentSize = 12, listLength = 12) => {
    const applySize = vi.fn();
    const requestLoad = vi.fn();
    const manager = useCardWallAuto({
      getCurrentSize: () => currentSize,
      applySize,
      requestLoad,
      listLength: () => listLength,
    });
    return {manager, applySize, requestLoad};
  };

  it('gates the first load until the wall settles a size', async () => {
    ControllableResizeObserver.reset();
    vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
    stubTracks('1px 1px 1px 1px 1px'); // 5 cols → derived 10
    const {manager, applySize, requestLoad} = mountAuto(12);

    // Gated from creation — load() before bindWall waits too; the lazy
    // backstop guarantees resolution at the fallback size if no wall comes.
    expect(manager.pending.value).toBe(true);
    const wallRef = ref<HTMLElement | null>(null);
    manager.bindWall(wallRef);
    expect(manager.pending.value).toBe(true);

    wallRef.value = fakeWall().el;
    await nextTick();
    expect(manager.pending.value).toBe(false);
    expect(applySize).toHaveBeenCalledWith(10, {preservePosition: true});
    expect(requestLoad).not.toHaveBeenCalled(); // the sized load replaces it
  });

  it('issues a plain first load when the derived size equals the current one', async () => {
    ControllableResizeObserver.reset();
    vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
    stubTracks('1px 1px 1px 1px'); // 4 cols → derived 12
    const {manager, applySize, requestLoad} = mountAuto(12);

    const wallRef = ref<HTMLElement | null>(null);
    manager.bindWall(wallRef);
    wallRef.value = fakeWall().el;
    await nextTick();

    expect(applySize).not.toHaveBeenCalled();
    expect(requestLoad).toHaveBeenCalledTimes(1);
  });

  it('derives the fallback size for a hidden wall', async () => {
    ControllableResizeObserver.reset();
    vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
    stubTracks('none');
    const {manager, applySize, requestLoad} = mountAuto(12);

    const wallRef = ref<HTMLElement | null>(null);
    manager.bindWall(wallRef);
    wallRef.value = fakeWall().el;
    await nextTick();

    expect(applySize).not.toHaveBeenCalled();
    expect(requestLoad).toHaveBeenCalledTimes(1);
    expect(manager.cols.value).toBe(0);
  });

  it('measures for ghost counts but never applies while manual', async () => {
    ControllableResizeObserver.reset();
    vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
    stubTracks('1px 1px 1px 1px 1px'); // 5 cols → derived 10
    let currentSize = 12;
    const applySize = vi.fn(async (size: number) => {
      currentSize = size;
    });
    const manager = useCardWallAuto({
      getCurrentSize: () => currentSize,
      applySize,
      requestLoad: vi.fn(),
      listLength: () => 2,
    });

    const wallRef = ref<HTMLElement | null>(null);
    manager.bindWall(wallRef);
    const {el, setWidth} = fakeWall();
    wallRef.value = el;
    await nextTick();
    expect(currentSize).toBe(10); // first resolution settled at derived 10
    applySize.mockClear();

    manager.setManual(24);
    expect(applySize).toHaveBeenCalledWith(24, {preservePosition: false});
    expect(currentSize).toBe(24);
    expect(manager.sizeAuto.value).toBe(false);
    expect(manager.ghostCount.value).toBe(3); // 5 cols, 2 rendered → 3 ghosts

    // Further band changes must not fight the explicit choice. The new band
    // derives 14 (7 cols), which differs from the pinned 24 — dropping the
    // manual guard would visibly re-apply here — and the window advances
    // past both the resize debounce and the apply cooldown so a deferred
    // apply has every chance to fire.
    stubTracks('1px 1px 1px 1px 1px 1px 1px');
    vi.useFakeTimers({toFake: ['setTimeout', 'clearTimeout', 'Date']});
    setWidth(1800);
    ControllableResizeObserver.last.trigger();
    vi.advanceTimersByTime(CARD_WALL_APPLY_MIN_INTERVAL_MS + CARD_WALL_RESIZE_DEBOUNCE_MS + 100);
    expect(applySize).toHaveBeenCalledTimes(1); // only the explicit 24
    expect(currentSize).toBe(24);
    // Ghost counts still follow the measured band while manual.
    expect(manager.cols.value).toBe(7);
    expect(manager.ghostCount.value).toBe(5); // 7 cols, 2 rendered → 5 ghosts
  });

  it('re-applies the wall-derived size when auto is re-enabled', async () => {
    ControllableResizeObserver.reset();
    vi.stubGlobal('ResizeObserver', ControllableResizeObserver);
    stubTracks('1px 1px 1px 1px 1px'); // 5 cols → derived 10
    let currentSize = 12;
    const applySize = vi.fn(async (size: number) => {
      currentSize = size;
    });
    const manager = useCardWallAuto({
      getCurrentSize: () => currentSize,
      applySize,
      requestLoad: vi.fn(),
      listLength: () => 12,
    });

    const wallRef = ref<HTMLElement | null>(null);
    manager.bindWall(wallRef);
    wallRef.value = fakeWall().el;
    await nextTick();
    expect(currentSize).toBe(10);

    manager.setManual(24);
    expect(currentSize).toBe(24);

    manager.enableAuto();
    expect(manager.sizeAuto.value).toBe(true);
    expect(currentSize).toBe(10);
    expect(applySize).toHaveBeenLastCalledWith(10, {preservePosition: true});
  });
});
