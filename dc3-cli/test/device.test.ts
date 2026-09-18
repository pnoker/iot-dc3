import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

type FetchInit = {method?: string; headers?: unknown; body?: unknown};
const fetchCalls: Array<{url: string; init: FetchInit}> = [];

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({gateway: 'http://gw.test/', tenant: 't', username: 'u'})),
    load: vi.fn(async () => ({current_profile: 'default'})),
    getSettings: vi.fn(async () => ({renewal_threshold_hours: 12})),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    needsRenewal: vi.fn(async () => false),
  },
}));

import {Command} from 'commander';
import {registerDeviceCommand} from '../src/commands/device.js';

function buildProgram(): Command {
  const program = new Command();
  program.exitOverride();
  registerDeviceCommand(program);
  return program;
}

async function run(args: string[]): Promise<string> {
  const program = buildProgram();
  let output = '';
  vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
    output += String(chunk);
    return true;
  });
  vi.spyOn(process, 'exit').mockImplementation((() => undefined) as never);
  try {
    await program.parseAsync(args, {from: 'user'});
  } finally {
    (process.stdout.write as ReturnType<typeof vi.spyOn>).mockRestore();
  }
  return output;
}

describe('device import command', () => {
  let directory: string;
  let file: string;

  beforeEach(async () => {
    fetchCalls.length = 0;
    directory = await mkdtemp(join(tmpdir(), 'dc3-cli-device-'));
    file = join(directory, 'devices.xlsx');
    await writeFile(file, Buffer.from([1, 2, 3, 4]));
    vi.stubGlobal('fetch', vi.fn(async (url: string, init?: FetchInit) => {
      fetchCalls.push({url, init: init ?? {}});
      if (url.includes('/device/import')) {
        return new Response(JSON.stringify({operationId: 'op-1', statusUri: '/api/v3/manager/operations/get_by_id?id=op-1'}), {status: 202});
      }
      return new Response(JSON.stringify({
        operationId: 'op-1', status: 'SUCCEEDED', progress: 100, result: {imported: 1},
        error: null, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:01Z', expiresAt: null,
      }), {status: 200});
    }));
  });

  afterEach(async () => {
    vi.unstubAllGlobals();
    await rm(directory, {recursive: true, force: true});
  });

  it('sends the canonical JSON request part, XLSX file, and idempotency key', async () => {
    const output = await run([
      'device', 'import', file, '--driver-id', '11', '--profile-id', '12',
      '--idempotency-key', 'import-1', '--no-wait', '--format', 'json',
    ]);
    const form = fetchCalls[0].init.body as FormData;
    const requestPart = form.get('request');
    const filePart = form.get('file') as Blob & {name?: string};
    expect(JSON.parse(await (requestPart as Blob).text())).toEqual({driverId: '11', profileId: '12'});
    expect(filePart.type).toBe('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    expect(filePart.name).toBe('devices.xlsx');
    expect(fetchCalls[0].init.headers).toEqual({'Idempotency-Key': 'import-1'});
    expect(fetchCalls).toHaveLength(1);
    expect(JSON.parse(output)).toMatchObject({operationId: 'op-1'});
  });

  it('polls the status URI and returns a terminal operation by default', async () => {
    const output = await run(['device', 'import', file, '--driver-id', '11', '--profile-id', '12', '--format', 'json']);
    expect(fetchCalls).toHaveLength(2);
    expect(fetchCalls[1].url).toBe('http://gw.test/api/v3/manager/operations/get_by_id?id=op-1');
    expect(JSON.parse(output)).toMatchObject({status: 'SUCCEEDED', progress: 100});
  });

  it('rejects non-XLSX files before making a network request', async () => {
    const invalid = join(directory, 'devices.csv');
    await writeFile(invalid, 'x');
    await run(['device', 'import', invalid, '--driver-id', '11', '--profile-id', '12']);
    expect(fetchCalls).toHaveLength(0);
  });
});
