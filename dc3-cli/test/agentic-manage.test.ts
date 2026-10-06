import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Command } from 'commander';
import { registerProviderCommand } from '../src/commands/provider.js';
import { registerModelCommand } from '../src/commands/model.js';

const fetchCalls: Array<{ url: string; init: RequestInit }> = [];

/**
 * List responses the mocked client returns; overridable per test so the
 * update paths can be driven against bare arrays, envelopes, and hostile
 * shapes (report F043).
 */
let providerListResponse: unknown = [
  { id: '1', name: 'DeepSeek', baseUrl: 'https://api.deepseek.com', providerType: 'OPENAI_COMPATIBLE' },
];
let modelConfigListResponse: unknown = [
  { id: '10', model: 'deepseek-chat', label: 'DeepSeek Chat', providerId: '1', temperature: 0.7, maxTokens: 2048 },
];

vi.mock('../src/core/client.js', () => ({
  dc3Client: {
    get: vi.fn(async (url: string) => {
      fetchCalls.push({ url, init: { method: 'GET' } });
      if (url.includes('provider/list')) {
        return providerListResponse;
      }
      if (url.includes('model/list')) {
        return [{ model: 'deepseek-chat', label: 'DeepSeek Chat' }];
      }
      if (url.includes('model/config/list')) {
        return modelConfigListResponse;
      }
      return null;
    }),
    post: vi.fn(async (url: string, body?: unknown) => {
      fetchCalls.push({ url, init: { method: 'POST', body: JSON.stringify(body) } });
      return { ok: true };
    }),
    del: vi.fn(async (url: string) => {
      fetchCalls.push({ url, init: { method: 'DELETE' } });
      return undefined;
    }),
    request: vi.fn(),
  },
}));

vi.mock('../src/utils/config-manager.js', () => ({
  loadConfig: vi.fn(() => ({ gateway: 'http://gw.test' })),
  getActiveProfile: vi.fn(() => ({ gateway: 'http://gw.test' })),
}));

const run = async (args: string[]): Promise<void> => {
  const program = new Command();
  registerProviderCommand(program);
  registerModelCommand(program);
  program.exitOverride();
  vi.spyOn(process, 'exit').mockImplementation((() => undefined) as never);
  vi.spyOn(process.stdout, 'write').mockImplementation(() => true);
  try {
    try { await program.parseAsync(args, { from: 'user' }); } catch (e) { if (e?.name !== 'SilentExit') throw e; }
  } finally {
    (process.exit as unknown as ReturnType<typeof vi.spyOn>).mockRestore?.();
    (process.stdout.write as ReturnType<typeof vi.spyOn>).mockRestore();
  }
};

const postCalls = () => fetchCalls.filter((call) => call.init.method === 'POST');
const bodyOf = (call: { init: RequestInit }): Record<string, unknown> =>
  JSON.parse(String(call.init.body)) as Record<string, unknown>;

beforeEach(() => {
  fetchCalls.length = 0;
  process.exitCode = 0;
  providerListResponse = [
    { id: '1', name: 'DeepSeek', baseUrl: 'https://api.deepseek.com', providerType: 'OPENAI_COMPATIBLE' },
  ];
  modelConfigListResponse = [
    { id: '10', model: 'deepseek-chat', label: 'DeepSeek Chat', providerId: '1', temperature: 0.7, maxTokens: 2048 },
  ];
});

afterEach(() => {
  vi.clearAllMocks();
});

