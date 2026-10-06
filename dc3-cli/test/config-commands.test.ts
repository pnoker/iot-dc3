import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { existsSync } from 'node:fs';
import { mkdir, mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, dirname, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Command } from 'commander';

/*
 * Command-level guards for the config tree (reports F007/F016): profile
 * creation closing the README multi-profile gap, the reset wiping every local
 * auth store, and a snapshot scan asserting every "Run: dc3 ..." remediation
 * string in src points at a registered command (kills the whole dangling-
 * remediation class, not just the observed 'config init' instance).
 */

const osStub = vi.hoisted(() => ({ home: '' }));

vi.mock('node:os', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:os')>();
  return {
    ...actual,
    homedir: () => (osStub.home ? osStub.home : actual.homedir()),
  };
});

const keychainMock = vi.hoisted(() => ({
  available: false,
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
    async savePassword(): Promise<void> {}
    async deletePassword(identifier: string): Promise<void> {
      keychainMock.deleted.push(identifier);
    }
  },
}));

/** Captured result of one command run. */
interface RunResult {
  stdout: string;
  stderr: string;
  code: number;
  error?: { name?: string; message?: string };
}

/**
 * Run the config command tree in-process against the current temp HOME.
 * @param args - arguments after `config`
 * @returns captured streams, effective exit code, and escaped error shape
 */
