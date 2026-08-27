import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerDriverCommand(program: Command): void {
  const driver = program
    .command('driver')
    .description('Driver management');

  driver
    .command('list')
    .description('List drivers')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/manager/driver/list', {
        page: { current: Number(opts.page), size: Number(opts.size) },
      });
      printAndExit(result, format);
    });

  driver
    .command('get <id>')
    .description('Get driver by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/manager/driver/get_by_id?id=${id}`);
      printAndExit(result, format);
    });

  driver
    .command('status <id>')
    .description('Get driver status')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/data/driver/status/list', { id });
      printAndExit(result, format);
    });
}
