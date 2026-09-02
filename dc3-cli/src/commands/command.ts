import { Command } from 'commander';
import { dc3Client } from '../core/client.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import {
  deleteManagerResource,
  parseNonNegativeInteger,
  parsePositiveInteger,
  updateManagerResource,
} from '../utils/manager.js';

const COMMAND_BASE = '/api/v3/manager/command';

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
    .option('--offset <n>', 'Zero-based result offset', '0')
    .option('--limit <n>', 'Maximum items to return', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(`${COMMAND_BASE}/list`, {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
        ...(opts.deviceId ? { deviceId: opts.deviceId } : {}),
        ...(opts.profileId ? { profileId: opts.profileId } : {}),
      });
      printAndExit(result, format);
    });

  cmd
    .command('get <id>')
    .description('Get configured command by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`${COMMAND_BASE}/get_by_id?id=${encodeURIComponent(id)}`);
      printAndExit(result, format);
    });

  cmd
    .command('create')
    .description('Create a command configuration')
    .requiredOption('--name <name>', 'Command name')
    .requiredOption('--profile-id <id>', 'Profile ID')
    .option('--type <type>', 'Command type', 'CUSTOM')
    .option('--call-type <type>', 'Call type', 'SYNC')
    .option('--timeout <seconds>', 'Timeout in seconds', parsePositiveInteger, 30)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post(`${COMMAND_BASE}/add`, {
        commandName: opts.name,
        profileId: opts.profileId,
        commandTypeFlag: opts.type,
        callTypeFlag: opts.callType,
        timeout: opts.timeout,
      });
      printAndExit(result, format);
    });

  cmd
    .command('update <id>')
    .description('Update a command configuration')
    .requiredOption('--version <n>', 'Expected optimistic-lock version', parseNonNegativeInteger)
    .option('--name <name>', 'Command name')
    .option('--profile-id <id>', 'Profile ID')
    .option('--type <type>', 'Command type')
    .option('--call-type <type>', 'Call type')
    .option('--timeout <seconds>', 'Timeout in seconds', parsePositiveInteger)
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await updateManagerResource(COMMAND_BASE, id, opts.version, {
        ...(opts.name ? { commandName: opts.name } : {}),
        ...(opts.profileId ? { profileId: opts.profileId } : {}),
        ...(opts.type ? { commandTypeFlag: opts.type } : {}),
        ...(opts.callType ? { callTypeFlag: opts.callType } : {}),
        ...(opts.timeout !== undefined ? { timeout: opts.timeout } : {}),
      });
      printAndExit(result, format);
    });

  cmd
    .command('delete <id>')
    .description('Delete a command configuration')
    .requiredOption('--version <n>', 'Expected optimistic-lock version', parseNonNegativeInteger)
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await deleteManagerResource(COMMAND_BASE, id, opts.version);
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
        printAndExit({ ok: false, message: 'Invalid JSON in --params' }, format, 1);
        return;
      }
      const result = await dc3Client.post('/api/v3/data/command_history/call', {
        deviceId: opts.deviceId,
        commandId: opts.commandId,
        paramValues: params,
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
        `/api/v3/data/command_history/get_by_record_id?recordId=${encodeURIComponent(id)}`,
      );
      printAndExit(result, format);
    });

  cmd
    .command('history-list')
    .description('List command execution history')
    .option('--offset <n>', 'Zero-based result offset', '0')
    .option('--limit <n>', 'Maximum items to return', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/data/command_history/list', {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      });
      printAndExit(result, format);
    });
}
