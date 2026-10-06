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
import { readFile } from 'node:fs/promises';
import { Command } from 'commander';
import { mcpClient } from '../core/mcp.js';
import { UsageError, ValidationError } from '../core/errors.js';
import { detectFormat, printAndExit } from '../utils/format.js';

/**
 * Tool-catalog commands: the CLI's live view of the platform MCP tool catalog.
 *
 * First step of the Phase-3 direction (docs/design/token-unification-mcp-first-cli.md
 * §4): commands start reading the machine-readable catalog instead of hard-coded paths.
 * @param program - commander program to attach the command to
 */

/** Options accepted by the `tools call` action. */
interface ToolCallOptions {
  args?: string;
  argsFile?: string;
  format?: string;
}

/**
 * Read the raw tool-arguments JSON from the user's chosen source: inline
 * `--args`, or `--args-file <path>` with `'-'` for stdin, so payloads beyond
 * the OS argv limit stay invocable (report F032). Exactly one source may be
 * given; diagnostics name the offending flag.
 * @param opts - parsed `tools call` options
 * @returns the raw JSON text and the flag it came from
 */
async function readRawToolArgs(opts: ToolCallOptions): Promise<{ raw: string; source: string }> {
  if (opts.args !== undefined && opts.argsFile !== undefined) {
    throw new UsageError('--args and --args-file are mutually exclusive');
  }
  if (opts.args !== undefined) {
    return { raw: opts.args, source: '--args' };
  }
  if (opts.argsFile === undefined) {
    throw new UsageError("required option '--args <json>' (or '--args-file <path>') not specified");
  }
  if (opts.argsFile === '-') {
    const chunks: Buffer[] = [];
    for await (const chunk of process.stdin) {
      chunks.push(Buffer.from(chunk));
    }
    return { raw: Buffer.concat(chunks).toString('utf8'), source: '--args-file' };
  }
  try {
    return { raw: await readFile(opts.argsFile, 'utf8'), source: '--args-file' };
  } catch {
    throw new ValidationError(`Cannot read --args-file: ${opts.argsFile}`);
  }
}

/**
 * Register the `tools` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerToolCommand(program: Command): void {
  const tools = program
    .command('tools')
    .description('MCP tool catalog (requires dc3 auth login --oauth)');

  // dc3 tools list.
  //
  // Deliberately no try/catch around the action body: printAndExit unwinds
  // via SilentExit, and a catch that re-reports it as a failure used to print
  // a second JSON document and flip exit 0 to 1 (report F009). Failures are
  // typed errors that escape to the single top-level chokepoint, which keeps
  // the documented exit codes (auth 3, network 2, other 1).
  tools
    .command('list')
    .description('List tools visible to the current OAuth ticket')
    .option('--format <format>', 'Output format: json, table, yaml')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await mcpClient.listTools();
      const rows = (Array.isArray(result.tools) ? result.tools : []).map((t) => ({
        name: t.name,
        title: t.title ?? t.description,
        category: t.category ?? '',
      }));
      printAndExit(rows, format);
    });

  // dc3 tools call
  tools
    .command('call <name>')
    .description('Invoke a catalog tool (arguments passed verbatim)')
    .option('--args <json>', 'Arguments object as JSON, e.g. \'{"deviceId":1}\'')
    .option(
      '--args-file <path>',
      'Read the arguments JSON from a file ("-" reads stdin; needed for payloads beyond the OS argv limit)',
    )
    .option('--format <format>', 'Output format: json, table, yaml')
    .action(async (name: string, opts: ToolCallOptions) => {
      const format = detectFormat(opts.format);
      const { raw, source } = await readRawToolArgs(opts);
      let parsed: unknown;
      try {
        parsed = JSON.parse(raw);
      } catch {
        throw new ValidationError(`${source} is not valid JSON`);
      }
      // MCP `arguments` is an object; arrays, scalars, and null are rejected
      // client-side instead of surfacing as an opaque gateway 400 (report F034).
      if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
        throw new ValidationError(`${source} must be a JSON object`);
      }
      const result = await mcpClient.callTool(name, parsed as Record<string, unknown>);
      printAndExit(result, format);
    });
}
