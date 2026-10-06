import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Readable } from 'node:stream';
import { Command } from 'commander';
import { SilentExit } from '../src/utils/format.js';
import { handleFatalError } from '../src/core/errors.js';

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({ gateway: 'http://gw.test/' })),
    getActiveProfileName: vi.fn(async () => 'default'),
    getSettings: vi.fn(async () => ({})),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(),
    needsRenewal: vi.fn(async () => false),
    buildHeaders: (state: Record<string, unknown>) => ({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${state.token}`,
    }),
  },
}));

import { registerToolCommand } from '../src/commands/tool.js';
import { tokenManager } from '../src/core/token-manager.js';

const oauthState = { token: 'rs256.jwt.value', authType: 'oauth' };

/** Result of one program invocation routed through the entry failure path. */
interface RunResult {
  stdout: string;
  stderr: string;
  exitCode: number;
}

/**
 * Parse argv like the bin entry does: exitOverride surfaces commander
 * failures as rejections, and escaped failures flow through the same
 * chokepoint runCli uses.
 * @param args - arguments after `dc3`
 * @returns captured streams and effective exit code
 */
async function run(args: string[]): Promise<RunResult> {
  const program = new Command();
  program.exitOverride();
  registerToolCommand(program);
  let stdout = '';
  let stderr = '';
  process.exitCode = 0;
  const outSpy = vi
    .spyOn(process.stdout, 'write')
    .mockImplementation((chunk) => ((stdout += String(chunk)), true));
  const errSpy = vi
    .spyOn(process.stderr, 'write')
    .mockImplementation((chunk) => ((stderr += String(chunk)), true));
  try {
    try {
      await program.parseAsync(args, { from: 'user' });
    } catch (error) {
      if (!(error instanceof SilentExit)) {
        try {
          handleFatalError(error as Error);
        } catch {
          // handleFatalError reported and re-threw by contract
        }
      }
    }
  } finally {
    outSpy.mockRestore();
    errSpy.mockRestore();
  }
  return { stdout, stderr, exitCode: process.exitCode ?? 0 };
}

/**
 * Decode the single machine envelope a failure path may write to stdout.
 * @param stdout - the captured stdout of a failed invocation
 * @returns the parsed envelope, after asserting it is a single document
 */
function parseEnvelope(stdout: string): {
  ok: boolean;
  error: { kind: string; code: string; message: string };
} {
  const lines = stdout.split('\n').filter((line) => line.trim() !== '');
  expect(lines, `exactly one envelope document, got: ${stdout}`).toHaveLength(1);
  return JSON.parse(lines[0]) as { ok: boolean; error: { kind: string; code: string; message: string } };
}

/**
 * Fetch mock answering a JSON-RPC result while recording the request body.
 * @param result - the JSON-RPC result field of the response
 * @returns a fetch stub for the /mcp endpoint
 */
function mcpFetch(result: unknown) {
  return vi.fn(async (_url: string, init?: RequestInit) => {
    const request = JSON.parse(String(init?.body)) as Record<string, unknown>;
    return new Response(JSON.stringify({ jsonrpc: '2.0', id: request.id, result }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });
  });
}

/**
 * Decode the JSON-RPC params of the single captured /mcp request.
 * @param fetchMock - the fetch stub whose single call is inspected
 * @returns the parsed JSON-RPC params object
 */
function mcpRequestBody(fetchMock: ReturnType<typeof mcpFetch>): Record<string, unknown> {
  expect(fetchMock).toHaveBeenCalledTimes(1);
  return JSON.parse(String(fetchMock.mock.calls[0][1]?.body)) as Record<string, unknown>;
}

let directory: string;

beforeEach(() => {
  vi.mocked(tokenManager.getState).mockResolvedValue(oauthState);
  vi.stubGlobal('fetch', mcpFetch({ tools: [] }));
  process.exitCode = 0;
  directory = '';
});

afterEach(async () => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  process.exitCode = 0;
  if (directory) {
    await rm(directory, { recursive: true, force: true });
  }
});

describe('tools list (F009)', () => {
  it('prints exactly one JSON document and exits 0 on success', async () => {
    vi.stubGlobal(
      'fetch',
      mcpFetch({ tools: [{ name: 'read_device', title: 'Read device' }] }),
    );
    const result = await run(['tools', 'list', '--format', 'json']);

    expect(result.exitCode).toBe(0);
    expect(result.stderr).toBe('');
    // Single-document contract: concatenated documents would fail this parse.
    expect(JSON.parse(result.stdout)).toEqual([
      { name: 'read_device', title: 'Read device', category: '' },
    ]);
  });

  it('an auth failure keeps exit 3 with one envelope and one stderr line', async () => {
    vi.mocked(tokenManager.getState).mockResolvedValue(null);
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const result = await run(['tools', 'list', '--format', 'json']);

    expect(result.exitCode).toBe(3);
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.stderr.startsWith('Error: ')).toBe(true);
    expect(result.stderr.split('\n')).toHaveLength(2);
    const envelope = parseEnvelope(result.stdout);
    expect(envelope.ok).toBe(false);
    expect(envelope.error.kind).toBe('auth');
  });

  it('a network failure keeps exit 2 with one envelope', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('fetch failed');
      }),
    );
    const result = await run(['tools', 'list', '--format', 'json']);

    expect(result.exitCode).toBe(2);
    const envelope = parseEnvelope(result.stdout);
    expect(envelope.error.kind).toBe('network');
    expect(result.stderr.startsWith('Error: ')).toBe(true);
  });
});

describe('tools call --args object validation (F034, F009)', () => {
  it('round-trips a JSON object and prints exactly one document with exit 0', async () => {
    const fetchMock = mcpFetch({ content: [{ text: 'ok' }] });
    vi.stubGlobal('fetch', fetchMock);
    const result = await run(['tools', 'call', 'device.list', '--args', '{"limit":1}', '--format', 'json']);

    expect(result.exitCode).toBe(0);
    expect(result.stderr).toBe('');
    expect(JSON.parse(result.stdout)).toEqual({ content: [{ text: 'ok' }] });
    expect(mcpRequestBody(fetchMock).params).toEqual({
      name: 'device.list',
      arguments: { limit: 1 },
    });
  });

  it.each([['--args', '[1,2,3]'], ['--args', 'null'], ['--args', '42'], ['--args', '"abc"']])(
    '%s %s is rejected client-side with a structured validation error',
    async (_flag, value) => {
      const fetchMock = vi.fn();
      vi.stubGlobal('fetch', fetchMock);
      const result = await run(['tools', 'call', 'device.list', '--args', value, '--format', 'json']);

      expect(result.exitCode).toBe(1);
      expect(fetchMock).not.toHaveBeenCalled();
      const envelope = parseEnvelope(result.stdout);
      expect(envelope.error.kind).toBe('validation');
      expect(envelope.error.message).toBe('--args must be a JSON object');
      expect(result.stderr).toContain('--args must be a JSON object');
    },
  );

  it('inline --args that is not JSON fails with exactly one document and exit 1', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const result = await run(['tools', 'call', 'device.list', '--args', 'not-json', '--format', 'json']);

    expect(result.exitCode).toBe(1);
    expect(fetchMock).not.toHaveBeenCalled();
    const envelope = parseEnvelope(result.stdout);
    expect(envelope.error.kind).toBe('validation');
    expect(envelope.error.message).toBe('--args is not valid JSON');
    expect(result.stderr).toBe('Error: --args is not valid JSON\n');
  });
});

describe('tools call --args-file (F032)', () => {
  it('sends a >200KB argument object from a file verbatim on the wire', async () => {
    const blob = 'x'.repeat(220_000);
    directory = await mkdtemp(join(tmpdir(), 'dc3-cli-tool-'));
    const argsFile = join(directory, 'args.json');
    await writeFile(argsFile, JSON.stringify({ blob }), 'utf8');
    const fetchMock = mcpFetch({ ok: true });
    vi.stubGlobal('fetch', fetchMock);

    const result = await run(['tools', 'call', 'bulk.load', '--args-file', argsFile, '--format', 'json']);

    expect(result.exitCode).toBe(0);
    const params = mcpRequestBody(fetchMock).params as { arguments: { blob: string } };
    expect(params.arguments.blob).toHaveLength(220_000);
    expect(params.name).toBe('bulk.load');
    expect(JSON.parse(result.stdout)).toEqual({ ok: true });
  });

  it("reads the arguments from stdin via '-'", async () => {
    const originalStdin = process.stdin;
    Object.defineProperty(process, 'stdin', {
      value: Readable.from([JSON.stringify({ from: 'stdin', n: 7 })]),
      configurable: true,
    });
    try {
      const fetchMock = mcpFetch({ ok: true });
      vi.stubGlobal('fetch', fetchMock);
      const result = await run(['tools', 'call', 't.tool', '--args-file', '-', '--format', 'json']);

      expect(result.exitCode).toBe(0);
      const params = mcpRequestBody(fetchMock).params as { arguments: unknown };
      expect(params.arguments).toEqual({ from: 'stdin', n: 7 });
    } finally {
      Object.defineProperty(process, 'stdin', { value: originalStdin, configurable: true });
    }
  });

  it('rejects a --args-file that is not valid JSON before any request', async () => {
    directory = await mkdtemp(join(tmpdir(), 'dc3-cli-tool-'));
    const argsFile = join(directory, 'bad.json');
    await writeFile(argsFile, '{not-json', 'utf8');
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    const result = await run(['tools', 'call', 't.tool', '--args-file', argsFile, '--format', 'json']);

    expect(result.exitCode).toBe(1);
    expect(fetchMock).not.toHaveBeenCalled();
    const envelope = parseEnvelope(result.stdout);
    expect(envelope.error.kind).toBe('validation');
    expect(envelope.error.message).toBe('--args-file is not valid JSON');
  });

  it('rejects a missing --args-file with a structured error and zero requests', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const result = await run([
      'tools', 'call', 't.tool', '--args-file', join(tmpdir(), 'dc3-no-such-args-file.json'), '--format', 'json',
    ]);

    expect(result.exitCode).toBe(1);
    expect(fetchMock).not.toHaveBeenCalled();
    expect(parseEnvelope(result.stdout).error.message).toMatch(/Cannot read --args-file:/u);
  });

  it('rejects specifying both --args and --args-file as a usage error', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const result = await run(['tools', 'call', 't.tool', '--args', '{}', '--args-file', '-', '--format', 'json']);

    expect(result.exitCode).toBe(1);
    expect(fetchMock).not.toHaveBeenCalled();
    expect(parseEnvelope(result.stdout).error.kind).toBe('usage');
    expect(result.stderr).toContain('mutually exclusive');
  });

  it('rejects specifying neither --args nor --args-file as a usage error', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const result = await run(['tools', 'call', 't.tool', '--format', 'json']);

    expect(result.exitCode).toBe(1);
    expect(fetchMock).not.toHaveBeenCalled();
    expect(parseEnvelope(result.stdout).error.kind).toBe('usage');
    expect(result.stderr).toContain("--args <json>' (or '--args-file <path>')");
  });
});
