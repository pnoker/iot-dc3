import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerPointCommand(program: Command): void {
  const point = program
    .command('point')
    .description('Point (datapoint) management');

  // dc3 point list
  point
    .command('list')
    .description('List points')
    .option('--device-id <id>', 'Filter by device ID')
    .option('--profile-id <id>', 'Filter by profile ID')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        page: { current: Number(opts.page), size: Number(opts.size) },
      };
      if (opts.deviceId) body.deviceId = opts.deviceId;
      if (opts.profileId) body.profileId = opts.profileId;
      const result = await dc3Client.post('/api/v3/manager/point/list', body);
      printAndExit(result, format);
    });

  // dc3 point get <id>
  point
    .command('get <id>')
    .description('Get point by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`/api/v3/manager/point/get_by_id?id=${id}`);
      printAndExit(result, format);
    });

  // dc3 point read <id>
  point
    .command('read <id>')
    .description('Read the latest value of a point')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/data/point_value/latest', {
        pointId: id,
        page: { current: 1, size: 1 },
      });
      printAndExit(result, format);
    });

  // dc3 point history <id>
  point
    .command('history <id>')
    .description('Read point value history')
    .option('--count <n>', 'Number of records', '100')
    .option('--device-id <id>', 'Device ID (required for history)')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      if (!opts.deviceId) {
        printAndExit(
          { ok: false, message: '--device-id is required for history query' },
          format,
          1,
        );
      }
      const result = await dc3Client.get(
        `/api/v3/data/point_value/list_history_by_device_id_and_point_id?device_id=${opts.deviceId}&point_id=${id}&count=${opts.count}`,
      );
      printAndExit(result, format);
    });

  // dc3 point write <id>
  point
    .command('write <id>')
    .description('Write a value to a point')
    .requiredOption('--value <value>', 'Value to write')
    .option('--device-id <id>', 'Device ID (required)')
    .option('--confirm', 'Require user confirmation (pending action)', false)
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      if (!opts.deviceId) {
        printAndExit(
          { ok: false, message: '--device-id is required for write operations' },
          format,
          1,
        );
      }
      const result = await dc3Client.post('/api/v3/data/point_command/write', {
        deviceId: opts.deviceId,
        pointId: id,
        value: opts.value,
      });
      printAndExit(result, format);
    });

  // dc3 point create
  point
    .command('create')
    .description('Create a new point')
    .requiredOption('--name <name>', 'Point name')
    .requiredOption('--profile-id <id>', 'Profile ID')
    .option('--type <type>', 'Point type')
    .option('--unit <unit>', 'Unit')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = {
        pointName: opts.name,
        profileId: opts.profileId,
      };
      if (opts.type) body.pointType = opts.type;
      if (opts.unit) body.unit = opts.unit;
      const result = await dc3Client.post('/api/v3/manager/point/add', body);
      printAndExit(result, format);
    });

  // dc3 point update <id>
  point
    .command('update <id>')
    .description('Update a point')
    .option('--name <name>', 'New point name')
    .option('--unit <unit>', 'New unit')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const body: Record<string, unknown> = { id };
      if (opts.name) body.pointName = opts.name;
      if (opts.unit) body.unit = opts.unit;
      const result = await dc3Client.post('/api/v3/manager/point/update', body);
      printAndExit(result, format);
    });

  // dc3 point delete <id>
  point
    .command('delete <id>')
    .description('Delete a point')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(`/api/v3/manager/point/delete?id=${id}`);
      printAndExit(result, format);
    });
}
