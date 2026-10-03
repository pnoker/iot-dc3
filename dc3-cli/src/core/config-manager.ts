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
import { readFile, writeFile, mkdir, access } from 'node:fs/promises';
import { homedir } from 'node:os';
import { join } from 'node:path';
import { z } from 'zod';

/**
 * Profile configuration schema (without password — passwords live in credential stores).
 */
export const ProfileConfigSchema = z.object({
  gateway: z.string().url().default('http://localhost:8000'),
  tenant: z.string().min(1).default('default'),
  username: z.string().min(1),
  credential_store: z.enum(['keychain', 'encrypted', 'env', 'prompt']).default('keychain'),
});

/**
 * App-level settings.
 */
export const AppSettingsSchema = z.object({
  // Absent by default so the TTY-aware default (table on TTY, json on pipe) applies;
  // persisted only when the user explicitly chooses one.
  output_format: z.enum(['json', 'table', 'yaml']).optional(),
  color: z.boolean().default(true),
  renewal_threshold_hours: z.number().min(0).max(12).default(1),
  retry_count: z.number().min(0).max(3).default(1),
});

/**
 * Full config file schema.
 */
export const ConfigSchema = z.object({
  version: z.literal(1),
  current_profile: z.string().default('default'),
  settings: AppSettingsSchema.default({}),
  profiles: z.record(z.string(), ProfileConfigSchema),
});

/**
 * Per-profile persisted settings (gateway url, credentials, output format).
 */
export type ProfileConfig = z.infer<typeof ProfileConfigSchema>;
/**
 * Application-wide settings derived from the active profile.
 */
export type AppSettings = z.infer<typeof AppSettingsSchema>;
/**
 * Root configuration shape persisted to the CLI config file.
 */
export type Config = z.infer<typeof ConfigSchema>;

const CONFIG_DIR = join(homedir(), '.dc3');
const CONFIG_PATH = join(CONFIG_DIR, 'config.json');

function defaultConfig(): Config {
  return {
    version: 1,
    current_profile: 'default',
    settings: AppSettingsSchema.parse({}),
    profiles: {},
  };
}

async function ensureDir(): Promise<void> {
  try {
    await access(CONFIG_DIR);
  } catch {
    await mkdir(CONFIG_DIR, { recursive: true, mode: 0o755 });
  }
}

/**
 * Best-effort copy of a corrupt config file to `<path>.bak`, overwriting any previous backup.
 * @param raw - exact bytes previously read from the corrupt file
 * @returns true when the backup was written
 */
async function backupCorruptFile(raw: string): Promise<boolean> {
  try {
    await ensureDir();
    await writeFile(`${CONFIG_PATH}.bak`, raw, { mode: 0o644 });
    return true;
  } catch {
    // Backup is best-effort: a failure must never block CLI startup.
    return false;
  }
}

/** Read result: parsed config plus whether the on-disk file was corrupt. */
type ReadConfigResult = { config: Config; corrupt: boolean };

async function readConfig(): Promise<ReadConfigResult> {
  let raw: string;
  try {
    raw = await readFile(CONFIG_PATH, 'utf8');
  } catch {
    // Missing or unreadable file (first run): defaults, no noise, nothing to preserve.
    return { config: defaultConfig(), corrupt: false };
  }
  try {
    return { config: ConfigSchema.parse(JSON.parse(raw)), corrupt: false };
  } catch {
    const backedUp = await backupCorruptFile(raw);
    console.error(
      `Warning: ${CONFIG_PATH} is corrupt; using defaults for this session (file left in place` +
        `${backedUp ? `, copy saved to ${CONFIG_PATH}.bak` : ''}).`,
    );
    return { config: defaultConfig(), corrupt: true };
  }
}

async function writeConfig(config: Config): Promise<void> {
  await ensureDir();
  await writeFile(CONFIG_PATH, JSON.stringify(config, null, 2), { mode: 0o644 });
}

/**
 * ConfigManager — manages ~/.dc3/config.json
 *
 * File layout:
 * {
 *   "version": 1,
 *   "current_profile": "default",
 *   "settings": { "output_format": "table", "color": true, ... },
 *   "profiles": {
 *     "default": { "gateway": "...", "tenant": "...", "username": "...", "credential_store": "keychain" },
 *     "prod": { "gateway": "...", ... }
 *   }
 * }
 */
export class ConfigManager {
  private config: Config | null = null;
  /** True while the in-memory config came from defaults because the file on disk was corrupt. */
  private lastLoadCorrupt = false;
  /** Per-invocation profile override installed by the global `--profile` option. */
  private profileOverride: string | null = null;

