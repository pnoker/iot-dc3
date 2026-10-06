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
import { readFile, unlink } from 'node:fs/promises';
import { homedir } from 'node:os';
import { join } from 'node:path';
import { quarantineFile, withLock, writeFileAtomic } from './atomic-fs.js';
import { isTokenExpired, tokenTtl } from '../utils/jwt.js';

/**
 * Token state persisted to ~/.dc3/tokens.json (mode 0600 / restricted ACL).
 * One entry per profile.
 *
 * `tenant` and `username` are the AUTHORITATIVE identity of the login that
 * minted the token: credential lookups during renewal must key the password
 * store by this state identity (`username@tenant` via
 * {@link credentialIdentifier}), not by the mutable profile config, so a
 * post-login `config set tenant` cannot silently orphan the stored password.
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
  /**
   * Per-profile invalidation counter at save time. Bumped by
   * {@link TokenManager.clearState} so an in-flight renewal that captured an
   * older epoch refuses to persist (logout/session-revival guard).
   */
  epoch?: number;
}

/**
 * Token lifecycle states tracked by the token manager.
 */
export interface TokenStates {
  [profile: string]: TokenState;
}

/**
 * On-disk layout (version 2). `epochs` survives entry deletion: a cleared
 * profile keeps its (bumped) epoch so a concurrent renewal minted before the
 * logout can still be detected and discarded. Version 1 files (a flat
 * profile→state map) are migrated on first write.
 */
interface TokenFile {
  version: 2;
  epochs: Record<string, number>;
  states: TokenStates;
}

const TOKENS_PATH = join(homedir(), '.dc3', 'tokens.json');

/**
 * Build the credential-store identifier for a token state's identity.
 * @param state - state (or any object carrying the identity fields) to key
 * @returns the `username@tenant` identifier used by credential stores
 */
export function credentialIdentifier(state: Pick<TokenState, 'username' | 'tenant'>): string {
  return `${state.username}@${state.tenant}`;
}

function emptyTokenFile(): TokenFile {
  return { version: 2, epochs: {}, states: {} };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Loose shape check for a persisted token state entry.
 * @param value - decoded JSON value to inspect
 * @returns true when the value looks like a token state
 */
function looksLikeTokenState(value: unknown): value is TokenState {
  return isRecord(value) && typeof value.token === 'string';
}

/**
 * Shape check for the version-2 on-disk layout.
 * @param value - decoded JSON value to inspect
 * @returns true when the value is a token file
 */
function isTokenFile(value: unknown): value is TokenFile {
  return isRecord(value) && value.version === 2 && isRecord(value.epochs) && isRecord(value.states);
}

/**
 * Shape check for the legacy version-1 flat profile→state map.
 * @param value - decoded JSON value to inspect
 * @returns true when the value is a legacy token states map
 */
function isLegacyTokenStates(value: unknown): value is TokenStates {
  return isRecord(value) && Object.values(value).every((entry) => looksLikeTokenState(entry));
}

/**
 * Read the token file. Missing file → empty state (first run). Unparseable or
 * unrecognized content → quarantine-and-warn: the corrupt bytes are rotated to
 * a timestamped sibling (never silently reset in place, never destroyed), a
 * loud stderr warning names the file and reason, and an empty state is used
 * for this session. Must be called while holding the tokens lock for sections
 * that go on to write.
 * @returns the parsed token file (empty when missing or quarantined)
 */
async function readTokenFile(): Promise<TokenFile> {
  let raw: string;
  try {
    raw = await readFile(TOKENS_PATH, 'utf8');
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
      return emptyTokenFile();
    }
    throw error;
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return quarantineTokens('content is not valid JSON');
  }
  if (isTokenFile(parsed)) {
    // Drop entries that do not look like token states instead of failing hard.
    const states: TokenStates = {};
    for (const [profile, state] of Object.entries(parsed.states)) {
      if (looksLikeTokenState(state)) {
        states[profile] = state;
      }
    }
    return { version: 2, epochs: parsed.epochs, states };
  }
  if (isLegacyTokenStates(parsed)) {
    return { version: 2, epochs: {}, states: parsed };
  }
  return quarantineTokens('unrecognized file format');
}

