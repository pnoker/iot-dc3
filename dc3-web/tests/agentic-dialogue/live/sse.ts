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

/**
 * Minimal SSE client for the live agentic chat endpoint. Mirrors the frame
 * semantics of `src/api/agentic.ts` (agentic.event / agentic.visualization /
 * OpenAI-style delta chunks / `data: [DONE]`) but targets an absolute base URL
 * so the evaluator can run outside the browser bundle.
 */

export interface LiveTraceEvent {
  type: string;
  title: string;
  detail?: string;
  name?: string;
  phase?: string;
  status?: string;
}

export interface LiveOutcome {
  content: string;
  reasoning: string;
  events: LiveTraceEvent[];
  visualizationCount: number;
  finishReason?: string;
  done: boolean;
  error?: string;
  elapsedMs: number;
}

interface Frame {
  object?: string;
  type?: string;
  title?: string;
  detail?: string;
  name?: string;
  phase?: string;
  status?: string;
  visualization?: unknown;
  choices?: Array<{
    delta?: {content?: string; reasoning_content?: string};
    finish_reason?: string;
    message?: {content?: string};
  }>;
}

const parseFrame = (data: string, outcome: LiveOutcome): void => {
  if (!data || data === '[DONE]') {
    if (data === '[DONE]') outcome.done = true;
    return;
  }
  let frame: Frame;
  try {
    frame = JSON.parse(data) as Frame;
  } catch {
    outcome.error = `unparseable SSE frame: ${data.slice(0, 80)}`;
    return;
  }
  if (frame.object === 'agentic.event' && frame.type && frame.title) {
    outcome.events.push({
      type: frame.type,
      title: frame.title,
      detail: frame.detail,
      name: frame.name,
      phase: frame.phase,
      status: frame.status,
    });
    return;
  }
  if (frame.object === 'agentic.visualization' && frame.visualization) {
    outcome.visualizationCount += 1;
    return;
  }
  const choice = frame.choices?.[0];
  if (choice?.delta?.content) outcome.content += choice.delta.content;
  if (choice?.delta?.reasoning_content) outcome.reasoning += choice.delta.reasoning_content;
  if (choice?.message?.content) outcome.content += choice.message.content;
  if (choice?.finish_reason) outcome.finishReason = choice.finish_reason;
};

/**
 * Consume an SSE response body into a live outcome.
 * @param body - response body stream reader source
 * @returns the aggregated outcome
 */
export const consumeSse = async (body: string): Promise<LiveOutcome> => {
  const outcome: LiveOutcome = {
    content: '',
    reasoning: '',
    events: [],
    visualizationCount: 0,
    done: false,
    elapsedMs: 0,
  };
  const blocks = body.split(/\r?\n\r?\n/);
  for (const block of blocks) {
    const data = block
      .split(/\r?\n/)
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.replace(/^data:\s?/, ''))
      .join('\n')
      .trim();
    parseFrame(data, outcome);
  }
  return outcome;
};

export interface LiveRunConfig {
  baseUrl: string;
  tenant?: string;
  login?: string;
  cookie?: string;
  model?: string;
  timeoutMs?: number;
}

export interface LiveTurn {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

/**
 * Run one conversation against the live agentic chat endpoint.
 * @param config - endpoint and auth configuration
 * @param conversationId - client-generated conversation id shared across turns
 * @param turns - full user/assistant transcript to send (memory is server-side)
 * @returns the aggregated outcome of the final streamed turn
 */
export const runLiveConversation = async (
  config: LiveRunConfig,
  conversationId: string,
  turns: LiveTurn[]
): Promise<LiveOutcome> => {
  const started = Date.now();
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
    'Content-Type': 'application/json',
  };
  if (config.tenant) headers['X-Auth-Tenant'] = config.tenant;
  if (config.login) headers['X-Auth-Login'] = config.login;
  if (config.cookie) headers.Cookie = config.cookie;

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.timeoutMs ?? 120_000);
  try {
    const response = await fetch(`${config.baseUrl.replace(/\/$/, '')}/api/v3/agentic/chat/completions`, {
      method: 'POST',
      headers,
      signal: controller.signal,
      body: JSON.stringify({
        model: config.model ?? 'dc3-agentic',
        messages: turns,
        stream: true,
        conversationId,
      }),
    });
    if (!response.ok) {
      const text = await response.text();
      return {
        content: '',
        reasoning: '',
        events: [],
        visualizationCount: 0,
        done: false,
        error: `HTTP ${response.status}: ${text.slice(0, 200)}`,
        elapsedMs: Date.now() - started,
      };
    }
    const outcome = await consumeSse(await response.text());
    outcome.elapsedMs = Date.now() - started;
    return outcome;
  } catch (error) {
    return {
      content: '',
      reasoning: '',
      events: [],
      visualizationCount: 0,
      done: false,
      error: error instanceof Error ? error.message : String(error),
      elapsedMs: Date.now() - started,
    };
  } finally {
    clearTimeout(timer);
  }
};
