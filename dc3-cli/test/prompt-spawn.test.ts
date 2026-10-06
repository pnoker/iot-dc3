import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

/*
 * Spawn-level guards for the interactive-input contract (reports F004/F013,
 * spec item 4). Children run the REAL sources through Node type stripping
 * (plus a resolve hook mapping compiled .js specifiers back to .ts), so exit
 * codes, stream channels, and state writes are observed at process level:
 *
 * - EOF stdin → non-zero exit, a stderr diagnostic, and no state writes;
 * - open-but-silent stdin → still terminates (no hang);
 * - passwordPrompt keeps stdout byte-clean and fully functional.
 */

const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..');
const fixtures = join(repoRoot, 'test', 'fixtures');

/** Upper bound for a child that must terminate on its own (hang guard). */
const CHILD_TIMEOUT_MS = 30_000;

interface ChildResult {
  code: number | null;
  stdout: string;
  stderr: string;
}

/**
 * Spawn a fixture child with a redirected HOME and a controllable stdin.
 * @param fixture - fixture script under test/fixtures
 * @param args - arguments after the fixture path
 * @param options.stdin - 'answer' writes one line and ends; 'closed' uses
 *   'ignore'; 'silent' keeps the pipe open without ever writing
 * @param options.envHome - directory used as the child's HOME/USERPROFILE
 * @returns exit code and captured streams
 */
function runChild(
  fixture: string,
  args: string[],
  options: {
    stdin: 'answer' | 'closed' | 'silent';
    stdinLine?: string;
    envHome?: string;
    envExtra?: Record<string, string>;
  },
): Promise<ChildResult> {
  return new Promise((resolve, reject) => {
    const child = spawn(
      process.execPath,
      // Relative paths on purpose: Windows absolute paths (C:/...) are parsed
      // as URL schemes by --import; relative ones resolve against cwd.
      [
        '--experimental-strip-types',
        '--import',
        './test/fixtures/register-ts-resolve.mjs',
        join('./test/fixtures', fixture),
        ...args,
      ],
      {
        cwd: repoRoot,
        env: {
          ...process.env,
          DC3_PASSWORD: '',
          ...(options.envHome ? { HOME: options.envHome, USERPROFILE: options.envHome } : {}),
          ...(options.envExtra ?? {}),
        },
        stdio: [options.stdin === 'closed' ? 'ignore' : 'pipe', 'pipe', 'pipe'],
        windowsHide: true,
      },
    );

    const timer = setTimeout(() => {
      child.kill();
      reject(new Error(`${fixture} ${args.join(' ')}: child did not exit within ${CHILD_TIMEOUT_MS}ms (hang regression)`));
    }, CHILD_TIMEOUT_MS);

    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk) => (stdout += String(chunk)));
    child.stderr.on('data', (chunk) => (stderr += String(chunk)));
    child.on('error', (error) => {
      clearTimeout(timer);
      reject(error);
    });
    child.on('close', (code) => {
      clearTimeout(timer);
      resolve({ code, stdout, stderr });
    });

    if (options.stdin === 'answer') {
      child.stdin!.write(`${options.stdinLine ?? ''}\n`);
      child.stdin!.end();
    }
    // 'silent': deliberately never write, never end — the child must still terminate.
  });
}

describe('spawn-level prompt termination and channels (F004/F013)', () => {
  it('passwordPrompt answers a piped password while keeping stdout clean and intact', async () => {
    const result = await runChild('prompt-child.mjs', ['password'], {
      stdin: 'answer',
      stdinLine: 'secret123',
    });

    expect(result.code).toBe(0);
    expect(result.stdout).toContain('password=secret123');
    // The child probes its own stdout: no own `_write`, nothing stuck in the buffer.
    expect(result.stdout).toContain('stdout-intact=true');
    // The question travels on stderr only — piped stdout stays machine-clean.
    expect(result.stderr).toContain('Password:');
    expect(result.stdout).not.toContain('Password:');
  }, 60_000);

  it('a prompt with closed stdin exits non-zero instead of silent exit 0', async () => {
    const result = await runChild('prompt-child.mjs', ['prompt'], { stdin: 'closed' });

    expect(result.code).toBe(1);
    expect(result.stdout).toContain('error=UsageError:1:');
    expect(result.stdout).toContain('stdin is closed');
  }, 60_000);
});

