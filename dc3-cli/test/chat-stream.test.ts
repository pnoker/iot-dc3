import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createServer, type Server, type Socket } from 'node:http';
import { Command } from 'commander';
import { SilentExit } from '../src/utils/format.js';
import { NetworkError, handleFatalError } from '../src/core/errors.js';
import { applyGlobalOptions, resetCliContext } from '../src/core/context.js';

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

/**
 * Run the chat command, then report the escaped error through the real failure
 * chokepoint (exactly as the CLI entry point does) so both output channels and
 * the exit code can be asserted end to end.
 * @param args - arguments after `chat`
 * @returns captured stdout/stderr and the error that escaped the action
 */
async function runFatal(args: string[]): Promise<{ stdout: string; stderr: string; error: Error }> {
  const program = new Command();
  program.exitOverride();
  registerChatCommand(program);
  let stdout = '';
  let stderr = '';
  let failure: Error | undefined;
  process.exitCode = 0;
  const outSpy = vi
    .spyOn(process.stdout, 'write')
    .mockImplementation((chunk) => ((stdout += String(chunk)), true));
  const errSpy = vi
    .spyOn(process.stderr, 'write')
    .mockImplementation((chunk) => ((stderr += String(chunk)), true));
  try {
    try {
      await program.parseAsync(args, { from: 'user' });
    } catch (error) {
      if (!(error instanceof SilentExit)) {
        failure = error as Error;
        try {
          // The entry-point contract: report on both channels, then rethrow.
          handleFatalError(failure);
        } catch {
          // Expected rethrow — both channels were already written.
        }
      }
    }
  } finally {
    outSpy.mockRestore();
    errSpy.mockRestore();
  }
  return { stdout, stderr, error: failure as Error };
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

describe('chat --stream wire-form tolerance (Spring WebFlux production forms)', () => {
  let server: Server;
  const sockets = new Set<Socket>();

  afterEach(async () => {
    for (const socket of sockets) {
      socket.destroy();
    }
    sockets.clear();
    await new Promise<void>((resolve) => server.close(() => resolve()));
    process.exitCode = 0;
  });

  it(
    'streams identically from frames without the space after "data:" and terminates on [DONE]',
    async () => {
      server = createServer((req, res) => {
        res.writeHead(200, {
          'Content-Type': 'text/event-stream; charset=utf-8',
          'Cache-Control': 'no-cache',
        });
        // Spring WebFlux ServerSentEvent serialization emits `data:{json}`
        // with NO space after the colon — the production form this parser
        // must accept, including the terminator frame.
        res.write(`data:${JSON.stringify({ choices: [{ delta: { content: 'Hello ' } }] })}\n\n`);
        res.write(`data:${JSON.stringify({ choices: [{ delta: { content: 'world' } }] })}\n\n`);
        res.write('data:[DONE]\n\n');
        // Hold the connection open: terminating on the no-space [DONE] frame
        // alone (not on the server closing) is the asserted behavior.
        const heartbeat = setInterval(() => res.write(': hb\n\n'), 200);
        res.on('close', () => clearInterval(heartbeat));
      });
      server.on('connection', (socket) => {
        sockets.add(socket);
        socket.on('close', () => sockets.delete(socket));
      });
      await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
      gatewayUrl = `http://127.0.0.1:${server.address().port}`;

      const startedAt = Date.now();
      const { stdout, exitCode } = await run(['chat', 'hi', '--stream']);

      expect(Date.now() - startedAt).toBeLessThan(2000);
      expect(stdout).toBe('Hello world\n');
      expect(exitCode).toBe(0);
    },
    2500,
  );

  it(
    'terminates on a CRLF-framed [DONE] while the server holds the connection open',
    async () => {
      server = createServer((req, res) => {
        res.writeHead(200, {
          'Content-Type': 'text/event-stream; charset=utf-8',
          'Cache-Control': 'no-cache',
        });
        res.write(`data: ${JSON.stringify({ choices: [{ delta: { content: 'Hello ' } }] })}\r\n\r\n`);
        res.write(`data: ${JSON.stringify({ choices: [{ delta: { content: 'world' } }] })}\r\n\r\n`);
        // The terminator carries the CR of the CRLF frame ending: without
        // stripping it, `data === '[DONE]'` never matches and the process
        // hangs until the server closes (report F023, CRLF variant).
        res.write('data: [DONE]\r\n\r\n');
        const heartbeat = setInterval(() => res.write(': hb\r\n\r\n'), 200);
        res.on('close', () => clearInterval(heartbeat));
      });
      server.on('connection', (socket) => {
        sockets.add(socket);
        socket.on('close', () => sockets.delete(socket));
      });
      await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
      gatewayUrl = `http://127.0.0.1:${server.address().port}`;

      const startedAt = Date.now();
      const { stdout, exitCode } = await run(['chat', 'hi', '--stream']);

      expect(Date.now() - startedAt).toBeLessThan(2000);
      expect(stdout).toBe('Hello world\n');
      expect(exitCode).toBe(0);
    },
    2500,
  );

  it(
    'parses CRLF-framed streams with identical output and a single trailing newline',
    async () => {
      server = createServer((req, res) => {
        res.writeHead(200, {
          'Content-Type': 'text/event-stream; charset=utf-8',
          'Cache-Control': 'no-cache',
        });
        res.write(`data: ${JSON.stringify({ choices: [{ delta: { content: 'Hello ' } }] })}\r\n\r\n`);
        res.write(`data: ${JSON.stringify({ choices: [{ delta: { content: 'world' } }] })}\r\n\r\n`);
        res.write('data: [DONE]\r\n\r\n');
        // This variant closes the response itself shortly after [DONE]: the
        // parser tolerates the CR on content lines (JSON whitespace), and
        // byte-identical output proves no '\r' leaks into the stream.
        setTimeout(() => res.end(), 300);
      });
      server.on('connection', (socket) => {
        sockets.add(socket);
        socket.on('close', () => sockets.delete(socket));
      });
      await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
      gatewayUrl = `http://127.0.0.1:${server.address().port}`;

      const startedAt = Date.now();
      const { stdout, exitCode } = await run(['chat', 'hi', '--stream']);

      expect(Date.now() - startedAt).toBeLessThan(2000);
      expect(stdout).toBe('Hello world\n');
      expect(exitCode).toBe(0);
    },
    2500,
  );
});

describe('chat --stream mid-stream failure mapping and envelope suppression', () => {
  let server: Server;
  const sockets = new Set<Socket>();

  afterEach(async () => {
    for (const socket of sockets) {
      socket.destroy();
    }
    sockets.clear();
    await new Promise<void>((resolve) => server.close(() => resolve()));
    resetCliContext();
    process.exitCode = 0;
  });

  it(
    'a socket cut after the first content frame reports NetworkError exit 2 with no envelope on stdout',
    async () => {
      // Pin the failure format to json: the envelope WOULD be appended to
      // stdout by the chokepoint if the suppression flag were not set — this
      // makes the "no envelope" assertion below a real guard, not a default.
      await applyGlobalOptions({ format: 'json' });

      server = createServer((req, res) => {
        res.writeHead(200, {
          'Content-Type': 'text/event-stream; charset=utf-8',
          'Cache-Control': 'no-cache',
        });
        res.write(`data: ${JSON.stringify({ choices: [{ delta: { content: 'Hello ' } }] })}\n\n`);
        // Cut the TCP connection after the frame is on the wire: the body
        // reader must reject mid-stream instead of hanging on a half-open
        // response (the :150-153 mapping under guard).
        setTimeout(() => res.socket?.destroy(), 150);
      });
      server.on('connection', (socket) => {
        sockets.add(socket);
        socket.on('close', () => sockets.delete(socket));
      });
      await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
      gatewayUrl = `http://127.0.0.1:${server.address().port}`;

      const { stdout, stderr, error } = await runFatal(['chat', 'hi', '--stream']);

      // Mid-stream reader rejections surface as NetworkError (exit 2), never
      // as an uncaught TypeError escaping the stream loop.
      expect(error).toBeInstanceOf(NetworkError);
      expect(error.message).toMatch(/^Chat stream interrupted/u);
      expect(process.exitCode).toBe(2);
      // The chokepoint contract: exactly one human line on stderr.
      expect(stderr.trim().split('\n')).toHaveLength(1);
      expect(stderr).toContain('Error: Chat stream interrupted');
      // stdout keeps the partial content and never gains a JSON failure
      // document: suppressFailureEnvelope fired before the first content
      // write (deleting or misordering that call fails this assertion).
      expect(stdout).toContain('Hello ');
      expect(stdout).not.toContain('{"ok":false');
    },
    5000,
  );
});
