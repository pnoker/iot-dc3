import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SilentExit } from '../src/utils/format.js';

const fetchCalls: Array<{ url: string; init: RequestInit }> = [];

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({ gateway: 'http://gw.test/', tenant: 't', username: 'u', credential_store: 'env' })),
    getActiveProfileName: vi.fn(async () => 'default'),
    load: vi.fn(async () => ({ current_profile: 'default' })),
    getSettings: vi.fn(async () => ({ renewal_threshold_hours: 12, output_format: 'json', color: false, retry_count: 1 })),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    needsRenewal: vi.fn(async () => false),
  },
}));

import { Command } from 'commander';
import { registerDashboardCommand } from '../src/commands/dashboard.js';

function buildProgram(): Command {
  const program = new Command();
  program.exitOverride();
  registerDashboardCommand(program);
  return program;
}

async function run(args: string[]): Promise<string> {
  const program = buildProgram();
  let out = '';
  vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
    out += String(chunk);
    return true;
  });
  vi.spyOn(process, 'exit').mockImplementation(((code?: number) => undefined) as never);
  try {
    try { await program.parseAsync(args, { from: 'user' }); } catch (e) { if (!(e instanceof SilentExit)) throw e; }
  } finally {
    (process.stdout.write as ReturnType<typeof vi.spyOn>).mockRestore();
    (process.exit as ReturnType<typeof vi.spyOn>).mockRestore();
  }
  return out;
}

/** Result of one invocation: captured stdout plus the rejection, when the command failed. */
interface Capture {
  out: string;
  err?: unknown;
}

async function runCapture(args: string[]): Promise<Capture> {
  const program = buildProgram();
  let out = '';
  let err: unknown;
  const spy = vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
    out += String(chunk);
    return true;
  });
  try {
    try {
      await program.parseAsync(args, { from: 'user' });
    } catch (e) {
      if (!(e instanceof SilentExit)) err = e;
    }
  } finally {
    spy.mockRestore();
  }
  return { out, err };
}

describe('dashboard command group', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      fetchCalls.push({ url, init: init ?? {} });
      return new Response(JSON.stringify({ ok: true, data: [] }), { status: 200 });
    }));
  });

  it('timeseries applies the documented defaults', async () => {
    await run(['dashboard', 'timeseries']);
    expect(fetchCalls[0].url).toBe(
      'http://gw.test/api/v3/data/dashboard/stats/timeseries?granularity=hour&range_hours=24',
    );
  });

  it('timeseries forwards explicit granularity and range-hours', async () => {
    await run(['dashboard', 'timeseries', '--granularity', 'day', '--range-hours', '72']);
    expect(fetchCalls[0].url).toBe(
      'http://gw.test/api/v3/data/dashboard/stats/timeseries?granularity=day&range_hours=72',
    );
  });

  it("timeseries rejects a granularity that would inject 'hour&evil=1' (F017)", async () => {
    const { err } = await runCapture(['dashboard', 'timeseries', '--granularity', 'hour&evil=1']);
    expect(err).toBeInstanceOf(Error);
    expect((err as Error).message).toContain('Allowed choices are hour, day');
    expect(fetchCalls).toHaveLength(0);
    expect(JSON.stringify(fetchCalls)).not.toContain('evil');
  });

  it.each(['abc', '-5', '1e9', '2.5', '0x10'])(
    'timeseries rejects a garbage --range-hours %s before any request (F021)',
    async (value) => {
      const { err } = await runCapture(['dashboard', 'timeseries', '--range-hours', value]);
      expect(err).toBeInstanceOf(Error);
      expect((err as Error).message).toMatch(/integer/i);
      expect(fetchCalls).toHaveLength(0);
    },
  );

  it('top applies the documented defaults', async () => {
    await run(['dashboard', 'top']);
    expect(fetchCalls[0].url).toBe(
      'http://gw.test/api/v3/data/dashboard/top?dimension=device&range_hours=24&limit=10',
    );
  });

  it('top forwards explicit dimension, window, and limit', async () => {
    await run(['dashboard', 'top', '--dimension', 'driver', '--range-hours', '7', '--limit', '3']);
    expect(fetchCalls[0].url).toBe(
      'http://gw.test/api/v3/data/dashboard/top?dimension=driver&range_hours=7&limit=3',
    );
  });

  it("top rejects a dimension that would inject 'device&evil=1' (F017)", async () => {
    const { err } = await runCapture(['dashboard', 'top', '--dimension', 'device&evil=1']);
    expect(err).toBeInstanceOf(Error);
    expect((err as Error).message).toContain('Allowed choices are device, driver, point');
    expect(fetchCalls).toHaveLength(0);
  });

  it.each([
    ['--limit', 'abc'],
    ['--limit', '-3'],
    ['--limit', '1e9'],
    ['--range-hours', '0.5'],
  ])('top rejects a garbage %s %s before any request (F021)', async (flag, value) => {
    const { err } = await runCapture(['dashboard', 'top', flag, value]);
    expect(err).toBeInstanceOf(Error);
    expect((err as Error).message).toMatch(/integer/i);
    expect(fetchCalls).toHaveLength(0);
  });

  it('topology applies the default cardinality mode', async () => {
    await run(['dashboard', 'topology']);
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/data/dashboard/topology?mode=cardinality');
  });

  it('topology accepts the volume mode and rejects unknown modes (F017)', async () => {
    await run(['dashboard', 'topology', '--mode', 'volume']);
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/data/dashboard/topology?mode=volume');

    const { err } = await runCapture(['dashboard', 'topology', '--mode', 'volume&evil=1']);
    expect(err).toBeInstanceOf(Error);
    expect((err as Error).message).toContain('Allowed choices are cardinality, volume');
    expect(fetchCalls).toHaveLength(1); // only the volume request above
  });

  it('stream applies the default limit and rejects garbage values (F021)', async () => {
    await run(['dashboard', 'stream']);
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/data/dashboard/stream?limit=20');

    const { err } = await runCapture(['dashboard', 'stream', '--limit', 'abc']);
    expect(err).toBeInstanceOf(Error);
    expect((err as Error).message).toMatch(/integer/i);
    expect(fetchCalls).toHaveLength(1);
  });

  it('device-stats maps --top-n to the top_n param and rejects garbage values (F021)', async () => {
    await run(['dashboard', 'device-stats', '--top-n', '5']);
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/manager/dashboard/device/stats?top_n=5');

    const { err } = await runCapture(['dashboard', 'device-stats', '--top-n', '-3']);
    expect(err).toBeInstanceOf(Error);
    expect((err as Error).message).toMatch(/integer/i);
    expect(fetchCalls).toHaveLength(1);
  });
});
