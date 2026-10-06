import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SilentExit } from '../src/utils/format.js';
import { ValidationError } from '../src/core/errors.js';

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

const { readFileMock } = vi.hoisted(() => ({ readFileMock: vi.fn() }));
vi.mock('node:fs/promises', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:fs/promises')>();
  return { ...actual, readFile: readFileMock };
});

const { readFileSyncMock } = vi.hoisted(() => ({ readFileSyncMock: vi.fn() }));
vi.mock('node:fs', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:fs')>();
  return { ...actual, readFileSync: readFileSyncMock };
});

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
  vi.spyOn(process, 'exit').mockImplementation((() => undefined) as never);
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

beforeEach(() => {
  readFileMock.mockReset();
  readFileMock.mockImplementation(async () => {
    throw new Error('unexpected file read in this test');
  });
  readFileSyncMock.mockReset();
  readFileSyncMock.mockImplementation(() => {
    throw new Error('unexpected sync file read in this test');
  });
});

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
    const url = new URL(fetchCalls[0].url);
    expect(url.pathname).toBe('/api/v3/data/dashboard/alert/storm_sources');
    expect(url.searchParams.get('source')).toBe('device');
  });

  it('repeated --query flags all reach the URL instead of last-write-wins (F019)', async () => {
    await run(['alert', 'storm-sources', '--query', 'a=1', '--query', 'b=2']);
    const url = new URL(fetchCalls[0].url);
    expect(url.searchParams.get('a')).toBe('1');
    expect(url.searchParams.get('b')).toBe('2');
  });

  it.each(['noequal', '=value'])('a malformed --query %s fails fast with zero requests (F019)', async (bad) => {
    const { err } = await runCapture(['alert', 'storm-sources', '--query', bad]);
    expect(err).toBeInstanceOf(ValidationError);
    expect((err as Error).message).toContain(JSON.stringify(bad));
    expect(fetchCalls).toHaveLength(0);
  });

  it('adversarial query values are encoded, never injected into the query structure (F017)', async () => {
    await run(['alert', 'storm-sources', '--query', 'granularity=hour&evil=1', '--query', 'win=24#section']);
    const url = new URL(fetchCalls[0].url);
    expect(url.searchParams.get('granularity')).toBe('hour&evil=1');
    expect(url.searchParams.get('win')).toBe('24#section');
    expect(url.searchParams.get('evil')).toBeNull();
    expect(fetchCalls[0].url).toContain('hour%26evil%3D1');
    expect(fetchCalls[0].url).toContain('24%23section');
    expect(fetchCalls[0].url).not.toContain('#section');
  });

  it('confirm encodes a hostile source instead of injecting a parameter (F017)', async () => {
    await run(['alert', 'confirm', '--source', 'dev&x', '--id', '7']);
    const url = new URL(fetchCalls[0].url);
    expect(url.searchParams.get('source')).toBe('dev&x');
    expect(url.searchParams.get('x')).toBeNull();
  });

  it.each([
    ['alert', 'latest', '--limit', 'abc'],
    ['alert', 'trend', '--days', '-5'],
    ['alert', 'top-sources', '--limit', '1e9'],
    ['alert', 'silent-sources', '--baseline-days', '0'],
    ['alert', 'coverage-gap', '--limit', '2.5'],
    ['alert', 'type-distribution', '--days', '0x1f'],
  ])('%s rejects a garbage numeric %s=%s before any request (F021)', async (...argv) => {
    const [group, sub, flag, value] = argv as [string, string, string, string];
    const { err } = await runCapture([group, sub, flag, value]);
    expect(err).toBeInstanceOf(Error);
    expect((err as Error).message).toMatch(/integer/i);
    expect(fetchCalls).toHaveLength(0);
  });

  it('bulk-confirm posts the parsed items body verbatim', async () => {
    await run(['alert', 'bulk-confirm', '--args', '{"items":[{"source":"device","id":1}]}']);
    expect(fetchCalls[0].init.method).toBe('POST');
    const body = JSON.parse(String(fetchCalls[0].init.body));
    expect(body.items[0]).toEqual({ source: 'device', id: 1 });
  });

  it.each([
    '[1,2]',
    'null',
    '42',
    '"abc"',
  ])('bulk-confirm rejects a non-object --args body %s with zero requests (F034)', async (bad) => {
    const { err } = await runCapture(['alert', 'bulk-confirm', '--args', bad]);
    expect(err).toBeInstanceOf(ValidationError);
    expect((err as Error).message).toContain('must be a JSON object');
    expect(fetchCalls).toHaveLength(0);
  });

  it('bulk-confirm reads a large body from --args-file and posts it intact (F032)', async () => {
    const payload = { items: Array.from({ length: 5000 }, (_, i) => ({ source: 'device', id: i })) };
    const raw = JSON.stringify(payload);
    expect(raw.length).toBeGreaterThan(100_000);
    readFileMock.mockResolvedValue(raw);
    await run(['alert', 'bulk-confirm', '--args-file', 'bulk.json']);
    expect(readFileMock).toHaveBeenCalledWith('bulk.json', 'utf8');
    expect(fetchCalls).toHaveLength(1);
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual(payload);
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

  it('unknown analytics op exits with a validation error without calling fetch', async () => {
    const { err } = await runCapture(['analytics', 'run', 'nope', '--args', '{}']);
    expect(err).toBeInstanceOf(ValidationError);
    expect((err as Error).message).toContain('Unknown operation: nope');
    expect(fetchCalls.length).toBe(0);
  });

  it('run rejects an invalid inline --args JSON with zero requests', async () => {
    const { err } = await runCapture(['analytics', 'run', 'query_latest', '--args', 'not-json']);
    expect(err).toBeInstanceOf(ValidationError);
    expect((err as Error).message).toBe('--args is not valid JSON');
    expect(fetchCalls).toHaveLength(0);
  });

  it.each([
    '[1,2]',
    'null',
    '42',
    '"abc"',
  ])('run rejects a non-object --args body %s with zero requests (F034)', async (bad) => {
    const { err } = await runCapture(['analytics', 'run', 'query_latest', '--args', bad]);
    expect(err).toBeInstanceOf(ValidationError);
    expect((err as Error).message).toContain('must be a JSON object');
    expect(fetchCalls).toHaveLength(0);
  });

  it('run reads the body from --args-file and posts it intact (F032)', async () => {
    readFileMock.mockResolvedValue('{"pointId":7,"days":3}');
    await run(['analytics', 'run', 'query_history', '--args-file', 'body.json']);
    expect(readFileMock).toHaveBeenCalledWith('body.json', 'utf8');
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/data/analytics/query_history');
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({ pointId: 7, days: 3 });
  });

  it("run reads the body from stdin when --args-file is '-' (F032)", async () => {
    readFileSyncMock.mockReturnValue('{"pointId":9}');
    await run(['analytics', 'run', 'query_latest', '--args-file', '-']);
    expect(readFileSyncMock).toHaveBeenCalledWith(0, 'utf8');
    expect(readFileMock).not.toHaveBeenCalled();
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({ pointId: 9 });
  });

  it("run maps a failing stdin read for '-' to the same typed validation error (F032)", async () => {
    readFileSyncMock.mockImplementation(() => {
      throw Object.assign(new Error('read EPERM'), { code: 'EPERM' });
    });
    const { err } = await runCapture(['analytics', 'run', 'query_latest', '--args-file', '-']);
    expect(err).toBeInstanceOf(ValidationError);
    expect((err as Error).message).toBe('cannot read --args-file "-" (EPERM)');
    expect(fetchCalls).toHaveLength(0);
  });

  it('run rejects an unreadable --args-file naming the user-supplied path, not a resolved one (F032)', async () => {
    const missing = Object.assign(new Error("ENOENT: no such file or directory, open 'C:\\resolved\\abs\\nope.json'"), {
      code: 'ENOENT',
    });
    readFileMock.mockRejectedValue(missing);
    const { err } = await runCapture(['analytics', 'run', 'query_latest', '--args-file', 'nope.json']);
    expect(err).toBeInstanceOf(ValidationError);
    const message = (err as Error).message;
    expect(message).toContain('cannot read --args-file "nope.json"');
    expect(message).toContain('ENOENT');
    expect(message).not.toContain('C:\\resolved');
    expect(fetchCalls).toHaveLength(0);
  });

  it('run rejects a --args-file whose content is not JSON (F032)', async () => {
    readFileMock.mockResolvedValue('{{');
    const { err } = await runCapture(['analytics', 'run', 'query_latest', '--args-file', 'bad.json']);
    expect(err).toBeInstanceOf(ValidationError);
    expect((err as Error).message).toBe('--args-file is not valid JSON');
    expect(fetchCalls).toHaveLength(0);
  });

  it('run rejects --args together with --args-file (F032)', async () => {
    const { err } = await runCapture([
      'analytics',
      'run',
      'query_latest',
      '--args',
      '{}',
      '--args-file',
      'body.json',
    ]);
    expect(err).toBeInstanceOf(ValidationError);
    expect((err as Error).message).toContain('mutually exclusive');
    expect(fetchCalls).toHaveLength(0);
  });

  it('run rejects the absence of both --args and --args-file (F032)', async () => {
    const { err } = await runCapture(['analytics', 'run', 'query_latest']);
    expect(err).toBeInstanceOf(ValidationError);
    expect((err as Error).message).toContain('one of --args or --args-file is required');
    expect(fetchCalls).toHaveLength(0);
  });
});
