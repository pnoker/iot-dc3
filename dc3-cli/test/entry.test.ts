import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CommanderError } from 'commander';
import { z, ZodError } from 'zod';

// Mock the state singletons behind the entry layer so tests never touch ~/.dc3.
vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    setProfileOverride: vi.fn(),
    getSettings: vi.fn(async () => ({})),
    getActiveProfile: vi.fn(async () => ({
      gateway: 'http://gw.test/',
      tenant: 't',
      username: 'u',
    })),
    getActiveProfileName: vi.fn(async () => 'default'),
    load: vi.fn(async () => ({ current_profile: 'default' })),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    needsRenewal: vi.fn(async () => false),
    saveState: vi.fn(async () => undefined),
    clearState: vi.fn(async () => undefined),
  },
}));

vi.mock('../src/core/credential-store.js', () => ({
  resolvePassword: vi.fn(async () => 'dc3dc3dc3'),
}));

import { buildProgram, handleProgramFailure } from '../src/index.js';
import { resetCliContext } from '../src/core/context.js';
import { configManager } from '../src/core/config-manager.js';
import {
  ApiError,
  AuthError,
  NetworkError,
  TimeoutError,
  UsageError,
  ValidationError,
  handleFatalError,
} from '../src/core/errors.js';
import { SilentExit } from '../src/utils/format.js';

type FetchCall = { url: string; init: RequestInit };
const fetchCalls: FetchCall[] = [];

/** Result of one program invocation through the real entry failure path. */
interface RunResult {
  stdout: string;
  stderr: string;
  exitCode: number;
}

/**
 * Parse argv like the bin entry does: exitOverride surfaces commander
 * failures as rejections, which flow through the same handler runCli uses.
 * @param args - arguments after `dc3`
 * @returns captured streams and effective exit code
 */
async function run(args: string[]): Promise<RunResult> {
  const program = buildProgram();
  let stdout = '';
  let stderr = '';
  const outSpy = vi
    .spyOn(process.stdout, 'write')
    .mockImplementation((chunk) => ((stdout += String(chunk)), true));
  const errSpy = vi
    .spyOn(process.stderr, 'write')
    .mockImplementation((chunk) => ((stderr += String(chunk)), true));
  process.exitCode = 0;
  try {
    await program.parseAsync(args, { from: 'user' });
  } catch (error) {
    handleProgramFailure(error);
  }
  outSpy.mockRestore();
  errSpy.mockRestore();
  return { stdout, stderr, exitCode: process.exitCode ?? 0 };
}

/**
 * Override process.stdout.isTTY for the duration of a test.
 * @param value - the isTTY value the format chain should observe
 */
function setTty(value: boolean | undefined): void {
  Object.defineProperty(process.stdout, 'isTTY', { value, configurable: true });
}

/**
 * Decode the machine envelope written to stdout by the failure chokepoint.
 * @param stdout - the captured stdout of a failed invocation
 * @returns the parsed envelope, after asserting it is a single document
 */
function parseEnvelope(stdout: string): { ok: boolean; error: { kind: string; code: string; message: string } } {
  const lines = stdout.split('\n').filter((line) => line.trim() !== '');
  expect(lines, `exactly one envelope document, got: ${stdout}`).toHaveLength(1);
  return JSON.parse(lines[0]) as { ok: boolean; error: { kind: string; code: string; message: string } };
}

beforeEach(() => {
  resetCliContext();
  setTty(undefined);
  fetchCalls.length = 0;
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string | URL, init?: RequestInit) => {
      const request = { url: String(url), init: init ?? {} };
      fetchCalls.push(request);
      if (request.init.method === 'DELETE') {
        return new Response(null, { status: 204 });
      }
      if (request.url.includes('/get_by_id')) {
        return new Response(JSON.stringify({ id: '42', version: 7 }), { status: 200 });
      }
      if (request.url.includes('/manager/device/list')) {
        return new Response(JSON.stringify([{ id: 1 }, { id: 2 }]), { status: 200 });
      }
      return new Response(String(request.init.body ?? '{}'), { status: 200 });
    }),
  );
});

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  resetCliContext();
  setTty(undefined);
  process.exitCode = 0;
});

