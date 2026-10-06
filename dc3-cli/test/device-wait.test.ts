import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Command } from 'commander';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { registerDeviceCommand, waitForOperation } from '../src/commands/device.js';

type FetchInit = { method?: string; headers?: unknown; body?: unknown };
type FetchCall = { url: string; init: FetchInit; atMs: number };

const fetchCalls: FetchCall[] = [];

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({ gateway: 'http://gw.test/', tenant: 't', username: 'u' })),
    getActiveProfileName: vi.fn(async () => 'default'),
    load: vi.fn(async () => ({ current_profile: 'default' })),
    getSettings: vi.fn(async () => ({ renewal_threshold_hours: 12 })),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    needsRenewal: vi.fn(async () => false),
  },
}));

const ACCEPTED = { operationId: 'op-1', statusUri: '/api/v3/manager/operations/get_by_id?id=op-1' };

function runningOperation(overrides: Record<string, unknown> = {}): Response {
  return new Response(
    JSON.stringify({
      operationId: 'op-1',
      status: 'RUNNING',
      progress: 10,
      result: null,
      error: null,
      createdAt: '1970-01-01T00:00:00Z',
      updatedAt: '1970-01-01T00:00:01Z',
      expiresAt: null,
      ...overrides,
    }),
    { status: 200 },
  );
}

/**
 * Fake the clock at epoch 0. Only setTimeout/clearTimeout/Date are faked so
 * promise microtasks and the libuv thread pool keep running for real: the
 * action startup (commander parse + readFile) needs real event-loop rounds,
 * so tests drive the clock adaptively instead of assuming a fixed startup lag.
 */
function useWaitFakeTimers(): void {
  vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'Date'], now: 0 });
}

/**
 * Advance the faked clock until the condition holds or the budget runs out.
 * @param condition - checked after every step
 * @param maxFakeMs - total fake-millisecond budget
 * @param stepMs - fake-millisecond step per iteration
 * @returns whether the condition eventually held
 */
async function advanceUntil(condition: () => boolean, maxFakeMs: number, stepMs: number): Promise<boolean> {
  for (let advanced = 0; advanced < maxFakeMs && !condition(); advanced += stepMs) {
    await vi.advanceTimersByTimeAsync(stepMs);
  }
  return condition();
}

/** Sentinel while the driven execution has not settled yet. */
const PENDING = Symbol('pending');

/**
 * Advance the faked clock until the execution settles. If it never settles
 * the final await outlives the vitest timeout — which is exactly the
 * regression signal these guard tests exist to raise.
 * @param execution - the promise under test
 * @param maxFakeMs - total fake-millisecond budget
 * @param stepMs - fake-millisecond step per iteration
 * @returns the settled value (resolved value or rejection reason), or undefined
 */
async function driveUntilSettled(execution: Promise<unknown>, maxFakeMs: number, stepMs: number): Promise<unknown> {
  let outcome: unknown = PENDING;
  const watched = execution.then(
    (value) => {
      outcome = { value };
      return value;
    },
    (error) => {
      outcome = { value: error };
      return error;
    },
  );
  for (let advanced = 0; advanced < maxFakeMs && outcome === PENDING; advanced += stepMs) {
    await vi.advanceTimersByTimeAsync(stepMs);
  }
  await watched;
  return outcome === PENDING ? undefined : (outcome as { value: unknown }).value;
}

/**
 * True once the import POST reached the stubbed fetch.
 * @returns whether the import request was issued
 */
function importSubmitted(): boolean {
  return fetchCalls.some((call) => call.url.includes('/device/import'));
}

/**
 * Status-poll timestamps recorded by the fetch stub, normalized to the first poll.
 * @returns per-poll offsets in fake milliseconds
 */
function pollOffsets(): number[] {
  const times = fetchCalls.filter((call) => !call.url.includes('/device/import')).map((call) => call.atMs);
  return times.map((at) => at - times[0]);
}

function buildProgram(): Command {
  const program = new Command();
  program.exitOverride();
  registerDeviceCommand(program);
  return program;
}

function stubAlwaysRunning(expiresAtFromNowMs?: number): void {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init?: FetchInit) => {
      fetchCalls.push({ url, init: init ?? {}, atMs: Date.now() });
      if (String(url).includes('/device/import')) {
        return new Response(JSON.stringify(ACCEPTED), { status: 202 });
      }
      const expiresAt =
        expiresAtFromNowMs === undefined ? null : new Date(Date.now() + expiresAtFromNowMs).toISOString();
      return runningOperation({ expiresAt });
    }),
  );
}

