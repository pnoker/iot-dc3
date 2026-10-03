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
import { Command } from 'commander';
import { dc3Client, AuthError, NetworkError } from '../core/client.js';
import { configManager } from '../core/config-manager.js';
import { tokenManager } from '../core/token-manager.js';
import { fetchOrNetworkError, normalizeGateway } from '../core/http.js';
import { detectFormat, printAndExit } from '../utils/format.js';

/**
 * Register the `chat` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerChatCommand(program: Command): void {
  const chat = program.command('chat').description('AI chat (agentic service)');

  // dc3 chat <prompt>
  chat
    .argument('[prompt]', 'Chat prompt (natural language)')
    .description('Send a prompt to the AI agent')
    .option('-m, --model <model>', 'Model name (e.g., gpt-4o)')
    .option('--stream', 'Stream the response (SSE)', false)
    .option('--conversation-id <id>', 'Continue an existing conversation')
    .option('--format <format>', 'Output format (non-streaming only)')
    .action(async (prompt, opts) => {
      const format = detectFormat(opts.format);
      if (!prompt && !opts.conversationId) {
        printAndExit(
          {
            ok: false,
            message: 'Please provide a prompt or --conversation-id to continue',
          },
          format,
          1,
        );
      }

      const conversationId = opts.conversationId || undefined;
      const body: Record<string, unknown> = {
        stream: opts.stream,
        messages: prompt ? [{ role: 'user', content: prompt }] : [],
      };
      if (conversationId) body.conversationId = conversationId;
      if (opts.model) body.model = opts.model;

      if (opts.stream) {
        // Streaming needs direct access to the response body for SSE, so it
        // cannot go through Dc3Client.request — but it shares the same
        // gateway-normalization and transport-error seam.
        const profile = await configManager.getActiveProfile();
        const profileName = await configManager.getActiveProfileName();
        const state = await tokenManager.getState(profileName);
        const headers = state
          ? tokenManager.buildHeaders(state)
          : { 'Content-Type': 'application/json' };

        // The stream legitimately runs longer than any fixed request timeout, and
        // Node's fetch offers no headers-only timeout on a single call (an
        // AbortSignal fires while the body is still being consumed), so the
        // streaming request carries no timeout by design.
        const url = `${normalizeGateway(profile.gateway)}/api/v3/agentic/chat/completions`;
        const res = await fetchOrNetworkError(
          url,
          {
            method: 'POST',
            headers,
            body: JSON.stringify(body),
          },
          null,
        );

        if (res.status === 401) {
          throw new AuthError(`Authentication failed (401): ${await res.text()}`);
        }
        if (!res.ok) {
          printAndExit({ ok: false, message: await res.text() }, format, 1);
        }

        // Stream SSE to stdout
        const reader = res.body?.getReader();
        if (!reader) {
          printAndExit({ ok: false, message: 'No response body' }, format, 1);
        }

        const decoder = new TextDecoder();
        let buffer = '';
        try {
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';
            for (const line of lines) {
              if (line.startsWith('data: ')) {
                const data = line.slice(6);
                if (data === '[DONE]') {
                  process.stdout.write('\n');
                  break;
                }
                try {
                  const parsed = JSON.parse(data);
                  const content = parsed.choices?.[0]?.delta?.content || '';
                  if (content) {
                    process.stdout.write(content);
                  }
                } catch {
                  // Skip unparseable SSE data
                }
              }
            }
          }
        } catch (error) {
          // A mid-stream reader rejection would otherwise escape uncaught;
          // surface it as a network failure (exit 2).
          throw new NetworkError(`Chat stream interrupted: ${(error as Error).message}`);
        }
        process.stdout.write('\n');
        process.exit(0);
      } else {
        const result = await dc3Client.post('/api/v3/agentic/chat/completions', body);
        printAndExit(result, format);
      }
    });
}
