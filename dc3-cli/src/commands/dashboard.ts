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
import { Command, Option } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import { parsePositiveInteger } from '../utils/manager.js';

/**
 * Append a query string built through URLSearchParams, so every flag value is
 * encoded and can never inject extra gateway parameters (`&`), truncate the
 * query (`#`), or smuggle structure (`=`) — report F017.
 * @param base - gateway-relative path without a query string
 * @param params - query parameters to set, undefined values skipped
 * @returns the path with its encoded query string appended
 */
const withQuery = (base: string, params: Record<string, string | number | undefined>): string => {
  const qs = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined) qs.set(key, String(value));
  }
  const encoded = qs.toString();
  return encoded ? `${base}?${encoded}` : base;
};

/**
 * Register the `dashboard` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
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
    .addOption(
      new Option('--granularity <g>', 'Time bucket granularity: hour groups by hour-of-day, day by calendar date')
        .choices(['hour', 'day'])
        .default('hour'),
    )
    .option('--range-hours <n>', 'Hours to look back', parsePositiveInteger, 24)
    .option('--format <format>', 'Output format')
    .action(async (opts: { granularity: string; rangeHours: number; format?: string }) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        withQuery('/api/v3/data/dashboard/stats/timeseries', {
          granularity: opts.granularity,
          range_hours: opts.rangeHours,
        }),
      );
      printAndExit(result, format);
    });

  // dc3 dashboard top
  dash
    .command('top')
    .description('Top entities by dimension')
    .addOption(
      new Option('--dimension <dim>', 'Ranking dimension: device, driver, or point')
        .choices(['device', 'driver', 'point'])
        .default('device'),
    )
    .option('--range-hours <n>', 'Hours to look back', parsePositiveInteger, 24)
    .option('--limit <n>', 'Max results', parsePositiveInteger, 10)
    .option('--format <format>', 'Output format')
    .action(async (opts: { dimension: string; rangeHours: number; limit: number; format?: string }) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        withQuery('/api/v3/data/dashboard/top', {
          dimension: opts.dimension,
          range_hours: opts.rangeHours,
          limit: opts.limit,
        }),
      );
      printAndExit(result, format);
    });

  // dc3 dashboard topology
  dash
    .command('topology')
    .description('System topology')
    .addOption(
      new Option('--mode <mode>', 'Aggregation mode: cardinality counts per edge, volume weights by samples')
        .choices(['cardinality', 'volume'])
        .default('cardinality'),
    )
    .option('--format <format>', 'Output format')
    .action(async (opts: { mode: string; format?: string }) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        withQuery('/api/v3/data/dashboard/topology', { mode: opts.mode }),
      );
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
    .option('--limit <n>', 'Maximum number of records', parsePositiveInteger, 20)
    .option('--format <format>', 'Output format')
    .action(async (opts: { limit: number; format?: string }) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        withQuery('/api/v3/data/dashboard/stream', { limit: opts.limit }),
      );
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
    .option('--top-n <n>', 'Top N devices', parsePositiveInteger, 10)
    .option('--format <format>', 'Output format')
    .action(async (opts: { topN: number; format?: string }) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        withQuery('/api/v3/manager/dashboard/device/stats', { top_n: opts.topN }),
      );
      printAndExit(result, format);
    });
}
