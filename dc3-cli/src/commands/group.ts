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
