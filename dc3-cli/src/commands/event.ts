import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerEventCommand(program: Command): void {
  const event = program
    .command('event')
    .description('Event management (runtime data)');

  // Manager CRUD
  event
    .command('list')
    .description('List configured events')
    .option('--device-id <id>', 'Filter by device ID')
    .option('--profile-id <id>', 'Filter by profile ID')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      if (opts.deviceId) {
        const result = await dc3Client.get(
          `/api/v3/manager/event/list_by_device_id?device_id=${opts.deviceId}`,
        );
        printAndExit(result, format);
      }
      if (opts.profileId) {
        const result = await dc3Client.get(
          `/api/v3/manager/event/list_by_profile_id?profile_id=${opts.profileId}`,
        );
        printAndExit(result, format);
      }
      const result = await dc3Client.post('/api/v3/manager/event/list', {
        page: { current: Number(opts.page), size: Number(opts.size) },
      });
      printAndExit(result, format);
    });

  event
    .command('get <id>')
    .description('Get configured event by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/manager/event/get_by_id?id=${id}`,
      );
      printAndExit(result, format);
    });

  // Runtime event history
  event
    .command('history')
    .description('List event history records')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/data/event_history/list', {
        page: { current: Number(opts.page), size: Number(opts.size) },
      });
      printAndExit(result, format);
    });

  event
    .command('create')
    .description('Create a new event configuration')
    .requiredOption('--name <name>', 'Event name')
    .requiredOption('--profile-id <id>', 'Profile ID')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/manager/event/add', {
        eventName: opts.name,
        profileId: opts.profileId,
      });
      printAndExit(result, format);
    });

  event
    .command('delete <id>')
    .description('Delete an event configuration')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(
        `/api/v3/manager/event/delete?id=${id}`,
      );
      printAndExit(result, format);
    });
}
