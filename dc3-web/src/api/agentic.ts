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

import {httpGet, httpPost} from '@/api/common';
import {API_AGENTIC_BASE} from '@/config/constant/api';
import {AUTH_HEADERS} from '@/config/constant/common';
import type {
  AgenticAction,
  AgenticAttachment,
  AgenticChatCompletionRequest,
  AgenticChatCompletionResponse,
  AgenticMessage,
  AgenticModel,
  AgenticModelConfig,
  AgenticProvider,
  AgenticSession,
  AgenticStreamCallbacks,
  AgenticTraceEvent,
  AgenticVisualizationSpec,
  PageQuery,
  PageResult,
} from '@/config/types';
import {getStorage} from '@/utils/storageUtil';
import {isNull} from '@/utils/validationUtil';

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

export const listAgenticModels = () => httpGet<R<AgenticModel[]>>(`${API_AGENTIC_BASE}/model/list`);

export const listAgenticModelConfigs = () => httpGet<R<AgenticModelConfig[]>>(`${API_AGENTIC_BASE}/model/config/list`);

export const addAgenticModelConfig = (data: AgenticModelConfig) =>
  httpPost<R<AgenticModelConfig>>(`${API_AGENTIC_BASE}/model/config/add`, data);

export const updateAgenticModelConfig = (data: AgenticModelConfig) =>
  httpPost<R<AgenticModelConfig>>(`${API_AGENTIC_BASE}/model/config/update`, data);

export const deleteAgenticModelConfig = (id: string) =>
  httpPost<R<boolean>>(`${API_AGENTIC_BASE}/model/config/delete`, undefined, {params: {id}});

export const listAgenticProviders = () => httpGet<R<AgenticProvider[]>>(`${API_AGENTIC_BASE}/provider/list`);

export const addAgenticProvider = (data: AgenticProvider) =>
  httpPost<R<AgenticProvider>>(`${API_AGENTIC_BASE}/provider/config/add`, data);

export const updateAgenticProvider = (data: AgenticProvider) =>
  httpPost<R<AgenticProvider>>(`${API_AGENTIC_BASE}/provider/config/update`, data);

export const deleteAgenticProvider = (id: string) =>
  httpPost<R<boolean>>(`${API_AGENTIC_BASE}/provider/config/delete`, undefined, {params: {id}});

export const listAgenticSessions = (query?: PageQuery) =>
  httpPost<R<PageResult<AgenticSession>>>(`${API_AGENTIC_BASE}/session/list`, query ?? {});

export const deleteAgenticSession = (conversationId: string) =>
  httpPost<R<boolean>>(`${API_AGENTIC_BASE}/session/delete`, undefined, {
    params: {conversation_id: conversationId},
  });

export const updateAgenticSession = (
  conversationId: string,
  data: Partial<Pick<AgenticSession, 'title' | 'sessionExt'>>
) =>
  httpPost<R<AgenticSession>>(`${API_AGENTIC_BASE}/session/update`, data, {
    params: {conversation_id: conversationId},
  });

export const listAgenticMessages = (conversationId: string) =>
  httpGet<R<AgenticMessage[]>>(`${API_AGENTIC_BASE}/message/list`, {params: {conversation_id: conversationId}});

export const uploadAgenticAttachment = (conversationId: string, file: File) => {
  const data = new FormData();
  data.append('file', file);
  return httpPost<R<AgenticAttachment>>(`${API_AGENTIC_BASE}/attachment/upload`, data, {
    params: {conversation_id: conversationId},
    timeout: 0,
    headers: {'Content-Type': 'multipart/form-data'},
  });
};

export const listAgenticAttachments = (conversationId: string) =>
  httpGet<R<AgenticAttachment[]>>(`${API_AGENTIC_BASE}/attachment/list`, {
    params: {conversation_id: conversationId},
  });

export const listPendingAgenticActions = (conversationId: string) =>
  httpGet<R<AgenticAction[]>>(`${API_AGENTIC_BASE}/action/pending`, {params: {conversation_id: conversationId}});

export const confirmAgenticAction = (actionId: string) =>
  httpPost<R<AgenticAction>>(`${API_AGENTIC_BASE}/action/confirm`, undefined, {params: {action_id: actionId}});

export const rejectAgenticAction = (actionId: string) =>
  httpPost<R<AgenticAction>>(`${API_AGENTIC_BASE}/action/reject`, undefined, {params: {action_id: actionId}});

export const streamAgenticChatCompletion = async (
  data: AgenticChatCompletionRequest,
  callbacks: AgenticStreamCallbacks = {}
) => {
  if (!data.conversationId) {
    throw new Error('conversationId is required — generate a stable UUID per conversation before calling.');
  }
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

export const completeAgenticChatCompletion = async (
  data: AgenticChatCompletionRequest,
  signal?: AbortSignal
): Promise<AgenticChatCompletionResponse> => {
  if (!data.conversationId) {
    throw new Error('conversationId is required — generate a stable UUID per conversation before calling.');
  }

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

const buildFetchHeaders = (): HeadersInit => {
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
    'Content-Type': 'application/json',
  };

  const tenant = getStorage(AUTH_HEADERS.TENANT);
  if (!isNull(tenant)) {
    headers[AUTH_HEADERS.TENANT] = String(tenant);
  }

  const login = getStorage(AUTH_HEADERS.LOGIN);
  if (!isNull(login)) {
    headers[AUTH_HEADERS.LOGIN] = String(login);
  }

  const token = getStorage(AUTH_HEADERS.TOKEN);
  if (!isNull(token)) {
    headers[AUTH_HEADERS.TOKEN] = JSON.stringify(token);
  }

  return headers;
};

const handleStreamHttpError = async (response: Response): Promise<never> => {
  if (response.status === 401) {
    localStorage.clear();
    sessionStorage.clear();
    window.location.hash = '#/login';
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
