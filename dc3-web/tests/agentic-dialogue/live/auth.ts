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
 * Live auth flow for the evaluation runner: reproduce the web client's login
 * (salt → generate) against the gateway and carry the resulting httpOnly token
 * cookie plus tenant/login headers on every evaluated request.
 */

export interface AuthSession {
  /** Raw `Cookie` header value carrying the httpOnly auth token. */
  cookie: string;
  tenant: string;
  login: string;
}

interface Envelope<T> {
  ok?: boolean;
  code?: number | string;
  message?: string;
  data?: T;
}

const unwrap = async <T>(response: Response): Promise<T> => {
  const text = await response.text();
  try {
    const body = JSON.parse(text) as Envelope<T> | T;
    if (body && typeof body === 'object' && 'data' in (body as object)) {
      return ((body as Envelope<T>).data ?? '') as T;
    }
    return body as T;
  } catch {
    // The auth endpoints answer with raw text (e.g. the salt value), not JSON.
    return text as unknown as T;
  }
};

/**
 * Authenticate against the auth center through the gateway.
 * @param baseUrl - gateway base URL
 * @param tenant - tenant code (seed data uses `default`)
 * @param name - login name (seed data uses `dc3`)
 * @param password - plaintext password, hashed server-side per the salt flow
 * @returns the session cookie and header pair to attach to evaluated requests
 */
export const login = async (baseUrl: string, tenant: string, name: string, password: string): Promise<AuthSession> => {
  const base = baseUrl.replace(/\/$/, '');
  const headers = {'Content-Type': 'application/json', 'X-Auth-Tenant': tenant, 'X-Auth-Login': name};

  const saltResponse = await fetch(`${base}/api/v3/auth/token/salt`, {
    method: 'POST',
    headers,
    body: JSON.stringify({tenant, name}),
  });
  if (!saltResponse.ok) {
    throw new Error(`salt request failed: HTTP ${saltResponse.status} ${(await saltResponse.text()).slice(0, 200)}`);
  }
  const salt = String(await unwrap<string>(saltResponse));
  if (!salt) throw new Error('salt response carried no salt value');

  const tokenResponse = await fetch(`${base}/api/v3/auth/token/generate`, {
    method: 'POST',
    headers,
    body: JSON.stringify({tenant, name, salt, password}),
  });
  if (!tokenResponse.ok) {
    throw new Error(
      `token generation failed: HTTP ${tokenResponse.status} ${(await tokenResponse.text()).slice(0, 200)}`
    );
  }
  const setCookies =
    typeof tokenResponse.headers.getSetCookie === 'function' ? tokenResponse.headers.getSetCookie() : [];
  const cookie = setCookies.map((entry) => entry.split(';')[0]).join('; ');
  if (!cookie) throw new Error('token generation set no cookie');

  return {cookie, tenant, login: name};
};