describe('provider command', () => {
  it('list calls the provider list endpoint', async () => {
    await run(['provider', 'list', '--format', 'json']);
    expect(fetchCalls[0]!.url).toBe('/api/v3/agentic/provider/list');
  });

  it('add posts the provider vo to the add endpoint', async () => {
    await run([
      'provider', 'add',
      '--name', 'TestProvider',
      '--base-url', 'https://api.test.com',
      '--type', 'ANTHROPIC',
      '--api-key', 'sk-test',
      '--format', 'json',
    ]);
    expect(fetchCalls[0]!.url).toBe('/api/v3/agentic/provider/config/add');
    const body = JSON.parse(String(fetchCalls[0]!.init.body));
    expect(body).toMatchObject({ name: 'TestProvider', baseUrl: 'https://api.test.com', providerType: 'ANTHROPIC', apiKey: 'sk-test' });
  });

  it('delete sends DELETE to the delete endpoint', async () => {
    await run(['provider', 'delete', '42', '--format', 'json']);
    expect(fetchCalls[0]!.init.method).toBe('DELETE');
    expect(fetchCalls[0]!.url).toContain('/agentic/provider/config/delete?id=42');
  });

  it('check with --id posts to the check endpoint', async () => {
    await run(['provider', 'check', '--id', '1', '--format', 'json']);
    expect(fetchCalls[0]!.url).toBe('/api/v3/agentic/provider/check');
    const body = JSON.parse(String(fetchCalls[0]!.init.body));
    expect(body).toMatchObject({ id: '1', level: 'BOTH' });
  });

  it('check with --base-url sends a draft check without id', async () => {
    await run([
      'provider', 'check',
      '--base-url', 'https://api.draft.com',
      '--type', 'OPENAI_COMPATIBLE',
      '--format', 'json',
    ]);
    const body = JSON.parse(String(fetchCalls[0]!.init.body));
    expect(body).toMatchObject({ baseUrl: 'https://api.draft.com', providerType: 'OPENAI_COMPATIBLE', level: 'BOTH' });
    expect(body.id).toBeUndefined();
  });

  it('check without --id or --base-url fails as a usage error', async () => {
    await expect(
      run(['provider', 'check', '--format', 'json']),
    ).rejects.toMatchObject({
      name: 'UsageError',
      message: 'Either --id (saved provider) or --base-url (draft) is required',
    });
    expect(fetchCalls).toHaveLength(0);
  });

  it('check rejects an invalid --level before any request (F021)', async () => {
    await expect(
      run(['provider', 'check', '--id', '1', '--level', 'GARBAGE_LEVEL', '--format', 'json']),
    ).rejects.toThrow(/allowed: L1, L2, BOTH/u);
    expect(fetchCalls).toHaveLength(0);
  });

  it('check accepts each documented --level and normalizes case', async () => {
    await run(['provider', 'check', '--id', '1', '--level', 'L1', '--format', 'json']);
    await run(['provider', 'check', '--id', '1', '--level', 'l2', '--format', 'json']);
    expect(JSON.parse(String(fetchCalls[0]!.init.body)).level).toBe('L1');
    expect(JSON.parse(String(fetchCalls[1]!.init.body)).level).toBe('L2');
  });

  it('update unwraps a {data:[...]} envelope list before resolving the entry (F043)', async () => {
    providerListResponse = {
      ok: true,
      data: [
        {
          id: '5',
          name: 'Old',
          baseUrl: 'https://old.example.com',
          providerType: 'ANTHROPIC',
          defaultFlag: 'NOT_DEFAULT',
          enableFlag: 'ENABLE',
        },
      ],
      total: 1,
    };
    await run(['provider', 'update', '5', '--name', 'New', '--format', 'json']);

    const update = postCalls().find((call) => call.url.endsWith('/provider/config/update'));
    expect(update, 'update POST issued for the envelope-wrapped entry').toBeDefined();
    expect(bodyOf(update!)).toEqual({
      id: '5',
      name: 'New',
      baseUrl: 'https://old.example.com',
      providerType: 'ANTHROPIC',
      defaultFlag: 'NOT_DEFAULT',
      enableFlag: 'ENABLE',
    });
  });

  it('update never re-sends server, audit, or secret-named fields from the list entry (F043)', async () => {
    providerListResponse = [
      {
        id: '5',
        name: 'Old',
        baseUrl: 'https://old.example.com',
        providerType: 'OPENAI_COMPATIBLE',
        apiKey: 'sk-echoed-secret',
        defaultFlag: 'NOT_DEFAULT',
        enableFlag: 'ENABLE',
        tenantId: 'tenantA',
        creatorId: 'u1',
        creatorName: 'ops',
        createTime: '2026-01-01T00:00:00Z',
        operatorId: 'u2',
        operatorName: 'ops2',
        operateTime: '2026-01-02T00:00:00Z',
        remark: 'server-side remark',
        lastCheckStatus: 'UP',
        lastCheckTime: '2026-01-03T00:00:00Z',
        lastCheckLatencyMs: 42,
        lastCheckErrorType: null,
        lastCheckErrorMessage: null,
        lastCheckModel: 'gpt',
      },
    ];
    await run(['provider', 'update', '5', '--base-url', 'https://new.example.com', '--format', 'json']);

    const update = postCalls().find((call) => call.url.endsWith('/provider/config/update'));
    expect(update).toBeDefined();
    const body = bodyOf(update!);
    expect(body).toEqual({
      id: '5',
      name: 'Old',
      baseUrl: 'https://new.example.com',
      providerType: 'OPENAI_COMPATIBLE',
      defaultFlag: 'NOT_DEFAULT',
      enableFlag: 'ENABLE',
    });
    // negative guard: none of the server/audit/secret fields may round-trip
    for (const leaked of [
      'apiKey',
      'tenantId',
      'creatorId',
      'creatorName',
      'createTime',
      'operatorId',
      'operatorName',
      'operateTime',
      'remark',
      'lastCheckStatus',
      'lastCheckTime',
      'lastCheckLatencyMs',
      'lastCheckErrorType',
      'lastCheckErrorMessage',
      'lastCheckModel',
    ]) {
      expect(body, `${leaked} must not be re-sent`).not.toHaveProperty(leaked);
    }
  });

  it('update sends an explicitly provided --api-key', async () => {
    providerListResponse = [{ id: '5', name: 'Old', baseUrl: 'u', providerType: 'OPENAI_COMPATIBLE' }];
    await run(['provider', 'update', '5', '--api-key', 'sk-fresh', '--format', 'json']);
    const update = postCalls().find((call) => call.url.endsWith('/provider/config/update'));
    expect(bodyOf(update!).apiKey).toBe('sk-fresh');
  });

  it('update on an unrecognizable list shape fails with a distinct error, not "not found" (F043)', async () => {
    providerListResponse = { ok: true, weird: true };
    await expect(
      run(['provider', 'update', '5', '--name', 'New', '--format', 'json']),
    ).rejects.toMatchObject({
      name: 'ApiError',
      message: expect.stringMatching(/unrecognized shape/u),
    });
    expect(
      postCalls().filter((call) => call.url.endsWith('/provider/config/update')),
    ).toHaveLength(0);
  });

  it('update of a missing id still reports not found without issuing an update', async () => {
    await expect(
      run(['provider', 'update', '99', '--name', 'New', '--format', 'json']),
    ).rejects.toMatchObject({ name: 'ApiError', message: 'Provider 99 not found' });
    expect(
      postCalls().filter((call) => call.url.endsWith('/provider/config/update')),
    ).toHaveLength(0);
  });
});

