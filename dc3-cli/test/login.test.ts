import { describe, it, expect } from 'vitest';

import { extractTicket } from '../src/core/client.js';
import { decodeJwt } from '../src/utils/jwt.js';

function responseWithCookie(cookieValue: string): Response {
  return new Response(JSON.stringify({ ok: true, data: 'ok' }), {
    headers: { 'set-cookie': `dc3-token=${cookieValue}; Path=/; HttpOnly` },
  });
}

describe('extractTicket', () => {
  it('prefers a JWT-shaped body value (June-era contract)', () => {
    const res = new Response('{}');
    expect(extractTicket(res, 'eyJhbGciOi.eyJleHAiOn0.sig')).toBe(
      'eyJhbGciOi.eyJleHAiOn0.sig',
    );
  });

  it('falls back to the dc3-token cookie (current contract)', () => {
    const res = responseWithCookie('eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.AAA');
    const token = extractTicket(res, 'ok');
    expect(token).toBe('eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.AAA');
    // and the ticket must be parseable for iat/exp bookkeeping
    expect(() => decodeJwt(token)).not.toThrow();
  });

  it('throws when neither the body nor the cookie carries a ticket', () => {
    const res = new Response('{"ok":true,"data":"ok"}');
    expect(() => extractTicket(res, 'ok')).toThrow(/no dc3-token/);
  });

  it('does not mistake acknowledgement text for a ticket', () => {
    const res = new Response('{}');
    expect(() => extractTicket(res, 'ok')).toThrow(/no dc3-token/);
  });
});
