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

import {readdirSync, statSync} from 'node:fs';
import {join, relative} from 'node:path';

import {afterEach, describe, expect, it, vi} from 'vitest';

import * as agenticApi from '@/api/agentic';
import * as alarmApi from '@/api/alarm';
import * as authApi from '@/api/api';
import * as attributeApi from '@/api/attribute';
import * as alertApi from '@/api/dashboard/alert';
import * as statsApi from '@/api/dashboard/stats';
import * as systemApi from '@/api/dashboard/system';
import * as topologyApi from '@/api/dashboard/topology';
import * as commandApi from '@/api/command';
import * as deviceApi from '@/api/device';
import * as dictionaryApi from '@/api/dictionary';
import * as driverApi from '@/api/driver';
import * as eventApi from '@/api/event';
import * as groupApi from '@/api/group';
import * as infoApi from '@/api/info';
import * as labelApi from '@/api/label';
import * as menuApi from '@/api/menu';
import * as mcpApi from '@/api/mcp';
import * as pointApi from '@/api/point';
import * as profileApi from '@/api/profile';
import * as resourceApi from '@/api/resource';
import * as roleApi from '@/api/role';
import * as rolePrincipalBindApi from '@/api/rolePrincipalBind';
import * as roleResourceBindApi from '@/api/roleResourceBind';
import * as tokenApi from '@/api/token';
import * as userApi from '@/api/user';
import * as identityAuditApi from '@/api/identityAudit';
import * as localCredentialApi from '@/api/localCredential';
import * as principalApi from '@/api/principal';
import * as serviceAccountApi from '@/api/serviceAccount';
import * as tenantMembershipApi from '@/api/tenantMembership';
import {AUTH_HEADERS} from '@/config/constant/common';
import type {AgenticChatCompletionRequest} from '@/config/types';
import {setStorage} from '@/utils/storageUtil';

// Builds a minimal-but-valid streaming request. Only `messages`, `stream`,
// `conversationId` are required by the contract — everything else is optional
// and irrelevant to the wire-format assertions below.
const streamRequest = (overrides: Partial<AgenticChatCompletionRequest> = {}): AgenticChatCompletionRequest => ({
  messages: [{role: 'user', content: 'hello'}],
  conversationId: 'conversation-test',
  stream: true,
  ...overrides,
});

const apiSpies = vi.hoisted(() => ({
  httpGet: vi.fn(() => Promise.resolve({ok: true, data: null})),
  httpPost: vi.fn(() => Promise.resolve({ok: true, data: null})),
  request: vi.fn(() => Promise.resolve({ok: true, data: null})),
}));

vi.mock('@/api/common', () => ({
  httpGet: apiSpies.httpGet,
  httpPost: apiSpies.httpPost,
  crudAdd: (base: string, payload: unknown) => apiSpies.httpPost(`${base}/add`, payload),
  crudUpdate: (base: string, payload: unknown) => apiSpies.httpPost(`${base}/update`, payload),
  crudDelete: (base: string, id: string) => apiSpies.httpPost(`${base}/delete`, undefined, {params: {id}}),
  crudGetById: (base: string, id: string) => apiSpies.httpGet(`${base}/get_by_id`, {params: {id}}),
  crudList: (base: string, query: unknown) => apiSpies.httpPost(`${base}/list`, query),
}));

vi.mock('@/config/axios', () => ({
  default: apiSpies.request,
}));

type ApiModule = Record<string, unknown>;
type ApiFunction = (...args: unknown[]) => unknown;
type TransportCall = { transport: 'httpGet' | 'httpPost' | 'request'; args: unknown[] };

