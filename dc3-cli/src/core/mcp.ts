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
import { AuthError } from './errors.js';
import { fetchOrNetworkError, normalizeGateway, readBodyText } from './http.js';

/**
 * Thin MCP JSON-RPC client over the gateway's POST /mcp endpoint.
 *
 * Second transport of the dual-transport CLI design
 * (docs/design/token-unification-mcp-first-cli.md §4 Option B): the same OAuth ticket
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
 * Decode the data frames of an SSE body into JSON-RPC payloads. Frames are
 * separated by blank lines; every `data:` field line of a frame contributes to
 * its payload (joined per the SSE spec). Comment/keep-alive blocks without a
 * data field, and data payloads that are not valid JSON, are skipped — a
 * stream with zero decodable frames fails loudly in the caller.
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
    try {
      frames.push(JSON.parse(data) as RpcPayload<T>);
    } catch {
      // Skip non-JSON frames (server keep-alives, comments).
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
      throw new Error(`MCP request failed (${res.status}): ${detail}`);
    }
    const payload = await this.decodeRpcResponse<T>(res, id);
    if (payload.error) {
      throw new Error(`MCP error ${payload.error.code}: ${payload.error.message}`);
    }
    return payload.result as T;
  }

  /**
   * Decode a 2xx /mcp reply per its Content-Type (report F022): the Accept
   * header invites both application/json and SSE-framed JSON-RPC, so
   * text/event-stream bodies are decoded frame by frame and the response
   * matching this request id is selected; unknown content types fail with an
   * explicit diagnostic instead of a raw JSON SyntaxError.
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
        throw new Error('MCP endpoint returned an SSE stream with no JSON-RPC response frames');
      }
      const matched = frames.filter((frame) => frame.id === id);
      if (matched.length > 0) {
        return matched[0];
      }
      // Single-frame streams from servers that omit the id: accept it. A
      // multi-frame stream where nothing matches is a real misattribution.
      if (frames.length === 1) {
        return frames[0];
      }
      throw new Error(
        `MCP response id mismatch (expected ${id}, got ${frames.map((f) => String(f.id)).join(', ')})`,
      );
    }

    if (text.trim() === '') {
      throw new Error('MCP endpoint returned an empty body');
    }
    let payload: RpcPayload<T>;
    try {
      payload = JSON.parse(text) as RpcPayload<T>;
    } catch {
      if (contentType === '' || contentType.includes('json')) {
        throw new Error(
          `MCP response is not valid JSON (Content-Type: ${contentType || 'unknown'}): ` +
            `${text.trim().slice(0, SNIPPET_LENGTH)}`,
        );
      }
      throw new Error(`Unsupported /mcp response content-type: ${contentType}`);
    }
    if (payload.id !== undefined && payload.id !== null && payload.id !== id) {
      throw new Error(`MCP response id mismatch (expected ${id}, got ${String(payload.id)})`);
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
