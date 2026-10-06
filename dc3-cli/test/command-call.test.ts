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

import { registerCommandCommand } from '../src/commands/command.js';

function buildProgram(): Command {
  const program = new Command();
  program.exitOverride();
  program.configureOutput({ writeErr: () => undefined });
  registerCommandCommand(program);
  return program;
}

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

describe('command call --params validation (F034/F009)', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: FetchInit) => {
        fetchCalls.push({ url, init: init ?? {}});
        return new Response(String(init?.body), { status: 200 });
      }),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('round-trips a JSON object as paramValues', async () => {
    const output = await run([
      'command', 'call', '--device-id', 'dev-1', '--command-id', 'cmd-1',
      '--params', '{"mode":"fast","level":2}', '--format', 'json',
    ]);
    expect(fetchCalls).toHaveLength(1);
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({
      deviceId: 'dev-1',
      commandId: 'cmd-1',
      paramValues: { mode: 'fast', level: 2 },
    });
    expect(JSON.parse(output)).toMatchObject({ paramValues: { mode: 'fast' } });
  });

  it('defaults to an empty object', async () => {
    await run(['command', 'call', '--device-id', 'dev-1', '--command-id', 'cmd-1']);
    expect(JSON.parse(String(fetchCalls[0].init.body)).paramValues).toEqual({});
  });

  it('rejects invalid JSON as a typed validation error before the wire', async () => {
    await expect(buildProgram().parseAsync(
      ['command', 'call', '--device-id', 'dev-1', '--command-id', 'cmd-1', '--params', 'not-json'],
      { from: 'user' },
    )).rejects.toMatchObject({
      kind: 'validation',
      exitCode: 1,
      message: 'Invalid JSON in --params',
    });
    expect(fetchCalls).toHaveLength(0);
  });

  it.each([
    ['array', '[1,2,3]'],
    ['null', 'null'],
    ['number', '42'],
    ['string', '"abc"'],
  ])('rejects the non-object --params %s before the wire', async (_label, value) => {
    await expect(buildProgram().parseAsync(
      ['command', 'call', '--device-id', 'dev-1', '--command-id', 'cmd-1', '--params', value],
      { from: 'user' },
    )).rejects.toMatchObject({
      kind: 'validation',
      exitCode: 1,
      message: '--params must be a JSON object',
    });
    expect(fetchCalls).toHaveLength(0);
  });
});
