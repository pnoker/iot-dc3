import { Command } from 'commander';
import { randomUUID } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { basename } from 'node:path';
import { dc3Client } from '../core/client.js';
import type { OperationAccepted, OperationView } from '../core/contracts.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import {
  deleteManagerResource,
  parseNonNegativeInteger,
  updateManagerResource,
} from '../utils/manager.js';

const DEVICE_BASE = '/api/v3/manager/device';

const TERMINAL_OPERATION_STATUSES = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED']);

function operationPath(statusUri: string): string {
  const url = new URL(statusUri, 'http://dc3.invalid');
  return `${url.pathname}${url.search}`;
}

async function waitForOperation(
  accepted: OperationAccepted,
  pollIntervalMs: number,
): Promise<OperationView> {
  while (true) {
    const operation = await dc3Client.get<OperationView>(operationPath(accepted.statusUri));
    if (TERMINAL_OPERATION_STATUSES.has(operation.status)) return operation;
    await new Promise((resolve) => setTimeout(resolve, pollIntervalMs));
  }
}

export function registerDeviceCommand(program: Command): void {
  const device = program.command('device').description('Device management');

  // dc3 device list
  device
    .command('list')
    .description('List devices')
    .option('--driver-id <id>', 'Filter by driver ID')
    .option('--profile-id <id>', 'Filter by profile ID')
    .option('--group-id <id>', 'Filter by group ID')
    .option('--offset <n>', 'Zero-based result offset', '0')
    .option('--limit <n>', 'Maximum items to return', '20')
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      // Manager list endpoints use POST with body query
      const body: Record<string, unknown> = {
        offset: Number(opts.offset),
        limit: Number(opts.limit),
      };
      if (opts.driverId) body.driverId = opts.driverId;
      if (opts.profileId) body.profileId = opts.profileId;
      if (opts.groupId) body.groupId = opts.groupId;
      const result = await dc3Client.post(`${DEVICE_BASE}/list`, body);
      printAndExit(result, format);
    });

  // dc3 device get <id>
  device
    .command('get <id>')
    .description('Get device by ID')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.get(`${DEVICE_BASE}/get_by_id?id=${encodeURIComponent(id)}`);
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
      if (opts.description) body.remark = opts.description;
      if (opts.groupId) body.groupId = opts.groupId;
      const result = await dc3Client.post(`${DEVICE_BASE}/add`, body);
      printAndExit(result, format);
    });

  // dc3 device update <id>
  device
    .command('update <id>')
    .description('Update a device')
    .requiredOption('--version <n>', 'Expected optimistic-lock version', parseNonNegativeInteger)
    .option('--name <name>', 'Device name')
    .option('--driver-id <id>', 'Driver ID')
    .option('--profile-id <id>', 'Profile ID')
    .option('--description <desc>', 'New description')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await updateManagerResource(DEVICE_BASE, id, opts.version, {
        ...(opts.name ? { deviceName: opts.name } : {}),
        ...(opts.driverId ? { driverId: opts.driverId } : {}),
        ...(opts.profileId ? { profileId: opts.profileId } : {}),
        ...(opts.description ? { remark: opts.description } : {}),
      });
      printAndExit(result, format);
    });

  // dc3 device delete <id>
  device
    .command('delete <id>')
    .description('Delete a device')
    .requiredOption('--version <n>', 'Expected optimistic-lock version', parseNonNegativeInteger)
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await deleteManagerResource(DEVICE_BASE, id, opts.version);
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
      const result = await dc3Client.get(
        `/api/v3/manager/device/get_count_by_driver_id?driver_id=${opts.driverId}`,
      );
      printAndExit(result, format);
    });

  // dc3 device status <id>
  device
    .command('status <id>')
    .description('Get device online status')
    .option('--format <format>', 'Output format')
    .action(async (id, opts) => {
      const format = detectFormat(opts.format);
      const result = await dc3Client.post('/api/v3/data/device/status/list', {
        id,
      });
      printAndExit(result, format);
    });

  // dc3 device import <file>
  device
    .command('import <file>')
    .description('Submit an XLSX device import and wait for its durable operation')
    .requiredOption('--driver-id <id>', 'Driver ID used by imported devices')
    .requiredOption('--profile-id <id>', 'Profile ID used by imported devices')
    .option('--idempotency-key <key>', 'Stable key for safe retries (defaults to a UUIDv4)')
    .option('--no-wait', 'Return the accepted operation without polling')
    .option('--poll-interval <ms>', 'Polling interval in milliseconds', '500')
    .option('--format <format>', 'Output format')
    .action(async (file: string, opts) => {
      const format = detectFormat(opts.format);
      if (!file.toLowerCase().endsWith('.xlsx')) {
        printAndExit({ ok: false, message: 'Import file must use the .xlsx extension' }, 'json', 1);
        return;
      }
      const content = await readFile(file);
      if (content.length === 0) {
        printAndExit({ ok: false, message: 'Import file must not be empty' }, 'json', 1);
        return;
      }
      const form = new FormData();
      form.append(
        'request',
        new Blob(
          [
            JSON.stringify({
              driverId: opts.driverId,
              profileId: opts.profileId,
            }),
          ],
          { type: 'application/json' },
        ),
      );
      form.append(
        'file',
        new Blob([content], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        }),
        basename(file),
      );
      const accepted = await dc3Client.postForm<OperationAccepted>(
        '/api/v3/manager/device/import',
        form,
        { 'Idempotency-Key': opts.idempotencyKey ?? randomUUID() },
      );
      if (opts.wait === false) {
        printAndExit(accepted, format);
        return;
      }
      const operation = await waitForOperation(accepted, Math.max(100, Number(opts.pollInterval)));
      printAndExit(operation, format, operation.status === 'SUCCEEDED' ? 0 : 1);
    });
}
