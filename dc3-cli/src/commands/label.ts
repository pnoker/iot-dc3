import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerLabelCommand(program: Command): void {
  const label = program.command('label').description('Label management');

  label
    .command('list')
    .description('List labels')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/manager/label/list', {
        page: { current: Number(opts.page), size: Number(opts.size) },
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
        `/api/v3/manager/label/get_by_id?id=${id}`,
      );
      printAndExit(result, format);
    });
}
