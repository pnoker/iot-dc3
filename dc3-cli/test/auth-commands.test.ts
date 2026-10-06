import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { existsSync } from 'node:fs';
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Command } from 'commander';

/*
 * Command-level guards for the auth tree (reports F004/F005/F008/F033/F037/
 * F049/F051/F025). Real config/token/credential managers run against a temp
 * HOME; fetch and the OS keychain are stubbed; commander runs through the
 * exitOverride seam like the other command tests.
 */

const osStub = vi.hoisted(() => ({ home: '' }));

vi.mock('node:os', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:os')>();
  return {
    ...actual,
    homedir: () => (osStub.home ? osStub.home : actual.homedir()),
  };
});

// The real keychain shells out to OS tools (seconds-long on stock Windows);
// a controllable double keeps these tests fast and deterministic.
const keychainMock = vi.hoisted(() => ({
  available: false,
  saved: [] as Array<{ identifier: string; password: string }>,
  deleted: [] as string[],
}));

vi.mock('../src/core/credential-keychain.js', () => ({
  KeychainStore: class {
    readonly name = 'keychain';
    async isAvailable(): Promise<boolean> {
      return keychainMock.available;
    }
    async getPassword(): Promise<string | null> {
      return null;
    }
    async savePassword(identifier: string, password: string): Promise<void> {
      keychainMock.saved.push({ identifier, password });
    }
    async deletePassword(identifier: string): Promise<void> {
      keychainMock.deleted.push(identifier);
    }
  },
}));

