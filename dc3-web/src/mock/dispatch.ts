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

import type {Handler, RouteRule} from './types';

const exact = new Map<string, Handler>();
const rules: RouteRule[] = [];
let fallback: Handler | null = null;

const entryKey = (method: string, url: string) => `${method} ${url}`;

/**
 * Register an exact URL match. Pass an array to bind multiple methods (e.g.
 * get+post) to one handler.
 */
export function on(method: string | string[], url: string, handler: Handler): void {
  const methods = Array.isArray(method) ? method : [method];
  for (const m of methods) {
    exact.set(entryKey(m.toLowerCase(), url), handler);
  }
}

/** Register a regex-based rule for URL families that share a handler. */
export function onRe(method: string, pattern: RegExp, handler: Handler): void {
  rules.push({method: method.toLowerCase(), pattern, handler});
}

/** Register the last-resort handler used when no exact/regex rule matches. */
export function setFallback(handler: Handler): void {
  fallback = handler;
}

/** Resolve the handler for a request context, or the fallback. */
export function resolve(ctx: {method: string; url: string}): Handler {
  const byMethod = exact.get(entryKey(ctx.method, ctx.url));
  if (byMethod) return byMethod;

  for (const rule of rules) {
    if ((!rule.method || rule.method === ctx.method) && rule.pattern.test(ctx.url)) {
      return rule.handler;
    }
  }

  // Should always be set before any request fires; this is a programming
  // error if reached (setupMock forgot to call setFallback).
  return (
    fallback ??
    (() => {
      throw new Error('mock: fallback handler not registered');
    })
  );
}
