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
 * Error taxonomy shared by every transport (REST client, MCP client, chat stream)
 * plus the top-level handler that maps each class to its documented exit code:
 * 0 ok / 1 business error / 2 network error / 3 auth error.
 */

/**
 * Error thrown when the gateway rejects authentication (HTTP 401).
 */
export class AuthError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'AuthError';
  }
}

/**
 * Error thrown when a request never reaches the gateway (DNS, socket, TLS,
 * timeout, or connection-refused failures surfaced by fetch). Maps to exit code 2.
 */
export class NetworkError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'NetworkError';
  }
}

/**
 * Error thrown for non-2xx gateway responses, carrying status and problem details.
 */
export class ApiError extends Error {
  public readonly statusCode: number;

  constructor(message: string, statusCode: number) {
    super(message);
    this.name = 'ApiError';
    this.statusCode = statusCode;
  }
}

/**
 * Report a fatal error on stderr and exit with the contract exit code
 * (AuthError 3, NetworkError 2, anything else 1). Installed once as the
 * `parseAsync` catch handler in the CLI entry point.
 * @param err - the error that escaped a command action
 * @returns never — the process exits
 */
export function handleFatalError(err: Error): never {
  process.stderr.write(`Error: ${err.message}\n`);
  if (err instanceof AuthError) {
    process.exit(3);
  }
  if (err instanceof NetworkError) {
    process.exit(2);
  }
  process.exit(1);
}
