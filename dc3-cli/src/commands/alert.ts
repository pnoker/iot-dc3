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
import { Command, InvalidArgumentError } from 'commander';
import { dc3Client } from '../core/client.js';
import { ValidationError } from '../core/errors.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import { parseNonNegativeInteger, parsePositiveInteger, requireResourceId } from '../utils/manager.js';
import { readJsonObjectBody } from './analytics.js';

/** Cohort range windows the point-profile backend accepts (audit G31). */
const POINT_PROFILE_RANGE_KEYS = ['today', '24h', '7d', '30d'] as const;

/**
 * Collector for the repeatable `--query` flag: commander passes the
 * accumulated array as `previous`, so every occurrence appends instead of
 * the default last-write-wins (report F019).
 * @param value - one `key=value` occurrence
 * @param previous - values collected so far
 * @returns the accumulated list
 */
const collectQuery = (value: string, previous: string[] = []): string[] => previous.concat(value);

/**
 * Validate the point-profile cohort range key, mirroring the shared parser
 * style (audit G31).
 * @param value - raw --range-key option lexeme; must be one of the cohort
 * keys verbatim (case-sensitive)
 * @returns the validated cohort key, unchanged
 */
const parsePointProfileRangeKey = (value: string): string => {
  if (!POINT_PROFILE_RANGE_KEYS.includes(value as (typeof POINT_PROFILE_RANGE_KEYS)[number])) {
    throw new InvalidArgumentError(`allowed: ${POINT_PROFILE_RANGE_KEYS.join(', ')}`);
  }
  return value;
};

/**
 * Append the numeric/range flags and every `--query` occurrence to a
 * read-plane base path through URLSearchParams, so user values are always
 * encoded and can never inject extra gateway parameters (`&`), truncate the
 * query (`#`), or smuggle structure (`=`) — report F017. A malformed
 * `key=value` pair fails fast as a ValidationError before any request is
 * built (report F019).
 * @param base - gateway-relative path, with or without an existing query string
 * @param opts - command options carrying days/limit/baselineDays/silentMinutes/rangeKey/query
 * @returns the path with its encoded query string appended
 */
const appendQuery = (base: string, opts: Record<string, unknown>): string => {
  const params = new URLSearchParams();
  if (opts.days !== undefined) params.set('days', String(opts.days));
  if (opts.limit !== undefined) params.set('limit', String(opts.limit));
  if (opts.baselineDays !== undefined) params.set('baseline_days', String(opts.baselineDays));
  if (opts.silentMinutes !== undefined) params.set('silent_minutes', String(opts.silentMinutes));
  if (opts.rangeKey !== undefined) params.set('range_key', String(opts.rangeKey));
  const queries = Array.isArray(opts.query) ? (opts.query as string[]) : [];
  for (const kv of queries) {
    const eq = kv.indexOf('=');
    if (eq <= 0) {
      throw new ValidationError(`--query expects <key>=<value>, got: ${JSON.stringify(kv)}`);
    }
    params.append(kv.slice(0, eq), kv.slice(eq + 1));
  }
  const qs = params.toString();
  if (!qs) return base;
  return base.includes('?') ? `${base}&${qs}` : `${base}?${qs}`;
};

