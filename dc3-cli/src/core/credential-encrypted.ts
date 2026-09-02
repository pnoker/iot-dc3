import { readFile, writeFile, unlink } from 'node:fs/promises';
import { createCipheriv, createDecipheriv, randomBytes, scryptSync } from 'node:crypto';
import { homedir, hostname, userInfo, arch } from 'node:os';
import { join } from 'node:path';
import type { CredentialStore } from './credential-store.js';

/**
 * Encrypted file credential store — fallback when OS keychain is unavailable.
 *
 * Uses AES-256-GCM with a key derived from machine identity.
 * File stored at ~/.dc3/credentials.enc with mode 0600.
 *
 * SECURITY NOTE: The encryption key is derived from hostname+username+arch,
 * which is not a strong secret. This is a fallback, not a replacement for
 * OS keychain. The primary defense is file permissions (0600) and the fact
 * that the credential file is not in version control.
 */
const ENC_PATH = join(homedir(), '.dc3', 'credentials.enc');
const ALGORITHM = 'aes-256-gcm';

interface EncryptedData {
  iv: string;
  tag: string;
  data: string; // hex-encoded ciphertext
  entries: Record<string, string>; // identifier → password
}

function deriveKey(): Buffer {
  const material = `${hostname()}-${userInfo().username}-${arch()}`;
  return scryptSync(material, 'dc3-cli-static-salt', 32);
}

export class EncryptedFileStore implements CredentialStore {
  readonly name = 'encrypted';

  async isAvailable(): Promise<boolean> {
    // Always available — it's just a file with crypto (no external deps)
    return true;
  }

  private async readEntries(): Promise<Record<string, string>> {
    try {
      const raw = await readFile(ENC_PATH, 'utf8');
      const enc: EncryptedData = JSON.parse(raw);
      const key = deriveKey();
      const decipher = createDecipheriv(ALGORITHM, key, Buffer.from(enc.iv, 'hex'));
      decipher.setAuthTag(Buffer.from(enc.tag, 'hex'));
      const decrypted = Buffer.concat([
        decipher.update(Buffer.from(enc.data, 'hex')),
        decipher.final(),
      ]);
      return JSON.parse(decrypted.toString('utf8'));
    } catch {
      return {};
    }
  }

  private async writeEntries(entries: Record<string, string>): Promise<void> {
    const key = deriveKey();
    const iv = randomBytes(16);
    const cipher = createCipheriv(ALGORITHM, key, iv);
    const plaintext = JSON.stringify(entries);
    const encrypted = Buffer.concat([cipher.update(plaintext, 'utf8'), cipher.final()]);
    const tag = cipher.getAuthTag();
    const data: EncryptedData = {
      iv: iv.toString('hex'),
      tag: tag.toString('hex'),
      data: encrypted.toString('hex'),
      entries,
    };
    await writeFile(ENC_PATH, JSON.stringify(data), { mode: 0o600 });
  }

  async getPassword(identifier: string): Promise<string | null> {
    const entries = await this.readEntries();
    return entries[identifier] ?? null;
  }

  async savePassword(identifier: string, password: string): Promise<void> {
    const entries = await this.readEntries();
    entries[identifier] = password;
    await this.writeEntries(entries);
  }

  async deletePassword(identifier: string): Promise<void> {
    const entries = await this.readEntries();
    delete entries[identifier];
    if (Object.keys(entries).length === 0) {
      try {
        await unlink(ENC_PATH);
      } catch {
        // Already gone
      }
    } else {
      await this.writeEntries(entries);
    }
  }
}
