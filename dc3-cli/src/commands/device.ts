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
import { randomUUID } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { basename } from 'node:path';
import { dc3Client } from '../core/client.js';
import type { OperationAccepted, OperationView } from '../core/contracts.js';
import { CliError, TimeoutError, ValidationError } from '../core/errors.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import {
  deleteManagerResource,
  parseNonNegativeInteger,
  updateManagerResource,
} from '../utils/manager.js';

const DEVICE_BASE = '/api/v3/manager/device';

const TERMINAL_OPERATION_STATUSES = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED']);

/** Grace added to the operation's own expiresAt when clamping the wait deadline. */
const OPERATION_EXPIRY_GRACE_MS = 30_000;

/** Ceiling for the exponentially growing delay between status polls. */
const MAX_POLL_INTERVAL_MS = 10_000;

/** Floor for the delay between status polls, kept from the pre-deadline behavior. */
const MIN_POLL_INTERVAL_MS = 100;

function operationPath(statusUri: string): string {
  const url = new URL(statusUri, 'http://dc3.invalid');
  return `${url.pathname}${url.search}`;
}

/**
 * Structured wait deadline failure: carries the polling coordinates so an
 * agent can inspect or resume the operation instead of re-running the import
 * (report F010).
 */
class OperationWaitTimeout extends TimeoutError {
  public readonly operationId: string;
  public readonly lastStatus: string;
  public readonly elapsedMs: number;
  public readonly statusUri: string;

  constructor(accepted: OperationAccepted, last: OperationView, elapsedMs: number) {
    super(
      `timed out waiting for operation ${accepted.operationId} after ` +
        `${Math.round(elapsedMs / 100) / 10}s: last status ${last.status}; poll ${accepted.statusUri}`,
    );
    this.operationId = accepted.operationId;
    this.lastStatus = last.status;
    this.elapsedMs = elapsedMs;
    this.statusUri = accepted.statusUri;
  }
}

/**
 * Structured SIGINT outcome of an interrupted wait: names the last observed
 * status and the status URI. Maps onto the documented exit-code taxonomy
 * (exit 1) with the dedicated `INTERRUPTED` machine code.
 */
class OperationWaitInterrupted extends CliError {
  constructor(accepted: OperationAccepted, last: OperationView | undefined) {
    super(
      `wait for operation ${accepted.operationId} interrupted: last status ` +
        `${last?.status ?? 'UNKNOWN'}; poll ${accepted.statusUri}`,
      { kind: 'timeout', exitCode: 1, code: 'INTERRUPTED' },
    );
  }
}

/**
 * Clamp the wait deadline to the operation's own expiry plus a grace window:
 * an operation the gateway will never finish cannot hang the CLI past the
 * moment it is guaranteed to be dead (report F010).
 * @param deadline - current deadline in epoch milliseconds
 * @param expiresAt - operation expiry timestamp, when the gateway reports one
 * @returns the effective deadline in epoch milliseconds
 */
function clampDeadlineToOperationExpiry(deadline: number, expiresAt: string | null): number {
  if (!expiresAt) {
    return deadline;
  }
  const expiry = Date.parse(expiresAt);
  return Number.isFinite(expiry) ? Math.min(deadline, expiry + OPERATION_EXPIRY_GRACE_MS) : deadline;
}

/**
 * Sleep until the wake time or an abort, whichever comes first.
 * @param wakeAtMs - epoch millisecond timestamp to sleep until
 * @param signal - abort signal that ends the sleep early
 * @returns a promise settling at the wake time or on abort
 */
function delayUntil(wakeAtMs: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve) => {
    const onAbort = (): void => {
      clearTimeout(timer);
      resolve();
    };
    const timer = setTimeout(() => {
      signal.removeEventListener('abort', onAbort);
      resolve();
    }, Math.max(0, wakeAtMs - Date.now()));
    signal.addEventListener('abort', onAbort, { once: true });
  });
}

