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
import { createInterface } from 'node:readline';
import { Writable } from 'node:stream';
import { UsageError } from '../core/errors.js';

/**
 * Private no-op sink used as the readline output. Prompts never bind readline
 * to `process.stdout` and never mutate it (no `_write` override, no write
 * rebinding): piped stdout stays byte-clean for machine consumers, and output
 * the CLI prints after a prompt still reaches the terminal (report F013).
 * @returns a writable sink that discards everything readline writes
 */
function devNullOutput(): Writable {
  return new Writable({
    write(_chunk, _encoding, callback) {
      callback();
    },
  });
}

/**
 * Shared implementation of every interactive question. Contract (spec item 4):
 *
 * - The question text goes to STDERR; readline's own output goes to a private
 *   no-op sink. `process.stdout` is never touched.
 * - The returned promise ALWAYS settles: when stdin closes before an answer
 *   arrives (EOF, closed pipe) or the user presses Ctrl+C, it rejects with a
 *   {@link UsageError} instead of leaving the event loop to drain into a
 *   silent exit 0 (report F004).
 * - Callers gate on `stdin.isTTY` when an answer is mandatory — this settle
 *   contract is the defense in depth underneath that gate.
 * @param question - prompt text shown to the user on stderr
 * @returns the collected, trimmed answer
 */
function ask(question: string): Promise<string> {
  // Read the process streams lazily (never via a module-level named import):
  // the property lookup keeps tests able to substitute stdin, and production
  // behavior is identical.
  process.stderr.write(question);
  const rl = createInterface({ input: process.stdin, output: devNullOutput(), terminal: false });

  // First termination wins; later events (e.g. the close fired by our own
  // cleanup) are ignored. settleFn is assigned synchronously by the promise
  // executor below, before any event can fire.
  let settled = false;
  let settleFn: ((_result: PromiseSettledResult<string>) => void) | null = null;

  const finish = (result: PromiseSettledResult<string>): void => {
    if (settled) return;
    settled = true;
    process.removeListener('SIGINT', onSigint);
    rl.close();
    settleFn?.(result);
  };

  const onSigint = (): void =>
    finish({
      status: 'rejected',
      reason: new UsageError(
        'interactive input was interrupted before an answer was provided (SIGINT); ' +
          'pass the required flags or run dc3 in an interactive terminal',
      ),
    });

  return new Promise<string>((resolve, reject) => {
    settleFn = (result) =>
      result.status === 'fulfilled' ? resolve(result.value) : reject(result.reason);

    // EOF before (or while waiting for) an answer: the question callback never
    // fires, so termination must come from the interface's close event.
    rl.once('close', () =>
      finish({
        status: 'rejected',
        reason: new UsageError(
          'interactive input required but stdin is closed (no answer received); ' +
            'pass the required flags or run dc3 in an interactive terminal',
        ),
      }),
    );
    // Ctrl+C: readline only emits SIGINT itself in terminal mode; in
    // non-terminal mode the signal arrives at the process instead.
    rl.once('SIGINT', onSigint);
    process.once('SIGINT', onSigint);

    rl.question(question, (answer) => {
      finish({ status: 'fulfilled', value: answer.trim() });
    });
  });
}

/**
 * Interactively prompt the user for input. Question on stderr, stdin read in
 * line mode, `process.stdout` untouched; rejects with a {@link UsageError}
 * when stdin closes or is interrupted (report F004/F013).
 * @param question - prompt text shown to the user
 * @returns the collected user input
 */
export async function prompt(question: string): Promise<string> {
  return ask(question);
}

/**
 * Interactively prompt for a password or secret. Same channel contract as
 * {@link prompt}: the question goes to stderr and readline writes to a private
 * no-op sink, so nothing is echoed and `process.stdout` is never mutated —
 * hiding must not come from breaking the process stream (report F013).
 * @param question - prompt text shown to the user
 * @returns the collected user input
 */
export async function passwordPrompt(question: string): Promise<string> {
  return ask(question);
}

/**
 * Confirm a yes/no action. Settles exactly like {@link prompt} (UsageError on
 * closed/interrupted stdin); callers gate on `stdin.isTTY` for mandatory
 * confirmations and offer an explicit `--yes` escape for scripts.
 * @param question - prompt text shown to the user
 * @returns true only for an explicit y/yes answer
 */
export async function confirm(question: string): Promise<boolean> {
  const answer = await ask(`${question} [y/N] `);
  const normalized = answer.toLowerCase();
  return normalized === 'y' || normalized === 'yes';
}
