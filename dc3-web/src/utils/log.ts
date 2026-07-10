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
 * Unified logger that centralizes environment gating.
 *
 * Verbose levels (debug / info) only emit in the `dev` build, so production
 * bundles stay quiet. `warn` and `error` always emit — warnings and errors
 * must remain visible in production. Every line is prefixed with `[dc3]` so it
 * can be filtered out of, or grepped from, the browser console.
 *
 * Prefer this over raw `console.*` calls so the dev-gating convention is
 * applied consistently across the app.
 */

const PREFIX = '[dc3]';
const isDev = 'dev' === import.meta.env.MODE;

type LogArgs = unknown[];

const format = (args: LogArgs): LogArgs => [PREFIX, ...args];

export const logger = {
  /**
   * Debug-level output — suppressed in production. Use for high-frequency or
   * detailed diagnostic info (e.g. lifecycle steps, payload dumps).
   */
  debug(...args: LogArgs): void {
    if (isDev) {
      console.debug(...format(args));
    }
  },

  /**
   * Info-level output — suppressed in production. Use for notable but non-error
   * events the dev needs to see while working.
   */
  info(...args: LogArgs): void {
    if (isDev) {
      console.info(...format(args));
    }
  },

  /**
   * Warning — always emitted (including production). Use for recoverable
   * degradations or suspicious states worth surfacing.
   */
  warn(...args: LogArgs): void {
    console.warn(...format(args));
  },

  /**
   * Error — always emitted (including production). Pass the Error object as the
   * last argument so the stack trace is preserved.
   */
  error(...args: LogArgs): void {
    console.error(...format(args));
  },
};