describe('model command', () => {
  it('list calls the model list endpoint', async () => {
    await run(['model', 'list', '--format', 'json']);
    expect(fetchCalls[0]!.url).toBe('/api/v3/agentic/model/list');
  });

  it('config-list calls the full config endpoint', async () => {
    await run(['model', 'config-list', '--format', 'json']);
    expect(fetchCalls[0]!.url).toBe('/api/v3/agentic/model/config/list');
  });

  it('add posts the model config vo', async () => {
    await run([
      'model', 'add',
      '--model', 'gpt-4o',
      '--provider-id', '1',
      '--temperature', '0.5',
      '--max-tokens', '4096',
      '--format', 'json',
    ]);
    expect(fetchCalls[0]!.url).toBe('/api/v3/agentic/model/config/add');
    const body = JSON.parse(String(fetchCalls[0]!.init.body));
    expect(body).toMatchObject({ model: 'gpt-4o', providerId: '1', temperature: 0.5, maxTokens: 4096 });
  });

  it('delete sends DELETE to the delete endpoint', async () => {
    await run(['model', 'delete', '10', '--format', 'json']);
    expect(fetchCalls[0]!.init.method).toBe('DELETE');
    expect(fetchCalls[0]!.url).toContain('/agentic/model/config/delete?id=10');
  });

  it('check posts to the check endpoint with the id param', async () => {
    await run(['model', 'check', '10', '--format', 'json']);
    expect(fetchCalls[0]!.init.method).toBe('POST');
    expect(fetchCalls[0]!.url).toContain('/agentic/model/config/check?id=10');
  });

  it('update unwraps a {data:[...]} envelope list before resolving the entry (F043)', async () => {
    modelConfigListResponse = {
      ok: true,
      data: [
        {
          id: '10',
          model: 'deepseek-chat',
          label: 'DeepSeek Chat',
          providerId: '1',
          stream: true,
          toolCall: true,
          temperature: 0.7,
          maxTokens: 2048,
          defaultFlag: 'NOT_DEFAULT',
          enableFlag: 'ENABLE',
        },
      ],
      total: 1,
    };
    await run(['model', 'update', '10', '--label', 'Renamed', '--format', 'json']);

    const update = postCalls().find((call) => call.url.endsWith('/model/config/update'));
    expect(update, 'update POST issued for the envelope-wrapped entry').toBeDefined();
    expect(bodyOf(update!)).toEqual({
      id: '10',
      model: 'deepseek-chat',
      label: 'Renamed',
      providerId: '1',
      stream: true,
      toolCall: true,
      temperature: 0.7,
      maxTokens: 2048,
      defaultFlag: 'NOT_DEFAULT',
      enableFlag: 'ENABLE',
    });
  });

  it('update never re-sends server or audit fields from the list entry (F043)', async () => {
    modelConfigListResponse = [
      {
        id: '10',
        model: 'deepseek-chat',
        label: 'DeepSeek Chat',
        providerId: '1',
        stream: true,
        toolCall: true,
        vision: false,
        reasoning: false,
        temperature: 0.7,
        maxTokens: 2048,
        defaultFlag: 'NOT_DEFAULT',
        enableFlag: 'ENABLE',
        providerName: 'DeepSeek',
        tenantId: 'tenantA',
        creatorId: 'u1',
        createTime: '2026-01-01T00:00:00Z',
        operateTime: '2026-01-02T00:00:00Z',
        lastCheckStatus: 'UP',
        lastCheckLatencyMs: 12,
      },
    ];
    await run(['model', 'update', '10', '--no-stream', '--format', 'json']);

    const update = postCalls().find((call) => call.url.endsWith('/model/config/update'));
    expect(update).toBeDefined();
    const body = bodyOf(update!);
    expect(body).toEqual({
      id: '10',
      model: 'deepseek-chat',
      label: 'DeepSeek Chat',
      providerId: '1',
      stream: false,
      toolCall: true,
      vision: false,
      reasoning: false,
      temperature: 0.7,
      maxTokens: 2048,
      defaultFlag: 'NOT_DEFAULT',
      enableFlag: 'ENABLE',
    });
    for (const leaked of [
      'providerName',
      'tenantId',
      'creatorId',
      'createTime',
      'operateTime',
      'lastCheckStatus',
      'lastCheckLatencyMs',
    ]) {
      expect(body, `${leaked} must not be re-sent`).not.toHaveProperty(leaked);
    }
  });

  it('update on an unrecognizable list shape fails with a distinct error (F043)', async () => {
    modelConfigListResponse = { ok: true, weird: true };
    await expect(
      run(['model', 'update', '10', '--label', 'X', '--format', 'json']),
    ).rejects.toMatchObject({
      name: 'ApiError',
      message: expect.stringMatching(/unrecognized shape/u),
    });
    expect(
      postCalls().filter((call) => call.url.endsWith('/model/config/update')),
    ).toHaveLength(0);
  });

  it('update of a missing id still reports not found without issuing an update', async () => {
    await expect(
      run(['model', 'update', '77', '--label', 'X', '--format', 'json']),
    ).rejects.toMatchObject({ name: 'ApiError', message: 'Model config 77 not found' });
    expect(
      postCalls().filter((call) => call.url.endsWith('/model/config/update')),
    ).toHaveLength(0);
  });
});
