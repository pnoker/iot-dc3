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
import { InvalidArgumentError } from 'commander';
import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { ApiError } from '../core/errors.js';
import { detectFormat, printAndExit } from '../utils/format.js';

const BASE = '/api/v3/agentic/model';

/**
 * Fields a model-config update may carry — the same set the add action sends.
 * Gateway-only and audit fields (providerName, tenant, last-check telemetry)
 * are never echoed back in an update body (report F043).
 */
const MODEL_UPDATE_FIELDS = [
  'model',
  'label',
  'providerId',
  'stream',
  'toolCall',
  'vision',
  'reasoning',
  'temperature',
  'maxTokens',
  'defaultFlag',
  'enableFlag',
] as const;

const parseTemperature = (value: string): number => {
  const parsed = parseFloat(value);
  if (Number.isNaN(parsed)) {
    throw new InvalidArgumentError(`option '--temperature ${value}' is not a valid number`);
  }
  if (parsed < 0 || parsed > 2) {
    throw new InvalidArgumentError(`option '--temperature ${value}' is out of range (0.0–2.0)`);
  }
  return parsed;
};

const parseMaxTokens = (value: string): number => {
  const parsed = parseInt(value, 10);
  if (Number.isNaN(parsed) || parsed < 1) {
    throw new InvalidArgumentError(`option '--max-tokens ${value}' must be a positive integer`);
  }
  return parsed;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Unwrap a gateway list response into its rows: a bare array passes through,
 * the common envelope shapes ({data:[...]}, {items:[...]}) are unwrapped, and
 * an unrecognizable shape returns undefined so the caller fails with a
 * distinct shape error instead of misreporting every entry as missing
 * (report F043).
 * @param payload - decoded model config list response
 * @returns the row records, or undefined when the shape is unrecognizable
 */
function listRowsOf(payload: unknown): Array<Record<string, unknown>> | undefined {
  let rows: unknown;
  if (Array.isArray(payload)) {
    rows = payload;
  } else if (isRecord(payload)) {
    rows = ['data', 'items'].map((key) => payload[key]).find((value) => Array.isArray(value));
  }
  if (!Array.isArray(rows)) {
    return undefined;
  }
  return rows.filter(isRecord);
}

/**
 * Register the `model` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerModelCommand(program: Command): void {
  const model = program.command('model').description('AI model configuration management');

  model
    .command('list')
    .description('List available AI models (lightweight options for chat)')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`${BASE}/list`);
      printAndExit(result, format);
    });

  model
    .command('config-list')
    .description('List full AI model configurations')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`${BASE}/config/list`);
      printAndExit(result, format);
    });

  model
    .command('add')
    .description('Add a new AI model configuration')
    .requiredOption('--model <model>', 'Model identifier (e.g. gpt-4o, deepseek-chat)')
    .requiredOption('--provider-id <id>', 'Provider ID this model belongs to')
    .option('--label <label>', 'Display label (defaults to the model id)')
    .option('--stream', 'Enable streaming responses (default)', true)
    .option('--no-stream', 'Disable streaming responses')
    .option('--tool-call', 'Enable tool calling (default)', true)
    .option('--no-tool-call', 'Disable tool calling')
    .option('--vision', 'Enable vision input')
    .option('--no-vision', 'Disable vision input (default)')
    .option('--reasoning', 'Enable reasoning mode')
    .option('--no-reasoning', 'Disable reasoning mode (default)')
    .option('--temperature <t>', 'Sampling temperature (0.0-2.0)', parseTemperature)
    .option('--max-tokens <n>', 'Maximum output tokens', parseMaxTokens)
    .option('--default', 'Set as the tenant default model')
    .option('--enable', 'Enable the model (default)')
    .option('--disable', 'Disable the model')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        model: opts.model,
        providerId: opts.providerId,
        label: opts.label || opts.model,
        stream: Boolean(opts.stream),
        toolCall: Boolean(opts.toolCall),
        vision: Boolean(opts.vision),
        reasoning: Boolean(opts.reasoning),
      };
      if (opts.temperature !== undefined) body.temperature = opts.temperature;
      if (opts.maxTokens !== undefined) body.maxTokens = opts.maxTokens;
      body.defaultFlag = opts.default ? 'DEFAULT' : 'NOT_DEFAULT';
      body.enableFlag = opts.disable ? 'DISABLE' : 'ENABLE';
      const result = await dc3Client.post(`${BASE}/config/add`, body);
      printAndExit(result, format);
    });

  model
    .command('update <id>')
    .description('Update an AI model configuration')
    .option('--model <model>', 'New model identifier')
    .option('--label <label>', 'New display label')
    .option('--provider-id <id>', 'New provider ID')
    .option('--stream', 'Enable streaming')
    .option('--no-stream', 'Disable streaming')
    .option('--tool-call', 'Enable tool calling')
    .option('--no-tool-call', 'Disable tool calling')
    .option('--vision', 'Enable vision')
    .option('--no-vision', 'Disable vision')
    .option('--reasoning', 'Enable reasoning')
    .option('--no-reasoning', 'Disable reasoning')
    .option('--temperature <t>', 'New sampling temperature (0.0-2.0)', parseTemperature)
    .option('--max-tokens <n>', 'New maximum output tokens', parseMaxTokens)
    .option('--default', 'Set as tenant default')
    .option('--enable', 'Enable the model')
    .option('--disable', 'Disable the model')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const payload = await dc3Client.get(`${BASE}/config/list`);
      const rows = listRowsOf(payload);
      if (rows === undefined) {
        throw new ApiError(
          `Model config list response has an unrecognized shape (expected an array or a {data:[...]} envelope); ` +
            `refusing to update model config ${id}`,
        );
      }
      const current = rows.find((c) => String(c.id) === String(id));
      if (!current) {
        throw new ApiError(`Model config ${id} not found`);
      }
      const changes: Record<string, unknown> = {};
      if (opts.model !== undefined) changes.model = opts.model;
      if (opts.label !== undefined) changes.label = opts.label;
      if (opts.providerId !== undefined) changes.providerId = opts.providerId;
      if (opts.stream !== undefined) changes.stream = Boolean(opts.stream);
      if (opts.toolCall !== undefined) changes.toolCall = Boolean(opts.toolCall);
      if (opts.vision !== undefined) changes.vision = Boolean(opts.vision);
      if (opts.reasoning !== undefined) changes.reasoning = Boolean(opts.reasoning);
      if (opts.temperature !== undefined) changes.temperature = opts.temperature;
      if (opts.maxTokens !== undefined) changes.maxTokens = opts.maxTokens;
      if (opts.default) changes.defaultFlag = 'DEFAULT';
      if (opts.enable) changes.enableFlag = 'ENABLE';
      if (opts.disable) changes.enableFlag = 'DISABLE';
      // Add-time field whitelist: unchanged whitelisted fields keep their
      // stored values; everything else the list happened to carry (provider
      // name, connectivity telemetry, audit fields) is never sent back
      // (report F043).
      const body: Record<string, unknown> = { id };
      for (const field of MODEL_UPDATE_FIELDS) {
        if (changes[field] !== undefined) {
          body[field] = changes[field];
        } else if (current[field] !== undefined) {
          body[field] = current[field];
        }
      }
      const result = await dc3Client.post(`${BASE}/config/update`, body);
      printAndExit(result, format);
    });

  model
    .command('delete <id>')
    .description('Delete an AI model configuration')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      await dc3Client.del(`${BASE}/config/delete?id=${encodeURIComponent(id)}`);
      printAndExit(undefined, format);
    });

  model
    .command('check <id>')
    .description('Check model connectivity with a minimal chat probe (L2)')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(`${BASE}/config/check?id=${encodeURIComponent(id)}`);
      printAndExit(result, format);
    });
}
