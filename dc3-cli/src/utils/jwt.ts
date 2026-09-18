/**
 * Lightweight JWT decoder — reads the payload without verifying the signature.
 * Used only to inspect the `exp` field for proactive token renewal.
 * Token signature verification is handled by the DC3 Gateway.
 */
export interface JwtPayload {
  iss: string;
  sub: string;
  iat: number;
  exp: number;
}

export function decodeJwt(token: string): JwtPayload {
  const parts = token.split('.');
  if (parts.length !== 3) {
    throw new Error('Invalid JWT format: expected 3 parts separated by "."');
  }
  const payload = Buffer.from(parts[1], 'base64url').toString('utf8');
  return JSON.parse(payload) as JwtPayload;
}

/**
 * Check if a JWT token is expired or will expire within the given threshold seconds.
 */
export function isTokenExpired(token: string, thresholdSec = 0): boolean {
  const { exp } = decodeJwt(token);
  return exp * 1000 < Date.now() + thresholdSec * 1000;
}

/**
 * Get remaining seconds until token expiry.
 * Returns negative if already expired.
 */
export function tokenTtl(token: string): number {
  const { exp } = decodeJwt(token);
  return exp - Math.floor(Date.now() / 1000);
}
