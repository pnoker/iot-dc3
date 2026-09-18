import { createHash } from 'node:crypto';

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
