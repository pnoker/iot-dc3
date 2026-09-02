import { configManager } from './config-manager.js';
import { tokenManager } from './token-manager.js';
import { resolvePassword } from './credential-store.js';
import { decodeJwt } from '../utils/jwt.js';

/**
 * DC3 API client — HTTP wrapper that:
 * 1. Injects X-Auth-* headers automatically
 * 2. Proactively renews token before expiry
 * 3. Retries once on 401 with token renewal
 */
export class Dc3Client {
  private gateway: string | null = null;

  async getGateway(): Promise<string> {
    if (!this.gateway) {
      const profile = await configManager.getActiveProfile();
      this.gateway = profile.gateway.replace(/\/+$/, ''); // strip trailing slash
    }
    return this.gateway;
  }

  /**
   * Main request method. Call this for every API operation.
   */
  async request<T = unknown>(
    method: string,
    path: string,
    body?: unknown,
    retryOn401 = true,
    extraHeaders: Record<string, string> = {},
  ): Promise<T> {
    const gateway = await this.getGateway();
    const profile = await configManager.getActiveProfile();
    const profileName = (await configManager.load()).current_profile;
    const settings = await configManager.getSettings();
    const thresholdSec = settings.renewal_threshold_hours * 3600;

    // Proactive renewal
    if (await tokenManager.needsRenewal(profileName, thresholdSec)) {
      await this.renewToken(profileName, profile.tenant, profile.username);
    }

    const state = await tokenManager.getState(profileName);
    const headers = state
      ? tokenManager.buildHeaders(state)
      : { 'Content-Type': 'application/json' };
    Object.assign(headers, extraHeaders);
    const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;
    if (isFormData) {
      // Let fetch generate the multipart boundary and Content-Type header.
      delete headers['Content-Type'];
    }
    const requestBody = isFormData
      ? (body as FormData)
      : body === undefined
        ? undefined
        : JSON.stringify(body);

    const url = `${gateway}${path}`;
    const res = await fetch(url, {
      method,
      headers,
      body: requestBody,
    });

    // 401 fallback — renew and retry once
    if (res.status === 401 && retryOn401) {
      const renewed = await this.renewToken(profileName, profile.tenant, profile.username);
      if (renewed) {
        const newState = await tokenManager.getState(profileName);
        const newHeaders = newState ? tokenManager.buildHeaders(newState) : headers;
        Object.assign(newHeaders, extraHeaders);
        if (isFormData) {
          delete newHeaders['Content-Type'];
        }
        const retryRes = await fetch(url, {
          method,
          headers: newHeaders,
          body: requestBody,
        });
        if (!retryRes.ok) {
          throw await this.buildError(retryRes);
        }
        if (retryRes.status === 204) {
          return undefined as T;
        }
        return (await retryRes.json()) as T;
      }
    }

    if (!res.ok) {
      throw await this.buildError(res);
    }

    // 204 No Content (for delete operations)
    if (res.status === 204) {
      return undefined as T;
    }

    return (await res.json()) as T;
  }

  // Convenience methods
  async get<T = unknown>(path: string): Promise<T> {
    return this.request<T>('GET', path);
  }

  async post<T = unknown>(path: string, body?: unknown): Promise<T> {
    return this.request<T>('POST', path, body);
  }

  async postForm<T = unknown>(
    path: string,
    body: FormData,
    headers: Record<string, string> = {},
  ): Promise<T> {
    return this.request<T>('POST', path, body, true, headers);
  }

  async del<T = unknown>(path: string): Promise<T> {
    return this.request<T>('DELETE', path);
  }

