import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Command } from 'commander';
import { SilentExit } from '../src/utils/format.js';

type FetchInit = { method?: string; headers?: unknown; body?: unknown };
type FetchCall = { url: string; init: FetchInit };

const fetchCalls: FetchCall[] = [];

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({ gateway: 'http://gw.test/', tenant: 't', username: 'u' })),
    getActiveProfileName: vi.fn(async () => 'default'),
    load: vi.fn(async () => ({ current_profile: 'default' })),
    getSettings: vi.fn(async () => ({ renewal_threshold_hours: 12 })),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    needsRenewal: vi.fn(async () => false),
  },
}));

import { registerPointCommand } from '../src/commands/point.js';

function buildProgram(): Command {
  const program = new Command();
  program.exitOverride();
  program.configureOutput({ writeErr: () => undefined });
  registerPointCommand(program);
  return program;
}

/** Run a command expected to succeed; returns the printed output. */
async function run(args: string[]): Promise<string> {
  const program = buildProgram();
  let output = '';
  vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
    output += String(chunk);
    return true;
  });
  try {
    await program.parseAsync(args, { from: 'user' });
  } catch (error) {
    if (!(error instanceof SilentExit)) throw error;
  } finally {
    (process.stdout.write as ReturnType<typeof vi.spyOn>).mockRestore();
  }
  return output;
}

/** Run a command expected to fail; returns the rejection. */
async function runExpectingFailure(args: string[]): Promise<unknown> {
  return buildProgram()
    .parseAsync(args, { from: 'user' })
    .then(() => null, (error: unknown) => error);
}

function addArgs(extra: string[]): string[] {
  return ['point', 'add', '--name', 'pt', '--profile-id', '100', ...extra];
}

describe('point numeric fields (F039)', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: FetchInit) => {
        fetchCalls.push({ url, init: init ?? {}});
        if (String(url).includes('/get_by_id')) {
          return new Response(JSON.stringify({ id: '5', version: 7, pointName: 'old', profileId: '100' }), { status: 200 });
        }
        return new Response(String(init?.body), { status: 200 });
      }),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('add sends baseValue and multiple as JSON numbers', async () => {
    await run(addArgs(['--type', 'INT', '--rw', 'READ_WRITE', '--value-decimal', '5', '--base-value', '2', '--multiple', '0.5', '--unit', 'C']));
    const body = JSON.parse(String(fetchCalls[0].init.body));
    // The gateway PointVO declares BigDecimal fields: the wire type must be
    // number, never the raw option string (report F039).
    expect(body.baseValue).toBe(2);
    expect(body.multiple).toBe(0.5);
    expect(typeof body.baseValue).toBe('number');
    expect(typeof body.multiple).toBe('number');
  });

  it('add defaults send JSON numbers 0 and 1', async () => {
    await run(addArgs([]));
    const body = JSON.parse(String(fetchCalls[0].init.body));
    expect(body.baseValue).toBe(0);
    expect(body.multiple).toBe(1);
  });

  it('add accepts negative decimal base values', async () => {
    await run(addArgs(['--base-value', '-1.5']));
    expect(JSON.parse(String(fetchCalls[0].init.body)).baseValue).toBe(-1.5);
  });

  it('update sends baseValue and multiple as JSON numbers', async () => {
    await run(['point', 'update', '5', '--version', '3', '--base-value', '2.5', '--multiple', '0.5']);
    const body = JSON.parse(String(fetchCalls[1].init.body));
    expect(body.baseValue).toBe(2.5);
    expect(body.multiple).toBe(0.5);
    expect(typeof body.baseValue).toBe('number');
    expect(typeof body.multiple).toBe('number');
  });

  it.each(['abc', '', '0x10', '1e2', ' 5', '5.'])('rejects the non-numeric base value %s before any request', async (value) => {
    await expect(buildProgram().parseAsync(addArgs(['--base-value', value]), { from: 'user' }))
      .rejects.toMatchObject({ code: 'commander.invalidArgument' });
    expect(fetchCalls).toHaveLength(0);
  });

  it.each(['abc', '', '0x10', '1e2'])('rejects the non-numeric multiple %s before any request', async (value) => {
    await expect(buildProgram().parseAsync(addArgs(['--multiple', value]), { from: 'user' }))
      .rejects.toMatchObject({ code: 'commander.invalidArgument' });
    expect(fetchCalls).toHaveLength(0);
  });
});

describe('point history pagination (F021)', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: FetchInit) => {
        fetchCalls.push({ url, init: init ?? {}});
        return new Response('[]', { status: 200 });
      }),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('sends the parsed limit on the wire', async () => {
    await run(['point', 'history', '456', '--device-id', 'dev-1', '--limit', '50']);
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/data/point_value/history?device_id=dev-1&point_id=456&limit=50');
  });

  it.each(['-5', 'abc', '', '0', '1e2'])('rejects the limit %s before any request (shared parser)', async (value) => {
    await expect(buildProgram().parseAsync(
      ['point', 'history', '456', '--device-id', 'dev-1', '--limit', value],
      { from: 'user' },
    )).rejects.toMatchObject({ code: 'commander.invalidArgument' });
    expect(fetchCalls).toHaveLength(0);
  });

  it('requires --device-id as a typed validation error before any request', async () => {
    expect(await runExpectingFailure(['point', 'history', '456'])).toMatchObject({
      kind: 'validation',
      exitCode: 1,
      message: '--device-id is required for history query',
    });
    expect(fetchCalls).toHaveLength(0);
  });

  it('point write requires --device-id as a typed validation error before any request', async () => {
    expect(await runExpectingFailure(['point', 'write', '456', '--value', '1'])).toMatchObject({
      kind: 'validation',
      exitCode: 1,
      message: '--device-id is required for write operations',
    });
    expect(fetchCalls).toHaveLength(0);
  });
});
