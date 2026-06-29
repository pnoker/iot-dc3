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
  addAgenticModelConfig: vi.fn(() => Promise.resolve({data: true})),
  deleteAgenticModelConfig: vi.fn(() => Promise.resolve({data: true})),
  listAgenticModelConfigs: vi.fn(() => Promise.resolve({data: []})),
  listAgenticProviders: vi.fn(() => Promise.resolve({data: []})),
  updateAgenticModelConfig: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/agentic', () => agenticMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('AgenticSettings view', () => {
  it('loads model configs on mount via usePagedList', async () => {
    const AgenticSettings = (await import('@/views/settings/agentic/AgenticSettings.vue')).default;
    await mountListPage({
      component: AgenticSettings,
      stubs: {
        modelConfigTool: {template: '<div />'},
        modelConfigEditForm: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(agenticMocks.listAgenticModelConfigs).toHaveBeenCalledTimes(1);
  });
});
