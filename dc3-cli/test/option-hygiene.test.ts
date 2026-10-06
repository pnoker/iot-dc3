import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { readdir, readFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Command } from 'commander';
import { SilentExit } from '../src/utils/format.js';

/*
 * Truthiness option-guard hygiene (audit G32): every string-valued optional
 * must be forwarded on presence (`!== undefined`), never on truthiness — an
 * explicit `--driver-id ""` is a filter the user asked for, not an absent
 * flag. The static scan bans the two truthiness forms across src/commands,
 * with an explicit allowlist for genuine boolean flags; a behavioral test
 * pins the filter contract end to end.
 */

const commandsDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'commands');

/**
 * Boolean flags (declared without a value) exempt from the existence-check
 * rule, per file. Adding an entry requires the flag to be a real boolean.
 */
const BOOLEAN_FLAG_ALLOWLIST: Record<string, Set<string>> = {
  'chat.ts': new Set(['stream']),
  'model.ts': new Set(['default', 'disable', 'enable']),
  'provider.ts': new Set(['default', 'disable', 'enable']),
};

/**
 * Truthiness guard forms banned on command options. The `?` lookahead spares
 * the nullish coalesce (`opts.x ?? fallback`), which tests null/undefined —
 * not the empty string — and is therefore correct for defaulted options.
 */
const GUARD_PATTERNS: Array<{ label: string; regex: RegExp }> = [
  { label: 'if (opts.X)', regex: /if \(opts\.(\w+)\)/gu },
  { label: 'opts.X ?', regex: /opts\.(\w+) \?(?!\?)/gu },
];

/**
 * Scan one source text for truthiness guards outside the boolean allowlist.
 * @param source - command file content
 * @param file - file name for the allowlist lookup
 * @returns human-readable violation labels
 */
function truthinessViolations(source: string, file: string): string[] {
  const violations: string[] = [];
  for (const { label, regex } of GUARD_PATTERNS) {
    regex.lastIndex = 0;
    let match = regex.exec(source);
    while (match !== null) {
      const property = match[1];
      if (!BOOLEAN_FLAG_ALLOWLIST[file]?.has(property)) {
        violations.push(`${file}: "${label}" on opts.${property}`);
      }
      match = regex.exec(source);
    }
  }
  return violations;
}

describe('static scan: no truthiness guards on string-valued options (G32)', () => {
  it('src/commands is free of truthiness option guards outside the boolean allowlist', async () => {
    const files = (await readdir(commandsDir)).filter((file) => file.endsWith('.ts')).sort();
    expect(files.length).toBeGreaterThan(10);
    const violations: string[] = [];
    for (const file of files) {
      const source = await readFile(join(commandsDir, file), 'utf8');
      violations.push(...truthinessViolations(source, file));
    }
    expect(violations, violations.join('\n')).toEqual([]);
  });

  it('negative control: the detector flags truthiness guards and spares the nullish coalesce', () => {
    const source = [
      'if (opts.driverId) body.driverId = opts.driverId;',
      'const x = opts.format ? opts.format : "json";',
      'const y = opts.idempotencyKey ?? uuid();',
      'if (opts.stream) { stream(); }',
    ].join('\n');
    expect(truthinessViolations(source, 'device.ts')).toEqual([
      'device.ts: "if (opts.X)" on opts.driverId',
      'device.ts: "if (opts.X)" on opts.stream',
      'device.ts: "opts.X ?" on opts.format',
    ]);
    // The boolean allowlist entry is honored by the same detector.
    expect(truthinessViolations(source, 'chat.ts')).toEqual([
      'chat.ts: "if (opts.X)" on opts.driverId',
      'chat.ts: "opts.X ?" on opts.format',
    ]);
  });
});

/*
 * Behavioral half of the guard: run the real command tree against a stubbed
 * gateway and pin the wire body.
 */
vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({
      gateway: 'http://gw.test/',
      tenant: 'tenantA',
      username: 'admin',
    })),
    getActiveProfileName: vi.fn(async () => 'default'),
    getSettings: vi.fn(async () => ({ renewal_threshold_hours: 12 })),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    needsRenewal: vi.fn(async () => false),
  },
}));

import { registerDeviceCommand } from '../src/commands/device.js';
import { registerProfileCommand } from '../src/commands/profile.js';

type FetchCall = { url: string; init: RequestInit };
const fetchCalls: FetchCall[] = [];

async function runDeviceList(args: string[]): Promise<void> {
  const program = new Command();
  program.exitOverride();
  registerDeviceCommand(program);
  const spy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true);
  try {
    try {
      await program.parseAsync(['device', ...args], { from: 'user' });
    } catch (error) {
      if (!(error instanceof SilentExit)) throw error;
    }
  } finally {
    spy.mockRestore();
  }
}

/**
 * Run `profile list` and return the escaped error (undefined on success).
 * @param args - arguments after `profile`
 * @returns the error thrown by the action, or undefined on success
 */
async function runProfileList(args: string[]): Promise<unknown> {
  const program = new Command();
  program.exitOverride();
  registerProfileCommand(program);
  const spy = vi.spyOn(process.stdout, 'write').mockImplementation(() => true);
  try {
    try {
      await program.parseAsync(['profile', ...args], { from: 'user' });
    } catch (error) {
      if (!(error instanceof SilentExit)) return error;
    }
  } finally {
    spy.mockRestore();
  }
  return undefined;
}

describe('behavioral guard: explicit empty option values are honored (G32)', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        fetchCalls.push({ url: String(url), init: init ?? {} });
        return new Response(JSON.stringify({ ok: true }), { status: 200 });
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('device list --driver-id "" sends the filter key with the empty value', async () => {
    await runDeviceList(['list', '--driver-id', '', '--format', 'json']);
    expect(fetchCalls).toHaveLength(1);
    const body = JSON.parse(String(fetchCalls[0].init.body)) as Record<string, unknown>;
    expect(body.driverId).toBe('');
    expect(body).toMatchObject({ offset: 0, limit: 20 });
  });

  it('device list without --driver-id omits the filter key entirely', async () => {
    await runDeviceList(['list', '--format', 'json']);
    const body = JSON.parse(String(fetchCalls[0].init.body)) as Record<string, unknown>;
    expect('driverId' in body).toBe(false);
  });

  it('profile list --device-id "" rejects the empty filter before any request (G32 residual)', async () => {
    const error = await runProfileList(['list', '--device-id', '']);

    // An explicitly empty device filter is invalid input, not an absent flag:
    // kind validation, exit 1, and zero requests (never `?device_id=`).
    expect(error).toMatchObject({ name: 'ValidationError', kind: 'validation', exitCode: 1 });
    expect((error as Error).message).toMatch(/--device-id must be a non-empty value/u);
    expect(fetchCalls).toHaveLength(0);
  });

  it('profile list --device-id "  " (whitespace-only) is rejected the same way', async () => {
    const error = await runProfileList(['list', '--device-id', '   ']);

    expect(error).toMatchObject({ name: 'ValidationError', kind: 'validation' });
    expect(fetchCalls).toHaveLength(0);
  });
});