/**
 * Wait for a durable operation to reach a terminal status with a bounded,
 * interruptible polling loop (report F010):
 *
 * - The overall deadline comes from `--wait-timeout` seconds (default 600) and
 *   is additionally clamped to the operation's `expiresAt` plus a grace
 *   window, so the wait always ends even when the gateway keeps answering
 *   PENDING/RUNNING forever.
 * - The poll delay grows exponentially from the base interval and is capped
 *   at 10 seconds; it also never sleeps past the deadline.
 * - SIGINT aborts the wait; the raised error carries the last observed status
 *   and the status URI so the operation can be inspected manually.
 *
 * Exported as the seam for the deadline/backoff/interrupt guard tests.
 * @param accepted - the 202 Accepted envelope of the submitted operation
 * @param pollIntervalMs - base polling interval in milliseconds (floored at 100)
 * @param waitTimeoutMs - caller-supplied maximum wait in milliseconds
 * @param interruptSignal - extra abort signal honored alongside SIGINT
 * @returns the operation view in a terminal status
 */
export async function waitForOperation(
  accepted: OperationAccepted,
  pollIntervalMs: number,
  waitTimeoutMs: number,
  interruptSignal?: AbortSignal,
): Promise<OperationView> {
  const startedAt = Date.now();
  let deadline = startedAt + waitTimeoutMs;
  let delayMs = Math.max(MIN_POLL_INTERVAL_MS, pollIntervalMs);
  let last: OperationView | undefined;

  const controller = new AbortController();
  const onSigint = (): void => controller.abort();
  const forwardAbort = (): void => controller.abort();
  process.on('SIGINT', onSigint);
  if (interruptSignal) {
    if (interruptSignal.aborted) {
      controller.abort();
    } else {
      interruptSignal.addEventListener('abort', forwardAbort);
    }
  }
  try {
    while (true) {
      if (controller.signal.aborted) {
        throw new OperationWaitInterrupted(accepted, last);
      }
      const operation = await dc3Client.get<OperationView>(operationPath(accepted.statusUri));
      last = operation;
      if (TERMINAL_OPERATION_STATUSES.has(operation.status)) {
        return operation;
      }
      deadline = clampDeadlineToOperationExpiry(deadline, operation.expiresAt);
      if (Date.now() >= deadline) {
        throw new OperationWaitTimeout(accepted, operation, Date.now() - startedAt);
      }
      await delayUntil(Date.now() + Math.min(delayMs, MAX_POLL_INTERVAL_MS, deadline - Date.now()), controller.signal);
      delayMs = Math.min(delayMs * 2, MAX_POLL_INTERVAL_MS);
    }
  } finally {
    process.removeListener('SIGINT', onSigint);
    interruptSignal?.removeEventListener('abort', forwardAbort);
  }
}

/**
 * Map an import file read failure to a structured validation error naming the
 * path exactly as the user supplied it: the raw errno message embeds the
 * resolved absolute path, which the CLI must not leak (report F047).
 * @param userPath - import file path as supplied on the command line
 * @param error - the error raised by readFile
 * @returns the validation error to raise
 */
function importReadError(userPath: string, error: unknown): ValidationError {
  const code = (error as { code?: unknown } | null)?.code;
  const reason =
    code === 'ENOENT'
      ? 'not found'
      : code === 'EISDIR'
        ? 'is a directory'
        : code === 'EACCES' || code === 'EPERM'
          ? 'is not readable'
          : 'could not be read';
  return new ValidationError(`Import file ${reason}: ${userPath}`);
}

