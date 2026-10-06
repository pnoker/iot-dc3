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
import { readFile } from 'node:fs/promises';
import { homedir } from 'node:os';
import { join } from 'node:path';
import { z, type ZodError, type ZodIssue } from 'zod';
import { quarantineFile, withLock, writeFileAtomic } from './atomic-fs.js';

/**
 * Gateway URL field shared by every profile schema variant. Stricter than a
 * bare `z.string().url()`: only http/https origins are accepted, without a
 * path, query, or fragment — a misconfigured gateway must fail at
 * `config set` time, not as a confusing fetch error on first use.
 */
const GatewaySchema = z
  .string()
  .url()
  .regex(/^https?:\/\//u, 'gateway must use http or https')
  .refine((value) => {
    try {
      const url = new URL(value);
      return url.pathname === '/' && url.search === '' && url.hash === '';
    } catch {
      return false;
    }
  }, 'gateway must be a bare origin (no path, query, or fragment)');

/**
 * Profile configuration schema (without password — passwords live in credential stores).
 * `username` is required at CONSUMPTION time (see {@link ConfigManager.getActiveProfile}),
 * not at persistence time, so a fresh install can bootstrap field by field
 * (`config set gateway ...` first, username last).
 */
export const ProfileConfigSchema = z.object({
  gateway: GatewaySchema.default('http://localhost:8000'),
  tenant: z.string().min(1).default('default'),
  username: z.string().default(''),
  credential_store: z.enum(['keychain', 'encrypted', 'env', 'prompt']).default('keychain'),
});

/**
 * Shape persisted inside config.json: identical to {@link ProfileConfigSchema}
 * except `username` may be absent until the profile is actually used.
 */
export const StoredProfileSchema = ProfileConfigSchema.extend({
  username: z.string().min(1).optional(),
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
  profiles: z.record(z.string(), StoredProfileSchema),
});

/**
 * Per-profile persisted settings (gateway url, credentials, output format).
 */
export type ProfileConfig = z.infer<typeof ProfileConfigSchema>;
/**
 * Profile shape as persisted (username may still be unset during bootstrap).
 */
export type StoredProfile = z.infer<typeof StoredProfileSchema>;
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

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Collapse a ZodError into one readable line: callers must never leak the raw
 * issues array to users. Used by every schema-validating setter in this module.
 * @param label - configuration key the value belongs to
 * @param error - the zod failure to render
 * @param value - the rejected value
 * @returns a single-line validation message
 */
function validationMessage(label: string, error: ZodError, value: unknown): string {
  const issue: ZodIssue | undefined = error.issues[0];
  if (!issue) {
    return `Invalid value for ${label}: ${JSON.stringify(value)}`;
  }
  const detail = (() => {
    switch (issue.code) {
      case 'invalid_type':
        return `expected ${issue.expected}, received ${issue.received}`;
      case 'too_small':
        return `must be ${issue.inclusive ? '>=' : '>'} ${String(issue.minimum)}`;
      case 'too_big':
        return `must be ${issue.inclusive ? '<=' : '<'} ${String(issue.maximum)}`;
      default:
        return issue.message;
    }
  })();
  return `Invalid value for ${label}: ${JSON.stringify(value)} (${detail})`;
}

/** How the last load degraded, if at all. */
type LoadDegraded = 'none' | 'corrupt' | 'salvaged';

/** Read result: parsed config, the readable raw bytes (for last-good backup), and degradation. */
type ReadConfigResult = { config: Config; raw: string | null; degraded: LoadDegraded };

/**
 * Rebuild a config from a structurally valid but partially invalid document:
 * parseable profiles survive, invalid settings fall back per-field with their
 * schema defaults. This is the defense against the CLI persisting state it
 * cannot read back: a single out-of-range value must never blank the profiles.
 * Returns null when the document is structurally unrecognizable.
 * @param parsed - JSON-decoded document to salvage
 * @returns a config with every parseable part kept, or null
 */
function salvageConfig(parsed: unknown): Config | null {
  if (!isRecord(parsed) || parsed.version !== 1) {
    return null;
  }
  const dropped: string[] = [];
  const config = defaultConfig();
  if (typeof parsed.current_profile === 'string') {
    config.current_profile = parsed.current_profile;
  } else if ('current_profile' in parsed) {
    dropped.push('current_profile');
  }
  if (isRecord(parsed.settings)) {
    for (const key of Object.keys(parsed.settings)) {
      if (key in AppSettingsSchema.shape) {
        const result = (AppSettingsSchema.shape as Record<string, z.ZodTypeAny>)[key].safeParse(
          parsed.settings[key],
        );
        if (result.success) {
          // safeParse succeeded: the union output is assignable to this settings key.
          (config.settings as Record<string, unknown>)[key] = result.data;
        } else {
          dropped.push(`settings.${key}`);
        }
      } else {
        dropped.push(`settings.${key} (unknown key)`);
      }
    }
  } else if ('settings' in parsed) {
    dropped.push('settings');
  }
  if (isRecord(parsed.profiles)) {
    for (const [name, profile] of Object.entries(parsed.profiles)) {
      const result = StoredProfileSchema.safeParse(profile);
      if (result.success) {
        config.profiles[name] = result.data;
      } else {
        dropped.push(`profile "${name}"`);
      }
    }
  } else if ('profiles' in parsed) {
    dropped.push('profiles');
  }
  console.error(
    `Warning: ${CONFIG_PATH} contains invalid values (${dropped.join(', ')});` +
      ` invalid parts fall back to defaults, valid profiles are kept.`,
  );
  return config;
}

async function quarantineConfig(reason: string): Promise<void> {
  let quarantined: string | null = null;
  try {
    quarantined = await quarantineFile(CONFIG_PATH);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`Warning: could not quarantine ${CONFIG_PATH} (${message}).`);
  }
  console.error(
    `Warning: ${CONFIG_PATH} is corrupt (${reason}); using defaults for this session` +
      (quarantined
        ? ` — the corrupt file was preserved as ${quarantined}`
        : ' — the corrupt file was left in place'),
  );
}

