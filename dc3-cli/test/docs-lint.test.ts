import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { CommanderError } from 'commander';

// Mock every state singleton behind the command tree so README lines can be
// executed against the real program without touching ~/.dc3 or the network.
vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    setProfileOverride: vi.fn(),
    getSettings: vi.fn(async () => ({})),
    setSetting: vi.fn(async () => undefined),
    setProfile: vi.fn(async () => undefined),
    switchProfile: vi.fn(async () => undefined),
    deleteProfile: vi.fn(async () => undefined),
    getAllProfiles: vi.fn(async () => ({ default: {} })),
    getActiveProfile: vi.fn(async () => ({
      gateway: 'http://gw.test/',
      tenant: 'tenant-1',
      username: 'user-1',
    })),
    getActiveProfileName: vi.fn(async () => 'default'),
    load: vi.fn(async () => ({ current_profile: 'default', profiles: { default: {} } })),
    reset: vi.fn(async () => undefined),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    needsRenewal: vi.fn(async () => false),
    saveState: vi.fn(async () => undefined),
    clearState: vi.fn(async () => undefined),
    clearAll: vi.fn(async () => undefined),
    getAllStates: vi.fn(async () => ({})),
    buildHeaders: vi.fn(() => ({})),
  },
  credentialIdentifier: vi.fn((state: { username: string; tenant: string }) => {
    if (!state) return 'unknown';
    const { username = 'u', tenant = 't' } = state;
    return `${username}@${tenant}`;
  }),
}));

vi.mock('../src/core/credential-store.js', () => ({
  resolvePassword: vi.fn(async () => null),
  savePasswordToStore: vi.fn(async () => ({ persisted: true, store: 'encrypted' })),
  deletePasswordFromStore: vi.fn(async () => undefined),
  clearAllStoredCredentials: vi.fn(async () => undefined),
  resetAllLocalState: vi.fn(async () => undefined),
}));

// Prompts always reject: README interactive examples must still parse and
// dispatch (structural validation), and a rejected prompt terminates the
// action deterministically instead of waiting on the test worker's stdin.
vi.mock('../src/utils/prompt.js', async () => {
  const { UsageError } = await import('../src/core/errors.js');
  const reject = async () => {
    throw new UsageError('docs-lint: interactive prompt is not answerable here');
  };
  return { prompt: reject, passwordPrompt: reject, confirm: reject };
});

import { buildProgram } from '../src/index.js';
import { resetCliContext } from '../src/core/context.js';

const readmePath = join(dirname(fileURLToPath(import.meta.url)), '..', 'README.md');
const readme = readFileSync(readmePath, 'utf8');

/** One executable `dc3 ...` line extracted from a README bash fence. */
interface ReadmeCommand {
  line: number;
  text: string;
  argv: string[];
}

/**
 * Tokenize one shell line: quote-aware, stopping at a comment token. A token
 * may mix quoted and unquoted segments (`[--params '{"k":"v"}']`), which shell
 * concatenates into one word. Sufficient for the README's single-line command
 * style (no pipes, no substitutions).
 * @param line - raw README line
 * @returns shell tokens, or null when the line is blank/comment-only
 */
function tokenizeLine(line: string): string[] | null {
  const tokens: string[] = [];
  let index = 0;
  while (index < line.length) {
    while (index < line.length && /\s/.test(line[index])) index += 1;
    if (index >= line.length) break;
    if (line[index] === '#') break; // comment runs to end of line
    let token = '';
    while (index < line.length && !/\s/.test(line[index])) {
      const quote = line[index] === "'" || line[index] === '"' ? line[index] : null;
      if (quote) {
        index += 1;
        while (index < line.length && line[index] !== quote) {
          token += line[index];
          index += 1;
        }
        index += 1; // closing quote (or end of line)
      } else {
        token += line[index];
        index += 1;
      }
    }
    tokens.push(token);
  }
  return tokens.length > 0 ? tokens : null;
}

/**
 * Substitute a README `<placeholder>` with a dummy value that satisfies the
 * real validators (choice sets, URL shapes, integer parsers).
 * @param placeholder - placeholder text without angle brackets
 * @returns a concrete value accepted by the corresponding option/argument
 */
function dummyFor(placeholder: string): string {
  const key = placeholder.toLowerCase();
  if (key.includes('format')) return 'json';
  if (key.includes('url')) return 'http://gw.test/';
  if (key.includes('file') || key.endsWith('.xlsx')) return 'file.xlsx';
  // Output paths point into the temp dir so README examples that WRITE a file
  // (device import-template --output) never dirty the working tree.
  if (key.includes('path')) return join(tmpdir(), 'dc3-docs-lint-output.json');
  if (key.includes('cursor')) return 'cursor-1';
  if (key.includes('json') || key.includes('args')) return '{}';
  if (key.includes('scope')) return 'scope-1';
  if (key.includes('secret') || key.includes('password')) return 'secret-1';
  if (key.includes('model')) return 'model-1';
  if (key.includes('name')) return 'name-1';
  return '1';
}

