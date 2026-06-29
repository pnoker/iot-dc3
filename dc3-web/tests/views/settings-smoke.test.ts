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

import {flushPromises} from '@vue/test-utils';
import {describe, expect, it, vi} from 'vitest';
import type {Component} from 'vue';

import {mountListPage} from './_helpers';

// Intercept the single HTTP chokepoint every settings API funnels through so
// no real network call fires during the smoke mount. `data` is a hybrid empty
// value: an array (so dashboard endpoints that `.map`/`.length` over it stay
// happy) that also carries the page-result keys (`records`/`total`) list pages
// read — one stub satisfies both response shapes without per-endpoint mocking.
vi.mock('@/api/common', () => {
  const empty = () => Object.assign([] as unknown[], {records: [], total: 0, rows: [], list: [], children: []});
  const ok = () => Promise.resolve({data: empty()});
  return {httpGet: ok, httpPost: ok, crudAdd: ok, crudUpdate: ok, crudDelete: ok, crudGetById: ok, crudList: ok};
});

vi.mock('@/utils/notificationUtil', () => ({
  failMessage: vi.fn(),
  successMessage: vi.fn(),
  warnMessage: vi.fn(),
  infoMessage: vi.fn(),
}));

// In the real build, unplugin-vue-components auto-registers every SFC under
// src/components (locally, per consuming SFC). vitest has no such plugin, so a
// page referencing e.g. <external-link> would fail to resolve and emit a Vue
// warning the strict handler promotes to a throw — a false negative that has
// nothing to do with the page's own runtime health. Passthrough-stub every
// local component here to mirror that auto-registration. (Missing registration
// in SEPARATED-mode pages is a real bug the unplugin can't fix; that class is
// covered by tests/guardrails/separated-mode-components.test.ts, not here.)
const HELPER_OWNED = new Set(['ToolCard', 'BlankCard', 'SkeletonCard']);
const localComponentStubs: Record<string, Component> = {};
for (const p of Object.keys(import.meta.glob('@/components/**/*.vue'))) {
  const name = p
    .split('/')
    .pop()!
    .replace(/\.vue$/, '');
  if (!HELPER_OWNED.has(name)) {
    localComponentStubs[name] = {template: '<div class="local-component-stub"><slot /></div>'};
  }
}

interface SmokePage {
  name: string;
  loader: () => Promise<{default: Component}>;
  props?: Record<string, unknown>;
}

// Every settings route-entry page, grouped to mirror the 8-section sidebar.
// Mounting each under the shared scaffolding is the regression gate that
// catches "opens with an error" at test time instead of at runtime.
const PAGES: SmokePage[] = [
  // identity
  {name: 'User', loader: () => import('@/views/settings/user/User.vue')},
  {name: 'Principal', loader: () => import('@/views/settings/principal/Principal.vue')},
  {name: 'TenantMembership', loader: () => import('@/views/settings/tenantMembership/TenantMembership.vue')},
  {name: 'LocalCredential', loader: () => import('@/views/settings/localCredential/LocalCredential.vue')},
  {name: 'ServiceAccount', loader: () => import('@/views/settings/serviceAccount/ServiceAccount.vue')},
  // access control
  {name: 'Role', loader: () => import('@/views/settings/role/Role.vue')},
  {name: 'RolePrincipalBind', loader: () => import('@/views/settings/rolePrincipalBind/RolePrincipalBind.vue')},
  {name: 'Resource', loader: () => import('@/views/settings/resource/Resource.vue')},
  {name: 'Api', loader: () => import('@/views/settings/api/Api.vue')},
  {name: 'Menu', loader: () => import('@/views/settings/menu/Menu.vue')},
  // model
  {name: 'ModelConfig', loader: () => import('@/views/settings/agentic/AgenticSettings.vue')},
  {name: 'ModelProvider', loader: () => import('@/views/settings/agentic/ProviderSettings.vue')},
  // alarm
  {name: 'AlarmRule', loader: () => import('@/views/settings/alarm/AlarmNotify.vue'), props: {entity: 'rule'}},
  {name: 'AlarmNotify', loader: () => import('@/views/settings/alarm/AlarmNotify.vue'), props: {entity: 'notify'}},
  {
    name: 'AlarmMessage',
    loader: () => import('@/views/settings/alarm/AlarmNotify.vue'),
    props: {entity: 'message'},
  },
  {
    name: 'AlarmChannel',
    loader: () => import('@/views/settings/alarm/AlarmNotify.vue'),
    props: {entity: 'channel'},
  },
  {name: 'AlarmBind', loader: () => import('@/views/settings/alarm/AlarmNotify.vue'), props: {entity: 'bind'}},
  {name: 'AlarmState', loader: () => import('@/views/settings/alarm/AlarmNotify.vue'), props: {entity: 'state'}},
  {
    name: 'AlarmHistory',
    loader: () => import('@/views/settings/alarm/AlarmNotify.vue'),
    props: {entity: 'history'},
  },
  {name: 'AlarmOverview', loader: () => import('@/views/settings/alarm/Overview.vue')},
  {name: 'DriverAlarm', loader: () => import('@/views/settings/alarm/DriverEvent.vue')},
  {name: 'DeviceAlarm', loader: () => import('@/views/settings/alarm/DeviceEvent.vue')},
  {name: 'PointAlarm', loader: () => import('@/views/settings/alarm/PointEvent.vue')},
  // event & command
  {name: 'EventHistory', loader: () => import('@/views/settings/event/EventHistory.vue')},
  {name: 'CommandHistory', loader: () => import('@/views/settings/command/CommandHistory.vue')},
  // audit
  {name: 'IdentityAudit', loader: () => import('@/views/settings/identityAudit/IdentityAudit.vue')},
  {name: 'McpAudit', loader: () => import('@/views/settings/mcpAudit/McpAudit.vue')},
  // integration
  {name: 'McpServer', loader: () => import('@/views/settings/mcp/McpServer.vue')},
  {name: 'McpConnection', loader: () => import('@/views/settings/mcp/McpConnection.vue')},
  {name: 'McpClient', loader: () => import('@/views/settings/mcp/McpClient.vue')},
  {name: 'McpTool', loader: () => import('@/views/settings/mcp/McpTool.vue')},
  // system
  {name: 'Group', loader: () => import('@/views/settings/group/Group.vue')},
  {name: 'Label', loader: () => import('@/views/settings/label/Label.vue')},
  {name: 'About', loader: () => import('@/views/settings/about/About.vue')},
];

describe('Settings pages smoke mount', () => {
  for (const page of PAGES) {
    it(`mounts ${page.name} without error`, async () => {
      const component = (await page.loader()).default;
      const wrapper = await mountListPage({component, props: page.props, stubs: localComponentStubs});
      await flushPromises();
      expect(wrapper.exists()).toBe(true);
    });
  }
});
