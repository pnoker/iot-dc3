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

export function registerGroupCommand(program: Command): void {
  const group = program.command('group').description('Group management');

  group
    .command('list')
    .description('List groups')
    .option('--offset <n>', 'Zero-based result offset', '0')
    .option('--limit <n>', 'Maximum items to return', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/manager/group/list', {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      });
      printAndExit(result, format);
    });

  group
    .command('get <id>')
    .description('Get group by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/manager/group/get_by_id?id=${id}`);
      printAndExit(result, format);
    });

  group
    .command('create')
    .description('Create a new group')
    .requiredOption('--name <name>', 'Group name')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/manager/group/add', {
        groupName: opts.name,
      });
      printAndExit(result, format);
    });
}