/**
 * Expand one README line into a concrete argv: strip the optionality brackets,
 * substitute `<placeholder>` values, and neutralize the stdin marker so no
 * validation run can block on file descriptor 0.
 * @param text - the README line
 * @returns argv without the leading `dc3`, or null when not a dc3 command
 */
function readmeLineToArgv(text: string): string[] | null {
  const tokens = tokenizeLine(text);
  if (!tokens || tokens[0] !== 'dc3') return null;
  const argv: string[] = [];
  for (const raw of tokens.slice(1)) {
    if (raw === '...') continue; // literal ellipsis filler
    const token = raw.replaceAll('[', '').replaceAll(']', '');
    const substituted = token.replace(/<([^<>]+)>/g, (_m, placeholder: string) =>
      dummyFor(placeholder),
    );
    // `--args-file -` reads stdin; point it at a (missing) file instead so the
    // validation run fails fast on our own error rather than blocking on fd 0.
    argv.push(substituted === '-' ? 'payload.json' : substituted);
  }
  return argv;
}

/**
 * Collect every `dc3` command line from the README's bash fences, joining
 * backslash continuations.
 * @returns all executable command lines with their README line numbers
 */
function collectReadmeCommands(): ReadmeCommand[] {
  const commands: ReadmeCommand[] = [];
  const fence = /```bash\r?\n([\s\S]*?)```/g;
  let match: RegExpExecArray | null;
  while ((match = fence.exec(readme)) !== null) {
    const blockLines = match[1].split(/\r?\n/);
    for (let i = 0; i < blockLines.length; i += 1) {
      let text = blockLines[i];
      const startLine = i + 1;
      while (/\\$/.test(text) && i + 1 < blockLines.length) {
        i += 1;
        text = `${text.slice(0, -1)}${blockLines[i]}`;
      }
      const argv = readmeLineToArgv(text);
      if (argv) commands.push({ line: startLine, text: text.trim(), argv });
    }
  }
  return commands;
}

const readmeCommands = collectReadmeCommands();

/**
 * Run one argv through the real program (same seam as the bin entry) and
 * classify the outcome. The lint fails only on COMMANDER-level rejections —
 * unknown options/commands, invalid or missing option values, excess
 * arguments — because those mean the README documents a command shape the CLI
 * does not accept. Business/auth/network failures are fine: the README's
 * placeholder values are not expected to pass the gateway.
 * @param argv - arguments after `dc3`
 * @returns the error that escaped, or null on success
 */
async function runThroughProgram(argv: string[]): Promise<unknown> {
  const program = buildProgram();
  const outSpy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true);
  const errSpy = vi.spyOn(process.stderr, 'write').mockImplementation(() => true);
  process.exitCode = 0;
  let escaped: unknown = null;
  try {
    await program.parseAsync(argv, { from: 'user' });
  } catch (error) {
    escaped = error;
  } finally {
    outSpy.mockRestore();
    errSpy.mockRestore();
  }
  return escaped;
}

/**
 * Assert one README command line passes commander validation.
 * @param command - the extracted README command
 */
async function assertReadmeCommandValidates(command: ReadmeCommand): Promise<void> {
  const escaped = await runThroughProgram(command.argv);
  if (escaped instanceof CommanderError) {
    // commander.help / commander.version are successful help exits, not drift.
    if (escaped.code === 'commander.help' || escaped.code === 'commander.version') return;
    throw new Error(
      `README line ${command.line} is not accepted by the CLI ` +
        `(commander ${escaped.code}: ${escaped.message}): ${command.text}`,
    );
  }
}

