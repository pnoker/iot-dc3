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

import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';

import {useAgenticStore} from '@/store';
import type {AgenticStreamCallbacks} from '@/config/types';

const apiMocks = vi.hoisted(() => ({
  completeAgenticChatCompletion: vi.fn(),
  confirmAgenticAction: vi.fn(),
  deleteAgenticSession: vi.fn(),
  listAgenticAttachments: vi.fn(),
  listAgenticMessages: vi.fn(),
  listAgenticModels: vi.fn(),
  getPendingAgenticActions: vi.fn(),
  listAgenticSessions: vi.fn(),
  rejectAgenticAction: vi.fn(),
  streamAgenticChatCompletion: vi.fn(),
  updateAgenticSession: vi.fn(),
  uploadAgenticAttachment: vi.fn(),
}));

vi.mock('@/api/agentic', () => apiMocks);

vi.mock('@/utils/notificationUtil', () => ({
  failMessage: vi.fn(),
  warnMessage: vi.fn(),
}));

describe('agentic store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();

    apiMocks.listAgenticMessages.mockResolvedValue({data: undefined});
    apiMocks.listAgenticAttachments.mockResolvedValue({data: []});
    apiMocks.getPendingAgenticActions.mockResolvedValue({data: []});
    apiMocks.listAgenticModels.mockResolvedValue({data: []});
    apiMocks.listAgenticSessions.mockResolvedValue({data: {records: []}});
    apiMocks.updateAgenticSession.mockImplementation((conversationId: string, data: Record<string, unknown>) =>
      Promise.resolve({data: {conversationId, ...data}})
    );
  });

  it('accumulates streaming reasoning text and avoids redundant session preference updates while sending', async () => {
    apiMocks.streamAgenticChatCompletion.mockImplementation(
      async (_request: unknown, callbacks: AgenticStreamCallbacks) => {
        callbacks.onReasoning?.('检查设备状态，');
        callbacks.onReasoning?.('确认采集点。');
        callbacks.onDelta?.('设备运行正常。');
      }
    );
    apiMocks.listAgenticMessages.mockResolvedValue({
      data: [
        {
          id: 'persisted-user-1',
          role: 'user',
          content: '查看设备状态',
          messageIndex: 1,
        },
        {
          id: 'persisted-assistant-1',
          role: 'assistant',
          content: '设备运行正常。',
          contentExt: {
            reasoning: true,
            reasoningContent: '检查设备状态，确认采集点。',
          },
          messageIndex: 2,
        },
      ],
    });

    const store = useAgenticStore();
    // Direct field assignment instead of `bootstrap()` — going through the
    // public action would force four mocked API calls (sessions / models /
    // attachments / pending actions) per test for values orthogonal to the
    // streaming-reasoning behaviour under test. The store is plain refs so
    // assignment is well-defined; no derivation logic is being bypassed.
    store.models = [
      {
        model: 'deepseek-v4-pro',
        label: 'DeepSeek V4 Pro',
        stream: true,
        toolCall: true,
        vision: false,
        reasoning: true,
      },
    ];
    store.selectedModel = 'deepseek-v4-pro';
    store.reasoningEnabled = true;
    store.newSession();
    store.sessions = store.sessions.map((session) => ({
      ...session,
      createTime: '2026-05-16 00:00:00',
      operateTime: '2026-05-16 00:00:00',
    }));

    const conversationId = store.activeConversationId;
    await store.sendMessage('查看设备状态');

    const assistantMessage = store.messagesByConversation[conversationId].find(
      (message) => message.role === 'assistant'
    );
    expect(assistantMessage?.reasoning).toBe('检查设备状态，确认采集点。');
    expect(assistantMessage?.content).toBe('设备运行正常。');

    const streamRequest = apiMocks.streamAgenticChatCompletion.mock.calls[0][0];
    expect(streamRequest).toMatchObject({
      model: 'deepseek-v4-pro',
      reasoning: true,
      conversationId,
      messages: [{role: 'user', content: '查看设备状态'}],
    });

    const sessionPayloads = apiMocks.updateAgenticSession.mock.calls.map(([, payload]) => payload);
    expect(sessionPayloads).toEqual([{title: '查看设备状态'}]);
    for (const payload of sessionPayloads) {
      expect(payload).not.toHaveProperty('model');
      expect(payload).not.toHaveProperty('sessionConfig');
      expect(payload).not.toHaveProperty('sessionExt');
    }
    expect(apiMocks.listAgenticSessions).not.toHaveBeenCalled();
  });

  it('restores session preferences from persisted session_ext metadata', async () => {
    apiMocks.listAgenticModels.mockResolvedValue({
      data: [
        {
          model: 'deepseek-v4-pro',
          label: 'DeepSeek V4 Pro',
          stream: true,
          toolCall: true,
          vision: false,
          reasoning: true,
          temperature: 0.7,
          maxTokens: 2048,
        },
      ],
    });
    apiMocks.listAgenticSessions.mockResolvedValue({
      data: {
        records: [
          {
            conversationId: 'conversation-1',
            title: 'Device status',
            session_ext: {
              model: 'deepseek-v4-pro',
              reasoning_enabled: true,
              temperature: 0.2,
              max_tokens: 4096,
            },
          },
        ],
      },
    });

    const store = useAgenticStore();
    await store.bootstrap();

    expect(store.activeConversationId).toBe('conversation-1');
    expect(store.selectedModel).toBe('deepseek-v4-pro');
    expect(store.reasoningEnabled).toBe(true);
    expect(store.temperature).toBe(0.2);
    expect(store.maxTokens).toBe(4096);
    expect(store.currentSession?.sessionExt).toEqual(
      expect.objectContaining({
        model: 'deepseek-v4-pro',
        reasoningEnabled: true,
      })
    );
  });

  it('attaches streaming visualizations to the active assistant message', async () => {
    apiMocks.streamAgenticChatCompletion.mockImplementation(
      async (_request: unknown, callbacks: AgenticStreamCallbacks) => {
        callbacks.onVisualization?.({
          id: 'chart-1',
          type: 'line',
          title: 'Point Trend',
          dataset: [{index: 0, value: 23.5}],
          encode: {x: 'index', y: 'value'},
        });
        callbacks.onDelta?.('趋势分析完成。');
      }
    );

    const store = useAgenticStore();
    store.newSession();
    const conversationId = store.activeConversationId;

    await store.sendMessage('分析位号历史数据');

    const assistantMessage = store.messagesByConversation[conversationId].find(
      (message) => message.role === 'assistant'
    );
    expect(assistantMessage?.content).toBe('趋势分析完成。');
    expect(assistantMessage?.contentExt?.charts).toEqual([
      expect.objectContaining({
        id: 'chart-1',
        type: 'line',
        title: 'Point Trend',
      }),
    ]);
  });
});
