import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createServer, type Server, type Socket } from 'node:http';
import { Command } from 'commander';
import { SilentExit } from '../src/utils/format.js';

/**
 * Gateway URL of the hold-open SSE server; assigned once the listener is up
 * (read lazily inside the config-manager mock closures).
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

import { registerChatCommand } from '../src/commands/chat.js';

/**
 * Run the chat command and capture everything written to stdout.
 * @param args - arguments after `chat`
 * @returns captured stdout and effective exit code
 */
async function run(args: string[]): Promise<{ stdout: string; exitCode: number }> {
  const program = new Command();
  program.exitOverride();
  registerChatCommand(program);
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

describe('chat --stream termination on [DONE] (F023)', () => {
  let server: Server;
  const sockets = new Set<Socket>();

  beforeEach(async () => {
    server = createServer((req, res) => {
      res.writeHead(200, {
        'Content-Type': 'text/event-stream; charset=utf-8',
        'Cache-Control': 'no-cache',
      });
      // Comment frame first: keep-alive traffic must be skipped by the parser.
      res.write(': keep-alive\n\n');
      res.write(`data: ${JSON.stringify({ choices: [{ delta: { content: 'Hello ' } }] })}\n\n`);
      res.write(`data: ${JSON.stringify({ choices: [{ delta: { content: 'world' } }] })}\n\n`);
      // The protocol terminator...
      res.write('data: [DONE]\n\n');
      // ...and the hostile part: the gateway NEVER closes the connection
      // (heart-beating, session-reuse style). The CLI must terminate on
      // [DONE] alone and release the socket itself.
      const heartbeat = setInterval(() => res.write(': hb\n\n'), 200);
      res.on('close', () => clearInterval(heartbeat));
    });
    server.on('connection', (socket) => {
      sockets.add(socket);
      socket.on('close', () => sockets.delete(socket));
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    gatewayUrl = `http://127.0.0.1:${server.address().port}`;
  });

  afterEach(async () => {
    for (const socket of sockets) {
      socket.destroy();
    }
    sockets.clear();
    await new Promise<void>((resolve) => server.close(() => resolve()));
    process.exitCode = 0;
  });

  it(
    'finishes on [DONE] while the server holds the connection open, with one trailing newline',
    async () => {
      const startedAt = Date.now();
      const { stdout, exitCode } = await run(['chat', 'hi', '--stream']);
      const elapsedMs = Date.now() - startedAt;

      // The heartbeat keeps the socket alive forever; a regression back to
      // "wait for the server" hangs this promise until the test timeout.
      expect(elapsedMs).toBeLessThan(2000);
      expect(stdout).toBe('Hello world\n');
      expect(exitCode).toBe(0);
    },
    2500,
  );

  it('cancels the response body reader so the socket is released client-side', async () => {
    await run(['chat', 'hi', '--stream']);
    // reader.cancel() must destroy the connection even though the server
    // never ends the response; poll the tracked server-side sockets.
    const deadline = Date.now() + 1500;
    while (sockets.size > 0 && Date.now() < deadline) {
      await new Promise((resolve) => setTimeout(resolve, 25));
    }
    expect(sockets.size).toBe(0);
  });
});
