import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Command } from 'commander';

/*
 * Negative guards for JWT payload validation (report F016 leftover / jwt
 * hardening): a well-formed 3-part token whose payload lacks NUMERIC exp/iat
 * used to pass through the decode cast, minting a NaN expiresAt that read as
 * an eternally valid session ("NaNh" remaining). Every layer must treat such
 * a token as expired instead.
 */

const osStub = vi.hoisted(() => ({ home: '' }));

vi.mock('node:os', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:os')>();
  return {
    ...actual,
    homedir: () => (osStub.home ? osStub.home : actual.homedir()),
  };
});

// The auth command tree pulls in the credential chain; a controllable
// keychain double keeps the command-level runs hermetic and fast.
vi.mock('../src/core/credential-keychain.js', () => ({
  KeychainStore: class {
    readonly name = 'keychain';
    async isAvailable(): Promise<boolean> {
      return false;
    }
    async getPassword(): Promise<string | null> {
      return null;
    }
    async savePassword(): Promise<void> {}
    async deletePassword(): Promise<void> {}
  },
}));

import { decodeJwt, isTokenExpired, tokenTtl } from '../src/utils/jwt.js';

function fakeJwt(payload: Record<string, unknown> | unknown): string {
  const encode = (value: unknown) =>
    Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.sig`;
}

describe('decodeJwt payload validation (jwt hardening)', () => {
  it('decodes a payload carrying numeric exp and iat', () => {
    const payload = decodeJwt(fakeJwt({ sub: '1', iat: 100, exp: 2_000_000_000 }));
    expect(payload.exp).toBe(2_000_000_000);
    expect(payload.iat).toBe(100);
  });

  it.each([
    ['an empty payload object', fakeJwt({})],
    ['a string exp', fakeJwt({ exp: 'soon', iat: 1 })],
    ['a missing iat', fakeJwt({ exp: 2_000_000_000 })],
    ['a string iat', fakeJwt({ exp: 2_000_000_000, iat: 'now' })],
    ['a non-object payload', fakeJwt(42)],
  ])('throws a typed validation error for %s', (_label, token) => {
    let error: unknown;
    try {
      decodeJwt(token as string);
    } catch (caught) {
      error = caught;
    }
    expect(error).toBeInstanceOf(Error);
    expect((error as Error).name).toBe('ValidationError');
    expect((error as Error).message).toMatch(/exp and iat must be numeric/u);
  });

  it('still rejects tokens that are not three dot-separated parts', () => {
    expect(() => decodeJwt('two.parts')).toThrow(/3 parts/u);
  });

  it('isTokenExpired treats an undecodable token as expired, never eternal', () => {
    expect(isTokenExpired(fakeJwt({ exp: 'soon', iat: 1 }))).toBe(true);
    expect(isTokenExpired(fakeJwt({}))).toBe(true);
    // Control: a genuinely long-lived token is not expired.
    expect(isTokenExpired(fakeJwt({ iat: 1, exp: 2_000_000_000 }))).toBe(false);
  });

  it('tokenTtl is maximally negative for an undecodable token', () => {
    expect(tokenTtl(fakeJwt({ exp: 'soon', iat: 1 }))).toBe(Number.NEGATIVE_INFINITY);
  });
});

describe('token-manager treats malformed persisted tokens as expired', () => {
  let home: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-jwtguard-'));
    osStub.home = home;
    vi.resetModules();
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    await mkdir(join(home, '.dc3'), { recursive: true });
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  /**
   * Persist a handcrafted state the way a broken mint would have written it:
   * the expiresAt mirrors the (non-numeric) jwt exp, exactly what login used
   * to store before decode validation existed.
   * @param token - raw token string to persist
   * @param expiresAt - expiry mirror to persist alongside it
   */
  async function seedState(token: string, expiresAt: unknown): Promise<void> {
    await writeFile(
      join(home, '.dc3', 'tokens.json'),
      JSON.stringify({
        version: 2,
        epochs: {},
        states: {
          default: {
            token,
            salt: '',
            tenant: 'tenantA',
            username: 'admin',
            issuedAt: 1,
            ...(expiresAt !== undefined ? { expiresAt } : {}),
          },
        },
      }),
      'utf8',
    );
  }

  it.each([
    ['payload {"exp":"soon"}', fakeJwt({ exp: 'soon', iat: 1 }), 'soon'],
    ['an empty payload', fakeJwt({}), undefined],
  ])('needs renewal, is unauthenticated, and has no TTL for %s', async (_l, token, exp) => {
    await seedState(token, exp);
    const { tokenManager } = await import('../src/core/token-manager.js');

    expect(await tokenManager.needsRenewal('default', 3600)).toBe(true);
    expect(await tokenManager.isAuthenticated('default')).toBe(false);
    expect(await tokenManager.getTtl('default')).toBe(Number.NEGATIVE_INFINITY);
  });
});

describe('auth status never reports NaN-driven eternal validity', () => {
  let home: string;
  let prevExitCode: number | string | undefined;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-jwtstatus-'));
    osStub.home = home;
    vi.resetModules();
    await mkdir(join(home, '.dc3'), { recursive: true });
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  /**
   * Run `auth status` in-process against the current temp HOME.
   * @returns captured stdout, stderr, effective exit code, and escaped error
   */
  async function runStatus(): Promise<{
    stdout: string;
    stderr: string;
    code: number;
    error?: { name?: string; message?: string };
  }> {
    vi.resetModules();
    const { registerAuthCommand } = await import('../src/commands/auth.js');
    const { classifyError } = await import('../src/core/errors.js');
    const program = new Command();
    program.exitOverride();
    program.configureOutput({ writeOut: () => undefined, writeErr: () => undefined });
    registerAuthCommand(program);

    let stdout = '';
    let stderr = '';
    vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
      stdout += String(chunk);
      return true;
    });
    vi.spyOn(process.stderr, 'write').mockImplementation((chunk) => {
      stderr += String(chunk);
      return true;
    });
    vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
      stderr += args.join(' ');
    });
    prevExitCode = process.exitCode ?? undefined;
    process.exitCode = undefined;

    let result: { stdout: string; stderr: string; code: number; error?: { name?: string } };
    try {
      await program.parseAsync(['auth', 'status'], { from: 'user' });
      result = { stdout, stderr, code: process.exitCode ?? 0 };
    } catch (error) {
      if ((error as Error)?.name === 'SilentExit') {
        result = { stdout, stderr, code: process.exitCode ?? 0 };
      } else {
        result = {
          stdout,
          stderr,
          code: classifyError(error as Error).exitCode,
          error: error as { name?: string },
        };
      }
    }
    process.exitCode = prevExitCode;
    return result;
  }

  it.each([
    ['payload {"exp":"soon"} mirrored into expiresAt', fakeJwt({ exp: 'soon', iat: 1 }), 'soon'],
    ['an empty payload with no expiresAt at all', fakeJwt({}), undefined],
  ])('reports expired (never NaN) for %s', async (_label, token, exp) => {
    await writeFile(
      join(home, '.dc3', 'tokens.json'),
      JSON.stringify({
        version: 2,
        epochs: {},
        states: {
          default: {
            token,
            salt: '',
            tenant: 'tenantA',
            username: 'admin',
            issuedAt: 1,
            ...(exp !== undefined ? { expiresAt: exp } : {}),
          },
        },
      }),
      'utf8',
    );

    const result = await runStatus();

    expect(result.code, result.stderr).toBe(0);
    const payload = JSON.parse(result.stdout);
    expect(payload.authenticated).toBe(false);
    expect(payload.remaining).toBe('expired');
    expect(payload.expires_at).toBe('unknown');
    // The headline negative guard: no NaN ever reaches the user.
    expect(result.stdout).not.toMatch(/NaN/u);
  });
});
