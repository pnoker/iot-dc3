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
import { readFileSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { ValidationError } from '../core/errors.js';
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
 * Read a JSON request body supplied inline (`--args`) or through a file
 * (`--args-file`, where `'-'` reads stdin). The two flags are mutually
 * exclusive and exactly one is required; every failure mode — unreadable
 * file, non-JSON content, non-object JSON — fails fast as a ValidationError
 * before any request is built, so a malformed payload can never reach the
 * wire (reports F032 and F034). Shared with `alert bulk-confirm`, which
 * exposes the same flag pair; lives here until a utils-layer owner extracts
 * a common module.
 * @param args - inline `--args` JSON text, when supplied
 * @param argsFile - `--args-file` path or `'-'` for stdin, when supplied
 * @returns the parsed JSON object body
 */
export async function readJsonObjectBody(
  args: string | undefined,
  argsFile: string | undefined,
): Promise<Record<string, unknown>> {
  if (args !== undefined && argsFile !== undefined) {
    throw new ValidationError('--args and --args-file are mutually exclusive; provide exactly one');
  }
  const fromFile = argsFile !== undefined;
  const source = fromFile ? await readArgsFile(argsFile) : args;
  if (source === undefined) {
    throw new ValidationError('one of --args or --args-file is required');
  }
  const flag = fromFile ? '--args-file' : '--args';
  let body: unknown;
  try {
    body = JSON.parse(source);
  } catch {
    throw new ValidationError(`${flag} is not valid JSON`);
  }
  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    throw new ValidationError(`${flag} must be a JSON object`);
  }
  return body as Record<string, unknown>;
}

/**
 * Read the raw text of an `--args-file` argument; `'-'` reads stdin through
 * file descriptor 0 (the piped-input channel — `readFileSync` is the only
 * fs API whose typings accept a numeric fd). Read failures surface the errno
 * code next to the user-supplied path — never the resolved absolute path.
 * @param path - user-supplied file path, or `'-'` for stdin
 * @returns the raw file content
 */
async function readArgsFile(path: string): Promise<string> {
  try {
    return path === '-' ? readFileSync(0, 'utf8') : await readFile(path, 'utf8');
  } catch (err) {
    const code = (err as { code?: unknown } | null)?.code;
    throw new ValidationError(
      `cannot read --args-file ${JSON.stringify(path)}${typeof code === 'string' ? ` (${code})` : ''}`,
    );
  }
}

/**
 * Analytics command group — the nine coarse-grained statistical reads the backend
 * exposes for agents (the S19 agent surface): each op posts a
 * single JSON body and returns one self-contained conclusion.
 * @param program - commander program to attach the command to
 */
export function registerAnalyticsCommand(program: Command): void {
  const analytics = program
    .command('analytics')
    .description('AI analytics over device point time series');

  analytics
    .command('list')
    .description('List available analytics operations')
    .option('--format <format>', 'Output format')
    .action((opts) => printAndExit({ operations: OPS }, detectFormat(opts.format)));

  analytics
    .command('run <op>')
    .description(`Run an analytics operation (${OPS.join(', ')})`)
    .option('--args <json>', 'Request body as a JSON object')
    .option(
      '--args-file <path>',
      "Request body read from a JSON file ('-' reads stdin); use for payloads beyond the argv size limit",
    )
    .option('--format <format>', 'Output format')
    .action(async (op: string, opts: { args?: string; argsFile?: string; format?: string }) => {
      const format = detectFormat(opts.format);
      if (!(OPS as readonly string[]).includes(op)) {
        throw new ValidationError(`Unknown operation: ${op}. Run: dc3 analytics list`);
      }
      const body = await readJsonObjectBody(opts.args, opts.argsFile);
      const result = await dc3Client.post(`/api/v3/data/analytics/${op}`, body);
      printAndExit(result, format);
    });
}
