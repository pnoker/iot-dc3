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
import { parseNonNegativeInteger, updateManagerResource } from '../utils/manager.js';

/**
 * Register the `label` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerLabelCommand(program: Command): void {
  const label = program.command('label').description('Label management');

  label
    .command('list')
    .description('List labels')
    .option('--offset <n>', 'Zero-based result offset', parseNonNegativeInteger, 0)
    .option('--limit <n>', 'Maximum items to return', parseNonNegativeInteger, 20)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/manager/label/list', {
        offset: opts.offset,
        limit: opts.limit,
      });
      printAndExit(result, format);
    });

  label
    .command('get <id>')
    .description('Get label by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/manager/label/get_by_id?id=${encodeURIComponent(id)}`,
      );
      printAndExit(result, format);
    });

  label
    .command('add')
    .alias('create')
    .description('Add a new label')
    .requiredOption('--name <name>', 'Label name')
    .option('--code <code>', 'Label code')
    .option('--color <color>', 'Label color (e.g. red, #ff0000)')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = { labelName: opts.name };
      if (opts.code !== undefined) body.labelCode = opts.code;
      if (opts.color !== undefined) body.labelColor = opts.color;
      const result = await dc3Client.post('/api/v3/manager/label/add', body);
      printAndExit(result, format);
    });

  label
    .command('update <id>')
    .description('Update a label')
    .requiredOption('--version <n>', 'Current version for optimistic locking', parseNonNegativeInteger)
    .option('--name <name>', 'New label name')
    .option('--code <code>', 'New label code')
    .option('--color <color>', 'New label color')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const changes: Record<string, unknown> = {};
      if (opts.name !== undefined) changes.labelName = opts.name;
      if (opts.code !== undefined) changes.labelCode = opts.code;
      if (opts.color !== undefined) changes.labelColor = opts.color;
      const result = await updateManagerResource(
        '/api/v3/manager/label',
        id,
        opts.version,
        changes,
      );
      printAndExit(result, format);
    });

  label
    .command('delete <id>')
    .description('Delete a label')
    .requiredOption('--version <n>', 'Current version for optimistic locking', parseNonNegativeInteger)
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      await dc3Client.del(
        `/api/v3/manager/label/delete?id=${encodeURIComponent(id)}&version=${opts.version}`,
      );
      printAndExit(undefined, format);
    });
}
