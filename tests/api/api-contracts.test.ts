/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

import { afterEach, describe, expect, it, vi } from 'vitest';

import * as agenticApi from '@/api/agentic';
import * as authApi from '@/api/api';
import * as attributeApi from '@/api/attribute';
import * as alertApi from '@/api/dashboard/alert';
import * as statsApi from '@/api/dashboard/stats';
import * as systemApi from '@/api/dashboard/system';
import * as topologyApi from '@/api/dashboard/topology';
import * as deviceApi from '@/api/device';
import * as dictionaryApi from '@/api/dictionary';
import * as driverApi from '@/api/driver';
import * as groupApi from '@/api/group';
import * as infoApi from '@/api/info';
import * as labelApi from '@/api/label';
import * as menuApi from '@/api/menu';
import * as pointApi from '@/api/point';
import * as profileApi from '@/api/profile';
import * as resourceApi from '@/api/resource';
import * as roleApi from '@/api/role';
import * as roleResourceBindApi from '@/api/roleResourceBind';
import * as roleUserBindApi from '@/api/roleUserBind';
import * as tokenApi from '@/api/token';
import * as userApi from '@/api/user';
import { AUTH_HEADERS } from '@/config/constant/common';
import { setStorage } from '@/utils/storageUtil';

const apiSpies = vi.hoisted(() => ({
  httpGet: vi.fn(() => Promise.resolve({ ok: true, data: null })),
  httpPost: vi.fn(() => Promise.resolve({ ok: true, data: null })),
  request: vi.fn(() => Promise.resolve({ ok: true, data: null })),
}));

vi.mock('@/api/common', () => ({
  httpGet: apiSpies.httpGet,
  httpPost: apiSpies.httpPost,
}));

vi.mock('@/config/axios', () => ({
  default: apiSpies.request,
}));

type ApiModule = Record<string, unknown>;
type ApiFunction = (...args: unknown[]) => unknown;
type TransportCall = { transport: 'httpGet' | 'httpPost' | 'request'; args: unknown[] };

const modules: Record<string, ApiModule> = {
  agentic: agenticApi,
  api: authApi,
  attribute: attributeApi,
  alert: alertApi,
  stats: statsApi,
  system: systemApi,
  topology: topologyApi,
  device: deviceApi,
  dictionary: dictionaryApi,
  driver: driverApi,
  group: groupApi,
  info: infoApi,
  label: labelApi,
  menu: menuApi,
  point: pointApi,
  profile: profileApi,
  resource: resourceApi,
  role: roleApi,
  roleResourceBind: roleResourceBindApi,
  roleUserBind: roleUserBindApi,
  token: tokenApi,
  user: userApi,
};

const coveredApiSourceFiles = new Set([
  'agentic',
  'api',
  'attribute',
  'dashboard/alert',
  'dashboard/stats',
  'dashboard/system',
  'dashboard/topology',
  'device',
  'dictionary',
  'driver',
  'group',
  'info',
  'label',
  'menu',
  'point',
  'profile',
  'resource',
  'role',
  'roleResourceBind',
  'roleUserBind',
  'token',
  'user',
]);

const apiSourceFileExclusions = new Set(['common', 'dashboard/index']);
const excludedExports = new Set(['streamAgenticChatCompletion', 'completeAgenticChatCompletion']);

const pageQuery = {
  page: { current: 2, size: 20 },
  keyword: 'demo',
};

const payload = {
  id: 'payload-1',
  name: 'demo',
  enableFlag: 'ENABLE',
};

function exportedFunctions(moduleApi: ApiModule) {
  return Object.entries(moduleApi).filter(
    ([name, value]) => typeof value === 'function' && !excludedExports.has(name)
  ) as Array<[string, ApiFunction]>;
}

