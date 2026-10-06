import { describe, it, expect, vi, beforeEach } from 'vitest';

const { getState } = vi.hoisted(() => ({ getState: vi.fn() }));

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({ gateway: 'http://gw.example.com/' })),
    getActiveProfileName: vi.fn(async () => 'default'),
    load: vi.fn(async () => ({ current_profile: 'default' })),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState,
    buildHeaders: (state: Record<string, unknown>) => ({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${state.token}`,
    }),
  },
}));

import { ApiError, AuthError } from '../src/core/client.js';
import { McpClient } from '../src/core/mcp.js';

const oauthState = { token: 'rs256.jwt.value', authType: 'oauth' };
const loginState = { token: 'hs.jwt.value', authType: 'login' };

/**
 * Fetch mock answering application/json while echoing the request id (F040).
 * @param mutate - optional payload mutation applied before the response is built
 * @returns a fetch stub returning the JSON-RPC response
 */
function echoJsonRpc(mutate?: (_payload: Record<string, unknown>) => void) {
  return vi.fn(async (_url: string, init?: RequestInit) => {
    const request = JSON.parse(String(init?.body)) as Record<string, unknown>;
    const payload: Record<string, unknown> = { jsonrpc: '2.0', id: request.id };
    mutate?.(payload);
    return new Response(JSON.stringify(payload), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    });
  });
}

/**
 * Fetch mock answering a text/event-stream body built from the request id.
 * @param frames - per-frame builders receiving the echoed request id
 * @returns a fetch stub returning the SSE-framed response
 */
function sseRpc(frames: Array<(_id: number) => string>) {
  return vi.fn(async (_url: string, init?: RequestInit) => {
    const request = JSON.parse(String(init?.body)) as { id: number };
    const body = frames.map((frame) => `event: message\ndata: ${frame(request.id)}\n\n`).join('');
    return new Response(`: keep-alive\n\n${body}`, {
      status: 200,
      headers: { 'Content-Type': 'text/event-stream' },
    });
  });
}

describe('McpClient', () => {
  beforeEach(() => {
    getState.mockResolvedValue(oauthState);
    vi.unstubAllGlobals();
  });

  it('sends a JSON-RPC tools/list with the Bearer ticket to /mcp', async () => {
    const fetchMock = echoJsonRpc((payload) => {
      payload.result = { tools: [{ name: 'read_device' }] };
    });
    vi.stubGlobal('fetch', fetchMock);

    const client = new McpClient();
    const result = await client.listTools();

    expect(result.tools?.[0].name).toBe('read_device');
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://gw.example.com/mcp');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer rs256.jwt.value');
    const body = JSON.parse(init.body);
    expect(body.jsonrpc).toBe('2.0');
    expect(body.method).toBe('tools/list');
  });

  it('rejects classic login tickets with an auth error before calling /mcp', async () => {
    getState.mockResolvedValue(loginState);
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    const client = new McpClient();
    await expect(client.callTool('x', {})).rejects.toBeInstanceOf(AuthError);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('surfaces JSON-RPC error objects as failures', async () => {
    vi.stubGlobal(
      'fetch',
      echoJsonRpc((payload) => {
        payload.error = { code: -32000, message: 'denied' };
      }),
    );

    const client = new McpClient();
    await expect(client.listTools()).rejects.toThrow(/MCP error -32000: denied/);
  });

  it('maps a 401 challenge to a clean auth error', async () => {
    getState.mockResolvedValue(oauthState);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('unauthorized', { status: 401 })));

    const client = new McpClient();
    await expect(client.listTools()).rejects.toBeInstanceOf(AuthError);
  });

  it('passes tool name and arguments through on tools/call', async () => {
    const fetchMock = echoJsonRpc((payload) => {
      payload.result = { content: [{ text: 'ok' }] };
    });
    vi.stubGlobal('fetch', fetchMock);

    const client = new McpClient();
    const out = await client.callTool('read_device', { deviceId: 1 });
    expect(out).toEqual({ content: [{ text: 'ok' }] });
    const body = JSON.parse(fetchMock.mock.calls[0][1].body);
    expect(body.method).toBe('tools/call');
    expect(body.params).toEqual({ name: 'read_device', arguments: { deviceId: 1 } });
  });
});

describe('McpClient SSE and id handling (F022/F040)', () => {
  beforeEach(() => {
    getState.mockResolvedValue(oauthState);
    vi.unstubAllGlobals();
  });

  it('decodes SSE-framed JSON-RPC responses (F022)', async () => {
    vi.stubGlobal(
      'fetch',
      sseRpc([(id) => JSON.stringify({ jsonrpc: '2.0', id, result: { tools: [] } })]),
    );

    const result = await new McpClient().listTools();

    expect(result).toEqual({ tools: [] });
  });

  it('selects the SSE frame matching the request id from a multi-frame stream', async () => {
    vi.stubGlobal(
      'fetch',
      sseRpc([
        () => JSON.stringify({ jsonrpc: '2.0', id: 424242, result: { tools: [{ name: 'other' }] } }),
        (id) => JSON.stringify({ jsonrpc: '2.0', id, result: { tools: [{ name: 'mine' }] } }),
      ]),
    );

    const result = await new McpClient().listTools();

    expect(result.tools?.map((tool) => tool.name)).toEqual(['mine']);
  });

  it('fails loudly when an SSE stream carries no decodable JSON-RPC frame', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(': keep-alive\n\nevent: message\ndata: not-json\n\n', {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        }),
      ),
    );

    await expect(new McpClient().listTools()).rejects.toThrow(/no JSON-RPC response frames/u);
  });

  it('rejects unsupported response content types with an explicit diagnostic', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('col1,col2', { status: 200, headers: { 'Content-Type': 'text/csv' } }),
      ),
    );

    // Typed ApiError with a dedicated machine code — the generic
    // api/INTERNAL classification hid the failure mode (error-kind precision).
    const error = await new McpClient().listTools().catch((err: unknown) => err);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).message).toMatch(
      /Unsupported \/mcp response content-type: text\/csv/u,
    );
    expect((error as ApiError).code).toBe('MCP_UNSUPPORTED_CONTENT_TYPE');
    expect((error as ApiError).kind).toBe('api');
  });

  it('rejects a JSON response whose id does not match the request', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ jsonrpc: '2.0', id: 424242, result: {} }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    );

    await expect(new McpClient().listTools()).rejects.toThrow(/MCP response id mismatch/u);
  });

  it('assigns distinct ids even within the same frozen millisecond (F040)', async () => {
    vi.useFakeTimers();
    try {
      const ids: number[] = [];
      vi.stubGlobal(
        'fetch',
        echoJsonRpc((payload) => {
          ids.push(payload.id as number);
          payload.result = { tools: [] };
        }),
      );
      const client = new McpClient();
      await client.listTools();
      await client.callTool('read_device', {});

      expect(ids[0]).not.toBe(ids[1]);
      expect(typeof ids[0]).toBe('number');
    } finally {
      vi.useRealTimers();
    }
  });
});

describe('McpClient envelope validation (valid-JSON non-RPC 2xx bodies)', () => {
  beforeEach(() => {
    getState.mockResolvedValue(oauthState);
    vi.unstubAllGlobals();
  });

  /**
   * Fetch mock answering 200 application/json with a fixed raw body.
   * @param body - raw response body text
   * @returns a fetch stub returning that body
   */
  function rawJsonBody(body: string) {
    return vi.fn().mockResolvedValue(
      new Response(body, { status: 200, headers: { 'Content-Type': 'application/json' } }),
    );
  }

  it.each([
    ['a health-ping object', '{"health":"ok"}'],
    ['a bare JSON string', '"ok"'],
    ['a JSON array', '[1,2]'],
    ['a JSON null', 'null'],
  ])('rejects %s 2xx body with typed MCP_BAD_BODY, never a raw TypeError', async (_label, body) => {
    vi.stubGlobal('fetch', rawJsonBody(body));

    const error = await new McpClient().listTools().catch((err: unknown) => err);

    // Before the envelope guard these bodies dereferenced `payload.error` or
    // surfaced in commands/tool.ts as `result.tools` on undefined/null.
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).code).toBe('MCP_BAD_BODY');
    expect((error as ApiError).kind).toBe('api');
    expect((error as ApiError).message).toMatch(/is not a JSON-RPC envelope/u);
    expect((error as ApiError).message).toContain(body);
  });

  it('rejects a single-frame SSE stream whose frame is not an RPC envelope', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response('event: message\ndata: {"health":"ok"}\n\n', {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        }),
      ),
    );

    const error = await new McpClient().listTools().catch((err: unknown) => err);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).code).toBe('MCP_BAD_BODY');
  });

  it('skips non-object SSE frames instead of treating them as responses', async () => {
    vi.stubGlobal(
      'fetch',
      sseRpc([
        () => '"keepalive"',
        (id) => JSON.stringify({ jsonrpc: '2.0', id, result: { tools: [{ name: 'real' }] } }),
      ]),
    );

    const result = await new McpClient().listTools();

    expect(result.tools?.map((tool) => tool.name)).toEqual(['real']);
  });

  it('a well-formed envelope with a null result resolves instead of crashing', async () => {
    vi.stubGlobal(
      'fetch',
      echoJsonRpc((payload) => {
        payload.result = null;
      }),
    );

    // `tools list` renders this as an empty table via its Array.isArray
    // guard; the contract here is that decoding never throws on it.
    await expect(new McpClient().listTools()).resolves.toBeNull();
  });
});