describe('entry: root options are positional (F001)', () => {
  it.each([
    'device',
    'driver',
    'point',
    'profile',
    'group',
    'label',
    'command',
    'event',
  ])('%s update <id> --version <n> executes the write (no root flag hijack)', async (entity) => {
    const result = await run([entity, 'update', '42', '--version', '3', '--name', 'new-name']);

    expect(result.stdout).not.toContain('0.1.0');
    expect(result.exitCode).toBe(0);
    const post = fetchCalls.find(
      (call) => call.init.method === 'POST' && call.url.endsWith(`/${entity}/update`),
    );
    expect(post, `update POST issued for ${entity}`).toBeDefined();
    expect(JSON.parse(String(post?.init.body)).version).toBe(3);
  });

  it.each([
    'device',
    'driver',
    'point',
    'profile',
    'group',
    'label',
    'command',
    'event',
  ])('%s delete <id> --version <n> executes the delete', async (entity) => {
    const result = await run([entity, 'delete', '42', '--version', '3']);

    expect(result.stdout).not.toContain('0.1.0');
    expect(result.exitCode).toBe(0);
    expect(fetchCalls).toHaveLength(1);
    expect(fetchCalls[0].init.method).toBe('DELETE');
    expect(fetchCalls[0].url).toContain(`version=3`);
  });

  it('the equals form --version=3 keeps working and stays equivalent', async () => {
    const result = await run(['device', 'update', '42', '--version=3', '--name', 'new-name']);

    expect(result.exitCode).toBe(0);
    const post = fetchCalls.find((call) => call.url.endsWith('/device/update'));
    expect(JSON.parse(String(post?.init.body)).version).toBe(3);
  });

  it('root --version and -V still print the version and exit 0', async () => {
    const long = await run(['--version']);
    expect(long.stdout.trim()).toBe('0.1.0');
    expect(long.exitCode).toBe(0);
    const short = await run(['-V']);
    expect(short.stdout.trim()).toBe('0.1.0');
    expect(short.exitCode).toBe(0);
  });

  it('root options after the subcommand are unknown options, not silent hijacks', async () => {
    const result = await run(['device', 'list', '-V']);

    // README contract: global options only parse before the first subcommand.
    // A root flag after it is a strict usage error (never the "0.1.0" no-op).
    expect(result.exitCode).toBe(1);
    expect(result.stdout).not.toContain('0.1.0');
    expect(result.stderr).toContain("unknown option '-V'");
    expect(parseEnvelope(result.stdout).error.kind).toBe('usage');
  });
});

describe('entry: format precedence under positional options (spec item 7)', () => {
  it('the command --format wins over the global --format', async () => {
    const result = await run(['--format', 'json', 'device', 'list', '--format', 'yaml']);

    expect(result.exitCode).toBe(0);
    expect(result.stdout).toContain('- id: 1');
  });

  it('the global --format applies when the command has none', async () => {
    const result = await run(['--format', 'yaml', 'device', 'list']);

    expect(result.exitCode).toBe(0);
    expect(result.stdout).toContain('- id: 1');
  });

  it('settings.output_format applies when neither option is given', async () => {
    (configManager.getSettings as ReturnType<typeof vi.fn>).mockResolvedValue({
      output_format: 'yaml',
    });
    const result = await run(['device', 'list']);

    expect(result.exitCode).toBe(0);
    expect(result.stdout).toContain('- id: 1');
  });

  it('an explicit invalid leaf --format is a usage error', async () => {
    const result = await run(['device', 'list', '--format', 'xml']);

    expect(result.exitCode).toBe(1);
    expect(result.stderr).toContain("unknown format 'xml'");
    expect(parseEnvelope(result.stdout).error.kind).toBe('usage');
    expect(fetchCalls).toHaveLength(0);
  });

  it('an explicit empty --format is rejected, not silently defaulted (F052)', async () => {
    const globalForm = await run(['--format', '', 'device', 'list']);
    expect(globalForm.exitCode).toBe(1);
    expect(globalForm.stderr).toContain("unknown format ''");
    expect(fetchCalls).toHaveLength(0);

    const leafForm = await run(['device', 'list', '--format', '']);
    expect(leafForm.exitCode).toBe(1);
    expect(leafForm.stderr).toContain("unknown format ''");
  });
});

