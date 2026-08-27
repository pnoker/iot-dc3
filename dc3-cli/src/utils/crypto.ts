import { createHash } from 'node:crypto';

/**
 * MD5 hash utility.
 *
 * NOTE: the DC3 backend expects the PLAINTEXT password on the wire (verified by
 * `PasswordUtil.verify` via Argon2id/BCrypt) — it does NOT expect MD5(password).
 * The CLI currently still applies MD5 before submitting, which mismatches the
 * server contract; this is tracked as a CLI bug. Do not treat MD5 as the correct
 * protocol. Kept here until the login flow is corrected.
 */
export function md5(input: string): string {
  return createHash('md5').update(input).digest('hex');
}

/**
 * SHA-256 hash, used for encrypted credential storage.
 */
export function sha256(input: string): string {
  return createHash('sha256').update(input).digest('hex');
}

/**
 * Random bytes generator for IV/salt in credential encryption.
 */
export function randomBytes(size: number): Buffer {
  const { randomBytes: rb } = require('node:crypto');
  return rb(size);
}
