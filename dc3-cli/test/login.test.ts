import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({ gateway: 'http://gw.test/', tenant: 't', username: 'u' })),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    saveState: vi.fn(async () => undefined),
    saveStateIfEpochUnchanged: vi.fn(async () => true),
    getEpoch: vi.fn(async () => 0),
  },
  credentialIdentifier: vi.fn(
    (state: { username: string; tenant: string }) => `${state.username}@${state.tenant}`,
  ),
}));

vi.mock('../src/core/credential-store.js', () => ({
  resolvePassword: vi.fn(async () => 'dc3dc3dc3'),
}));

import { Dc3Client, parseTokenResource, parseAuthField } from '../src/core/client.js';
import { decodeJwt } from '../src/utils/jwt.js';

describe('direct auth resources', () => {
  it('accepts a JWT resource', () => {
    const token = parseTokenResource('eyJhbGciOi.eyJleHAiOn0.sig');
    expect(token).toBe('eyJhbGciOi.eyJleHAiOn0.sig');
  });

  it('rejects payloads without numeric exp/iat — never-expiring guard', () => {
    // Transport shape (3 parts) and expiry semantics are different layers: the
    // resource regex accepts the shape, but a payload without numeric exp/iat
    // must fail expiry bookkeeping loudly instead of minting a NaN session.
    const token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.AAA';
    expect(() => decodeJwt(token)).toThrow(/exp and iat must be numeric/u);
  });

  it('rejects acknowledgement envelopes and cookie-only responses', () => {
    expect(() => parseTokenResource('ok')).toThrow(/invalid resource/);
    expect(() => parseTokenResource({ok: true, data: 'token'})).toThrow(/invalid resource/);
  });

  it('extracts auth fields from JSON objects only — bare text is a contract violation', () => {
    expect(parseAuthField('{"salt":"salt-abc"}', 'salt', 'Salt')).toBe('salt-abc');
    // Bare text/plain scalals are the retired wire shape: they must be rejected.
    expect(() => parseAuthField('salt-abc', 'salt', 'Salt')).toThrow(/invalid resource/);
    expect(() => parseAuthField('{"salt":""}', 'salt', 'Salt')).toThrow(/invalid resource/);
  });
});

function fakeJwt(payload: Record<string, unknown>): string {
  const encode = (value: Record<string, unknown>) =>
    Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.sig`;
}

describe('login wire contract', () => {
  let fetchCalls: Array<{ url: string; body: Record<string, unknown> }>;

  beforeEach(() => {
    fetchCalls = [];
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        const body = init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : {};
        fetchCalls.push({ url: String(url), body });
        if (String(url).endsWith('/token/salt')) {
          // Real contract: auth endpoints answer with JSON object fields.
          return new Response(JSON.stringify({ salt: 'salt-abc' }), { status: 200 });
        }
        return new Response(JSON.stringify({ token: fakeJwt({ sub: '1', iat: 100, exp: 2_000_000_000 }) }), { status: 200 });
      }),
    );
  });

  it('login sends the plaintext password for server-side bcrypt/argon2 verification', async () => {
    const client = new Dc3Client();
    await client.login('default', 'dc3', 'dc3dc3dc3', 'default');

    expect(fetchCalls[0].url).toBe('http://gw.test/api/v3/auth/token/salt');
    expect(fetchCalls[1].url).toBe('http://gw.test/api/v3/auth/token/generate');
    expect(fetchCalls[1].body).toMatchObject({
      name: 'dc3',
      tenant: 'default',
      salt: 'salt-abc',
      password: 'dc3dc3dc3',
    });
  });

  it('renewToken also sends the plaintext password', async () => {
    const client = new Dc3Client();
    const renewed = await client.renewToken('default', 't', 'u');

    expect(renewed).toBe(true);
    expect(fetchCalls[1].url).toBe('http://gw.test/api/v3/auth/token/generate');
    expect(fetchCalls[1].body.password).toBe('dc3dc3dc3');
  });
});