const modules: Record<string, ApiModule> = {
  agentic: agenticApi,
  alarm: alarmApi,
  api: authApi,
  attribute: attributeApi,
  alert: alertApi,
  stats: statsApi,
  system: systemApi,
  topology: topologyApi,
  command: commandApi,
  device: deviceApi,
  dictionary: dictionaryApi,
  driver: driverApi,
  event: eventApi,
  group: groupApi,
  info: infoApi,
  label: labelApi,
  menu: menuApi,
  mcp: mcpApi,
  point: pointApi,
  profile: profileApi,
  resource: resourceApi,
  role: roleApi,
  rolePrincipalBind: rolePrincipalBindApi,
  roleResourceBind: roleResourceBindApi,
  token: tokenApi,
  user: userApi,
  identityAudit: identityAuditApi,
  localCredential: localCredentialApi,
  principal: principalApi,
  serviceAccount: serviceAccountApi,
  tenantMembership: tenantMembershipApi,
};

const coveredApiSourceFiles = new Set([
  'agentic',
  'alarm',
  'api',
  'attribute',
  'command',
  'dashboard/alert',
  'dashboard/stats',
  'dashboard/system',
  'dashboard/topology',
  'device',
  'dictionary',
  'driver',
  'event',
  'group',
  'info',
  'label',
  'menu',
  'mcp',
  'point',
  'profile',
  'resource',
  'role',
  'rolePrincipalBind',
  'roleResourceBind',
  'token',
  'user',
  'identityAudit',
  'localCredential',
  'principal',
  'serviceAccount',
  'tenantMembership',
]);

const apiSourceFileExclusions = new Set(['common', 'dashboard/index']);
const excludedExports = new Set(['streamAgenticChatCompletion', 'completeAgenticChatCompletion']);

