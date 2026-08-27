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
  vi.spyOn(process, 'exit').mockImplementation(((code?: number) => undefined) as never);
  try {
    await program.parseAsync(args, { from: 'user' });
  } finally {
    (process.stdout.write as ReturnType<typeof vi.spyOn>).mockRestore();
  }
  return out;
}

describe('session command group', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
      fetchCalls.push({ url, init: init ?? {} });
      return new Response(JSON.stringify({ ok: true, data: {} }), { status: 200 });
    }));
  });

  it('list posts paging body to /agentic/session/list', async () => {
    await run(['session', 'list', '--page', '2', '--size', '5']);
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/agentic/session/list');
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({
      page: { current: 2, size: 5 },
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
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({ name: 'new-name' });
  });

  it('delete posts to the delete route with the id param', async () => {
    await run(['session', 'delete', 'c9']);
    expect(fetchCalls[0].init.method).toBe('POST');
    expect(fetchCalls[0].url).toContain('/agentic/session/delete?conversation_id=c9');
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

  it('confirm and reject post action_id as query param', async () => {
    await run(['action', 'confirm', 'a-1']);
    await run(['action', 'reject', 'a-2']);
    expect(fetchCalls[0].init.method).toBe('POST');
    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/agentic/action/confirm?action_id=a-1');
    expect(fetchCalls[1].init.method).toBe('POST');
    expect(fetchCalls[1].url).toBe('http://gw.test/api/v3/agentic/action/reject?action_id=a-2');
  });
});
