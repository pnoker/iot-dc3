import { describe, it, expect, vi, beforeEach } from 'vitest';

const { getState } = vi.hoisted(() => ({ getState: vi.fn() }));

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({ gateway: 'http://gw.example.com/' })),
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

import { AuthError } from '../src/core/client.js';
import { McpClient } from '../src/core/mcp.js';

const oauthState = { token: 'rs256.jwt.value', authType: 'oauth' };
const loginState = { token: 'hs.jwt.value', authType: 'login' };

describe('McpClient', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('sends a JSON-RPC tools/list with the Bearer ticket to /mcp', async () => {
    getState.mockResolvedValue(oauthState);
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ jsonrpc: '2.0', id: 1, result: { tools: [{ name: 'read_device' }] } }),
      { status: 200 },
    ));
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
    getState.mockResolvedValue(oauthState);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ jsonrpc: '2.0', id: 1, error: { code: -32000, message: 'denied' } }),
      { status: 200 },
    )));

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
    getState.mockResolvedValue(oauthState);
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ jsonrpc: '2.0', id: 7, result: { content: [{ text: 'ok' }] } }),
      { status: 200 },
    ));
    vi.stubGlobal('fetch', fetchMock);

    const client = new McpClient();
    const out = await client.callTool('read_device', { deviceId: 1 });
    expect(out).toEqual({ content: [{ text: 'ok' }] });
    const body = JSON.parse(fetchMock.mock.calls[0][1].body);
    expect(body.method).toBe('tools/call');
    expect(body.params).toEqual({ name: 'read_device', arguments: { deviceId: 1 } });
  });
});
