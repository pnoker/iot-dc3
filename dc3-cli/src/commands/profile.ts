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
import { ValidationError } from '../core/errors.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import {
  deleteManagerResource,
  getManagerResource,
  parseNonNegativeInteger,
  updateManagerResource,
} from '../utils/manager.js';

const PROFILE_BASE = '/api/v3/manager/profile';

/**
 * Register the `profile` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerProfileCommand(program: Command): void {
  const profile = program.command('profile').description('Profile (device template) management');

  profile
    .command('list')
    .description('List profiles')
    .option('--device-id <id>', 'Filter by device ID (unpaged endpoint)')
    .option('--type <type>', 'Filter by type')
    .option(
      '--offset <n>',
      'Zero-based result offset (cannot be combined with --device-id)',
      parseNonNegativeInteger,
    )
    .option(
      '--limit <n>',
      'Maximum items to return (cannot be combined with --device-id)',
      parseNonNegativeInteger,
    )
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      // An explicitly empty device filter is invalid input, not an absent flag
      // (audit G32): reject it before any request is built instead of sending
      // `?device_id=` to the gateway.
      if (opts.deviceId !== undefined && opts.deviceId.trim() === '') {
        throw new ValidationError('--device-id must be a non-empty value');
      }
      if (opts.deviceId !== undefined) {
        // The device filter is a separate GET endpoint without paging: refuse the
        // combination instead of silently ignoring explicit paging flags.
        if (opts.offset !== undefined || opts.limit !== undefined) {
          throw new ValidationError(
            '--offset/--limit cannot be combined with --device-id: the device filter endpoint is not paged',
          );
        }
        const result = await dc3Client.get(
          `/api/v3/manager/profile/list_by_device_id?device_id=${encodeURIComponent(opts.deviceId)}`,
        );
        printAndExit(result, format);
      }
      const body: Record<string, unknown> = {
        offset: opts.offset ?? 0,
        limit: opts.limit ?? 20,
      };
      if (opts.type !== undefined) body.profileTypeFlag = opts.type;
      const result = await dc3Client.post(`${PROFILE_BASE}/list`, body);
      printAndExit(result, format);
    });

  profile
    .command('get <id>')
    .description('Get profile by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      // Shared manager helper: rejects empty/whitespace ids before any request.
      const result = await getManagerResource(PROFILE_BASE, id);
      printAndExit(result, format);
    });

  profile
    .command('add')
    .alias('create')
    .description('Add a new profile')
    .requiredOption('--name <name>', 'Profile name')
    .option('--type <type>', 'Profile type')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = { profileName: opts.name };
      if (opts.type !== undefined) body.profileTypeFlag = opts.type;
      const result = await dc3Client.post(`${PROFILE_BASE}/add`, body);
      printAndExit(result, format);
    });

  profile
    .command('update <id>')
    .description('Update a profile')
    .requiredOption('--version <n>', 'Expected optimistic-lock version', parseNonNegativeInteger)
    .option('--name <name>', 'New profile name')
    .option('--type <type>', 'Profile type')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await updateManagerResource(PROFILE_BASE, id, opts.version, {
        ...(opts.name !== undefined ? { profileName: opts.name } : {}),
        ...(opts.type !== undefined ? { profileTypeFlag: opts.type } : {}),
      });
      printAndExit(result, format);
    });

  profile
    .command('delete <id>')
    .description('Delete a profile')
    .requiredOption('--version <n>', 'Expected optimistic-lock version', parseNonNegativeInteger)
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await deleteManagerResource(PROFILE_BASE, id, opts.version);
      printAndExit(result, format);
    });
}