function sampleArgs(name: string): unknown[] {
  if (name === 'updateAgenticSession') return ['conversation-1', { title: 'Renamed session' }];
  if (name === 'uploadAgenticAttachment')
    return ['conversation-1', new File(['demo'], 'demo.txt', { type: 'text/plain' })];
  if (name === 'alertConfirm' || name === 'alertUnconfirm') return ['driver', 'alert-1'];
  if (name === 'alertBulkConfirm') return [[{ source: 'driver', id: 'alert-1' }], true];
  if (name === 'getPointValueHistory') return [1001, 2002, 30];
  if (name === 'getRoleListByUserId') return ['user-1', 1000];
  if (name === 'getDriverInfoByDeviceIdAndAttributeId') return ['device-1', 'attribute-1'];
  if (name === 'getPointInfoByDeviceIdAndPointId') return ['device-1', 'point-1'];
  if (name === 'getAgenticSessions' || name === 'alertPage') return [pageQuery];
  if (/ByIds$/.test(name) || name === 'getPointUnit') return [['id-1', 'id-2']];
  if (/ListBy|By[A-Z].*Id$|ByName$/.test(name)) return ['id-1'];
  if (/Tree$/.test(name)) return [pageQuery];
  if (/List$|Dictionary$/.test(name)) return [pageQuery];
  if (/^(add|update|import|upload|read|write)/.test(name) || /Status$/.test(name)) return [payload];
  if (/^(delete|get|confirm|reject)/.test(name)) return ['id-1'];
  return [];
}

function walkApiSourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry);
    const stat = statSync(path);
    if (stat.isDirectory()) return walkApiSourceFiles(path);
    if (!entry.endsWith('.ts')) return [];

    return [relative(join(process.cwd(), 'src/api'), path).replace(/\.ts$/, '').replaceAll('\\', '/')];
  });
}

function resetTransportSpies() {
  apiSpies.httpGet.mockClear();
  apiSpies.httpPost.mockClear();
  apiSpies.request.mockClear();
}

function transportCalls(): TransportCall[] {
  return [
    ...apiSpies.httpGet.mock.calls.map((args) => ({ transport: 'httpGet' as const, args })),
    ...apiSpies.httpPost.mock.calls.map((args) => ({ transport: 'httpPost' as const, args })),
    ...apiSpies.request.mock.calls.map((args) => ({ transport: 'request' as const, args })),
  ];
}

function callUrl(call: TransportCall) {
  const firstArg = call.args[0];
  if (typeof firstArg === 'string') return firstArg;
  if (firstArg && typeof firstArg === 'object' && 'url' in firstArg) {
    return String((firstArg as { url?: unknown }).url);
  }
  return '';
}

function callConfig(call: TransportCall) {
  if (call.transport === 'request') {
    return call.args[0] as { params?: Record<string, unknown> };
  }
  if (call.transport === 'httpGet') {
    return call.args[1] as { params?: Record<string, unknown> } | undefined;
  }
  return call.args[2] as { params?: Record<string, unknown> } | undefined;
}

