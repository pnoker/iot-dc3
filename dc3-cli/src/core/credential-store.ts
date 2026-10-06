/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
/**
 * CredentialStore — interface for password storage backends plus the shared
 * resolution chain.
 *
 * DOCUMENTED CHAIN (implemented by {@link resolvePassword}): the profile's
 * configured store is consulted first, then the remaining stores are tried in
 * priority order keychain → encrypted file → env (DC3_PASSWORD). The first
 * non-empty password wins; unavailable stores are skipped without error.
 * Saving always targets the configured store only and reports whether the
 * password was actually persisted ({@link savePasswordToStore}).
 */
import { configManager } from './config-manager.js';
import { tokenManager, credentialIdentifier } from './token-manager.js';
import { KeychainStore } from './credential-keychain.js';
import { EncryptedFileStore } from './credential-encrypted.js';
import { EnvCredentialStore } from './credential-env.js';

/**
 * Storage contract for persisted credentials.
 */
export interface CredentialStore {
  /** Human-readable name for the store */
  readonly name: string;

  /** Check if this store is available in the current environment */
  isAvailable(): Promise<boolean>;

  /** Retrieve a password by identifier (typically "username@tenant") */
  getPassword(_identifier: string): Promise<string | null>;

  /** Save a password */
  savePassword(_identifier: string, _password: string): Promise<void>;

  /** Delete a password */
  deletePassword(_identifier: string): Promise<void>;
}

/**
 * Outcome of a save attempt: persistence is never silent — when the
 * configured store is unavailable the caller must tell the user.
 */
export interface SavePasswordResult {
  /** Whether the password actually reached persistent storage */
  persisted: boolean;
  /** Name of the store that was targeted */
  store: string;
}

const keychainStore = new KeychainStore();
const encryptedStore = new EncryptedFileStore();
const envStore = new EnvCredentialStore();

function selectStore(storeType: string): CredentialStore {
  switch (storeType) {
    case 'keychain':
      return keychainStore;
    case 'encrypted':
      return encryptedStore;
    case 'env':
      return envStore;
    case 'prompt':
    default:
      // prompt mode: no storage; user must enter password each time
      return {
        name: 'prompt',
        isAvailable: async () => false,
        getPassword: async () => null,
        savePassword: async () => {},
        deletePassword: async () => {},
      };
  }
}

/**
 * Resolve the credential store type a store operation should target. Returns
 * null when the profile (or the whole config) is not usable — callers decide
 * whether that is fatal (save) or just shortens the chain (resolve).
 * @param profileName - explicit profile to target; the active profile when omitted
 * @returns the configured store type, or null when no profile is usable
 */
async function storeTypeForProfile(profileName?: string): Promise<string | null> {
  try {
    if (profileName) {
      return (await configManager.load()).profiles[profileName]?.credential_store ?? null;
    }
    return (await configManager.getActiveProfile()).credential_store;
  } catch {
    // Config not yet initialized (or incomplete): store operations continue
    // without a configured store instead of failing the whole chain.
    return null;
  }
}

/**
 * Stores in resolution order: the configured store first, then the documented
 * fallback chain, deduplicated. Availability is checked per lookup by the caller.
 * @param configured - the profile's configured store, when one could be resolved
 * @returns stores in the order they must be tried
 */
function chainStores(configured: CredentialStore | null): CredentialStore[] {
  const chain = [configured, keychainStore, encryptedStore, envStore].filter(
    (store): store is CredentialStore => store !== null,
  );
  const seen = new Set<string>();
  return chain.filter((store) => {
    if (seen.has(store.name)) {
      return false;
    }
    seen.add(store.name);
    return true;
  });
}

/**
 * Resolve a password through the documented chain: the configured store
 * first, then keychain → encrypted file → env (DC3_PASSWORD). The first
 * non-empty password wins; unavailable or failing stores are skipped
 * without error.
 * @param identifier - username the password belongs to (username@tenant)
 * @param profileName - profile whose configured store to consult first;
 *   defaults to the active profile
 * @returns the resolved password, or null when no store holds one
 */
