import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { Readable } from 'node:stream';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

/*
 * Prompt contract guards (reports F004/F013, spec item 4):
 * - questions go to stderr, readline output to a private sink — process.stdout
 *   is never written to, never given an own `_write`, and stays drainable;
 * - every prompt settles: EOF rejects with a UsageError, SIGINT rejects with a
 *   UsageError, and the temporary SIGINT listener is removed afterwards.
 *
 * prompt.ts captures `stdin` from node:process at module evaluation, so each
 * test installs a fake stdin FIRST and then imports the module fresh.
 */

const promptModuleUrl = pathToFileURL(
  join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'utils', 'prompt.ts'),
).href;

/** Descriptor of the real process.stdin, restored after every test. */
let originalStdin: PropertyDescriptor;

/** Install a controllable stdin and (re)import the prompt module against it. */
async function freshPrompt(tty = true): Promise<{
  prompt: (question: string) => Promise<string>;
  passwordPrompt: (question: string) => Promise<string>;
  confirm: (question: string) => Promise<boolean>;
  stdin: Readable;
}> {
  const fake = new Readable({ read() {} });
  Object.defineProperty(fake, 'isTTY', { value: tty ? {} : undefined, configurable: true });
  Object.defineProperty(process, 'stdin', { value: fake, configurable: true });
  vi.resetModules();
  const module = (await import(promptModuleUrl)) as {
    prompt: (question: string) => Promise<string>;
    passwordPrompt: (question: string) => Promise<string>;
    confirm: (question: string) => Promise<boolean>;
  };
  return { ...module, stdin: fake };
}

/** Race a promise against a timeout so a regression shows up as a failure, not a hang. */
function withTimeout<T>(promise: Promise<T>, ms: number, label: string): Promise<T> {
  let timer: ReturnType<typeof setTimeout>;
  const timeout = new Promise<never>((_, reject) => {
    timer = setTimeout(() => reject(new Error(`${label}: promise did not settle within ${ms}ms`)), ms);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer!));
}

describe('prompt channel contract (F013)', () => {
  let stdoutWrites: string[];
  let stderrWrites: string[];

  beforeEach(() => {
    originalStdin = Object.getOwnPropertyDescriptor(process, 'stdin')!;
    stdoutWrites = [];
    stderrWrites = [];
    vi.spyOn(process.stdout, 'write').mockImplementation((chunk) => {
      stdoutWrites.push(String(chunk));
      return true;
    });
    vi.spyOn(process.stderr, 'write').mockImplementation((chunk) => {
      stderrWrites.push(String(chunk));
      return true;
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    Object.defineProperty(process, 'stdin', originalStdin);
    vi.resetModules();
  });

  it('writes the question to stderr and never to stdout', async () => {
    const { prompt, stdin } = await freshPrompt();
    const pending = withTimeout(prompt('Tenant: '), 1000, 'prompt');
    stdin.push('acme\n');
    await expect(pending).resolves.toBe('acme');
    expect(stderrWrites.join('')).toContain('Tenant: ');
    expect(stdoutWrites).toEqual([]);
  });

  it('passwordPrompt leaves process.stdout fully intact (negative F013 guard)', async () => {
    const { passwordPrompt, stdin } = await freshPrompt();
    const pending = withTimeout(passwordPrompt('Password: '), 1000, 'passwordPrompt');
    stdin.push('dc3dc3dc3\n');
    await expect(pending).resolves.toBe('dc3dc3dc3');

    // The defect left stdout with a stuck writableLength that never drained:
    // zero here means the prompt never buffered anything into it. (The
    // own-`_write` negative check lives in the spawn-level fixture — vitest
    // itself patches process.stdout in-process, so it cannot be asserted here.)
    expect(process.stdout.writableLength).toBe(0);
    expect(process.stdout.destroyed).toBe(false);
    // Output written after the prompt still goes through (captured by the spy).
    process.stdout.write('after-prompt\n');
    expect(stdoutWrites).toEqual(['after-prompt\n']);
  });

  it('confirm resolves y to true and anything else to false', async () => {
    const yes = await freshPrompt();
    const yesPending = withTimeout(yes.confirm('Proceed?'), 1000, 'confirm-yes');
    yes.stdin.push('y\n');
    await expect(yesPending).resolves.toBe(true);

    const no = await freshPrompt();
    const noPending = withTimeout(no.confirm('Proceed?'), 1000, 'confirm-no');
    no.stdin.push('nope\n');
    await expect(noPending).resolves.toBe(false);
    expect(stderrWrites.join('')).toContain('[y/N]');
  });
});

describe('prompt termination contract (F004)', () => {
  beforeEach(() => {
    originalStdin = Object.getOwnPropertyDescriptor(process, 'stdin')!;
    vi.spyOn(process.stderr, 'write').mockImplementation(() => true);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    Object.defineProperty(process, 'stdin', originalStdin);
    vi.resetModules();
  });

  it('rejects with a UsageError when stdin closes mid-question (EOF)', async () => {
    const { passwordPrompt, stdin } = await freshPrompt();
    const pending = passwordPrompt('Password: ');
    stdin.push(null); // EOF with no data ever arriving
    const error = await withTimeout(
      pending.then(
        () => undefined,
        (err: unknown) => err,
      ),
      1000,
      'eof-rejection',
    );
    expect(error?.name).toBe('UsageError');
    expect(error?.message).toMatch(/stdin is closed/u);
    expect((error as { exitCode?: number })?.exitCode).toBe(1);
  });

  it('settles even when stdin is already closed when the prompt starts', async () => {
    const api = await freshPrompt();
    api.stdin.push(null); // closed before any question
    const error = await withTimeout(
      api.prompt('Tenant: ').then(
        () => undefined,
        (err: unknown) => err,
      ),
      1000,
      'pre-closed-stdin',
    );
    expect(error?.name).toBe('UsageError');
  });

  it('SIGINT rejects with a UsageError and removes its process listener', async () => {
    const { prompt } = await freshPrompt();
    const before = process.listenerCount('SIGINT');
    const pending = prompt('Tenant: ');
    process.emit('SIGINT');
    const error = await withTimeout(
      pending.then(
        () => undefined,
        (err: unknown) => err,
      ),
      1000,
      'sigint-rejection',
    );
    expect(error?.name).toBe('UsageError');
    expect(error?.message).toMatch(/SIGINT/u);
    expect((error as { exitCode?: number })?.exitCode).toBe(1);
    // The prompt must not leak its temporary process-level listener.
    expect(process.listenerCount('SIGINT')).toBe(before);
  });

  it('confirm rejects (never resolves) when stdin closes', async () => {
    const { confirm, stdin } = await freshPrompt();
    const pending = confirm('Proceed?');
    stdin.push(null);
    await expect(withTimeout(pending, 1000, 'confirm-eof')).rejects.toMatchObject({
      name: 'UsageError',
    });
  });
});
