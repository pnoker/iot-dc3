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

export interface AgenticModel {
  model: string;
  label: string;
  stream: boolean;
  toolCall: boolean;
  vision: boolean;
  reasoning: boolean;
  temperature?: number;
  maxTokens?: number;
}

export interface AgenticProvider {
  id?: string;
  name: string;
  providerType: string;
  baseUrl: string;
  defaultFlag?: string;
  enableFlag?: string;
  remark?: string;
  createTime?: string;
  operateTime?: string;
}

export interface AgenticModelConfig {
  /** Inlined from AgenticModel. */
  model: string;
  /** Inlined from AgenticModel. */
  label: string;
  /** Inlined from AgenticModel. */
  stream: boolean;
  /** Inlined from AgenticModel. */
  toolCall: boolean;
  /** Inlined from AgenticModel. */
  vision: boolean;
  /** Inlined from AgenticModel. */
  reasoning: boolean;
  /** Inlined from AgenticModel. */
  temperature?: number;
  /** Inlined from AgenticModel. */
  maxTokens?: number;
  id?: string;
  providerId?: string;
  providerName?: string;
  defaultFlag?: string;
  enableFlag?: string;
  remark?: string;
  createTime?: string;
  operateTime?: string;
}

export interface AgenticSession {
  conversationId: string;
  title?: string;
  sessionExt?: AgenticSessionExt;
  createTime?: string;
  operateTime?: string;
}

export interface AgenticSessionExt {
  model?: string;
  reasoningEnabled?: boolean;
  temperature?: number;
  maxTokens?: number;
}

export type AgenticMessageRole = 'user' | 'assistant' | 'system';

export interface AgenticMessage {
  id: string;
  role: AgenticMessageRole;
  content: string;
  contentExt?: AgenticMessageContent;
  model?: string;
  messageIndex?: number;
  status?: number;
  streaming?: boolean;
  reasoning?: string;
  finishReason?: string;
  createTime?: string;
}

export interface AgenticMessageContent {
  text?: string;
  format?: string;
  attachments?: number[];
  tools?: string[];
  traces?: AgenticTraceEvent[];
  charts?: AgenticVisualizationSpec[];
  contexts?: AgenticMessageContext[];
  tokens?: AgenticMessageTokens;
  reasoning?: boolean;
  reasoningContent?: string;
}

export type AgenticVisualizationType =
  'line' | 'area' | 'column' | 'bar' | 'pie' | 'donut' | 'heatmap' | 'scatter' | 'stat';

export interface AgenticVisualizationSpec {
  id?: string;
  type: AgenticVisualizationType;
  title?: string;
  description?: string;
  dataset: Array<Record<string, unknown>>;
  encode: {
    x?: string;
    y?: string;
    color?: string;
    size?: string;
    shape?: string;
  };
  scale?: Record<string, unknown>;
  meta?: Record<string, unknown>;
  annotations?: AgenticVisualizationAnnotation[];
}

export interface AgenticVisualizationAnnotation {
  type?: string;
  value?: unknown;
  label?: string;
}

export interface AgenticMessageContext {
  type: 'attachment' | 'retrieval' | 'memory' | 'tool' | 'system';
  content: string;
}

export interface AgenticMessageTokens {
  input?: number;
  output?: number;
  text?: number;
  context?: number;
  system?: number;
  memory?: number;
}

export interface AgenticAttachment {
  id: number;
  conversationId: string;
  fileName: string;
  contentType: string;
  size: number;
  filePath?: string;
  createTime?: string;
}

export interface AgenticAction {
  id?: string;
  actionId: string;
  conversationId: string;
  actionType: string;
  title: string;
  description: string;
  payload: Record<string, unknown>;
  status: number;
  expireTime?: string;
  remark?: string;
}

export interface AgenticChatMessage {
  role: AgenticMessageRole;
  content: string;
}

export interface AgenticChatCompletionRequest {
  model?: string;
  messages: AgenticChatMessage[];
  temperature?: number;
  maxTokens?: number;
  stream: boolean;
  /**
   * Stable client-generated conversation ID. The backend uses this to replay chat
   * memory across turns, so callers MUST reuse the same value for follow-up
   * messages and only generate a fresh UUID when starting a new conversation.
   * Leaving this empty is rejected by the backend with HTTP 400.
   */
  conversationId: string;
  attachments?: number[];
  reasoning?: boolean;
}

export interface AgenticChatCompletionResponse {
  id?: string;
  object?: string;
  created?: number;
  model?: string;
  choices?: Array<{
    index?: number;
    message?: {
      role?: AgenticMessageRole;
      content?: string;
      contentExt?: AgenticMessageContent;
      content_ext?: AgenticMessageContent;
    };
    finishReason?: string;
    finish_reason?: string;
  }>;
  usage?: {
    promptTokens?: number;
    completionTokens?: number;
    totalTokens?: number;
    prompt_tokens?: number;
    completion_tokens?: number;
    total_tokens?: number;
  };
}

export interface AgenticStreamCallbacks {
  signal?: AbortSignal;
  onDelta?: (content: string) => void;
  onReasoning?: (content: string) => void;
  onEvent?: (event: AgenticTraceEvent) => void;
  onVisualization?: (visualization: AgenticVisualizationSpec) => void;
  onFinish?: (reason: string) => void;
  onDone?: () => void;
  onError?: (error: Error) => void;
}

export interface AgenticTraceEvent {
  id?: string;
  type: 'tool' | 'reasoning' | 'error';
  title: string;
  detail?: string;
  name?: string;
  phase?: 'start' | 'result' | 'error';
  status?: 'running' | 'success' | 'empty' | 'failed';
  code?: string;
  created?: number;
}
