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
   */
  async getState(profile: string): Promise<TokenState | null> {
    await this.load();
    return this.states[profile] ?? null;
  }

  /**
   * Save or update token state.
   */
  async saveState(state: TokenState, profile: string): Promise<void> {
    await this.load();
    this.states[profile] = state;
    await this.save();
  }

  /**
   * Clear token state (logout).
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
   * @param thresholdSec — renew if expiring within this many seconds
   */
  async needsRenewal(profile: string, thresholdSec: number): Promise<boolean> {
    const state = await this.getState(profile);
    if (!state) return false; // Not logged in
    return isTokenExpired(state.token, thresholdSec);
  }

  /**
   * Get remaining TTL in seconds. Negative if expired.
   */
  async getTtl(profile: string): Promise<number | null> {
    const state = await this.getState(profile);
    if (!state) return null;
    return tokenTtl(state.token);
  }

  /**
   * Check if a profile has an active token.
   */
  async isAuthenticated(profile: string): Promise<boolean> {
    const state = await this.getState(profile);
    if (!state) return false;
    return !isTokenExpired(state.token, 0);
  }

  /**
   * Load all states (for status display).
   */
  async getAllStates(): Promise<TokenStates> {
    await this.load();
    return { ...this.states };
  }

  /**
   * Build the X-Auth-* headers from token state.
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
