import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

/*
 * Squad CMDS guard file for client.ts error-surface gaps:
 * - G16: the logout remote-cancel warning joins the reason with an em dash,
 *   never a bare space.
 * - G23: response-shape failures on the login/renewal endpoints classify as
 *   ApiError code API_BAD_BODY (kind api), not the generic INTERNAL fallback
 *   of a plain Error.
 * - G24: requestForBytes shares request()'s error mapping (401/403 →
 *   AuthError exit 3) and hands back the raw response body.
 *
 * (test/client.test.ts belongs to Squad AUTHCORE; new assertions live here.)
 */

const mocks = vi.hoisted(() => ({
  getActiveProfile: vi.fn(),
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

import { Dc3Client, ApiError, AuthError, parseTokenResource, parseAuthField } from '../src/core/client.js';
import { classifyError } from '../src/core/errors.js';

const loginState = {
  token: 'old.jwt.value',
  salt: 'old-salt',
  tenant: 'tenantA',
  username: 'admin',
};

const PAYLOAD = Buffer.from([0x50, 0x4b, 0x03, 0x04]);

type FetchCall = { url: string; init: RequestInit };
let fetchCalls: FetchCall[];
let stderrOutput: string;

function stubFetch(responder: () => Response | Promise<Response>): void {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init?: RequestInit) => {
      fetchCalls.push({ url: String(url), init: init ?? {} });
      return responder();
    }),
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  fetchCalls = [];
  stderrOutput = '';
  mocks.getActiveProfile.mockResolvedValue({
    gateway: 'http://gw.test/',
    tenant: 'tenantA',
    username: 'admin',
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

describe('logout remote-cancel warning join (G16)', () => {
  it('separates the reason with an em dash, never a bare space', async () => {
    mocks.getState.mockResolvedValue({ ...loginState });
    stubFetch(() => new Response(JSON.stringify({ detail: 'token expired' }), { status: 401 }));

    const error = await new Dc3Client().logout('default').catch((err: unknown) => err);

    expect(error).toBeInstanceOf(AuthError);
    expect(stderrOutput).toContain(
      'could not be revoked remotely — Authentication failed (401): token expired.',
    );
    // Negative guard: the space-joined form read as one garbled sentence.
    expect(stderrOutput).not.toMatch(/remotely Authentication/u);
    expect(stderrOutput).not.toMatch(/remotely\s+[A-Z]/u);
  });
});

describe('auth endpoint response-shape failures classify as API_BAD_BODY (G23)', () => {
  it('parseAuthField on a non-JSON salt body throws ApiError with the dedicated code', () => {
    let thrown: unknown = null;
    try {
      parseAuthField('   ', 'salt', 'Salt');
    } catch (err) {
      thrown = err;
    }
    expect(thrown).toBeInstanceOf(ApiError);
    expect(thrown).toMatchObject({ code: 'API_BAD_BODY', kind: 'api', exitCode: 1 });
    expect((thrown as Error).message).toBe('Salt endpoint returned an invalid resource');
    expect(classifyError(thrown as Error)).toMatchObject({ kind: 'api', code: 'API_BAD_BODY' });
  });

  it('parseTokenResource on a non-JWT body throws ApiError with the dedicated code', () => {
    let thrown: unknown = null;
    try {
      parseTokenResource('<html>login page</html>');
    } catch (err) {
      thrown = err;
    }
    expect(thrown).toBeInstanceOf(ApiError);
    expect(thrown).toMatchObject({ code: 'API_BAD_BODY', kind: 'api', exitCode: 1 });
    expect((thrown as Error).message).toBe('Token endpoint returned an invalid resource');
  });

  it('login surfaces the salt shape failure as kind api / API_BAD_BODY (not INTERNAL)', async () => {
    stubFetch(() => new Response('', { status: 200 }));

    const error = await new Dc3Client()
      .login('tenantA', 'admin', 'pw', 'default')
      .catch((err: unknown) => err);

    expect(error).toBeInstanceOf(ApiError);
    expect(classifyError(error as Error)).toMatchObject({ kind: 'api', code: 'API_BAD_BODY' });
  });

  it('login surfaces the token shape failure as kind api / API_BAD_BODY (not INTERNAL)', async () => {
    stubFetch(() => new Response('not-a-jwt', { status: 200 }));

    const error = await new Dc3Client()
      .login('tenantA', 'admin', 'pw', 'default')
      .catch((err: unknown) => err);

    expect(error).toBeInstanceOf(ApiError);
    expect(classifyError(error as Error)).toMatchObject({ kind: 'api', code: 'API_BAD_BODY' });
  });
});

describe('loginOAuth typed 2xx-body failure', () => {
  it('a 200 HTML body from the token endpoint throws AuthError naming the Content-Type, never a raw SyntaxError', async () => {
    stubFetch(
      () =>
        new Response('<html><body>gateway login page</body></html>', {
          status: 200,
          headers: { 'Content-Type': 'text/html' },
        }),
    );

    const error = await new Dc3Client()
      .loginOAuth('client-1', 'secret-1', undefined, 'default')
      .catch((err: unknown) => err);

    // Auth plane: typed AuthError (exit 3) — the previous raw res.json() let a
    // non-JSON 2xx escape as SyntaxError classified kind api / code INTERNAL.
    expect(error).toBeInstanceOf(AuthError);
    expect(error).toMatchObject({ kind: 'auth', exitCode: 3, code: 'AUTH' });
    expect((error as Error).message).toContain('non-JSON 2xx response');
    expect((error as Error).message).toContain('text/html');
    expect(classifyError(error as Error)).toMatchObject({ kind: 'auth', code: 'AUTH' });
    // Nothing was persisted: the malformed response ends the login, not a
    // half-minted session.
    expect(mocks.saveState).not.toHaveBeenCalled();
  });
});

describe('requestForBytes binary seam (G24)', () => {
  it('returns the raw 2xx response with the body unconsumed and the auth headers attached', async () => {
    mocks.getState.mockResolvedValue({ ...loginState });
    stubFetch(() => new Response(PAYLOAD, { status: 200 }));

    const res = await new Dc3Client().requestForBytes(
      'POST',
      '/api/v3/manager/device/export/import_template',
      { driverId: '10', profileId: '100' },
    );

    expect(res.status).toBe(200);
    expect(Buffer.from(await res.arrayBuffer()).equals(PAYLOAD)).toBe(true);
    const headers = fetchCalls[0].init.headers as Record<string, string>;
    expect(headers['X-Auth-Login']).toBe('admin');
    expect(JSON.parse(String(fetchCalls[0].init.body))).toEqual({
      driverId: '10',
      profileId: '100',
    });
  });

  it('a 401 maps to AuthError exit 3 — never an API_401 business failure', async () => {
    stubFetch(() => new Response(JSON.stringify({ detail: 'token expired' }), { status: 401 }));

    const error = await new Dc3Client()
      .requestForBytes('POST', '/api/v3/manager/device/export/import_template', {})
      .catch((err: unknown) => err);

    expect(error).toBeInstanceOf(AuthError);
    expect(error).toMatchObject({ kind: 'auth', exitCode: 3, code: 'AUTH' });
    expect(classifyError(error as Error).kind).not.toBe('api');
  });

  it('a 403 maps to AuthError exit 3 through the shared buildError path', async () => {
    stubFetch(() => new Response(JSON.stringify({ detail: 'forbidden' }), { status: 403 }));

    const error = await new Dc3Client()
      .requestForBytes('POST', '/api/v3/manager/device/export/import_template', {})
      .catch((err: unknown) => err);

    expect(error).toBeInstanceOf(AuthError);
    expect(error).toMatchObject({ kind: 'auth', exitCode: 3 });
  });
});