describe('device import wait loop (F010)', () => {
  let directory: string;
  let file: string;

  beforeEach(async () => {
    fetchCalls.length = 0;
    directory = await mkdtemp(join(tmpdir(), 'dc3-cli-wait-'));
    file = join(directory, 'devices.xlsx');
    await writeFile(file, Buffer.from([1, 2, 3, 4]));
  });

  afterEach(async () => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    await rm(directory, { recursive: true, force: true });
  });

  it('ends with a structured timeout error instead of hanging on a never-terminal operation', async () => {
    useWaitFakeTimers();
    stubAlwaysRunning();

    const execution = buildProgram().parseAsync(
      ['device', 'import', file, '--driver-id', '11', '--profile-id', '12', '--wait-timeout', '1', '--poll-interval', '100'],
      { from: 'user' },
    );
    expect(await advanceUntil(importSubmitted, 1_000_000, 100)).toBe(true);
    const failure = await driveUntilSettled(execution, 10_000, 50);

    // The command must TERMINATE (an unbounded loop outlives the vitest
    // timeout) with the structured TimeoutError coordinates.
    expect(failure).toMatchObject({
      kind: 'timeout',
      code: 'TIMEOUT',
      exitCode: 1,
      operationId: 'op-1',
      lastStatus: 'RUNNING',
      statusUri: ACCEPTED.statusUri,
    });
    expect((failure as { elapsedMs: number }).elapsedMs).toBeGreaterThanOrEqual(1000);
    expect((failure as { message: string }).message).toContain('timed out waiting for operation op-1');

    // Poll spacing grows exponentially from the base interval: 100 -> 200 ->
    // 400, with the final sleep clamped by the remaining deadline. Offsets are
    // normalized to the first poll so the real startup lag cannot skew them.
    expect(pollOffsets()).toEqual([0, 100, 300, 700, 1000]);
  });

  it('clamps the deadline to the operation expiresAt plus a grace window', async () => {
    useWaitFakeTimers();
    // The gateway keeps answering RUNNING but reports the operation as
    // expiring one second after each poll.
    stubAlwaysRunning(1_000);

    const execution = buildProgram().parseAsync(
      ['device', 'import', file, '--driver-id', '11', '--profile-id', '12', '--wait-timeout', '600'],
      { from: 'user' },
    );
    expect(await advanceUntil(importSubmitted, 1_000_000, 100)).toBe(true);
    const failure = await driveUntilSettled(execution, 45_000, 250);

    // The 600s user timeout must NOT govern: the expiresAt clamp (1s + 30s
    // grace) ends the wait roughly 31s after the first poll.
    expect(failure).toMatchObject({ kind: 'timeout', code: 'TIMEOUT' });
    const elapsedMs = (failure as { elapsedMs: number }).elapsedMs;
    expect(elapsedMs).toBeGreaterThanOrEqual(30_000);
    expect(elapsedMs).toBeLessThan(34_000);
  });

  it('caps the exponential backoff at 10 seconds between polls', async () => {
    useWaitFakeTimers();
    stubAlwaysRunning();

    const execution = buildProgram().parseAsync(
      ['device', 'import', file, '--driver-id', '11', '--profile-id', '12', '--wait-timeout', '45', '--poll-interval', '100'],
      { from: 'user' },
    );
    expect(await advanceUntil(importSubmitted, 1_000_000, 100)).toBe(true);
    const failure = await driveUntilSettled(execution, 60_000, 250);
    expect(failure).toMatchObject({ kind: 'timeout' });

    const offsets = pollOffsets();
    const gaps = offsets.slice(1).map((at, i) => at - offsets[i]);
    // Backoff doubles from 100ms, is capped at 10s, and never exceeds the cap
    // even for the sleeps the doubling would have made longer.
    expect(Math.max(...gaps)).toBe(10_000);
    expect(gaps.slice(0, 6)).toEqual([100, 200, 400, 800, 1600, 3200]);
  });

  it('installs and removes the SIGINT listener around the wait', async () => {
    useWaitFakeTimers();
    stubAlwaysRunning();

    const baseline = process.listenerCount('SIGINT');
    const execution = buildProgram().parseAsync(
      ['device', 'import', file, '--driver-id', '11', '--profile-id', '12', '--wait-timeout', '600'],
      { from: 'user' },
    );

    expect(
      await advanceUntil(() => process.listenerCount('SIGINT') === baseline + 1, 1_000_000, 100),
    ).toBe(true);

    await driveUntilSettled(execution, 700_000, 1_000);
    expect(process.listenerCount('SIGINT')).toBe(baseline);
  });
});

describe('waitForOperation interrupt signal (F010)', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('an aborted wait reports the last status and the status URI', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: FetchInit) => {
        fetchCalls.push({ url, init: init ?? {}, atMs: Date.now() });
        return runningOperation();
      }),
    );

    const controller = new AbortController();
    const wait = waitForOperation(ACCEPTED, 5_000, 600_000, controller.signal);
    await new Promise((resolve) => setTimeout(resolve, 20));
    controller.abort();

    const failure = await wait.then(() => null, (error: unknown) => error);
    expect(failure).toMatchObject({
      kind: 'timeout',
      code: 'INTERRUPTED',
      exitCode: 1,
    });
    expect((failure as { message: string }).message).toContain('op-1');
    expect((failure as { message: string }).message).toContain('RUNNING');
    expect((failure as { message: string }).message).toContain(ACCEPTED.statusUri);
    expect(fetchCalls.length).toBeGreaterThanOrEqual(1);
  });

  it('returns the terminal operation view untouched', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => runningOperation({ status: 'SUCCEEDED', progress: 100 })),
    );
    const operation = await waitForOperation(ACCEPTED, 100, 600_000);
    expect(operation).toMatchObject({ operationId: 'op-1', status: 'SUCCEEDED' });
  });
});