function fakeJwt(payload: Record<string, unknown>): string {
  const encode = (value: Record<string, unknown>) =>
    Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.sig`;
}

const NOW = Math.floor(Date.now() / 1000);
const VALID_TOKEN = fakeJwt({ sub: '1', iat: NOW, exp: NOW + 7200 });

/** Captured result of one command run. */
interface RunResult {
  stdout: string;
  stderr: string;
  code: number;
  error?: { name?: string; message?: string };
}

describe('auth login: input sources, store selection, and reporting', () => {
  let home: string;
  let configPath: string;
  let tokensPath: string;
  let fetchCalls: Array<{ url: string; body: Record<string, unknown> }>;
  let prevExitCode: number | string | undefined;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-auth-'));
    osStub.home = home;
    keychainMock.available = false;
    keychainMock.saved = [];
    keychainMock.deleted = [];
    delete process.env.DC3_PASSWORD;
    fetchCalls = [];
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        fetchCalls.push({
          url: String(url),
          body: init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : {},
        });
        if (String(url).endsWith('/token/salt')) {
          return new Response('salt-abc', { status: 200 });
        }
        return new Response(VALID_TOKEN, { status: 200 });
      }),
    );
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  /**
   * Seed a default profile so consumption (getActiveProfile) succeeds; the
   * login action then overwrites the identity fields with the login values.
   */
  async function seedProfile(overrides: Record<string, unknown> = {}): Promise<void> {
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(
      join(home, '.dc3', 'config.json'),
      JSON.stringify({
        version: 1,
        current_profile: 'default',
        settings: {},
        profiles: {
          default: { gateway: 'http://127.0.0.1:9400', username: 'seed-user', ...overrides },
        },
      }),
      'utf8',
    );
  }

  /** Run the auth command tree in-process against the current temp HOME. */
  async function runAuth(args: string[]): Promise<RunResult> {
    vi.resetModules();
    const { registerAuthCommand } = await import('../src/commands/auth.js');
    const { classifyError } = await import('../src/core/errors.js');
    const program = new Command();
    program.exitOverride();
    program.configureOutput({ writeOut: () => undefined, writeErr: () => undefined });
    registerAuthCommand(program);

    let stdout = '';
    let stderr = '';
    vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
      stdout += String(chunk);
      return true;
    });
    vi.spyOn(process.stderr, 'write').mockImplementation((chunk) => {
      stderr += String(chunk);
      return true;
    });
    vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
      stderr += args.join(' ');
    });
    prevExitCode = process.exitCode;
    process.exitCode = undefined;

    let result: RunResult = { stdout, stderr, code: 0 };
    try {
      await program.parseAsync(['auth', ...args], { from: 'user' });
      result = { stdout, stderr, code: process.exitCode ?? 0 };
    } catch (error) {
      if ((error as Error)?.name === 'SilentExit') {
        result = { stdout, stderr, code: process.exitCode ?? 0 };
      } else {
        result = {
          stdout,
          stderr,
          code: classifyError(error as Error).exitCode,
          error: error as { name?: string; message?: string },
        };
      }
    }
    process.exitCode = prevExitCode;
    return result;
  }

  async function readStoredConfig(): Promise<Record<string, any>> {
    return JSON.parse(await readFile(join(home, '.dc3', 'config.json'), 'utf8'));
  }

  it('logs in headless from DC3_PASSWORD with closed stdin semantics (F033)', async () => {
    await seedProfile({ credential_store: 'env' });
    vi.stubEnv('DC3_PASSWORD', 'env-pw');

    const result = await runAuth(['login', '-t', 'tenantA', '-u', 'admin', '--store', 'env']);

    expect(result.code, result.stderr).toBe(0);
    expect(fetchCalls).toHaveLength(2);
    expect(fetchCalls[1].body).toMatchObject({ name: 'admin', tenant: 'tenantA', password: 'env-pw' });
    const payload = JSON.parse(result.stdout);
    expect(payload.ok).toBe(true);
    expect(payload.password_saved).toBe(true);
    // The token state was persisted for the active profile.
    const tokens = JSON.parse(await readFile(join(home, '.dc3', 'tokens.json'), 'utf8'));
    expect(tokens.states.default.username).toBe('admin');
    expect(tokens.states.default.tenant).toBe('tenantA');
    // env store never writes a credential file.
    expect(existsSync(join(home, '.dc3', 'credentials.enc'))).toBe(false);
  });

  it('missing required input on non-interactive stdin fails fast with zero requests (F004)', async () => {
    await seedProfile();
    const configBefore = await readFile(join(home, '.dc3', 'config.json'), 'utf8');

    const result = await runAuth(['login', '-t', 'tenantA', '-u', 'admin']);

    expect(result.code).toBe(1);
    expect(result.error?.name).toBe('UsageError');
    expect(result.error?.message).toMatch(/--password/u);
    expect(result.error?.message).toMatch(/DC3_PASSWORD/u);
    // Negative guards: no wire traffic, no state writes at all.
    expect(fetchCalls).toHaveLength(0);
    expect(existsSync(join(home, '.dc3', 'tokens.json'))).toBe(false);
    expect((await readFile(join(home, '.dc3', 'config.json'), 'utf8')).trim()).toBe(
      configBefore.trim(),
    );
  });

  it('missing tenant on non-interactive stdin is a usage error naming the flag (F004)', async () => {
    await seedProfile();
    const result = await runAuth(['login', '-u', 'admin', '-p', 'pw']);
    expect(result.code).toBe(1);
    expect(result.error?.message).toMatch(/--tenant/u);
    expect(fetchCalls).toHaveLength(0);
  });

  it('--no-save never persists the password and records the prompt store (F008)', async () => {
    await seedProfile({ credential_store: 'encrypted' });

    const result = await runAuth([
      'login',
      '-t',
      'tenantA',
      '-u',
      'admin',
      '-p',
      'plain-pw',
      '--no-save',
    ]);

    expect(result.code, result.stderr).toBe(0);
    // Password not saved anywhere — the explicit opt-out must hold even with a
    // configured, available store.
    expect(existsSync(join(home, '.dc3', 'credentials.enc'))).toBe(false);
    const config = await readStoredConfig();
    expect(config.profiles.default.credential_store).toBe('prompt');
    const payload = JSON.parse(result.stdout);
    expect(payload.password_saved).toBe(false);
  });

  it('a plain login keeps the pre-configured store instead of silently downgrading it (F008)', async () => {
    await seedProfile({ credential_store: 'encrypted' });

    const result = await runAuth(['login', '-t', 'tenantA', '-u', 'admin', '-p', 'plain-pw']);

    expect(result.code, result.stderr).toBe(0);
    const config = await readStoredConfig();
    expect(config.profiles.default.credential_store).toBe('encrypted');
    // The password landed in the retained store.
    expect(existsSync(join(home, '.dc3', 'credentials.enc'))).toBe(true);
    const payload = JSON.parse(result.stdout);
    expect(payload.password_saved).toBe(true);
    expect(payload.credential_store).toBe('encrypted');
  });

  it('an explicit --store wins over the configured one (F008)', async () => {
    await seedProfile({ credential_store: 'encrypted' });

    const result = await runAuth([
      'login',
      '-t',
      'tenantA',
      '-u',
      'admin',
      '-p',
      'plain-pw',
      '--store',
      'keychain',
    ]);

    expect(result.code, result.stderr).toBe(0);
    const config = await readStoredConfig();
    expect(config.profiles.default.credential_store).toBe('keychain');
    expect(existsSync(join(home, '.dc3', 'credentials.enc'))).toBe(false);
  });

  it('rejects an invalid --store before any request (usage error)', async () => {
    await seedProfile();
    const result = await runAuth(['login', '-t', 't', '-u', 'u', '-p', 'pw', '--store', 'gcp']);
    expect(result.code).toBe(1);
    expect(result.error?.name).toBe('UsageError');
    expect(result.error?.message).toMatch(/--store/u);
    expect(fetchCalls).toHaveLength(0);
  });

  it('reports password_saved:false with a loud warning when the store did not persist (F005)', async () => {
    await seedProfile({ credential_store: 'keychain' }); // keychain unavailable in this fixture

    const result = await runAuth(['login', '-t', 'tenantA', '-u', 'admin', '-p', 'plain-pw']);

    expect(result.code, result.stderr).toBe(0);
    const payload = JSON.parse(result.stdout);
    expect(payload.ok).toBe(true);
    expect(payload.password_saved).toBe(false);
    // Loud, actionable warning — never a silent non-persist.
    expect(result.stderr).toMatch(/NOT saved/u);
    expect(result.stderr).toMatch(/keychain/u);
    expect(result.stderr).toMatch(/renewal/u);
  });

  it('expires_at is ISO-8601 and denotes the same instant as the persisted state (F049)', async () => {
    await seedProfile();
    const result = await runAuth(['login', '-t', 'tenantA', '-u', 'admin', '-p', 'pw']);

    expect(result.code, result.stderr).toBe(0);
    const payload = JSON.parse(result.stdout);
    expect(payload.expires_at).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/u);
    const tokens = JSON.parse(await readFile(join(home, '.dc3', 'tokens.json'), 'utf8'));
    expect(new Date(payload.expires_at).getTime()).toBe(tokens.states.default.expiresAt * 1000);
    // No locale-formatted timestamp anywhere in the machine output.
    expect(result.stdout).not.toMatch(/\d{4}\/\d{1,2}\/\d{1,2} \d{1,2}:\d{2}:\d{2}/u);
  });

  it('auth login --help lists --oauth (F051)', async () => {
    vi.resetModules();
    const { registerAuthCommand } = await import('../src/commands/auth.js');
    const program = new Command();
    registerAuthCommand(program);
    const login = program.commands.find((c) => c.name() === 'auth')!.commands.find((c) => c.name() === 'login')!;
    const help = login.helpInformation();
    expect(help).toContain('--oauth');
    expect(login.options.find((o) => o.long === '--oauth')?.hidden ?? false).toBe(false);
  });

  it('--oauth without flags on non-interactive stdin names the missing option (F004)', async () => {
    await seedProfile();
    const result = await runAuth(['login', '--oauth']);
    expect(result.code).toBe(1);
    expect(result.error?.message).toMatch(/--client-id/u);
    expect(fetchCalls).toHaveLength(0);
  });
});

describe('auth logout: guaranteed local cleanup (F025)', () => {
  let home: string;
  let prevExitCode: number | string | undefined;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-logout-'));
    osStub.home = home;
    // Fresh modules BEFORE seeding: the managers bake their file paths at
    // import time, so they must load against this test's temp HOME.
    vi.resetModules();
    keychainMock.available = false;
    keychainMock.saved = [];
    keychainMock.deleted = [];
    delete process.env.DC3_PASSWORD;
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  /** Seed profile + token state + an encrypted credential for admin@tenantA. */
  async function seedLoggedIn(): Promise<void> {
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(
      join(home, '.dc3', 'config.json'),
      JSON.stringify({
        version: 1,
        current_profile: 'default',
        settings: {},
        profiles: {
          default: { gateway: 'http://127.0.0.1:9400', username: 'admin', credential_store: 'encrypted' },
        },
      }),
      'utf8',
    );
    const { tokenManager } = await import('../src/core/token-manager.js');
    await tokenManager.saveState(
      {
        token: VALID_TOKEN,
        salt: 'salt-abc',
        tenant: 'tenantA',
        username: 'admin',
        issuedAt: NOW,
        expiresAt: NOW + 7200,
      },
      'default',
    );
    const { EncryptedFileStore } = await import('../src/core/credential-encrypted.js');
    await new EncryptedFileStore().savePassword('admin@tenantA', 'plain-pw');
  }

  async function runLogout(): Promise<RunResult> {
    vi.resetModules();
    const { registerAuthCommand } = await import('../src/commands/auth.js');
    const { classifyError } = await import('../src/core/errors.js');
    const program = new Command();
    program.exitOverride();
    program.configureOutput({ writeOut: () => undefined, writeErr: () => undefined });
    registerAuthCommand(program);

    let stdout = '';
    let stderr = '';
    vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
      stdout += String(chunk);
      return true;
    });
    vi.spyOn(process.stderr, 'write').mockImplementation((chunk) => {
      stderr += String(chunk);
      return true;
    });
    prevExitCode = process.exitCode;
    process.exitCode = undefined;
    let result: RunResult = { stdout, stderr, code: 0 };
    try {
      await program.parseAsync(['auth', 'logout'], { from: 'user' });
      result = { stdout, stderr, code: process.exitCode ?? 0 };
    } catch (error) {
      if ((error as Error)?.name === 'SilentExit') {
        result = { stdout, stderr, code: process.exitCode ?? 0 };
      } else {
        result = {
          stdout,
          stderr,
          code: classifyError(error as Error).exitCode,
          error: error as { name?: string; message?: string },
        };
      }
    }
    process.exitCode = prevExitCode;
    return result;
  }

  it('network failure still clears local state, deletes the password, and warns about the survivor', async () => {
    await seedLoggedIn();
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('fetch failed');
      }),
    );

    const result = await runLogout();

    expect(result.code).toBe(2);
    expect(result.error?.name).toBe('NetworkError');
    // The user asked for destruction: token entry and password are gone locally…
    const { tokenManager } = await import('../src/core/token-manager.js');
    expect(await tokenManager.getState('default')).toBeNull();
    const { EncryptedFileStore } = await import('../src/core/credential-encrypted.js');
    expect(await new EncryptedFileStore().getPassword('admin@tenantA')).toBeNull();
    // …and the retention that DID happen (gateway-side token) is explicit.
    expect(result.stderr).toMatch(/could not be revoked remotely/u);
    expect(result.stderr).toMatch(/stays valid until it expires/u);
    expect(result.stderr).toMatch(/admin@tenantA/u);
  });

  it('a successful logout prints exactly one clean document', async () => {
    await seedLoggedIn();
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response(null, { status: 204 })),
    );

    const result = await runLogout();

    expect(result.code, result.stderr).toBe(0);
    const payload = JSON.parse(result.stdout);
    expect(payload).toEqual({ ok: true, message: 'Logged out successfully' });
    const { tokenManager } = await import('../src/core/token-manager.js');
    expect(await tokenManager.getState('default')).toBeNull();
  });
});

describe('identity mismatch warning and renewal keying, end-to-end (F037)', () => {
  let home: string;
  let stderr: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-mismatch-'));
    osStub.home = home;
    // Fresh modules for this test's temp HOME (paths bake at import time).
    vi.resetModules();
    keychainMock.available = false;
    delete process.env.DC3_PASSWORD;
    stderr = '';
    vi.spyOn(process.stderr, 'write').mockImplementation((chunk) => {
      stderr += String(chunk);
      return true;
    });
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  it('renewal keys the stored password by the token-state identity and warns about the drift', async () => {
    // Profile drifted after login: tenant switched tenantA -> tenantB.
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(
      join(home, '.dc3', 'config.json'),
      JSON.stringify({
        version: 1,
        current_profile: 'default',
        settings: {},
        profiles: {
          default: {
            gateway: 'http://127.0.0.1:9400',
            tenant: 'tenantB',
            username: 'bob',
            credential_store: 'encrypted',
          },
        },
      }),
      'utf8',
    );
    // …while the stored session (and password) still belong to admin@tenantA.
    const { tokenManager } = await import('../src/core/token-manager.js');
    const expiringSoon = fakeJwt({ sub: '1', iat: NOW, exp: NOW + 600 });
    await tokenManager.saveState(
      {
        token: expiringSoon,
        salt: 'old-salt',
        tenant: 'tenantA',
        username: 'admin',
        issuedAt: NOW,
        expiresAt: NOW + 600,
      },
      'default',
    );
    const { EncryptedFileStore } = await import('../src/core/credential-encrypted.js');
    await new EncryptedFileStore().savePassword('admin@tenantA', 'plain-pw');

    const fetchCalls: Array<{ url: string; body: Record<string, unknown> }> = [];
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        fetchCalls.push({
          url: String(url),
          body: init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : {},
        });
        if (String(url).endsWith('/token/salt')) return new Response('salt-new', { status: 200 });
        if (String(url).endsWith('/token/generate')) {
          return new Response(VALID_TOKEN, { status: 200 });
        }
        return new Response(JSON.stringify([]), { status: 200 });
      }),
    );

    const { dc3Client } = await import('../src/core/client.js');
    const data = await dc3Client.get('/api/v3/manager/device/list');

    // The request succeeded via silent renewal…
    expect(data).toEqual([]);
    const salt = fetchCalls.find((call) => call.url.endsWith('/token/salt'));
    // …keyed by the STORED identity, not the drifted profile.
    expect(salt?.body).toMatchObject({ name: 'admin', tenant: 'tenantA' });
    const renewed = await tokenManager.getState('default');
    expect(renewed?.token).toBe(VALID_TOKEN);
    expect(renewed?.tenant).toBe('tenantA');
    // The drift itself is no longer silent: the warning names both identities.
    expect(stderr).toMatch(/admin@tenantA/u);
    expect(stderr).toMatch(/bob@tenantB/u);
    expect(stderr).toMatch(/Warning: profile "default"/u);
  });
});