describe('docs-lint: README bash fences validate against the real program (F028 class)', () => {
  beforeEach(() => {
    resetCliContext();
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_url: string | URL, init?: Parameters<typeof fetch>[1]) => {
        if (init?.method === 'DELETE') return new Response(null, { status: 204 });
        return new Response(JSON.stringify({}), {
          status: 200,
          headers: { 'content-type': 'application/json' },
        });
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    resetCliContext();
    process.exitCode = 0;
  });

  it('the extractor finds the README command surface (guards against a vacuous lint)', () => {
    expect(readmeCommands.length).toBeGreaterThan(80);
  });

  it.each(readmeCommands.map((command) => [command.text, command] as const))(
    'line validates: %s',
    async (_text, command) => {
      await assertReadmeCommandValidates(command);
    },
  );

  it('negative control: the original F028 drift (--count) is caught by this validator', async () => {
    const escaped = await runThroughProgram([
      'point',
      'history',
      '456789',
      '--device-id',
      'dev-1',
      '--count',
      '100',
    ]);
    expect(escaped).toBeInstanceOf(CommanderError);
    expect((escaped as CommanderError).code).toBe('commander.unknownOption');
  });

  it('negative control: a value-less flag is caught (optionMissingArgument)', async () => {
    const escaped = await runThroughProgram(['device', 'list', '--limit']);
    expect(escaped).toBeInstanceOf(CommanderError);
    expect((escaped as CommanderError).code).toBe('commander.optionMissingArgument');
  });
});

describe('docs-lint: every registered command path is documented in the README (F031 class)', () => {
  /**
   * Walk the program tree and collect every command path below the root,
   * skipping the internal `help` commands.
   * @returns space-joined paths like `config profile create`
   */
  function collectCommandPaths(): string[] {
    const paths: string[] = [];
    const walk = (command: { name(): string; commands: unknown[] }, prefix: string[]): void => {
      for (const child of command.commands as { name(): string; commands: unknown[] }[]) {
        if (child.name() === 'help') continue;
        const path = [...prefix, child.name()].join(' ');
        paths.push(path);
        walk(child, [...prefix, child.name()]);
      }
    };
    walk(buildProgram(), []);
    return paths;
  }

  it('negative control: the tree walk sees the full command surface', () => {
    const paths = collectCommandPaths();
    expect(paths).toContain('device import');
    expect(paths).toContain('config profile create');
    expect(paths).toContain('alert type-distribution');
    expect(paths).toContain('provider check');
    expect(paths.length).toBeGreaterThan(80);
  });

  it('every registered group/subcommand path appears in the README', () => {
    const missing = collectCommandPaths().filter(
      (path) => !readme.includes(`dc3 ${path}`),
    );
    expect(missing, `undocumented command paths: ${missing.join(', ')}`).toEqual([]);
  });
});

describe('docs-lint: README .mcp.json snippets satisfy the mcpServers schema (F050 class)', () => {
  /**
   * Parse JSONC text (comments and trailing commas stripped) into a value.
   * Comment stripping is string-aware: `//` inside a URL value is content,
   * not a comment.
   * @param text - fenced snippet content
   * @returns the parsed value
   */
  function parseJsonc(text: string): unknown {
    let out = '';
    let inString = false;
    for (let i = 0; i < text.length; i += 1) {
      const char = text[i];
      if (inString) {
        out += char;
        if (char === '\\') {
          out += text[i + 1] ?? '';
          i += 1;
        } else if (char === '"') {
          inString = false;
        }
        continue;
      }
      if (char === '"') {
        inString = true;
        out += char;
        continue;
      }
      if (char === '/' && text[i + 1] === '/') {
        while (i < text.length && text[i] !== '\n') i += 1;
        continue;
      }
      out += char;
    }
    return JSON.parse(out.replace(/,(\s*[}\]])/g, '$1'));
  }

  const KNOWN_TRANSPORTS = new Set(['stdio', 'sse', 'http', 'ws']);
  const KNOWN_KEYS = new Set(['type', 'url', 'command', 'args', 'env', 'headers']);

  /**
   * Validate one mcpServers document against the Claude Code configuration
   * schema rules the README must honor.
   * @param text - fenced snippet content
   * @returns list of problems (empty when valid)
   */
  function mcpSnippetProblems(text: string): string[] {
    const problems: string[] = [];
    let parsed: unknown;
    try {
      parsed = parseJsonc(text);
    } catch (error) {
      return [`snippet is not parseable JSONC: ${String(error)}`];
    }
    const servers = (parsed as { mcpServers?: unknown })?.mcpServers;
    if (typeof servers !== 'object' || servers === null || Array.isArray(servers)) {
      return ['snippet has no mcpServers object'];
    }
    for (const [name, entry] of Object.entries(servers as Record<string, unknown>)) {
      if (typeof entry !== 'object' || entry === null) {
        problems.push(`server "${name}" is not an object`);
        continue;
      }
      const record = entry as Record<string, unknown>;
      for (const key of Object.keys(record)) {
        if (!KNOWN_KEYS.has(key)) {
          problems.push(`server "${name}" uses unknown key "${key}" (schema key is "type")`);
        }
      }
      if ('url' in record && typeof record.type !== 'string') {
        problems.push(`server "${name}" has a "url" but no "type"`);
      }
      if (typeof record.type === 'string' && !KNOWN_TRANSPORTS.has(record.type)) {
        problems.push(`server "${name}" has unknown type "${record.type}"`);
      }
      if ('url' in record && record.type === 'stdio') {
        problems.push(`server "${name}" pairs "url" with type "stdio"`);
      }
    }
    return problems;
  }

  /** All fenced snippets that declare an mcpServers object. */
  const snippets: { fence: number; text: string }[] = [];
  {
    const fence = /```(?:jsonc|json)\r?\n([\s\S]*?)```/g;
    let match: RegExpExecArray | null;
    let index = 0;
    while ((match = fence.exec(readme)) !== null) {
      index += 1;
      if (match[1].includes('mcpServers')) snippets.push({ fence: index, text: match[1] });
    }
  }

  it('the README contains at least one mcpServers snippet', () => {
    expect(snippets.length).toBeGreaterThan(0);
  });

  it.each(snippets.map((snippet) => [snippet.fence, snippet] as const))(
    'mcp.json snippet %i validates',
    (_fence, snippet) => {
      expect(mcpSnippetProblems(snippet.text)).toEqual([]);
    },
  );

  it('negative control: the pre-fix README shape (transport key, no type) is rejected', () => {
    const legacy = JSON.stringify(
      { mcpServers: { dc3: { transport: 'http', url: 'http://localhost:8000/mcp' } } },
      null,
      2,
    );
    const problems = mcpSnippetProblems(legacy);
    expect(problems).toContain('server "dc3" uses unknown key "transport" (schema key is "type")');
    expect(problems).toContain('server "dc3" has a "url" but no "type"');
  });

  it('negative control: url paired with the wrong transport type is rejected', () => {
    const problems = mcpSnippetProblems(
      JSON.stringify({ mcpServers: { dc3: { type: 'stdio', url: 'http://localhost:8000/mcp' } } }),
    );
    expect(problems).toContain('server "dc3" pairs "url" with type "stdio"');
  });
});

