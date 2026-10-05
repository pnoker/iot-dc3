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

import {flushPromises} from '@vue/test-utils';
import {describe, expect, it} from 'vitest';

import {mountListPage} from './_helpers';

import type {AgenticProviderCheckResult} from '@/config/types';

const passResult: AgenticProviderCheckResult = {
  overall: 'PASS',
  l1: {status: 'PASS', latencyMs: 120, models: ['mock-model-a', 'mock-model-b']},
  l2: {status: 'PASS', latencyMs: 240, model: 'mock-model-a'},
  dimensions: ['CONNECTIVITY', 'AUTH', 'MODEL_VISIBLE', 'INFERENCE'],
  checkedAt: '2026-10-04T08:30:00',
};

const failResult: AgenticProviderCheckResult = {
  overall: 'FAIL',
  l1: {status: 'FAIL', latencyMs: 90, errorType: 'AUTH_FAILED', message: 'HTTP 401 from mock gateway', upstreamStatus: 401},
  l2: {status: 'SKIPPED', latencyMs: 0},
  dimensions: ['CONNECTIVITY'],
  checkedAt: '2026-10-04T08:30:00',
};

const mountDialog = async () => {
  const dialog = (await import('@/views/settings/agentic/check/ProviderCheckResultDialog.vue')).default;
  const wrapper = await mountListPage({component: dialog, skipForm: true});
  return wrapper;
};

const showResult = (wrapper: Awaited<ReturnType<typeof mountDialog>>, result: AgenticProviderCheckResult) => {
  // show() is exposed through defineExpose; the script-setup vm typing keeps
  // it out of the generic instance type, so narrow structurally (single cast).
  (wrapper.vm as {show: (result: AgenticProviderCheckResult) => void}).show(result);
};

describe('ProviderCheckResultDialog', () => {
  it('renders both probe levels and listed models for a passing check', async () => {
    const wrapper = await mountDialog();
    showResult(wrapper, passResult);
    await flushPromises();

    expect(wrapper.findAll('.check-level')).toHaveLength(2);
    expect(wrapper.find('.check-error-type').exists()).toBe(false);
    // Listed model ids surface in the collapsible L1 section.
    expect(wrapper.text()).toContain('mock-model-a');
    expect(wrapper.text()).toContain('mock-model-b');
  });

  it('renders the classified error and raw upstream message for a failing check', async () => {
    const wrapper = await mountDialog();
    showResult(wrapper, failResult);
    await flushPromises();

    expect(wrapper.find('.check-error-type').exists()).toBe(true);
    expect(wrapper.find('.check-error-hint').exists()).toBe(true);
    // The raw upstream error text stays inspectable for gateway troubleshooting.
    expect(wrapper.text()).toContain('HTTP 401 from mock gateway');
    expect(wrapper.text()).toContain('HTTP 401');
  });
});
