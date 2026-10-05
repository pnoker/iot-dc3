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
import { configManager } from './config-manager.js';
import { SilentExit, type OutputFormat } from '../utils/format.js';

const OUTPUT_FORMATS: readonly OutputFormat[] = ['json', 'table', 'yaml'];

let globalFormat: OutputFormat | undefined;
let settingsFormat: OutputFormat | undefined;

function parseFormat(value: string | undefined): OutputFormat | undefined {
  return OUTPUT_FORMATS.includes(value as OutputFormat) ? (value as OutputFormat) : undefined;
}

/**
 * Apply the program-level global options (`--profile`, `--format`) once per
 * invocation from the commander `preAction` hook:
 *
 * - `--profile` installs a per-invocation profile override on the config manager
 *   (validated to exist; a missing profile throws, which the top-level handler
 *   maps to exit code 1), so gateway/tenant/token resolution flows through it.
 * - `--format` seeds the format-resolution context consulted by
 *   `detectFormat()` as the priority between the command's own `--format`
 *   option and the persisted `settings.output_format`.
 * @param opts - global program options as parsed by commander
 * @param opts.profile - profile name to use for this invocation only
 * @param opts.format - global output format (json, table, yaml)
 */
export async function applyGlobalOptions(opts: {
  profile?: string;
  format?: string;
}): Promise<void> {
  if (opts.profile && opts.profile.trim()) {
    await configManager.setProfileOverride(opts.profile.trim());
  }
  // Reject unknown --format values here rather than silently dropping them:
  // commander routes --format to the program level even when the subcommand
  // also declares it, so detectFormat never sees the raw invalid string.
  if (opts.format !== undefined && opts.format !== '' && !OUTPUT_FORMATS.includes(opts.format as OutputFormat)) {
    process.stderr.write(`error: unknown format '${opts.format}' (expected json, table, or yaml)\n`);
    process.exitCode = 1;
    throw new SilentExit();
  }
  const settings = await configManager.getSettings();
  globalFormat = parseFormat(opts.format);
  settingsFormat = parseFormat(settings.output_format);
}

/**
 * Get the format requested via the global `--format` option, if any.
 * @returns the global format override, or undefined when not supplied
 */
export function getGlobalFormatOverride(): OutputFormat | undefined {
  return globalFormat;
}

/**
 * Get the persisted `settings.output_format`, if set. Left unset by default so
 * the TTY-aware default (table on TTY, json on pipe) still applies.
 * @returns the persisted format preference, or undefined when unset
 */
export function getSettingsFormatOverride(): OutputFormat | undefined {
  return settingsFormat;
}

/**
 * Reset the per-invocation context (test seam).
 */
export function resetCliContext(): void {
  globalFormat = undefined;
  settingsFormat = undefined;
}
