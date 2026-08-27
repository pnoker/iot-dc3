import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerDeviceCommand(program: Command): void {
  const device = program
    .command('device')
    .description('Device management');

  // dc3 device list
  device
    .command('list')
    .description('List devices')
    .option('--driver-id <id>', 'Filter by driver ID')
    .option('--profile-id <id>', 'Filter by profile ID')
    .option('--group-id <id>', 'Filter by group ID')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      // Manager list endpoints use POST with body query
      const body: Record<string, unknown> = { page: { current: Number(opts.page), size: Number(opts.size) } };
      if (opts.driverId) body.driverId = opts.driverId;
      if (opts.profileId) body.profileId = opts.profileId;
      if (opts.groupId) body.groupId = opts.groupId;
      const result = await dc3Client.post('/api/v3/manager/device/list', body);
      printAndExit(result, format);
    });

  // dc3 device get <id>
  device
    .command('get <id>')
    .description('Get device by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/manager/device/get_by_id?id=${id}`);
      printAndExit(result, format);
    });

  // dc3 device create
  device
    .command('create')
    .description('Create a new device')
    .requiredOption('--name <name>', 'Device name')
    .requiredOption('--driver-id <id>', 'Driver ID')
    .requiredOption('--profile-id <id>', 'Profile ID')
    .option('--description <desc>', 'Description')
    .option('--group-id <id>', 'Group ID')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        deviceName: opts.name,
        driverId: opts.driverId,
        profileId: opts.profileId,
      };
      if (opts.description) body.description = opts.description;
      if (opts.groupId) body.groupId = opts.groupId;
      const result = await dc3Client.post('/api/v3/manager/device/add', body);
      printAndExit(result, format);
    });

  // dc3 device update <id>
  device
    .command('update <id>')
    .description('Update a device')
    .option('--name <name>', 'New device name')
    .option('--description <desc>', 'New description')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = { id };
      if (opts.name) body.deviceName = opts.name;
      if (opts.description) body.description = opts.description;
      const result = await dc3Client.post('/api/v3/manager/device/update', body);
      printAndExit(result, format);
    });

  // dc3 device delete <id>
  device
    .command('delete <id>')
    .description('Delete a device')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(`/api/v3/manager/device/delete?id=${id}`);
      printAndExit(result, format);
    });

  // dc3 device count
  device
    .command('count')
    .description('Count devices by driver')
    .requiredOption('--driver-id <id>', 'Driver ID')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/manager/device/get_count_by_driver_id?driver_id=${opts.driverId}`);
      printAndExit(result, format);
    });

  // dc3 device status <id>
  device
    .command('status <id>')
    .description('Get device online status')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/data/device/status/list', { id });
      printAndExit(result, format);
    });
}
