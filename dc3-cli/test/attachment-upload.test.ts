import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { createServer, type Server } from 'node:http';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Command } from 'commander';
import { SilentExit } from '../src/utils/format.js';

/**
 * Gateway URL of the capture server; assigned once the listener is up (read
 * lazily inside the config-manager mock closures).
 */
let gatewayUrl = '';

vi.mock('../src/core/config-manager.js', () => ({
  configManager: {
    getActiveProfile: vi.fn(async () => ({ gateway: gatewayUrl })),
    getActiveProfileName: vi.fn(async () => 'default'),
    getSettings: vi.fn(async () => ({})),
    load: vi.fn(async () => ({ current_profile: 'default' })),
  },
}));

vi.mock('../src/core/token-manager.js', () => ({
  tokenManager: {
    getState: vi.fn(async () => null),
    needsRenewal: vi.fn(async () => false),
    buildHeaders: vi.fn(),
  },
}));

import { registerAttachmentCommand } from '../src/commands/attachment.js';

/** One captured multipart part: raw header lines plus the exact payload bytes. */
interface CapturedPart {
  disposition: string;
  contentType: string | null;
  data: Buffer;
}

/** Everything the capture server saw for one upload request. */
interface CapturedRequest {
  method: string;
  url: string;
  contentType: string;
  body: Buffer;
}

let captured: CapturedRequest | null = null;

/**
 * Split a raw multipart body into its parts, keeping the payload as raw
 * bytes so binary fixtures compare exactly.
 * @param body - raw request body bytes
 * @param boundary - multipart boundary from the Content-Type header
 * @returns the captured parts in order
 */
function parseParts(body: Buffer, boundary: string): CapturedPart[] {
  const delimiter = Buffer.from(`--${boundary}`);
  const parts: CapturedPart[] = [];
  let start = body.indexOf(delimiter);
  while (start !== -1) {
    const next = body.indexOf(delimiter, start + delimiter.length);
    if (next === -1) break;
    let cursor = start + delimiter.length;
    if (body[cursor] === 0x0d && body[cursor + 1] === 0x0a) cursor += 2;
    const segment = body.subarray(cursor, next - 2); // strip the \r\n before the delimiter
    const headerEnd = segment.indexOf('\r\n\r\n');
    if (headerEnd !== -1) {
      const headerText = segment.subarray(0, headerEnd).toString('utf8');
      parts.push({
        disposition: /content-disposition:\s*(.+)/iu.exec(headerText)?.[1] ?? '',
        contentType: /content-type:\s*(.+)/iu.exec(headerText)?.[1] ?? null,
        data: Buffer.from(segment.subarray(headerEnd + 4)),
      });
    }
    start = next;
  }
  return parts;
}

/** Run the attachment command against the capture server. */
async function run(args: string[]): Promise<{ stdout: string; exitCode: number }> {
  const program = new Command();
  program.exitOverride();
  registerAttachmentCommand(program);
  let stdout = '';
  process.exitCode = 0;
  const outSpy = vi
    .spyOn(process.stdout, 'write')
    .mockImplementation((chunk) => ((stdout += String(chunk)), true));
  try {
    try {
      await program.parseAsync(args, { from: 'user' });
    } catch (error) {
      if (!(error instanceof SilentExit)) throw error;
    }
  } finally {
    outSpy.mockRestore();
  }
  return { stdout, exitCode: process.exitCode ?? 0 };
}

describe('attachment upload multipart wire format (F003)', () => {
  // One capture server for the whole file: the dc3Client singleton caches the
  // resolved gateway after the first request, so the port must stay stable
  // across tests.
  let server: Server;
  let directory: string;

  beforeEach(async () => {
    captured = null;
    directory = await mkdtemp(join(tmpdir(), 'dc3-cli-attachment-'));
  });

  beforeAll(async () => {
    server = createServer((req, res) => {
      const chunks: Buffer[] = [];
      req.on('data', (chunk: Buffer) => chunks.push(chunk));
      req.on('end', () => {
        captured = {
          method: req.method ?? '',
          url: req.url ?? '',
          contentType: String(req.headers['content-type'] ?? ''),
          body: Buffer.concat(chunks),
        };
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ id: 'att-1', conversationId: 'conv-1' }));
      });
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    gatewayUrl = `http://127.0.0.1:${server.address().port}`;
  });

  afterAll(async () => {
    await new Promise<void>((resolve) => server.close(() => resolve()));
  });

  afterEach(async () => {
    await rm(directory, { recursive: true, force: true });
    process.exitCode = 0;
  });

  it('uploads the exact file bytes as a multipart `file` part with a non-ASCII filename', async () => {
    const fixture = Buffer.from(Array.from({ length: 256 }, (_, i) => i));
    const fileName = '数据-附件.bin';
    const file = join(directory, fileName);
    await writeFile(file, fixture);

    const { stdout, exitCode } = await run([
      'attachment', 'upload', file, '--conversation-id', 'conv 1', '--format', 'json',
    ]);

    expect(exitCode).toBe(0);
    expect(JSON.parse(stdout)).toMatchObject({ id: 'att-1' });
    expect(captured?.method).toBe('POST');
    expect(captured?.url).toBe('/api/v3/agentic/attachment/upload?conversation_id=conv%201');

    // (a) transport-level content type is multipart with a fetch-generated boundary
    expect(captured?.contentType.startsWith('multipart/form-data; boundary=')).toBe(true);
    // negative: the JSON-serialized-Buffer regression must never come back
    expect(captured?.contentType.startsWith('application/json')).toBe(false);
    expect(captured?.body.subarray(0, 1).toString('utf8')).not.toBe('{');

    // (b) a `file` part exists whose bytes equal the fixture exactly and
    // whose disposition carries the (non-ASCII) filename
    const boundary = /boundary=(.+)$/u.exec(captured?.contentType ?? '')?.[1] ?? '';
    const parts = parseParts(captured?.body ?? Buffer.alloc(0), boundary);
    const filePart = parts.find((part) => part.disposition.includes('name="file"'));
    expect(filePart, 'a part named "file" exists').toBeDefined();
    expect(filePart?.disposition).toContain(`filename="${fileName}"`);
    expect(filePart?.data.equals(fixture)).toBe(true);
    expect(parts).toHaveLength(1);
  });

  it('rejects an unreadable file with a structured validation error before any request', async () => {
    await expect(
      run(['attachment', 'upload', join(directory, 'missing.bin'), '--conversation-id', 'c1', '--format', 'json']),
    ).rejects.toMatchObject({ name: 'ValidationError', message: /Cannot read file:/u });
    expect(captured).toBeNull();
  });

  it('rejects an empty file before any request', async () => {
    const empty = join(directory, 'empty.bin');
    await writeFile(empty, Buffer.alloc(0));
    await expect(
      run(['attachment', 'upload', empty, '--conversation-id', 'c1', '--format', 'json']),
    ).rejects.toMatchObject({ name: 'ValidationError', message: 'Attachment file must not be empty' });
    expect(captured).toBeNull();
  });

  it('lists attachments for a conversation', async () => {
    const { stdout, exitCode } = await run([
      'attachment', 'list', '--conversation-id', 'conv 1', '--format', 'json',
    ]);
    expect(exitCode).toBe(0);
    expect(captured?.url).toBe('/api/v3/agentic/attachment/list?conversation_id=conv%201');
    expect(JSON.parse(stdout)).toMatchObject({ id: 'att-1' });
  });
});