const pageQuery = {
  page: {current: 2, size: 20},
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

// Explicit registry — preferred path. Add new wrappers HERE rather than
// extending the heuristic fallback below; the heuristic exists only for
// legacy modules whose call shapes were already well-covered when the
// registry was introduced. New entries should pass the exact arguments
// the function expects, in declaration order.
const sampleArgsRegistry: Record<string, unknown[]> = {
  // Multi-arg or non-pattern shapes that can't be derived from the name.
  updateAgenticSession: ['conversation-1', {title: 'Renamed session'}],
  uploadAgenticAttachment: ['conversation-1', new File(['demo'], 'demo.txt', {type: 'text/plain'})],
  alertConfirm: ['driver', 'alert-1'],
  alertUnconfirm: ['driver', 'alert-1'],
  alertBulkConfirm: [[{source: 'driver', id: 'alert-1'}], true],
  listPointValueHistory: ['1001', '2002', 30],
  listRoleByPrincipalId: ['principal-1'],
  replaceMcpConnectionTools: ['connection-1', ['tool-1', 'tool-2']],
  listMcpAudit: [{principalId: 'principal-1', toolId: 'tool-1', status: 'ACTIVE', riskLevel: 'LOW', limit: 20}],
  listMcpConnectionTool: ['connection-1'],
  registerMcpClient: [payload],
  revokeMcpConnection: ['connection-1'],
  getDriverInfoByDeviceIdAndAttributeId: ['device-1', 'attribute-1'],
  getPointInfoByDeviceIdAndPointId: ['device-1', 'point-1'],
  listCommandInfoByDeviceIdAndCommandId: ['device-1', 'command-1'],
  listEventInfoByDeviceIdAndEventId: ['device-1', 'event-1'],
  getCommandHistoryByRecordId: ['record-1'],
  getEventHistoryByRecordId: ['record-1'],
  // Auth wrappers whose enable/disable/reset/check verbs don't match the heuristic.
  listIdentityAudit: [
    {
      principalId: 'principal-1',
      action: 'CREATE',
      resourceType: 'role',
      resourceId: 'res-1',
      status: 'SUCCESS',
      limit: 20,
    },
  ],
  resetLocalCredentialPassword: ['id-1', 'secret-1'],
  checkLoginNameAvailable: ['login-1'],
  enablePrincipal: ['id-1'],
  disablePrincipal: ['id-1'],
  enableServiceAccount: ['id-1'],
  disableServiceAccount: ['id-1'],
  // Page-query callers whose names don't match the heuristic prefixes.
  getAgenticSessions: [pageQuery],
  alertPage: [pageQuery],
  // Array-id callers without `ByIds` suffix.
  getPointUnit: [['id-1', 'id-2']],
};

function sampleArgs(name: string): unknown[] {
  // 1. Explicit registry wins.
  if (Object.prototype.hasOwnProperty.call(sampleArgsRegistry, name)) {
    return sampleArgsRegistry[name];
  }

  // 2. Heuristic fallback — most-specific first to avoid short-circuit
  //    ambiguity (e.g. `listFooByBarId` matches both `/^list[A-Z]/` and
  //    `/By[A-Z].*Id$/`; the byId case must win because it takes a string,
  //    not a page query).
  if (/ByIds$/.test(name)) return [['id-1', 'id-2']];
  if (/ListBy|By[A-Z].*Id$|ByName$/.test(name)) return ['id-1'];
  if (/Tree$/.test(name)) return [pageQuery];
  if (/^list[A-Z]/.test(name)) return [pageQuery];
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
    ...apiSpies.httpGet.mock.calls.map((args) => ({transport: 'httpGet' as const, args})),
    ...apiSpies.httpPost.mock.calls.map((args) => ({transport: 'httpPost' as const, args})),
    ...apiSpies.request.mock.calls.map((args) => ({transport: 'request' as const, args})),
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

  const segments = url.split('/').filter(Boolean);
  const allowsTrailingPathParam = /^api\/v3\/data\/(?:command_history|event_history)\/[^/]+$/.test(url);
  const staticSegments = allowsTrailingPathParam ? segments.slice(0, -1) : segments;

  for (const segment of staticSegments) {
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
    setStorage(AUTH_HEADERS.TOKEN, {salt: 'salt', token: 'token'});

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
        headers: {'Content-Type': 'text/event-stream'},
      });
    });

    vi.stubGlobal('fetch', fetchMock);

    const onDelta = vi.fn();
    const onEvent = vi.fn();
    const onReasoning = vi.fn();
    const onDone = vi.fn();

    await agenticApi.streamAgenticChatCompletion(streamRequest(), {
      onDelta,
      onEvent,
      onReasoning,
      onDone,
    });

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
          [AUTH_HEADERS.TOKEN]: JSON.stringify({salt: 'salt', token: 'token'}),
        }),
      })
    );
    expect(onEvent).toHaveBeenCalledWith(
      expect.objectContaining({type: 'tool', title: 'Tool call', phase: 'start', status: 'running'})
    );
    expect(onEvent).toHaveBeenCalledWith(
      expect.objectContaining({type: 'tool', title: 'Device page loaded', phase: 'result', status: 'success'})
    );
    expect(onReasoning.mock.calls.map(([chunk]) => chunk)).toEqual(['let me think ', 'about this']);
    expect(onDelta.mock.calls.map(([chunk]) => chunk)).toEqual(['hello ', 'world']);
    expect(onDone).toHaveBeenCalledTimes(1);
  });

  it('normalizes failed stream responses and redirects on 401', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('expired', {status: 401}))
    );

    const onError = vi.fn();

    await expect(agenticApi.streamAgenticChatCompletion(streamRequest({messages: []}), {onError})).rejects.toThrow(
      'expired'
    );

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
          headers: {'Content-Type': 'text/event-stream'},
        });
      })
    );

    const onEvent = vi.fn();
    const onFinish = vi.fn();
    const onDone = vi.fn();
    const onError = vi.fn();

    await agenticApi.streamAgenticChatCompletion(streamRequest(), {onEvent, onFinish, onDone, onError});

    expect(onEvent).toHaveBeenCalledWith(expect.objectContaining({type: 'error', title: 'Request failed'}));
    expect(onFinish).toHaveBeenCalledWith('error');
    expect(onDone).toHaveBeenCalledTimes(1);
    expect(onError).not.toHaveBeenCalled();
  });
});
