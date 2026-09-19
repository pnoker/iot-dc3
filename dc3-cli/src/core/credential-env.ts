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
