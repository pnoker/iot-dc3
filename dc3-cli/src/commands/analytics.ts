import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

const OPS = [
  'query_latest',
  'query_history',
  'compute_stats',
  'compare_periods',
  'rank_entities',
  'trend_analysis',
  'threshold_report',
  'correlate',
  'data_quality_report',
] as const;

/**
 * Analytics command group — the nine coarse-grained statistical reads the backend
 * exposes for agents (docs/design/tsdb-abstraction.md S19 surface): each op posts a
 * single JSON body and returns one self-contained conclusion.
 */
export function registerAnalyticsCommand(program: Command): void {
  const analytics = program
    .command('analytics')
    .description('AI analytics over device point time series');

  analytics
    .command('list')
    .description('List available analytics operations')
    .action(() => printAndExit({ operations: OPS }, 'json'));

  analytics
    .command('run <op>')
    .description(`Run an analytics operation (${OPS.join(', ')})`)
    .requiredOption('--args <json>', 'Request body as JSON')
    .option('--format <format>', 'Output format')
    .action(async (op: string, opts: { args: string; format?: string }) => {
      const format = detectFormat(opts.format);
      if (!(OPS as readonly string[]).includes(op)) {
        printAndExit(
          { ok: false, message: `Unknown operation: ${op}. Run: dc3 analytics list` },
          'json',
          1,
        );
        return;
      }
      let body: unknown;
      try {
        body = JSON.parse(opts.args);
      } catch {
        printAndExit({ ok: false, message: '--args is not valid JSON' }, 'json', 1);
        return;
      }
      const result = await dc3Client.post(`/api/v3/data/analytics/${op}`, body);
      printAndExit(result, format);
    });
}
