import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import type { Mock } from 'vitest';

/*
 * Guard tests for the client-side fixes assigned to fix:http-client:
 * F003 (postForm seam), F016 (identity cross-check), F024/F025 (logout
 * ordering + renewal discard), F030/F042 (error surface + body decoding),
 * F037 (renewal identity), F055 (cleartext transport warning).
 *
 * Shared mock fns are hoisted so they survive vi.resetModules (used to get a
 * fresh process-level warning flag for F055).
 */
const mocks = vi.hoisted(() => ({
  getActiveProfile: vi.fn(async () => ({
    gateway: 'http://localhost:9400/',
    tenant: 'tenantB',
    username: 'bob',
  })),
  getActiveProfileName: vi.fn(async () => 'default'),
  getSettings: vi.fn(async () => ({ renewal_threshold_hours: 12 })),
  getState: vi.fn(),
  needsRenewal: vi.fn(async () => false),
  buildHeaders: vi.fn(
    (state: { tenant: string; username: string; salt: string; token: string }) => ({
      'Content-Type': 'application/json',
      'X-Auth-Tenant': state.tenant,
      'X-Auth-Login': state.username,
      'X-Auth-Token': JSON.stringify({ salt: state.salt, token: state.token }),
    }),
  ),
  saveState: vi.fn(async () => undefined),
  saveStateIfEpochUnchanged: vi.fn(async () => true),
  getEpoch: vi.fn(async () => 0),
  credentialIdentifier: vi.fn(
    (state: { username: string; tenant: string }) => `${state.username}@${state.tenant}`,
  ),
  clearState: vi.fn(async () => undefined),
  resolvePassword: vi.fn(async () => null),
  deletePasswordFromStore: vi.fn(async () => undefined),
}));

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: mocks.getActiveProfile,
    getActiveProfileName: mocks.getActiveProfileName,
    getSettings: mocks.getSettings,
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: mocks.getState,
    needsRenewal: mocks.needsRenewal,
    buildHeaders: mocks.buildHeaders,
    saveState: mocks.saveState,
    saveStateIfEpochUnchanged: mocks.saveStateIfEpochUnchanged,
    getEpoch: mocks.getEpoch,
    clearState: mocks.clearState,
  },
  credentialIdentifier: mocks.credentialIdentifier,
}));

vi.mock('../src/core/credential-store.js', () => ({
  resolvePassword: mocks.resolvePassword,
  deletePasswordFromStore: mocks.deletePasswordFromStore,
}));

import { Dc3Client, ApiError, AuthError, NetworkError } from '../src/core/client.js';

const loginState = {
  token: 'old.jwt.value',
  salt: 'old-salt',
  tenant: 'tenantA',
  username: 'admin',
};

