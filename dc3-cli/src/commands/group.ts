import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerGroupCommand(program: Command): void {
  const group = program.command('group').description('Group management');

  group
    .command('list')
    .description('List groups')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/manager/group/list', {
        page: { current: Number(opts.page), size: Number(opts.size) },
      });
      printAndExit(result, format);
    });

  group
    .command('get <id>')
    .description('Get group by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/manager/group/get_by_id?id=${id}`,
      );
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
