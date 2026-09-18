import type { CredentialStore } from './credential-store.js';

/**
 * Environment variable credential store — reads password from DC3_PASSWORD.
 * Designed for CI/CD and scripting scenarios.
 */
export class EnvCredentialStore implements CredentialStore {
  readonly name = 'env';

  async isAvailable(): Promise<boolean> {
    return !!process.env.DC3_PASSWORD;
  }

  async getPassword(_identifier: string): Promise<string | null> {
    return process.env.DC3_PASSWORD ?? null;
  }

  async savePassword(_identifier: string, _password: string): Promise<void> {
    // Env-based credentials are managed externally (CI/CD secrets)
    // We don't write to the environment.
  }

  async deletePassword(_identifier: string): Promise<void> {
    // Not applicable for env-based credentials
  }
}