describe('spawn-level command gating (F004) and help surface (F051)', () => {
  let home: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-spawn-'));
  });

  afterEach(async () => {
    await rm(home, { recursive: true, force: true });
  });

  it('auth login with closed stdin fails fast and writes no state (negative F004 guard)', async () => {
    const result = await runChild('cli-child.mjs', ['auth', 'login'], {
      stdin: 'closed',
      envHome: home,
    });

    expect(result.code).not.toBe(0);
    expect(result.stderr).toContain('--tenant');
    expect(result.stderr).toContain('not interactive');
    // Silent fake success: no config.json, no tokens.json, nothing at all.
    expect(existsSync(join(home, '.dc3'))).toBe(false);
  }, 60_000);

  it('auth login with an open-but-silent stdin still terminates (hang guard)', async () => {
    const result = await runChild('cli-child.mjs', ['auth', 'login'], {
      stdin: 'silent',
      envHome: home,
    });

    // Reaching this assertion at all proves termination; a hang regression
    // fails via the child timeout instead.
    expect(result.code).not.toBe(0);
    expect(existsSync(join(home, '.dc3'))).toBe(false);
  }, 60_000);

  it('config reset with closed stdin refuses instead of exiting 0 silently', async () => {
    const configDir = join(home, '.dc3');
    await mkdir(configDir, { recursive: true });
    const configPath = join(configDir, 'config.json');
    const seeded = JSON.stringify(
      {
        version: 1,
        current_profile: 'default',
        settings: {},
        profiles: { default: { username: 'admin', gateway: 'http://localhost:8000' } },
      },
      null,
      2,
    );
    await writeFile(configPath, seeded, 'utf8');

    const result = await runChild('cli-child.mjs', ['config', 'reset'], {
      stdin: 'closed',
      envHome: home,
    });

    expect(result.code).not.toBe(0);
    expect(result.stderr).toContain('--yes');
    // The user's config is byte-for-byte untouched by the refusal.
    expect((await readFile(configPath, 'utf8')).trim()).toBe(seeded.trim());
  }, 60_000);

  it('config reset --yes wipes tokens and resets config non-interactively', async () => {
    const configDir = join(home, '.dc3');
    await mkdir(configDir, { recursive: true });
    await writeFile(
      join(configDir, 'config.json'),
      JSON.stringify({
        version: 1,
        current_profile: 'default',
        settings: {},
        profiles: { default: { username: 'admin', gateway: 'http://localhost:8000' } },
      }),
      'utf8',
    );
    await writeFile(
      join(configDir, 'tokens.json'),
      JSON.stringify({
        version: 2,
        epochs: {},
        states: { default: { token: 'tok', salt: 's', tenant: 't', username: 'u', issuedAt: 1, expiresAt: 2 } },
      }),
      'utf8',
    );

    const result = await runChild('cli-child.mjs', ['config', 'reset', '--yes'], {
      stdin: 'closed',
      envHome: home,
    });

    expect(result.code).toBe(0);
    expect(existsSync(join(configDir, 'tokens.json'))).toBe(false);
    const config = JSON.parse(await readFile(join(configDir, 'config.json'), 'utf8'));
    expect(config.profiles).toEqual({});
  }, 60_000);

  it('auth login --help documents the --oauth option (F051)', async () => {
    const result = await runChild('cli-child.mjs', ['auth', 'login', '--help'], {
      stdin: 'closed',
      envHome: home,
    });

    expect(result.code).toBe(0);
    expect(result.stdout).toContain('--oauth');
    expect(result.stdout).toContain('--client-id');
  }, 60_000);
});

describe('spawn-level headless login through DC3_PASSWORD (F033)', () => {
  it('auth login --store env logs in against a real gateway with closed stdin', async () => {
    const { createServer } = await import('node:http');
    const now = Math.floor(Date.now() / 1000);
    const encode = (value: Record<string, unknown>) =>
      Buffer.from(JSON.stringify(value)).toString('base64url');
    const token = `${encode({ alg: 'HS256' })}.${encode({ sub: '1', iat: now, exp: now + 7200 })}.sig`;

    const requests: Array<{ url: string; body: Record<string, unknown> }> = [];
    const server = createServer((req, res) => {
      let body = '';
      req.on('data', (chunk) => (body += String(chunk)));
      req.on('end', () => {
        requests.push({
          url: String(req.url),
          body: body ? (JSON.parse(body) as Record<string, unknown>) : {},
        });
        if (req.url?.endsWith('/token/salt')) {
          res.writeHead(200, { 'content-type': 'text/plain' });
          res.end('salt-abc');
          return;
        }
        if (req.url?.endsWith('/token/generate')) {
          res.writeHead(200, { 'content-type': 'text/plain' });
          res.end(token);
          return;
        }
        res.writeHead(404);
        res.end('{}');
      });
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    const port = (server.address() as { port: number }).port;

    const home = await mkdtemp(join(tmpdir(), 'dc3-cli-headless-'));
    try {
      await mkdir(join(home, '.dc3'), { recursive: true });
      await writeFile(
        join(home, '.dc3', 'config.json'),
        JSON.stringify({
          version: 1,
          current_profile: 'default',
          settings: {},
          profiles: {
            default: {
              gateway: `http://127.0.0.1:${port}`,
              username: 'seed-user',
              credential_store: 'env',
            },
          },
        }),
        'utf8',
      );

      const result = await runChild(
        'cli-child.mjs',
        ['auth', 'login', '-t', 'tenantA', '-u', 'admin', '--store', 'env'],
        { stdin: 'closed', envHome: home, envExtra: { DC3_PASSWORD: 'env-pw' } },
      );

      // Headless login: exit 0 with closed stdin, no prompt, no hang.
      expect(result.code, result.stderr).toBe(0);
      expect(result.stderr).not.toContain('Password:');
      const payload = JSON.parse(result.stdout);
      expect(payload.ok).toBe(true);
      expect(payload.password_saved).toBe(true);
      // The env password traveled on the wire and the state persisted.
      expect(requests).toHaveLength(2);
      expect(requests[1].body).toMatchObject({ name: 'admin', tenant: 'tenantA', password: 'env-pw' });
      const tokens = JSON.parse(await readFile(join(home, '.dc3', 'tokens.json'), 'utf8'));
      expect(tokens.states.default.username).toBe('admin');
      expect(tokens.states.default.tenant).toBe('tenantA');
    } finally {
      await rm(home, { recursive: true, force: true });
      server.close();
    }
  }, 60_000);
});
