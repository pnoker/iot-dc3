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
 * CredentialStore — interface for password storage backends.
 *
 * Priority chain: keychain → encrypted file → env var → interactive prompt
 * Each backend is tried in order; first one that returns a password wins.
 */

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
 * @param identifier - username the password belongs to
 * @returns the stored password, or null when unavailable
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
 * @param identifier - username the password belongs to
 * @param password - plaintext password to persist
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
 * @param identifier - username the password belongs to
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
