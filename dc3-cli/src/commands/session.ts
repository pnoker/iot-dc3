import { Command } from 'commander';
import { dc3Client, AuthError } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

/**
 * Session & action plane of the agentic center: conversation lifecycle plus the
 * high-risk tool-call approval loop (docs/design/token-unification-mcp-first-cli.md
 * Q2 — CLI TTY confirmation channel). All routes live under /api/v3/agentic.
 */
export function registerSessionCommand(program: Command): void {
  const session = program
    .command('session')
    .description('Agentic conversation sessions');

  // dc3 session list
  session
    .command('list')
    .description('List conversations for the current principal')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/agentic/session/list', {
        page: { current: Number(opts.page), size: Number(opts.size) },
      });
      printAndExit(result, format);
    });

  // dc3 session get
  session
    .command('get <conversation_id>')
    .description('Fetch one conversation by id')
    .option('--format <format>', 'Output format')
    .action(async (id: string, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/agentic/session/get_by_conversation_id?conversation_id=${encodeURIComponent(id)}`,
      );
      printAndExit(result, format);
    });

  // dc3 session messages
  session
    .command('messages <conversation_id>')
    .description('List messages of a conversation')
    .option('--format <format>', 'Output format')
    .action(async (id: string, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/agentic/message/list?conversation_id=${encodeURIComponent(id)}`,
      );
      printAndExit(result, format);
    });

  // dc3 session rename
  session
    .command('rename <conversation_id>')
    .description('Update an editable field (name) of a conversation')
    .requiredOption('--name <name>', 'New conversation name')
    .option('--format <format>', 'Output format')
    .action(async (id: string, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.request(
        'POST',
        `/api/v3/agentic/session/update?conversation_id=${encodeURIComponent(id)}`,
        { name: opts.name },
      );
      printAndExit(result, format);
    });

  // dc3 session delete
  session
    .command('delete <conversation_id>')
    .description('Delete a conversation with its message history')
    .option('--format <format>', 'Output format')
    .action(async (id: string, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.request(
        'POST',
        `/api/v3/agentic/session/delete?conversation_id=${encodeURIComponent(id)}`,
      );
      printAndExit(result, format);
    });
}

export function registerActionCommand(program: Command): void {
  const action = program
    .command('action')
    .description(
      'Pending agent tool calls and their approval loop (high-risk step-up)',
    );

  const act = (
    sub: 'confirm' | 'reject',
    description: string,
  ): void => {
    action
      .command(`${sub} <action_id>`)
      .description(description)
      .option('--format <format>', 'Output format')
      .action(async (actionId: string, opts) => {
        const format = detectFormat(opts.format);
        try {
          const result = await dc3Client.request(
            'POST',
            `/api/v3/agentic/action/${sub}?action_id=${encodeURIComponent(actionId)}`,
          );
          printAndExit(result, format);
        } catch (err) {
          if (err instanceof AuthError) {
            printAndExit({ ok: false, message: err.message }, 'json', 3);
          }
          throw err;
        }
      });
  };

  // dc3 action pending --conversation-id
  action
    .command('pending')
    .description('List tool calls awaiting approval for one conversation')
    .requiredOption('--conversation-id <id>', 'Conversation identifier')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      try {
        const result = await dc3Client.get(
          `/api/v3/agentic/action/pending?conversation_id=${encodeURIComponent(opts.conversationId)}`,
        );
        printAndExit(result, format);
      } catch (err) {
        if (err instanceof AuthError) {
          printAndExit({ ok: false, message: err.message }, 'json', 3);
        }
        throw err;
      }
    });

  act('confirm', 'Approve a pending tool call');
  act('reject', 'Reject a pending tool call');
}
