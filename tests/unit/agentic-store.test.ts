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

import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

import { useAgenticStore } from '@/store';
import type { AgenticStreamCallbacks } from '@/config/types';

const apiMocks = vi.hoisted(() => ({
  completeAgenticChatCompletion: vi.fn(),
  confirmAgenticAction: vi.fn(),
  deleteAgenticSession: vi.fn(),
  getAgenticAttachments: vi.fn(),
  getAgenticMessages: vi.fn(),
  getAgenticModels: vi.fn(),
  getPendingAgenticActions: vi.fn(),
  getAgenticSessions: vi.fn(),
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

    apiMocks.getAgenticMessages.mockResolvedValue({ data: undefined });
    apiMocks.getAgenticAttachments.mockResolvedValue({ data: [] });
    apiMocks.getPendingAgenticActions.mockResolvedValue({ data: [] });
    apiMocks.getAgenticModels.mockResolvedValue({ data: [] });
    apiMocks.getAgenticSessions.mockResolvedValue({ data: { records: [] } });
    apiMocks.updateAgenticSession.mockImplementation((conversationId: string, data: Record<string, unknown>) =>
      Promise.resolve({ data: { conversationId, ...data } })
    );
  });

  it('accumulates streaming reasoning text and persists session state through sessionExt only', async () => {
    apiMocks.streamAgenticChatCompletion.mockImplementation(
      async (_request: unknown, callbacks: AgenticStreamCallbacks) => {
        callbacks.onReasoning?.('检查设备状态，');
        callbacks.onReasoning?.('确认采集点。');
        callbacks.onDelta?.('设备运行正常。');
      }
    );

    const store = useAgenticStore();
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
    });

    const sessionPayloads = apiMocks.updateAgenticSession.mock.calls.map(([, payload]) => payload);
    expect(sessionPayloads).toContainEqual({
      sessionExt: expect.objectContaining({
        model: 'deepseek-v4-pro',
        reasoningEnabled: true,
      }),
    });
    for (const payload of sessionPayloads) {
      expect(payload).not.toHaveProperty('model');
      expect(payload).not.toHaveProperty('sessionConfig');
    }
  });

  it('restores session preferences from persisted session_ext metadata', async () => {
    apiMocks.getAgenticModels.mockResolvedValue({
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
    apiMocks.getAgenticSessions.mockResolvedValue({
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
});
