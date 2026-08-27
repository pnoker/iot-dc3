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
  output_format: z.enum(['json', 'table', 'yaml']).default('table'),
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

export type ProfileConfig = z.infer<typeof ProfileConfigSchema>;
export type AppSettings = z.infer<typeof AppSettingsSchema>;
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

async function readConfig(): Promise<Config> {
  try {
    const raw = await readFile(CONFIG_PATH, 'utf8');
    return ConfigSchema.parse(JSON.parse(raw));
  } catch {
    return defaultConfig();
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

  async load(): Promise<Config> {
    if (!this.config) {
      this.config = await readConfig();
    }
    return this.config;
  }

  async save(): Promise<void> {
    if (!this.config) throw new Error('Config not loaded');
    await writeConfig(this.config);
  }

  /**
   * Get the active profile, merging with defaults.
   */
  async getActiveProfile(): Promise<ProfileConfig> {
    const config = await this.load();
    const profileName = config.current_profile;
    const profile = config.profiles[profileName];
    if (!profile) {
      throw new Error(
        `Profile "${profileName}" not found. Run: dc3 config init`,
      );
    }
    return profile;
  }

  /**
   * Get settings from config.
   */
  async getSettings(): Promise<AppSettings> {
    const config = await this.load();
    return config.settings;
  }

  /**
   * Get all profiles.
   */
  async getAllProfiles(): Promise<Record<string, ProfileConfig>> {
    const config = await this.load();
    return config.profiles;
  }

  /**
   * Set a profile value and save.
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
    await writeConfig(config);
    this.config = config;
  }

  /**
   * Switch the active profile.
   */
  async switchProfile(name: string): Promise<void> {
    const config = await this.load();
    if (!config.profiles[name]) {
      throw new Error(`Profile "${name}" not found. Available: ${Object.keys(config.profiles).join(', ')}`);
    }
    config.current_profile = name;
    await writeConfig(config);
    this.config = config;
  }

  /**
   * Delete a profile.
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
    await writeConfig(config);
    this.config = config;
  }

  /**
   * Set a settings value.
   */
  async setSetting<K extends keyof AppSettings>(key: K, value: AppSettings[K]): Promise<void> {
    const config = await this.load();
    config.settings[key] = value;
    await writeConfig(config);
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
