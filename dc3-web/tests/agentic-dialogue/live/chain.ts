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

import type {LiveRunConfig} from './sse';

/**
 * Gateway chain helpers for the live evaluation: session and message reads go
 * through the same REST endpoints the web client uses, so persistence is
 * verified against the real auth/manager/agentic path rather than in-memory
 * state.
 */

interface Envelope<T> {
  data?: T;
  items?: T;
}

const authHeaders = (config: LiveRunConfig): Record<string, string> => ({
  'Content-Type': 'application/json',
  ...(config.tenant ? {'X-Auth-Tenant': config.tenant} : {}),
  ...(config.login ? {'X-Auth-Login': config.login} : {}),
  ...(config.cookie ? {Cookie: config.cookie} : {}),
});

const unwrap = async <T>(response: Response): Promise<T> => {
  const text = await response.text();
  try {
    const body = JSON.parse(text) as Envelope<T>;
    if (body && typeof body === 'object' && 'data' in body) return body.data as T;
    return body as unknown as T;
  } catch {
    return text as unknown as T;
  }
};

export interface SessionRow {
  conversationId: string;
  title?: string;
}

export interface MessageRow {
  id: string;
  role: string;
  content: string;
  messageIndex?: number;
}

/**
 * List agentic sessions for the authenticated principal.
 * @param baseUrl - gateway base URL
 * @param config - run configuration carrying auth
 * @returns the session rows
 */
export const listSessions = async (baseUrl: string, config: LiveRunConfig): Promise<SessionRow[]> => {
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/v3/agentic/session/list`, {
    method: 'POST',
    headers: authHeaders(config),
    body: JSON.stringify({offset: 0, limit: 200}),
  });
  if (!response.ok) return [];
  const page = await unwrap<{items?: SessionRow[]} | SessionRow[]>(response);
  return Array.isArray(page) ? page : (page.items ?? []);
};

/**
 * List persisted messages of one conversation.
 * @param baseUrl - gateway base URL
 * @param config - run configuration carrying auth
 * @param conversationId - conversation to read back
 * @returns the message rows in index order
 */
export const listMessages = async (
  baseUrl: string,
  config: LiveRunConfig,
  conversationId: string
): Promise<MessageRow[]> => {
  const response = await fetch(
    `${baseUrl.replace(/\/$/, '')}/api/v3/agentic/message/list?conversation_id=${encodeURIComponent(conversationId)}`,
    {
      method: 'GET',
      headers: authHeaders(config),
    }
  );
  if (!response.ok) return [];
  const rows = await unwrap<MessageRow[] | {items?: MessageRow[]}>(response);
  return Array.isArray(rows) ? rows : (rows.items ?? []);
};
