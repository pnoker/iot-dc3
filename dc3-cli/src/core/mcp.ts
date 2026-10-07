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
import { configManager } from './config-manager.js';
import { tokenManager } from './token-manager.js';
import { ApiError, AuthError } from './errors.js';
import { fetchOrNetworkError, normalizeGateway, readBodyText } from './http.js';

/**
 * Thin MCP JSON-RPC client over the gateway's POST /mcp endpoint.
 *
 * Second transport of the dual-transport CLI design
 * (token-unification Phase 4, Option B): the same OAuth ticket
 * stored by `dc3 auth login --oauth` is presented as Bearer here, while REST commands
 * keep their own transport. Requires an oauth-type login — classic login tickets are
 * not introspectable at this endpoint.
 */

/** Decoded JSON-RPC response payload. */
interface RpcPayload<T> {
  jsonrpc: string;
  id?: number | string | null;
  result?: T;
  error?: { code: number; message: string };
}

/**
 * Monotonic JSON-RPC request id. Wall-clock ids (Date.now) collide within one
 * millisecond, making responses unattributable (report F040).
 */
let nextRpcId = 1;

/** Maximum number of body characters embedded in a decode diagnostic. */
const SNIPPET_LENGTH = 120;

/**
 * Whether the value is a non-array object (a candidate JSON-RPC envelope).
 * @param value - decoded value to shape-check
 * @returns true when the value is a record
 */
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Assert that a decoded /mcp body is a JSON-RPC response envelope: a record
 * carrying a `result` or `error` member. A 2xx body of valid JSON that is not
 * an RPC envelope (health pings, bare strings/arrays/null) would otherwise
 * flow downstream and crash at the call site with a raw TypeError instead of
 * a typed failure.
 * @param payload - decoded body or SSE frame to validate
 * @param snippet - raw text excerpt for the diagnostic
 * @returns the payload, narrowed to an RPC envelope
 */
function assertRpcEnvelope<T>(payload: unknown, snippet: string): RpcPayload<T> {
  if (!isRecord(payload) || !('result' in payload || 'error' in payload)) {
    throw new ApiError(
      'MCP response is not a JSON-RPC envelope (expected an object with result or error): ' +
        `${snippet.trim().slice(0, SNIPPET_LENGTH)}`,
      undefined,
      undefined,
      'MCP_BAD_BODY',
    );
  }
  return payload as unknown as RpcPayload<T>;
}

/**
 * Decode the data frames of an SSE body into JSON-RPC payloads. Frames are
 * separated by blank lines; every `data:` field line of a frame contributes to
 * its payload (joined per the SSE spec). Comment/keep-alive blocks without a
 * data field, data payloads that are not valid JSON, and frames that decode
 * to non-object values are skipped — a stream with zero decodable frames
 * fails loudly in the caller.
 * @param text - raw text/event-stream body
 * @returns the decoded JSON-RPC payloads in stream order
 */
function parseSseJsonRpcFrames<T>(text: string): Array<RpcPayload<T>> {
  const frames: Array<RpcPayload<T>> = [];
  for (const block of text.split(/\r?\n\r?\n/u)) {
    const data = block
      .split(/\r?\n/u)
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice('data:'.length).replace(/^ /u, ''))
      .join('\n');
    if (data.trim() === '') {
      continue;
    }
    let parsed: unknown;
    try {
      parsed = JSON.parse(data);
    } catch {
      // Skip non-JSON frames (server keep-alives, comments).
      continue;
    }
    // A JSON-RPC frame is always an object; scalars/arrays/null are not
    // candidate responses even though they are valid JSON.
    if (isRecord(parsed)) {
      frames.push(parsed as unknown as RpcPayload<T>);
    }
  }
  return frames;
}

/**
 * MCP client: one JSON-RPC method call per gateway POST, with SSE-aware
 * response decoding and per-request id correlation.
 */