describe('entry: argv hygiene (F053)', () => {
  it('excess positional arguments are usage errors with zero requests', async () => {
    const result = await run(['device', 'get', '1', '2']);

    expect(result.exitCode).toBe(1);
    expect(result.stderr).toContain('too many arguments');
    expect(result.stderr).not.toContain('Usage:');
    expect(fetchCalls).toHaveLength(0);
    expect(parseEnvelope(result.stdout).error.kind).toBe('usage');
  });

  it('a stray positional on a zero-argument command is a usage error', async () => {
    const result = await run(['device', 'list', 'frobnicate']);

    expect(result.exitCode).toBe(1);
    expect(result.stderr).toContain('too many arguments');
    expect(fetchCalls).toHaveLength(0);
  });

  it('an empty --profile is a usage error instead of a silent default fallback', async () => {
    const empty = await run(['--profile', '', 'device', 'list']);
    expect(empty.exitCode).toBe(1);
    expect(empty.stderr).toContain('--profile must not be empty or whitespace-only');
    expect(fetchCalls).toHaveLength(0);

    const blank = await run(['--profile', '   ', 'device', 'list']);
    expect(blank.exitCode).toBe(1);
    expect(blank.stderr).toContain('--profile must not be empty or whitespace-only');
    expect(fetchCalls).toHaveLength(0);
  });

  it('a provided --profile still routes to the validated override', async () => {
    const result = await run(['--profile', 'prod', 'device', 'list']);

    expect(configManager.setProfileOverride).toHaveBeenCalledWith('prod');
    expect(result.exitCode).toBe(0);
  });
});

