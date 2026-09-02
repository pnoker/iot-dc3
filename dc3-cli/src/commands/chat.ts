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