/**
 * Register the `alert` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
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
    .option('--offset <n>', 'Zero-based result offset', parseNonNegativeInteger, 0)
    .option('--limit <n>', 'Maximum items to return', parseNonNegativeInteger, 20)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        offset: opts.offset,
        limit: opts.limit,
      };
      if (opts.source !== undefined) body.source = opts.source;
      const result = await dc3Client.post('/api/v3/data/dashboard/alert/page', body);
      printAndExit(result, format);
    });

  // dc3 alert latest
  alert
    .command('latest')
    .description('Latest alerts')
    .option('--limit <n>', 'Maximum number of alerts', parsePositiveInteger, 10)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(appendQuery('/api/v3/data/dashboard/alert/latest', opts));
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
      // The alert id keys the confirmation — reject empty ids before the wire
      // (audit G13/G27).
      requireResourceId('--id', opts.id);
      const result = await dc3Client.post(
        `/api/v3/data/dashboard/alert/confirm?source=${encodeURIComponent(opts.source)}&id=${encodeURIComponent(opts.id)}`,
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
      // Same empty-id contract as confirm (audit G13/G27).
      requireResourceId('--id', opts.id);
      const result = await dc3Client.post(
        `/api/v3/data/dashboard/alert/unconfirm?source=${encodeURIComponent(opts.source)}&id=${encodeURIComponent(opts.id)}`,
      );
      printAndExit(result, format);
    });

  // dc3 alert trend
  alert
    .command('trend')
    .description('Alert trend over time')
    .option('--days <n>', 'Number of days', parsePositiveInteger, 30)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(appendQuery('/api/v3/data/dashboard/alert/trend', opts));
      printAndExit(result, format);
    });

  // dc3 alert top-sources
  alert
    .command('top-sources')
    .description('Top alert sources')
    .option('--days <n>', 'Number of days', parsePositiveInteger, 30)
    .option('--limit <n>', 'Max results', parsePositiveInteger, 10)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        appendQuery('/api/v3/data/dashboard/alert/top_sources', opts),
      );
      printAndExit(result, format);
    });

  // dc3 alert type-distribution
  alert
    .command('type-distribution')
    .description('Alert type distribution')
    .option('--days <n>', 'Number of days', parsePositiveInteger, 30)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        appendQuery('/api/v3/data/dashboard/alert/type_distribution', opts),
      );
      printAndExit(result, format);
    });

  // ---- Deep-analysis surface (added 2026-08): generic day/limit + kv passthrough ----

  const addGetAnalysis = (name: string, sub: string, description: string): void => {
    alert
      .command(name)
      .description(description)
      .option('--days <n>', 'Look-back window in days', parsePositiveInteger)
      .option('--limit <n>', 'Maximum rows to return', parsePositiveInteger)
      .option('--query <k=v>', 'Extra server query param (repeatable)', collectQuery, [] as string[])
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
    .option('--days <n>', 'Look-back window in days', parsePositiveInteger)
    .option('--query <k=v>', 'Extra server query param (repeatable)', collectQuery, [] as string[])
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
    .option('--baseline-days <n>', 'Baseline window in days', parsePositiveInteger)
    .option('--silent-minutes <n>', 'Silence threshold in minutes', parsePositiveInteger)
    .option('--limit <n>', 'Maximum rows to return', parsePositiveInteger, 50)
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
    .option('--limit <n>', 'Maximum rows to return', parsePositiveInteger, 100)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(appendQuery('/api/v3/data/dashboard/coverage/gap', opts));
      printAndExit(result, format);
    });

  alert
    .command('bulk-confirm')
    .description('Confirm many alerts at once')
    .option('--args <json>', 'Body as a JSON object, e.g. {"items":[{"source":"device","id":789}]}')
    .option(
      '--args-file <path>',
      "Body read from a JSON file ('-' reads stdin); use for payloads beyond the argv size limit",
    )
    .option('--format <format>', 'Output format')
    .action(async (opts: { args?: string; argsFile?: string; format?: string }) => {
      const format = detectFormat(opts.format);
      const body = await readJsonObjectBody(opts.args, opts.argsFile);
      const result = await dc3Client.post('/api/v3/data/dashboard/alert/bulk_confirm', body);
      printAndExit(result, format);
    });

  // dc3 alert point-profile <point_id>
  alert
    .command('point-profile <point_id>')
    .description('Alert profile for a single point across its peer cohort')
    .option('--days <n>', 'Look-back window in days', parsePositiveInteger)
    .option(
      '--range-key <key>',
      'Cohort range window: today, 24h, 7d, or 30d',
      parsePointProfileRangeKey,
    )
    .option('--format <format>', 'Output format')
    .action(async (pointId, opts) => {
      const format = detectFormat(opts.format);
      // The point id keys the profile lookup — reject empty ids before the
      // wire (audit G13/G27).
      requireResourceId('/api/v3/data/dashboard/alert/point_profile', pointId);
      const result = await dc3Client.get(
        appendQuery(
          `/api/v3/data/dashboard/alert/point_profile?point_id=${encodeURIComponent(pointId)}`,
          opts,
        ),
      );
      printAndExit(result, format);
    });
}
