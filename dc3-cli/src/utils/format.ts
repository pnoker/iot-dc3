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
/**
 * Output formatters for command results.
 * - json: structured output for AI agents and scripts (default for non-TTY);
 *   rendered byte-identically to `JSON.stringify(data, null, 2)` — the agent
 *   wire contract, do not change shape
 * - table: human-readable tabular output (default for TTY); list envelopes
 *   (items/data arrays) render as rows with a pagination footer
 * - yaml: structured output dumped through js-yaml so record keys and values
 *   stay parseable (report F041)
 */
import { createRequire } from 'node:module';
import { getGlobalFormatOverride, getSettingsFormatOverride } from '../core/context.js';
import { UsageError } from '../core/errors.js';

// js-yaml ships no bundled typings and @types/js-yaml is not installed; go
// through a typed require seam until the dependency owner adds the types.
const { dump: yamlDump } = createRequire(import.meta.url)('js-yaml') as {
  dump: (_data: unknown, _options?: { lineWidth?: number }) => string;
};

type FormatValue = string | number | boolean | null | undefined;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function flattenObject(obj: Record<string, unknown>, prefix = ''): Record<string, FormatValue> {
  const result: Record<string, FormatValue> = {};
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (isRecord(value)) {
      Object.assign(result, flattenObject(value, path));
    } else if (Array.isArray(value)) {
      result[path] = JSON.stringify(value);
    } else {
      result[path] = value as FormatValue;
    }
  }
  return result;
}

/** Keys under which gateway list payloads carry their row arrays. */
const LIST_ENVELOPE_ROW_KEYS = ['items', 'data'] as const;

/** A list payload unwrapped into its rows plus the scalar pagination metadata. */
interface ListEnvelope {
  rows: unknown[];
  meta: Record<string, unknown>;
}

/**
 * Detect a known list envelope: a record whose `items` or `data` field is an
 * array and whose remaining top-level values are scalar metadata (total,
 * offset, limit, hasNext, ...). Domain records with nested objects alongside
 * the array field are not treated as envelopes.
 * @param data - decoded gateway payload
 * @returns the unwrapped envelope, or undefined when not a list envelope
 */
function extractListEnvelope(data: Record<string, unknown>): ListEnvelope | undefined {
  for (const key of LIST_ENVELOPE_ROW_KEYS) {
    const rows = data[key];
    if (!Array.isArray(rows)) continue;
    const meta = { ...data };
    delete meta[key];
    if (!Object.values(meta).every((value) => value === null || typeof value !== 'object')) {
      continue;
    }
    return { rows, meta };
  }
  return undefined;
}

/** Column keys whose object cells are entity extension blobs (driverExt, pointExt, ...). */
const EXT_BLOB_KEY = /ext$/iu;

/** Hard cap on a rendered cell's width so one long blob cannot blow up the table layout. */
const MAX_CELL_WIDTH = 80;

/**
 * Render an extension blob one level deep: `key=value` pairs inline, null and
 * undefined members dropped, values that are themselves structured kept as
 * compact JSON. A raw `{"type":"MQTT","version":null}` cell becomes
 * `type=MQTT` — readable in a terminal column and free of wire-format noise.
 * @param value - extension blob to render
 * @returns the flattened cell text
 */
function formatExtBlob(value: Record<string, unknown>): string {
  const parts: string[] = [];
  for (const [key, member] of Object.entries(value)) {
    if (member === null || member === undefined) continue;
    parts.push(`${key}=${typeof member === 'object' ? (JSON.stringify(member) ?? String(member)) : String(member)}`);
  }
  return parts.length > 0 ? parts.join(' ') : '{}';
}

/**
 * Render one table cell: extension blobs flattened one level, other nested
 * objects and arrays as compact JSON (never `[object Object]`), null as
 * `null`, undefined as an empty cell.
 * @param value - cell value from a row
 * @param key - column key the value came from (drives ext-blob flattening)
 * @returns the printable cell text
 */
