import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { readdir, readFile, stat } from 'node:fs/promises';
import { join, dirname, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

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
import { AuthError, NetworkError, handleFatalError } from '../src/core/errors.js';

const srcDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'src');
const commandsDir = join(srcDir, 'commands');

/** Sentinel thrown by the mocked process.exit so never-returning handlers stop cleanly. */
class ExitSignal extends Error {
  constructor(readonly code: number) {
    super(`process.exit(${code})`);
  }
}

/** Override process.stdout.isTTY for the duration of a test. */
function setTty(value: boolean | undefined): void {
  Object.defineProperty(process.stdout, 'isTTY', { value, configurable: true });
}

/** Run handleFatalError and return the exit code it selected. */
function exitCodeFor(err: Error): number {
  process.exitCode = 0;
  try {
    handleFatalError(err);
  } catch {
    // handleFatalError prints, sets process.exitCode, and re-throws; read the code
  }
  return process.exitCode ?? 1;
  throw new Error('handleFatalError returned without exiting');
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
