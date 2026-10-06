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
import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

const BASE = '/api/v3/agentic/provider';
const PROVIDER_TYPES = ['OPENAI_COMPATIBLE', 'ANTHROPIC'] as const;

const parseProviderType = (value: string): string => {
  const upper = value.toUpperCase();
  if (!PROVIDER_TYPES.includes(upper as (typeof PROVIDER_TYPES)[number])) {
    throw new Error(`option '--type ${value}' is invalid. allowed: ${PROVIDER_TYPES.join(', ')}`);
  }
  return upper;
};

/**
 * Register the `provider` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerProviderCommand(program: Command): void {
  const provider = program.command('provider').description('AI model provider management');

  provider
    .command('list')
    .description('List configured AI model providers')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`${BASE}/list`);
      printAndExit(result, format);
    });

  provider
    .command('add')
    .description('Add a new AI model provider')
    .requiredOption('--name <name>', 'Provider display name')
    .requiredOption('--base-url <url>', 'API base URL (e.g. https://api.deepseek.com)')
    .option('--type <type>', 'Provider protocol type (OPENAI_COMPATIBLE or ANTHROPIC)', parseProviderType, 'OPENAI_COMPATIBLE')
    .option('--api-key <key>', 'API key (omit for local endpoints without auth)')
    .option('--default', 'Set as the tenant default provider')
    .option('--enable', 'Enable the provider (default)')
    .option('--disable', 'Disable the provider')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        name: opts.name,
        baseUrl: opts.baseUrl,
        providerType: opts.type,
      };
      if (opts.apiKey !== undefined) body.apiKey = opts.apiKey;
      if (opts.default) body.defaultFlag = 'DEFAULT';
      else body.defaultFlag = 'NOT_DEFAULT';
      if (opts.disable) body.enableFlag = 'DISABLE';
      else body.enableFlag = 'ENABLE';
      const result = await dc3Client.post(`${BASE}/config/add`, body);
      printAndExit(result, format);
    });

  provider
    .command('update <id>')
    .description('Update an AI model provider (blank api-key keeps the stored one)')
    .option('--name <name>', 'New display name')
    .option('--base-url <url>', 'New API base URL')
    .option('--type <type>', 'New protocol type (OPENAI_COMPATIBLE or ANTHROPIC)', parseProviderType)
    .option('--api-key <key>', 'New API key (omit to keep the stored one)')
    .option('--default', 'Set as the tenant default')
    .option('--enable', 'Enable the provider')
    .option('--disable', 'Disable the provider')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      // The provider controller has no get_by_id, so resolve from the list.
      const providers = await dc3Client.get<Array<Record<string, unknown>>>(`${BASE}/list`);
      const current = Array.isArray(providers)
        ? providers.find((p) => String(p.id) === String(id))
        : null;
      if (!current) {
        printAndExit({ ok: false, message: `Provider ${id} not found` }, format, 1);
      }
      const changes: Record<string, unknown> = {};
      if (opts.name !== undefined) changes.name = opts.name;
      if (opts.baseUrl !== undefined) changes.baseUrl = opts.baseUrl;
      if (opts.type !== undefined) changes.providerType = opts.type;
      if (opts.apiKey !== undefined) changes.apiKey = opts.apiKey;
      if (opts.default) changes.defaultFlag = 'DEFAULT';
      if (opts.enable) changes.enableFlag = 'ENABLE';
      if (opts.disable) changes.enableFlag = 'DISABLE';
      const result = await dc3Client.post(`${BASE}/config/update`, {
        ...current,
        ...changes,
        id,
      });
      printAndExit(result, format);
    });

  provider
    .command('delete <id>')
    .description('Delete an AI model provider')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      await dc3Client.del(`${BASE}/config/delete?id=${encodeURIComponent(id)}`);
      printAndExit(undefined, format);
    });

  provider
    .command('check')
    .description('Check provider connectivity (L1 model-list + L2 minimal chat probe)')
    .option('--id <id>', 'Provider ID to check (saved configuration)')
    .option('--base-url <url>', 'Draft mode: check an unsaved base URL')
    .option('--type <type>', 'Protocol type (required for draft mode; default OPENAI_COMPATIBLE)', parseProviderType)
    .option('--api-key <key>', 'Draft mode: API key (optional)')
    .option('--model <model>', 'Model to use for the L2 chat probe')
    .option('--level <level>', 'Probe level: L1 (model list only), L2 (chat only), or BOTH (default)', 'BOTH')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      if (!opts.id && !opts.baseUrl) {
        printAndExit(
          { ok: false, message: 'Either --id (saved provider) or --base-url (draft) is required' },
          format,
          1,
        );
      }
      const body: Record<string, unknown> = { level: opts.level };
      if (opts.id) body.id = opts.id;
      if (opts.baseUrl) body.baseUrl = opts.baseUrl;
      // Draft mode: default to OPENAI_COMPATIBLE when type is not specified
      // (the backend requires an explicit type for draft checks).
      if (opts.baseUrl) body.providerType = opts.type || 'OPENAI_COMPATIBLE';
      else if (opts.type) body.providerType = opts.type;
      if (opts.apiKey !== undefined) body.apiKey = opts.apiKey;
      if (opts.model !== undefined) body.model = opts.model;
      const result = await dc3Client.post(`${BASE}/check`, body);
      printAndExit(result, format);
    });
}
