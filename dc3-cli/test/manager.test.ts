import { describe, it, expect, vi, beforeEach } from 'vitest';
import { InvalidArgumentError } from 'commander';
import { ValidationError } from '../src/core/errors.js';

/*
 * Guard tests for utils/manager.ts: F035 (lexical integer parsing), F038
 * (delete 2xx-body contract), F053b (empty/whitespace id rejection before any
 * request).
 */
vi.mock('../src/core/client.js', () => ({
  dc3Client: {
    get: vi.fn(async () => ({})),
    post: vi.fn(async () => ({})),
    del: vi.fn(async () => undefined),
  },
}));

import { dc3Client } from '../src/core/client.js';
import {
  parseNonNegativeInteger,
  parsePositiveInteger,
  getManagerResource,
  updateManagerResource,
  deleteManagerResource,
} from '../src/utils/manager.js';

const BASE = '/api/v3/manager/device';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('lexical integer parsing (F035)', () => {
  const plainAccepted = ['0', '5', '100', '07'];
  // Numeric coercion (Number) silently accepts every one of these today:
  // '' → 0 (empty pagination), ' 5' → 5, '0x10' → 16, '5.0' → 5, '1e2' → 100.
  const rejected = ['', ' ', ' 5', '5 ', '0x10', '5.0', '1e2', 'abc', '-5', '+5', '5.', '1_0'];

  it.each(plainAccepted)('parseNonNegativeInteger accepts plain decimal %s', (value) => {
    expect(parseNonNegativeInteger(value)).toBe(Number(value));
  });

  it.each(rejected)(
    'parseNonNegativeInteger rejects the coerced form %s',
    (value) => {
      expect(() => parseNonNegativeInteger(value)).toThrow(InvalidArgumentError);
    },
  );

  it('parseNonNegativeInteger rejects values beyond the safe integer range', () => {
    expect(() => parseNonNegativeInteger('9007199254740993')).toThrow(InvalidArgumentError);
  });

  it.each(['1', '5', '100'])('parsePositiveInteger accepts plain decimal %s', (value) => {
    expect(parsePositiveInteger(value)).toBe(Number(value));
  });

  it('parsePositiveInteger rejects zero and every coerced form', () => {
    expect(() => parsePositiveInteger('0')).toThrow(InvalidArgumentError);
    for (const value of ['', ' 5', '0x10', '5.0', '1e2']) {
      expect(() => parsePositiveInteger(value)).toThrow(InvalidArgumentError);
    }
  });
});

describe('resource id hygiene (F053b)', () => {
  // Business validation, not an argv-shape error: the id must fail as a
  // typed ValidationError (kind "validation", exit 1) before any request.
  it.each(['', ' ', '   '])('delete rejects the empty/whitespace id %s before any request', async (id) => {
    await expect(deleteManagerResource(BASE, id, 3)).rejects.toBeInstanceOf(ValidationError);
    expect(dc3Client.del).not.toHaveBeenCalled();
  });

  it.each(['', ' '])('update rejects the empty/whitespace id %s before any request', async (id) => {
    await expect(updateManagerResource(BASE, id, 3, {})).rejects.toBeInstanceOf(ValidationError);
    expect(dc3Client.get).not.toHaveBeenCalled();
    expect(dc3Client.post).not.toHaveBeenCalled();
  });

  it.each(['', ' '])('get rejects the empty/whitespace id %s before any request', async (id) => {
    await expect(getManagerResource(BASE, id)).rejects.toBeInstanceOf(ValidationError);
    expect(dc3Client.get).not.toHaveBeenCalled();
  });

  it('get builds the encoded get_by_id URL for a valid id', async () => {
    await getManagerResource(BASE, 'id A&B');
    expect(dc3Client.get).toHaveBeenCalledWith(`${BASE}/get_by_id?id=id%20A%26B`);
  });
});

describe('delete 2xx-body contract (F038)', () => {
  it('returns a non-empty 2xx delete body so the command layer prints it', async () => {
    const body = { ok: true, warning: 'device had dependent points' };
    (dc3Client.del as ReturnType<typeof vi.fn>).mockResolvedValueOnce(body);
    await expect(deleteManagerResource(BASE, '7', 3)).resolves.toBe(body);
    expect(dc3Client.del).toHaveBeenCalledWith(`${BASE}/delete?id=7&version=3`);
  });

  it('204/empty delete bodies stay silent (resolve to undefined)', async () => {
    (dc3Client.del as ReturnType<typeof vi.fn>).mockResolvedValueOnce(undefined);
    await expect(deleteManagerResource(BASE, '7', 3)).resolves.toBeUndefined();
  });
});
