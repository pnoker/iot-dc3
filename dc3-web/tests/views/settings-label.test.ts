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

const labelMocks = vi.hoisted(() => ({
  addLabel: vi.fn(() => Promise.resolve({data: true})),
  deleteLabel: vi.fn(() => Promise.resolve({data: true})),
  listLabel: vi.fn(() => Promise.resolve({data: {records: [{id: 'l-1', labelName: 'PROD'}], total: 1}})),
  updateLabel: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/label', () => labelMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('SettingsLabel view', () => {
  it('lists labels on mount', async () => {
    const Label = (await import('@/views/settings/label/Label.vue')).default;
    await mountListPage({
      component: Label,
      stubs: {labelTool: {template: '<div />'}, labelAddForm: {template: '<div />'}},
    });
    await flushPromises();
    expect(labelMocks.listLabel).toHaveBeenCalledTimes(1);
  });
});
