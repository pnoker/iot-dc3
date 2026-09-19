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
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

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
      if (!prompt && !opts.conversationId) {
        printAndExit(
          {
            ok: false,
            message: 'Please provide a prompt or --conversation-id to continue',
          },
          'json',
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
        // For streaming, we need a raw fetch to handle SSE
        const { configManager } = await import('../core/config-manager.js');
        const { tokenManager } = await import('../core/token-manager.js');
        const profile = await configManager.getActiveProfile();
        const profileName = (await configManager.load()).current_profile;
        const state = await tokenManager.getState(profileName);
        const headers = state
          ? tokenManager.buildHeaders(state)
          : { 'Content-Type': 'application/json' };

        const url = `${profile.gateway}/api/v3/agentic/chat/completions`;
        const res = await fetch(url, {
          method: 'POST',
          headers,
          body: JSON.stringify(body),
        });

        if (!res.ok) {
          const err = await res.text();
          printAndExit({ ok: false, message: err }, 'json', 1);
        }

        // Stream SSE to stdout
        const reader = res.body?.getReader();
        if (!reader) {
          printAndExit({ ok: false, message: 'No response body' }, 'json', 1);
        }

        const decoder = new TextDecoder();
        let buffer = '';
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
        process.stdout.write('\n');
        process.exit(0);
      } else {
        const format = detectFormat(opts.format);
        const result = await dc3Client.post('/api/v3/agentic/chat/completions', body);
        printAndExit(result, format);
      }
    });
}