export async function resolvePassword(
  identifier: string,
  profileName?: string,
): Promise<string | null> {
  const storeType = await storeTypeForProfile(profileName);
  const configured = storeType ? selectStore(storeType) : null;
  for (const store of chainStores(configured)) {
    try {
      if (!(await store.isAvailable())) {
        continue;
      }
      const password = await store.getPassword(identifier);
      if (typeof password === 'string' && password.length > 0) {
        return password;
      }
    } catch {
      // A store that fails to answer is skipped, not fatal.
    }
  }
  return null;
}

/**
 * Save a password to the configured store. Saving never falls back to another
 * store (the user chose where the password lives) and never pretends to have
 * persisted: an unavailable store is reported via the returned result so the
 * command layer can warn.
 * @param identifier - username the password belongs to (username@tenant)
 * @param password - plaintext password to persist
 * @param profileName - profile whose configured store to target; defaults to
 *   the active profile
 * @returns whether the password was persisted and to which store
 */
export async function savePasswordToStore(
  identifier: string,
  password: string,
  profileName?: string,
): Promise<SavePasswordResult> {
  const storeType = await storeTypeForProfile(profileName);
  if (!storeType) {
    throw new Error('No profile configured; cannot save password');
  }
  const store = selectStore(storeType);
  if (!(await store.isAvailable())) {
    return { persisted: false, store: store.name };
  }
  await store.savePassword(identifier, password);
  return { persisted: true, store: store.name };
}

/**
 * Delete a password from the configured store (logout cleanup). Best effort:
 * failures are swallowed — the token state cleanup must not be blocked by a
 * store hiccup.
 * @param identifier - username the password belongs to (username@tenant)
 * @param profileName - profile whose configured store to target; defaults to
 *   the active profile
 */
export async function deletePasswordFromStore(
  identifier: string,
  profileName?: string,
): Promise<void> {
  try {
    const storeType = await storeTypeForProfile(profileName);
    if (!storeType) {
      return;
    }
    const store = selectStore(storeType);
    if (await store.isAvailable()) {
      await store.deletePassword(identifier);
    }
  } catch {
    // Store cleanup must never break logout.
  }
}

/**
 * Delete every known identifier from every storage backend (config reset).
 * Collects identifiers from the token states (logged-in identities) and from
 * the encrypted store (entries whose token already expired away); the OS
 * keychain has no portable enumeration and is scrubbed by identifier only.
 * @param identifiers - extra identifiers to scrub (e.g. from token states)
 */
export async function clearAllStoredCredentials(identifiers: string[]): Promise<void> {
  const targets = new Set(identifiers);
  try {
    for (const identifier of await encryptedStore.getAllIdentifiers()) {
      targets.add(identifier);
    }
  } catch {
    // Enumeration failure is reported per-store below when deletion fails.
  }
  const failures: string[] = [];
  for (const identifier of targets) {
    try {
      if (await keychainStore.isAvailable()) {
        await keychainStore.deletePassword(identifier);
      }
    } catch {
      failures.push(`keychain:${identifier}`);
    }
    try {
      await encryptedStore.deletePassword(identifier);
    } catch {
      failures.push(`encrypted:${identifier}`);
    }
  }
  if (failures.length > 0) {
    throw new Error(`failed to delete: ${failures.join(', ')}`);
  }
}

/**
 * One-call local state reset: wipes config.json, every token state, and every
 * stored password through {@link ConfigManager.reset} so no slice of local
 * auth state can survive a reset. Intended as the single chokepoint the
 * `config reset` command (and any future reset surface) calls.
 */
export async function resetAllLocalState(): Promise<void> {
  const states = await tokenManager.getAllStates();
  const identifiers = Object.values(states).map((state) => credentialIdentifier(state));
  await configManager.reset({
    clearTokens: () => tokenManager.clearAll(),
    clearCredentials: () => clearAllStoredCredentials(identifiers),
  });
}
