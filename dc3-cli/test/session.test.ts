import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SilentExit } from '../src/utils/format.js';
import { NetworkError } from '../src/core/errors.js';

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

const { confirmMock } = vi.hoisted(() => ({ confirmMock: vi.fn(async () => false) }));
vi.mock('../src/utils/prompt.js', () => ({ confirm: confirmMock }));

import { Command } from 'commander';
import { registerSessionCommand, registerActionCommand } from '../src/commands/session.js';

function buildProgram(): Command {
  const program = new Command();
  program.exitOverride();
  registerSessionCommand(program);
  registerActionCommand(program);
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
  confirmMock.mockReset();
  confirmMock.mockResolvedValue(false);
});

describe('session command group', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      fetchCalls.push({ url, init: init ?? {} });
      return new Response(JSON.stringify({ ok: true, data: {} }), { status: 200 });
    }));
  });

  it('list posts paging body to /agentic/session/list', async () => {
    await run(['session', 'list', '--offset', '5', '--limit', '5']);
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/agentic/session/list');
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({
      offset: 5,
      limit: 5,
    });
  });

  it('get/messages encode conversation_id as a query param', async () => {
    await run(['session', 'get', 'conv A&B']);
    await run(['session', 'messages', 'conv A&B']);
    expect(fetchCalls[0].url).toBe(
      'http://gw.test/api/v3/agentic/session/get_by_conversation_id?conversation_id=conv%20A%26B',
    );
    expect(fetchCalls[1].url).toContain('/agentic/message/list?conversation_id=conv%20A%26B');
  });

  it('rename posts the update body with conversation_id param', async () => {
    await run(['session', 'rename', 'c1', '--name', 'new-name']);
    expect(fetchCalls[0].url).toBe(
      'http://gw.test/api/v3/agentic/session/update?conversation_id=c1',
    );
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({ title: 'new-name' });
  });

  it('delete sends DELETE to the delete route with the id param when --yes skips the gate', async () => {
    await run(['session', 'delete', 'c9', '--yes']);
    expect(fetchCalls[0].init.method).toBe('DELETE');
    expect(fetchCalls[0].url).toContain('/agentic/session/delete?conversation_id=c9');
    expect(confirmMock).not.toHaveBeenCalled();
  });

  it('a declined delete confirmation issues zero requests and reports Cancelled (F045)', async () => {
    confirmMock.mockResolvedValue(false);
    const out = await run(['session', 'delete', 'c9']);
    expect(fetchCalls).toHaveLength(0);
    expect(confirmMock).toHaveBeenCalledTimes(1);
    expect(confirmMock).toHaveBeenCalledWith(expect.stringContaining('"c9"'));
    expect(JSON.parse(out)).toEqual({ ok: true, message: 'Cancelled' });
  });

  it('an approved delete confirmation proceeds with the request', async () => {
    confirmMock.mockResolvedValue(true);
    await run(['session', 'delete', 'c9']);
    expect(fetchCalls).toHaveLength(1);
    expect(fetchCalls[0].init.method).toBe('DELETE');
  });
});

describe('action approval loop', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      fetchCalls.push({ url, init: init ?? {} });
      return new Response(JSON.stringify({ ok: true, data: {} }), { status: 200 });
    }));
  });

  it('pending scopes by conversation_id', async () => {
    await run(['action', 'pending', '--conversation-id', 'conv-1']);
    expect(fetchCalls[0].url).toBe(
      'http://gw.test/api/v3/agentic/action/pending?conversation_id=conv-1',
    );
  });

  it('confirm and reject post action_id as query param when --yes skips the gate', async () => {
    await run(['action', 'confirm', 'a-1', '--yes']);
    await run(['action', 'reject', 'a-2', '--yes']);
    expect(fetchCalls[0].init.method).toBe('POST');
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/agentic/action/confirm?action_id=a-1');
    expect(fetchCalls[1].init.method).toBe('POST');
    expect(fetchCalls[1].url).toBe('http://gw.test/api/v3/agentic/action/reject?action_id=a-2');
    expect(confirmMock).not.toHaveBeenCalled();
  });

  it('declined confirm/reject approvals issue zero requests and report Cancelled (F045)', async () => {
    confirmMock.mockResolvedValue(false);
    const confirmed = await run(['action', 'confirm', 'a-1']);
    expect(fetchCalls).toHaveLength(0);
    expect(confirmMock).toHaveBeenCalledWith(expect.stringContaining('Approve'));
    expect(JSON.parse(confirmed)).toEqual({ ok: true, message: 'Cancelled' });

    const rejected = await run(['action', 'reject', 'a-2']);
    expect(fetchCalls).toHaveLength(0);
    expect(confirmMock).toHaveBeenCalledWith(expect.stringContaining('Reject'));
    expect(JSON.parse(rejected)).toEqual({ ok: true, message: 'Cancelled' });
  });

  it('an approved confirmation proceeds with the request', async () => {
    confirmMock.mockResolvedValue(true);
    await run(['action', 'confirm', 'a-1']);
    expect(fetchCalls).toHaveLength(1);
    expect(fetchCalls[0].url).toContain('/agentic/action/confirm?action_id=a-1');
  });

  it('pending forwards offset and limit when supplied', async () => {
    await run(['action', 'pending', '--conversation-id', 'conv-1', '--offset', '10', '--limit', '5']);
    expect(fetchCalls[0].url).toBe(
      'http://gw.test/api/v3/agentic/action/pending?conversation_id=conv-1&offset=10&limit=5',
    );
  });

  it('pending encodes a hostile conversation_id instead of injecting extra params (F017)', async () => {
    await run(['action', 'pending', '--conversation-id', 'a&b=c#d']);
    const url = new URL(fetchCalls[0].url);
    expect(url.searchParams.get('conversation_id')).toBe('a&b=c#d');
    expect(url.searchParams.get('b')).toBeNull();
    expect(url.searchParams.get('c')).toBeNull();
  });

  it.each([
    ['--offset', 'abc'],
    ['--offset', '-5.9'],
    ['--offset', '1e9'],
    ['--limit', '0x10'],
    ['--limit', '2.5'],
  ])('pending rejects a garbage %s %s before any request (F021)', async (flag, value) => {
    const { err } = await runCapture(['action', 'pending', '--conversation-id', 'conv-1', flag, value]);
    expect(err).toBeInstanceOf(Error);
    expect((err as Error).message).toMatch(/integer/i);
    expect(fetchCalls).toHaveLength(0);
  });
});

describe('action failures flow through the typed-error path (F009)', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      fetchCalls.push({ url, init: init ?? {} });
      throw new TypeError('fetch failed');
    }));
  });

  it('a transport failure propagates as NetworkError without a second stdout document', async () => {
    const { out, err } = await runCapture(['action', 'confirm', 'a-1', '--yes']);

    expect(err).toBeInstanceOf(NetworkError);
    // The command itself must not print an error payload: the entry chokepoint
    // owns failure reporting (previously this path printed {ok:false,...} and
    // rewrote the exit code from inside a catch block).
    expect(out).toBe('');
    expect(fetchCalls).toHaveLength(1);
  });

  it('a declined gate plus a failing gateway still issues zero requests', async () => {
    confirmMock.mockResolvedValue(false);
    const { out, err } = await runCapture(['action', 'reject', 'a-2']);
    expect(err).toBeUndefined();
    expect(out).toContain('Cancelled');
    expect(fetchCalls).toHaveLength(0);
  });
});
