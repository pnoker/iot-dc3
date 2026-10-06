import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { existsSync } from 'node:fs';
import { mkdir, mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Command } from 'commander';
import {
  ConfigSchema,
  ProfileConfigSchema,
  StoredProfileSchema,
  AppSettingsSchema,
} from '../src/core/config-manager';

// Redirect os.homedir() to a per-test temp dir so config-manager never touches the real HOME.
// The stub starts empty: static imports above then fall back to the real homedir (they only
// exercise the schemas, never the filesystem).
const osStub = vi.hoisted(() => ({ home: '' }));

vi.mock('node:os', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:os')>();
  return {
    ...actual,
    homedir: () => (osStub.home ? osStub.home : actual.homedir()),
  };
});

/** Sentinel thrown by the mocked process.exit so command actions stop like a real exit. */
class ExitSignal extends Error {
  constructor(readonly code: number) {
    super(`process.exit(${code})`);
  }
}

describe('config schemas', () => {
  it('ProfileConfigSchema should accept minimal config', () => {
    const result = ProfileConfigSchema.safeParse({
      username: 'test',
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.gateway).toBe('http://localhost:8000');
      expect(result.data.tenant).toBe('default');
      expect(result.data.credential_store).toBe('keychain');
    }
  });

  it('StoredProfileSchema rejects an empty username but allows bootstrap without one (F007)', () => {
    // Persistence accepts a partial profile (gateway-first bootstrap)…
    expect(StoredProfileSchema.safeParse({}).success).toBe(true);
    // …but an EXPLICITLY empty username is invalid input.
    expect(StoredProfileSchema.safeParse({ username: '' }).success).toBe(false);
  });

  it('ProfileConfigSchema should accept full config', () => {
    const result = ProfileConfigSchema.safeParse({
      gateway: 'https://example.com',
      tenant: 'production',
      username: 'admin',
      credential_store: 'env',
    });
    expect(result.success).toBe(true);
  });

  it('ProfileConfigSchema should reject invalid credential_store', () => {
    const result = ProfileConfigSchema.safeParse({
      username: 'test',
      credential_store: 'plaintext',
    });
    expect(result.success).toBe(false);
  });

  it('gateway accepts only bare http/https origins (F054)', () => {
    for (const bad of [
      'javascript:alert(1)',
      'ftp://127.0.0.1:1',
      'http://127.0.0.1:52478/base',
      'http://127.0.0.1:52478/?x=1',
      'not-a-url',
    ]) {
      expect(ProfileConfigSchema.safeParse({ username: 'u', gateway: bad }).success, bad).toBe(false);
    }
    for (const good of ['http://127.0.0.1:8000', 'https://example.com', 'http://127.0.0.1:8000/']) {
      expect(ProfileConfigSchema.safeParse({ username: 'u', gateway: good }).success, good).toBe(true);
    }
  });

  it('AppSettingsSchema should use defaults', () => {
    const result = AppSettingsSchema.safeParse({});
    expect(result.success).toBe(true);
    if (result.success) {
      // output_format stays unset so the TTY-aware default (table/json) applies.
      expect(result.data.output_format).toBeUndefined();
      expect(result.data.color).toBe(true);
      expect(result.data.renewal_threshold_hours).toBe(1);
    }
  });

  it('ConfigSchema should accept minimal config', () => {
    const result = ConfigSchema.safeParse({
      version: 1,
      profiles: {
        default: { username: 'test' },
      },
    });
    expect(result.success).toBe(true);
  });

  it('ConfigSchema should reject invalid version', () => {
    const result = ConfigSchema.safeParse({
      version: 2,
      profiles: {},
    });
    expect(result.success).toBe(false);
  });
});

