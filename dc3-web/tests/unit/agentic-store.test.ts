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
  listPendingAgenticActions: vi.fn(),
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

    apiMocks.listAgenticMessages.mockResolvedValue(undefined);
    apiMocks.listAgenticAttachments.mockResolvedValue([]);
    apiMocks.listPendingAgenticActions.mockResolvedValue({items: []});
    apiMocks.listAgenticModels.mockResolvedValue([]);
    apiMocks.listAgenticSessions.mockResolvedValue({items: []});
    apiMocks.updateAgenticSession.mockImplementation((conversationId: string, data: Record<string, unknown>) =>
      Promise.resolve({conversationId, ...data})
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
    apiMocks.listAgenticMessages.mockResolvedValue([
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
    ]);

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
    apiMocks.listAgenticModels.mockResolvedValue([
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
    ]);
    apiMocks.listAgenticSessions.mockResolvedValue({
      items: [
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

  it('keeps the session error visible and clears loading after a failed refresh', async () => {
    const error = new Error('session endpoint unavailable');
    apiMocks.listAgenticSessions.mockRejectedValueOnce(error);

    const store = useAgenticStore();
    expect(await store.loadSessions()).toBe(false);

    expect(store.sessionsError).toBe(error);
    expect(store.sessionsLoading).toBe(false);
  });

  it('retries sessions and reloads the active conversation data', async () => {
    apiMocks.listAgenticSessions
      .mockRejectedValueOnce(new Error('temporary failure'))
      .mockResolvedValueOnce({
        items: [{conversationId: 'conversation-retry', title: 'Retry me'}],
      });
    apiMocks.listAgenticMessages.mockResolvedValueOnce([
      {id: 'message-retry', role: 'user', content: 'hello', messageIndex: 1},
    ]);

    const store = useAgenticStore();
    expect(await store.loadSessions()).toBe(false);
    expect(await store.retrySessions()).toBe(true);

    expect(store.sessionsError).toBeNull();
    expect(store.activeConversationId).toBe('conversation-retry');
    expect(store.currentMessages).toEqual([
      expect.objectContaining({id: 'message-retry', content: 'hello'}),
    ]);
    expect(apiMocks.listAgenticAttachments).toHaveBeenCalledWith('conversation-retry');
    expect(apiMocks.listPendingAgenticActions).toHaveBeenCalledWith('conversation-retry');
  });

  it('ignores stale session selections when the user switches quickly', async () => {
    let resolveFirst!: (value: unknown[]) => void;
    let resolveSecond!: (value: unknown[]) => void;
    apiMocks.listAgenticSessions.mockResolvedValue({
      items: [
        {conversationId: 'conversation-a', title: 'A'},
        {conversationId: 'conversation-b', title: 'B'},
      ],
    });
    apiMocks.listAgenticMessages
      .mockImplementationOnce(() => new Promise((resolve) => (resolveFirst = resolve)))
      .mockImplementationOnce(() => new Promise((resolve) => (resolveSecond = resolve)));

    const store = useAgenticStore();
    await store.loadSessions();
    const firstSelection = store.selectSession('conversation-a');
    const secondSelection = store.selectSession('conversation-b');

    resolveSecond([{id: 'message-b', role: 'user', content: 'B'}]);
    expect(await secondSelection).toBe(true);
    resolveFirst([{id: 'message-a', role: 'user', content: 'A'}]);
    expect(await firstSelection).toBe(false);
    expect(store.activeConversationId).toBe('conversation-b');
  });

  it('keeps optimistic messages when an in-flight loadMessages response lands mid-stream', async () => {
    let resolveInFlight!: (value: unknown[]) => void;
    let resolveStream!: () => void;
    apiMocks.listAgenticMessages
      .mockImplementationOnce(() => new Promise((resolve) => (resolveInFlight = resolve)))
      .mockResolvedValue([
        {id: 'server-user-1', role: 'user', content: '查看设备状态', messageIndex: 1},
        {id: 'server-assistant-1', role: 'assistant', content: '设备运行正常。', messageIndex: 2},
      ]);
    apiMocks.streamAgenticChatCompletion.mockImplementation(
      async (_request: unknown, callbacks: AgenticStreamCallbacks) => {
        callbacks.onDelta?.('设备运行正常。');
        await new Promise<void>((resolve) => (resolveStream = resolve));
      }
    );

    const store = useAgenticStore();
    store.newSession();
    const conversationId = store.activeConversationId;

    // The user selects the conversation (list request goes out, stays in
    // flight) and immediately sends a message, so the optimistic turn exists
    // while the stale snapshot is still on the wire.
    const selection = store.selectSession(conversationId);
    const send = store.sendMessage('查看设备状态');
    await Promise.resolve();

    resolveInFlight([{id: 'stale-user', role: 'user', content: 'stale', messageIndex: 0}]);
    await selection;

    const messages = store.messagesByConversation[conversationId];
    expect(messages.some((message) => message.role === 'user' && message.content === '查看设备状态')).toBe(true);
    expect(messages.some((message) => message.id === 'stale-user')).toBe(false);
    expect(messages.some((message) => message.role === 'assistant' && message.streaming)).toBe(true);
    expect(store.messagesLoadingByConversation[conversationId]).toBe(false);

    resolveStream();
    await send;

    const finalMessages = store.messagesByConversation[conversationId];
    expect(finalMessages.some((message) => message.role === 'user' && message.content === '查看设备状态')).toBe(true);
    expect(finalMessages.some((message) => message.role === 'assistant' && message.content === '设备运行正常。')).toBe(true);
  });

  it('blocks deleting the conversation that is currently streaming', async () => {
    apiMocks.streamAgenticChatCompletion.mockImplementation(() => new Promise(() => undefined));

    const store = useAgenticStore();
    store.newSession();
    const conversationId = store.activeConversationId;
    const send = store.sendMessage('keep this conversation');
    await Promise.resolve();

    expect(store.streamingConversationId).toBe(conversationId);
    expect(await store.deleteSession(conversationId)).toBe(false);
    expect(apiMocks.deleteAgenticSession).not.toHaveBeenCalled();
    store.stopStreaming();
    send.catch(() => undefined);
  });

  it('deduplicates session rename and delete requests', async () => {
    let resolveRename!: (value: unknown) => void;
    apiMocks.updateAgenticSession.mockImplementationOnce(
      () => new Promise((resolve) => (resolveRename = resolve))
    );
    let resolveDelete!: () => void;
    apiMocks.deleteAgenticSession.mockImplementationOnce(
      () => new Promise((resolve) => (resolveDelete = resolve))
    );

    const store = useAgenticStore();
    store.sessions = [{conversationId: 'conversation-actions', title: 'Before'}];

    const rename = store.renameSession('conversation-actions', 'After');
    expect(await store.renameSession('conversation-actions', 'Ignored')).toBe(false);
    expect(store.sessionActionLoading['conversation-actions']).toBe(true);
    resolveRename({conversationId: 'conversation-actions', title: 'After'});
    expect(await rename).toBe(true);
    expect(apiMocks.updateAgenticSession).toHaveBeenCalledTimes(1);

    const firstDelete = store.deleteSession('conversation-actions');
    expect(await store.deleteSession('conversation-actions')).toBe(false);
    expect(store.sessionActionLoading['conversation-actions']).toBe(true);
    resolveDelete();
    expect(await firstDelete).toBe(true);
    expect(apiMocks.deleteAgenticSession).toHaveBeenCalledTimes(1);
  });

  it('always clears action loading when confirmation or rejection fails', async () => {
    apiMocks.confirmAgenticAction.mockRejectedValueOnce(new Error('confirm failed'));
    apiMocks.rejectAgenticAction.mockRejectedValueOnce(new Error('reject failed'));

    const store = useAgenticStore();
    store.activeConversationId = 'conversation-actions';
    await expect(store.confirmAction('action-confirm')).rejects.toThrow('confirm failed');
    expect(store.actionLoading['action-confirm']).toBeUndefined();
    await expect(store.rejectAction('action-reject')).rejects.toThrow('reject failed');
    expect(store.actionLoading['action-reject']).toBeUndefined();
  });
});
