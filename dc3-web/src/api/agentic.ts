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

import {httpDelete, httpGet, httpPost} from '@/api/common';
import {API_AGENTIC_BASE} from '@/config/constant/api';
import {AXIOS_CONFIG} from '@/config/constant/axios';
import {handleAuthFailure} from '@/config/axios';
import type {
  AgenticAction,
  AgenticAttachment,
  AgenticChatCompletionRequest,
  AgenticChatCompletionResponse,
  AgenticMessage,
  AgenticModel,
  AgenticModelConfig,
  AgenticProvider,
  AgenticProviderCheckRequest,
  AgenticProviderCheckResult,
  AgenticSession,
  AgenticStreamCallbacks,
  AgenticTraceEvent,
  AgenticVisualizationSpec,
  PageQuery,
  PageResult,
} from '@/config/types';
import {buildAuthHeaders} from '@/utils/authSession';

interface OpenAIChunk {
  object?: string;
  type?: AgenticTraceEvent['type'];
  title?: string;
  detail?: string;
  name?: string;
  phase?: AgenticTraceEvent['phase'];
  status?: AgenticTraceEvent['status'];
  code?: string;
  created?: number;
  visualization?: AgenticVisualizationSpec;
  choices?: Array<{
    delta?: {
      content?: string;
      reasoning_content?: string;
    };
    finish_reason?: string;
  }>;
}

/**
 * List agentic models.
 * @returns the listed agentic models
 */
export const listAgenticModels = () => httpGet<AgenticModel[]>(`${API_AGENTIC_BASE}/model/list`);

/**
 * List agentic model configs.
 * @returns the listed agentic model configs
 */
export const listAgenticModelConfigs = () => httpGet<AgenticModelConfig[]>(`${API_AGENTIC_BASE}/model/config/list`);

/**
 * Create agentic model config.
 * @param data - payload to send or render
 * @returns the operation result
 */
export const addAgenticModelConfig = (data: AgenticModelConfig) =>
  httpPost<AgenticModelConfig>(`${API_AGENTIC_BASE}/model/config/add`, data);

/**
 * Update agentic model config.
 * @param data - payload to send or render
 * @returns the operation result
 */
export const updateAgenticModelConfig = (data: AgenticModelConfig) =>
  httpPost<AgenticModelConfig>(`${API_AGENTIC_BASE}/model/config/update`, data);

/**
 * Delete agentic model config.
 * @param id - record id
 * @returns the operation result
 */
export const deleteAgenticModelConfig = (id: string) =>
  httpDelete<void>(`${API_AGENTIC_BASE}/model/config/delete`, {params: {id}});

/**
 * List agentic providers.
 * @returns the listed agentic providers
 */
export const listAgenticProviders = () => httpGet<AgenticProvider[]>(`${API_AGENTIC_BASE}/provider/list`);

/**
 * Create agentic provider.
 * @param data - payload to send or render
 * @returns the operation result
 */
export const addAgenticProvider = (data: AgenticProvider) =>
  httpPost<AgenticProvider>(`${API_AGENTIC_BASE}/provider/add`, data);

/**
 * Update agentic provider.
 * @param data - payload to send or render
 * @returns the operation result
 */
export const updateAgenticProvider = (data: AgenticProvider) =>
  httpPost<AgenticProvider>(`${API_AGENTIC_BASE}/provider/update`, data);

/**
 * Delete agentic provider.
 * @param id - record id
 * @returns the operation result
 */
export const deleteAgenticProvider = (id: string) =>
  httpDelete<void>(`${API_AGENTIC_BASE}/provider/delete`, {params: {id}});

/**
 * Check agentic provider connectivity. Probe a saved provider by id (blank
 * fields fall back to stored values) or an unsaved draft configuration. The
 * endpoint always answers HTTP 200; a failed probe is a diagnosis in the
 * result body, never a transport error. Longer timeout than the global
 * default: the L2 minimal chat probe may take tens of seconds.
 * @param data - saved provider id and/or draft provider fields, plus probe level
 * @returns the L1/L2 probe outcomes with a classified error diagnosis
 */
export const checkAgenticProvider = (data: AgenticProviderCheckRequest) =>
  httpPost<AgenticProviderCheckResult>(`${API_AGENTIC_BASE}/provider/check`, data, {timeout: 60000});

/**
 * Check one agentic model config end to end with a minimal chat completion.
 * Longer timeout than the global default for the real inference round trip.
 * @param id - model config id
 * @returns the L2 probe outcome with a classified error diagnosis
 */
