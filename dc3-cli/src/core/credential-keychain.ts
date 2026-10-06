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
import { execFile } from 'node:child_process';
import { access, constants } from 'node:fs/promises';
import { delimiter, join } from 'node:path';
import { promisify } from 'node:util';
import type { CredentialStore } from './credential-store.js';

const execFileAsync = promisify(execFile);

/**
 * OS-level keychain credential store.
 *
 * - macOS:   Keychain Access (`security` command)
 * - Linux:   Secret Service / libsecret (`secret-tool` command)
 * - Windows: Credential Manager via the CredentialManager PowerShell module
 *
 * TRANSPORT SECURITY: every command runs through `execFile` with an argv
 * array — no shell is ever involved, so identifiers and passwords can never
 * break out into command position. Secrets travel on the child's stdin
 * wherever the platform CLI supports it (secret-tool reads the secret from
 * stdin; the Windows PowerShell script reads the password from stdin). On
 * macOS the `security` CLI has no stdin form for `add-generic-password -w`
 * (its `-i` line protocol corrupts passwords containing whitespace), so the
 * password is passed as a discrete argv element — no shell, no interpolation,
 * same-user process listings remain the only exposure vector.
 */
const SERVICE = 'dc3-cli';
/** Bounded probe: an availability check must never add seconds to a command. */
const PROBE_TIMEOUT_MS = 2000;
const OPERATION_TIMEOUT_MS = 10_000;

/**
 * Whether `bin` exists and is executable somewhere on PATH.
 * @param bin - binary name to look for (POSIX platforms only)
 * @returns true when an executable match was found
 */
async function binaryOnPath(bin: string): Promise<boolean> {
  const dirs = (process.env.PATH ?? '').split(delimiter).filter(Boolean);
  for (const dir of dirs) {
    try {
      await access(join(dir, bin), constants.X_OK);
      return true;
    } catch {
      // Not in this directory — keep scanning.
    }
  }
  return false;
}

/**
 * Quote a value as a PowerShell single-quoted literal (embedded quotes doubled).
 * @param value - text to embed in the script
 * @returns the quoted literal, safe from statement breakout
 */
function psQuote(value: string): string {
  return `'${value.replace(/'/gu, "''")}'`;
}

/**
 * Build `powershell -NoProfile -EncodedCommand <base64 UTF-16LE>` argv.
 * @param script - PowerShell statements to encode
 * @returns argv elements for execFile
 */
function psEncodedArgs(script: string): string[] {
  const encoded = Buffer.from(script, 'utf16le').toString('base64');
  return ['-NoProfile', '-EncodedCommand', encoded];
}

/**
 * Windows Credential Manager target name (the module requires tame names).
 * @param identifier - credential identifier to sanitize
 * @returns the target name for the stored credential
 */
function winTarget(identifier: string): string {
  return `dc3-cli-${identifier.replace(/[^a-zA-Z0-9]/gu, '-')}`;
}

/**
 * Shared execFile options for keychain operations.
 * @param extra - additional options (e.g. stdin payload for secrets)
 * @param extra.input - secret bytes piped to the child's stdin
 * @returns merged options for the child process
 */
function execOptions(extra: { input?: string } = {}) {
  return {
    timeout: OPERATION_TIMEOUT_MS,
    windowsHide: true,
    ...extra,
  };
}

/**
 * Keychain store backed by per-platform OS credential managers.
 */
export class KeychainStore implements CredentialStore {
  readonly name = 'keychain';

  async isAvailable(): Promise<boolean> {
    try {
      if (process.platform === 'darwin') {
        return await binaryOnPath('security');
      }
      if (process.platform === 'linux') {
        return await binaryOnPath('secret-tool');
      }
      if (process.platform === 'win32') {
        // Fast bounded capability probe: stock Windows never installs this
        // module, and the answer must be false in ~≤2s, not ~10s.
        await execFileAsync(
          'powershell',
          [
            '-NoProfile',
            '-Command',
            'if (Get-Command New-StoredCredential -ErrorAction SilentlyContinue) { exit 0 }\nexit 1',
          ],
          { timeout: PROBE_TIMEOUT_MS, windowsHide: true },
        );
        return true;
      }
    } catch {
      // Probe failed or timed out: treat as unavailable.
    }
    return false;
  }

  async getPassword(identifier: string): Promise<string | null> {
    try {
      if (process.platform === 'darwin') {
        const { stdout } = await execFileAsync(
          'security',
          ['find-generic-password', '-a', identifier, '-s', SERVICE, '-w'],
          execOptions(),
        );
        return stdout.trim() || null;
      }
      if (process.platform === 'linux') {
        const { stdout } = await execFileAsync(
          'secret-tool',
          ['lookup', 'service', SERVICE, 'account', identifier],
          execOptions(),
        );
        return stdout.trim() || null;
      }
      if (process.platform === 'win32') {
        const script =
          `$c = Get-StoredCredential -Target ${psQuote(winTarget(identifier))}; ` +
          'if ($c) { [Console]::Out.Write($c.GetNetworkCredential().Password) }';
        const { stdout } = await execFileAsync(
          'powershell',
          psEncodedArgs(script),
          execOptions(),
        );
        return stdout.trim() || null;
      }
    } catch {
      // Entry not found or keychain not available
    }
    return null;
  }

  async savePassword(identifier: string, password: string): Promise<void> {
    if (process.platform === 'darwin') {
      // -U updates an existing entry (upsert); argv array, no shell.
      await execFileAsync(
        'security',
        ['add-generic-password', '-a', identifier, '-s', SERVICE, '-w', password, '-U'],
        execOptions(),
      );
    } else if (process.platform === 'linux') {
      // secret-tool reads the secret itself from stdin — it never appears in
      // argv or in an echo pipe.
      await execFileAsync(
        'secret-tool',
        ['store', '--label=dc3-cli', 'service', SERVICE, 'account', identifier],
        execOptions({ input: password }),
      );
    } else if (process.platform === 'win32') {
      // Password travels on stdin; the script embeds only the quoted target
      // and account name (PS single-quote doubling).
      const script =
        `$pw = [Console]::In.ReadToEnd(); ` +
        'if ([string]::IsNullOrEmpty($pw)) { throw "no password received on stdin" }\n' +
        `$sec = ConvertTo-SecureString $pw -AsPlainText -Force; ` +
        `$cred = New-Object System.Management.Automation.PSCredential(${psQuote(identifier)}, $sec); ` +
        `New-StoredCredential -Target ${psQuote(winTarget(identifier))} -Credentials $cred -Persist LocalMachine | Out-Null`;
      await execFileAsync('powershell', psEncodedArgs(script), execOptions({ input: password }));
    }
  }

  async deletePassword(identifier: string): Promise<void> {
    try {
      if (process.platform === 'darwin') {
        await execFileAsync(
          'security',
          ['delete-generic-password', '-a', identifier, '-s', SERVICE],
          execOptions(),
        );
      } else if (process.platform === 'linux') {
        await execFileAsync(
          'secret-tool',
          ['clear', 'service', SERVICE, 'account', identifier],
          execOptions(),
        );
      } else if (process.platform === 'win32') {
        const script = `Remove-StoredCredential -Target ${psQuote(winTarget(identifier))}`;
        await execFileAsync('powershell', psEncodedArgs(script), execOptions());
      }
    } catch {
      // Already deleted or not found — that's fine
    }
  }
}