  /**
   * Renew the token: salt → generate → persist.
   * Returns true on success, false if password is unavailable.
   */
  async renewToken(profileName: string, tenant: string, username: string): Promise<boolean> {
    const current = await tokenManager.getState(profileName);
    if (current?.authType === 'oauth') {
      // OAuth tickets cannot be silently renewed without the client secret; expiry
      // is short by design — surface a clean auth error instead.
      return false;
    }
    const password = await resolvePassword(`${username}@${tenant}`);
    if (!password) {
      return false;
    }

    try {
      const gateway = await this.getGateway();

      // Step 1: Get salt
      const saltRes = await fetch(`${gateway}/api/v3/auth/token/salt`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: username, tenant }),
      });
      if (!saltRes.ok) throw await this.buildError(saltRes);
      const salt = parseScalarResource(await saltRes.text(), 'Salt');

      // Step 2: Generate token
      const tokenRes = await fetch(`${gateway}/api/v3/auth/token/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: username,
          tenant,
          salt,
          password,
        }),
      });
      if (!tokenRes.ok) throw await this.buildError(tokenRes);
      const token = parseTokenResource(parseScalarResource(await tokenRes.text(), 'Token'));

      // Step 3: Parse and persist
      const jwtPayload = decodeJwt(token);
      await tokenManager.saveState(
        {
          token,
          salt,
          tenant,
          username,
          issuedAt: jwtPayload.iat,
          expiresAt: jwtPayload.exp,
        },
        profileName,
      );

      return true;
    } catch {
      // Silent failure — the 401 retry will surface the error
      return false;
    }
  }

  /**
   * OAuth client_credentials login against the gateway token endpoint.
   *
   * The client must be pre-registered on the console (settings/mcp) with the
   * client_credentials grant bound to a service account; tenant and principal come
   * from that registration. Stores an 'oauth'-type state whose requests travel as
   * Authorization: Bearer and are verified at the gateway via JWKS when enabled.
   */
  async loginOAuth(
    clientId: string,
    clientSecret: string,
    scope: string | undefined,
    profileName: string,
  ): Promise<{ token: string; expiresAt: number; scope?: string[] }> {
    const gateway = await this.getGateway();
    const form = new URLSearchParams({ grant_type: 'client_credentials' });
    if (scope && scope.trim()) {
      form.set('scope', scope.trim());
    }
    const res = await fetch(`${gateway}/oauth2/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        Authorization: `Basic ${Buffer.from(`${clientId}:${clientSecret}`).toString('base64')}`,
      },
      body: form.toString(),
    });
    const payload = (await res.json()) as Record<string, unknown>;
    if (!res.ok || typeof payload.access_token !== 'string') {
      throw new AuthError(
        `OAuth token request failed (${res.status}): ${String(payload.error_description ?? payload.error ?? 'unknown')}`,
      );
    }
    const token = String(payload.access_token);
    const jwtPayload = decodeJwt(token);
    const scopes =
      typeof payload.scope === 'string' ? payload.scope.split(/\s+/).filter(Boolean) : undefined;
    await tokenManager.saveState(
      {
        token,
        salt: '',
        tenant: '',
        username: clientId,
        issuedAt: jwtPayload.iat,
        expiresAt: jwtPayload.exp,
        authType: 'oauth',
        scope: scopes,
      },
      profileName,
    );
    return { token, expiresAt: jwtPayload.exp, scope: scopes };
  }

  /**
   * Perform the full login flow (salt → generate) and persist tokens.
   */
  async login(
    tenant: string,
    username: string,
    password: string,
    profileName: string,
  ): Promise<string> {
    const gateway = await this.getGateway();

    // Step 1: salt
    const saltRes = await fetch(`${gateway}/api/v3/auth/token/salt`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: username, tenant }),
    });
    if (!saltRes.ok) throw await this.buildError(saltRes);
    const salt = parseScalarResource(await saltRes.text(), 'Salt');

    // Step 2: generate
    const tokenRes = await fetch(`${gateway}/api/v3/auth/token/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: username,
        tenant,
        salt,
        password,
      }),
    });
    if (!tokenRes.ok) throw await this.buildError(tokenRes);
    const token = parseTokenResource(parseScalarResource(await tokenRes.text(), 'Token'));
    const jwtPayload = decodeJwt(token);
    await tokenManager.saveState(
      {
        token,
        salt,
        tenant,
        username,
        issuedAt: jwtPayload.iat,
        expiresAt: jwtPayload.exp,
      },
      profileName,
    );

    return token;
  }

  /**
   * Logout: call cancel endpoint + clear local state.
   */
  async logout(profileName: string): Promise<void> {
    const state = await tokenManager.getState(profileName);
    if (state) {
      try {
        await this.request(
          'POST',
          '/api/v3/auth/token/cancel',
          {
            name: state.username,
            tenant: state.tenant,
          },
          false,
        ); // Don't retry on 401 for logout
      } catch {
        // Cancel may fail if token already expired — that's fine
      }
    }
    await tokenManager.clearState(profileName);
  }

  private async buildError(res: Response): Promise<Error> {
    let body: string;
    try {
      body = await res.text();
    } catch {
      body = 'Unable to read response body';
    }
    const msg = (() => {
      try {
        const json = JSON.parse(body);
        return json.detail ?? json.title ?? body;
      } catch {
        return body;
      }
    })();

    switch (res.status) {
      case 401:
        return new AuthError(`Authentication failed (401): ${msg}`);
      case 403:
        return new AuthError(`Forbidden (403): ${msg}`);
      case 404:
        return new ApiError(`Not found (404): ${msg}`, 404);
      case 500:
        return new ApiError(`Server error (500): ${msg}`, 500);
      default:
        return new ApiError(`HTTP ${res.status}: ${msg}`, res.status);
    }
  }
}

export class AuthError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'AuthError';
  }
}

export class ApiError extends Error {
  public readonly statusCode: number;

  constructor(message: string, statusCode: number) {
    super(message);
    this.name = 'ApiError';
    this.statusCode = statusCode;
  }
}

/** Validate the direct token resource returned by the auth endpoint. */
export function parseTokenResource(value: unknown): string {
  if (typeof value !== 'string' || !/^eyJ[\w-]*\.[\w-]*\.[\w-]*$/u.test(value)) {
    throw new Error('Token endpoint returned an invalid resource');
  }
  return value;
}

/**
 * Parse a scalar auth resource (salt, token). The auth endpoints answer as
 * text/plain with a bare value; a JSON-encoded string is tolerated as well.
 */
export function parseScalarResource(body: string, label: string): string {
  const trimmed = body.trim();
  if (trimmed.startsWith('"')) {
    try {
      const parsed: unknown = JSON.parse(trimmed);
      if (typeof parsed === 'string' && parsed.length > 0) {
        return parsed;
      }
    } catch {
      // Not JSON after all; fall back to the raw text below.
    }
  }
  if (trimmed.length === 0) {
    throw new Error(`${label} endpoint returned an invalid resource`);
  }
  return trimmed;
}

/** Singleton instance */
export const dc3Client = new Dc3Client();
