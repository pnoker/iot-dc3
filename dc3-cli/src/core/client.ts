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

    // Step 3: persist
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

    return tokenData.data;
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

/** Singleton instance */
export const dc3Client = new Dc3Client();
