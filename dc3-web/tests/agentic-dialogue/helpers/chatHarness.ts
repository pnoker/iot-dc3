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

import {vi} from 'vitest';

import {completeAgenticChatCompletion, streamAgenticChatCompletion} from '@/api/agentic';
import type {AgenticStreamCallbacks, AgenticTraceEvent, AgenticVisualizationSpec} from '@/config/types';
import i18n from '@/config/i18n';
import {db} from '@/mock/db';
import {installAgenticFetchMock} from '@/mock/fetch';

import type {DialogueCase} from '../corpus/types';

/**
 * Test harness that runs corpus cases through the real dialogue chain: the
 * agentic fetch API layer, its SSE parser, and the scripted mock chat engine
 * (`installAgenticFetchMock`) that stands in for the backend agent loop.
 */

export interface ChatOutcome {
  content: string;
  reasoning: string;
  events: AgenticTraceEvent[];
  visualizations: AgenticVisualizationSpec[];
  finishReason?: string;
  done: boolean;
  error?: Error;
  elapsedMs: number;
}

let mockInstalled = false;
let conversationSeq = 0;

/**
 * Install the mock chat engine once per test process and keep the global fetch
 * stub pointing at it (the api layer resolves bare `fetch`, not `window.fetch`).
 */
export const ensureMockChatEngine = (): void => {
  if (mockInstalled) return;
  installAgenticFetchMock(db);
  vi.stubGlobal('fetch', window.fetch);
  mockInstalled = true;
};

/**
 * Apply the mock locale under test for a case.
 * @param item - case whose expectations declare the reply language
 */
export const applyCaseLocale = (item: DialogueCase): void => {
  i18n.global.locale.value = item.expectations.language === 'en' ? 'en' : 'zh';
};

const chatRequest = (item: DialogueCase, content: string) => ({
  model: 'dc3-agentic',
  messages: [{role: 'user' as const, content}],
  stream: true,
  // The transport contract requires a conversation id; give every case turn a
  // fresh one so mock persistence cannot bleed across cases.
  conversationId: `dialogue-${item.id}-${++conversationSeq}`,
  temperature: undefined,
  maxTokens: undefined,
  attachments: [],
  reasoning: false,
});

/**
 * Run one case's final utterance through the streaming chat chain.
 * @param item - case to run
 * @param content - utterance to send (usually the last turn)
 * @returns the collected stream outcome
 */
export const runStreaming = async (item: DialogueCase, content: string): Promise<ChatOutcome> => {
  ensureMockChatEngine();
  applyCaseLocale(item);

  const outcome: ChatOutcome = {
    content: '',
    reasoning: '',
    events: [],
    visualizations: [],
    done: false,
    elapsedMs: 0,
  };
  const callbacks: AgenticStreamCallbacks = {
    onDelta: (delta) => {
      outcome.content += delta;
    },
    onReasoning: (reasoning) => {
      outcome.reasoning += reasoning;
    },
    onEvent: (event) => outcome.events.push(event),
    onVisualization: (visualization) => outcome.visualizations.push(visualization),
    onFinish: (reason) => {
      outcome.finishReason = reason;
    },
    onDone: () => {
      outcome.done = true;
    },
    onError: (error) => {
      outcome.error = error;
    },
  };

  const started = Date.now();
  try {
    await streamAgenticChatCompletion(chatRequest(item, content), callbacks);
  } catch (error) {
    outcome.error = error instanceof Error ? error : new Error(String(error));
  }
  outcome.elapsedMs = Date.now() - started;
  return outcome;
};

/**
 * Run one case's final utterance through the blocking (non-stream) chat chain.
 * @param item - case to run
 * @param content - utterance to send (usually the last turn)
 * @returns the collected blocking outcome
 */
export const runBlocking = async (item: DialogueCase, content: string): Promise<ChatOutcome> => {
  ensureMockChatEngine();
  applyCaseLocale(item);

  const outcome: ChatOutcome = {
    content: '',
    reasoning: '',
    events: [],
    visualizations: [],
    done: true,
    elapsedMs: 0,
  };
  const started = Date.now();
  try {
    const response = await completeAgenticChatCompletion({...chatRequest(item, content), stream: false});
    const choice = response.choices?.[0];
    outcome.content = choice?.message?.content || '';
    outcome.finishReason = choice?.finishReason ?? choice?.finish_reason;
    outcome.visualizations = (choice?.message?.contentExt?.charts || []) as AgenticVisualizationSpec[];
  } catch (error) {
    outcome.error = error instanceof Error ? error : new Error(String(error));
  }
  outcome.elapsedMs = Date.now() - started;
  return outcome;
};

/** CJK detection used by the reply-language assertions. */
export const containsCjk = (text: string): boolean => /[\u3400-\u9fff]/u.test(text);
