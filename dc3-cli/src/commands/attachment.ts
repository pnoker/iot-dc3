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
import { readFile } from 'node:fs/promises';
import { basename } from 'node:path';
import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { ValidationError } from '../core/errors.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import { requireResourceId } from '../utils/manager.js';

const BASE = '/api/v3/agentic/attachment';

/**
 * Register the `attachment` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerAttachmentCommand(program: Command): void {
  const attachment = program.command('attachment').description('Chat attachment management');

  attachment
    .command('upload <file>')
    .description('Upload a file as a chat attachment')
    .requiredOption('--conversation-id <id>', 'Conversation to attach the file to')
    .option('--format <format>', 'Output format')
    .action(async (file, opts) => {
      const format = detectFormat(opts.format);
      // The conversation id keys the upload — reject empty ids before any
      // file read or request (audit G13/G27).
      requireResourceId('--conversation-id', opts.conversationId);
      let content: Buffer;
      try {
        content = await readFile(file);
      } catch {
        throw new ValidationError(`Cannot read file: ${file}`);
      }
      if (content.length === 0) {
        throw new ValidationError('Attachment file must not be empty');
      }
      // The backend's AttachmentController expects a multipart/form-data `file`
      // part (Spring @RequestPart, filename read from the part disposition) —
      // never a JSON-serialized buffer (report F003). postForm lets fetch own
      // the multipart boundary; the file name rides the disposition.
      const form = new FormData();
      form.append('file', new Blob([content]), basename(file));
      const result = await dc3Client.postForm(
        `${BASE}/upload?conversation_id=${encodeURIComponent(opts.conversationId)}`,
        form,
      );
      printAndExit(result, format);
    });

  attachment
    .command('list')
    .description('List attachments for a conversation')
    .requiredOption('--conversation-id <id>', 'Conversation ID')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      // Same empty-id contract as upload (audit G13/G27).
      requireResourceId('--conversation-id', opts.conversationId);
      const result = await dc3Client.get(
        `${BASE}/list?conversation_id=${encodeURIComponent(opts.conversationId)}`,
      );
      printAndExit(result, format);
    });

  attachment
    .command('delete <id>')
    .description('Delete a chat attachment')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      // The id keys the delete — reject empty ids before the wire (audit
      // G13/G27).
      requireResourceId(`${BASE}/delete`, id);
      // Pinned wire contract (mirrors model delete / ModelController.delete):
      // DELETE /attachment/delete?id=<id> answered by 204 with an empty body.
      await dc3Client.del(`${BASE}/delete?id=${encodeURIComponent(id)}`);
      printAndExit(undefined, format);
    });
}
