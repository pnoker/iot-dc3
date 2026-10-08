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

import {flushPromises, mount} from '@vue/test-utils';
import {createPinia, setActivePinia} from 'pinia';
import {defineComponent, h} from 'vue';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgenticAssistant from '@/components/agentic/AgenticAssistant.vue';
import i18n from '@/config/i18n';
import type {AgenticMessage} from '@/config/types';
import {useAgenticStore, useAppStore} from '@/store';

import {createElButtonStub, layoutStubs} from '../setup/stubs/element-plus';

vi.mock('vue-element-plus-x/styles/index.css', () => ({}));
vi.mock('vue-element-plus-x', () => ({
  Prompts: {template: '<div class="prompts-stub" />'},
  Welcome: {template: '<div class="welcome-stub" />'},
}));

const PassthroughStub = defineComponent({
  name: 'PassthroughStub',
  setup(_, {slots}) {
    return () => h('span', slots.default?.());
  },
});

const message = (id: string, role: AgenticMessage['role'], content: string, tools: string[] = []): AgenticMessage => ({
  id,
  role,
  content,
  contentExt: tools.length ? {tools} : {},
} as AgenticMessage);

// Same seeding escape hatch as agentic-assistant.test.ts — direct store state
// instead of four mocked API calls per test.
function seedWorkbench(messages: AgenticMessage[]) {
  const store = useAgenticStore();
  store.visible = true;
  store.workbenchExpanded = true;
  store.loading = false;
  store.streaming = false;
  store.activeConversationId = 'conversation-workbench';
  store.sessions = [
    {conversationId: 'conversation-workbench', title: 'Workbench session'},
    {conversationId: 'conversation-other', title: 'Other session'},
  ];
  store.models = [{model: 'm1', label: 'Model 1', stream: true, toolCall: true, vision: false, reasoning: true}];
  store.selectedModel = 'm1';
  store.messagesByConversation = {'conversation-workbench': messages};
  store.attachmentsByConversation = {'conversation-workbench': []};
  store.pendingAttachmentIdsByConversation = {'conversation-workbench': []};
  store.pendingActionsByConversation = {'conversation-workbench': []};
  store.traceEventsByConversation = {'conversation-workbench': []};
}

function mountWorkbench() {
  return mount(AgenticAssistant, {
    global: {
      plugins: [i18n],
      directives: {loading: () => undefined},
      stubs: {
        ...layoutStubs,
        ElButton: createElButtonStub(),
        ElDropdown: {template: '<span><slot /><slot name="dropdown" /></span>'},
        ElDropdownItem: {template: '<span><slot /></span>'},
        ElDropdownMenu: {template: '<span><slot /></span>'},
        ElIcon: PassthroughStub,
        ElPopover: {template: '<span><slot name="reference" /><slot /></span>'},
        ElScrollbar: {template: '<div><slot /></div>'},
        ElSlider: {template: '<input class="el-slider-stub" />'},
        Aim: PassthroughStub,
        CaretBottom: PassthroughStub,
        ChatDotRound: PassthroughStub,
        ChatLineSquare: PassthroughStub,
        Check: PassthroughStub,
        CircleCheck: PassthroughStub,
        CircleClose: PassthroughStub,
        Clock: PassthroughStub,
        Close: PassthroughStub,
        Cpu: PassthroughStub,
        DataAnalysis: PassthroughStub,
        Delete: PassthroughStub,
        Document: PassthroughStub,
        DocumentCopy: PassthroughStub,
        EditPen: PassthroughStub,
        Expand: PassthroughStub,
        Fold: PassthroughStub,
        FullScreen: PassthroughStub,
        Lightning: PassthroughStub,
        Loading: PassthroughStub,
        MagicStick: PassthroughStub,
        Paperclip: PassthroughStub,
        Plus: PassthroughStub,
        Promotion: PassthroughStub,
        Setting: PassthroughStub,
        Tickets: PassthroughStub,
        Tools: PassthroughStub,
        VideoPause: PassthroughStub,
        Warning: PassthroughStub,
        RenderedAssistantMessage: {
          props: ['content'],
          template: '<div class="rendered-assistant-message">{{ content }}</div>',
        },
      },
    },
  });
}

describe('AgenticAssistant workbench mode', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    // The global setup mock answers `matches: false` to every query, which
    // makes useBreakpoint fall back to xs (mobile). Workbench behaviour is
    // desktop-first, so answer `true` to min-width queries instead.
    Object.defineProperty(window, 'matchMedia', {
      configurable: true,
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: query.includes('min-width'),
        media: query,
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });
  });

  it('shows the sessions rail and the context panel only in workbench mode', async () => {
    seedWorkbench([message('m1', 'assistant', 'hello')]);
    const wrapper = mountWorkbench();

    expect(wrapper.find('.agentic-panel--expanded').exists()).toBe(true);
    expect(wrapper.find('.agentic-sessions').exists()).toBe(true);

    const store = useAgenticStore();
    store.workbenchExpanded = false;
    await flushPromises();
    expect(wrapper.find('.agentic-panel').exists()).toBe(true);
    expect(wrapper.find('.agentic-sessions').exists()).toBe(false);
    expect(wrapper.find('.agentic-context').exists()).toBe(false);
  });

  it('opens the context panel on demand and reflects the focused message run', async () => {
    seedWorkbench([
      message('m1', 'user', 'question'),
      message('m2', 'assistant', 'first answer', ['searchDevices']),
      message('m3', 'assistant', 'second answer', ['deviceStatusList']),
    ]);
    const appStore = useAppStore();
    appStore.setAgenticContextPanelOpen(true);
    const wrapper = mountWorkbench();

    // Defaults to the last assistant message.
    await flushPromises();
    expect(wrapper.find('.agentic-context').exists()).toBe(true);
    expect(wrapper.find('.agentic-context').text()).toContain('deviceStatusList');
    expect(wrapper.find('.agentic-context').text()).not.toContain('searchDevices');

    // Selecting an earlier message retargets the panel.
    const items = wrapper.findAll('.agentic-message');
    await items[1].trigger('click');
    await flushPromises();
    expect(wrapper.find('.agentic-context').text()).toContain('searchDevices');
  });

  it('collapses the sessions rail through the app-store preference', async () => {
    seedWorkbench([message('m1', 'assistant', 'hello')]);
    const wrapper = mountWorkbench();

    expect(wrapper.find('.agentic-sessions').classes()).not.toContain('is-collapsed');
    const appStore = useAppStore();
    appStore.toggleAgenticRailCollapsed();
    await flushPromises();
    expect(wrapper.find('.agentic-sessions').classes()).toContain('is-collapsed');
  });

  it('exits the workbench on the first Escape and closes the panel on the second', async () => {
    seedWorkbench([message('m1', 'assistant', 'hello')]);
    const wrapper = mountWorkbench();
    const store = useAgenticStore();

    await wrapper.find('.agentic-panel').trigger('keydown', {key: 'Escape'});
    expect(store.workbenchExpanded).toBe(false);
    expect(store.visible).toBe(true);

    await wrapper.find('.agentic-panel').trigger('keydown', {key: 'Escape'});
    expect(store.visible).toBe(false);
  });
});
