import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { SilentExit } from '../src/utils/format.js';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

type FetchInit = {method?: string; headers?: unknown; body?: unknown};
const fetchCalls: Array<{url: string; init: FetchInit}> = [];

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({gateway: 'http://gw.test/', tenant: 't', username: 'u'})),
    getActiveProfileName: vi.fn(async () => 'default'),
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
import {registerDeviceCommand, waitForOperation} from '../src/commands/device.js';
import {registerDriverCommand} from '../src/commands/driver.js';

function buildProgram(): Command {
  const program = new Command();
  program.exitOverride();
  registerDeviceCommand(program);
  registerDriverCommand(program);
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
  } catch (error) {
    if (!(error instanceof SilentExit)) throw error;
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
    const requestPart = form.get('request') as File;
    const filePart = form.get('file') as File;
    expect(JSON.parse(await requestPart.text())).toEqual({driverId: '11', profileId: '12'});
    expect(requestPart.type).toBe('application/json');
    expect(requestPart.name).toBe('request.json');
    expect(filePart.type).toBe('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    expect(filePart.name).toBe('devices.xlsx');
    expect(fetchCalls[0].init.headers).toEqual({'Idempotency-Key': 'import-1'});
    expect(fetchCalls).toHaveLength(1);
    expect(JSON.parse(output)).toMatchObject({operationId: 'op-1'});

    // Golden wire (F046): serialize with the same machinery fetch uses and pin
    // the exact part dispositions, so an undici upgrade cannot silently drift
    // the canonical multipart contract back to filename="blob".
    const wire = await new Request('http://gw.test/import', {method: 'POST', body: form}).text();
    expect(wire).toContain('Content-Disposition: form-data; name="request"; filename="request.json"');
    expect(wire).toContain('Content-Type: application/json\r\n\r\n{"driverId":"11","profileId":"12"}');
    expect(wire).toContain(`Content-Disposition: form-data; name="file"; filename="devices.xlsx"`);
    expect(wire).not.toContain('filename="blob"');
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
    await expect(buildProgram().parseAsync(
      ['device', 'import', invalid, '--driver-id', '11', '--profile-id', '12'],
      {from: 'user'},
    )).rejects.toMatchObject({kind: 'validation', message: 'Import file must use the .xlsx extension'});
    expect(fetchCalls).toHaveLength(0);
  });

  it('rejects an empty import file before making a network request', async () => {
    const empty = join(directory, 'empty.xlsx');
    await writeFile(empty, Buffer.alloc(0));
    await expect(buildProgram().parseAsync(
      ['device', 'import', empty, '--driver-id', '11', '--profile-id', '12'],
      {from: 'user'},
    )).rejects.toMatchObject({kind: 'validation', message: 'Import file must not be empty'});
    expect(fetchCalls).toHaveLength(0);
  });

  it('reports a missing import file with the user-supplied path and no absolute-path leak (F047)', async () => {
    const previousCwd = process.cwd();
    process.chdir(directory);
    try {
      const failure = await buildProgram().parseAsync(
        ['device', 'import', 'missing.xlsx', '--driver-id', '11', '--profile-id', '12', '--format', 'json'],
        {from: 'user'},
      ).then(() => null, (error: Error) => error);
      // Structured validation failure with the exact user-supplied path: the
      // raw errno message would have embedded the resolved absolute path.
      expect(failure).toMatchObject({
        kind: 'validation',
        exitCode: 1,
        message: 'Import file not found: missing.xlsx',
      });
      expect((failure as Error).message).not.toContain(tmpdir());
    } finally {
      process.chdir(previousCwd);
    }
    expect(fetchCalls).toHaveLength(0);
  });
});

describe('driver status single-driver endpoint (G25)', () => {
  beforeEach(() => {
    fetchCalls.length = 0;
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  const stubStatus = (body: string, status = 200): void => {
    vi.stubGlobal('fetch', vi.fn(async (url: string | URL, init?: RequestInit) => {
      fetchCalls.push({url: String(url), init});
      return new Response(body, {status});
    }));
  };

  const statusCalls = () => fetchCalls.filter((call) => call.url.includes('/driver/status/get_by_driver_id'));

  it('resolves one driver from the single-key map, hitting the endpoint exactly once', async () => {
    stubStatus(JSON.stringify({d42: 'OFFLINE'}));

    const output = await run(['driver', 'status', 'd42', '--format', 'json']);

    expect(JSON.parse(output)).toEqual({id: 'd42', status: 'OFFLINE'});
    expect(statusCalls()).toHaveLength(1);
    expect(statusCalls()[0].url).toBe('http://gw.test/api/v3/data/driver/status/get_by_driver_id?driver_id=d42');
  });

  it('URL-encodes the driver id query parameter', async () => {
    stubStatus(JSON.stringify({'d & 1': 'ONLINE'}));

    await run(['driver', 'status', 'd & 1', '--format', 'json']);

    expect(statusCalls()[0].url).toBe(
      'http://gw.test/api/v3/data/driver/status/get_by_driver_id?driver_id=d%20%26%201',
    );
  });

  it('rejects a map whose sole key does not echo the requested id (contract guard)', async () => {
    stubStatus(JSON.stringify({someoneElse: 'ONLINE'}));

    await expect(
      buildProgram().parseAsync(['driver', 'status', 'd42', '--format', 'json'], {from: 'user'}),
    ).rejects.toMatchObject({kind: 'validation', message: 'Driver d42 status not found'});
  });

  it('surfaces a gateway 404 for an unknown driver through the typed api error', async () => {
    stubStatus(JSON.stringify({detail: 'Driver does not exist'}), 404);

    await expect(
      buildProgram().parseAsync(['driver', 'status', 'd999', '--format', 'json'], {from: 'user'}),
    ).rejects.toMatchObject({kind: 'api', statusCode: 404});
  });
});

describe('wait deadline elapsed rendering (G17)', () => {
  const ACCEPTED = {operationId: 'op-1', statusUri: '/api/v3/manager/operations/get_by_id?id=op-1'};

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('a sub-second deadline never renders as "after 0s"', async () => {
    fetchCalls.length = 0;
    vi.stubGlobal('fetch', vi.fn(async () => new Response(
      JSON.stringify({
        operationId: 'op-1', status: 'RUNNING', progress: 1, result: null, error: null,
        createdAt: '1970-01-01T00:00:00Z', updatedAt: '1970-01-01T00:00:01Z', expiresAt: null,
      }),
      {status: 200},
    )));

    const failure = await waitForOperation(ACCEPTED, 100, 0).then(
      () => null,
      (error: Error) => error,
    );

    expect(failure).toMatchObject({kind: 'timeout', code: 'TIMEOUT'});
    const message = (failure as Error).message;
    expect(message).toMatch(/after 0\.1s/u);
    expect(message).not.toMatch(/after 0s/u);
  });
});
