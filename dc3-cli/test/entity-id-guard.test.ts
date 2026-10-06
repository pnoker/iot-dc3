import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Command } from 'commander';

/*
 * Entity-table guard for the empty-id wire leak (report F053 residual): the
 * shared manager helpers validate ids, but the command layer used to bypass
 * them for `get` (and label/group `delete`), sending `?id=` to the gateway.
 * Every manager get/update/delete path of every entity must reject an
 * empty or whitespace id BEFORE any request is built.
 *
 * The second table (audit G13/G27) covers the NON-CRUD id-keyed surfaces —
 * positional ids and id-valued options on data-plane and agentic commands —
 * asserting zero HTTP requests on empty/whitespace ids.
 */

vi.mock('../src/core/client.js', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/core/client.js')>();
  return {
    ...actual,
    dc3Client: {
      get: vi.fn(async () => ({})),
      post: vi.fn(async () => ({})),
      del: vi.fn(async () => undefined),
      postForm: vi.fn(async () => ({})),
      request: vi.fn(async () => ({})),
      requestForBytes: vi.fn(async () => new Response(new Uint8Array([1]))),
    },
  };
});

import { dc3Client } from '../src/core/client.js';
import { registerDeviceCommand } from '../src/commands/device.js';
import { registerDriverCommand } from '../src/commands/driver.js';
import { registerPointCommand } from '../src/commands/point.js';
import { registerProfileCommand } from '../src/commands/profile.js';
import { registerEventCommand } from '../src/commands/event.js';
import { registerCommandCommand } from '../src/commands/command.js';
import { registerGroupCommand } from '../src/commands/group.js';
import { registerLabelCommand } from '../src/commands/label.js';
import { registerAlertCommand } from '../src/commands/alert.js';
import { registerAttachmentCommand } from '../src/commands/attachment.js';
import { registerModelCommand } from '../src/commands/model.js';
import { registerProviderCommand } from '../src/commands/provider.js';
import { registerChatCommand } from '../src/commands/chat.js';
import { registerSessionCommand, registerActionCommand } from '../src/commands/session.js';

type RegisterFn = (_program: Command) => void;

/** The full manager entity table: name + command-tree registrar. */
const ENTITY_TABLE: Array<{ name: string; register: RegisterFn }> = [
  { name: 'device', register: registerDeviceCommand },
  { name: 'driver', register: registerDriverCommand },
  { name: 'point', register: registerPointCommand },
  { name: 'profile', register: registerProfileCommand },
  { name: 'event', register: registerEventCommand },
  { name: 'command', register: registerCommandCommand },
  { name: 'group', register: registerGroupCommand },
  { name: 'label', register: registerLabelCommand },
];

/**
 * Run one entity subcommand in-process and capture the escaped error.
 * @param register - registrar for the entity command tree
 * @param args - arguments after the entity name
 * @returns the classification-ready error thrown by the action
 */
async function runEntity(register: RegisterFn, args: string[]): Promise<unknown> {
  const program = new Command();
  program.exitOverride();
  program.configureOutput({ writeOut: () => undefined, writeErr: () => undefined });
  register(program);
  try {
    await program.parseAsync(args, { from: 'user' });
  } catch (error) {
    if ((error as Error)?.name === 'SilentExit') {
      return undefined;
    }
    return error;
  }
  return undefined;
}

beforeEach(() => {
  vi.clearAllMocks();
});

/**
 * Every dc3Client request method, for the zero-HTTP assertions. postForm,
 * request, and requestForBytes are typed loosely because the mock object is
 * inferred.
 */
const requestMocks = [
  dc3Client.get,
  dc3Client.post,
  dc3Client.del,
  (dc3Client as unknown as { postForm: ReturnType<typeof vi.fn> }).postForm,
  (dc3Client as unknown as { request: ReturnType<typeof vi.fn> }).request,
  (dc3Client as unknown as { requestForBytes: ReturnType<typeof vi.fn> }).requestForBytes,
] as const;

/**
 * Build a program carrying every command tree the second table drives.
 * @returns the commander program with all registrars applied
 */
function buildFullProgram(): Command {
  const program = new Command();
  program.exitOverride();
  program.configureOutput({ writeOut: () => undefined, writeErr: () => undefined });
  registerDeviceCommand(program);
  registerCommandCommand(program);
  registerAlertCommand(program);
  registerAttachmentCommand(program);
  registerModelCommand(program);
  registerProviderCommand(program);
  registerPointCommand(program);
  registerChatCommand(program);
  registerSessionCommand(program);
  registerActionCommand(program);
  return program;
}

/**
 * Parse one argv against the full program and capture the escaped error
 * (SilentExit counts as success).
 * @param args - full argv including the group name
 * @returns the error thrown by the action, or undefined on success
 */
async function runFull(args: string[]): Promise<unknown> {
  try {
    await buildFullProgram().parseAsync(args, { from: 'user' });
  } catch (error) {
    if ((error as Error)?.name === 'SilentExit') {
      return undefined;
    }
    return error;
  }
  return undefined;
}