describe('entry: bare invocation and help paths (F048, F014, F056)', () => {
  it('bare dc3 prints help to stdout and exits 0', async () => {
    const result = await run([]);

    expect(result.exitCode).toBe(0);
    expect(result.stdout.startsWith('Usage: dc3')).toBe(true);
    expect(result.stderr).toBe('');
  });

  it('dc3 --help and dc3 help print the same help on stdout with exit 0', async () => {
    const helpFlag = await run(['--help']);
    const helpCommand = await run(['help']);

    expect(helpFlag.exitCode).toBe(0);
    expect(helpCommand.exitCode).toBe(0);
    expect(helpFlag.stdout.startsWith('Usage: dc3')).toBe(true);
    expect(helpCommand.stdout).toBe(helpFlag.stdout);
  });

  it('help resolves every path segment to the leaf command help (F014)', async () => {
    const viaHelp = await run(['help', 'device', 'list']);
    const viaFlag = await run(['device', 'list', '--help']);

    expect(viaHelp.exitCode).toBe(0);
    expect(viaFlag.exitCode).toBe(0);
    expect(viaHelp.stdout.split('\n')[0]).toBe(viaFlag.stdout.split('\n')[0]);
    expect(viaHelp.stdout.split('\n')[0]).toBe('Usage: dc3 device list [options]');
  });

  it('help resolves nested group paths (config profile) to the group help', async () => {
    const viaHelp = await run(['help', 'config', 'profile']);
    const viaFlag = await run(['config', 'profile', '--help']);

    expect(viaHelp.exitCode).toBe(0);
    expect(viaHelp.stdout.split('\n')[0]).toBe(viaFlag.stdout.split('\n')[0]);
  });

  it('group-level help resolves subcommand paths too', async () => {
    const viaHelp = await run(['device', 'help', 'list']);
    const viaFlag = await run(['device', 'list', '--help']);

    expect(viaHelp.exitCode).toBe(0);
    expect(viaHelp.stdout.split('\n')[0]).toBe(viaFlag.stdout.split('\n')[0]);
  });

  it('help with an unknown segment is a single-line usage error, no help dump (F056)', async () => {
    const result = await run(['help', 'device', 'no-such-sub']);

    expect(result.exitCode).toBe(1);
    expect(result.stderr).toBe("Error: unknown command 'no-such-sub'\n");
    expect(result.stderr).not.toContain('Usage:');
    expect(result.stdout).not.toContain('Usage:');
    expect(parseEnvelope(result.stdout).error.kind).toBe('usage');
  });

  it('help with an unknown top-level target is a single-line usage error', async () => {
    const result = await run(['help', 'nonexistent']);

    expect(result.exitCode).toBe(1);
    expect(result.stderr).toBe("Error: unknown command 'nonexistent'\n");
    expect(result.stderr).not.toContain('Usage:');
  });

  it('an unknown command is a single-line usage error', async () => {
    const result = await run(['nonexistent']);

    expect(result.exitCode).toBe(1);
    expect(result.stderr).toBe("Error: unknown command 'nonexistent'\n");
    expect(result.stderr).not.toContain('Usage:');
    expect(parseEnvelope(result.stdout).error.kind).toBe('usage');
  });

  it('an invalid option value is a single-line usage error before any request', async () => {
    const result = await run(['device', 'update', '42', '--version', 'abc', '--name', 'x']);

    expect(result.exitCode).toBe(1);
    expect(result.stderr.split('\n')).toHaveLength(2);
    expect(result.stderr).toContain("'abc'");
    expect(result.stderr).not.toContain('Usage:');
    expect(fetchCalls).toHaveLength(0);
    expect(parseEnvelope(result.stdout).error.kind).toBe('usage');
  });

  it('a bare group invocation keeps commander behavior: group help on stderr, exit 1', async () => {
    const result = await run(['device']);

    expect(result.exitCode).toBe(1);
    expect(result.stdout).toBe('');
    expect(result.stderr.startsWith('Usage: dc3 device')).toBe(true);
  });
});

describe('entry: deprecated create alias (F029)', () => {
  it('invoking via the alias warns once on stderr and still routes to add', async () => {
    const result = await run([
      'device',
      'create',
      '--name',
      'n',
      '--driver-id',
      '1',
      '--profile-id',
      '1',
    ]);

    expect(result.exitCode).toBe(0);
    // Warning-prefix casing is part of the stderr contract: every degradation
    // or deprecation warning in the CLI uses capitalised 'Warning: '.
    expect(result.stderr).toContain("Warning: 'create' is a deprecated compat alias of 'add'");
    expect(result.stderr).not.toMatch(/(?<![A-Za-z])warning:/u);
    expect(result.stderr).toContain('dc3 device add');
    expect(result.stderr).not.toContain('dc3 device create');
    expect(fetchCalls.some((call) => call.url.endsWith('/manager/device/add'))).toBe(true);
  });

  it('the canonical name never warns', async () => {
    const result = await run([
      'device',
      'add',
      '--name',
      'n',
      '--driver-id',
      '1',
      '--profile-id',
      '1',
    ]);

    expect(result.exitCode).toBe(0);
    expect(result.stderr).not.toContain('deprecated');
  });

  it('group help lists the alias as deprecated, not first-class', async () => {
    const result = await run(['device', '--help']);

    expect(result.exitCode).toBe(0);
    expect(result.stdout).toContain('add (deprecated alias: create)');
  });
});

