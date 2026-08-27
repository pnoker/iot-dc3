import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerProfileCommand(program: Command): void {
  const profile = program
    .command('profile')
    .description('Profile (device template) management');

  profile
    .command('list')
    .description('List profiles')
    .option('--device-id <id>', 'Filter by device ID')
    .option('--type <type>', 'Filter by type')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
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
        page: { current: Number(opts.page), size: Number(opts.size) },
      };
      if (opts.type) body.profileType = opts.type;
      const result = await dc3Client.post('/api/v3/manager/profile/list', body);
      printAndExit(result, format);
    });

  profile
    .command('get <id>')
    .description('Get profile by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/manager/profile/get_by_id?id=${id}`,
      );
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
      if (opts.type) body.profileType = opts.type;
      const result = await dc3Client.post('/api/v3/manager/profile/add', body);
      printAndExit(result, format);
    });

  profile
    .command('update <id>')
    .description('Update a profile')
    .option('--name <name>', 'New profile name')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = { id };
      if (opts.name) body.profileName = opts.name;
      const result = await dc3Client.post('/api/v3/manager/profile/update', body);
      printAndExit(result, format);
    });

  profile
    .command('delete <id>')
    .description('Delete a profile')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(
        `/api/v3/manager/profile/delete?id=${id}`,
      );
      printAndExit(result, format);
    });
}