describe('config file corruption handling', () => {
  let home: string;
  let configPath: string;

  /** Serialized partially-invalid config: renewal_threshold_hours is null (the out-of-range write bug). */
  const invalidFieldConfig = JSON.stringify(
    {
      version: 1,
      current_profile: 'default',
      settings: { renewal_threshold_hours: null },
      profiles: { default: { username: 'kept-in-salvage' } },
    },
    null,
    2,
  );

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-config-'));
    osStub.home = home;
    configPath = join(home, '.dc3', 'config.json');
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  async function writeConfigFile(content: string): Promise<void> {
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(configPath, content, 'utf8');
  }

  async function readBytes(path: string): Promise<Buffer> {
    return readFile(path);
  }

  /**
   * Run `config set` on a freshly imported CLI so the singleton config manager is rebuilt
   * against the current temp HOME. Captures stdout/stderr and the (mocked) exit code.
   * Non-exit errors propagate to the caller (the real CLI routes them to the fatal handler).
   * @param key - config key passed as the first argument
   * @param value - config value passed as the second argument
   * @returns captured stdout, stderr, and exit code
   */
  async function runSet(key: string, value: string): Promise<{ stdout: string; stderr: string; code: number }> {
    vi.resetModules();
    const { registerConfigCommand } = await import('../src/commands/config');
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
    // console.error is intercepted by vitest before it reaches process.stderr, so spy here.
    vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
      stderr += args.join(' ');
    });
    vi.spyOn(process, 'exit').mockImplementation(((code?: number) => {
      throw new ExitSignal(code ?? 0);
    }) as never);

    let code = 0;
    try {
      try {
        await program.parseAsync(['config', 'set', key, value], { from: 'user' });
      } catch (e) {
        if (e instanceof ExitSignal) {
          code = e.code;
        } else if (e?.name === 'SilentExit') {
          code = process.exitCode ?? 0;
        } else {
          throw e;
        }
      }
    } catch (error) {
      if (error instanceof ExitSignal) {
        code = error.code;
      } else {
        throw error;
      }
    }
    return { stdout, stderr, code };
  }

  it('config set rejects a non-numeric renewal_threshold_hours and leaves the file untouched', async () => {
    const original = validConfig();
    await writeConfigFile(original);

    const result = await runSet('settings.renewal_threshold_hours', 'abc');

    expect(result.code).toBe(1);
    expect(result.stdout).toContain('abc');
    expect((await readBytes(configPath)).equals(Buffer.from(original))).toBe(true);
    expect(existsSync(`${configPath}.bak`)).toBe(false);
  });

  it('config set rejects a non-numeric retry_count and leaves the file untouched', async () => {
    const original = validConfig();
    await writeConfigFile(original);

    const result = await runSet('settings.retry_count', 'xyz');

    expect(result.code).toBe(1);
    expect(result.stdout).toContain('xyz');
    expect((await readBytes(configPath)).equals(Buffer.from(original))).toBe(true);
    expect(existsSync(`${configPath}.bak`)).toBe(false);
  });

  it('config set rejects an out-of-range retry_count before persisting (F006)', async () => {
    const original = validConfig();
    await writeConfigFile(original);

    await expect(runSet('settings.retry_count', '99')).rejects.toThrow(/must be <= 3/u);
    // Negative guard: the file is byte-for-byte unchanged, no backup churn.
    expect((await readBytes(configPath)).equals(Buffer.from(original))).toBe(true);
    expect(existsSync(`${configPath}.bak`)).toBe(false);
  });

  it('config set rejects an out-of-range renewal_threshold_hours before persisting (F006)', async () => {
    const original = validConfig();
    await writeConfigFile(original);

    await expect(runSet('settings.renewal_threshold_hours', '99')).rejects.toThrow(/must be <= 12/u);
    expect((await readBytes(configPath)).equals(Buffer.from(original))).toBe(true);
  });

  it('a settings validation error is one readable line, never a zod issues dump (F006)', async () => {
    vi.resetModules();
    const { configManager } = await import('../src/core/config-manager');
    const err = await configManager.setSetting('retry_count', 99).catch((e: Error) => e);
    expect(err).toBeInstanceOf(Error);
    expect(err.message).toMatch(/^Invalid value for settings\.retry_count/u);
    expect(err.message).toMatch(/must be <= 3/u);
    expect(err.message).not.toMatch(/issues/u);
    expect(err.message).not.toMatch(/\[\{/u);
  });

  it('field-level invalid values are salvaged: valid profiles survive, defaults fill the rest (F006)', async () => {
    await writeConfigFile(invalidFieldConfig);

    vi.resetModules();
    const { configManager } = await import('../src/core/config-manager');
    let stderr = '';
    vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
      stderr += args.join(' ');
    });

    const config = await configManager.load();

    // The valid profile survives the out-of-range settings field…
    expect(config.profiles.default?.username).toBe('kept-in-salvage');
    // …the invalid field falls back to its default…
    expect(config.settings.renewal_threshold_hours).toBe(1);
    // …and the warning names the file and the invalid part.
    expect(stderr).toContain(configPath);
    expect(stderr).toContain('renewal_threshold_hours');
    // The file on disk is untouched by the read, and nothing is quarantined
    // (the bytes are parseable — quarantine is only for unrecognizable files).
    expect((await readBytes(configPath)).equals(Buffer.from(invalidFieldConfig))).toBe(true);
    const siblings = (await readdir(join(home, '.dc3'))).filter((n) =>
      n.startsWith('config.json.corrupt-'),
    );
    expect(siblings).toEqual([]);
  });

  it('a write after a salvaged load keeps the surviving profile (F006 negative)', async () => {
    await writeConfigFile(invalidFieldConfig);

    const result = await runSet('settings.retry_count', '2');

    expect(result.code).toBe(0);
    const persisted = JSON.parse((await readBytes(configPath)).toString('utf8'));
    expect(persisted.settings.retry_count).toBe(2);
    expect(persisted.settings.renewal_threshold_hours).toBe(1);
    // The whole point of salvage: the profile must survive the next write.
    expect(persisted.profiles.default.username).toBe('kept-in-salvage');
    // Last-readable bytes are snapshotted to .bak before overwriting.
    expect((await readBytes(`${configPath}.bak`)).equals(Buffer.from(invalidFieldConfig))).toBe(true);
  });

  it('a structurally corrupt config is quarantined and defaults are used in-memory (F027)', async () => {
    const corruptBytes = '{"version": 1, "profiles": {'; // truncated JSON
    await writeConfigFile(corruptBytes);

    vi.resetModules();
    const { configManager } = await import('../src/core/config-manager');
    let stderr = '';
    vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
      stderr += args.join(' ');
    });

    const config = await configManager.load();
    expect(config.settings.renewal_threshold_hours).toBe(1);
    expect(config.profiles).toEqual({});
    expect(stderr).toContain(configPath);
    expect(stderr).toContain('corrupt');
    // Corrupt bytes are rotated to a timestamped quarantine copy, NOT into .bak.
    const quarantined = (await readdir(join(home, '.dc3'))).filter((n) =>
      n.startsWith('config.json.corrupt-'),
    );
    expect(quarantined.length).toBe(1);
    expect(existsSync(`${configPath}.bak`)).toBe(false);
    expect((await readBytes(join(home, '.dc3', quarantined[0]))).equals(Buffer.from(corruptBytes))).toBe(true);
  });
});

