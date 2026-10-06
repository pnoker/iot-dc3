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
import { configManager } from './config-manager.js';
import { tokenManager, credentialIdentifier } from './token-manager.js';
import type { TokenState } from './token-manager.js';
import { resolvePassword, deletePasswordFromStore } from './credential-store.js';
import { fetchOrNetworkError, normalizeGateway, readBodyText, warnInsecureTransport } from './http.js';
import { ApiError, AuthError } from './errors.js';
import { decodeJwt } from '../utils/jwt.js';

export { AuthError, NetworkError, ApiError } from './errors.js';

/** Maximum number of body characters embedded in a non-JSON diagnostic (report F042). */
const SNIPPET_LENGTH = 120;

/** Identity mismatches already warned about in this process (one warning each, report F016/F037). */
const warnedIdentityMismatches = new Set<string>();

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function snippetOf(text: string): string {
  return text.trim().slice(0, SNIPPET_LENGTH) + (text.trim().length > SNIPPET_LENGTH ? '…' : '');
}

function parseJsonOrUndefined(text: string): unknown {
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return undefined;
  }
}

/**
 * Extract the human-readable detail from an error body: the RFC 7807
 * `detail`/`title` fields when the body is JSON, otherwise the raw text. Never
 * returns an empty string so error messages never end in a dangling colon
 * (report F042). Exported for the other authenticated transports (chat stream)
 * that read their own responses but must report errors the same way.
 * @param text - raw error response body
 * @returns the detail line for the error message
 */
export function extractErrorDetail(text: string): string {
  const trimmed = text.trim();
  if (trimmed === '') {
    return '(empty body)';
  }
  const json = parseJsonOrUndefined(trimmed);
  if (isRecord(json)) {
    const detail = json.detail ?? json.title;
    if (typeof detail === 'string' && detail.trim() !== '') {
      return detail;
    }
    if (detail !== undefined && detail !== null) {
      return String(detail);
    }
  }
  return trimmed;
}

/**
 * Decode a 2xx response body that was read as text once (report F042): empty
 * bodies (including 204) resolve to undefined; a non-empty body that is not
 * valid JSON fails with an ApiError carrying the status, Content-Type, and a
 * short snippet instead of leaking a raw SyntaxError.
 * @param status - HTTP status of the response
 * @param contentType - Content-Type header value, when present
 * @param text - body text already read exactly once
 * @returns the decoded JSON value, or undefined for an empty body
 */
function decodeResponseBody<T>(status: number, contentType: string | null, text: string): T {
  if (status === 204 || text.trim() === '') {
    return undefined as T;
  }
  try {
    return JSON.parse(text) as T;
  } catch {
    // The status says success but the body is not the JSON contract: keep the
    // status on the error for diagnostics, but the machine code must never
    // read as a plain `API_200` success-status failure (error-kind precision).
    throw new ApiError(
      `Gateway returned a non-JSON ${status} response (Content-Type: ${contentType ?? 'unknown'}): ${snippetOf(text)}`,
      status,
      text,
      'API_BAD_BODY',
    );
  }
}

/**
 * Warn once per stored session when the active profile identity diverges from
 * the identity the stored token was issued for (report F016/F037). Requests
 * keep using the token-state identity; the warning names both so silent
 * tenant inheritance and orphaned renewal passwords become visible.
 * @param state - persisted token state for the active profile
 * @param profile - active profile configuration
 * @param profile.tenant - tenant name the profile is configured for
 * @param profile.username - username the profile is configured for
 * @param profileName - active profile name
 */
function warnIdentityMismatch(
  state: TokenState | null,
  profile: { tenant: string; username: string },
  profileName: string,
): void {
  if (!state || state.authType === 'oauth') {
    return;
  }
  if (state.tenant === profile.tenant && state.username === profile.username) {
    return;
  }
  const key = `${profileName}:${state.username}@${state.tenant}`;
  if (warnedIdentityMismatches.has(key)) {
    return;
  }
  warnedIdentityMismatches.add(key);
  process.stderr.write(
    `Warning: profile "${profileName}" is configured for ${profile.username}@${profile.tenant}, ` +
      `but the stored session belongs to ${state.username}@${state.tenant}. Requests use the stored ` +
      `session identity, and renewal reads the password stored for ${state.username}@${state.tenant}.\n`,
  );
}

