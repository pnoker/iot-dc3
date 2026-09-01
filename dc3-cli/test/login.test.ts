import { describe, it, expect } from 'vitest';

import { parseTokenResource } from '../src/core/client.js';
import { decodeJwt } from '../src/utils/jwt.js';

describe('direct auth resources', () => {
  it('accepts a JWT resource', () => {
    const token = parseTokenResource('eyJhbGciOi.eyJleHAiOn0.sig');
    expect(token).toBe('eyJhbGciOi.eyJleHAiOn0.sig');
  });

  it('keeps the resource parseable for expiry bookkeeping', () => {
    const token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.AAA';
    expect(() => decodeJwt(token)).not.toThrow();
  });

  it('rejects acknowledgement envelopes and cookie-only responses', () => {
    expect(() => parseTokenResource('ok')).toThrow(/invalid resource/);
    expect(() => parseTokenResource({ok: true, data: 'token'})).toThrow(/invalid resource/);
  });
});
