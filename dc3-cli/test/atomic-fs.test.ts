import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { execFile } from 'node:child_process';
import { mkdir, mkdtemp, readFile, readdir, rm, utimes, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { basename, join } from 'node:path';
import { promisify } from 'node:util';

import { quarantineFile, restrictToFileOwner, withLock, writeFileAtomic } from '../src/core/atomic-fs.js';

const execFileAsync = promisify(execFile);
const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Test seams around the real fs/promises module: the atomic writer's two write
 * paths (FileHandle.writeFile on the temp file, and a regressed direct
 * fs.writeFile) can be killed mid-flight, every rename is recorded, and
 * individual prune rm calls can be made to fail.
 */
const { fsHooks, renameCalls } = vi.hoisted(() => ({
  fsHooks: {
    failTempWrites: false,
    failRmSuffix: null as string | null,
  },
  renameCalls: [] as Array<[unknown, unknown]>,
}));

// Behavior-preserving wrapper: only the hooked paths deviate from the real
// module, so every other test in this file exercises the real filesystem.
vi.mock('node:fs/promises', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:fs/promises')>();
  return {
    ...actual,
    open: async (...args: Parameters<typeof actual.open>) => {
      const handle = await actual.open(...args);
      if (args.length > 0 && String(args[0]).includes('.tmp-')) {
        const realWriteFile = handle.writeFile.bind(handle);
        handle.writeFile = async (...writeArgs: Parameters<typeof realWriteFile>) => {
          if (fsHooks.failTempWrites) {
            throw new Error('simulated kill mid-write (FileHandle.writeFile)');
          }
          return realWriteFile(...writeArgs);
        };
      }
      return handle;
    },
    writeFile: async (...args: Parameters<typeof actual.writeFile>) => {
      if (fsHooks.failTempWrites && args.length > 0 && String(args[0]).includes('.tmp-')) {
        throw new Error('simulated kill mid-write (fs.writeFile)');
      }
      return actual.writeFile(...args);
    },
    rename: async (...args: Parameters<typeof actual.rename>) => {
      renameCalls.push([args[0], args[1]]);
      return actual.rename(...args);
    },
    rm: async (...args: Parameters<typeof actual.rm>) => {
      if (fsHooks.failRmSuffix !== null && args.length > 0 && String(args[0]).includes(fsHooks.failRmSuffix)) {
        throw new Error('EBUSY: simulated prune failure');
      }
      return actual.rm(...args);
    },
  };
});

beforeEach(() => {
  fsHooks.failTempWrites = false;
  fsHooks.failRmSuffix = null;
  renameCalls.length = 0;
});

