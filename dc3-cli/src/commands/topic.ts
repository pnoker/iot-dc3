import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerTopicCommand(program: Command): void {
  const topic = program.command('topic').description('MQTT topic management (read-only)');

  topic
    .command('list')
    .description('List MQTT topics')
    .option('--offset <n>', 'Zero-based result offset', '0')
    .option('--limit <n>', 'Maximum items to return', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/manager/topic/list', {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      });
      printAndExit(result, format);
    });
}