describe('docs-lint: README mcp.json snippets are strict JSON (G18)', () => {
  /**
   * Strip `//` line comments (string-aware) WITHOUT comma stripping: the
   * fenced body must already be strict-JSON parseable, so a pasted
   * `.mcp.json` works verbatim instead of failing on jsonc trailing commas.
   * @param text - fenced snippet content
   * @returns the comment-stripped text
   */
  function stripCommentsOnly(text: string): string {
    let out = '';
    let inString = false;
    for (let i = 0; i < text.length; i += 1) {
      const char = text[i];
      if (inString) {
        out += char;
        if (char === '\\') {
          out += text[i + 1] ?? '';
          i += 1;
        } else if (char === '"') {
          inString = false;
        }
        continue;
      }
      if (char === '"') {
        inString = true;
        out += char;
        continue;
      }
      if (char === '/' && text[i + 1] === '/') {
        while (i < text.length && text[i] !== '\n') i += 1;
        continue;
      }
      out += char;
    }
    return out;
  }

  /** All fenced snippets that declare an mcpServers object. */
  const mcpFences: { fence: number; text: string }[] = [];
  {
    const fence = /```(?:jsonc|json)\r?\n([\s\S]*?)```/g;
    let match: RegExpExecArray | null;
    let index = 0;
    while ((match = fence.exec(readme)) !== null) {
      index += 1;
      if (match[1].includes('mcpServers')) mcpFences.push({ fence: index, text: match[1] });
    }
  }

  it('the README contains at least one mcpServers fence to guard', () => {
    expect(mcpFences.length).toBeGreaterThan(0);
  });

  it.each(mcpFences.map((snippet) => [snippet.fence, snippet] as const))(
    'mcp.json snippet %i is strict-JSON parseable after comment stripping only',
    (_fence, snippet) => {
      expect(() => JSON.parse(stripCommentsOnly(snippet.text))).not.toThrow();
    },
  );

  it('negative control: the pre-fix jsonc trailing commas are rejected by strict JSON', () => {
    const legacy = [
      '// Claude Code: .mcp.json',
      '{',
      '  "mcpServers": {',
      '    "dc3": {',
      '      "type": "http",',
      '      "url": "http://localhost:8000/mcp",',
      '    },',
      '  },',
      '}',
    ].join('\n');
    expect(() => JSON.parse(stripCommentsOnly(legacy))).toThrow();
  });

  it('negative control: the comment stripper keeps // inside string values', () => {
    const parsed = JSON.parse(
      stripCommentsOnly('{ "url": "http://localhost:8000/mcp" } // trailing note'),
    ) as Record<string, string>;
    expect(parsed.url).toBe('http://localhost:8000/mcp');
  });
});