export class McpClient {
  private async rpc<T = unknown>(method: string, params?: unknown): Promise<T> {
    const profile = await configManager.getActiveProfile();
    const gateway = normalizeGateway(profile.gateway);
    const profileName = await configManager.getActiveProfileName();
    const state = await tokenManager.getState(profileName);
    if (!state || state.authType !== 'oauth') {
      throw new AuthError('MCP endpoint requires an OAuth ticket. Run: dc3 auth login --oauth');
    }
    const headers = {
      'Content-Type': 'application/json',
      Accept: 'application/json, text/event-stream',
      ...tokenManager.buildHeaders(state), // Authorization: Bearer …
    };
    const id = nextRpcId++;
    const res = await fetchOrNetworkError(`${gateway}/mcp`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        jsonrpc: '2.0',
        id,
        method,
        params: params ?? {},
      }),
    });
    if (res.status === 401) {
      throw new AuthError('Unauthorized at /mcp (401). Ticket may lack mcp scopes or be expired.');
    }
    if (!res.ok) {
      const text = await readBodyText(res);
      const detail = text.trim() === '' ? '(empty body)' : text.trim();
      throw new ApiError(`MCP request failed (${res.status}): ${detail}`, res.status);
    }
    const payload = await this.decodeRpcResponse<T>(res, id);
    if (payload.error) {
      throw new ApiError(
        `MCP error ${payload.error.code}: ${payload.error.message}`,
        undefined,
        payload.error,
        'MCP_RPC_ERROR',
      );
    }
    return payload.result as T;
  }

  /**
   * Decode a 2xx /mcp reply per its Content-Type (report F022): the Accept
   * header invites both application/json and SSE-framed JSON-RPC, so
   * text/event-stream bodies are decoded frame by frame and the response
   * matching this request id is selected; unknown content types fail with an
   * explicit diagnostic instead of a raw JSON SyntaxError. Whatever branch
   * decodes, the result must be a JSON-RPC envelope (a record with `result`
   * or `error`) — a valid-JSON non-RPC 2xx body fails with MCP_BAD_BODY
   * instead of crashing the caller.
   * @param res - gateway response to decode
   * @param id - request id the response must match
   * @returns the JSON-RPC payload for this request
   */
  private async decodeRpcResponse<T>(res: Response, id: number): Promise<RpcPayload<T>> {
    const contentType = (res.headers.get('content-type') ?? '').toLowerCase();
    const text = await readBodyText(res);

    if (contentType.includes('text/event-stream')) {
      const frames = parseSseJsonRpcFrames<T>(text);
      if (frames.length === 0) {
        throw new ApiError(
          'MCP endpoint returned an SSE stream with no JSON-RPC response frames',
          undefined,
          undefined,
          'MCP_BAD_BODY',
        );
      }
      const matched = frames.filter((frame) => frame.id === id);
      if (matched.length > 0) {
        return assertRpcEnvelope(matched[0], JSON.stringify(matched[0]) ?? '');
      }
      // Single-frame streams from servers that omit the id: accept it. A
      // multi-frame stream where nothing matches is a real misattribution.
      if (frames.length === 1) {
        return assertRpcEnvelope(frames[0], JSON.stringify(frames[0]) ?? '');
      }
      throw new ApiError(
        `MCP response id mismatch (expected ${id}, got ${frames.map((f) => String(f.id)).join(', ')})`,
        undefined,
        undefined,
        'MCP_ID_MISMATCH',
      );
    }

    if (text.trim() === '') {
      throw new ApiError('MCP endpoint returned an empty body', undefined, undefined, 'MCP_BAD_BODY');
    }
    let payload: RpcPayload<T>;
    try {
      payload = JSON.parse(text) as RpcPayload<T>;
    } catch {
      if (contentType === '' || contentType.includes('json')) {
        throw new ApiError(
          `MCP response is not valid JSON (Content-Type: ${contentType || 'unknown'}): ` +
            `${text.trim().slice(0, SNIPPET_LENGTH)}`,
          undefined,
          undefined,
          'MCP_BAD_BODY',
        );
      }
      throw new ApiError(
        `Unsupported /mcp response content-type: ${contentType}`,
        undefined,
        undefined,
        'MCP_UNSUPPORTED_CONTENT_TYPE',
      );
    }
    // Envelope shape first: the id check below dereferences the payload, and a
    // valid-JSON null/scalar body must fail typed, not as a TypeError.
    assertRpcEnvelope(payload, text);
    if (payload.id !== undefined && payload.id !== null && payload.id !== id) {
      throw new ApiError(
        `MCP response id mismatch (expected ${id}, got ${String(payload.id)})`,
        undefined,
        undefined,
        'MCP_ID_MISMATCH',
      );
    }
    return payload;
  }

  /**
   * List tools visible to this ticket's scopes.
   * @returns the tools visible to this ticket's scopes
   */
  listTools(): Promise<{ tools?: Array<Record<string, unknown>> }> {
    return this.rpc('tools/list');
  }

  /**
   * Invoke a tool; args become params.arguments per the MCP spec.
   * @param toolName - tool name used for the lookup
   * @param args - tool arguments forwarded as params.arguments
   * @returns the raw tool result payload
   */
  callTool(toolName: string, args: Record<string, unknown>): Promise<unknown> {
    return this.rpc('tools/call', { name: toolName, arguments: args });
  }
}

/** Singleton instance */
export const mcpClient = new McpClient();
