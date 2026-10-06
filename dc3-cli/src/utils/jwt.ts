/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import { ValidationError } from '../core/errors.js';

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

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Decode a JWT payload without verifying the signature. The payload must carry
 * NUMERIC `exp` and `iat`: a well-formed 3-part token without them used to
 * pass through the cast and mint a NaN/never-expiring session, so an
 * unusable payload fails loudly at the decode seam instead.
 * @param token - bearer token sent with the request
 * @returns the decoded payload
 */
export function decodeJwt(token: string): JwtPayload {
  const parts = token.split('.');
  if (parts.length !== 3) {
    throw new ValidationError('Invalid JWT format: expected 3 parts separated by "."');
  }
  const payload: unknown = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));
  if (!isRecord(payload) || typeof payload.exp !== 'number' || typeof payload.iat !== 'number') {
    throw new ValidationError('Invalid JWT payload: exp and iat must be numeric epoch seconds');
  }
  // exp/iat are proven numeric above; iss/sub stay optional in practice, so
  // the remaining widening goes through unknown on purpose.
  return payload as unknown as JwtPayload;
}

/**
 * Check if a JWT token is expired or will expire within the given threshold
 * seconds. A token whose payload cannot be decoded (or lacks numeric exp/iat)
 * counts as EXPIRED: renewal/re-login must trigger, never an eternal session.
 * @param token - bearer token sent with the request
 * @param thresholdSec - renewal window in seconds
 * @returns whether the condition holds
 */
export function isTokenExpired(token: string, thresholdSec = 0): boolean {
  try {
    const { exp } = decodeJwt(token);
    return exp * 1000 < Date.now() + thresholdSec * 1000;
  } catch {
    return true;
  }
}

/**
 * Get remaining seconds until token expiry.
 * Returns negative if already expired, and -Infinity for a token that cannot
 * be decoded (treated as maximally expired, mirroring {@link isTokenExpired}).
 * @param token - bearer token sent with the request
 * @returns remaining seconds until expiry
 */
export function tokenTtl(token: string): number {
  try {
    const { exp } = decodeJwt(token);
    return exp - Math.floor(Date.now() / 1000);
  } catch {
    return Number.NEGATIVE_INFINITY;
  }
}