function fakeJwt(payload: Record<string, unknown>): string {
  const encode = (value: Record<string, unknown>) =>
    Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.sig`;
}

type FetchCall = { url: string; init: RequestInit };
let fetchCalls: FetchCall[];
let stderrOutput: string;

beforeEach(() => {
  vi.clearAllMocks();
  fetchCalls = [];
  stderrOutput = '';
  mocks.getActiveProfile.mockResolvedValue({
    gateway: 'http://localhost:9400/',
    tenant: 'tenantB',
    username: 'bob',
  });
  mocks.getState.mockResolvedValue(null);
  mocks.needsRenewal.mockResolvedValue(false);
  mocks.resolvePassword.mockResolvedValue(null);
  vi.spyOn(process.stderr, 'write').mockImplementation((chunk) => {
    stderrOutput += String(chunk);
    return true;
  });
});

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

function stubFetch(responder: (call: FetchCall) => Response | Promise<Response>): void {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init?: RequestInit) => {
      const call = { url: String(url), init: init ?? {} };
      fetchCalls.push(call);
      return responder(call);
    }),
  );
}

describe('response body decoding (F042)', () => {
  it('an empty 2xx body resolves to undefined, like 204', async () => {
    stubFetch(() => new Response('', { status: 200 }));
    await expect(new Dc3Client().get('/api/v3/manager/device/list')).resolves.toBeUndefined();
  });

  it('a 204 response resolves to undefined', async () => {
    stubFetch(() => new Response(null, { status: 204 }));
    await expect(new Dc3Client().del('/api/v3/manager/device/delete?id=1&version=2')).resolves.toBeUndefined();
  });

  it('a non-JSON 2xx body throws ApiError naming status, content-type, and a snippet', async () => {
    stubFetch(
      () => new Response('plain ok', { status: 200, headers: { 'Content-Type': 'text/plain' } }),
    );
    const error = await new Dc3Client()
      .get('/api/v3/manager/device/list')
      .catch((err: unknown) => err);
    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError & { statusCode?: number };
    expect(apiError.message).toMatch(/non-JSON 200/);
    expect(apiError.message).toMatch(/text\/plain/);
    expect(apiError.message).toMatch(/plain ok/);
    expect(apiError.statusCode).toBe(200);
  });

  it('an empty 500 body never produces a dangling-colon detail', async () => {
    stubFetch(() => new Response('', { status: 500 }));
    const error = await new Dc3Client()
      .get('/api/v3/manager/device/list')
      .catch((err: unknown) => err);
    expect(error).toBeInstanceOf(ApiError);
    const message = (error as ApiError).message;
    expect(message).toBe('Server error (500): (empty body)');
    expect(message).not.toMatch(/: $/u);
  });

  it('a problem+json 404 keeps its detail and attaches the decoded problem', async () => {
    stubFetch(
      () =>
        new Response(
          JSON.stringify({ title: 'Not Found', detail: 'device 42 does not exist' }),
          { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
        ),
    );
    const error = await new Dc3Client()
      .get('/api/v3/manager/device/get_by_id?id=42')
      .catch((err: unknown) => err);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).message).toBe('Not found (404): device 42 does not exist');
    const problem = (error as ApiError & { problem?: unknown }).problem as Record<string, unknown>;
    expect(problem.detail).toBe('device 42 does not exist');
  });

  it('a non-JSON 401 body still maps to AuthError with the raw text', async () => {
    stubFetch(() => new Response('session expired', { status: 401, headers: { 'Content-Type': 'text/plain' } }));
    const error = await new Dc3Client()
      .get('/api/v3/manager/device/list')
      .catch((err: unknown) => err);
    expect(error).toBeInstanceOf(AuthError);
    expect((error as AuthError).message).toBe('Authentication failed (401): session expired');
  });
});

describe('postForm multipart seam (F003)', () => {
  it('forwards extra headers and lets fetch own the multipart Content-Type', async () => {
    stubFetch(() => new Response(JSON.stringify({ ok: true }), { status: 200 }));
    const form = new FormData();
    form.append('file', new Blob([new Uint8Array([1, 2, 3])]), 'a.bin');
    await new Dc3Client().postForm('/api/v3/agentic/attachment/upload', form, {
      'X-Filename': 'a.bin',
    });

    expect(fetchCalls).toHaveLength(1);
    const headers = fetchCalls[0].init.headers as Record<string, string>;
    expect(headers['X-Filename']).toBe('a.bin');
    // The caller must never pre-set Content-Type on a FormData body: fetch
    // generates the multipart boundary.
    expect(headers['Content-Type']).toBeUndefined();
    expect(fetchCalls[0].init.body).toBeInstanceOf(FormData);
  });

  it('a JSON post still carries application/json', async () => {
    stubFetch(() => new Response(JSON.stringify({ ok: true }), { status: 200 }));
    await new Dc3Client().post('/api/v3/manager/device/add', { deviceName: 'x' });
    const headers = fetchCalls[0].init.headers as Record<string, string>;
    expect(headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({ deviceName: 'x' });
  });
});

describe('token-state identity cross-check (F016/F037)', () => {
  it('warns once when the stored session identity differs from the profile and sends the state identity', async () => {
    mocks.getState.mockResolvedValue({ ...loginState });
    stubFetch(() => new Response(JSON.stringify([]), { status: 200 }));
    const client = new Dc3Client();
    await client.get('/api/v3/manager/device/list');
    await client.get('/api/v3/manager/device/list');

    expect(stderrOutput).toMatch(/admin@tenantA/);
    expect(stderrOutput).toMatch(/bob@tenantB/);
    expect(stderrOutput.match(/Warning: profile "default" is configured/g)).toHaveLength(1);
    // Requests keep using the token-state identity, never the drifted profile.
    const headers = fetchCalls[0].init.headers as Record<string, string>;
    expect(headers['X-Auth-Tenant']).toBe('tenantA');
    expect(headers['X-Auth-Login']).toBe('admin');
  });

  it('no warning when the identities match', async () => {
    mocks.getActiveProfile.mockResolvedValue({
      gateway: 'http://localhost:9400/',
      tenant: 'tenantA',
      username: 'admin',
    });
    mocks.getState.mockResolvedValue({ ...loginState });
    stubFetch(() => new Response(JSON.stringify([]), { status: 200 }));
    await new Dc3Client().get('/api/v3/manager/device/list');
    expect(stderrOutput).toBe('');
  });

  it('renewal keys the stored password by the token-state identity, not the profile', async () => {
    mocks.getState.mockResolvedValue({ ...loginState });
    mocks.resolvePassword.mockResolvedValue('dc3dc3dc3');
    stubFetch((call) => {
      if (call.url.endsWith('/token/salt')) return new Response('salt-abc', { status: 200 });
      return new Response(fakeJwt({ sub: '1', iat: 100, exp: 2_000_000_000 }), { status: 200 });
    });
    const renewed = await new Dc3Client().renewToken('default', 'tenantB', 'bob');

    expect(renewed).toBe(true);
    expect(mocks.resolvePassword).toHaveBeenCalledWith('admin@tenantA');
    const saltBody = JSON.parse(String(fetchCalls[0].init.body)) as Record<string, unknown>;
    expect(saltBody).toMatchObject({ name: 'admin', tenant: 'tenantA' });
    expect(mocks.saveStateIfEpochUnchanged).toHaveBeenCalledWith(
      expect.objectContaining({ tenant: 'tenantA', username: 'admin' }),
      'default',
      0,
    );
  });

  it('renewal falls back to the profile identity when no state exists', async () => {
    mocks.getState.mockResolvedValue(null);
    mocks.resolvePassword.mockResolvedValue('dc3dc3dc3');
    stubFetch((call) => {
      if (call.url.endsWith('/token/salt')) return new Response('salt-abc', { status: 200 });
      return new Response(fakeJwt({ sub: '1', iat: 100, exp: 2_000_000_000 }), { status: 200 });
    });
    const renewed = await new Dc3Client().renewToken('default', 'tenantB', 'bob');

    expect(renewed).toBe(true);
    expect(mocks.resolvePassword).toHaveBeenCalledWith('bob@tenantB');
  });
});

describe('logout ordering and guaranteed local cleanup (F024/F025)', () => {
  it('clears local state before the remote cancel, then deletes the password', async () => {
    const order: string[] = [];
    mocks.getState.mockResolvedValue({ ...loginState });
    mocks.clearState.mockImplementation(async () => {
      order.push('clearState');
    });
    mocks.deletePasswordFromStore.mockImplementation(async (identifier: string) => {
      order.push(`deletePassword:${identifier}`);
    });
    stubFetch((call) => {
      if (call.url.endsWith('/token/cancel')) {
        order.push('cancel');
        return new Response(null, { status: 204 });
      }
      return new Response(null, { status: 204 });
    });

    await new Dc3Client().logout('default');

    expect(order).toEqual(['clearState', 'cancel', 'deletePassword:admin@tenantA']);
  });

  it('keeps local cleanup and warns about the surviving gateway token when the cancel fails on transport', async () => {
    mocks.getState.mockResolvedValue({ ...loginState });
    stubFetch(() => {
      throw new TypeError('fetch failed');
    });

    const error = await new Dc3Client().logout('default').catch((err: unknown) => err);

    expect(error).toBeInstanceOf(NetworkError);
    expect(mocks.clearState).toHaveBeenCalledWith('default');
    expect(mocks.deletePasswordFromStore).toHaveBeenCalledWith('admin@tenantA');
    expect(stderrOutput).toMatch(/stays valid until it expires/u);
    expect(stderrOutput).toMatch(/admin@tenantA/u);
  });

  it('clears local state even when the gateway answers the cancel with 401 (expired token)', async () => {
    mocks.getState.mockResolvedValue({ ...loginState });
    stubFetch((call) => {
      if (call.url.endsWith('/token/cancel')) {
        return new Response(JSON.stringify({ detail: 'token expired' }), { status: 401 });
      }
      return new Response(null, { status: 204 });
    });

    const error = await new Dc3Client().logout('default').catch((err: unknown) => err);

    expect(error).toBeInstanceOf(AuthError);
    expect(mocks.clearState).toHaveBeenCalledWith('default');
    expect(mocks.deletePasswordFromStore).toHaveBeenCalledWith('admin@tenantA');
    expect(stderrOutput).toMatch(/could not be revoked remotely/u);
  });

  it('does not touch the network when there is no stored session', async () => {
    mocks.getState.mockResolvedValue(null);
    stubFetch(() => new Response(null, { status: 204 }));

    await new Dc3Client().logout('default');

    expect(fetchCalls).toHaveLength(0);
    expect(mocks.clearState).not.toHaveBeenCalled();
  });

  it('a renewal racing a logout discards the fresh token and cancels it server-side', async () => {
    const newToken = fakeJwt({ sub: '1', iat: 100, exp: 2_000_000_000 });
    mocks.getState.mockResolvedValue({ ...loginState });
    mocks.resolvePassword.mockResolvedValue('dc3dc3dc3');
    // The epoch moved between capture and save: a logout landed mid-renewal.
    mocks.saveStateIfEpochUnchanged.mockResolvedValueOnce(false);
    stubFetch((call) => {
      if (call.url.endsWith('/token/salt')) return new Response('salt-abc', { status: 200 });
      if (call.url.endsWith('/token/cancel')) return new Response(null, { status: 204 });
      return new Response(newToken, { status: 200 });
    });

    const renewed = await new Dc3Client().renewToken('default', 'tenantA', 'admin');

    expect(renewed).toBe(false);
    expect(mocks.saveState).not.toHaveBeenCalled();
    const cancel = fetchCalls.find((call) => call.url.endsWith('/token/cancel'));
    expect(cancel).toBeDefined();
    const headers = cancel?.init.headers as Record<string, string>;
    expect(JSON.parse(headers['X-Auth-Token'])).toMatchObject({ token: newToken });
  });
});

describe('cleartext transport warning (F055)', () => {
  beforeEach(() => {
    // The warned-flag is process-level; reset modules for a fresh one per test.
    vi.resetModules();
  });

  async function freshLogin(gateway: string): Promise<void> {
    const { configManager } = (await import('../src/core/config-manager.js')) as {
      configManager: { getActiveProfile: Mock };
    };
    configManager.getActiveProfile.mockResolvedValue({ gateway, tenant: 't', username: 'u' });
    const { Dc3Client: FreshClient } = await import('../src/core/client.js');
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string) => {
        if (String(url).endsWith('/token/salt')) return new Response('salt-abc', { status: 200 });
        return new Response(fakeJwt({ sub: '1', iat: 100, exp: 2_000_000_000 }), { status: 200 });
      }),
    );
    await new FreshClient().login('t', 'u', 'pw', 'default');
  }

  it('warns exactly once when credentials travel to a non-loopback http gateway', async () => {
    await freshLogin('http://10.1.2.3:8000/');
    await freshLogin('http://10.1.2.3:8000/');
    expect(stderrOutput.match(/cleartext/g)).toHaveLength(1);
    expect(stderrOutput).toMatch(/http:\/\/10\.1\.2\.3:8000/u);
  });

  it('stays silent for a loopback http gateway', async () => {
    await freshLogin('http://localhost:9400/');
    expect(stderrOutput).toBe('');
  });

  it('stays silent for an https gateway', async () => {
    await freshLogin('https://gw.example.com/');
    expect(stderrOutput).toBe('');
  });
});
