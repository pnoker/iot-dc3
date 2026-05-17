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

import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { defineComponent, h } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AgenticAssistant from '@/components/agentic/AgenticAssistant.vue';
import i18n from '@/config/i18n';
import type { AgenticMessage } from '@/config/types';
import { useAgenticStore } from '@/store';

vi.mock('vue-element-plus-x/styles/index.css', () => ({}));
vi.mock('vue-element-plus-x', () => ({
  Prompts: { template: '<div class="prompts-stub" />' },
  Welcome: { template: '<div class="welcome-stub" />' },
}));

const PassthroughStub = defineComponent({
  name: 'PassthroughStub',
  setup(_, { slots }) {
    return () => h('span', slots.default?.());
  },
});

const ButtonStub = defineComponent({
  name: 'ElButton',
  props: ['disabled'],
  setup(props, { slots }) {
    return () => h('button', { disabled: props.disabled, type: 'button' }, slots.default?.());
  },
});

function seedAssistant(messages: AgenticMessage[]) {
  const store = useAgenticStore();
  store.visible = true;
  store.loading = false;
  store.streaming = messages.some((message) => message.streaming);
  store.activeConversationId = 'conversation-ui-test';
  store.sessions = [{ conversationId: 'conversation-ui-test', title: 'Reasoning UI' }];
  store.models = [
    {
      model: 'reasoning-model',
      label: 'Reasoning Model',
      stream: true,
      toolCall: true,
      vision: false,
      reasoning: true,
    },
  ];
  store.selectedModel = 'reasoning-model';
  store.reasoningEnabled = true;
  store.messagesByConversation = { 'conversation-ui-test': messages };
  store.attachmentsByConversation = { 'conversation-ui-test': [] };
  store.pendingAttachmentIdsByConversation = { 'conversation-ui-test': [] };
  store.pendingActionsByConversation = { 'conversation-ui-test': [] };
  store.traceEventsByConversation = { 'conversation-ui-test': [] };
}

function mountAssistant() {
  return mount(AgenticAssistant, {
    global: {
      plugins: [i18n],
      directives: {
        loading: () => undefined,
      },
      stubs: {
        ChatDotRound: PassthroughStub,
        ChatLineSquare: PassthroughStub,
        Check: PassthroughStub,
        CircleClose: PassthroughStub,
        Clock: PassthroughStub,
        Close: PassthroughStub,
        Cpu: PassthroughStub,
        Delete: PassthroughStub,
        Document: PassthroughStub,
        DocumentCopy: PassthroughStub,
        EditPen: PassthroughStub,
        ElButton: ButtonStub,
        ElDropdown: { template: '<span><slot /><slot name="dropdown" /></span>' },
        ElDropdownItem: { template: '<span><slot /></span>' },
        ElDropdownMenu: { template: '<span><slot /></span>' },
        ElIcon: PassthroughStub,
        ElInput: { template: '<textarea />' },
        ElInputNumber: { template: '<input />' },
        ElOption: { template: '<option />' },
        ElPopover: { template: '<span><slot name="reference" /><slot /></span>' },
        ElSelect: { template: '<select><slot /></select>' },
        ElSlider: { template: '<input />' },
        ElSwitch: { template: '<input type="checkbox" />' },
        ElTag: { template: '<span><slot /></span>' },
        ElTooltip: PassthroughStub,
        Lightning: PassthroughStub,
        Paperclip: PassthroughStub,
        Plus: PassthroughStub,
        Promotion: PassthroughStub,
        RenderedAssistantMessage: {
          props: ['content'],
          template: '<div class="rendered-assistant-message">{{ content }}</div>',
        },
        Setting: PassthroughStub,
        VideoPause: PassthroughStub,
        Warning: PassthroughStub,
      },
    },
  });
}

describe('AgenticAssistant', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
  });

  it('opens the reasoning panel while an assistant answer is streaming', () => {
    seedAssistant([
      {
        id: 'assistant-1',
        role: 'assistant',
        content: '',
        reasoning: '正在确认当前租户上下文，并准备查询设备列表。',
        streaming: true,
      },
    ]);

    const wrapper = mountAssistant();
    const panel = wrapper.find('.agentic-reasoning-panel');

    expect(panel.exists()).toBe(true);
    expect(panel.attributes('open')).toBeDefined();
    expect(wrapper.find('.agentic-reasoning-panel__text').text()).toContain('正在确认当前租户上下文');
    expect(wrapper.find('.agentic-live-thinking').exists()).toBe(false);
  });

  it('collapses completed reasoning by default and keeps technical details separate', () => {
    seedAssistant([
      {
        id: 'assistant-1',
        role: 'assistant',
        content: '当前共有 3 台设备在线。',
        reasoning: '先读取租户上下文，再调用设备查询工具。',
        streaming: false,
        contentExt: {
          reasoning: true,
          tools: ['searchDevices'],
          tokens: { input: 40, output: 20 },
        },
      },
    ]);

    const wrapper = mountAssistant();
    const panel = wrapper.find('.agentic-reasoning-panel');

    expect(panel.exists()).toBe(true);
    expect(panel.attributes('open')).toBeUndefined();
    expect(wrapper.find('.agentic-reasoning-panel__text').text()).toContain('先读取租户上下文');
    expect(wrapper.find('.agentic-details').text()).not.toContain('先读取租户上下文');
    expect(wrapper.find('.agentic-details').text()).toContain('searchDevices');
  });
});
