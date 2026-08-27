import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerAlertCommand(program: Command): void {
  const alert = program
    .command('alert')
    .description('Alarm/alert management');

  // dc3 alert stats
  alert
    .command('stats')
    .description('Alert statistics overview')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get('/api/v3/data/dashboard/alert/stats');
      printAndExit(result, format);
    });

  // dc3 alert list
  alert
    .command('list')
    .description('List alerts')
    .option('--source <source>', 'Alert source: driver, device, point')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        page: { current: Number(opts.page), size: Number(opts.size) },
      };
      if (opts.source) body.source = opts.source;
      const result = await dc3Client.post(
        '/api/v3/data/dashboard/alert/page',
        body,
      );
      printAndExit(result, format);
    });

  // dc3 alert latest
  alert
    .command('latest')
    .description('Latest alerts')
    .option('--size <n>', 'Number of alerts', '10')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/data/dashboard/alert/latest?size=${opts.size}`,
      );
      printAndExit(result, format);
    });

  // dc3 alert confirm
  alert
    .command('confirm')
    .description('Confirm (acknowledge) an alert')
    .requiredOption('--source <source>', 'Alert source: driver, device, point')
    .requiredOption('--id <id>', 'Alert ID')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(
        `/api/v3/data/dashboard/alert/confirm?source=${opts.source}&id=${opts.id}`,
      );
      printAndExit(result, format);
    });

  // dc3 alert unconfirm
  alert
    .command('unconfirm')
    .description('Unconfirm an alert')
    .requiredOption('--source <source>', 'Alert source')
    .requiredOption('--id <id>', 'Alert ID')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(
        `/api/v3/data/dashboard/alert/unconfirm?source=${opts.source}&id=${opts.id}`,
      );
      printAndExit(result, format);
    });

  // dc3 alert trend
  alert
    .command('trend')
    .description('Alert trend over time')
    .option('--days <n>', 'Number of days', '30')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/data/dashboard/alert/trend?days=${opts.days}`,
      );
      printAndExit(result, format);
    });

  // dc3 alert top-sources
  alert
    .command('top-sources')
    .description('Top alert sources')
    .option('--days <n>', 'Number of days', '30')
    .option('--limit <n>', 'Max results', '10')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/data/dashboard/alert/top_sources?days=${opts.days}&limit=${opts.limit}`,
      );
      printAndExit(result, format);
    });

  // dc3 alert type-distribution
  alert
    .command('type-distribution')
    .description('Alert type distribution')
    .option('--days <n>', 'Number of days', '30')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/data/dashboard/alert/type_distribution?days=${opts.days}`,
      );
      printAndExit(result, format);
    });
}
