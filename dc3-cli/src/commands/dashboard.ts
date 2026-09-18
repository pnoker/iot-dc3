import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerDashboardCommand(program: Command): void {
  const dash = program.command('dashboard').description('Dashboard and monitoring');

  // dc3 dashboard stats
  dash
    .command('stats')
    .description('Today statistics')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get('/api/v3/data/dashboard/stats/today');
      printAndExit(result, format);
    });

  // dc3 dashboard timeseries
  dash
    .command('timeseries')
    .description('Time series data')
    .option('--granularity <g>', 'hour, day, week', 'hour')
    .option('--range-hours <n>', 'Hours to look back', '24')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/data/dashboard/stats/timeseries?granularity=${opts.granularity}&range_hours=${opts.rangeHours}`,
      );
      printAndExit(result, format);
    });

  // dc3 dashboard top
  dash
    .command('top')
    .description('Top entities by dimension')
    .option('--dimension <dim>', 'device, driver, point', 'device')
    .option('--range-hours <n>', 'Hours to look back', '24')
    .option('--limit <n>', 'Max results', '10')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/data/dashboard/top?dimension=${opts.dimension}&range_hours=${opts.rangeHours}&limit=${opts.limit}`,
      );
      printAndExit(result, format);
    });

  // dc3 dashboard topology
  dash
    .command('topology')
    .description('System topology')
    .option('--mode <mode>', 'Topology mode', 'cardinality')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/data/dashboard/topology?mode=${opts.mode}`);
      printAndExit(result, format);
    });

  // dc3 dashboard health
  dash
    .command('health')
    .description('System health check')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const [sysHealth, protocolHealth] = await Promise.all([
        dc3Client.get('/api/v3/data/dashboard/system/health'),
        dc3Client.get('/api/v3/data/dashboard/protocol/health'),
      ]);
      printAndExit({ system: sysHealth, protocol: protocolHealth }, format);
    });

  // dc3 dashboard stream
  dash
    .command('stream')
    .description('Latest point value stream')
    .option('--limit <n>', 'Maximum number of records', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/data/dashboard/stream?limit=${opts.limit}`);
      printAndExit(result, format);
    });

  // dc3 dashboard driver-stats
  dash
    .command('driver-stats')
    .description('Driver statistics from manager')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get('/api/v3/manager/dashboard/driver/stats');
      printAndExit(result, format);
    });

  // dc3 dashboard device-stats
  dash
    .command('device-stats')
    .description('Device statistics from manager')
    .option('--top-n <n>', 'Top N devices', '10')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/manager/dashboard/device/stats?top_n=${opts.topN}`,
      );
      printAndExit(result, format);
    });
}
