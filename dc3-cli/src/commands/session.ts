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
import { parseNonNegativeInteger, parsePositiveInteger, requireResourceId } from '../utils/manager.js';

/**
 * Session & action plane of the agentic center: conversation lifecycle plus the
 * high-risk tool-call approval loop (docs/design/token-unification-mcp-first-cli.md
 * Q2 — CLI TTY confirmation channel). All routes live under /api/v3/agentic.
 *
 * Destructive operations (session delete, action confirm/reject) are gated by
 * the TTY confirmation channel; `--yes` skips the prompt for scripts, CI, and
 * non-interactive agents, and a declined confirmation exits before any request
 * is built (report F045). Every id-keyed subcommand rejects an empty or
 * whitespace id as kind validation BEFORE the gate and the wire — the shared
 * requireResourceId contract (report F053).
 * @param program - commander program to attach the command to
 */
export function registerSessionCommand(program: Command): void {
  const session = program.command('session').description('Agentic conversation sessions');

  // dc3 session list
  session
    .command('list')
    .description('List conversations for the current principal')
    .option('--offset <n>', 'Zero-based result offset', parseNonNegativeInteger, 0)
    .option('--limit <n>', 'Maximum items to return', parseNonNegativeInteger, 20)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/agentic/session/list', {
        offset: opts.offset,
        limit: opts.limit,
      });
      printAndExit(result, format);
    });

  // dc3 session get
  session
    .command('get <conversation_id>')
    .description('Fetch one conversation by id')
    .option('--format <format>', 'Output format')
    .action(async (id: string, opts) => {
      requireResourceId('session get <conversation_id>', id);
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
      requireResourceId('session messages <conversation_id>', id);
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
      requireResourceId('session rename <conversation_id>', id);
      const format = detectFormat(opts.format);
      const result = await dc3Client.request(
        'POST',
        `/api/v3/agentic/session/update?conversation_id=${encodeURIComponent(id)}`,
        { title: opts.name },
      );
      printAndExit(result, format);
    });

  // dc3 session delete
  session
    .command('delete <conversation_id>')
    .description('Delete a conversation with its message history')
    .option('--yes', 'Skip the confirmation prompt (scripts, CI, non-interactive agents)')
    .option('--format <format>', 'Output format')
    .action(async (id: string, opts: { yes?: boolean; format?: string }) => {
      // Validate before the TTY gate: an empty id must fail as kind validation
      // without prompting, mirroring attachment/chat (report F053).
      requireResourceId('session delete <conversation_id>', id);
      const format = detectFormat(opts.format);
      if (!opts.yes) {
        const { confirm } = await import('../utils/prompt.js');
        const ok = await confirm(`Delete conversation "${id}" with its message history?`);
        if (!ok) {
          printAndExit({ ok: true, message: 'Cancelled' }, format);
        }
      }
      const result = await dc3Client.request(
        'DELETE',
        `/api/v3/agentic/session/delete?conversation_id=${encodeURIComponent(id)}`,
      );
      printAndExit(result, format);
    });
}

/**
 * Register the `action` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerActionCommand(program: Command): void {
  const action = program
    .command('action')
    .description('Pending agent tool calls and their approval loop (high-risk step-up)');

  const act = (sub: 'confirm' | 'reject', description: string): void => {
    action
      .command(`${sub} <action_id>`)
      .description(description)
      .option('--yes', 'Skip the confirmation prompt (scripts, CI, non-interactive agents)')
      .option('--format <format>', 'Output format')
      .action(async (actionId: string, opts: { yes?: boolean; format?: string }) => {
        // Validate before the TTY gate: an empty action id must fail as kind
        // validation without prompting or issuing a request (report F053).
        requireResourceId(`action ${sub} <action_id>`, actionId);
        const format = detectFormat(opts.format);
        if (!opts.yes) {
          const { confirm } = await import('../utils/prompt.js');
          const ok = await confirm(
            `${sub === 'confirm' ? 'Approve' : 'Reject'} pending action "${actionId}"?`,
          );
          if (!ok) {
            printAndExit({ ok: true, message: 'Cancelled' }, format);
          }
        }
        const result = await dc3Client.request(
          'POST',
          `/api/v3/agentic/action/${sub}?action_id=${encodeURIComponent(actionId)}`,
        );
        printAndExit(result, format);
      });
  };

  // dc3 action pending --conversation-id
  action
    .command('pending')
    .description('List tool calls awaiting approval for one conversation')
    .requiredOption('--conversation-id <id>', 'Conversation identifier')
    .option('--offset <n>', 'Zero-based result offset', parseNonNegativeInteger)
    .option('--limit <n>', 'Maximum results to return', parsePositiveInteger)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      // Same contract as attachment/chat: an explicitly empty conversation id
      // is invalid input, never `?conversation_id=` on the wire.
      requireResourceId('--conversation-id', opts.conversationId);
      const format = detectFormat(opts.format);
      const params = new URLSearchParams({ conversation_id: opts.conversationId });
      if (opts.offset !== undefined) params.set('offset', String(opts.offset));
      if (opts.limit !== undefined) params.set('limit', String(opts.limit));
      const result = await dc3Client.get(`/api/v3/agentic/action/pending?${params.toString()}`);
      printAndExit(result, format);
    });

  act('confirm', 'Approve a pending tool call');
  act('reject', 'Reject a pending tool call');
}
