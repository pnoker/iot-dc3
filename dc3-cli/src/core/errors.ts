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
/**
 * Error taxonomy shared by every transport (REST client, MCP client, chat
 * stream) plus the single failure chokepoint every escaped error flows
 * through. Every failure is reported on two channels with one exit code:
 *
 * - STDERR gets a single human line `Error: <message>`.
 * - STDOUT gets a machine envelope `{"ok":false,"error":{"kind","code","message"}}`
 *   when the effective output format is json or yaml (agents that only parse
 *   stdout can detect the failure; table keeps stdout human-clean).
 *
 * Exit codes stay 0 ok / 1 business or usage error / 2 network error / 3 auth
 * error (compatibility decision: agents discriminate via `error.kind`).
 */
import { CommanderError } from 'commander';
import { ZodError } from 'zod';
import { getGlobalFormatOverride, getSettingsFormatOverride } from './context.js';

/**
 * Failure categories exposed in the machine envelope. Consumers discriminate
 * on `kind` instead of parsing stderr text.
 */
export type ErrorKind = 'usage' | 'validation' | 'auth' | 'network' | 'api' | 'timeout';

/**
 * Base class of the CLI error taxonomy. Carries the envelope `kind`, a stable
 * machine `code`, and the process `exitCode` the failure maps to.
 */
export class CliError extends Error {
  public readonly kind: ErrorKind;
  public readonly exitCode: number;
  public readonly code: string;

  constructor(
    message: string,
    details: { kind: ErrorKind; exitCode: number; code: string },
  ) {
    super(message);
    this.name = new.target.name;
    this.kind = details.kind;
    this.exitCode = details.exitCode;
    this.code = details.code;
  }
}

/**
 * Error thrown when the command line itself is malformed (unknown option or
 * command, invalid flag value, excess arguments). Exit code 1.
 */
export class UsageError extends CliError {
  constructor(message: string) {
    super(message, { kind: 'usage', exitCode: 1, code: 'USAGE' });
  }
}

/**
 * Error thrown when a value fails business validation before or after a
 * gateway round-trip (schema violations, invalid payloads). Exit code 1.
 */
export class ValidationError extends CliError {
  constructor(message: string) {
    super(message, { kind: 'validation', exitCode: 1, code: 'VALIDATION' });
  }
}

/**
 * Error thrown when the gateway rejects authentication (HTTP 401/403).
 * Exit code 3.
 */
export class AuthError extends CliError {
  constructor(message: string) {
    super(message, { kind: 'auth', exitCode: 3, code: 'AUTH' });
  }
}

/**
 * Error thrown when a request never reaches the gateway (DNS, socket, TLS,
 * timeout, or connection-refused failures surfaced by fetch). Exit code 2.
 */
export class NetworkError extends CliError {
  constructor(message: string) {
    super(message, { kind: 'network', exitCode: 2, code: 'NETWORK' });
  }
}

/**
 * Error thrown when a long-running operation exceeds its deadline. Exit code 1.
 */
export class TimeoutError extends CliError {
  constructor(message: string) {
    super(message, { kind: 'timeout', exitCode: 1, code: 'TIMEOUT' });
  }
}

/**
 * Error thrown for non-2xx gateway responses, carrying the HTTP status and
 * the decoded problem payload when available. Exit code 1.
 */
export class ApiError extends CliError {
  public readonly statusCode?: number;

  /** Raw problem payload; only set when provided (keeps `'problem' in error` false otherwise). */
  public declare readonly problem?: unknown;

  constructor(message: string, statusCode?: number, problem?: unknown) {
    super(message, {
      kind: 'api',
      exitCode: 1,
      code: statusCode !== undefined ? `API_${statusCode}` : 'API',
    });
    this.statusCode = statusCode;
    if (problem !== undefined) {
      this.problem = problem;
    }
  }
}

/**
 * Normalized failure shape every error maps to before reporting.
 */
export interface FailureShape {
  kind: ErrorKind;
  code: string;
  message: string;
  exitCode: number;
}