describe('writeFileAtomic', () => {
  let dir: string;

  beforeEach(async () => {
    dir = await mkdtemp(join(tmpdir(), 'dc3-cli-atomic-'));
  });

  afterEach(async () => {
    await rm(dir, { recursive: true, force: true });
  });

  it('writes the exact content and leaves no temp files behind', async () => {
    const path = join(dir, 'state.json');
    await writeFileAtomic(path, '{"a":1}');
    expect(await readFile(path, 'utf8')).toBe('{"a":1}');
    const leftovers = (await readdir(dir)).filter((name) => name.includes('.tmp-'));
    expect(leftovers).toEqual([]);
  });

  it('creates missing parent directories', async () => {
    const path = join(dir, 'nested', 'deep', 'state.json');
    await writeFileAtomic(path, 'x');
    expect(await readFile(path, 'utf8')).toBe('x');
  });

  it('replaces an existing file atomically (old or new content, never partial)', async () => {
    const path = join(dir, 'state.json');
    await writeFileAtomic(path, 'old-content');
    await writeFileAtomic(path, 'new-content');
    expect(await readFile(path, 'utf8')).toBe('new-content');
    const leftovers = (await readdir(dir)).filter((name) => name.includes('.tmp-'));
    expect(leftovers).toEqual([]);
  });

  it('applies mode 0600 on POSIX for owner-restricted writes', async () => {
    if (process.platform === 'win32') {
      // Win32 reports 0o666 for every writable file, so a mode-bit assertion
      // there can never fail and would only fake coverage; the win32 contract
      // (inherited ACEs stripped) is asserted by the icacls tests below.
      return;
    }
    const path = join(dir, 'secret.json');
    await writeFileAtomic(path, 'x', { restrictToOwner: true });
    const { stat } = await import('node:fs/promises');
    const info = await stat(path);
    expect(info.mode & 0o777).toBe(0o600);
  });

  it('win32: restrictToOwner strips inherited ACEs (icacls /inheritance:r)', async () => {
    if (process.platform !== 'win32') {
      return;
    }
    const path = join(dir, 'secret.json');
    await writeFileAtomic(path, 'x', { restrictToOwner: true });
    const { stdout } = await execFileAsync('icacls', [path], { windowsHide: true });
    // Inherited ACEs are marked '(I)' — none may remain on a restricted file,
    // and the current user keeps full control (ACCOUNT:(F)).
    expect(stdout).not.toMatch(/\(I\)/u);
    expect(stdout).toMatch(/\(F\)/u);
  });

  it('win32: restrictToFileOwner keeps working on an existing file', async () => {
    if (process.platform !== 'win32') {
      return;
    }
    const path = join(dir, 'plain.json');
    await writeFile(path, 'x', 'utf8');
    await restrictToFileOwner(path);
    const { stdout } = await execFileAsync('icacls', [path], { windowsHide: true });
    expect(stdout).not.toMatch(/\(I\)/u);
  });

  it('a write killed mid-flight leaves the previous bytes intact and never commits (F011)', async () => {
    const path = join(dir, 'tokens.json');
    const previous = '{"access_token":"old","expires_at":123}';
    await writeFile(path, previous, 'utf8');

    fsHooks.failTempWrites = true;
    await expect(writeFileAtomic(path, '{"access_token":"half-written"}')).rejects.toThrow(
      /kill mid-write/u,
    );
    fsHooks.failTempWrites = false;

    // The pre-existing target is byte-for-byte unchanged and parseable...
    expect(await readFile(path, 'utf8')).toBe(previous);
    expect(JSON.parse(await readFile(path, 'utf8')).access_token).toBe('old');
    // ...the half-written temp file is cleaned up...
    expect((await readdir(dir)).filter((name) => name.includes('.tmp-'))).toEqual([]);
    // ...and the commit seam never fired: rename is what publishes a write.
    expect(renameCalls).toHaveLength(0);
  });

  it('commits through exactly one rename per successful atomic write', async () => {
    const path = join(dir, 'state.json');
    await writeFileAtomic(path, '{"a":1}');

    expect(renameCalls).toHaveLength(1);
    const [from, to] = renameCalls[0];
    expect(String(from)).toContain('.tmp-');
    expect(String(to)).toBe(path);
    expect(await readFile(path, 'utf8')).toBe('{"a":1}');
  });
});

