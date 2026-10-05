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
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Command } from 'commander';
import { registerProviderCommand } from '../src/commands/provider.js';
import { registerModelCommand } from '../src/commands/model.js';

const fetchCalls: Array<{ url: string; init: RequestInit }> = [];

vi.mock('../src/core/client.js', () => ({
  dc3Client: {
    get: vi.fn(async (url: string) => {
      fetchCalls.push({ url, init: { method: 'GET' } });
      if (url.includes('provider/list')) {
        return [
          { id: '1', name: 'DeepSeek', baseUrl: 'https://api.deepseek.com', providerType: 'OPENAI_COMPATIBLE' },
        ];
      }
      if (url.includes('model/list')) {
        return [{ model: 'deepseek-chat', label: 'DeepSeek Chat' }];
      }
      if (url.includes('model/config/list')) {
        return [
          { id: '10', model: 'deepseek-chat', label: 'DeepSeek Chat', providerId: '1', temperature: 0.7, maxTokens: 2048 },
        ];
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
    await program.parseAsync(args, { from: 'user' });
  } finally {
    (process.exit as unknown as ReturnType<typeof vi.spyOn>).mockRestore?.();
    (process.stdout.write as ReturnType<typeof vi.spyOn>).mockRestore();
  }
};

beforeEach(() => {
  fetchCalls.length = 0;
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

  it('check without --id or --base-url exits with error', async () => {
    const program = new Command();
    registerProviderCommand(program);
    program.exitOverride();
    vi.spyOn(process, 'exit').mockImplementation((() => undefined) as never);
    vi.spyOn(process.stdout, 'write').mockImplementation(() => true);
    try {
      await program.parseAsync(['provider', 'check', '--format', 'json'], { from: 'user' });
    } finally {
      (process.stdout.write as ReturnType<typeof vi.spyOn>).mockRestore();
    }
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
});