describe('entry: failure envelope on the real path (F015)', () => {
  it('a dead gateway exits 2 with a stdout envelope and one stderr line', async () => {
    vi.unstubAllGlobals();
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('fetch failed');
      }),
    );
    const result = await run(['device', 'list']);

    expect(result.exitCode).toBe(2);
    expect(result.stderr.startsWith('Error: ')).toBe(true);
    expect(result.stderr.split('\n')).toHaveLength(2); // single line + trailing newline
    const envelope = parseEnvelope(result.stdout);
    expect(envelope.ok).toBe(false);
    expect(envelope.error.kind).toBe('network');
    expect(envelope.error.code).toBe('NETWORK');
  });
});

describe('failure chokepoint taxonomy (F015, F030, F007)', () => {
  /**
   * Run handleFatalError directly and capture both streams plus the exit code.
   * @param err - the failure to report through the chokepoint
   * @returns captured streams and the installed exit code
   */
  function report(err: Error): RunResult {
    let stdout = '';
    let stderr = '';
    const outSpy = vi
      .spyOn(process.stdout, 'write')
      .mockImplementation((chunk) => ((stdout += String(chunk)), true));
    const errSpy = vi
      .spyOn(process.stderr, 'write')
      .mockImplementation((chunk) => ((stderr += String(chunk)), true));
    process.exitCode = 0;
    try {
      handleFatalError(err);
    } catch {
      // handleFatalError reports then re-throws by contract
    }
    outSpy.mockRestore();
    errSpy.mockRestore();
    return { stdout, stderr, exitCode: process.exitCode ?? 0 };
  }

  it.each([
    { error: () => new UsageError('bad flag'), kind: 'usage', code: 'USAGE', exitCode: 1 },
    {
      error: () => new ValidationError('bad payload'),
      kind: 'validation',
      code: 'VALIDATION',
      exitCode: 1,
    },
    { error: () => new AuthError('Not logged in'), kind: 'auth', code: 'AUTH', exitCode: 3 },
    {
      error: () => new NetworkError('gateway unreachable'),
      kind: 'network',
      code: 'NETWORK',
      exitCode: 2,
    },
    { error: () => new TimeoutError('poll deadline'), kind: 'timeout', code: 'TIMEOUT', exitCode: 1 },
    {
      error: () => new ApiError('Not found (404): thing', 404),
      kind: 'api',
      code: 'API_404',
      exitCode: 1,
    },
    { error: () => new Error('internal'), kind: 'api', code: 'INTERNAL', exitCode: 1 },
  ])('$kind failures report kind/code and keep exit code $exitCode', ({ error, kind, code, exitCode }) => {
    const result = report(error());

    expect(result.exitCode).toBe(exitCode);
    expect(result.stderr.startsWith('Error: ')).toBe(true);
    expect(result.stderr.split('\n')).toHaveLength(2);
    const envelope = parseEnvelope(result.stdout);
    expect(envelope.ok).toBe(false);
    expect(envelope.error.kind).toBe(kind);
    expect(envelope.error.code).toBe(code);
  });

  it('ZodError collapses to a one-line validation failure, never a raw issues dump (F007)', () => {
    const parsed = z.object({ username: z.string().min(1), gateway: z.string().url() }).safeParse({
      username: '',
      gateway: 'not-a-url',
    });
    expect(parsed.success).toBe(false);
    const result = report(parsed.error as ZodError);

    expect(result.exitCode).toBe(1);
    expect(result.stderr.split('\n')).toHaveLength(2);
    expect(result.stderr).toContain('username');
    expect(result.stderr).toContain('gateway');
    expect(result.stderr).not.toContain('"code"');
    expect(result.stderr).not.toContain('"invalid_string"');
    const envelope = parseEnvelope(result.stdout);
    expect(envelope.error.kind).toBe('validation');
  });

  it('commander usage errors bridge into the envelope with one human line', () => {
    const commanderError = new CommanderError(
      1,
      'commander.unknownOption',
      "error: unknown option '--nope'",
    );
    const result = report(commanderError);

    expect(result.exitCode).toBe(1);
    expect(result.stderr).toBe("Error: unknown option '--nope'\n");
    const envelope = parseEnvelope(result.stdout);
    expect(envelope.error.kind).toBe('usage');
    expect(envelope.error.code).toBe('commander.unknownOption');
  });

  it('the envelope is suppressed for table output (TTY default keeps stdout human)', () => {
    setTty(true);
    const result = report(new AuthError('Not logged in'));

    expect(result.exitCode).toBe(3);
    expect(result.stdout).toBe('');
    expect(result.stderr).toContain('Not logged in');
  });

  it('the envelope honors the persisted settings format', async () => {
    (configManager.getSettings as ReturnType<typeof vi.fn>).mockResolvedValue({
      output_format: 'yaml',
    });
    await run(['device', 'list']);
    const result = report(new NetworkError('down'));

    expect(parseEnvelope(result.stdout).error.kind).toBe('network');
  });
});