describe('withLock', () => {
  let dir: string;

  beforeEach(async () => {
    dir = await mkdtemp(join(tmpdir(), 'dc3-cli-lock-'));
  });

  afterEach(async () => {
    await rm(dir, { recursive: true, force: true });
  });

  it('serializes concurrent critical sections (no interleaving)', async () => {
    const path = join(dir, 'state.json');
    const events: string[] = [];
    await Promise.all([
      withLock(path, async () => {
        events.push('a-enter');
        await sleep(30);
        events.push('a-exit');
      }),
      withLock(path, async () => {
        events.push('b-enter');
        await sleep(10);
        events.push('b-exit');
      }),
    ]);
    const order = events.join(',');
    expect(
      order === 'a-enter,a-exit,b-enter,b-exit' || order === 'b-enter,b-exit,a-enter,a-exit',
      `sections interleaved: ${order}`,
    ).toBe(true);
    // The lockfile is cleaned up after the last section.
    expect((await readdir(dir)).filter((name) => name.endsWith('.lock'))).toEqual([]);
  });

  it('recovers a lockfile held by a dead pid', async () => {
    const path = join(dir, 'state.json');
    await writeFile(`${path}.lock`, '99999999', 'utf8');
    // Fresh mtime: staleness must come from the dead pid, not the age.
    const result = await withLock(path, async () => 'recovered');
    expect(result).toBe('recovered');
  });

  it('recovers a lockfile older than the stale age even with a live pid', async () => {
    const path = join(dir, 'state.json');
    await writeFile(`${path}.lock`, String(process.pid), 'utf8');
    const old = new Date(Date.now() - 60_000);
    await utimes(`${path}.lock`, old, old);
    const result = await withLock(path, async () => 'ran');
    expect(result).toBe('ran');
  });

  it('returns the section result and releases on throw', async () => {
    const path = join(dir, 'state.json');
    await expect(
      withLock(path, async () => {
        throw new Error('boom');
      }),
    ).rejects.toThrow('boom');
    expect((await readdir(dir)).filter((name) => name.endsWith('.lock'))).toEqual([]);
  });

  it(
    'acquisition timeout throws a typed TimeoutError (kind timeout), not a plain INTERNAL Error',
    async () => {
      const path = join(dir, 'state.json');
      // A live holder (this process) with a fresh mtime: neither staleness path
      // fires, so the acquire loop must run into its 10s deadline.
      await writeFile(`${path}.lock`, String(process.pid), 'utf8');

      // Fake only the wall clock and the poll sleep: the fs operations stay
      // real (staleness keeps seeing a fresh lock), while the clock is moved
      // past LOCK_ACQUIRE_TIMEOUT_MS (10s) without 400 real poll iterations.
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'Date'] });
      try {
        let outcome: { ok: boolean; value: unknown } | undefined;
        const settled = withLock(path, async () => 'never acquired').then(
          (value) => {
            outcome = { ok: true, value };
          },
          (error) => {
            outcome = { ok: false, value: error };
          },
        );
        // 1. Wait in real macrotask turns until the loop reaches its first
        //    poll sleep (performance.now stays real under the faked clock).
        const firstSleepAt = performance.now();
        while (vi.getTimerCount() === 0 && performance.now() - firstSleepAt < 500) {
          await new Promise((resolve) => setImmediate(resolve));
        }
        // 2. Jump past the acquire deadline. The pending sleep is now overdue,
        //    and overdue timers only fire on run-all — advance would skip it.
        vi.setSystemTime(new Date(Date.now() + 11_000));
        // 3. Drain in real turns until the awakened loop hits the deadline.
        const drainStartedAt = performance.now();
        while (outcome === undefined && performance.now() - drainStartedAt < 2000) {
          await vi.runAllTimersAsync();
          await new Promise((resolve) => setImmediate(resolve));
        }
        await settled;
        expect(outcome).toMatchObject({ ok: false });
        const error = (outcome as { value: unknown }).value as Error;

        // Taxonomy contract: kind timeout / code TIMEOUT at the chokepoint — a
        // regression to a plain Error reports as kind api / code INTERNAL.
        expect(error).toMatchObject({
          name: 'TimeoutError',
          kind: 'timeout',
          code: 'TIMEOUT',
          exitCode: 1,
        });
        expect(error.message).toMatch(/timed out waiting for the lock on .*\.lock/u);
        // The triggering EEXIST stays chained for diagnostics.
        expect((error as Error & { cause?: unknown }).cause).toMatchObject({ code: 'EEXIST' });
      } finally {
        vi.useRealTimers();
      }
      // The section never ran and the foreign lockfile was not stolen.
      expect((await readdir(dir)).filter((name) => name.endsWith('.lock'))).toEqual([
        'state.json.lock',
      ]);
    },
    15_000,
  );
});

