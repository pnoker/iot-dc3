import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerAlertCommand(program: Command): void {
  const alert = program.command('alert').description('Alarm/alert management');

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
    .option('--offset <n>', 'Zero-based result offset', '0')
    .option('--limit <n>', 'Maximum items to return', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      };
      if (opts.source) body.source = opts.source;
      const result = await dc3Client.post('/api/v3/data/dashboard/alert/page', body);
      printAndExit(result, format);
    });

  // dc3 alert latest
  alert
    .command('latest')
    .description('Latest alerts')
    .option('--limit <n>', 'Maximum number of alerts', '10')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/data/dashboard/alert/latest?limit=${opts.limit}`);
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
      const result = await dc3Client.get(`/api/v3/data/dashboard/alert/trend?days=${opts.days}`);
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

  // ---- Deep-analysis surface (added 2026-08): generic day/limit + kv passthrough ----

  const appendQuery = (base: string, opts: Record<string, unknown>): string => {
    const qs: string[] = [];
    if (opts.days !== undefined) qs.push(`days=${opts.days}`);
    if (opts.limit !== undefined) qs.push(`limit=${opts.limit}`);
    if (opts.baselineDays !== undefined) qs.push(`baseline_days=${opts.baselineDays}`);
    if (opts.silentMinutes !== undefined) qs.push(`silent_minutes=${opts.silentMinutes}`);
    // --query may arrive as one string or several values depending on usage.
    const kvs: string[] = Array.isArray(opts.query)
      ? (opts.query as string[])
      : typeof opts.query === 'string' && opts.query
        ? [opts.query]
        : [];
    for (const kv of kvs) {
      const eq = kv.indexOf('=');
      if (eq > 0) {
        qs.push(`${encodeURIComponent(kv.slice(0, eq))}=${encodeURIComponent(kv.slice(eq + 1))}`);
      }
    }
    return qs.length ? `${base}?${qs.join('&')}` : base;
  };

  const addGetAnalysis = (name: string, sub: string, description: string): void => {
    alert
      .command(name)
      .description(description)
      .option('--days <n>', 'Look-back window in days')
      .option('--limit <n>', 'Maximum rows to return')
      .option('--query <k=v>', 'Extra server query param (repeatable)')
      .option('--format <format>', 'Output format')
      .action(async (opts) => {
        const format = detectFormat(opts.format);
        const result = await dc3Client.get(
          appendQuery(`/api/v3/data/dashboard/alert/${sub}`, opts),
        );
        printAndExit(result, format);
      });
  };

  addGetAnalysis('activity', 'activity', 'Alert activity timeline');
  addGetAnalysis('storm-sources', 'storm_sources', 'Alert storm sources');
  addGetAnalysis('flapping', 'flapping', 'Flapping alerts analysis');
  addGetAnalysis('correlation', 'correlation', 'Correlated alert pairs');
  addGetAnalysis('peer-deviation', 'peer_deviation', 'Peer-deviation anomalies');
  addGetAnalysis('aging', 'aging', 'Unresolved alert ageing');
  addGetAnalysis('mtta', 'mtta', 'Mean-time-to-acknowledge metrics');

  // Non-alert-prefix variants
  alert
    .command('change-impact')
    .description('Change-impact analysis before/after config changes')
    .option('--days <n>', 'Look-back window in days')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        appendQuery('/api/v3/data/dashboard/alert/change_impact', opts),
      );
      printAndExit(result, format);
    });

  alert
    .command('latency')
    .description('Point-write latency histogram')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get('/api/v3/data/dashboard/stats/latency');
      printAndExit(result, format);
    });

  alert
    .command('silent-sources')
    .description('Data sources silent beyond a threshold')
    .option('--baseline-days <n>', 'Baseline window in days')
    .option('--silent-minutes <n>', 'Silence threshold in minutes')
    .option('--limit <n>', 'Maximum rows to return', '50')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        appendQuery('/api/v3/data/dashboard/silent/sources', opts),
      );
      printAndExit(result, format);
    });

  alert
    .command('coverage-gap')
    .description('Collection coverage gaps for points and devices')
    .option('--limit <n>', 'Maximum rows to return', '100')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/data/dashboard/coverage/gap?limit=${opts.limit}`);
      printAndExit(result, format);
    });

  alert
    .command('bulk-confirm')
    .description('Confirm many alerts at once')
    .requiredOption('--args <json>', 'Body as JSON, e.g. {"items":[{"source":"device","id":789}]}')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      let body: unknown;
      try {
        body = JSON.parse(opts.args);
      } catch {
        printAndExit({ ok: false, message: '--args is not valid JSON' }, 'json', 1);
        return;
      }
      const result = await dc3Client.post('/api/v3/data/dashboard/alert/bulk_confirm', body);
      printAndExit(result, format);
    });
}
