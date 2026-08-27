import { configManager } from './config-manager.js';
import { tokenManager } from './token-manager.js';
import { resolvePassword } from './credential-store.js';
import { md5 } from '../utils/crypto.js';
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

    const url = `${gateway}${path}`;
    const res = await fetch(url, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    // 401 fallback — renew and retry once
    if (res.status === 401 && retryOn401) {
      const renewed = await this.renewToken(
        profileName,
        profile.tenant,
        profile.username,
      );
      if (renewed) {
        const newState = await tokenManager.getState(profileName);
        const newHeaders = newState
          ? tokenManager.buildHeaders(newState)
          : headers;
        const retryRes = await fetch(url, {
          method,
          headers: newHeaders,
          body: body ? JSON.stringify(body) : undefined,
        });
        if (!retryRes.ok) {
          throw await this.buildError(retryRes);
        }
        return (await retryRes.json()) as T;
      }
    }

    if (!res.ok) {
      throw await this.buildError(res);
    }

    // 204 No Content (for delete operations)
    if (res.status === 204) {
      return { ok: true } as unknown as T;
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

  async del<T = unknown>(path: string): Promise<T> {
    return this.request<T>('DELETE', path);
  }

  /**
   * Renew the token: salt → generate → persist.
   * Returns true on success, false if password is unavailable.
   */
  async renewToken(
    profileName: string,
    tenant: string,
    username: string,
  ): Promise<boolean> {
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
      const saltData = (await saltRes.json()) as { ok: boolean; data: string };
      if (!saltData.ok) throw new Error(`Salt failed: ${(saltData as unknown as Record<string, string>).message}`);

      // Step 2: Generate token
      const tokenRes = await fetch(`${gateway}/api/v3/auth/token/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: username,
          tenant,
          salt: saltData.data,
          password: md5(password),
        }),
      });
      const tokenData = (await tokenRes.json()) as {
        ok: boolean;
        data: string;
        message?: string;
      };
      if (!tokenData.ok) throw new Error(`Token generation failed: ${tokenData.message}`);

      // Step 3: Parse and persist
      const jwtPayload = decodeJwt(tokenData.data);
      await tokenManager.saveState(
        {
          token: tokenData.data,
          salt: saltData.data,
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
      typeof payload.scope === 'string'
        ? payload.scope.split(/\s+/).filter(Boolean)
        : undefined;
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
    const saltData = (await saltRes.json()) as { ok: boolean; data: string; message?: string };
    if (!saltData.ok) {
      throw new Error(`Salt failed: ${saltData.message ?? 'unknown error'}`);
    }

    // Step 2: generate
    const tokenRes = await fetch(`${gateway}/api/v3/auth/token/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: username,
        tenant,
        salt: saltData.data,
        password: md5(password),
      }),
    });
    const tokenData = (await tokenRes.json()) as {
      ok: boolean;
      data: string;
      message?: string;
    };
    if (!tokenData.ok) {
      throw new Error(
        `Login failed: ${tokenData.message ?? 'invalid credentials'}`,
      );
    }

    // Current backends deliver the ticket via the dc3-token cookie and an
    // acknowledgement body (data:"ok"); June-era builds returned the JWT in data.
    // Support both shapes so the CLI works against either.
    const token = extractTicket(tokenRes, tokenData.data);
    const jwtPayload = decodeJwt(token);
    await tokenManager.saveState(
      {
        token,
        salt: saltData.data,
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
        await this.request('POST', '/api/v3/auth/token/cancel', {
          name: state.username,
          tenant: state.tenant,
        }, false); // Don't retry on 401 for logout
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
        return json.message ?? body;
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


/**
 * Pull the login ticket out of a generate response: prefer a dc3-token cookie
 * (current contract), fall back to a JWT-shaped body value (legacy contract).
 */
export function extractTicket(res: Response, bodyData: unknown): string {
  const asString = typeof bodyData === 'string' ? bodyData : '';
  if (/^eyJ[\w-]*\.[\w-]*\.[\w-]*$/.test(asString)) {
    return asString;
  }
  const setCookie = res.headers.get('set-cookie') ?? '';
  const match = setCookie.match(/dc3-token=([^;\s]+)/u);
  if (match?.[1]) {
    return decodeURIComponent(match[1]);
  }
  throw new Error('Login succeeded but no dc3-token was found in the response');
}

/** Singleton instance */
export const dc3Client = new Dc3Client();