/**
 * Collapse a ZodError into one readable line instead of dumping the raw
 * issues array (report F007).
 * @param err - zod validation error
 * @returns single-line "path: message; ..." summary
 */
function collapseZodError(err: ZodError): string {
  return err.issues
    .map((issue) => `${issue.path.length > 0 ? issue.path.join('.') : '(root)'}: ${issue.message}`)
    .join('; ');
}

/**
 * Map any thrown value onto the failure taxonomy: CliError subclasses pass
 * through, ZodError collapses to a one-line validation failure, commander
 * parse errors bridge as usage failures, and anything else maps to the
 * generic exit-1 business failure.
 * @param err - the thrown value to classify
 * @returns the normalized failure shape for reporting
 */
export function classifyError(err: unknown): FailureShape {
  if (err instanceof CliError) {
    return { kind: err.kind, code: err.code, message: err.message, exitCode: err.exitCode };
  }
  if (err instanceof ZodError) {
    return { kind: 'validation', code: 'VALIDATION', message: collapseZodError(err), exitCode: 1 };
  }
  if (err instanceof CommanderError) {
    // commander prefixes its messages with "error: "; the chokepoint adds its
    // own "Error: " prefix, so strip the embedded one to keep a single line.
    const message = err.message.replace(/^error:\s*/, '');
    return { kind: 'usage', code: err.code, message, exitCode: err.exitCode || 1 };
  }
  if (err instanceof Error) {
    // Unclassified failures keep the business exit code and message; the kind
    // vocabulary has no "internal", so they report as the generic api failure.
    return { kind: 'api', code: 'INTERNAL', message: err.message, exitCode: 1 };
  }
  return { kind: 'api', code: 'INTERNAL', message: String(err), exitCode: 1 };
}

let failureEnvelopeSuppressed = false;

/**
 * Suppress the STDOUT failure envelope for the rest of this invocation.
 * Commands that stream partial output to stdout (e.g. chat --stream) call
 * this once the stream is no longer clean, so a later failure cannot append
 * a JSON document onto already-written content.
 */
export function suppressFailureEnvelope(): void {
  failureEnvelopeSuppressed = true;
}

/**
 * Resolve the effective output format for the failure envelope. Mirrors
 * detectFormat's chain minus the command-level option (unknown at failure
 * time): global --format, persisted settings, then the TTY default.
 * @returns the format the invocation was rendering with
 */
function resolveFailureFormat(): 'json' | 'table' | 'yaml' {
  return getGlobalFormatOverride() ?? getSettingsFormatOverride() ?? (process.stdout.isTTY ? 'table' : 'json');
}

/**
 * Write the machine envelope to STDOUT when the effective format is a
 * structured one (json/yaml). Never throws: a broken or closed stdout must
 * not mask the stderr contract.
 * @param failure - normalized failure to envelope
 */
function emitFailureEnvelope(failure: FailureShape): void {
  if (failureEnvelopeSuppressed) return;
  const format = resolveFailureFormat();
  if (format !== 'json' && format !== 'yaml') return;
  if (process.stdout.destroyed || process.stdout.writable === false) return;
  const envelope = { ok: false, error: { kind: failure.kind, code: failure.code, message: failure.message } };
  try {
    process.stdout.write(`${JSON.stringify(envelope)}\n`);
  } catch {
    // stdout is no longer writable (stream corruption): keep the stderr line.
  }
}

/**
 * Report a fatal error through the single failure chokepoint: one human line
 * on STDERR, the machine envelope on STDOUT for structured formats, and the
 * contract exit code (auth 3, network 2, usage/validation/api/timeout 1).
 * Installed once as the `parseAsync` catch handler in the CLI entry point.
 * @param err - the error that escaped a command action
 * @returns never — re-throws after reporting so the entry point unwinds
 */
export function handleFatalError(err: Error): never {
  const failure = classifyError(err);
  process.stderr.write(`Error: ${failure.message}\n`);
  emitFailureEnvelope(failure);
  process.exitCode = failure.exitCode;
  throw err;
}