describe('fresh-install bootstrap (F007)', () => {
  let home: string;
  let configPath: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-boot-'));
    osStub.home = home;
    configPath = join(home, '.dc3', 'config.json');
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  it('gateway-first bootstrap works: partial profiles persist field by field', async () => {
    vi.resetModules();
    const { configManager } = await import('../src/core/config-manager');

    // README quick start step 1: set the gateway before anything else.
    await configManager.setProfile('default', { gateway: 'http://127.0.0.1:8000' });
    const stored = JSON.parse(await readFile(configPath, 'utf8'));
    expect(stored.profiles.default.gateway).toBe('http://127.0.0.1:8000');

    // Consumption is what requires the username, with an actionable message.
    await expect(configManager.getActiveProfile()).rejects.toThrow(/username/u);

    await configManager.setProfile('default', { username: 'admin' });
    const profile = await configManager.getActiveProfile();
    expect(profile.username).toBe('admin');
    expect(profile.tenant).toBe('default');
  });

  it('per-key profile validation rejects invalid input with one readable line (F007/F054)', async () => {
    vi.resetModules();
    const { configManager } = await import('../src/core/config-manager');
    await configManager.setProfile('default', { username: 'admin' });

    await expect(configManager.setProfile('default', { gateway: 'javascript:alert(1)' })).rejects.toThrow(
      /Invalid value for gateway/u,
    );
    await expect(configManager.setProfile('default', { tenant: '' })).rejects.toThrow(
      /Invalid value for tenant/u,
    );
    // Nothing was persisted by the rejected writes.
    const stored = JSON.parse(await readFile(configPath, 'utf8'));
    expect(stored.profiles.default.gateway).toBe('http://localhost:8000');
    expect(stored.profiles.default.tenant).toBe('default');
  });

  it('the error never leaks a raw zod issues array (F007)', async () => {
    vi.resetModules();
    const { configManager } = await import('../src/core/config-manager');
    const err = await configManager.setProfile('default', { gateway: 'not-a-url' }).catch((e: Error) => e);
    expect(err).toBeInstanceOf(Error);
    expect(err.message).not.toMatch(/"code"/u);
    expect(err.message).not.toMatch(/\[\{/u);
    expect(err.message).toMatch(/^Invalid value for gateway: /u);
  });
});

/**
 * A minimal valid config used as the pre-existing state for input-validation tests.
 * @returns serialized valid configuration JSON
 */
function validConfig(): string {
  return JSON.stringify(
    {
      version: 1,
      current_profile: 'default',
      settings: { output_format: 'table', color: true, renewal_threshold_hours: 2, retry_count: 1 },
      profiles: { default: { username: 'u' } },
    },
    null,
    2,
  );
}
