import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Capture every child process the keychain store spawns. The mock provides
// ONLY execFile: if the store ever falls back to a shell (exec/spawn with a
// command string), the call crashes and these guards fail.
const child = vi.hoisted(() => ({
  calls: [] as Array<{ file: string; args: string[]; opts: Record<string, unknown> }>,
  failNext: false,
  stdout: '',
}));

vi.mock('node:child_process', () => ({
  execFile: (
    file: string,
    args: string[],
    opts: Record<string, unknown>,
    callback: (_err: Error | null, _result?: { stdout: string; stderr: string }) => void,
  ) => {
    child.calls.push({ file, args, opts });
    if (child.failNext) {
      child.failNext = false;
      callback(new Error('command failed'));
      return;
    }
    callback(null, { stdout: child.stdout, stderr: '' });
  },
}));

import { KeychainStore } from '../src/core/credential-keychain';

const REAL_PLATFORM = process.platform;

function stubPlatform(value: string): void {
  Object.defineProperty(process, 'platform', { value, configurable: true });
}

/** Adversarial identifier that achieves command execution through shell interpolation. */
const INJECT_ID = `x')) ; whoami > marker ; #`;

describe('KeychainStore transport security (execFile argv, secrets on stdin)', () => {
  let store: KeychainStore;

  beforeEach(() => {
    store = new KeychainStore();
    child.calls.length = 0;
    child.failNext = false;
    child.stdout = '';
  });

  afterEach(() => {
    stubPlatform(REAL_PLATFORM);
  });

  describe('isAvailable (fast bounded probe)', () => {
    it('win32: probes Get-Command with -NoProfile and a timeout of at most 2000ms (F005)', async () => {
      stubPlatform('win32');
      expect(await store.isAvailable()).toBe(true);
      expect(child.calls).toHaveLength(1);
      const call = child.calls[0];
      expect(call.file).toBe('powershell');
      expect(call.args[0]).toBe('-NoProfile');
      expect(call.args[1]).toBe('-Command');
      expect(String(call.args[2])).toContain('Get-Command New-StoredCredential');
      expect(Number(call.opts.timeout)).toBeLessThanOrEqual(2000);
      expect(Number(call.opts.timeout)).toBeGreaterThan(0);
    });

    it('win32: a failed probe reports unavailable instead of hanging or erroring (F005)', async () => {
      stubPlatform('win32');
      child.failNext = true;
      expect(await store.isAvailable()).toBe(false);
    });

    it('linux: the probe never spawns a process (pure PATH existence check)', async () => {
      stubPlatform('linux');
      await store.isAvailable();
      expect(child.calls).toHaveLength(0);
    });

    it('darwin: the probe never spawns a process either', async () => {
      stubPlatform('darwin');
      await store.isAvailable();
      expect(child.calls).toHaveLength(0);
    });
  });

  describe('savePassword', () => {
    it('win32: EncodedCommand script, PS single-quote doubling, password via stdin only (F012)', async () => {
      stubPlatform('win32');
      await store.savePassword(INJECT_ID, 'p@ss w0rd');

      expect(child.calls).toHaveLength(1);
      const call = child.calls[0];
      expect(call.file).toBe('powershell');
      expect(call.args[0]).toBe('-NoProfile');
      expect(call.args[1]).toBe('-EncodedCommand');
      // The encoded script decodes from UTF-16LE base64.
      const script = Buffer.from(String(call.args[2]), 'base64').toString('utf16le');

      // Every single quote inside the identifier is doubled inside a PS literal.
      const quoted = `'${INJECT_ID.replace(/'/gu, "''")}'`;
      expect(script).toContain(quoted);
      // The password is never part of the script.
      expect(script.includes('p@ss w0rd')).toBe(false);
      // It travels on the child's stdin instead.
      expect(call.opts.input).toBe('p@ss w0rd');
      // The raw identifier with un-doubled quotes appears nowhere else.
      expect(script.includes(INJECT_ID)).toBe(false);
    });

    it('linux: secret-tool reads the secret from stdin — never argv, never echo (F012)', async () => {
      stubPlatform('linux');
      const evil = `p$(touch /tmp/pwned)\`id\``;
      await store.savePassword(INJECT_ID, evil);

      expect(child.calls).toHaveLength(1);
      const call = child.calls[0];
      expect(call.file).toBe('secret-tool');
      // argv is an array with the identifier as discrete elements — no shell.
      expect(call.args).toEqual(
        expect.arrayContaining(['store', '--label=dc3-cli', 'service', 'dc3-cli', 'account', INJECT_ID]),
      );
      // Negative guard: the secret is in stdin only.
      expect(call.opts.input).toBe(evil);
      expect(call.args.join(' ').includes(evil)).toBe(false);
    });

    it('darwin: security runs with an argv array (no shell, identifier as one element)', async () => {
      stubPlatform('darwin');
      await store.savePassword(INJECT_ID, 'pw');

      expect(child.calls).toHaveLength(1);
      const call = child.calls[0];
      expect(call.file).toBe('security');
      expect(Array.isArray(call.args)).toBe(true);
      // The identifier travels as ONE discrete argv element: the payload text
      // cannot split into extra command tokens for any shell to parse.
      expect(call.args.filter((arg) => arg.includes('whoami'))).toEqual([INJECT_ID]);
    });
  });

  describe('getPassword', () => {
    it('linux: lookup via argv array with the identifier as a discrete element', async () => {
      stubPlatform('linux');
      child.stdout = 'recovered\n';
      expect(await store.getPassword(INJECT_ID)).toBe('recovered');
      const call = child.calls[0];
      expect(call.file).toBe('secret-tool');
      expect(call.args).toEqual(['lookup', 'service', 'dc3-cli', 'account', INJECT_ID]);
    });

    it('win32: Get-StoredCredential via EncodedCommand with a sanitized, quoted target', async () => {
      stubPlatform('win32');
      child.stdout = 'recovered';
      expect(await store.getPassword('admin@tenant')).toBe('recovered');
      const call = child.calls[0];
      expect(call.file).toBe('powershell');
      const script = Buffer.from(String(call.args[2]), 'base64').toString('utf16le');
      expect(script).toContain('Get-StoredCredential');
      expect(script).toContain("'dc3-cli-admin-tenant'");
    });

    it('darwin: find-generic-password via argv array', async () => {
      stubPlatform('darwin');
      child.stdout = 'recovered';
      expect(await store.getPassword('admin@tenant')).toBe('recovered');
      expect(child.calls[0].args).toEqual([
        'find-generic-password',
        '-a',
        'admin@tenant',
        '-s',
        'dc3-cli',
        '-w',
      ]);
    });

    it('a failing lookup returns null without throwing', async () => {
      stubPlatform('linux');
      child.failNext = true;
      expect(await store.getPassword('admin@tenant')).toBeNull();
    });
  });

  describe('deletePassword', () => {
    it('win32: Remove-StoredCredential via EncodedCommand', async () => {
      stubPlatform('win32');
      await store.deletePassword('admin@tenant');
      const script = Buffer.from(String(child.calls[0].args[2]), 'base64').toString('utf16le');
      expect(script).toContain('Remove-StoredCredential');
      expect(script).toContain("'dc3-cli-admin-tenant'");
    });

    it('linux: clear via argv array', async () => {
      stubPlatform('linux');
      await store.deletePassword(INJECT_ID);
      expect(child.calls[0].args).toEqual(['clear', 'service', 'dc3-cli', 'account', INJECT_ID]);
    });

    it('deleting a missing entry is not an error', async () => {
      stubPlatform('darwin');
      child.failNext = true;
      await expect(store.deletePassword('admin@tenant')).resolves.toBeUndefined();
    });
  });
});
