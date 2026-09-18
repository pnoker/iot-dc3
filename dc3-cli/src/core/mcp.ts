import { configManager } from './config-manager.js';
import { tokenManager } from './token-manager.js';
import { AuthError } from './client.js';

/**
 * Thin MCP JSON-RPC client over the gateway's POST /mcp endpoint.
 *
 * Second transport of the dual-transport CLI design
 * (docs/design/token-unification-mcp-first-cli.md §4 Option B): the same OAuth ticket
 * stored by `dc3 auth login --oauth` is presented as Bearer here, while REST commands
 * keep their own transport. Requires an oauth-type login — classic login tickets are
 * not introspectable at this endpoint.
 */
export class McpClient {
  private async rpc<T = unknown>(method: string, params?: unknown): Promise<T> {
    const profile = await configManager.getActiveProfile();
    const gateway = profile.gateway.replace(/\/+$/, '');
    const profileName = (await configManager.load()).current_profile;
    const state = await tokenManager.getState(profileName);
    if (!state || state.authType !== 'oauth') {
      throw new AuthError('MCP endpoint requires an OAuth ticket. Run: dc3 auth login --oauth');
    }
    const headers = {
      'Content-Type': 'application/json',
      Accept: 'application/json, text/event-stream',
      ...tokenManager.buildHeaders(state), // Authorization: Bearer …
    };
    const res = await fetch(`${gateway}/mcp`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        jsonrpc: '2.0',
        id: Date.now(),
        method,
        params: params ?? {},
      }),
    });
    if (res.status === 401) {
      throw new AuthError('Unauthorized at /mcp (401). Ticket may lack mcp scopes or be expired.');
    }
    if (!res.ok) {
      throw new Error(`MCP request failed (${res.status}): ${await res.text()}`);
    }
    const payload = (await res.json()) as {
      jsonrpc: string;
      id?: number | string | null;
      result?: T;
      error?: { code: number; message: string };
    };
    if (payload.error) {
      throw new Error(`MCP error ${payload.error.code}: ${payload.error.message}`);
    }
    return payload.result as T;
  }

  /**
   * List tools visible to this ticket's scopes.
   */
  listTools(): Promise<{ tools?: Array<Record<string, unknown>> }> {
    return this.rpc('tools/list');
  }

  /**
   * Invoke a tool; args become params.arguments per the MCP spec.
   */
  callTool(toolName: string, args: Record<string, unknown>): Promise<unknown> {
    return this.rpc('tools/call', { name: toolName, arguments: args });
  }
}

/** Singleton instance */
export const mcpClient = new McpClient();
