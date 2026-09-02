/**
 * CredentialStore — interface for password storage backends.
 *
 * Priority chain: keychain → encrypted file → env var → interactive prompt
 * Each backend is tried in order; first one that returns a password wins.
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
 * Resolve a password using the configured credential store type.
 * Falls back through the chain until a password is found.
 */
import { configManager } from './config-manager.js';
import { KeychainStore } from './credential-keychain.js';
import { EncryptedFileStore } from './credential-encrypted.js';
import { EnvCredentialStore } from './credential-env.js';

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
 * Try to get the password from the configured store.
 * Returns null if not available or not found.
 */
export async function resolvePassword(identifier: string): Promise<string | null> {
  try {
    const profile = await configManager.getActiveProfile();
    const store = selectStore(profile.credential_store);
    if (await store.isAvailable()) {
      return await store.getPassword(identifier);
    }
  } catch {
    // Config not yet initialized
  }
  return null;
}

/**
 * Save password to the configured store.
 */
export async function savePasswordToStore(identifier: string, password: string): Promise<void> {
  const profile = await configManager.getActiveProfile();
  const store = selectStore(profile.credential_store);
  if (await store.isAvailable()) {
    await store.savePassword(identifier, password);
  }
}

/**
 * Delete password from the configured store.
 */
export async function deletePasswordFromStore(identifier: string): Promise<void> {
  try {
    const profile = await configManager.getActiveProfile();
    const store = selectStore(profile.credential_store);
    if (await store.isAvailable()) {
      await store.deletePassword(identifier);
    }
  } catch {
    // Config may be gone already
  }
}
