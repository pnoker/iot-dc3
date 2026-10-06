import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Command } from 'commander';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { SilentExit } from '../src/utils/format.js';

/*
 * Guards for the binary-capable client seam behind `device import-template`
 * (audit G24): the XLSX download must ride the authenticated pipeline —
 * proactive renewal, 401-retry renewal, AuthError (exit 3) mapping — instead
 * of a direct fetch that skipped all of it and degraded 401/403 to plain
 * ApiError exit 1.
 */

const mocks = vi.hoisted(() => ({
  getActiveProfile: vi.fn(),
  getActiveProfileName: vi.fn(async () => 'default'),
  getSettings: vi.fn(async () => ({ renewal_threshold_hours: 12 })),
  getState: vi.fn(),
  needsRenewal: vi.fn(async () => false),
  buildHeaders: vi.fn(
    (state: { tenant: string; username: string; salt: string; token: string }) => ({
      'Content-Type': 'application/json',
      'X-Auth-Tenant': state.tenant,
      'X-Auth-Login': state.username,
      'X-Auth-Token': JSON.stringify({ salt: state.salt, token: state.token }),
    }),
  ),
  saveState: vi.fn(async () => undefined),
  saveStateIfEpochUnchanged: vi.fn(async () => true),
  getEpoch: vi.fn(async () => 0),
  credentialIdentifier: vi.fn(
    (state: { username: string; tenant: string }) => `${state.username}@${state.tenant}`,
  ),
  clearState: vi.fn(async () => undefined),
  resolvePassword: vi.fn(async (): Promise<string | null> => null),
  deletePasswordFromStore: vi.fn(async () => undefined),
}));

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: mocks.getActiveProfile,
    getActiveProfileName: mocks.getActiveProfileName,
    getSettings: mocks.getSettings,
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: mocks.getState,
    needsRenewal: mocks.needsRenewal,
    buildHeaders: mocks.buildHeaders,
    saveState: mocks.saveState,
    saveStateIfEpochUnchanged: mocks.saveStateIfEpochUnchanged,
    getEpoch: mocks.getEpoch,
    clearState: mocks.clearState,
  },
  credentialIdentifier: mocks.credentialIdentifier,
}));

vi.mock('../src/core/credential-store.js', () => ({
  resolvePassword: mocks.resolvePassword,
  deletePasswordFromStore: mocks.deletePasswordFromStore,
}));

// writeFile seam: defaults to the real implementation so the success path
// writes a real file; individual tests override it to inject errno failures.
const { writeFileMock } = vi.hoisted(() => ({ writeFileMock: vi.fn() }));
vi.mock('node:fs/promises', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:fs/promises')>();
  return { ...actual, writeFile: writeFileMock };
});

import { registerDeviceCommand } from '../src/commands/device.js';

const TEMPLATE_BYTES = Buffer.from([0x50, 0x4b, 0x03, 0x04, 0x00, 0xff, 0x0a, 0x5a]);

const loginState = {
  token: 'old.jwt.value',
  salt: 'old-salt',
  tenant: 'tenantA',
  username: 'admin',
  epoch: 0,
};

