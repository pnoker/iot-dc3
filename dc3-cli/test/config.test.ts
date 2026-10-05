import { SilentExit } from '../src/utils/format.js';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { existsSync } from 'node:fs';
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Command } from 'commander';
import { ConfigSchema, ProfileConfigSchema, AppSettingsSchema } from '../src/core/config-manager';

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

  it('ProfileConfigSchema should reject empty username', () => {
    const result = ProfileConfigSchema.safeParse({ username: '' });
    expect(result.success).toBe(false);
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

  /** Serialized corrupt config: renewal_threshold_hours became null (the NaN write bug). */
  const corruptConfig = JSON.stringify(
    {
      version: 1,
      current_profile: 'default',
      settings: { renewal_threshold_hours: null },
      profiles: { default: { username: 'kept-in-bak' } },
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

  it('readConfig warns on stderr, backs up, and keeps a schema-invalid config in place', async () => {
    await writeConfigFile(corruptConfig);

    vi.resetModules();
    const { configManager } = await import('../src/core/config-manager');
    let stderr = '';
    vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
      stderr += args.join(' ');
    });

    const config = await configManager.load();

    // Defaults are used in-memory only.
    expect(config.settings.renewal_threshold_hours).toBe(1);
    expect(config.profiles).toEqual({});
    // Loud warning naming the file and the fallback.
    expect(stderr).toContain(configPath);
    expect(stderr).toContain('defaults');
    // Original corrupt bytes preserved in .bak, corrupt file itself not rewritten.
    expect((await readBytes(`${configPath}.bak`)).equals(Buffer.from(corruptConfig))).toBe(true);
    expect((await readBytes(configPath)).equals(Buffer.from(corruptConfig))).toBe(true);
  });

  it('a valid set after a failed load writes defaults plus the new value and warns about carry-over', async () => {
    await writeConfigFile(corruptConfig);

    const result = await runSet('settings.retry_count', '2');

    expect(result.code).toBe(0);
    expect(result.stderr).toContain(configPath);
    expect(result.stderr).toContain('defaults');
    expect(result.stderr).toContain('not carried over');
    expect(result.stderr).toContain('.bak');

    const persisted = JSON.parse((await readBytes(configPath)).toString('utf8'));
    expect(persisted.settings.retry_count).toBe(2);
    expect(persisted.settings.renewal_threshold_hours).toBe(1);
    expect(persisted.profiles).toEqual({});
    expect((await readBytes(`${configPath}.bak`)).equals(Buffer.from(corruptConfig))).toBe(true);
  });
});

/** A minimal valid config used as the pre-existing state for input-validation tests. */
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
