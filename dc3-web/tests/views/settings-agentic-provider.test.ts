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

import {mountListPage} from './_helpers';

const agenticMocks = vi.hoisted(() => ({
  addAgenticProvider: vi.fn(() => Promise.resolve({data: true})),
  deleteAgenticProvider: vi.fn(() => Promise.resolve({data: true})),
  listAgenticProviders: vi.fn(() => Promise.resolve({data: []})),
  updateAgenticProvider: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/agentic', () => agenticMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('ProviderSettings view', () => {
  it('loads providers on mount via usePagedList', async () => {
    const ProviderSettings = (await import('@/views/settings/agentic/ProviderSettings.vue')).default;
    await mountListPage({
      component: ProviderSettings,
      stubs: {
        providerTool: {template: '<div />'},
        providerEditForm: {template: '<div />'},
        modelConfigTool: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(agenticMocks.listAgenticProviders).toHaveBeenCalledTimes(1);
  });
});