function cellify(value: unknown, key?: string): string {
  if (value === undefined) return '';
  if (value === null) return 'null';
  if (isRecord(value)) {
    return key !== undefined && EXT_BLOB_KEY.test(key) ? formatExtBlob(value) : JSON.stringify(value) ?? String(value);
  }
  if (Array.isArray(value)) return JSON.stringify(value) ?? String(value);
  return String(value);
}

/**
 * Clamp a rendered cell to the column budget with an ellipsis marker.
 * @param text - fully rendered cell text
 * @returns the clamped cell text
 */
function clampCell(text: string): string {
  return text.length > MAX_CELL_WIDTH ? `${text.slice(0, MAX_CELL_WIDTH - 1)}…` : text;
}

/**
 * Union of record keys across rows in first-seen order, so heterogeneous
 * rows never silently drop columns.
 * @param rows - table rows
 * @returns the ordered header keys
 */
function rowKeys(rows: unknown[]): string[] {
  const keys: string[] = [];
  for (const row of rows) {
    if (!isRecord(row)) continue;
    for (const key of Object.keys(row)) {
      if (!keys.includes(key)) keys.push(key);
    }
  }
  return keys;
}

/**
 * First numeric value among the given metadata keys.
 * @param meta - envelope metadata
 * @param keys - candidate key names (camelCase and snake_case variants)
 * @returns the number found, or undefined
 */
function firstNumber(meta: Record<string, unknown>, keys: string[]): number | undefined {
  for (const key of keys) {
    const value = meta[key];
    if (typeof value === 'number') return value;
  }
  return undefined;
}

/**
 * Build the pagination footer line for an offset-style page, e.g.
 * `-- 3 of 42 (offset 4, limit 3), hasNext: true`. Omitted entirely when the
 * metadata carries no pagination fields.
 * @param rowCount - number of rendered rows
 * @param meta - envelope metadata
 * @returns the footer line, or undefined when there is nothing to report
 */
function paginationFooter(rowCount: number, meta: Record<string, unknown>): string | undefined {
  const total = firstNumber(meta, ['total', 'count']);
  const offset = firstNumber(meta, ['offset']);
  const limit = firstNumber(meta, ['limit']);
  const hasNext = meta.hasNext ?? meta.has_next;
  let text = '';
  if (total !== undefined) {
    text = `${rowCount} of ${total}`;
  }
  if (offset !== undefined || limit !== undefined) {
    text += ` (offset ${offset ?? '?'}, limit ${limit ?? '?'})`;
  }
  if (hasNext !== undefined) {
    text = text ? `${text}, hasNext: ${String(hasNext)}` : `hasNext: ${String(hasNext)}`;
  }
  return text ? `-- ${text}` : undefined;
}

/**
 * Render rows as an aligned table with an optional trailing footer line.
 * Only interior columns are padded: the last column is never padded and every
 * line is trimmed, so no rendered row or header carries trailing whitespace.
 * @param rows - values to render (records spread over their keys, scalars
 * into a single `value` column)
 * @param footer - optional pagination footer appended after the rows
 * @returns the rendered table
 */
function formatRowsAsTable(rows: unknown[], footer?: string): string {
  if (rows.length === 0) return '(empty)';
  const recordKeys = rowKeys(rows);
  const headers = recordKeys.length > 0 ? recordKeys : ['value'];
  const cells = rows.map((row) =>
    headers.map((header) =>
      clampCell(recordKeys.length > 0 && isRecord(row) ? cellify(row[header], header) : cellify(row)),
    ),
  );
  const colWidths = headers.map((header, i) =>
    Math.max(header.length, ...cells.map((row) => row[i].length)),
  );
  const sep = colWidths.map((w) => '-'.repeat(w)).join('-+-');
  const renderRow = (row: string[]): string =>
    row
      .map((cell, i) => (i === row.length - 1 ? cell : cell.padEnd(colWidths[i])))
      .join(' | ')
      .trimEnd();
  const headerRow = renderRow(headers);
  const dataRows = cells.map(renderRow).join('\n');
  const table = `${headerRow}\n${sep}\n${dataRows}`;
  return footer ? `${table}\n${footer}` : table;
}