async function runConfig(args: string[]): Promise<RunResult> {
  vi.resetModules();
  const { registerConfigCommand } = await import('../src/commands/config.js');
  const { classifyError } = await import('../src/core/errors.js');
  const program = new Command();
  program.exitOverride();
  program.configureOutput({ writeOut: () => undefined, writeErr: () => undefined });
  registerConfigCommand(program);

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
  const prevExitCode = process.exitCode;
  process.exitCode = undefined;

  let result: RunResult;
  try {
    await program.parseAsync(['config', ...args], { from: 'user' });
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

describe('config profile create (F007)', () => {
  let home: string;
  let configPath: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-profile-'));
    osStub.home = home;
    vi.resetModules();
    configPath = join(home, '.dc3', 'config.json');
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  it('creates a profile with flags, then the README use/get flow works on it', async () => {
    const created = await runConfig([
      'profile',
      'create',
      'prod',
      '--gateway',
      'https://iot.example.com',
      '--username',
      'admin',
    ]);
    expect(created.code, created.stderr).toBe(0);

    const stored = JSON.parse(await readFile(configPath, 'utf8'));
    // Omitted fields keep their schema defaults (partial bootstrap is legal).
    expect(stored.profiles.prod).toMatchObject({
      gateway: 'https://iot.example.com',
      username: 'admin',
      tenant: 'default',
      credential_store: 'keychain',
    });
    // Creating does not steal the active profile…
    expect(stored.current_profile).toBe('default');

    const switched = await runConfig(['profile', 'use', 'prod']);
    expect(switched.code, switched.stderr).toBe(0);
    const got = await runConfig(['get', 'username']);
    expect(got.code, got.stderr).toBe(0);
    expect(got.stdout.trim()).toBe('"admin"');
  });

  it('--switch activates the new profile immediately', async () => {
    const created = await runConfig([
      'profile',
      'create',
      'staging',
      '--gateway',
      'http://127.0.0.1:8000',
      '--switch',
    ]);
    expect(created.code, created.stderr).toBe(0);
    const stored = JSON.parse(await readFile(configPath, 'utf8'));
    expect(stored.current_profile).toBe('staging');
  });

  it('refuses to create a profile that already exists', async () => {
    await runConfig(['profile', 'create', 'prod', '--username', 'admin']);
    const duplicate = await runConfig(['profile', 'create', 'prod', '--username', 'other']);

    expect(duplicate.code).toBe(1);
    expect(duplicate.error?.message).toMatch(/already exists/u);
    // The existing profile was not overwritten.
    const stored = JSON.parse(await readFile(configPath, 'utf8'));
    expect(stored.profiles.prod.username).toBe('admin');
  });

  it('invalid values fail as one readable line with nothing persisted', async () => {
    const bad = await runConfig(['profile', 'create', 'prod', '--gateway', 'not-a-url']);

    expect(bad.code).toBe(1);
    expect(bad.error?.message).toMatch(/^Invalid value for gateway/u);
    expect(bad.error?.message).not.toMatch(/"code"/u);
    expect(bad.error?.message).not.toMatch(/\[\{/u);
    // The rejected write persisted nothing at all on a fresh install.
    expect(existsSync(configPath)).toBe(false);
  });

  it('rejects an invalid --store before touching the config', async () => {
    const bad = await runConfig(['profile', 'create', 'prod', '--store', 'gcp']);
    expect(bad.code).toBe(1);
    expect(bad.error?.name).toBe('UsageError');
    expect(bad.error?.message).toMatch(/--store/u);
  });

  it('profile use on a missing profile fails listing what is available', async () => {
    await runConfig(['profile', 'create', 'prod', '--username', 'admin']);

    const result = await runConfig(['profile', 'use', 'ghost']);

    expect(result.code).toBe(1);
    expect(result.error?.name).toBe('ValidationError');
    expect(result.error?.message).toMatch(/not found\. Available: prod/u);
    // The active profile did not move.
    const stored = JSON.parse(await readFile(configPath, 'utf8'));
    expect(stored.current_profile).toBe('default');
  });

  it('profile delete refuses the active profile and unknown names, deleting nothing', async () => {
    await runConfig(['profile', 'create', 'prod', '--username', 'admin']);
    await runConfig(['profile', 'use', 'prod']);

    const active = await runConfig(['profile', 'delete', 'prod']);
    expect(active.code).toBe(1);
    expect(active.error?.name).toBe('ValidationError');
    expect(active.error?.message).toMatch(/Cannot delete active profile "prod"/u);

    const missing = await runConfig(['profile', 'delete', 'ghost']);
    expect(missing.code).toBe(1);
    expect(missing.error?.name).toBe('ValidationError');
    expect(missing.error?.message).toMatch(/not found/u);

    // Negative guard: both refusals left the config untouched.
    const stored = JSON.parse(await readFile(configPath, 'utf8'));
    expect(stored.profiles.prod).toBeDefined();
    expect(stored.current_profile).toBe('prod');
  });
});

describe('config set/get key contract (F036 + F015 residual)', () => {
  let home: string;
  let configPath: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-setget-'));
    osStub.home = home;
    vi.resetModules();
    configPath = join(home, '.dc3', 'config.json');
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  /** Bootstrap a usable default profile (username first, then the gateway). */
  async function seedProfile(): Promise<void> {
    await runConfig(['set', 'username', 'admin']);
    await runConfig(['set', 'gateway', 'http://127.0.0.1:9400']);
  }

  it('set and get accept the optional settings. prefix symmetrically', async () => {
    await seedProfile();

    const prefixed = await runConfig(['set', 'settings.renewal_threshold_hours', '2']);
    expect(prefixed.code, prefixed.stderr).toBe(0);
    const bare = await runConfig(['set', 'renewal_threshold_hours', '3']);
    expect(bare.code, bare.stderr).toBe(0);

    const viaPrefix = await runConfig(['get', 'settings.renewal_threshold_hours']);
    const viaBare = await runConfig(['get', 'renewal_threshold_hours']);
    expect(viaPrefix.code, viaPrefix.stderr).toBe(0);
    expect(viaBare.code, viaBare.stderr).toBe(0);
    expect(viaPrefix.stdout).toBe(viaBare.stdout);
    expect(viaBare.stdout.trim()).toBe('3');
  });

  it('boolean settings accept exactly "true"/"false" — never a truthiness coercion', async () => {
    await seedProfile();
    const before = await readFile(configPath, 'utf8');

    const bad = await runConfig(['set', 'settings.color', 'maybe']);

    expect(bad.code).toBe(1);
    expect(bad.error?.name).toBe('ValidationError');
    expect(bad.error?.message).toMatch(/settings\.color/u);
    expect(bad.error?.message).toMatch(/maybe/u);
    // Nothing was persisted: the config file is byte-identical.
    expect(await readFile(configPath, 'utf8')).toBe(before);

    // Both spellings accept the strict boolean lexemes.
    const setBare = await runConfig(['set', 'color', 'false']);
    expect(setBare.code, setBare.stderr).toBe(0);
    const got = await runConfig(['get', 'settings.color']);
    expect(got.code, got.stderr).toBe(0);
    expect(got.stdout.trim()).toBe('false');
  });

  it('unknown keys on set and get fail as typed validation errors with nothing persisted', async () => {
    await seedProfile();
    const before = await readFile(configPath, 'utf8');

    for (const key of ['nosuch.key', 'settings.nosuch']) {
      const bad = await runConfig(['set', key, 'x']);
      expect(bad.code).toBe(1);
      expect(bad.error?.name).toBe('ValidationError');
      expect(bad.error?.message).toMatch(/Unknown config key/u);
    }
    const badGet = await runConfig(['get', 'nosuch.key']);
    expect(badGet.code).toBe(1);
    expect(badGet.error?.name).toBe('ValidationError');

    expect(await readFile(configPath, 'utf8')).toBe(before);
  });

  it('the removed retry_count knob is an unknown key on set and get, persisting nothing', async () => {
    // retry_count was settable and persisted but consumed by nothing; it is
    // removed from the settings surface, so both spellings must refuse it.
    await seedProfile();
    const before = await readFile(configPath, 'utf8');

    const badSet = await runConfig(['set', 'retry_count', '2']);
    expect(badSet.code).toBe(1);
    expect(badSet.error?.name).toBe('ValidationError');
    expect(badSet.error?.message).toMatch(/Unknown config key/u);

    const badSetPrefixed = await runConfig(['set', 'settings.retry_count', '2']);
    expect(badSetPrefixed.code).toBe(1);
    expect(badSetPrefixed.error?.name).toBe('ValidationError');
    expect(badSetPrefixed.error?.message).toMatch(/Unknown config key/u);

    const badGet = await runConfig(['get', 'retry_count']);
    expect(badGet.code).toBe(1);
    expect(badGet.error?.name).toBe('ValidationError');
    expect(badGet.error?.message).toMatch(/Unknown config key/u);

    expect(await readFile(configPath, 'utf8')).toBe(before);
  });

  it('invalid settings values (integer, output format) fail with nothing persisted', async () => {
    await seedProfile();
    const before = await readFile(configPath, 'utf8');

    const badInt = await runConfig(['set', 'renewal_threshold_hours', 'x']);
    expect(badInt.code).toBe(1);
    expect(badInt.error?.name).toBe('ValidationError');
    expect(badInt.error?.message).toMatch(/renewal_threshold_hours/u);

    const badFormat = await runConfig(['set', 'settings.output_format', 'xml']);
    expect(badFormat.code).toBe(1);
    expect(badFormat.error?.name).toBe('ValidationError');
    expect(badFormat.error?.message).toMatch(/output_format/u);

    expect(await readFile(configPath, 'utf8')).toBe(before);
  });
});

describe('config reset clears every local auth store (F016)', () => {
  let home: string;
  let configPath: string;
  let tokensPath: string;
  let credentialsPath: string;
  let keyPath: string;
  let bakPath: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-resetcmd-'));
    osStub.home = home;
    vi.resetModules();
    configPath = join(home, '.dc3', 'config.json');
    tokensPath = join(home, '.dc3', 'tokens.json');
    credentialsPath = join(home, '.dc3', 'credentials.enc');
    keyPath = join(home, '.dc3', 'credentials.key');
    bakPath = `${configPath}.bak`;
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  /** Seed a logged-in machine: config profile, backup, live token, stored password. */
  async function seedLoggedInMachine(): Promise<void> {
    await mkdir(join(home, '.dc3'), { recursive: true });
    const config = JSON.stringify({
      version: 1,
      current_profile: 'default',
      settings: {},
      profiles: {
        default: { gateway: 'http://127.0.0.1:9400', username: 'admin', credential_store: 'encrypted' },
      },
    });
    await writeFile(configPath, config, 'utf8');
    // The last-good backup the config manager maintains on every mutation.
    await writeFile(bakPath, config, 'utf8');
    const { tokenManager } = await import('../src/core/token-manager.js');
    await tokenManager.saveState(
      {
        token: 'tok-reset-cmd',
        salt: 'salt',
        tenant: 'tenantA',
        username: 'admin',
        issuedAt: 1,
        expiresAt: 2_000_000_000,
      },
      'default',
    );
    const { EncryptedFileStore } = await import('../src/core/credential-encrypted.js');
    await new EncryptedFileStore().savePassword('admin@tenantA', 'plain-pw');
    expect(existsSync(tokensPath)).toBe(true);
    expect(existsSync(credentialsPath)).toBe(true);
    expect(existsSync(keyPath)).toBe(true);
    expect(existsSync(bakPath)).toBe(true);
  }

  it('reset --yes wipes config, tokens, backup, ciphertext, and AES key in one call', async () => {
    await seedLoggedInMachine();

    const result = await runConfig(['reset', '--yes']);

    expect(result.code, result.stderr).toBe(0);
    // The confirmation text matches reality: everything local is gone.
    expect(existsSync(tokensPath)).toBe(false);
    expect(existsSync(credentialsPath)).toBe(false);
    expect(existsSync(keyPath)).toBe(false);
    // The pre-reset backup is part of the wiped state, not a keepsake.
    expect(existsSync(bakPath)).toBe(false);
    const stored = JSON.parse(await readFile(configPath, 'utf8'));
    expect(stored.profiles).toEqual({});
    const { tokenManager } = await import('../src/core/token-manager.js');
    expect(await tokenManager.getAllStates()).toEqual({});
  });

  it('reset without --yes on non-interactive stdin refuses and changes nothing', async () => {
    await seedLoggedInMachine();
    const before = await readFile(configPath, 'utf8');

    const result = await runConfig(['reset']);

    expect(result.code).toBe(1);
    expect(result.error?.name).toBe('UsageError');
    expect(result.error?.message).toMatch(/--yes/u);
    // Negative guard: not a single state file moved.
    expect((await readFile(configPath, 'utf8')).trim()).toBe(before.trim());
    expect(existsSync(tokensPath)).toBe(true);
    expect(existsSync(credentialsPath)).toBe(true);
    expect(existsSync(keyPath)).toBe(true);
    expect(existsSync(bakPath)).toBe(true);
  });
});

describe('profile list --device-id: unpaged endpoint contract', () => {
  let home: string;
  let fetchCalls: Array<{ url: string; method?: string }>;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-profelist-'));
    osStub.home = home;
    vi.resetModules();
    fetchCalls = [];
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
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        fetchCalls.push({ url: String(url), method: init?.method });
        return new Response(JSON.stringify([{ id: 1, profileName: 'p1' }]), { status: 200 });
      }),
    );
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  /**
   * Run the profile ENTITY command tree (device templates, not config
   * profiles) in-process against the current temp HOME.
   * @param args - arguments after `profile`
   * @returns captured streams, effective exit code, and escaped error shape
   */
  async function runProfile(args: string[]): Promise<RunResult> {
    vi.resetModules();
    const { registerProfileCommand } = await import('../src/commands/profile.js');
    const { classifyError } = await import('../src/core/errors.js');
    const program = new Command();
    program.exitOverride();
    program.configureOutput({ writeOut: () => undefined, writeErr: () => undefined });
    registerProfileCommand(program);

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
    const prevExitCode = process.exitCode;
    process.exitCode = undefined;

    let result: RunResult;
    try {
      await program.parseAsync(['profile', ...args], { from: 'user' });
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

  it('--device-id issues the unpaged GET list_by_device_id endpoint', async () => {
    const result = await runProfile(['list', '--device-id', 'dev 42']);

    expect(result.code, result.stderr).toBe(0);
    expect(fetchCalls).toHaveLength(1);
    expect(fetchCalls[0].method).toBe('GET');
    expect(fetchCalls[0].url).toBe(
      'http://127.0.0.1:9400/api/v3/manager/profile/list_by_device_id?device_id=dev%2042',
    );
    // The paged POST endpoint is never touched for a device filter.
    expect(fetchCalls.some((call) => call.url.endsWith('/api/v3/manager/profile/list'))).toBe(false);
    expect(JSON.parse(result.stdout)[0].profileName).toBe('p1');
  });

  it('--device-id with --limit is a typed validation error before any request', async () => {
    const result = await runProfile(['list', '--device-id', 'd1', '--limit', '5']);

    expect(result.code).toBe(1);
    expect(result.error?.name).toBe('ValidationError');
    expect(result.error?.message).toMatch(/--offset\/--limit cannot be combined with --device-id/u);
    // Explicit paging flags are never silently dropped on the floor.
    expect(fetchCalls).toHaveLength(0);
  });

  it('--device-id with --offset is rejected the same way', async () => {
    const result = await runProfile(['list', '--device-id', 'd1', '--offset', '0']);
    expect(result.code).toBe(1);
    expect(result.error?.message).toMatch(/cannot be combined with --device-id/u);
    expect(fetchCalls).toHaveLength(0);
  });
});

describe('every "dc3 ..." remediation string points at a registered command (F007)', () => {
  const srcRoot = join(dirname(fileURLToPath(import.meta.url)), '..', 'src');

  /**
   * Walk the command tree from a token sequence, consuming matching children.
   * @param program - command tree root to resolve from
   * @param tokens - command path segments (flags and placeholders stop the walk)
   * @returns how far the walk got, where it stopped, and whether it reached a leaf
   */
  function resolveReference(
    program: Command,
    tokens: string[],
  ): { consumed: string[]; stoppedAt: string | null; nodeIsLeaf: boolean } {
    let node: Command = program;
    const consumed: string[] = [];
    for (const token of tokens) {
      if (token.startsWith('-') || token.startsWith('<')) break;
      const next = node.commands.find(
        (child) => child.name() === token || child.aliases().includes(token),
      );
      if (!next) {
        return { consumed, stoppedAt: token, nodeIsLeaf: node.commands.length === 0 };
      }
      consumed.push(token);
      node = next;
    }
    return { consumed, stoppedAt: null, nodeIsLeaf: node.commands.length === 0 };
  }

  async function collectTsFiles(dir: string): Promise<string[]> {
    const entries = await readdir(dir, { withFileTypes: true });
    const nested = await Promise.all(
      entries.map(async (entry) => {
        const full = join(dir, entry.name);
        if (entry.isDirectory()) return collectTsFiles(full);
        return entry.name.endsWith('.ts') ? [full] : [];
      }),
    );
    return nested.flat();
  }

  it('no remediation message in src references a dangling command', async () => {
    vi.resetModules();
    const { buildProgram } = await import('../src/index.js');
    const program = buildProgram();

    const files = await collectTsFiles(srcRoot);
    expect(files.length).toBeGreaterThan(20);
    const references: string[] = [];
    for (const file of files) {
      const rel = relative(srcRoot, file).split(sep).join('/');
      const source = await readFile(file, 'utf8');
      // Remediation hints: "Run: dc3 ...", "Create it with: dc3 ...",
      // "Set it with: dc3 ...", "(dc3 ... for scripts)".
      const pattern = /(?::|\()\s*dc3\s+([^"'\n)]+)/gu;
      for (const match of source.matchAll(pattern)) {
        references.push(`${rel}: dc3 ${match[1].trim()}`);
      }
    }
    // The scan itself must be live (these classes of hints exist in src).
    expect(references.length).toBeGreaterThan(3);

    const failures: string[] = [];
    for (const reference of references) {
      const tokens = reference
        .slice(reference.indexOf('dc3 ') + 4)
        .split(/\s+/u)
        .filter(Boolean)
        // Template literals leave trailing punctuation on the last token.
        .map((token) => token.replace(/[`.,;]+$/u, ''));
      const { consumed, stoppedAt, nodeIsLeaf } = resolveReference(program, tokens);
      if (consumed.length === 0) {
        failures.push(`${reference} -> resolves no command segment`);
        continue;
      }
      // A bare token that matches no subcommand is only legal when the walk
      // already reached a leaf command (it is that command's argument).
      if (stoppedAt !== null && !nodeIsLeaf) {
        failures.push(`${reference} -> dangling segment '${stoppedAt}'`);
      }
    }
    expect(failures, `dangling remediation references:\n${failures.join('\n')}`).toEqual([]);
  });
});
