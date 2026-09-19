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

/**
 * Decode a JWT payload without verifying the signature.
 * @param token - bearer token sent with the request
 * @returns the transformed value
 */
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
 * @param token - bearer token sent with the request
 * @param thresholdSec - renewal window in seconds
 * @returns whether the condition holds
 */
export function isTokenExpired(token: string, thresholdSec = 0): boolean {
  const { exp } = decodeJwt(token);
  return exp * 1000 < Date.now() + thresholdSec * 1000;
}

/**
 * Get remaining seconds until token expiry.
 * Returns negative if already expired.
 * @param token - bearer token sent with the request
 * @returns remaining seconds until expiry
 */
export function tokenTtl(token: string): number {
  const { exp } = decodeJwt(token);
  return exp - Math.floor(Date.now() / 1000);
}
