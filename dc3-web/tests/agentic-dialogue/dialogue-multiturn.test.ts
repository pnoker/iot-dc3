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
import {db} from '@/mock/db';

import {CORPUS} from './corpus';
import {ensureMockChatEngine} from './helpers/chatHarness';

/**
 * Multi-turn dialogue continuity: the real Pinia store drives sequential turns
 * through the real streaming transport and the mock chat engine, while the
 * persisted history comes from the mock DB exactly as the backend would serve
 * it. Asserts transcript growth, role ordering, per-turn request shape, and
 * conversation-id continuity.
 */

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
  updateAgenticSession: vi.fn(),
  uploadAgenticAttachment: vi.fn(),
}));

vi.mock('@/api/agentic', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/agentic')>();
  return {...actual, ...apiMocks};
});

vi.mock('@/utils/notificationUtil', () => ({
  failMessage: vi.fn(),
  warnMessage: vi.fn(),
}));

interface CapturedRequest {
  conversationId?: string;
  messages?: Array<{role?: string; content?: string}>;
  model?: string;
  stream?: boolean;
}

const multiTurnCases = CORPUS.filter((item) => item.turns.length > 1);
const captured: CapturedRequest[] = [];
let recorderInstalled = false;

const installRequestRecorder = (): void => {
  ensureMockChatEngine();
  if (recorderInstalled) return;
  recorderInstalled = true;
  const previous = window.fetch.bind(window);
  const recording = (async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
    if (url.includes('/chat/completions') && typeof init?.body === 'string') {
      try {
        captured.push(JSON.parse(init.body) as CapturedRequest);
      } catch {
        /* non-JSON bodies are not dialogue requests */
      }
    }
    return previous(input as RequestInfo, init);
  }) as typeof window.fetch;
  window.fetch = recording;
  vi.stubGlobal('fetch', recording);
};

describe('multi-turn dialogue continuity', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    captured.length = 0;
    installRequestRecorder();

    apiMocks.listAgenticMessages.mockImplementation((conversationId: string) =>
      Promise.resolve(db.agenticMessages.filter((message) => String(message.conversationId) === String(conversationId)))
    );
    apiMocks.listAgenticAttachments.mockResolvedValue([]);
    apiMocks.listPendingAgenticActions.mockResolvedValue({items: []});
    apiMocks.listAgenticModels.mockResolvedValue([]);
    apiMocks.listAgenticSessions.mockResolvedValue({items: []});
    apiMocks.updateAgenticSession.mockImplementation((conversationId: string, data: Record<string, unknown>) =>
      Promise.resolve({conversationId, ...data})
    );
    apiMocks.completeAgenticChatCompletion.mockResolvedValue(undefined);
  });

  it('covers at least 80 multi-turn scripts', () => {
    expect(multiTurnCases.length).toBeGreaterThanOrEqual(80);
  });

  it.each(multiTurnCases.map((item) => [item.id, item] as const))(
    '%s keeps the transcript coherent across turns',
    async (_id, item) => {
      const store = useAgenticStore();
      store.newSession();

      for (const turn of item.turns) {
        const ok = await store.sendMessage(turn);
        expect(ok).toBe(true);
      }

      const messages = store.messagesByConversation[store.activeConversationId] ?? [];
      // Every turn contributes one user message and one assistant reply.
      expect(messages.length).toBe(item.turns.length * 2);
      messages.forEach((message, index) => {
        expect(message.role).toBe(index % 2 === 0 ? 'user' : 'assistant');
      });
      for (const message of messages.filter((entry) => entry.role === 'assistant')) {
        expect(message.content.trim().length).toBeGreaterThan(0);
        expect(message.streaming).toBe(false);
      }

      // Protocol continuity: one conversation id, one user message per request.
      expect(captured.length).toBe(item.turns.length);
      const conversationIds = new Set(captured.map((request) => request.conversationId));
      expect(conversationIds.size).toBe(1);
      captured.forEach((request, index) => {
        expect(request.messages).toEqual([{role: 'user', content: item.turns[index]}]);
      });
    }
  );
});
