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
import { createCipheriv, createDecipheriv, randomBytes, scryptSync } from 'node:crypto';
import { homedir, hostname, userInfo, arch } from 'node:os';
import { join } from 'node:path';
import { quarantineFile, withLock, writeFileAtomic } from './atomic-fs.js';
import type { CredentialStore } from './credential-store.js';

/**
 * Encrypted file credential store — fallback when OS keychain is unavailable.
 *
 * SECURITY DESIGN:
 * - Payloads are AES-256-GCM encrypted under a random 256-bit key stored in
 *   ~/.dc3/credentials.key (mode 0600 on POSIX; explicit restricted ACL on
 *   Windows). The ciphertext file alone can no longer be decrypted offline:
 *   the key is random per installation, not derivable from public machine
 *   identity. The OS keychain remains the recommended primary store.
 * - The serialized format NEVER contains passwords (or any mirror of them):
 *   only iv/tag/ciphertext travel to disk. Legacy files that leaked a
 *   plaintext `entries` copy are scrubbed (re-encrypted) on first load.
 * - Unreadable files are quarantined to a timestamped sibling and a warning
 *   is emitted; the remaining entries are never silently reset in place.
 * - All writes are atomic and serialized by an exclusive lock.
 */
const DC3_DIR = join(homedir(), '.dc3');
const ENC_PATH = join(DC3_DIR, 'credentials.enc');
const KEY_PATH = join(DC3_DIR, 'credentials.key');
const ALGORITHM = 'aes-256-gcm';

/** Current on-disk format: ciphertext only, no plaintext echo of any entry. */
interface EncryptedFile {
  v: 2;
  iv: string; // hex
  tag: string; // hex
  data: string; // hex-encoded ciphertext of the JSON identifier→password map
}

/**
 * Pre-fix format: derived-key ciphertext plus a full PLAINTEXT copy of every
 * entry. Recognized only to migrate away from it.
 */
interface LegacyEncryptedFile {
  iv: string;
  tag: string;
  data: string;
  entries: Record<string, string>;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isEncryptedFile(value: unknown): value is EncryptedFile {
  return (
    isRecord(value) &&
    value.v === 2 &&
    typeof value.iv === 'string' &&
    typeof value.tag === 'string' &&
    typeof value.data === 'string'
  );
}

function isLegacyEncryptedFile(value: unknown): value is LegacyEncryptedFile {
  return (
    isRecord(value) &&
    typeof value.iv === 'string' &&
    typeof value.tag === 'string' &&
    typeof value.data === 'string' &&
    isRecord(value.entries) &&
    Object.values(value.entries).every((entry) => typeof entry === 'string')
  );
}

/**
 * Legacy key derivation (public machine identity + static salt). Kept ONLY to
 * migrate old files; no new encryption ever uses it.
 * @returns the legacy 256-bit key
 */
function legacyDeriveKey(): Buffer {
  const material = `${hostname()}-${userInfo().username}-${arch()}`;
  return scryptSync(material, 'dc3-cli-static-salt', 32);
}

function decryptEntries(
  key: Buffer,
  ivHex: string,
  tagHex: string,
  dataHex: string,
): Record<string, string> {
  const decipher = createDecipheriv(ALGORITHM, key, Buffer.from(ivHex, 'hex'));
  decipher.setAuthTag(Buffer.from(tagHex, 'hex'));
  const plain = Buffer.concat([decipher.update(Buffer.from(dataHex, 'hex')), decipher.final()]);
  const parsed: unknown = JSON.parse(plain.toString('utf8'));
  if (!isRecord(parsed) || !Object.values(parsed).every((v) => typeof v === 'string')) {
    throw new Error('decrypted payload is not an identifier→password map');
  }
  return parsed as Record<string, string>;
}

/**
 * Read the key file; null when absent or malformed.
 * @returns the 256-bit key, or null when unavailable
 */
async function loadKeyFile(): Promise<Buffer | null> {
  try {
    const key = Buffer.from((await readFile(KEY_PATH, 'utf8')).trim(), 'hex');
    return key.length === 32 ? key : null;
  } catch {
    return null;
  }
}

/**
 * Load or create the random key file. Must only be called while holding the
 * credentials lock so two first-time writers cannot generate different keys.
 * @returns the existing or freshly generated 256-bit key
 */
async function ensureKeyFileLocked(): Promise<Buffer> {
  const existing = await loadKeyFile();
  if (existing) {
    return existing;
  }
  const key = randomBytes(32);
  await writeFileAtomic(KEY_PATH, key.toString('hex'), { restrictToOwner: true });
  return key;
}

async function encryptAndWriteLocked(
  key: Buffer,
  entries: Record<string, string>,
): Promise<void> {
  const iv = randomBytes(16);
  const cipher = createCipheriv(ALGORITHM, key, iv);
  const encrypted = Buffer.concat([
    cipher.update(JSON.stringify(entries), 'utf8'),
    cipher.final(),
  ]);
  const file: EncryptedFile = {
    v: 2,
    iv: iv.toString('hex'),
    tag: cipher.getAuthTag().toString('hex'),
    data: encrypted.toString('hex'),
  };
  await writeFileAtomic(ENC_PATH, JSON.stringify(file), { restrictToOwner: true });
}

/**
 * Quarantine an unreadable credentials file and report loudly. The corrupt
 * bytes survive under a timestamped sibling; the store continues empty so the
 * surviving entries in the quarantine copy stay recoverable (they are never
 * overwritten by a fresh write).
 * @param reason - human-readable cause of the read failure
 * @returns an empty entries map for this session
 */
async function quarantineCredentials(reason: string): Promise<Record<string, string>> {
  let quarantined: string | null = null;
  try {
    quarantined = await quarantineFile(ENC_PATH);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`Warning: could not quarantine ${ENC_PATH} (${message}).`);
  }
  console.error(
    `Warning: ${ENC_PATH} could not be read (${reason}); starting from an empty credential store.` +
      (quarantined ? ` The previous file was preserved as ${quarantined}.` : ''),
  );
  return {};
}