  async load(): Promise<Config> {
    if (!this.config) {
      const result = await readConfig();
      this.config = result.config;
      this.lastLoadCorrupt = result.corrupt;
    }
    return this.config;
  }

  /**
   * Persist the in-memory config, warning first when it was built from defaults because the
   * last load failed: settings and profiles from the corrupt file are not carried over into
   * what gets written (they stay preserved in the `.bak` copy).
   * @param config - config to persist
   */
  private async persist(config: Config): Promise<void> {
    if (this.lastLoadCorrupt) {
      console.error(
        `Warning: writing ${CONFIG_PATH} from in-memory defaults; other settings and profiles` +
          ` from the corrupt file are not carried over (preserved in ${CONFIG_PATH}.bak).`,
      );
    }
    await writeConfig(config);
    this.lastLoadCorrupt = false;
  }

  async save(): Promise<void> {
    if (!this.config) throw new Error('Config not loaded');
    await this.persist(this.config);
  }

  /**
   * Install a per-invocation profile override (global `--profile` option). The
   * name is validated to exist; a missing profile throws, which the top-level
   * handler maps to exit code 1. Every profile-dependent read (gateway, tenant,
   * token resolution) flows through {@link getActiveProfileName} afterwards.
   * @param name - profile name to use for this invocation only
   */
  async setProfileOverride(name: string): Promise<void> {
    const config = await this.load();
    if (!config.profiles[name]) {
      throw new Error(
        `Profile "${name}" not found. Available: ${Object.keys(config.profiles).join(', ') || '(none)'}`,
      );
    }
    this.profileOverride = name;
  }

  /**
   * Get the active profile name: the per-invocation `--profile` override when
   * installed, otherwise the persisted `current_profile`.
   * @returns the profile name all profile-dependent reads must resolve through
   */
  async getActiveProfileName(): Promise<string> {
    if (this.profileOverride) {
      return this.profileOverride;
    }
    const config = await this.load();
    return config.current_profile;
  }

  /**
   * Get the active profile, merging with defaults.
   * @returns the active profile merged with defaults
   */
  async getActiveProfile(): Promise<ProfileConfig> {
    const profileName = await this.getActiveProfileName();
    const config = await this.load();
    const profile = config.profiles[profileName];
    if (!profile) {
      throw new Error(`Profile "${profileName}" not found. Run: dc3 config init`);
    }
    return profile;
  }

  /**
   * Get settings from config.
   * @returns the application-wide settings
   */
  async getSettings(): Promise<AppSettings> {
    const config = await this.load();
    return config.settings;
  }

  /**
   * Get all profiles.
   * @returns all profiles keyed by name
   */
  async getAllProfiles(): Promise<Record<string, ProfileConfig>> {
    const config = await this.load();
    return config.profiles;
  }

  /**
   * Set a profile value and save.
   * @param profileName - profile name used for the lookup
   * @param partial - profile fields to overwrite
   */
  async setProfile(profileName: string, partial: Partial<ProfileConfig>): Promise<void> {
    const config = await this.load();
    const existing = config.profiles[profileName] || {};
    // Validate the MERGED profile, not a blank template: fields without schema
    // defaults (username) only need to be present after the merge.
    config.profiles[profileName] = ProfileConfigSchema.parse({
      ...existing,
      ...partial,
    });
    await this.persist(config);
    this.config = config;
  }

  /**
   * Switch the active profile.
   * @param name - resource name
   */
  async switchProfile(name: string): Promise<void> {
    const config = await this.load();
    if (!config.profiles[name]) {
      throw new Error(
        `Profile "${name}" not found. Available: ${Object.keys(config.profiles).join(', ')}`,
      );
    }
    config.current_profile = name;
    await this.persist(config);
    this.config = config;
  }

  /**
   * Delete a profile.
   * @param name - resource name
   */
  async deleteProfile(name: string): Promise<void> {
    const config = await this.load();
    if (!config.profiles[name]) {
      throw new Error(`Profile "${name}" not found`);
    }
    if (config.current_profile === name) {
      throw new Error(`Cannot delete active profile "${name}". Switch first.`);
    }
    delete config.profiles[name];
    await this.persist(config);
    this.config = config;
  }

  /**
   * Set a settings value.
   * @param key - lookup key
   * @param value - value to set
   */
  async setSetting<K extends keyof AppSettings>(key: K, value: AppSettings[K]): Promise<void> {
    const config = await this.load();
    config.settings[key] = value;
    await this.persist(config);
    this.config = config;
  }

  /**
   * Reset all config (for --reset).
   */
  async reset(): Promise<void> {
    this.config = defaultConfig();
    await writeConfig(this.config);
  }
}

/** Singleton instance */
export const configManager = new ConfigManager();
