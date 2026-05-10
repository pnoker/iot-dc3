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

export interface AgenticSkill {
  name: string;
  description?: string;
}

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
  defaultFlag?: number;
  enableFlag?: number;
  remark?: string;
}

export interface AgenticModelConfig extends AgenticModel {
  id?: string;
  providerId?: string;
  providerName?: string;
  defaultFlag?: number;
  enableFlag?: number;
  remark?: string;
  createTime?: string;
  operateTime?: string;
}

export interface AgenticSession {
  conversationId: string;
  title?: string;
  createTime?: string;
  operateTime?: string;
}

export type AgenticMessageRole = 'user' | 'assistant' | 'system';

export interface AgenticMessage {
  id: string;
  role: AgenticMessageRole;
  content: string;
  contentExt?: AgenticMessageContent;
  model?: string;
  skills?: string[];
  messageIndex?: number;
  status?: number;
  streaming?: boolean;
  reasoning?: string;
  createTime?: string;
}

export interface AgenticMessageContent {
  text?: string;
  format?: string;
  attachments?: number[];
  skills?: string[];
  tools?: string[];
  contexts?: AgenticMessageContext[];
  tokens?: AgenticMessageTokens;
  reasoning?: boolean;
  directContextProvided?: boolean;
}

export interface AgenticMessageContext {
  type: 'backend' | 'attachment' | 'retrieval' | 'memory' | 'tool' | 'system';
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
  conversationId?: string;
  skill?: string;
  attachments?: number[];
  reasoning?: boolean;
  confirmActions?: boolean;
}

export interface AgenticStreamCallbacks {
  signal?: AbortSignal;
  onDelta?: (content: string) => void;
  onEvent?: (event: AgenticTraceEvent) => void;
  onDone?: () => void;
  onError?: (error: Error) => void;
}

export interface AgenticTraceEvent {
  id?: string;
  type: 'skill' | 'tools' | 'tool' | 'reasoning' | 'error';
  title: string;
  detail?: string;
  name?: string;
  created?: number;
}