function fakeJwt(payload: Record<string, unknown>): string {
  const encode = (value: Record<string, unknown>) =>
    Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.sig`;
}

type FetchCall = { url: string; init: RequestInit };
let fetchCalls: FetchCall[];
let directory: string;

/**
 * Run one `device import-template` invocation and capture stdout.
 * @param args - arguments after `device`
 * @returns the captured stdout text
 */
async function run(args: string[]): Promise<string> {
  const program = new Command();
  program.exitOverride();
  registerDeviceCommand(program);
  let output = '';
  const spy = vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
    output += String(chunk);
    return true;
  });
  try {
    try {
      await program.parseAsync(['device', ...args], { from: 'user' });
    } catch (error) {
      if (!(error instanceof SilentExit)) throw error;
    }
  } finally {
    spy.mockRestore();
  }
  return output;
}

beforeEach(async () => {
  vi.clearAllMocks();
  fetchCalls = [];
  directory = await mkdtemp(join(tmpdir(), 'dc3-cli-template-'));
  mocks.getActiveProfile.mockResolvedValue({
    gateway: 'http://gw.test/',
    tenant: 'tenantA',
    username: 'admin',
  });
  mocks.getActiveProfileName.mockResolvedValue('default');
  mocks.getSettings.mockResolvedValue({ renewal_threshold_hours: 12 });
  mocks.getState.mockResolvedValue(null);
  mocks.needsRenewal.mockResolvedValue(false);
  mocks.resolvePassword.mockResolvedValue(null);
  const actual = await vi.importActual<typeof import('node:fs/promises')>('node:fs/promises');
  writeFileMock.mockReset();
  writeFileMock.mockImplementation(actual.writeFile);
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init?: RequestInit) => {
      fetchCalls.push({ url: String(url), init: init ?? {} });
      return new Response(TEMPLATE_BYTES, { status: 200 });
    }),
  );
});

afterEach(async () => {
  vi.unstubAllGlobals();
  await rm(directory, { recursive: true, force: true });
  process.exitCode = 0;
});

describe('device import-template (G24 authenticated binary seam)', () => {
  it('downloads through the authenticated client, writes the exact bytes, and prints the ok envelope', async () => {
    mocks.getState.mockResolvedValue({ ...loginState });
    const output = await run([
      'import-template',
      '--driver-id',
      '10',
      '--profile-id',
      '100',
      '--output',
      join(directory, 'template.xlsx'),
      '--format',
      'json',
    ]);

    expect(fetchCalls).toHaveLength(1);
    expect(fetchCalls[0].url).toBe(
      'http://gw.test/api/v3/manager/device/export/import_template',
    );
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({
      driverId: '10',
      profileId: '100',
    });
    // Auth headers travel with the binary request like any other call.
    const headers = fetchCalls[0].init.headers as Record<string, string>;
    expect(headers['X-Auth-Login']).toBe('admin');
    const written = await readFile(join(directory, 'template.xlsx'));
    expect(written.equals(TEMPLATE_BYTES)).toBe(true);
    expect(JSON.parse(output)).toEqual({
      ok: true,
      path: join(directory, 'template.xlsx'),
      size: TEMPLATE_BYTES.length,
    });
  });

  it('an expired token without a stored password fails as AuthError exit 3 — never an API_401 business failure', async () => {
    mocks.getState.mockResolvedValue({ ...loginState });
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        fetchCalls.push({ url: 'template', init: {} });
        return new Response(JSON.stringify({ detail: 'token expired' }), { status: 401 });
      }),
    );

    const failure = await run([
      'import-template',
      '--driver-id',
      '10',
      '--profile-id',
      '100',
      '--format',
      'json',
    ]).then(
      () => null,
      (error: Error & { kind?: string; exitCode?: number; code?: string }) => error,
    );

    expect(failure).toMatchObject({ kind: 'auth', exitCode: 3, code: 'AUTH' });
    expect(failure?.message).toBe('Authentication failed (401): token expired');
    // Negative guard: the pre-fix seam degraded this to kind api / API_401.
    expect(failure).not.toMatchObject({ kind: 'api' });
  });

  it('an expired token with a stored password self-heals via renewal and exits 0', async () => {
    mocks.getState.mockResolvedValue({ ...loginState });
    mocks.needsRenewal.mockResolvedValue(true);
    mocks.resolvePassword.mockResolvedValue('dc3dc3dc3');
    const freshToken = fakeJwt({ sub: '1', iat: 100, exp: 2_000_000_000 });
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        fetchCalls.push({ url: String(url), init: init ?? {} });
        if (String(url).endsWith('/token/salt')) {
          return new Response(JSON.stringify({ salt: 'salt-abc' }), { status: 200 });
        }
        if (String(url).endsWith('/token/generate')) {
          return new Response(JSON.stringify({ token: freshToken }), { status: 200 });
        }
        return new Response(TEMPLATE_BYTES, { status: 200 });
      }),
    );

    const output = await run([
      'import-template',
      '--driver-id',
      '10',
      '--profile-id',
      '100',
      '--output',
      join(directory, 'healed.xlsx'),
      '--format',
      'json',
    ]);

    // Renewal fired before the download (the pre-fix seam skipped it).
    expect(fetchCalls.map((call) => call.url)).toEqual([
      'http://gw.test/api/v3/auth/token/salt',
      'http://gw.test/api/v3/auth/token/generate',
      'http://gw.test/api/v3/manager/device/export/import_template',
    ]);
    expect(JSON.parse(output).ok).toBe(true);
  });

  it('a not-logged-in 401 surfaces as AuthError exit 3 without any renewal attempt', async () => {
    mocks.getState.mockResolvedValue(null);
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        fetchCalls.push({ url: String(url), init: init ?? {} });
        return new Response(JSON.stringify({ detail: 'missing X-Auth-Token header' }), {
          status: 401,
        });
      }),
    );

    const failure = await run([
      'import-template',
      '--driver-id',
      '10',
      '--profile-id',
      '100',
      '--format',
      'json',
    ]).then(
      () => null,
      (error: Error & { kind?: string; exitCode?: number }) => error,
    );

    expect(failure).toMatchObject({ kind: 'auth', exitCode: 3 });
    expect(fetchCalls.filter((call) => call.url.includes('/token/'))).toHaveLength(0);
  });

  it('rejects empty driver/profile ids before any request (G13/G27)', async () => {
    const failure = await run([
      'import-template',
      '--driver-id',
      '',
      '--profile-id',
      '100',
      '--format',
      'json',
    ]).then(
      () => null,
      (error: Error & { kind?: string }) => error,
    );
    expect(failure).toMatchObject({ kind: 'validation' });
    expect(fetchCalls).toHaveLength(0);
  });

  it('a write failure reports the user-supplied path as a validation error, never the resolved absolute (G29)', async () => {
    const outputArg = join(directory, 'blocked.xlsx');
    writeFileMock.mockRejectedValue(
      Object.assign(new Error(`EACCES: permission denied, open '${outputArg}'`), {
        code: 'EACCES',
      }),
    );

    const failure = await run([
      'import-template',
      '--driver-id',
      '10',
      '--profile-id',
      '100',
      '--output',
      outputArg,
      '--format',
      'json',
    ]).then(
      () => null,
      (error: Error & { kind?: string }) => error,
    );

    expect(failure).toMatchObject({ kind: 'validation', exitCode: 1 });
    expect(failure?.message).toBe(`Template output is not writable: ${outputArg}`);
  });

  it('an explicitly empty --output is honored and fails loudly instead of silently using the default (G32)', async () => {
    const failure = await run([
      'import-template',
      '--driver-id',
      '10',
      '--profile-id',
      '100',
      '--output',
      '',
      '--format',
      'json',
    ]).then(
      () => null,
      (error: Error & { kind?: string }) => error,
    );

    // writeFile('') fails ENOENT; the guard maps it to the user-supplied path.
    expect(failure).toMatchObject({ kind: 'validation' });
    expect(failure?.message).toBe('Template output path not found: ');
  });
});