function formatTable(data: unknown): string {
  if (Array.isArray(data)) {
    return formatRowsAsTable(data);
  }

  if (isRecord(data)) {
    const envelope = extractListEnvelope(data);
    if (envelope) {
      return formatRowsAsTable(envelope.rows, paginationFooter(envelope.rows.length, envelope.meta));
    }
    const flat = flattenObject(data);
    const maxKeyLen = Math.max(...Object.keys(flat).map((k) => k.length));
    return Object.entries(flat)
      .map(([k, v]) => `${k.padEnd(maxKeyLen)} │ ${v ?? 'null'}`)
      .join('\n');
  }

  return String(data);
}

/**
 * Supported output render formats (json, table, yaml).
 */
export type OutputFormat = 'json' | 'table' | 'yaml';

/**
 * Render a payload in the requested output format. The json branch is the
 * agent wire contract and must stay byte-identical.
 * @param data - command result to render; undefined renders as an empty string
 * @param format - renderer to apply ('json' output is the byte-stable wire contract)
 * @returns the rendered document, without a trailing newline
 */
export function formatOutput(data: unknown, format: OutputFormat = 'json'): string {
  if (data === undefined) return '';
  switch (format) {
    case 'json':
      return JSON.stringify(data, null, 2);
    case 'table':
      return formatTable(data);
    case 'yaml': {
      // Normalize through JSON first (same as before) so undefined values and
      // non-plain structures behave deterministically, then dump real YAML so
      // keys and nested values stay parseable (report F041) and envelope rows
      // render as a proper list (report F020).
      const json = JSON.parse(JSON.stringify(data));
      return yamlDump(json, { lineWidth: -1 }).trimEnd();
    }
    default:
      return JSON.stringify(data, null, 2);
  }
}

/**
 * Detect the best output format. Priority: the command's own `--format`
 * option, then the global `--format` option, then the persisted
 * `settings.output_format`, and finally the TTY-aware default (table for
 * interactive TTY, json for pipes). An explicit-but-unsupported value —
 * including the empty string — is a usage error, never a silent fallback.
 * @param explicit - format requested via the command's --format, if any
 * @returns the format to render with
 */
export function detectFormat(explicit?: string): OutputFormat {
  if (explicit !== undefined) {
    if (explicit === 'json' || explicit === 'table' || explicit === 'yaml') {
      return explicit;
    }
    // Reject unknown formats instead of silently falling back — the user
    // explicitly asked for something the CLI does not support.
    throw new UsageError(`unknown format '${explicit}' (expected json, table, or yaml)`);
  }
  const globalOverride = getGlobalFormatOverride();
  if (globalOverride) {
    return globalOverride;
  }
  const settingsOverride = getSettingsFormatOverride();
  if (settingsOverride) {
    return settingsOverride;
  }
  return process.stdout.isTTY ? 'table' : 'json';
}

/**
 * Print formatted output and exit. Used at the end of every command.
 * Stays the SUCCESS payload path; failures should throw a typed error from
 * core/errors.ts so they flow through the single failure chokepoint.
 * @param data - success payload to write to stdout (an empty rendering writes nothing)
 * @param format - renderer to apply to `data`
 * @param exitCode - exit code installed on the process (never process.exit)
 */
export function printAndExit(data: unknown, format: OutputFormat = 'json', exitCode = 0): never {
  const output = formatOutput(data, format);
  if (output) process.stdout.write(output + '\n');
  // Use exitCode + throw instead of process.exit(): the abrupt exit crashes
  // Node on Windows with a libuv assertion when fetch keep-alive handles are
  // still open. Setting the code and unwinding lets the runtime close them.
  process.exitCode = exitCode;
  throw new SilentExit();
}

/** Control-flow signal: unwinds to the top-level catch which exits cleanly. */
export class SilentExit extends Error {
  constructor() {
    super('silent exit');
    this.name = 'SilentExit';
  }
}
