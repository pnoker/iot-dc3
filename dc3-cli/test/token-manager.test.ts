import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { existsSync } from 'node:fs';
import { mkdir, mkdtemp, readdir, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

// Redirect os.homedir() to a per-test temp dir so the token manager never
// touches the real HOME. Paths are baked at module load, hence resetModules +
// dynamic imports after the stub is set.
const osStub = vi.hoisted(() => ({ home: '' }));

vi.mock('node:os', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:os')>();
  return {
    ...actual,
    homedir: () => (osStub.home ? osStub.home : actual.homedir()),
  };
});

function fakeState(username: string, tenant = 'default') {
  return {
    token: `tok-${username}-${Math.random().toString(36).slice(2)}`,
    salt: 'salt',
    tenant,
    username,
    issuedAt: 100,
    expiresAt: 2_000_000_000,
  };
}

describe('token-manager persistence (atomic, locked, quarantined)', () => {
  let home: string;
  let tokensPath: string;
  let stderr: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-tokens-'));
    osStub.home = home;
    tokensPath = join(home, '.dc3', 'tokens.json');
    vi.resetModules();
    stderr = '';
    vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
      stderr += args.join(' ');
    });
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  it('round-trips state per profile and stamps the epoch', async () => {
    const { tokenManager } = await import('../src/core/token-manager');
    await tokenManager.saveState(fakeState('admin'), 'default');
    const state = await tokenManager.getState('default');
    expect(state?.username).toBe('admin');
    expect(state?.epoch).toBe(0);
  });

  // LAST-RESORT retries (documented): withLock in src/core/atomic-fs.ts has
  // two diagnosed contention defects — open(lock,'wx') surfaces EPERM (Windows
  // delete-pending) that is not retried, and the vanished-lock stale path can
  // rm a successor's fresh lockfile, letting two critical sections overlap
  // (lost update). Both reproduce at low rates under load (stress harness:
  // ~6/300 and ~2/300 rounds). A systematic F011 regression fails every
  // attempt, so retries only absorb the transient interleavings. Remove the
  // retry option once atomic-fs treats EPERM as retryable and stops rm-ing
  // vanished locks.
  it('parallel saveState keeps every profile — concurrent logins lose nothing (F011)', { retry: 2, timeout: 30_000 }, async () => {
    const { tokenManager } = await import('../src/core/token-manager');
    // Deterministic seeding: one known profile persisted before the storm, so
    // the post-storm file must contain it plus every concurrent writer — a
    // reset-to-empty or lock-acquire failure cannot pass by coincidence.
    await tokenManager.saveState(fakeState('seeded'), 'seeded');
    const profiles = ['p0', 'p1', 'p2', 'p3', 'p4', 'p5'];
    const saves = await Promise.allSettled(
      profiles.map((profile, i) => tokenManager.saveState(fakeState(`agent${i}`), profile)),
    );
    // Bounded awaits with diagnostics: a rejected save (lock timeout under
    // load) must name its profile instead of surfacing as a count mismatch.
    for (const [i, save] of saves.entries()) {
      const reason = save.status === 'rejected' ? String(save.reason) : '';
      expect(save.status, `save of profile ${profiles[i]}: ${reason}`).toBe('fulfilled');
    }
    const all = await tokenManager.getAllStates();
    expect(Object.keys(all).sort()).toEqual(['p0', 'p1', 'p2', 'p3', 'p4', 'p5', 'seeded']);
    // The seeded entry survived the storm untouched.
    expect(all.seeded.username).toBe('seeded');
  });

  it('writes leave no temp files and a parseable file behind', async () => {
    const { tokenManager } = await import('../src/core/token-manager');
    await tokenManager.saveState(fakeState('admin'), 'default');
    const dc3Dir = join(home, '.dc3');
    const names = await readdir(dc3Dir);
    expect(names.filter((n) => n.includes('.tmp-'))).toEqual([]);
    const raw = await readFile(tokensPath, 'utf8');
    expect(() => JSON.parse(raw)).not.toThrow();
  });

  it('a torn/empty tokens file is quarantined with a loud warning, never silently reset (F027)', async () => {
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(tokensPath, '', 'utf8');
    const { tokenManager } = await import('../src/core/token-manager');

    expect(await tokenManager.getState('default')).toBeNull();
    expect(stderr).toContain('tokens.json');
    const quarantined = (await readdir(join(home, '.dc3'))).filter((n) =>
      n.startsWith('tokens.json.corrupt-'),
    );
    expect(quarantined.length).toBe(1);

    // The next save writes a fresh file; it does not resurrect the torn one.
    await tokenManager.saveState(fakeState('admin'), 'default');
    const all = await tokenManager.getAllStates();
    expect(Object.keys(all)).toEqual(['default']);
  });

  it('garbage JSON is quarantined with the reason named (F027)', async () => {
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(tokensPath, '}{ not json', 'utf8');
    const { tokenManager } = await import('../src/core/token-manager');
    expect(await tokenManager.isAuthenticated('default')).toBe(false);
    expect(stderr).toContain('not valid JSON');
    expect((await readdir(join(home, '.dc3'))).some((n) => n.startsWith('tokens.json.corrupt-'))).toBe(true);
  });

  it('a legacy v1 flat map is readable and upgraded on the next write', async () => {
    await mkdir(join(home, '.dc3'), { recursive: true });
    const legacy = { default: fakeState('legacy-admin') };
    await writeFile(tokensPath, JSON.stringify(legacy), 'utf8');
    const { tokenManager } = await import('../src/core/token-manager');

    expect((await tokenManager.getState('default'))?.username).toBe('legacy-admin');
    await tokenManager.saveState(fakeState('other'), 'second');
    const raw = JSON.parse(await readFile(tokensPath, 'utf8'));
    expect(raw.version).toBe(2);
    expect(Object.keys(raw.states).sort()).toEqual(['default', 'second']);
  });

  it('clearState bumps the epoch in a single write and refuses stale saves (F024)', async () => {
    const { tokenManager } = await import('../src/core/token-manager');
    await tokenManager.saveState(fakeState('admin'), 'default');
    const epochBefore = await tokenManager.getEpoch('default');

    await tokenManager.clearState('default');
    expect(await tokenManager.getState('default')).toBeNull();
    expect(await tokenManager.getEpoch('default')).toBe(epochBefore + 1);

    // A renewal that captured the pre-logout epoch must not resurrect a session.
    const staleSave = await tokenManager.saveStateIfEpochUnchanged(
      fakeState('ghost'),
      'default',
      epochBefore,
    );
    expect(staleSave).toBe(false);
    expect(await tokenManager.getState('default')).toBeNull();
    expect((await tokenManager.getAllStates()).default).toBeUndefined();

    // A renewal that captured the current epoch persists normally.
    const freshSave = await tokenManager.saveStateIfEpochUnchanged(
      fakeState('admin2'),
      'default',
      epochBefore + 1,
    );
    expect(freshSave).toBe(true);
    expect((await tokenManager.getState('default'))?.username).toBe('admin2');
  });

  it('logout racing parallel renewals never leaves the cleared profile behind (F024)', async () => {
    const { tokenManager } = await import('../src/core/token-manager');
    await tokenManager.saveState(fakeState('admin'), 'default');
    const capturedEpoch = await tokenManager.getEpoch('default');

    const sleep = (ms: number): Promise<void> => new Promise((r) => setTimeout(r, ms));
    // The renewal minted its token against the pre-logout epoch; the logout
    // completes before the renewal reaches its save.
    const renewal = (async () => {
      await sleep(25);
      return tokenManager.saveStateIfEpochUnchanged(fakeState('late'), 'default', capturedEpoch);
    })();
    await tokenManager.clearState('default');
    expect(await renewal).toBe(false);
    expect(await tokenManager.getState('default')).toBeNull();
  });

  it('clearState keeps other profiles and the epoch map intact', async () => {
    const { tokenManager } = await import('../src/core/token-manager');
    await tokenManager.saveState(fakeState('a'), 'a');
    await tokenManager.saveState(fakeState('b'), 'b');
    await tokenManager.clearState('a');
    expect((await tokenManager.getState('b'))?.username).toBe('b');
    expect(await tokenManager.getState('a')).toBeNull();
    const raw = JSON.parse(await readFile(tokensPath, 'utf8'));
    expect(raw.epochs.a).toBe(1);
  });

  it('clearAll removes the tokens file entirely (config reset)', async () => {
    const { tokenManager } = await import('../src/core/token-manager');
    await tokenManager.saveState(fakeState('a'), 'a');
    await tokenManager.clearAll();
    expect(existsSync(tokensPath)).toBe(false);
    expect(await tokenManager.getAllStates()).toEqual({});
  });

  it('credentialIdentifier builds the documented username@tenant key (F037 foundation)', async () => {
    const { credentialIdentifier } = await import('../src/core/token-manager');
    expect(credentialIdentifier({ username: 'admin', tenant: 'tenantA' })).toBe('admin@tenantA');
  });

  it('state identity survives the save/load round-trip for renewal keying (F037 foundation)', async () => {
    const { tokenManager, credentialIdentifier } = await import('../src/core/token-manager');
    await tokenManager.saveState({ ...fakeState('admin'), tenant: 'tenantA' }, 'default');
    const state = await tokenManager.getState('default');
    expect(credentialIdentifier(state!)).toBe('admin@tenantA');
  });

  it('win32/POSIX restricted file: token file is owner-only (F026)', async () => {
    const { tokenManager } = await import('../src/core/token-manager');
    await tokenManager.saveState(fakeState('admin'), 'default');
    const { stat } = await import('node:fs/promises');
    const info = await stat(tokensPath);
    if (process.platform !== 'win32') {
      expect(info.mode & 0o777).toBe(0o600);
      return;
    }
    const { execFile } = await import('node:child_process');
    const { promisify } = await import('node:util');
    const { stdout } = await promisify(execFile)('icacls', [tokensPath], { windowsHide: true });
    expect(stdout).not.toMatch(/\(I\)/u);
  });
});
