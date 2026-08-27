import { describe, it, expect } from 'vitest';
import { md5, sha256 } from '../src/utils/crypto';
import { decodeJwt, isTokenExpired, tokenTtl } from '../src/utils/jwt';

describe('crypto utils', () => {
  it('md5 should produce correct hash', () => {
    const result = md5('hello');
    expect(result).toBe('5d41402abc4b2a76b9719d911017c592');
    expect(result).toHaveLength(32);
  });

  it('sha256 should produce correct hash', () => {
    const result = sha256('hello');
    expect(result).toBe('2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824');
    expect(result).toHaveLength(64);
  });

  it('md5 of empty string', () => {
    const result = md5('');
    expect(result).toBe('d41d8cd98f00b204e9800998ecf8427e');
  });
});

describe('jwt utils', () => {
  // A valid-looking JWT with known payload
  const futureJwt = [
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9', // header
    Buffer.from(JSON.stringify({
      iss: 'test:123',
      sub: 'test:user',
      iat: Math.floor(Date.now() / 1000) - 3600,
      exp: Math.floor(Date.now() / 1000) + 3600 * 12, // 12h from now
    })).toString('base64url'),
    'signature',
  ].join('.');

  const expiredJwt = [
    'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9',
    Buffer.from(JSON.stringify({
      iss: 'test:123',
      sub: 'test:user',
      iat: Math.floor(Date.now() / 1000) - 3600 * 24,
      exp: Math.floor(Date.now() / 1000) - 3600, // 1h ago
    })).toString('base64url'),
    'signature',
  ].join('.');

  it('decodeJwt should parse payload', () => {
    const payload = decodeJwt(futureJwt);
    expect(payload.iss).toBe('test:123');
    expect(payload.sub).toBe('test:user');
    expect(payload.exp).toBeGreaterThan(Date.now() / 1000);
  });

  it('isTokenExpired should return false for future token', () => {
    expect(isTokenExpired(futureJwt)).toBe(false);
  });

  it('isTokenExpired should return true for expired token', () => {
    expect(isTokenExpired(expiredJwt)).toBe(true);
  });

  it('isTokenExpired with threshold', () => {
    // Future token should NOT be expired with a reasonable threshold
    expect(isTokenExpired(futureJwt, 3600)).toBe(false);
    // But should be "expired" if threshold exceeds remaining time
    expect(isTokenExpired(futureJwt, 3600 * 24)).toBe(true);
  });

  it('tokenTtl should return positive for future token', () => {
    const ttl = tokenTtl(futureJwt);
    expect(ttl).toBeGreaterThan(0);
    expect(ttl).toBeLessThan(3600 * 13); // less than 13h
  });

  it('tokenTtl should return negative for expired token', () => {
    const ttl = tokenTtl(expiredJwt);
    expect(ttl).toBeLessThan(0);
  });

  it('decodeJwt should throw for malformed JWT', () => {
    expect(() => decodeJwt('not-a-jwt')).toThrow();
    expect(() => decodeJwt('a.b')).toThrow();
  });
});
