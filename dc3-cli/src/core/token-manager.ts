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
import { readFile, writeFile, unlink } from 'node:fs/promises';
import { homedir } from 'node:os';
import { join } from 'node:path';
import { isTokenExpired, tokenTtl } from '../utils/jwt.js';

/**
 * Token state persisted to ~/.dc3/tokens.json (mode 0600).
 * One entry per profile.
 */
export interface TokenState {
  token: string;
  salt: string;
  tenant: string;
  username: string;
  issuedAt: number; // epoch seconds
  expiresAt: number; // epoch seconds
  /** 'oauth' tickets travel as Authorization: Bearer; classic login uses X-Auth-* headers */
  authType?: 'login' | 'oauth';
  /** scopes granted to an oauth ticket */
  scope?: string[];
}

/**
 * Token lifecycle states tracked by the token manager.
 */
export interface TokenStates {
  [profile: string]: TokenState;
}

const TOKENS_PATH = join(homedir(), '.dc3', 'tokens.json');

/**
 * TokenManager — manages JWT token lifecycle: parse, persist, renew.
 *
 * Proactive renewal: if token expires within 1 hour, trigger renewal before
 * the API call, so the user never sees a 401.
 *
 * Reactive fallback: if we get a 401 anyway (clock skew, server restart),
 * retry once after renewal.
 */
export class TokenManager {
  private states: TokenStates = {};
  private loaded = false;

  async load(): Promise<void> {
    if (this.loaded) return;
    try {
      const raw = await readFile(TOKENS_PATH, 'utf8');
      this.states = JSON.parse(raw);
    } catch {
      this.states = {};
    }
    this.loaded = true;
  }

  private async save(): Promise<void> {
    await writeFile(TOKENS_PATH, JSON.stringify(this.states, null, 2), {
      mode: 0o600,
    });
  }

  /**
   * Get token state for a profile. Returns null if not logged in.
   * @param profile - profile name whose state to read
   * @returns the stored token state, or null when not logged in
   */
  async getState(profile: string): Promise<TokenState | null> {
    await this.load();
    return this.states[profile] ?? null;
  }

  /**
   * Save or update token state.
   * @param state - token state to persist
   * @param profile - profile name to save the state under
   */
  async saveState(state: TokenState, profile: string): Promise<void> {
    await this.load();
    this.states[profile] = state;
    await this.save();
  }

  /**
   * Clear token state (logout).
   * @param profile - profile name whose state to clear
   */
  async clearState(profile: string): Promise<void> {
    await this.load();
    delete this.states[profile];
    if (Object.keys(this.states).length === 0) {
      try {
        await unlink(TOKENS_PATH);
      } catch {
        // Already gone
      }
    } else {
      await this.save();
    }
  }

  /**
   * Check if the token for a profile needs renewal.
   * @param profile - profile name whose token to check
   * @param thresholdSec — renew if expiring within this many seconds
   * @returns true when the token expires within the threshold
   */
  async needsRenewal(profile: string, thresholdSec: number): Promise<boolean> {
    const state = await this.getState(profile);
    if (!state) return false; // Not logged in
    return isTokenExpired(state.token, thresholdSec);
  }

  /**
   * Get remaining TTL in seconds. Negative if expired.
   * @param profile - profile name whose token to inspect
   * @returns remaining seconds, negative when already expired
   */
  async getTtl(profile: string): Promise<number | null> {
    const state = await this.getState(profile);
    if (!state) return null;
    return tokenTtl(state.token);
  }

  /**
   * Check if a profile has an active token.
   * @param profile - profile name to check
   * @returns true when the profile holds an unexpired token
   */
  async isAuthenticated(profile: string): Promise<boolean> {
    const state = await this.getState(profile);
    if (!state) return false;
    return !isTokenExpired(state.token, 0);
  }

  /**
   * Load all states (for status display).
   * @returns all persisted states keyed by profile
   */
  async getAllStates(): Promise<TokenStates> {
    await this.load();
    return { ...this.states };
  }

  /**
   * Build the X-Auth-* headers from token state.
   * @param state - token state to derive headers from
   * @returns auth headers for the next request
   */
  buildHeaders(state: TokenState): Record<string, string> {
    if (state.authType === 'oauth') {
      return {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${state.token}`,
      };
    }
    return {
      'Content-Type': 'application/json',
      'X-Auth-Tenant': state.tenant,
      'X-Auth-Login': state.username,
      'X-Auth-Token': JSON.stringify({
        salt: state.salt,
        token: state.token,
      }),
    };
  }
}

/** Singleton instance */
export const tokenManager = new TokenManager();
