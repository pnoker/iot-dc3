import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import {
  deleteManagerResource,
  parseNonNegativeInteger,
  updateManagerResource,
} from '../utils/manager.js';

const PROFILE_BASE = '/api/v3/manager/profile';

export function registerProfileCommand(program: Command): void {
  const profile = program.command('profile').description('Profile (device template) management');

  profile
    .command('list')
    .description('List profiles')
    .option('--device-id <id>', 'Filter by device ID')
    .option('--type <type>', 'Filter by type')
    .option('--offset <n>', 'Zero-based result offset', '0')
    .option('--limit <n>', 'Maximum items to return', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      if (opts.deviceId) {
        const result = await dc3Client.get(
          `/api/v3/manager/profile/list_by_device_id?device_id=${opts.deviceId}`,
        );
        printAndExit(result, format);
      }
      const body: Record<string, unknown> = {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      };
      if (opts.type) body.profileTypeFlag = opts.type;
      const result = await dc3Client.post(`${PROFILE_BASE}/list`, body);
      printAndExit(result, format);
    });

  profile
    .command('get <id>')
    .description('Get profile by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`${PROFILE_BASE}/get_by_id?id=${encodeURIComponent(id)}`);
      printAndExit(result, format);
    });

  profile
    .command('create')
    .description('Create a new profile')
    .requiredOption('--name <name>', 'Profile name')
    .option('--type <type>', 'Profile type')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = { profileName: opts.name };
      if (opts.type) body.profileTypeFlag = opts.type;
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
        ...(opts.name ? { profileName: opts.name } : {}),
        ...(opts.type ? { profileTypeFlag: opts.type } : {}),
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