/**
 * Register the `device` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerDeviceCommand(program: Command): void {
  const device = program.command('device').description('Device management');

  // dc3 device list
  device
    .command('list')
    .description('List devices')
    .option('--driver-id <id>', 'Filter by driver ID')
    .option('--profile-id <id>', 'Filter by profile ID')
    .option('--group-id <id>', 'Filter by group ID')
    .option('--offset <n>', 'Zero-based result offset', parseNonNegativeInteger, 0)
    .option('--limit <n>', 'Maximum items to return', parseNonNegativeInteger, 20)
    .option('--format <format>', 'Output format')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      // Manager list endpoints use POST with body query
      const body: Record<string, unknown> = {
        offset: opts.offset,
        limit: opts.limit,
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

  // dc3 device add (create kept as a one-release compat alias)
  device
    .command('add')
    .alias('create')
    .description('Add a new device')
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
      if (opts.description !== undefined) body.remark = opts.description;
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
        ...(opts.name !== undefined ? { deviceName: opts.name } : {}),
        ...(opts.driverId ? { driverId: opts.driverId } : {}),
        ...(opts.profileId ? { profileId: opts.profileId } : {}),
        ...(opts.description !== undefined ? { remark: opts.description } : {}),
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
        `/api/v3/manager/device/get_count_by_driver_id?driver_id=${encodeURIComponent(opts.driverId)}`,
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
      // Single-device status is a GET path param (DeviceStatusController L176),
      // not the POST /list query body used for paginated status listings.
      const result = await dc3Client.get(
        `/api/v3/data/device/status/${encodeURIComponent(id)}`,
      );
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
    .option(
      '--poll-interval <ms>',
      'Polling interval in milliseconds',
      parseNonNegativeInteger,
      500,
    )
    .option(
      '--wait-timeout <seconds>',
      'Maximum seconds to wait for the operation (also clamped by its expiry; 0 gives up after the first poll)',
      parseNonNegativeInteger,
      600,
    )
    .option('--format <format>', 'Output format')
    .action(async (file: string, opts) => {
      const format = detectFormat(opts.format);
      if (!file.toLowerCase().endsWith('.xlsx')) {
        throw new ValidationError('Import file must use the .xlsx extension');
      }
      let content: Buffer;
      try {
        content = await readFile(file);
      } catch (error) {
        throw importReadError(file, error);
      }
      if (content.length === 0) {
        throw new ValidationError('Import file must not be empty');
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
        // Pin the part disposition with an explicit filename instead of the
        // serializer default filename="blob" (report F046); the part keeps its
        // application/json content type, which is what the gateway
        // @RequestPart("request") converter keys on.
        'request.json',
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
      const operation = await waitForOperation(
        accepted,
        Math.max(100, opts.pollInterval),
        opts.waitTimeout * 1000,
      );
      printAndExit(operation, format, operation.status === 'SUCCEEDED' ? 0 : 1);
    });

  // dc3 device import-template
  device
    .command('import-template')
    .description('Download the XLSX import template for bulk device import')
    .requiredOption('--driver-id <id>', 'Driver ID to shape the template for')
    .requiredOption('--profile-id <id>', 'Profile ID to shape the template for')
    .option('--output <path>', 'Output file path (defaults to device-import-template.xlsx)')
    .option('--format <format>', 'Output format (ignored; always saves the binary)')
    .action(async (opts) => {
      const format = detectFormat(opts.format);
      const outputPath = opts.output || 'device-import-template.xlsx';
      // Binary XLSX download bypasses the JSON-decoding client pipeline; use
      // the shared fetch seam directly (same auth headers, same error mapping).
      const { fetchOrNetworkError, normalizeGateway } = await import('../core/http.js');
      const { configManager } = await import('../core/config-manager.js');
      const { tokenManager } = await import('../core/token-manager.js');
      const profile = await configManager.getActiveProfile();
      const profileName = await configManager.getActiveProfileName();
      const state = await tokenManager.getState(profileName);
      const headers: Record<string, string> = { 'Content-Type': 'application/json' };
      if (state) {
        Object.assign(headers, tokenManager.buildHeaders(state));
      }
      const res = await fetchOrNetworkError(
        `${normalizeGateway(profile.gateway)}/api/v3/manager/device/export/import_template`,
        {
          method: 'POST',
          headers,
          body: JSON.stringify({ driverId: opts.driverId, profileId: opts.profileId }),
        },
      );
      if (!res.ok) {
        const text = await res.text().catch(() => '');
        printAndExit({ ok: false, message: `Template download failed (${res.status}): ${text.slice(0, 200)}` }, format, 1);
      }
      const buffer = Buffer.from(await res.arrayBuffer());
      if (buffer.length === 0) {
        printAndExit({ ok: false, message: 'Template download returned empty body' }, format, 1);
      }
      const { writeFile } = await import('node:fs/promises');
      await writeFile(outputPath, buffer);
      printAndExit({ ok: true, path: outputPath, size: buffer.length }, format);
    });
}
