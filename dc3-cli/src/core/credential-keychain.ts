import { exec } from 'node:child_process';
import { promisify } from 'node:util';
import type { CredentialStore } from './credential-store.js';

const execAsync = promisify(exec);

/**
 * OS-level keychain credential store.
 *
 * - macOS:   Keychain Access (security command)
 * - Linux:   Secret Service / libsecret (secret-tool command)
 * - Windows: Credential Manager (PowerShell)
 */
export class KeychainStore implements CredentialStore {
  readonly name = 'keychain';

  async isAvailable(): Promise<boolean> {
    try {
      if (process.platform === 'darwin') {
        await execAsync('security -h');
        return true;
      }
      if (process.platform === 'linux') {
        await execAsync('secret-tool --help');
        return true;
      }
      if (process.platform === 'win32') {
        await execAsync('powershell -Command "Get-Help Get-StoredCredential"');
        return true;
      }
    } catch {
      // Not available
    }
    return false;
  }

  async getPassword(identifier: string): Promise<string | null> {
    try {
      if (process.platform === 'darwin') {
        const { stdout } = await execAsync(
          `security find-generic-password -a "${identifier}" -s "dc3-cli" -w`,
          { timeout: 5000 },
        );
        return stdout.trim() || null;
      }
      if (process.platform === 'linux') {
        const { stdout } = await execAsync(
          `secret-tool lookup service dc3-cli account "${identifier}"`,
          { timeout: 5000 },
        );
        return stdout.trim() || null;
      }
      if (process.platform === 'win32') {
        const escapedTarget = `dc3-cli-${identifier.replace(/[^a-zA-Z0-9]/g, '-')}`;
        const { stdout } = await execAsync(
          `powershell -Command "(Get-StoredCredential -Target '${escapedTarget}').GetNetworkCredential().Password"`,
          { timeout: 5000 },
        );
        const result = stdout.trim();
        return result === '' ? null : result;
      }
    } catch {
      // Entry not found or keychain not available
    }
    return null;
  }

  async savePassword(identifier: string, password: string): Promise<void> {
    if (process.platform === 'darwin') {
      // Remove existing entry first (upsert)
      await execAsync(
        `security delete-generic-password -a "${identifier}" -s "dc3-cli" 2>/dev/null; ` +
          `security add-generic-password -a "${identifier}" -s "dc3-cli" -w "${password}" -U`,
        { timeout: 10000 },
      );
    } else if (process.platform === 'linux') {
      await execAsync(
        `echo "${password}" | secret-tool store --label="dc3-cli (${identifier})" service dc3-cli account "${identifier}"`,
        { timeout: 10000 },
      );
    } else if (process.platform === 'win32') {
      const escapedTarget = `dc3-cli-${identifier.replace(/[^a-zA-Z0-9]/g, '-')}`;
      await execAsync(
        `powershell -Command "` +
          `$cred = New-Object System.Management.Automation.PSCredential('${identifier}', ` +
          `(ConvertTo-SecureString '${password}' -AsPlainText -Force)); ` +
          `New-StoredCredential -Target '${escapedTarget}' -Credentials $cred -Persist LocalMachine"`,
        { timeout: 10000 },
      );
    }
  }

  async deletePassword(identifier: string): Promise<void> {
    try {
      if (process.platform === 'darwin') {
        await execAsync(`security delete-generic-password -a "${identifier}" -s "dc3-cli"`, {
          timeout: 5000,
        });
      } else if (process.platform === 'linux') {
        await execAsync(`secret-tool clear service dc3-cli account "${identifier}"`, {
          timeout: 5000,
        });
      } else if (process.platform === 'win32') {
        const escapedTarget = `dc3-cli-${identifier.replace(/[^a-zA-Z0-9]/g, '-')}`;
        await execAsync(
          `powershell -Command "Remove-StoredCredential -Target '${escapedTarget}'"`,
          { timeout: 5000 },
        );
      }
    } catch {
      // Already deleted or not found — that's fine
    }
  }
}