/**
 * Cancel a token server-side without touching persisted state. Best effort:
 * used to revoke freshly minted tokens that must be discarded, so a failure
 * here must never mask the discard itself.
 * @param gateway - resolved gateway base URL
 * @param token - raw token value to revoke
 * @param salt - salt the token was issued with
 * @param tenant - tenant the token belongs to
 * @param username - login the token belongs to
 */
async function cancelTokenBestEffort(
  gateway: string,
  token: string,
  salt: string,
  tenant: string,
  username: string,
): Promise<void> {
  try {
    await fetchOrNetworkError(`${gateway}/api/v3/auth/token/cancel`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Tenant': tenant,
        'X-Auth-Login': username,
        'X-Auth-Token': JSON.stringify({ salt, token }),
      },
      body: JSON.stringify({ name: username, tenant }),
    });
  } catch {
    // Best effort — the local discard already happened.
  }
}

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
      this.gateway = normalizeGateway(profile.gateway);
    }
    return this.gateway;
  }

  /**
   * Shared authenticated fetch machinery behind {@link Dc3Client.request} and
   * {@link Dc3Client.requestForBytes}: proactive renewal, header injection,
   * the single 401 renewal-retry, and the typed error mapping (401/403 →
   * AuthError exit 3, everything else → ApiError). Returns the raw Response
   * with the body still unconsumed on 2xx — only the error paths read the
   * body, so binary payloads survive intact.
   * @param method - HTTP method for the request
   * @param path - gateway-relative request path
   * @param body - request body payload
   * @param retryOn401 - whether to retry once after a silent token renewal
   * @param extraHeaders - headers merged into the request
   * @returns the gateway response (2xx, body unconsumed)
   */
  private async fetchAuthenticated(
    method: string,
    path: string,
    body: unknown,
    retryOn401: boolean,
    extraHeaders: Record<string, string>,
  ): Promise<Response> {
    const gateway = await this.getGateway();
    const profile = await configManager.getActiveProfile();
    const profileName = await configManager.getActiveProfileName();
    const settings = await configManager.getSettings();
    const thresholdSec = settings.renewal_threshold_hours * 3600;

    const state = await tokenManager.getState(profileName);
    warnIdentityMismatch(state, profile, profileName);

    // Epoch snapshot the request anchors every renewal save to: the epoch
    // stamped into the observed state (report F024). While an entry exists it
    // equals the profile's current epoch — only clearState (logout) bumps it,
    // and it deletes the entry in the same atomic write. A renewal that read
    // the epoch lazily (after its own mint started) would capture the
    // already-bumped counter and resurrect the logged-out session.
    const epoch = state?.epoch ?? 0;

    // Proactive renewal. renewToken derives the password lookup key from the
    // persisted token-state identity first, falling back to the profile values
    // (report F037).
    if (await tokenManager.needsRenewal(profileName, thresholdSec)) {
      await this.renewToken(profileName, profile.tenant, profile.username, epoch);
    }

    const currentState = await tokenManager.getState(profileName);
    const headers = currentState
      ? tokenManager.buildHeaders(currentState)
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
    const res = await fetchOrNetworkError(url, {
      method,
      headers,
      body: requestBody,
    });

    // 401 fallback — renew and retry once. Only a request that WENT OUT with
    // a stored session (currentState) may renew: after a logout cleared the
    // state there is no session to heal, and minting one from a lingering
    // stored password would resurrect the logged-out session (report F024).
    // The renewal persists against the epoch captured at request entry, so a
    // logout that landed mid-request refuses the save and surfaces this 401.
    if (res.status === 401 && retryOn401 && currentState) {
      const renewed = await this.renewToken(profileName, profile.tenant, profile.username, epoch);
      if (renewed) {
        const newState = await tokenManager.getState(profileName);
        const newHeaders = newState ? tokenManager.buildHeaders(newState) : headers;
        Object.assign(newHeaders, extraHeaders);
        if (isFormData) {
          delete newHeaders['Content-Type'];
        }
        const retryRes = await fetchOrNetworkError(url, {
          method,
          headers: newHeaders,
          body: requestBody,
        });
        if (!retryRes.ok) {
          throw this.buildError(retryRes.status, await readBodyText(retryRes));
        }
        return retryRes;
      }
    }

    if (!res.ok) {
      throw this.buildError(res.status, await readBodyText(res));
    }

    return res;
  }

  /**
   * Main request method. Call this for every API operation.
   * @param method - HTTP method for the request
   * @param path - gateway-relative request path
   * @param body - request body payload
   * @param retryOn401 - whether to retry once after a silent token renewal
   * @param extraHeaders - headers merged into the request
   * @returns the decoded response body
   */
  async request<T = unknown>(
    method: string,
    path: string,
    body?: unknown,
    retryOn401 = true,
    extraHeaders: Record<string, string> = {},
  ): Promise<T> {
    const res = await this.fetchAuthenticated(method, path, body, retryOn401, extraHeaders);
    return decodeResponseBody<T>(res.status, res.headers.get('content-type'), await readBodyText(res));
  }

  /**
   * Binary-capable authenticated request (audit G24): shares request()'s
   * proactive renewal, 401-retry renewal, epoch-capture-before-mint contract,
   * and error mapping (401/403 → AuthError exit 3, never an `API_401` business
   * failure), but hands the caller the raw Response so a non-JSON payload
   * (e.g. an XLSX template download) is never round-tripped through the JSON
   * decoder. The returned response is always 2xx; failures already threw.
   * @param method - HTTP method for the request
   * @param path - gateway-relative request path
   * @param body - request body payload
   * @returns the raw gateway response with the body unconsumed
   */
  async requestForBytes(method: string, path: string, body?: unknown): Promise<Response> {
    return this.fetchAuthenticated(method, path, body, true, {});
  }

  // Convenience methods
  async get<T = unknown>(path: string): Promise<T> {
    return this.request<T>('GET', path);
  }

  async post<T = unknown>(path: string, body?: unknown): Promise<T> {
    return this.request<T>('POST', path, body);
  }

  /**
   * THE path for multipart request bodies (spec item 6): FormData is forwarded
   * untouched so fetch generates the boundary, and any extra headers travel
   * with the request. Callers must not pre-set Content-Type on FormData.
   * @param path - gateway-relative request path
   * @param body - multipart form payload
   * @param headers - extra headers merged into the request
   * @returns the decoded response body
   */
  async postForm<T = unknown>(
    path: string,
    body: FormData,
    headers: Record<string, string> = {},
  ): Promise<T> {
    return this.request<T>('POST', path, body, true, headers);
  }

  /**
   * Delete a resource. Non-empty 2xx bodies are returned for the caller to
   * decide whether to print them; 204 and empty bodies resolve to undefined
   * (report F038).
   * @param path - gateway-relative request path
   * @returns the decoded response body, or undefined when empty
   */
  async del<T = unknown>(path: string): Promise<T> {
    return this.request<T>('DELETE', path);
  }

  /**
   * Renew the token: salt → generate → persist. The stored password is keyed
   * by the persisted token-state identity, falling back to the profile values
   * when no state exists (report F037). Returns true on success, false when
   * the password is unavailable or the session was logged out concurrently.
   * @param profileName - profile name used for the lookup
   * @param fallbackTenant - tenant to use when no token state exists
   * @param fallbackUsername - username to use when no token state exists
   * @param expectedEpoch - epoch the caller captured before its request work
   *   started (report F024); the guarded save persists only while it is
   *   unchanged, so a logout that raced the request discards the fresh token
   * @returns true when the token was renewed
   */
  async renewToken(
    profileName: string,
    fallbackTenant: string,
    fallbackUsername: string,
    expectedEpoch?: number,
  ): Promise<boolean> {
    const current = await tokenManager.getState(profileName);
    if (current?.authType === 'oauth') {
      // OAuth tickets cannot be silently renewed without the client secret; expiry
      // is short by design — surface a clean auth error instead.
      return false;
    }
    const tenant = current?.tenant || fallbackTenant;
    const username = current?.username || fallbackUsername;
    const password = await resolvePassword(credentialIdentifier({ username, tenant }));
    if (!password) {
      return false;
    }

    try {
      const gateway = await this.getGateway();
      warnInsecureTransport(gateway);

      // Epoch guarding the persisted save. Callers that already did network
      // work (both renewal paths inside request()) pass the epoch they
      // captured BEFORE that work: reading it here would already observe a
      // logout that landed meanwhile, and the save would resurrect the
      // session it just cleared (report F024). Standalone callers keep the
      // capture-before-minting contract.
      const epoch = expectedEpoch ?? (await tokenManager.getEpoch(profileName));

      // Step 1: Get salt
      const saltRes = await fetchOrNetworkError(`${gateway}/api/v3/auth/token/salt`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: username, tenant }),
      });
      const saltText = await readBodyText(saltRes);
      if (!saltRes.ok) throw this.buildError(saltRes.status, saltText);
      const salt = parseAuthField(saltText, 'salt', 'Salt');

      // Step 2: Generate token
      const tokenRes = await fetchOrNetworkError(`${gateway}/api/v3/auth/token/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: username,
          tenant,
          salt,
          password,
        }),
      });
      const tokenText = await readBodyText(tokenRes);
      if (!tokenRes.ok) throw this.buildError(tokenRes.status, tokenText);
      const token = parseTokenResource(parseAuthField(tokenText, 'token', 'Token'));

      // Step 3: Parse and persist — guarded by the captured epoch. When a
      // logout won the race, discard the fresh token and cancel it
      // server-side instead of resurrecting the session.
      const jwtPayload = decodeJwt(token);
      const persisted = await tokenManager.saveStateIfEpochUnchanged(
        {
          token,
          salt,
          tenant,
          username,
          issuedAt: jwtPayload.iat,
          expiresAt: jwtPayload.exp,
        },
        profileName,
        epoch,
      );
      if (!persisted) {
        await cancelTokenBestEffort(gateway, token, salt, tenant, username);
        return false;
      }

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
   * @param clientId - client id to scope the request
   * @param clientSecret - client secret issued with the registration
   * @param scope - space-separated scope list to request
   * @param profileName - profile name used for the lookup
   * @returns the issued token with expiry and granted scope
   */
  async loginOAuth(
    clientId: string,
    clientSecret: string,
    scope: string | undefined,
    profileName: string,
  ): Promise<{ token: string; expiresAt: number; scope?: string[] }> {
    const gateway = await this.getGateway();
    warnInsecureTransport(gateway);
    const form = new URLSearchParams({ grant_type: 'client_credentials' });
    if (scope && scope.trim()) {
      form.set('scope', scope.trim());
    }
    const res = await fetchOrNetworkError(`${gateway}/oauth2/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        Authorization: `Basic ${Buffer.from(`${clientId}:${clientSecret}`).toString('base64')}`,
      },
      body: form.toString(),
    });
    if (!res.ok) {
      // The token endpoint may answer non-JSON error bodies — read as text first.
      const body = await readBodyText(res);
      const json = parseJsonOrUndefined(body);
      const detail = isRecord(json)
        ? (json.error_description ?? json.error ?? extractErrorDetail(body))
        : extractErrorDetail(body);
      throw new AuthError(`OAuth token request failed (${res.status}): ${detail}`);
    }
    const body = await readBodyText(res);
    const payload = parseJsonOrUndefined(body);
    if (!isRecord(payload)) {
      // A 2xx with a non-JSON body (HTML error page, empty proxy reply) used to
      // escape as a raw SyntaxError classified api/INTERNAL. This is the auth
      // plane, so fail typed — mirroring decodeResponseBody's API_BAD_BODY
      // treatment but on the AuthError taxonomy (exit 3).
      throw new AuthError(
        `OAuth token endpoint returned a non-JSON 2xx response (Content-Type: ${res.headers.get('content-type') ?? 'unknown'}): ${snippetOf(body)}`,
      );
    }
    if (typeof payload.access_token !== 'string') {
      throw new AuthError('OAuth token endpoint returned no access_token');
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
   * @param tenant - tenant name for the login
   * @param username - login username
   * @param password - login password
   * @param profileName - profile name used for the lookup
   * @returns the issued token with expiry
   */
  async login(
    tenant: string,
    username: string,
    password: string,
    profileName: string,
  ): Promise<string> {
    const gateway = await this.getGateway();
    warnInsecureTransport(gateway);

    // Step 1: salt
    const saltRes = await fetchOrNetworkError(`${gateway}/api/v3/auth/token/salt`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: username, tenant }),
    });
    const saltText = await readBodyText(saltRes);
    if (!saltRes.ok) throw this.buildError(saltRes.status, saltText);
    const salt = parseAuthField(saltText, 'salt', 'Salt');

    // Step 2: generate
    const tokenRes = await fetchOrNetworkError(`${gateway}/api/v3/auth/token/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: username,
        tenant,
        salt,
        password,
      }),
    });
    const tokenText = await readBodyText(tokenRes);
    if (!tokenRes.ok) throw this.buildError(tokenRes.status, tokenText);
    const token = parseTokenResource(parseAuthField(tokenText, 'token', 'Token'));
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
   * Logout with guaranteed local cleanup (spec item 5): clear the persisted
   * session FIRST (so neither a concurrent renewal nor an unreachable gateway
   * can leave a usable ticket on disk), then cancel the token remotely, then
   * delete the stored password. When the remote cancel fails, an explicit
   * warning names what survived (the gateway-side token, until it expires)
   * before the original error propagates (NetworkError → exit 2,
   * AuthError → exit 3).
   * @param profileName - profile name used for the lookup
   */
  async logout(profileName: string): Promise<void> {
    const state = await tokenManager.getState(profileName);
    if (!state) {
      return;
    }
    // Local-first: clearState performs the epoch-bump + entry removal as one
    // atomic write (once the token-manager lands the epoch).
    await tokenManager.clearState(profileName);

    let remoteError: unknown = null;
    try {
      const gateway = await this.getGateway();
      const res = await fetchOrNetworkError(`${gateway}/api/v3/auth/token/cancel`, {
        method: 'POST',
        headers: tokenManager.buildHeaders(state),
        body: JSON.stringify({ name: state.username, tenant: state.tenant }),
      });
      if (!res.ok) {
        const text = await readBodyText(res);
        throw this.buildError(res.status, text);
      }
    } catch (error) {
      remoteError = error;
    }

    // Password cleanup is guaranteed regardless of the remote outcome.
    await deletePasswordFromStore(credentialIdentifier(state));

    if (remoteError !== null) {
      process.stderr.write(
        `Warning: the token for ${state.username}@${state.tenant} could not be revoked remotely — ` +
          `${(remoteError as Error).message}. ` +
          `Local session state and the stored password were removed; the gateway-side token ` +
          `stays valid until it expires.\n`,
      );
      throw remoteError;
    }
  }

  /**
   * Build a typed error for a non-2xx response from the already-read body
   * text: maps 401/403 to AuthError and everything else to ApiError carrying
   * the status code and the decoded problem payload; empty and non-JSON bodies
   * degrade to an explicit placeholder instead of a dangling colon or a raw
   * SyntaxError (report F030/F042).
   * @param status - HTTP status of the response
   * @param text - body text already read exactly once
   * @returns the error to throw
   */
  private buildError(status: number, text: string): Error {
    const detail = extractErrorDetail(text);
    const problem = parseJsonOrUndefined(text);
    switch (status) {
      case 401:
        return new AuthError(`Authentication failed (401): ${detail}`);
      case 403:
        return new AuthError(`Forbidden (403): ${detail}`);
      case 404:
        return new ApiError(`Not found (404): ${detail}`, 404, problem);
      case 500:
        return new ApiError(`Server error (500): ${detail}`, 500, problem);
      default:
        return new ApiError(`HTTP ${status}: ${detail}`, status, problem);
    }
  }
}

/**
 * Validate the direct token resource returned by the auth endpoint.
 * @param value - token string returned by the auth endpoint, validated as a 3-part JWT
 * @returns the validated token, unchanged
 */
export function parseTokenResource(value: unknown): string {
  if (typeof value !== 'string' || !/^eyJ[\w-]*\.[\w-]*\.[\w-]*$/u.test(value)) {
    // Response-shape failure on the login endpoint: classify as the dedicated
    // API_BAD_BODY machine code (mirroring decodeResponseBody) instead of a
    // plain Error that the chokepoint would render as code INTERNAL.
    throw new ApiError('Token endpoint returned an invalid resource', undefined, undefined, 'API_BAD_BODY');
  }
  return value;
}

/**
 * Extract a named field from a JSON auth resource. The auth endpoints answer
 * with JSON objects ({"salt": ...}, {"token": ...}); anything else is a
 * contract violation.
 * @param body - response body text from the auth endpoint
 * @param field - the field name to extract (salt or token)
 * @param label - display label naming the resource in error messages (e.g. Salt)
 * @returns the non-empty field value
 */
export function parseAuthField(body: string, field: string, label: string): string {
  let parsed: unknown;
  try {
    parsed = JSON.parse(body);
  } catch {
    parsed = undefined;
  }
  if (parsed !== null && typeof parsed === 'object' && typeof (parsed as Record<string, unknown>)[field] === 'string') {
    const value = (parsed as Record<string, string>)[field];
    if (value.length > 0) return value;
  }
  throw new ApiError(
    `${label} endpoint returned an invalid resource`,
    undefined,
    undefined,
    'API_BAD_BODY',
  );
}

/** Singleton instance */
export const dc3Client = new Dc3Client();