describe('quarantineFile', () => {
  let dir: string;

  beforeEach(async () => {
    dir = await mkdtemp(join(tmpdir(), 'dc3-cli-quar-'));
  });

  afterEach(async () => {
    await rm(dir, { recursive: true, force: true });
  });

  it('moves the file to a timestamped sibling, preserving bytes', async () => {
    const path = join(dir, 'tokens.json');
    await mkdir(dir, { recursive: true });
    await writeFile(path, 'corrupt-bytes', 'utf8');
    const target = await quarantineFile(path);
    expect(target).not.toBeNull();
    expect(target).toMatch(/tokens\.json\.corrupt-/u);
    expect(await readFile(target!, 'utf8')).toBe('corrupt-bytes');
    // The original path is freed: a later write can never destroy the bytes.
    await expect(readFile(path, 'utf8')).rejects.toMatchObject({ code: 'ENOENT' });
  });

  it('returns null for a missing file', async () => {
    const target = await quarantineFile(join(dir, 'missing.json'));
    expect(target).toBeNull();
  });

  it('prunes old quarantine copies down to the newest five (rotation cap)', async () => {
    const path = join(dir, 'tokens.json');
    await writeFile(path, 'fresh-corrupt', 'utf8');
    // Seven pre-existing quarantine siblings with increasing timestamps.
    const stamps = [
      '2020-01-01T00-00-00-000Z',
      '2020-01-02T00-00-00-000Z',
      '2020-01-03T00-00-00-000Z',
      '2020-01-04T00-00-00-000Z',
      '2020-01-05T00-00-00-000Z',
      '2020-01-06T00-00-00-000Z',
      '2020-01-07T00-00-00-000Z',
    ];
    for (const stamp of stamps) {
      await writeFile(`${path}.corrupt-${stamp}-deadbeef`, 'old', 'utf8');
    }

    const target = await quarantineFile(path);

    expect(target).not.toBeNull();
    const remaining = (await readdir(dir))
      .filter((name) => name.startsWith('tokens.json.corrupt-'))
      .sort();
    expect(remaining).toHaveLength(5);
    // The fresh copy and the four newest fakes survive...
    expect(remaining).toContain(basename(target!));
    expect(remaining).toContain(`tokens.json.corrupt-${stamps[6]}-deadbeef`);
    expect(remaining).toContain(`tokens.json.corrupt-${stamps[3]}-deadbeef`);
    // ...the three oldest are pruned.
    expect(remaining).not.toContain(`tokens.json.corrupt-${stamps[0]}-deadbeef`);
    expect(remaining).not.toContain(`tokens.json.corrupt-${stamps[1]}-deadbeef`);
    expect(remaining).not.toContain(`tokens.json.corrupt-${stamps[2]}-deadbeef`);
  });

  it('a prune failure warns but never fails the quarantine', async () => {
    const path = join(dir, 'config.json');
    await writeFile(path, 'corrupt', 'utf8');
    for (let i = 1; i <= 6; i += 1) {
      await writeFile(`${path}.corrupt-2020-01-0${i}T00-00-00-000Z-fake`, 'old', 'utf8');
    }
    fsHooks.failRmSuffix = '2020-01-01T00-00-00-000Z-fake';
    const warnSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    try {
      const target = await quarantineFile(path);

      // The quarantine itself succeeded: bytes preserved, warning emitted.
      expect(target).not.toBeNull();
      expect(await readFile(target!, 'utf8')).toBe('corrupt');
      expect(
        warnSpy.mock.calls.some((call) => String(call[0]).includes('could not prune old quarantine copy')),
      ).toBe(true);
      // The un-prunable oldest copy still exists for manual recovery.
      await expect(readFile(`${path}.corrupt-2020-01-01T00-00-00-000Z-fake`, 'utf8')).resolves.toBe(
        'old',
      );
    } finally {
      warnSpy.mockRestore();
    }
  });
});
