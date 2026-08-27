import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerCommandCommand(program: Command): void {
  const cmd = program
    .command('command')
    .description('Device command management (configuration + execution)');

  // Manager CRUD
  cmd
    .command('list')
    .description('List configured commands')
    .option('--device-id <id>', 'Filter by device ID')
    .option('--profile-id <id>', 'Filter by profile ID')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      if (opts.deviceId) {
        const result = await dc3Client.get(
          `/api/v3/manager/command/list_by_device_id?device_id=${opts.deviceId}`,
        );
        printAndExit(result, format);
      }
      const result = await dc3Client.post('/api/v3/manager/command/list', {
        page: { current: Number(opts.page), size: Number(opts.size) },
      });
      printAndExit(result, format);
    });

  cmd
    .command('get <id>')
    .description('Get configured command by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/manager/command/get_by_id?id=${id}`,
      );
      printAndExit(result, format);
    });

  // Execute a command
  cmd
    .command('call')
    .description('Execute a command on a device')
    .requiredOption('--device-id <id>', 'Device ID')
    .requiredOption('--command-id <id>', 'Command ID')
    .option('--params <json>', 'Command parameters as JSON string', '{}')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      let params: Record<string, unknown>;
      try {
        params = JSON.parse(opts.params);
      } catch {
        printAndExit(
          { ok: false, message: 'Invalid JSON in --params' },
          format,
          1,
        );
        return;
      }
      const result = await dc3Client.post('/api/v3/data/command_history/call', {
        deviceId: opts.deviceId,
        commandId: opts.commandId,
        ...params,
      });
      printAndExit(result, format);
    });

  // Command history
  cmd
    .command('history <id>')
    .description('Get command execution result by record ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(
        `/api/v3/data/command_history/${id}`,
      );
      printAndExit(result, format);
    });

  cmd
    .command('history-list')
    .description('List command execution history')
    .option('--page <n>', 'Page number', '1')
    .option('--size <n>', 'Page size', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/data/command_history/list', {
        page: { current: Number(opts.page), size: Number(opts.size) },
      });
      printAndExit(result, format);
    });
}
