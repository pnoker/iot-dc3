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

import {createPinia, setActivePinia} from 'pinia';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import type {AgenticStreamCallbacks} from '@/config/types';
import {useAgenticStore} from '@/store';

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
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), warnMessage: vi.fn()}));

/**
 * Controllable stream fixture: the store awaits streamAgenticChatCompletion,
 * so the test drives the callbacks and resolves the promise — the network's
 * timing is replaced by the test's own hands.
 */
const controllableStream = () => {
  const state = {
    callbacks: undefined as AgenticStreamCallbacks | undefined,
    settle: undefined as undefined | (() => void),
    blow: undefined as undefined | ((reason: unknown) => void),
  };
  apiMocks.streamAgenticChatCompletion.mockImplementation((_request: unknown, callbacks: AgenticStreamCallbacks) => {
    state.callbacks = callbacks;
    return new Promise<void>((resolve, reject) => {
      state.settle = resolve;
      state.blow = reject;
    });
  });
  return {
    delta: (text: string) => state.callbacks?.onDelta?.(text),
    reasoning: (text: string) => state.callbacks?.onReasoning?.(text),
    finish: (reason?: string) => {
      state.callbacks?.onFinish?.(reason || 'stop');
      state.callbacks?.onDone?.();
      state.settle?.();
    },
    fail: (message: string) => {
      state.callbacks?.onError?.(new Error(message));
      state.settle?.();
    },
    abort: (reason: unknown) => state.blow?.(reason),
  };
};

const seededStore = () => {
  apiMocks.listAgenticMessages.mockResolvedValue(undefined);
  apiMocks.listAgenticAttachments.mockResolvedValue([]);
  apiMocks.listPendingAgenticActions.mockResolvedValue({items: []});
  apiMocks.listAgenticModels.mockResolvedValue([]);
  apiMocks.listAgenticSessions.mockResolvedValue({items: []});
  const store = useAgenticStore();
  store.models = [{model: 'm1', label: 'M1', stream: true, toolCall: true, vision: false, reasoning: false}];
  store.selectedModel = 'm1';
  return store;
};

describe('stream state machine and send races', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('rejects a second send while a stream is in flight', async () => {
    const stream = controllableStream();
    const store = seededStore();
    store.newSession();

    const first = store.sendMessage('first');
    await Promise.resolve();
    await Promise.resolve();
    expect(store.streaming).toBe(true);
    expect(await store.sendMessage('second')).toBe(false);

    stream.finish();
    await first;
    expect(store.streaming).toBe(false);
  });

  it('locks conversation switching while streaming and keeps deltas on the origin', async () => {
    const stream = controllableStream();
    const store = seededStore();
    store.newSession();
    const firstConversation = store.activeConversationId;

    const send = store.sendMessage('hello');
    await Promise.resolve();
    await Promise.resolve();

    // switching away mid-stream is refused: the stream owns the conversation
    store.newSession();
    expect(store.activeConversationId).toBe(firstConversation);
    stream.delta('part-1');
    stream.delta('part-2');

    const origin = store.messagesByConversation[firstConversation].find((m) => m.role === 'assistant');
    expect(origin?.content).toBe('part-1part-2');

    stream.finish();
    await send;
    // once settled, switching opens a fresh conversation again
    store.newSession();
    expect(store.activeConversationId).not.toBe(firstConversation);
  });

  it('records a failed finish when the stream errors and clears the streaming state', async () => {
    const stream = controllableStream();
    const store = seededStore();
    store.newSession();
    const conversationId = store.activeConversationId;

    const send = store.sendMessage('will fail');
    await Promise.resolve();
    await Promise.resolve();
    stream.delta('partial');
    stream.abort(new Error('connection reset'));
    await send.catch(() => undefined);

    const assistant = store.messagesByConversation[conversationId].find((m) => m.role === 'assistant');
    expect(store.streaming).toBe(false);
    // partial output is preserved and the failure is surfaced in-line
    expect(assistant?.content).toContain('partial');
    expect(assistant?.content).toContain('Request failed');
    expect(assistant?.finishReason).toBe('error');
  });

  it('keeps partial output when the stream is stopped by the user', async () => {
    const stream = controllableStream();
    const store = seededStore();
    store.newSession();
    const conversationId = store.activeConversationId;

    const send = store.sendMessage('long running');
    await Promise.resolve();
    await Promise.resolve();
    stream.delta('half of the answer');
    store.stopStreaming();
    stream.finish(); // the fixture settles the transport after the cancel
    await send.catch(() => undefined);

    const assistant = store.messagesByConversation[conversationId].find((m) => m.role === 'assistant');
    expect(store.streaming).toBe(false);
    expect(assistant?.content).toContain('half of the answer');
    expect(store.streamingConversationId).toBe('');
  });

  it('serializes rapid sends so only the first one streams', async () => {
    const stream = controllableStream();
    const store = seededStore();
    store.newSession();

    const winner = store.sendMessage('one');
    const losers = await Promise.all([store.sendMessage('two'), store.sendMessage('three')]);
    expect(losers).toEqual([false, false]);

    stream.finish();
    expect(await winner).toBe(true);
    expect(store.streaming).toBe(false);
  });
});