function expectStandardUrl(call: TransportCall) {
  const url = callUrl(call);
  expect(url).toMatch(/^api\/v3\//);
  expect(url).not.toContain('undefined');
  expect(url).not.toContain('?');

  for (const segment of url.split('/').filter(Boolean)) {
    expect(segment).not.toMatch(/[A-Z-]/);
    expect(segment).not.toMatch(/^\{.+\}$/);
    expect(segment).not.toMatch(/^(id|ids|name|code|service|[a-z0-9]+_id)$/);
  }
}

function expectStandardParams(call: TransportCall) {
  const params = callConfig(call)?.params;
  if (!params) return;

  for (const key of Object.keys(params)) {
    expect(key).not.toMatch(/[A-Z-]/);
  }
}

describe('API wrapper contracts', () => {
  afterEach(() => {
    resetTransportSpies();
  });

  it('includes every public API wrapper source file in the contract matrix', () => {
    const sourceFiles = walkApiSourceFiles(join(process.cwd(), 'src/api'))
      .filter((file) => !apiSourceFileExclusions.has(file))
      .sort();

    expect(sourceFiles).toEqual([...coveredApiSourceFiles].sort());
  });

  for (const [moduleName, moduleApi] of Object.entries(modules)) {
    describe(moduleName, () => {
      const entries = exportedFunctions(moduleApi);

      for (const [exportName, apiFunction] of entries) {
        it(`${exportName} maps to its transport contract`, async () => {
          await apiFunction(...sampleArgs(exportName));

          const calls = transportCalls();
          expect(calls).toHaveLength(1);
          expectStandardUrl(calls[0]);
          expectStandardParams(calls[0]);
          expect(calls[0]).toMatchSnapshot();
        });
      }

      it('has every export covered by the contract matrix', () => {
        expect(entries.map(([name]) => name).sort()).toMatchSnapshot();
      });
    });
  }
});

describe('agentic streaming contract', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('posts SSE chat requests with auth headers and emits parsed callbacks', async () => {
    setStorage(AUTH_HEADERS.TENANT, 'default');
    setStorage(AUTH_HEADERS.LOGIN, 'dc3');
    setStorage(AUTH_HEADERS.TOKEN, { salt: 'salt', token: 'token' });

    const fetchMock = vi.fn(async () => {
      const body = [
        'data: {"object":"agentic.event","type":"tool","title":"Tool call","detail":"device","name":"searchDevices","phase":"start","status":"running","created":1}',
        '',
        'data: {"object":"agentic.event","type":"tool","title":"Device page loaded","detail":"OK","name":"searchDevices","phase":"result","status":"success","code":"OK","created":2}',
        '',
        'data: {"choices":[{"delta":{"reasoning_content":"let me think "}}]}',
        '',
        'data: {"choices":[{"delta":{"reasoning_content":"about this"}}]}',
        '',
        'data: {"choices":[{"delta":{"content":"hello "}}]}',
        '',
        'data: {"choices":[{"delta":{"content":"world"}}]}',
        '',
        'data: [DONE]',
        '',
      ].join('\n');

      return new Response(body, {
        status: 200,
        headers: { 'Content-Type': 'text/event-stream' },
      });
    });

    vi.stubGlobal('fetch', fetchMock);

    const onDelta = vi.fn();
    const onEvent = vi.fn();
    const onReasoning = vi.fn();
    const onDone = vi.fn();

    await agenticApi.streamAgenticChatCompletion(
      {
        messages: [{ role: 'user', content: 'hello' }],
        conversationId: 'conversation-test',
        stream: true,
      } as never,
      {
        onDelta,
        onEvent,
        onReasoning,
        onDone,
      }
    );

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v3/agentic/chat/completions',
      expect.objectContaining({
        method: 'post',
        credentials: 'include',
        headers: expect.objectContaining({
          Accept: 'text/event-stream',
          'Content-Type': 'application/json',
          [AUTH_HEADERS.TENANT]: 'default',
          [AUTH_HEADERS.LOGIN]: 'dc3',
          [AUTH_HEADERS.TOKEN]: JSON.stringify({ salt: 'salt', token: 'token' }),
        }),
      })
    );
    expect(onEvent).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'tool', title: 'Tool call', phase: 'start', status: 'running' })
    );
    expect(onEvent).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'tool', title: 'Device page loaded', phase: 'result', status: 'success' })
    );
    expect(onReasoning.mock.calls.map(([chunk]) => chunk)).toEqual(['let me think ', 'about this']);
    expect(onDelta.mock.calls.map(([chunk]) => chunk)).toEqual(['hello ', 'world']);
    expect(onDone).toHaveBeenCalledTimes(1);
  });

  it('normalizes failed stream responses and redirects on 401', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('expired', { status: 401 }))
    );

    const onError = vi.fn();

    await expect(
      agenticApi.streamAgenticChatCompletion(
        { messages: [], conversationId: 'conversation-test', stream: true } as never,
        { onError }
      )
    ).rejects.toThrow('expired');

    expect(onError).toHaveBeenCalledWith(expect.any(Error));
    expect(window.location.hash).toBe('#/login');
  });

  it('keeps backend SSE error events as structured trace events', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        const body = [
          'data: {"object":"agentic.event","type":"error","title":"Request failed","detail":"backend failed","created":1}',
          '',
          'data: {"choices":[{"delta":{},"finish_reason":"error"}]}',
          '',
          'data: [DONE]',
          '',
        ].join('\n');

        return new Response(body, {
          status: 200,
          headers: { 'Content-Type': 'text/event-stream' },
        });
      })
    );

    const onEvent = vi.fn();
    const onFinish = vi.fn();
    const onDone = vi.fn();
    const onError = vi.fn();

    await agenticApi.streamAgenticChatCompletion(
      { messages: [{ role: 'user', content: 'hello' }], conversationId: 'conversation-test', stream: true } as never,
      { onEvent, onFinish, onDone, onError }
    );

    expect(onEvent).toHaveBeenCalledWith(expect.objectContaining({ type: 'error', title: 'Request failed' }));
    expect(onFinish).toHaveBeenCalledWith('error');
    expect(onDone).toHaveBeenCalledTimes(1);
    expect(onError).not.toHaveBeenCalled();
  });
});
