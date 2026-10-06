import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { readdir, readFile, stat } from 'node:fs/promises';
import { join, dirname, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { InvalidArgumentError } from 'commander';

// Mock the config read behind the CLI context so tests never touch ~/.dc3.
vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    setProfileOverride: vi.fn(),
    getSettings: vi.fn(async () => ({})),
  },
}));

import { detectFormat } from '../src/utils/format.js';
import { applyGlobalOptions, resetCliContext } from '../src/core/context.js';
import { configManager } from '../src/core/config-manager.js';
import { AuthError as ClientAuthError } from '../src/core/client.js';
import { AuthError, NetworkError, ValidationError, classifyError, handleFatalError } from '../src/core/errors.js';
import { buildProgram } from '../src/index.js';
import type { Command, Option } from 'commander';

const srcDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'src');
const commandsDir = join(srcDir, 'commands');

/** Sentinel thrown by the mocked process.exit so never-returning handlers stop cleanly. */
class ExitSignal extends Error {
  constructor(readonly code: number) {
    super(`process.exit(${code})`);
  }
}

/**
 * Override process.stdout.isTTY for the duration of a test.
 * @param value - the isTTY value the format chain should observe
 */
function setTty(value: boolean | undefined): void {
  Object.defineProperty(process.stdout, 'isTTY', { value, configurable: true });
}

/**
 * Run handleFatalError and return the exit code it selected.
 * @param err - the failure to report through the chokepoint
 * @returns the exit code handleFatalError installed
 */
function exitCodeFor(err: Error): number {
  process.exitCode = 0;
  try {
    handleFatalError(err);
  } catch {
    // handleFatalError prints, sets process.exitCode, and re-throws by
    // contract (its return type is never); the code is read below.
  }
  return process.exitCode ?? 1;
}

describe('detectFormat resolution chain', () => {
  beforeEach(() => {
    resetCliContext();
    setTty(undefined);
    (configManager.getSettings as ReturnType<typeof vi.fn>).mockResolvedValue({});
  });

  afterEach(() => {
    resetCliContext();
    setTty(undefined);
  });

  it('the explicit command option wins over the global option and settings', async () => {
    (configManager.getSettings as ReturnType<typeof vi.fn>).mockResolvedValue({
      output_format: 'yaml',
    });
    await applyGlobalOptions({ format: 'table' });
    expect(detectFormat('json')).toBe('json');
  });

  it('the global --format option wins over settings and the TTY default', async () => {
    (configManager.getSettings as ReturnType<typeof vi.fn>).mockResolvedValue({
      output_format: 'yaml',
    });
    setTty(true);
    await applyGlobalOptions({ format: 'json' });
    expect(detectFormat()).toBe('json');
  });

  it('settings.output_format applies when neither option is given', async () => {
    (configManager.getSettings as ReturnType<typeof vi.fn>).mockResolvedValue({
      output_format: 'yaml',
    });
    setTty(true);
    await applyGlobalOptions({});
    expect(detectFormat()).toBe('yaml');
  });

  it('unset settings fall back to the TTY-aware default (table on TTY, json on pipe)', async () => {
    (configManager.getSettings as ReturnType<typeof vi.fn>).mockResolvedValue({});
    await applyGlobalOptions({});
    setTty(true);
    expect(detectFormat()).toBe('table');
    setTty(undefined);
    expect(detectFormat()).toBe('json');
  });

  it('invalid format values are rejected with a stderr error', async () => {
    (configManager.getSettings as ReturnType<typeof vi.fn>).mockResolvedValue({});
    vi.spyOn(process.stderr, 'write').mockImplementation(() => true);
    await expect(applyGlobalOptions({ format: 'xml' })).rejects.toThrow();
    (process.stderr.write as ReturnType<typeof vi.spyOn>).mockRestore();
  });

  it('applyGlobalOptions routes --profile to the validated profile override', async () => {
    await applyGlobalOptions({ profile: 'prod' });
    expect(configManager.setProfileOverride).toHaveBeenCalledWith('prod');
  });
});

