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
import {
  deleteManagerResource,
  getManagerResource,
  parseNonNegativeInteger,
  parsePositiveInteger,
  requireResourceId,
  updateManagerResource,
} from '../utils/manager.js';

const POINT_BASE = '/api/v3/manager/point';

/**
 * Plain decimal number lexeme (optional sign, digits, optional fraction).
 * Numeric coercion alone (Number) accepts '', ' 5', '0x10', and '1e2'; the
 * lexical check keeps those out before the finite check.
 */
const DECIMAL_NUMBER_PATTERN = /^[+-]?(?:\d+(?:\.\d+)?|\.\d+)$/u;

/**
 * Parse a CLI option as a decimal number for the BigDecimal-backed point
 * fields: the value is validated lexically and sent as a JSON number so the
 * wire type matches the gateway PointVO (report F039).
 * @param value - raw CLI option lexeme: optional sign, digits, optional
 * fraction (e.g. 0, 2, 0.5, -1.25)
 * @returns the finite numeric value to send as a JSON number
 */
function parseDecimalNumber(value: string): number {
  if (!DECIMAL_NUMBER_PATTERN.test(value)) {
    throw new InvalidArgumentError('must be a plain decimal number (e.g. 0, 2, 0.5, -1.25)');
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    throw new InvalidArgumentError('must be a finite decimal number');
  }
  return parsed;
}

/**
 * Register the `point` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerPointCommand(program: Command): void {
  const point = program.command('point').description('Point (datapoint) management');

  // dc3 point list
  point
    .command('list')
    .description('List points')
    .option('--device-id <id>', 'Filter by device ID')
    .option('--profile-id <id>', 'Filter by profile ID')
    .option('--offset <n>', 'Zero-based result offset', parseNonNegativeInteger, 0)
    .option('--limit <n>', 'Maximum items to return', parseNonNegativeInteger, 20)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        offset: opts.offset,
        limit: opts.limit,
      };
      if (opts.deviceId !== undefined) body.deviceId = opts.deviceId;
      if (opts.profileId !== undefined) body.profileId = opts.profileId;
      const result = await dc3Client.post(`${POINT_BASE}/list`, body);
      printAndExit(result, format);
    });

  // dc3 point get <id>
  point
    .command('get <id>')
    .description('Get point by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      // Shared manager helper: rejects empty/whitespace ids before any request.
      const result = await getManagerResource(POINT_BASE, id);
      printAndExit(result, format);
    });

  // dc3 point read <id>
  point
    .command('read <id>')
    .description('Read the latest value of a point')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      // The point id keys the latest-value lookup — reject empty ids before
      // the wire (audit G13/G27).
      requireResourceId('/api/v3/data/point_value/latest', id);
      const result = await dc3Client.post('/api/v3/data/point_value/latest', {
        pointId: id,
        offset: 0,
        limit: 1,
      });
      printAndExit(result, format);
    });

  // dc3 point history <id>
  point
    .command('history <id>')
    .description('Read point value history')
    .option('--limit <n>', 'Number of records per page', parsePositiveInteger, 100)
    .option('--cursor <cursor>', 'Opaque cursor from the previous page')
    .option('--device-id <id>', 'Device ID (required for history)')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      if (opts.deviceId === undefined) {
        throw new ValidationError('--device-id is required for history query');
      }
      // Both ids key the history lookup — reject empty ids before the wire
      // instead of silently treating them as absent (audit G13/G27, G32).
      requireResourceId('/api/v3/data/point_value/history', id);
      requireResourceId('--device-id', opts.deviceId);
      const params = new URLSearchParams({
        device_id: opts.deviceId,
        point_id: id,
        limit: String(opts.limit),
      });
      if (opts.cursor !== undefined) params.set('cursor', opts.cursor);
      const result = await dc3Client.get(`/api/v3/data/point_value/history?${params}`);
      printAndExit(result, format);
    });

  // dc3 point write <id> (asynchronous command submission)
  point
    .command('write <id>')
    .description('Write a value to a point')
    .requiredOption('--value <value>', 'Value to write')
    .option('--device-id <id>', 'Device ID (required)')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      if (opts.deviceId === undefined) {
        throw new ValidationError('--device-id is required for write operations');
      }
      // Both ids key the write submission — reject empty ids before the wire
      // (audit G13/G27, G32).
      requireResourceId('/api/v3/data/point_command/write', id);
      requireResourceId('--device-id', opts.deviceId);
      const result = await dc3Client.post('/api/v3/data/point_command/write', {
        deviceId: opts.deviceId,
        pointId: id,
        value: opts.value,
      });
      printAndExit(result, format);
    });

  // dc3 point add (create kept as a one-release compat alias)
  point
    .command('add')
    .alias('create')
    .description('Add a new point')
    .requiredOption('--name <name>', 'Point name')
    .requiredOption('--profile-id <id>', 'Profile ID')
    .option('--type <type>', 'Point type', 'FLOAT')
    .option('--rw <type>', 'Read/write type', 'READ_ONLY')
    .option('--value-decimal <n>', 'Decimal precision', parseNonNegativeInteger, 3)
    .option('--base-value <value>', 'Base value (sent as a JSON number)', parseDecimalNumber, 0)
    .option('--multiple <value>', 'Scale multiplier (sent as a JSON number)', parseDecimalNumber, 1)
    .option('--unit <unit>', 'Unit')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        pointName: opts.name,
        profileId: opts.profileId,
        pointTypeFlag: opts.type,
        rwFlag: opts.rw,
        valueDecimal: opts.valueDecimal,
        baseValue: opts.baseValue,
        multiple: opts.multiple,
      };
      if (opts.unit !== undefined) body.unit = opts.unit;
      const result = await dc3Client.post(`${POINT_BASE}/add`, body);
      printAndExit(result, format);
    });

  // dc3 point update <id>
  point
    .command('update <id>')
    .description('Update a point')
    .requiredOption('--version <n>', 'Expected optimistic-lock version', parseNonNegativeInteger)
    .option('--name <name>', 'New point name')
    .option('--profile-id <id>', 'Profile ID')
    .option('--type <type>', 'Point type')
    .option('--rw <type>', 'Read/write type')
    .option('--value-decimal <n>', 'Decimal precision', parseNonNegativeInteger)
    .option('--base-value <value>', 'Base value (sent as a JSON number)', parseDecimalNumber)
    .option('--multiple <value>', 'Scale multiplier (sent as a JSON number)', parseDecimalNumber)
    .option('--unit <unit>', 'New unit')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await updateManagerResource(POINT_BASE, id, opts.version, {
        ...(opts.name !== undefined ? { pointName: opts.name } : {}),
        ...(opts.profileId !== undefined ? { profileId: opts.profileId } : {}),
        ...(opts.type !== undefined ? { pointTypeFlag: opts.type } : {}),
        ...(opts.rw !== undefined ? { rwFlag: opts.rw } : {}),
        ...(opts.valueDecimal !== undefined ? { valueDecimal: opts.valueDecimal } : {}),
        ...(opts.baseValue !== undefined ? { baseValue: opts.baseValue } : {}),
        ...(opts.multiple !== undefined ? { multiple: opts.multiple } : {}),
        ...(opts.unit !== undefined ? { unit: opts.unit } : {}),
      });
      printAndExit(result, format);
    });

  // dc3 point delete <id>
  point
    .command('delete <id>')
    .description('Delete a point')
    .requiredOption('--version <n>', 'Expected optimistic-lock version', parseNonNegativeInteger)
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await deleteManagerResource(POINT_BASE, id, opts.version);
      printAndExit(result, format);
    });
}