describe('entry seam contracts', () => {
  it('handleProgramFailure passes SilentExit through untouched', () => {
    const before = process.exitCode;
    expect(() => handleProgramFailure(new SilentExit())).not.toThrow();
    expect(process.exitCode).toBe(before);
  });

  it('commander help exits are silent and keep their exit code', () => {
    process.exitCode = 0;
    handleProgramFailure(new CommanderError(0, 'commander.help', '(outputHelp)'));
    expect(process.exitCode).toBe(0);

    handleProgramFailure(new CommanderError(1, 'commander.help', '(outputHelp)'));
    expect(process.exitCode).toBe(1);
  });

  it('the real entry shape (versioned program) never regresses to bare programs', () => {
    const program = buildProgram();
    expect(program.version()).toBe('0.1.0');
    expect(program.name()).toBe('dc3');
    expect(program.commands.length).toBeGreaterThan(20);
    // Asserting `new Command().version()` here was a tautology (a bare
    // commander program never carries our version); the buildProgram-based
    // assertions above are the real regression guard.
  });
});

describe('entry: full-tree help equivalence (F014)', () => {
  /**
   * Walk the program tree and collect every command path below the root,
   * skipping the internal `help` commands. Copied from docs-lint.test.ts's
   * walker rather than cross-imported between test files.
   * @returns space-joined paths like `config profile create`
   */
  function collectCommandPaths(): string[] {
    const paths: string[] = [];
    const walk = (
      command: { name(): string; commands: readonly unknown[] },
      prefix: string[],
    ): void => {
      for (const child of command.commands as { name(): string; commands: readonly unknown[] }[]) {
        if (child.name() === 'help') continue;
        const path = [...prefix, child.name()].join(' ');
        paths.push(path);
        walk(child, [...prefix, child.name()]);
      }
    };
    walk(buildProgram(), []);
    return paths;
  }

  it('for every group/subcommand path, `help <path>` and `<path> --help` agree', async () => {
    const paths = collectCommandPaths().filter((path) => path.split(' ').length >= 2);
    expect(paths.length).toBeGreaterThan(60);

    for (const path of paths) {
      const segments = path.split(' ');
      const viaHelp = await run(['help', ...segments]);
      const viaFlag = await run([...segments, '--help']);

      expect(viaHelp.exitCode, `exit of "help ${path}"`).toBe(0);
      expect(viaFlag.exitCode, `exit of "${path} --help"`).toBe(0);
      const helpFirst = viaHelp.stdout.split('\n')[0];
      const flagFirst = viaFlag.stdout.split('\n')[0];
      expect(helpFirst, `first help line of "${path}"`).toBe(flagFirst);
      expect(helpFirst.startsWith('Usage: dc3 '), `usage header of "${path}"`).toBe(true);
    }
  });
});