describe('exit-code mapping (top-level fatal handler)', () => {
  beforeEach(() => {
    vi.spyOn(process.stdout, 'write').mockImplementation(() => true);
    vi.spyOn(process.stderr, 'write').mockImplementation(() => true);
    vi.spyOn(process, 'exit').mockImplementation(((code?: number) => {
      throw new ExitSignal(code ?? 0);
    }) as never);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('AuthError exits 3 and reports on stderr', () => {
    const err = new AuthError('Not logged in. Run: dc3 auth login');
    expect(exitCodeFor(err)).toBe(3);
    expect(process.stderr.write).toHaveBeenCalledWith(`Error: ${err.message}\n`);
  });

  it('failures also emit the stdout machine envelope for structured formats (F015)', () => {
    const err = new NetworkError('gateway unreachable');
    expect(exitCodeFor(err)).toBe(2);
    const envelope = JSON.parse(
      String((process.stdout.write as ReturnType<typeof vi.fn>).mock.calls[0]?.[0]),
    );
    expect(envelope).toEqual({
      ok: false,
      error: { kind: 'network', code: 'NETWORK', message: err.message },
    });
  });

  it('NetworkError exits 2', () => {
    expect(exitCodeFor(new NetworkError('gateway unreachable'))).toBe(2);
  });

  it('generic errors exit 1', () => {
    expect(exitCodeFor(new Error('business failure'))).toBe(1);
  });

  it('client-re-exported errors are the same classes the handler maps', () => {
    expect(new ClientAuthError('x')).toBeInstanceOf(AuthError);
  });
});

describe('error-kind precision (residual round)', () => {
  it('commander invalid-argument values classify as validation, not usage', () => {
    const failure = classifyError(new InvalidArgumentError('must be a non-empty value'));
    expect(failure.kind).toBe('validation');
    expect(failure.exitCode).toBe(1);
  });

  it('ZodError keeps collapsing to a single-line validation failure', () => {
    const failure = classifyError(new ValidationError('Invalid value for settings.color: "maybe"'));
    expect(failure).toMatchObject({ kind: 'validation', code: 'VALIDATION', exitCode: 1 });
  });
});

describe('static source scans', () => {
  /**
   * Command groups whose own chain carries no --format: their leaves declare it.
   * Keep this allowlist explicit — a new group command must be added here.
   */
  const COMMAND_GROUP_ALLOWLIST = new Set([
    'config',
    'auth',
    'device',
    'driver',
    'point',
    'profile',
    'group',
    'label',
    'event',
    'command',
    'alert',
    'dashboard',
    'topic',
    'chat',
    'tools',
    'analytics',
    'session',
    'action',
    'provider',
    'model',
    'attachment',
  ]);

  it('every leaf command declares a --format option', async () => {
    const files = (await readdir(commandsDir)).filter((f) => f.endsWith('.ts')).sort();
    expect(files.length).toBeGreaterThan(10);
    for (const file of files) {
      const source = await readFile(join(commandsDir, file), 'utf8');
      const segments = source.split('.command(').slice(1);
      expect(segments.length, `${file} registers commands`).toBeGreaterThan(0);
      for (const segment of segments) {
        const nameMatch = segment.match(/^(['"`])([^'"`]+)\1/);
        const name = nameMatch ? nameMatch[2].split(' ')[0] : '(dynamic)';
        if (COMMAND_GROUP_ALLOWLIST.has(name)) continue;
        expect(segment, `${file}: leaf command "${name}"`).toContain('--format');
      }
    }
  });

  /** Files allowed to call fetch( directly: the shared HTTP seam, and nothing else. */
  const FETCH_ALLOWLIST = new Set(['core/http.ts']);

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

  it('no src file prints failure payloads — failures must throw through the chokepoint (F015)', async () => {
    // Full-src walk (not just commands/): a failure printAndExit hidden in
    // core/ or utils/ would evade a commands-only scan. The file defining
    // printAndExit is the one legitimate place to mention it.
    const files = (await collectTsFiles(srcDir)).sort();
    expect(files.length).toBeGreaterThan(20);
    for (const file of files) {
      const rel = relative(srcDir, file).split(sep).join('/');
      if (rel === 'utils/format.ts') continue;
      const source = await readFile(file, 'utf8');
      expect(
        source.match(/printAndExit\(\s*\{[^)]*ok:\s*false/s),
        `${rel} must throw a typed error instead of printing a failure payload`,
      ).toBeNull();
    }
  });

  it('fetch( appears only inside the shared HTTP seam', async () => {
    expect(await stat(join(srcDir, 'core', 'http.ts'))).toBeTruthy();
    const files = await collectTsFiles(srcDir);
    expect(files.length).toBeGreaterThan(20);
    for (const file of files) {
      const rel = relative(srcDir, file).split(sep).join('/');
      if (FETCH_ALLOWLIST.has(rel)) continue;
      const source = await readFile(file, 'utf8');
      expect(
        source.match(/(^|[^.\w])fetch\(/),
        `${rel} must use the shared fetch seam (core/http.ts)`,
      ).toBeNull();
    }
  });
});

describe('option introspection scan (F021 guard b)', () => {
  /**
   * Placeholders that match the numeric pattern but are genuinely string-typed
   * identifiers on their surface (add here only with a wire-contract reason).
   * Currently empty: every numeric-looking placeholder in the tree is numeric.
   */
  const STRING_TYPED_PLACEHOLDERS = new Set<string>();

  /** Placeholder shapes this scan treats as numeric-valued options. */
  const NUMERIC_PLACEHOLDER =
    /\b(?:n|num|count|days|hours|minutes|limit|offset|top[-_]n|timeout|port|version|seconds|ms)\b/iu;

  /**
   * Walk the real program tree and pair every declared option with the full
   * invocation path it belongs to (the same registration the bin entry runs).
   * @returns option entries with their owning command path
   */
  function collectOptions(): Array<{ path: string; option: Option }> {
    const found: Array<{ path: string; option: Option }> = [];
    const walk = (cmd: Command, prefix: string): void => {
      const path = prefix ? `${prefix} ${cmd.name()}` : cmd.name();
      for (const option of cmd.options) {
        found.push({ path, option });
      }
      for (const child of cmd.commands) {
        if (child.name() === 'help') continue;
        walk(child, path);
      }
    };
    walk(buildProgram(), '');
    return found;
  }

  /**
   * Extract the value placeholder of an option's flags, e.g. `<n>` from
   * `--offset <n>` and `[size]` from `--size [size]`.
   * @param flags - the option's flags string
   * @returns the placeholder, or undefined for value-less flags
   */
  function optionPlaceholder(flags: string): string | undefined {
    return flags.match(/<([^>]+)>/u)?.[1] ?? flags.match(/\[([^\]]+)\]/u)?.[1];
  }

  it('the tree walk sees the full numeric option surface (scan sanity control)', () => {
    const numeric = collectOptions().filter(
      ({ option }) =>
        (optionPlaceholder(option.flags) !== undefined &&
          NUMERIC_PLACEHOLDER.test(optionPlaceholder(option.flags) as string)) ||
        typeof option.defaultValue === 'number',
    );
    expect(numeric.length).toBeGreaterThan(20);
    expect(
      numeric.filter(({ option }) => typeof option.defaultValue === 'number').length,
    ).toBeGreaterThan(5);
  });

  it('every numeric-valued option carries a parser (a future --foo <n> without one fails here)', () => {
    const offenders: string[] = [];
    for (const { path, option } of collectOptions()) {
      const placeholder = optionPlaceholder(option.flags);
      const looksNumeric = placeholder !== undefined && NUMERIC_PLACEHOLDER.test(placeholder);
      const numericDefault = typeof option.defaultValue === 'number';
      if (!looksNumeric && !numericDefault) continue;
      if (placeholder !== undefined && STRING_TYPED_PLACEHOLDERS.has(placeholder)) continue;
      if (option.parseArg === undefined) {
        offenders.push(`${path} ${option.flags}`);
      }
    }
    expect(
      offenders,
      `numeric options without a value parser (their values would reach the wire as strings): ` +
        offenders.join('; '),
    ).toEqual([]);
  });
});
