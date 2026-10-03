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

import {mount} from '@vue/test-utils';

import {createElButtonStub} from '../setup/stubs/element-plus';
import {describe, expect, it} from 'vitest';

import ActionApprovalCard from '@/components/agentic/cards/ActionApprovalCard.vue';
import AttachmentCard from '@/components/agentic/cards/AttachmentCard.vue';
import ChatCardShell from '@/components/agentic/cards/ChatCardShell.vue';
import NoticeBar from '@/components/agentic/cards/NoticeBar.vue';
import QuoteCard from '@/components/agentic/cards/QuoteCard.vue';
import StatTile from '@/components/agentic/cards/StatTile.vue';
import ToolStepCard from '@/components/agentic/cards/ToolStepCard.vue';
import i18n from '@/config/i18n';
import type {AgenticAction} from '@/config/types';

const global = {
  plugins: [i18n],
  stubs: {
    ElIcon: true,
    ElTag: {template: '<span class="el-tag-stub"><slot /></span>'},
    ElButton: createElButtonStub(),
  },
};

describe('agentic card family', () => {
  it('renders the shell title, status and action slots in the platform card grammar', () => {
    const wrapper = mount(ChatCardShell, {
      global,
      props: {title: 'Tool chain', tone: 'warn', active: true},
      slots: {default: '<p class="body-slot">body</p>', status: '<i class="status-slot"/>', actions: '<i class="action-slot"/>'},
    });
    expect(wrapper.find('.chat-card').classes()).toContain('chat-card--warn');
    expect(wrapper.find('.chat-card').classes()).toContain('is-active');
    expect(wrapper.find('.chat-card__title').text()).toBe('Tool chain');
    expect(wrapper.find('.body-slot').exists()).toBe(true);
    expect(wrapper.find('.status-slot').exists()).toBe(true);
    expect(wrapper.find('.action-slot').exists()).toBe(true);
  });

  it('stacks stat labels by default and rows them on demand', () => {
    const stacked = mount(StatTile, {global, props: {label: 'Input', value: '87'}});
    expect(stacked.find('.chat-stat').text()).toContain('Input');
    expect(stacked.find('strong').text()).toBe('87');
    expect(stacked.find('.chat-stat--row').exists()).toBe(false);
    const row = mount(StatTile, {global, props: {label: 'Output', value: 2, layout: 'row'}});
    expect(row.find('.chat-stat--row').exists()).toBe(true);
  });

  it('renders the notice tone class and its actions slot', () => {
    const wrapper = mount(NoticeBar, {
      global,
      props: {tone: 'danger'},
      slots: {default: 'load failed', actions: '<button class="retry-slot">retry</button>'},
    });
    expect(wrapper.find('.chat-notice').classes()).toContain('chat-notice--danger');
    expect(wrapper.text()).toContain('load failed');
    expect(wrapper.find('.retry-slot').exists()).toBe(true);
  });

  it('renders quote text and emits remove only when removable', () => {
    const locked = mount(QuoteCard, {global, props: {label: 'quote', text: 'quoted body'}});
    expect(locked.find('.chat-quote__close').exists()).toBe(false);
    expect(locked.find('.chat-quote__text').text()).toBe('quoted body');

    const removable = mount(QuoteCard, {
      global,
      props: {label: 'quote', text: 'body', tone: 'on-primary', removable: true},
    });
    expect(removable.find('.chat-quote').classes()).toContain('chat-quote--on-primary');
    removable.find('.chat-quote__close').trigger('click');
    expect(removable.emitted('remove')).toHaveLength(1);
  });

  it('surfaces the tool step status badge and meta text', () => {
    const wrapper = mount(ToolStepCard, {
      global,
      props: {
        step: {id: 's1', index: 2, label: 'searchDevices', detail: 'loaded', meta: 'success · ok', status: 'failed'},
      },
    });
    expect(wrapper.find('.chat-tool-step').classes()).toContain('chat-tool-step--failed');
    expect(wrapper.find('.chat-tool-step__index').text()).toBe('2');
    expect(wrapper.text()).toContain('searchDevices');
    expect(wrapper.text()).toContain('loaded');
    expect(wrapper.text()).toContain('success · ok');
    expect(wrapper.find('.el-tag-stub').text()).toMatch(/失败|Failed/);
  });

  it('renders action payload rows and emits confirm and reject with the id', async () => {
    const action: AgenticAction = {
      actionId: 'a-1',
      conversationId: 'c-1',
      actionType: 'POINT_WRITE',
      title: 'Write point',
      description: 'set 35.4',
      payload: {from: '30', to: '35.4', empty: '', nested: {k: 1}},
      status: 0,
      expireTime: '2026-10-01 00:00:00',
    };
    const wrapper = mount(ActionApprovalCard, {global, props: {action}});
    // writes read louder: POINT_WRITE is the danger tone
    expect(wrapper.find('.chat-card').classes()).toContain('chat-card--danger');
    const text = wrapper.text();
    expect(text).toContain('Write point');
    expect(text).toContain('from');
    expect(text).toContain('35.4');
    expect(text).toContain('{"k":1}'); // nested payload stringified, never raw crash
    expect(text).not.toContain('empty'); // blank payload keys are dropped

    const buttons = wrapper.findAll('button');
    await buttons[0].trigger('click');
    await buttons[1].trigger('click');
    expect(wrapper.emitted('confirm')![0]).toEqual(['a-1']);
    expect(wrapper.emitted('reject')![0]).toEqual(['a-1']);

    // locked state disables the actions
    const lockedBox = mount(ActionApprovalCard, {global, props: {action, locked: true}});
    for (const b of lockedBox.findAll('button')) expect(b.attributes('disabled')).not.toBeUndefined();
  });

  it('shows attachment size, pending state and emits remove', async () => {
    const pending = mount(AttachmentCard, {
      global,
      props: {name: 'report.csv', size: 2048, removable: true},
    });
    expect(pending.text()).toContain('report.csv');
    expect(pending.text()).toContain('2.0 KB');
    expect(pending.text()).toMatch(/待上传|Pending/);
    pending.find('.chat-attachment__remove').trigger('click');
    expect(pending.emitted('remove')).toHaveLength(1);

    const done = mount(AttachmentCard, {global, props: {name: 'a.txt', size: 10, pending: false}});
    expect(done.text()).toContain('10 B');
    expect(done.text()).toMatch(/已就绪|Ready/);
    expect(done.find('.chat-attachment__remove').exists()).toBe(false);
  });
});
