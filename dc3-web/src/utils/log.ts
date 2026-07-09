/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
