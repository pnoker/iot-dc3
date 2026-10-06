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
import { NetworkError } from './errors.js';

/**
 * Default timeout applied to every non-streaming gateway request, in milliseconds.
 * Override per call by passing an explicit timeout to {@link fetchOrNetworkError}.
 */
export const DEFAULT_REQUEST_TIMEOUT_MS = 30_000;

/**
 * Normalize a configured gateway base URL by stripping trailing slashes so
 * request paths can be appended without producing `//` in the URL.
 * @param gateway - gateway base URL as stored in the profile config
 * @returns the gateway base URL without trailing slashes
 */
export function normalizeGateway(gateway: string): string {
  return gateway.replace(/\/+$/, '');
}

/**
 * fetch that rejects with the typed {@link NetworkError} on transport failures
 * (DNS, socket, TLS, timeout) so callers map them to the documented exit code 2.
 * This is the only place in `src/` allowed to call `fetch` directly.
 * @param url - absolute request URL
 * @param init - fetch initializer (method, headers, body)
 * @param timeoutMs - request timeout applied via `AbortSignal.timeout`; pass
 *   `null` to disable. Streaming responses (SSE) must pass `null`: the signal
 *   aborts body consumption too, and Node's fetch offers no headers-only
 *   timeout on a single call, so a long-lived stream cannot carry a fixed timeout.
 * @returns the gateway response
 */
export async function fetchOrNetworkError(
  url: string,
  init: Parameters<typeof fetch>[1] = {},
  timeoutMs: number | null = DEFAULT_REQUEST_TIMEOUT_MS,
): Promise<Response> {
  try {
    if (timeoutMs === null) {
      return await fetch(url, init);
    }
    return await fetch(url, { ...init, signal: AbortSignal.timeout(timeoutMs) });
  } catch (error) {
    throw new NetworkError(`Network request failed (${url}): ${(error as Error).message}`);
  }
}

/**
 * Read a response body as text, degrading to an empty string when the body
 * cannot be consumed (aborted mid-body) so callers never crash on a raw
 * transport error while formatting a diagnostic.
 * @param res - response whose body to read
 * @returns the decoded body text, or '' when unreadable
 */
export async function readBodyText(res: Response): Promise<string> {
  try {
    return await res.text();
  } catch {
    return '';
  }
}

/** Whether the cleartext-transport warning already fired in this process. */
let cleartextTransportWarned = false;

/**
 * Warn once per process when credentials are about to be sent over cleartext
 * http to a non-loopback gateway (report F055). Loopback gateways are exempt:
 * the default local development gateway is http by design.
 * @param gateway - resolved gateway base URL about to receive credentials
 */
export function warnInsecureTransport(gateway: string): void {
  if (cleartextTransportWarned) {
    return;
  }
  let hostname: string;
  try {
    const url = new URL(gateway);
    if (url.protocol !== 'http:') {
      return;
    }
    hostname = url.hostname.replace(/^\[/u, '').replace(/\]$/u, '');
  } catch {
    // Not a parseable URL; the request itself will fail with a clear error.
    return;
  }
  const loopback =
    hostname === 'localhost' ||
    hostname === '::1' ||
    hostname === '127.0.0.1' ||
    /^127\.\d{1,3}\.\d{1,3}\.\d{1,3}$/u.test(hostname);
  if (loopback) {
    return;
  }
  cleartextTransportWarned = true;
  process.stderr.write(
    `Warning: credentials will be sent in cleartext to the http gateway ${gateway}. ` +
      `Configure an https gateway for remote deployments.\n`,
  );
}