export const checkAgenticModelConfig = (id: string) =>
  httpPost<AgenticProviderCheckResult>(`${API_AGENTIC_BASE}/model/config/check`, undefined, {
    params: {id},
    timeout: 60000,
  });

/**
 * List agentic sessions.
 * @param query - page query with filters and paging
 * @returns the listed agentic sessions
 */
export const listAgenticSessions = (query?: PageQuery) =>
  httpPost<PageResult<AgenticSession>>(`${API_AGENTIC_BASE}/session/list`, query ?? {});

/**
 * Delete agentic session.
 * @param conversationId - conversation id to scope the request
 * @returns the operation result
 */
export const deleteAgenticSession = (conversationId: string) =>
  httpDelete<void>(`${API_AGENTIC_BASE}/session/delete`, {
    params: {conversation_id: conversationId},
  });

/**
 * Update agentic session.
 * @param conversationId - conversation id to scope the request
 * @param data - payload to send or render
 * @returns the operation result
 */
export const updateAgenticSession = (
  conversationId: string,
  data: Partial<Pick<AgenticSession, 'title' | 'sessionExt'>>
) =>
  httpPost<AgenticSession>(`${API_AGENTIC_BASE}/session/update`, data, {
    params: {conversation_id: conversationId},
  });

/**
 * List agentic messages, walking all pages so callers keep receiving the full
 * conversation history. The endpoint paginates (limit capped at 200 server-side);
 * this wrapper stitches the pages back together until hasNext turns false.
 * @param conversationId - conversation id to scope the request
 * @returns the listed agentic messages
 */
export const listAgenticMessages = async (conversationId: string): Promise<AgenticMessage[]> => {
  const limit = 200;
  let offset = 0;
  const messages: AgenticMessage[] = [];
  // Safety bound: 100 pages x 200 rows is far beyond any real conversation and stops
  // a buggy backend hasNext flag from looping forever.
  for (let page = 0; page < 100; page += 1) {
    const result = await httpPost<PageResult<AgenticMessage>>(`${API_AGENTIC_BASE}/message/list`, {
      conversationId,
      offset,
      limit,
    });
    if (!result || !Array.isArray(result.items)) {
      return messages;
    }
    messages.push(...result.items);
    if (!result.hasNext) {
      return messages;
    }
    offset += result.items.length || limit;
  }
  return messages;
};

/**
 * Upload agentic attachment.
 * @param conversationId - conversation id to scope the request
 * @param file - uploaded file payload
 * @returns the operation result
 */
export const uploadAgenticAttachment = (conversationId: string, file: File) => {
  const data = new FormData();
  data.append('file', file);
  return httpPost<AgenticAttachment>(`${API_AGENTIC_BASE}/attachment/upload`, data, {
    params: {conversation_id: conversationId},
    timeout: 0,
    headers: {'Content-Type': 'multipart/form-data'},
  });
};

/**
 * List agentic attachments.
 * @param conversationId - conversation id to scope the request
 * @returns the listed agentic attachments
 */
export const listAgenticAttachments = (conversationId: string) =>
  httpGet<AgenticAttachment[]>(`${API_AGENTIC_BASE}/attachment/list`, {
    params: {conversation_id: conversationId},
  });

/**
 * List pending agentic actions.
 * @param conversationId - conversation id to scope the request
 * @param query - page query with filters and paging
 * @returns the listed pending agentic actions
 */
export const listPendingAgenticActions = (conversationId: string, query?: Pick<PageQuery, 'offset' | 'limit'>) =>
  httpPost<PageResult<AgenticAction>>(`${API_AGENTIC_BASE}/action/pending`, {
    conversationId,
    offset: query?.offset ?? 0,
    limit: query?.limit ?? 50,
  });

/**
 * Confirm agentic action.
 * @param actionId - action id to scope the request
 * @returns the agentic action response
 */
export const confirmAgenticAction = (actionId: string) =>
  httpPost<AgenticAction>(`${API_AGENTIC_BASE}/action/confirm`, undefined, {params: {action_id: actionId}});

/**
 * Reject agentic action.
 * @param actionId - action id to scope the request
 * @returns the agentic action response
 */
export const rejectAgenticAction = (actionId: string) =>
  httpPost<AgenticAction>(`${API_AGENTIC_BASE}/action/reject`, undefined, {params: {action_id: actionId}});

/**
 * Stream agentic chat completion. The SSE parser reads data lines only, so the
 * server's named events (chat.completion.chunk, agentic.event, agentic.visualization)
 * and keep-alive comment frames are transparent to it. An omitted conversationId is
 * allowed: the server generates a fresh one (OpenAI stateless style) — pass a stable
 * id to keep server-side memory replay across turns.
 * @param data - payload to send or render
 * @param callbacks - callbacks entries
 */
