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
import { readFileSync } from 'node:fs';
import { basename } from 'node:path';
import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

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
      let content: Buffer;
      try {
        content = readFileSync(file);
      } catch {
        printAndExit({ ok: false, message: `Cannot read file: ${file}` }, format, 1);
      }
      const result = await dc3Client.post(
        `${BASE}/upload?conversation_id=${encodeURIComponent(opts.conversationId)}`,
        content,
        { 'Content-Type': 'application/octet-stream', 'X-Filename': basename(file) },
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
      const result = await dc3Client.get(
        `${BASE}/list?conversation_id=${encodeURIComponent(opts.conversationId)}`,
      );
      printAndExit(result, format);
    });
}
