import { describe, it, expect, vi, beforeEach } from 'vitest';

const fetchCalls: Array<{ url: string; init: RequestInit }> = [];

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({ gateway: 'http://gw.test/', tenant: 't', username: 'u', credential_store: 'env' })),
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
import { registerAlertCommand } from '../src/commands/alert.js';
import { registerAnalyticsCommand } from '../src/commands/analytics.js';

function buildProgram(): Command {
  const program = new Command();
  program.exitOverride();
  registerAlertCommand(program);
  registerAnalyticsCommand(program);
  return program;
}

async function run(args: string[]): Promise<string> {
  const program = buildProgram();
  let out = '';
  vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
    out += String(chunk);
    return true;
  });
  vi.spyOn(process, 'exit').mockImplementation(((_code?: number) => undefined) as never);
  try {
    await program.parseAsync(args, { from: 'user' });
  } finally {
    (process.stdout.write as ReturnType<typeof vi.spyOn>).mockRestore();
  }
  return out;
}

describe('alert deep-analysis commands', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      fetchCalls.push({ url, init: init ?? {} });
      return new Response(JSON.stringify({ ok: true, data: [] }), { status: 200 });
    }));
  });

  it('aging hits the alert aging endpoint with days passthrough', async () => {
    await run(['alert', 'aging', '--days', '7']);
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/data/dashboard/alert/aging?days=7');
  });

  it('silent-sources maps baseline/limit to snake_case server params', async () => {
    await run(['alert', 'silent-sources', '--baseline-days', '3', '--limit', '5']);
    expect(fetchCalls[0].url).toContain('/dashboard/silent/sources');
    expect(fetchCalls[0].url).toContain('baseline_days=3');
    expect(fetchCalls[0].url).toContain('limit=5');
  });

  it('arbitrary kv pairs pass through for endpoints with extra params', async () => {
    await run(['alert', 'storm-sources', '--query', 'source=device']);
    const url = fetchCalls[0].url;
    expect(url).toContain('/alert/storm_sources');
    expect(url).toContain('source=device');
  });

  it('bulk-confirm posts the parsed items body verbatim', async () => {
    await run(['alert', 'bulk-confirm', '--args', '{"items":[{"source":"device","id":1}]}']);
    expect(fetchCalls[0].init.method).toBe('POST');
    const body = JSON.parse(String(fetchCalls[0].init.body));
    expect(body.items[0]).toEqual({ source: 'device', id: 1 });
  });

  it('unknown analytics op exits with a business error without calling fetch', async () => {
    fetchCalls.length = 0;
    await run(['analytics', 'run', 'nope', '--args', '{}']).catch(() => undefined);
    expect(fetchCalls.length).toBe(0);
  });
});

describe('analytics command group', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      fetchCalls.push({ url, init: init ?? {} });
      return new Response(JSON.stringify({ ok: true, data: { conclusion: 'x' } }), { status: 200 });
    }));
  });

  it('run posts the JSON body to the mapped operation path', async () => {
    await run(['analytics', 'run', 'query_history', '--args', '{"pointId":1,"days":2}']);
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/data/analytics/query_history');
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({ pointId: 1, days: 2 });
  });

  it('list prints the nine operations without network access', async () => {
    fetchCalls.length = 0;
    const out = await run(['analytics', 'list']);
    const parsed = JSON.parse(out);
    expect(parsed.operations).toHaveLength(9);
    expect(parsed.operations).toContain('data_quality_report');
    expect(fetchCalls.length).toBe(0);
  });
});
