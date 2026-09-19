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
import { mcpClient } from '../core/mcp.js';
import { AuthError } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

/**
 * Tool-catalog commands: the CLI's live view of the platform MCP tool catalog.
 *
 * First step of the Phase-3 direction (docs/design/token-unification-mcp-first-cli.md
 * §4): commands start reading the machine-readable catalog instead of hard-coded paths.
 * @param program - commander program to attach the command to
 */
export function registerToolCommand(program: Command): void {
  const tools = program
    .command('tools')
    .description('MCP tool catalog (requires dc3 auth login --oauth)');

  // dc3 tools list
  tools
    .command('list')
    .description('List tools visible to the current OAuth ticket')
    .option('--format <format>', 'Output format: json, table, yaml')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      try {
        const result = await mcpClient.listTools();
        const tools = Array.isArray(result.tools) ? result.tools : [];
        const rows = tools.map((t) => ({
          name: t.name,
          title: t.title ?? t.description,
          category: t.category ?? '',
        }));
        printAndExit(rows, format, 0);
      } catch (err) {
        if (err instanceof AuthError) {
          printAndExit({ ok: false, message: err.message }, 'json', 3);
        }
        printAndExit({ ok: false, message: (err as Error).message }, 'json', 1);
      }
    });

  // dc3 tools call
  tools
    .command('call <name>')
    .description('Invoke a catalog tool (arguments passed verbatim)')
    .requiredOption('--args <json>', 'Arguments object as JSON, e.g. \'{"deviceId":1}\'')
    .option('--format <format>', 'Output format: json, table, yaml')
    .action(async (name: string, opts: { args: string; format?: string }) => {
      const format = detectFormat(opts.format);
      try {
        let args: Record<string, unknown>;
        try {
          args = JSON.parse(opts.args);
        } catch {
          printAndExit({ ok: false, message: '--args is not valid JSON' }, 'json', 1);
          return;
        }
        const result = await mcpClient.callTool(name, args);
        printAndExit(result, format, 0);
      } catch (err) {
        if (err instanceof AuthError) {
          printAndExit({ ok: false, message: err.message }, 'json', 3);
        }
        printAndExit({ ok: false, message: (err as Error).message }, 'json', 1);
      }
    });
}
