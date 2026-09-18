import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerLabelCommand(program: Command): void {
  const label = program.command('label').description('Label management');

  label
    .command('list')
    .description('List labels')
    .option('--offset <n>', 'Zero-based result offset', '0')
    .option('--limit <n>', 'Maximum items to return', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/manager/label/list', {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      });
      printAndExit(result, format);
    });

  label
    .command('get <id>')
    .description('Get label by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/manager/label/get_by_id?id=${id}`);
      printAndExit(result, format);
    });
}