async function quarantineTokens(reason: string): Promise<TokenFile> {
  let quarantined: string | null = null;
  try {
    quarantined = await quarantineFile(TOKENS_PATH);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`Warning: could not quarantine ${TOKENS_PATH} (${message}).`);
  }
  console.error(
    `Warning: ${TOKENS_PATH} could not be read (${reason}); starting from an empty token state.` +
      (quarantined ? ` The unreadable file was preserved as ${quarantined}.` : ''),
  );
  return emptyTokenFile();
}

async function writeTokenFile(file: TokenFile): Promise<void> {
  await writeFileAtomic(TOKENS_PATH, JSON.stringify(file, null, 2), { restrictToOwner: true });
}

/**
 * TokenManager — manages JWT token lifecycle: parse, persist, renew.
 *
 * Proactive renewal: if token expires within 1 hour, trigger renewal before
 * the API call, so the user never sees a 401.
 *
 * Reactive fallback: if we get a 401 anyway (clock skew, server restart),
 * retry once after renewal.
 *
 * Persistence: every mutation is a locked read-modify-write of the whole file
 * through an atomic temp+fsync+rename, so concurrent CLI processes cannot
 * lose each other's profiles or observe torn writes.
 */
export class TokenManager {
  /**
   * Get token state for a profile. Returns null if not logged in.
   * @param profile - profile name whose state to read
   * @returns the stored token state, or null when not logged in
   */
  async getState(profile: string): Promise<TokenState | null> {
    const file = await readTokenFile();
    return file.states[profile] ? { ...file.states[profile] } : null;
  }

  /**
   * Read the current invalidation epoch for a profile. Survives logout: the
   * epoch keeps counting after the entry is cleared.
   * @param profile - profile name whose epoch to read
   * @returns the current epoch (0 when never logged in or cleared file)
   */
  async getEpoch(profile: string): Promise<number> {
    const file = await readTokenFile();
    return file.epochs[profile] ?? 0;
  }

  /**
   * Save or update token state (unconditional; login and OAuth flows).
   * @param state - token state to persist
   * @param profile - profile name to save the state under
   */
  async saveState(state: TokenState, profile: string): Promise<void> {
    await withLock(TOKENS_PATH, async () => {
      const file = await readTokenFile();
      const epoch = file.epochs[profile] ?? 0;
      file.states[profile] = { ...state, epoch };
      await writeTokenFile(file);
    });
  }

  /**
   * Save a token state only when the profile's epoch still equals the epoch
   * the writer captured before minting the token. This is the renewal guard:
   * a logout (or any clear) that happened while the token was being generated
   * bumps the epoch, the save is refused, and the caller must discard the
   * freshly minted token (cancelling it server-side).
   * @param state - token state to persist
   * @param profile - profile name to save the state under
   * @param expectedEpoch - epoch captured before the token was generated
   * @returns true when the state was persisted; false when the epoch moved
   */
  async saveStateIfEpochUnchanged(
    state: TokenState,
    profile: string,
    expectedEpoch: number,
  ): Promise<boolean> {
    return withLock(TOKENS_PATH, async () => {
      const file = await readTokenFile();
      if ((file.epochs[profile] ?? 0) !== expectedEpoch) {
        return false;
      }
      file.states[profile] = { ...state, epoch: expectedEpoch };
      await writeTokenFile(file);
      return true;
    });
  }

  /**
   * Clear token state for a profile (logout). Bumps the profile's epoch and
   * removes its entry in ONE atomic write, so a concurrent renewal either
   * sees the pre-logout state (fine) or the post-logout epoch (its save is
   * refused). The file itself is kept while other profiles still have state.
   * @param profile - profile name whose state to clear
   */
  async clearState(profile: string): Promise<void> {
    await withLock(TOKENS_PATH, async () => {
      const file = await readTokenFile();
      file.epochs[profile] = (file.epochs[profile] ?? 0) + 1;
      delete file.states[profile];
      await writeTokenFile(file);
    });
  }

  /**
   * Remove every profile's state and the tokens file itself (config reset).
   * Contrary to {@link clearState} this unlinks the file: reset also wipes the
   * profile config, so no renewal can proceed afterwards anyway.
   */
  async clearAll(): Promise<void> {
    await withLock(TOKENS_PATH, async () => {
      try {
        await unlink(TOKENS_PATH);
      } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'ENOENT') {
          throw error;
        }
      }
    });
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
    const file = await readTokenFile();
    return { ...file.states };
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
