import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { existsSync } from 'node:fs';
import { mkdir, mkdtemp, readdir, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createCipheriv, randomBytes, scryptSync } from 'node:crypto';

// Redirect os.homedir() to a per-test temp dir; the store bakes its paths at
// module load, hence resetModules + dynamic imports after the stub is set.
const osStub = vi.hoisted(() => ({ home: '' }));

vi.mock('node:os', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:os')>();
  return {
    ...actual,
    homedir: () => (osStub.home ? osStub.home : actual.homedir()),
  };
});

describe('EncryptedFileStore (random key file, no plaintext, quarantine)', () => {
  let home: string;
  let encPath: string;
  let keyPath: string;
  let stderr: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-enc-'));
    osStub.home = home;
    encPath = join(home, '.dc3', 'credentials.enc');
    keyPath = join(home, '.dc3', 'credentials.key');
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

  async function freshStore() {
    const { EncryptedFileStore } = await import('../src/core/credential-encrypted');
    return new EncryptedFileStore();
  }

  it('never writes the password (or any entries copy) to disk (F002)', async () => {
    const store = await freshStore();
    await store.savePassword('admin@default', 'Plain#Pass9');

    const raw = await readFile(encPath, 'utf8');
    // Negative guards: the defect CANNOT recur — no plaintext, no entries echo.
    expect(raw.includes('Plain#Pass9')).toBe(false);
    expect(raw.includes('admin@default')).toBe(false);
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    expect(parsed.entries).toBeUndefined();
    expect(parsed.v).toBe(2);

    // Round-trip still works.
    expect(await store.getPassword('admin@default')).toBe('Plain#Pass9');
  });

  it('keeps entries of other profiles when one is overwritten', async () => {
    const store = await freshStore();
    await store.savePassword('a@t1', 'pw-a');
    await store.savePassword('b@t2', 'pw-b');
    await store.savePassword('a@t1', 'pw-a2');
    expect(await store.getPassword('a@t1')).toBe('pw-a2');
    expect(await store.getPassword('b@t2')).toBe('pw-b');
  });

  it('parallel saves keep every entry — concurrent logins lose nothing (F011)', async () => {
    const store = await freshStore();
    await Promise.all(
      Array.from({ length: 6 }, (_, i) => store.savePassword(`agent${i}@tenant`, `pw-${i}`)),
    );
    for (let i = 0; i < 6; i++) {
      expect(await store.getPassword(`agent${i}@tenant`)).toBe(`pw-${i}`);
    }
  });

  it('without the key file the store is undecryptable: quarantine + loud warning (F002)', async () => {
    const store = await freshStore();
    await store.savePassword('admin@default', 'Plain#Pass9');
    await rm(keyPath);

    expect(await store.getPassword('admin@default')).toBeNull();
    expect(stderr).toContain('credentials.key');
    expect(stderr).toContain('credentials.enc');
    const quarantined = (await readdir(join(home, '.dc3'))).filter((n) =>
      n.startsWith('credentials.enc.corrupt-'),
    );
    // The previous bytes survive for recovery; a fresh write cannot destroy them.
    expect(quarantined.length).toBe(1);
  });

  it('a corrupt store is quarantined and warned, never silently reset (F027)', async () => {
    const store = await freshStore();
    await store.savePassword('admin@default', 'Plain#Pass9');
    await writeFile(encPath, 'GARBAGE-NOT-JSON', 'utf8');

    expect(await store.getPassword('admin@default')).toBeNull();
    expect(stderr).toContain('not valid JSON');
    expect((await readdir(join(home, '.dc3'))).some((n) => n.startsWith('credentials.enc.corrupt-'))).toBe(true);

    // Re-login after corruption starts fresh but preserves the quarantine copy.
    await store.savePassword('new@default', 'pw-new');
    expect(await store.getPassword('new@default')).toBe('pw-new');
    expect((await readdir(join(home, '.dc3'))).filter((n) => n.startsWith('credentials.enc.corrupt-')).length).toBe(1);
  });

  it('legacy files with plaintext entries are scrubbed (re-encrypted) on first read (F002 migration)', async () => {
    const os = await import('node:os');
    const legacyKey = scryptSync(
      `${os.hostname()}-${os.userInfo().username}-${os.arch()}`,
      'dc3-cli-static-salt',
      32,
    );
    const entries = { 'admin@default': 'Legacy#Pass1' };
    const iv = randomBytes(16);
    const cipher = createCipheriv('aes-256-gcm', legacyKey, iv);
    const data = Buffer.concat([cipher.update(JSON.stringify(entries), 'utf8'), cipher.final()]);
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(
      encPath,
      JSON.stringify({
        iv: iv.toString('hex'),
        tag: cipher.getAuthTag().toString('hex'),
        data: data.toString('hex'),
        entries, // the leaked plaintext copy
      }),
      'utf8',
    );

    const store = await freshStore();
    expect(await store.getPassword('admin@default')).toBe('Legacy#Pass1');
    expect(stderr).toContain('migrated');

    const raw = await readFile(encPath, 'utf8');
    expect(raw.includes('Legacy#Pass1')).toBe(false);
    expect(raw.includes('admin@default')).toBe(false);
    expect((JSON.parse(raw) as Record<string, unknown>).entries).toBeUndefined();
    expect(existsSync(keyPath)).toBe(true);
  });

  it('legacy files that cannot be decrypted are quarantined with a warning', async () => {
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(
      encPath,
      JSON.stringify({
        iv: randomBytes(16).toString('hex'),
        tag: randomBytes(16).toString('hex'),
        data: randomBytes(64).toString('hex'),
        entries: { 'admin@default': 'deadbeef' },
      }),
      'utf8',
    );
    const store = await freshStore();
    expect(await store.getPassword('admin@default')).toBeNull();
    expect(stderr).toContain('legacy');
    expect((await readdir(join(home, '.dc3'))).some((n) => n.startsWith('credentials.enc.corrupt-'))).toBe(true);
  });

  it('deleting the last entry removes the file (logout hygiene)', async () => {
    const store = await freshStore();
    await store.savePassword('admin@default', 'pw');
    await store.deletePassword('admin@default');
    expect(existsSync(encPath)).toBe(false);
    expect(await store.getPassword('admin@default')).toBeNull();
  });

  it('getAllIdentifiers lists stored entries (reset scrubbing)', async () => {
    const store = await freshStore();
    await store.savePassword('a@t1', 'pw-a');
    await store.savePassword('b@t2', 'pw-b');
    expect((await store.getAllIdentifiers()).sort()).toEqual(['a@t1', 'b@t2']);
  });

  it('credentials.enc and credentials.key are owner-only (F026)', async () => {
    const store = await freshStore();
    await store.savePassword('admin@default', 'pw');
    const { stat } = await import('node:fs/promises');
    if (process.platform !== 'win32') {
      expect((await stat(encPath)).mode & 0o777).toBe(0o600);
      expect((await stat(keyPath)).mode & 0o777).toBe(0o600);
      return;
    }
    const { execFile } = await import('node:child_process');
    const { promisify } = await import('node:util');
    const icacls = promisify(execFile);
    for (const path of [encPath, keyPath]) {
      const { stdout } = await icacls('icacls', [path], { windowsHide: true });
      expect(stdout).not.toMatch(/\(I\)/u);
    }
  });
});