export const streamAgenticChatCompletion = async (
  data: AgenticChatCompletionRequest,
  callbacks: AgenticStreamCallbacks = {}
) => {
  try {
    const response = await fetch(`/${API_AGENTIC_BASE}/chat/completions`, {
      method: 'post',
      credentials: 'include',
      signal: callbacks.signal,
      headers: buildFetchHeaders(),
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      await handleStreamHttpError(response);
    }
    if (!response.body) {
      throw new Error('Streaming response body is empty');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';
    let done = false;

    while (!done) {
      const result = await reader.read();
      done = result.done;
      buffer += decoder.decode(result.value || new Uint8Array(), {stream: !done});
      buffer = parseSseBuffer(buffer, callbacks);
    }

    parseSseBuffer(buffer, callbacks, true);
    callbacks.onDone?.();
  } catch (error) {
    const normalized = error instanceof Error ? error : new Error(String(error));
    callbacks.onError?.(normalized);
    throw normalized;
  }
};

/**
 * Complete agentic chat completion. An omitted conversationId is allowed: the server
 * generates a fresh one (OpenAI stateless style).
 * @param data - payload to send or render
 * @param signal - abort signal
 * @returns the operation result
 */
export const completeAgenticChatCompletion = async (
  data: AgenticChatCompletionRequest,
  signal?: AbortSignal
): Promise<AgenticChatCompletionResponse> => {
  const response = await fetch(`/${API_AGENTIC_BASE}/chat/completions`, {
    method: 'post',
    credentials: 'include',
    signal,
    headers: buildFetchHeaders(),
    body: JSON.stringify({...data, stream: false}),
  });

  if (!response.ok) {
    await handleStreamHttpError(response);
  }

  return (await response.json()) as AgenticChatCompletionResponse;
};

const buildFetchHeaders = (): HeadersInit => ({
  Accept: 'text/event-stream',
  'Content-Type': 'application/json',
  // Token travels in an httpOnly cookie via fetch credentials — not injected here.
  ...buildAuthHeaders(),
});

const handleStreamHttpError = async (response: Response): Promise<never> => {
  if (response.status === AXIOS_CONFIG.UNAUTHORIZED_STATUS) {
    // Same recovery as the axios interceptor: aggregated warning toast,
    // auth-session reset and a router redirect — one helper, both transports.
    handleAuthFailure();
  }

  const message = await response.text();
  throw new Error(message || `Agentic stream failed with status ${response.status}`);
};

const parseSseBuffer = (buffer: string, callbacks: AgenticStreamCallbacks, flush = false) => {
  const blocks = buffer.split(/\r?\n\r?\n/);
  const rest = flush ? '' : blocks.pop() || '';

  for (const block of blocks) {
    parseSseBlock(block, callbacks);
  }

  if (flush && rest) {
    parseSseBlock(rest, callbacks);
  }

  return rest;
};

const parseSseBlock = (block: string, callbacks: AgenticStreamCallbacks) => {
  const data = block
    .split(/\r?\n/)
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.replace(/^data:\s?/, ''))
    .join('\n')
    .trim();

  if (!data || data === '[DONE]') {
    return;
  }

  try {
    const chunk = JSON.parse(data) as OpenAIChunk;
    if (chunk.object === 'agentic.event' && chunk.type && chunk.title) {
      const event: AgenticTraceEvent = {
        id: `${chunk.type}-${chunk.created || Date.now()}-${Math.random().toString(16).slice(2)}`,
        type: chunk.type,
        title: chunk.title,
        detail: chunk.detail,
        name: chunk.name,
        phase: chunk.phase,
        status: chunk.status,
        code: chunk.code,
        created: chunk.created,
      };
      callbacks.onEvent?.(event);
      return;
    }
    if (chunk.object === 'agentic.visualization' && chunk.visualization) {
      callbacks.onVisualization?.(chunk.visualization);
      return;
    }
    const content = chunk.choices?.[0]?.delta?.content;
    if (content) {
      callbacks.onDelta?.(content);
    }
    const reasoningContent = chunk.choices?.[0]?.delta?.reasoning_content;
    if (reasoningContent) {
      callbacks.onReasoning?.(reasoningContent);
    }
    const finishReason = chunk.choices?.[0]?.finish_reason;
    if (finishReason) {
      callbacks.onFinish?.(finishReason);
    }
  } catch (error) {
    throw error instanceof Error ? error : new Error(String(error));
  }
};
