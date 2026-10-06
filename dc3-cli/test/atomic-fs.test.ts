import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { execFile } from 'node:child_process';
import { mkdir, mkdtemp, readFile, readdir, rm, utimes, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { promisify } from 'node:util';

import { quarantineFile, restrictToFileOwner, withLock, writeFileAtomic } from '../src/core/atomic-fs.js';

const execFileAsync = promisify(execFile);
const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

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
    const path = join(dir, 'secret.json');
    await writeFileAtomic(path, 'x', { restrictToOwner: true });
    const { stat } = await import('node:fs/promises');
    const info = await stat(path);
    if (process.platform === 'win32') {
      // Mode bits cannot express per-principal restrictions on win32; the ACL
      // path is asserted separately below.
      expect(info.mode & 0o600).toBeTruthy();
    } else {
      expect(info.mode & 0o777).toBe(0o600);
    }
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
});