describe('empty resource ids never reach the wire (F053 residual)', () => {
  it.each(ENTITY_TABLE)('$name get "" rejects the empty id before any request', async (entity) => {
    const error = await runEntity(entity.register, [entity.name, 'get', '']);

    expect(error).toBeInstanceOf(Error);
    expect((error as Error).name).toBe('ValidationError');
    expect((error as Error).message).toMatch(/non-empty/u);
    expect(dc3Client.get).not.toHaveBeenCalled();
    expect(dc3Client.post).not.toHaveBeenCalled();
    expect(dc3Client.del).not.toHaveBeenCalled();
  });

  it.each(ENTITY_TABLE)('$name delete "" rejects the empty id before any request', async (entity) => {
    const error = await runEntity(entity.register, [
      entity.name,
      'delete',
      '',
      '--version',
      '1',
    ]);

    expect(error).toBeInstanceOf(Error);
    expect((error as Error).name).toBe('ValidationError');
    expect(dc3Client.get).not.toHaveBeenCalled();
    expect(dc3Client.post).not.toHaveBeenCalled();
    expect(dc3Client.del).not.toHaveBeenCalled();
  });

  it.each(ENTITY_TABLE)('$name update "" rejects the empty id before any request', async (entity) => {
    const error = await runEntity(entity.register, [
      entity.name,
      'update',
      ' ',
      '--version',
      '1',
    ]);

    expect(error).toBeInstanceOf(Error);
    expect((error as Error).name).toBe('ValidationError');
    expect(dc3Client.get).not.toHaveBeenCalled();
    expect(dc3Client.post).not.toHaveBeenCalled();
    expect(dc3Client.del).not.toHaveBeenCalled();
  });

  it('a valid id still reaches the wire through the shared helper', async () => {
    const error = await runEntity(registerDeviceCommand, ['device', 'get', '42']);

    expect(error).toBeUndefined();
    expect(dc3Client.get).toHaveBeenCalledWith(
      '/api/v3/manager/device/get_by_id?id=42',
    );
  });
});

describe('empty ids on non-CRUD id-keyed surfaces never reach the wire (G13/G27)', () => {
  /** One id-keyed surface and the argv that drives it with an empty id. */
  const CASES: Array<{ label: string; args: string[] }> = [
    { label: 'device status', args: ['device', 'status', ''] },
    { label: 'device count --driver-id', args: ['device', 'count', '--driver-id', ''] },
    { label: 'command history', args: ['command', 'history', ' '] },
    { label: 'alert confirm --id', args: ['alert', 'confirm', '--source', 'device', '--id', ''] },
    { label: 'alert unconfirm --id', args: ['alert', 'unconfirm', '--source', 'device', '--id', '  '] },
    { label: 'alert point-profile', args: ['alert', 'point-profile', ''] },
    { label: 'model delete', args: ['model', 'delete', ''] },
    { label: 'model check', args: ['model', 'check', ' '] },
    { label: 'provider delete', args: ['provider', 'delete', ''] },
    {
      label: 'attachment upload --conversation-id',
      args: ['attachment', 'upload', 'file.bin', '--conversation-id', ''],
    },
    { label: 'attachment list --conversation-id', args: ['attachment', 'list', '--conversation-id', ' '] },
    {
      label: 'attachment delete',
      args: ['attachment', 'delete', ''],
    },
    {
      label: 'device import-template --driver-id',
      args: ['device', 'import-template', '--driver-id', '', '--profile-id', '100'],
    },
    {
      label: 'device import-template --profile-id',
      args: ['device', 'import-template', '--driver-id', '10', '--profile-id', '  '],
    },
    { label: 'point read', args: ['point', 'read', ''] },
    { label: 'point history', args: ['point', 'history', ' ', '--device-id', 'dev-1'] },
    { label: 'point history --device-id', args: ['point', 'history', '456', '--device-id', ''] },
    { label: 'point write', args: ['point', 'write', '', '--device-id', 'dev-1', '--value', '1'] },
    { label: 'point write --device-id', args: ['point', 'write', '456', '--device-id', '', '--value', '1'] },
    { label: 'chat --conversation-id', args: ['chat', 'hello', '--conversation-id', ''] },
    { label: 'session get', args: ['session', 'get', ''] },
    { label: 'session messages', args: ['session', 'messages', '  '] },
    { label: 'session rename', args: ['session', 'rename', '', '--name', 'n'] },
    { label: 'session delete', args: ['session', 'delete', '', '--yes'] },
    { label: 'action confirm', args: ['action', 'confirm', '', '--yes'] },
    { label: 'action reject', args: ['action', 'reject', ' ', '--yes'] },
    { label: 'action pending --conversation-id', args: ['action', 'pending', '--conversation-id', ''] },
  ];

  it.each(CASES)('$label rejects the empty id before any request', async ({ args }) => {
    const error = await runFull(args);

    expect(error).toBeInstanceOf(Error);
    expect(error).toMatchObject({ name: 'ValidationError', kind: 'validation', exitCode: 1 });
    expect((error as Error).message).toMatch(/non-empty/u);
    for (const mock of requestMocks) {
      expect(mock).not.toHaveBeenCalled();
    }
  });

  it('a valid alert point-profile id still reaches the wire', async () => {
    const error = await runFull(['alert', 'point-profile', 'pt-1']);

    expect(error).toBeUndefined();
    expect(dc3Client.get).toHaveBeenCalledWith(
      '/api/v3/data/dashboard/alert/point_profile?point_id=pt-1',
    );
  });
});