async function readConfig(): Promise<ReadConfigResult> {
  let raw: string;
  try {
    raw = await readFile(CONFIG_PATH, 'utf8');
  } catch {
    // Missing or unreadable file (first run): defaults, no noise, nothing to preserve.
    return { config: defaultConfig(), raw: null, degraded: 'none' };
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    await quarantineConfig('not valid JSON');
    return { config: defaultConfig(), raw: null, degraded: 'corrupt' };
  }
  const result = ConfigSchema.safeParse(parsed);
  if (result.success) {
    return { config: result.data, raw, degraded: 'none' };
  }
  const salvaged = salvageConfig(parsed);
  if (salvaged) {
    return { config: salvaged, raw, degraded: 'salvaged' };
  }
  await quarantineConfig('unrecognized structure');
  return { config: defaultConfig(), raw: null, degraded: 'corrupt' };
}

async function writeConfig(config: Config): Promise<void> {
  await writeFileAtomic(CONFIG_PATH, JSON.stringify(config, null, 2), { mode: 0o644 });
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
 *
 * Writes are atomic (temp+fsync+rename) and every mutation re-reads the file
 * inside an exclusive lock, so concurrent CLI processes never lose updates.
 * `<path>.bak` always holds the last readable bytes, never corrupt ones.
 */
export class ConfigManager {
  private config: Config | null = null;
  /** How the in-memory config degraded at load, if at all. */
  private loadDegraded: LoadDegraded = 'none';
  /** Per-invocation profile override installed by the global `--profile` option. */
  private profileOverride: string | null = null;

  async load(): Promise<Config> {
    if (!this.config) {
      const result = await readConfig();
      this.config = result.config;
      this.loadDegraded = result.degraded;
    }
    return this.config;
  }

  /**
   * Run a read-modify-write cycle on the config file: re-read fresh inside an
   * exclusive lock, apply `update`, snapshot the last readable bytes to
   * `.bak`, and atomically persist the result.
   * @param update - mutates the freshly read config in place
   * @returns the persisted config
   */
  private async mutate(update: (_config: Config) => void | Promise<void>): Promise<Config> {
    const next = await withLock(CONFIG_PATH, async () => {
      const fresh = await readConfig();
      await update(fresh.config);
      if (fresh.raw !== null) {
        try {
          await writeFileAtomic(`${CONFIG_PATH}.bak`, fresh.raw, { mode: 0o644 });
        } catch {
          // Last-good backup is best-effort: never block the user's write.
        }
      }
      if (fresh.degraded === 'corrupt') {
        console.error(
          `Warning: writing ${CONFIG_PATH} from in-memory defaults; settings and profiles from` +
            ` the corrupt file are not carried over (the corrupt bytes were quarantined next to it).`,
        );
      }
      await writeConfig(fresh.config);
      return fresh.config;
    });
    this.config = next;
    this.loadDegraded = 'none';
    return next;
  }

  /**
   * Persist the in-memory config as-is (rare; mutations should use the
   * internal locked path instead so concurrent writers are not lost).
 */
  async save(): Promise<void> {
    if (!this.config) throw new Error('Config not loaded');
    await writeConfig(this.config);
    this.loadDegraded = 'none';
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
   * Get the active profile with defaults applied. This is the consumption
   * point that enforces `username`: bootstrap may persist a profile without
   * one, but no authenticated request can be built until it is set.
   * @returns the active profile merged with defaults
   */
  async getActiveProfile(): Promise<ProfileConfig> {
    const profileName = await this.getActiveProfileName();
    const config = await this.load();
    const stored = config.profiles[profileName];
    if (!stored) {
      throw new Error(
        `Profile "${profileName}" not found. Create it with: dc3 config set gateway <url>`,
      );
    }
    const result = ProfileConfigSchema.safeParse(stored);
    if (!result.success) {
      throw new Error(
        `Profile "${profileName}" is incomplete (${validationMessage(
          `profiles.${profileName}`,
          result.error,
          stored,
        )}). Complete it with: dc3 config set username <name>`,
      );
    }
    // Consumption-time check: the schema allows an empty username during
    // bootstrap (config set gateway first, username later), but no
    // authenticated request can be built without one.
    if (!result.data.username || result.data.username.trim() === '') {
      throw new Error(
        `Profile "${profileName}" has no username. Set it with: dc3 config set auth.username <name>`,
      );
    }
    return result.data;
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
   * Get all profiles as persisted (username may be absent during bootstrap).
   * @returns all profiles keyed by name
   */
  async getAllProfiles(): Promise<Record<string, StoredProfile>> {
    const config = await this.load();
    return config.profiles;
  }

  /**
   * Set profile fields and save. Each PROVIDED key is validated on its own
   * against the per-key schema (invalid input fails loudly, file untouched);
   * the merged profile is intentionally NOT validated as a whole so partial
   * bootstrap writes are legal — {@link getActiveProfile} enforces username.
   * @param profileName - profile name used for the lookup
   * @param partial - profile fields to overwrite
   */
  async setProfile(profileName: string, partial: Partial<ProfileConfig>): Promise<void> {
    for (const [key, value] of Object.entries(partial) as Array<[keyof StoredProfile, unknown]>) {
      const field = StoredProfileSchema.shape[key];
      const result = field.safeParse(value);
      if (!result.success) {
        throw new Error(validationMessage(String(key), result.error, value));
      }
    }
    await this.mutate((config) => {
      config.profiles[profileName] = StoredProfileSchema.parse({
        ...config.profiles[profileName],
        ...partial,
      });
    });
  }

  /**
   * Switch the active profile.
   * @param name - resource name
   */
  async switchProfile(name: string): Promise<void> {
    await this.mutate((config) => {
      if (!config.profiles[name]) {
        throw new Error(
          `Profile "${name}" not found. Available: ${Object.keys(config.profiles).join(', ')}`,
        );
      }
      config.current_profile = name;
    });
  }

  /**
   * Delete a profile.
   * @param name - resource name
   */
  async deleteProfile(name: string): Promise<void> {
    await this.mutate((config) => {
      if (!config.profiles[name]) {
        throw new Error(`Profile "${name}" not found`);
      }
      if (config.current_profile === name) {
        throw new Error(`Cannot delete active profile "${name}". Switch first.`);
      }
      delete config.profiles[name];
    });
  }

  /**
   * Set a settings value. The value is validated against the per-key schema
   * BEFORE anything is persisted, so the CLI can never again write a config
   * it refuses to read back (out-of-range values exit as validation errors
   * with the file untouched).
   * @param key - lookup key
   * @param value - value to set
   */
  async setSetting<K extends keyof AppSettings>(key: K, value: AppSettings[K]): Promise<void> {
    const result = AppSettingsSchema.shape[key].safeParse(value);
    if (!result.success) {
      throw new Error(validationMessage(`settings.${String(key)}`, result.error, value));
    }
    await this.mutate((config) => {
      // Per-key safeParse above guarantees the shape; the union output cannot
      // be statically matched back onto the generic key.
      config.settings[key] = result.data as AppSettings[K];
    });
  }

  /**
   * Reset all config (config reset). Local auth state spans three stores
   * (config.json, tokens.json, credential stores); the optional clearers make
   * this the single chokepoint that wipes all of them — pass
   * `clearTokens: () => tokenManager.clearAll()` and
   * `clearCredentials: () => clearAllStoredCredentials(ids)` (or call
   * `resetAllLocalState()` from credential-store.ts) so a reset can never
   * leave live tokens or stored passwords behind. Clearer failures are
   * reported loudly naming what survived; they never block the config reset.
   * @param clearers - optional local-state cleanup hooks run after the config write
   */
  async reset(clearers: ResetClearers = {}): Promise<void> {
    await withLock(CONFIG_PATH, async () => {
      await writeConfig(defaultConfig());
    });
    this.config = defaultConfig();
    this.loadDegraded = 'none';
    this.profileOverride = null;
    const steps: Array<[string, (() => Promise<void>) | undefined]> = [
      ['token state (tokens.json)', clearers.clearTokens],
      ['stored passwords (credential stores)', clearers.clearCredentials],
    ];
    for (const [label, clearer] of steps) {
      if (!clearer) continue;
      try {
        await clearer();
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        console.error(`Warning: config reset could not clear ${label}: ${message}`);
      }
    }
  }
}

/**
 * Local-state cleanup hooks for {@link ConfigManager.reset}.
 */
export interface ResetClearers {
  /** Wipe all token states (e.g. `tokenManager.clearAll`). */
  clearTokens?: () => Promise<void>;
  /** Wipe all stored passwords (e.g. `clearAllStoredCredentials`). */
  clearCredentials?: () => Promise<void>;
}

/** Singleton instance */
export const configManager = new ConfigManager();