/**
 * Read the entries map while holding the credentials lock. Legacy files
 * (plaintext `entries` echo + machine-derived key) are migrated in place:
 * re-encrypted under the random key file with the plaintext copy removed.
 * @returns the decrypted identifier→password map
 */
async function readEntriesLocked(): Promise<Record<string, string>> {
  return withLock(ENC_PATH, async () => readEntriesUnlocked());
}

async function readEntriesUnlocked(): Promise<Record<string, string>> {
  let raw: string;
  try {
    raw = await readFile(ENC_PATH, 'utf8');
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
      return {};
    }
    throw error;
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return quarantineCredentials('content is not valid JSON');
  }
  if (isEncryptedFile(parsed)) {
    const key = await loadKeyFile();
    if (!key) {
      return quarantineCredentials(`cannot decrypt without ${KEY_PATH}`);
    }
    try {
      return decryptEntries(key, parsed.iv, parsed.tag, parsed.data);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      return quarantineCredentials(`decryption failed (${message})`);
    }
  }
  if (isLegacyEncryptedFile(parsed)) {
    let entries: Record<string, string>;
    try {
      entries = decryptEntries(legacyDeriveKey(), parsed.iv, parsed.tag, parsed.data);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      return quarantineCredentials(`legacy data could not be decrypted (${message})`);
    }
    const key = await ensureKeyFileLocked();
    await encryptAndWriteLocked(key, entries);
    console.error(
      `Warning: ${ENC_PATH} was a legacy file carrying a plaintext copy of stored passwords;` +
        ` migrated to the random-key encrypted format (plaintext copy removed).`,
    );
    return entries;
  }
  return quarantineCredentials('unrecognized file format');
}

/**
 * Credential file store that encrypts payloads at rest.
 */
export class EncryptedFileStore implements CredentialStore {
  readonly name = 'encrypted';

  async isAvailable(): Promise<boolean> {
    // Always available — it's just a file with crypto (no external deps)
    return true;
  }

  async getPassword(identifier: string): Promise<string | null> {
    const entries = await readEntriesLocked();
    return entries[identifier] ?? null;
  }

  async savePassword(identifier: string, password: string): Promise<void> {
    await withLock(ENC_PATH, async () => {
      const entries = await readEntriesUnlocked();
      entries[identifier] = password;
      const key = await ensureKeyFileLocked();
      await encryptAndWriteLocked(key, entries);
    });
  }

  async deletePassword(identifier: string): Promise<void> {
    await withLock(ENC_PATH, async () => {
      const entries = await readEntriesUnlocked();
      if (!(identifier in entries)) {
        return;
      }
      delete entries[identifier];
      if (Object.keys(entries).length === 0) {
        try {
          await unlink(ENC_PATH);
        } catch {
          // Already gone
        }
      } else {
        const key = await ensureKeyFileLocked();
        await encryptAndWriteLocked(key, entries);
      }
    });
  }

  /**
   * Enumerate stored identifiers (config reset scrubbing: token states cover
   * logged-in identities, this covers entries whose token is already gone).
   * @returns all identifiers currently stored in the encrypted file
   */
  async getAllIdentifiers(): Promise<string[]> {
    const entries = await readEntriesLocked();
    return Object.keys(entries);
  }
}
