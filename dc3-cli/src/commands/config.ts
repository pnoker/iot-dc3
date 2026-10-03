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
import { Command, InvalidArgumentError } from 'commander';
import { configManager } from '../core/config-manager.js';
import { detectFormat, printAndExit, type OutputFormat } from '../utils/format.js';
import { parseNonNegativeInteger } from '../utils/manager.js';

/**
 * Parse a numeric `config set` value, rejecting invalid input before anything is persisted.
 * @param key - full config key as typed by the user, used in the error message
 * @param value - raw CLI value to parse
 * @param format - output format for the error report
 * @returns the parsed non-negative integer
 */
function parseSettingInteger(key: string, value: string, format: OutputFormat): number {
  try {
    return parseNonNegativeInteger(value);
  } catch (error) {
    const reason = error instanceof InvalidArgumentError ? error.message : String(error);
    printAndExit(
      { ok: false, message: `Invalid value for ${key}: "${value}" (${reason})` },
      format,
      1,
    );
  }
}

/**
 * Register the `config` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerConfigCommand(program: Command): void {
  const config = program.command('config').description('Configuration management');

  // dc3 config set <key> <value>
  config
    .command('set')
    .description('Set a configuration value')
    .argument('<key>', 'Config key (e.g., gateway, auth.tenant, auth.username)')
    .argument('<value>', 'Config value')
    .option('--format <format>', 'Output format')
    .action(async (key: string, value: string, options) => {
      const format = detectFormat(options.format);
      const profileName = await configManager.getActiveProfileName();

      switch (key) {
        case 'gateway':
          await configManager.setProfile(profileName, { gateway: value });
          printAndExit({ ok: true, key, value, message: `gateway set to ${value}` }, format);
          break;
        case 'auth.tenant':
        case 'tenant':
          await configManager.setProfile(profileName, { tenant: value });
          printAndExit({ ok: true, key, value, message: `tenant set to ${value}` }, format);
          break;
        case 'auth.username':
        case 'username':
          await configManager.setProfile(profileName, { username: value });
          printAndExit({ ok: true, key, value, message: `username set to ${value}` }, format);
          break;
        case 'auth.store':
        case 'credential_store':
          if (!['keychain', 'encrypted', 'env', 'prompt'].includes(value)) {
            printAndExit(
              {
                ok: false,
                message: `Invalid store type: ${value}. Must be one of: keychain, encrypted, env, prompt`,
              },
              format,
              1,
            );
          }
          await configManager.setProfile(profileName, {
            credential_store: value as 'keychain' | 'encrypted' | 'env' | 'prompt',
          });
          printAndExit(
            { ok: true, key, value, message: `credential_store set to ${value}` },
            format,
          );
          break;
        default:
          // Generic setting
          if (key.startsWith('settings.')) {
            const settingKey = key.replace('settings.', '');
            if (settingKey === 'output_format') {
              if (!['json', 'table', 'yaml'].includes(value)) {
                printAndExit({ ok: false, message: `Invalid format: ${value}` }, format, 1);
              }
              await configManager.setSetting('output_format', value as 'json' | 'table' | 'yaml');
            } else if (settingKey === 'color') {
              await configManager.setSetting('color', value === 'true');
            } else if (settingKey === 'renewal_threshold_hours') {
              await configManager.setSetting(
                'renewal_threshold_hours',
                parseSettingInteger(key, value, format),
              );
            } else if (settingKey === 'retry_count') {
              await configManager.setSetting(
                'retry_count',
                parseSettingInteger(key, value, format),
              );
            } else {
              printAndExit({ ok: false, message: `Unknown setting: ${settingKey}` }, format, 1);
            }
          } else {
            printAndExit({ ok: false, message: `Unknown config key: ${key}` }, format, 1);
          }
          printAndExit({ ok: true, key, value, message: `${key} set to ${value}` }, format);
      }
    });

  // dc3 config get <key>
  config
    .command('get')
    .description('Get a configuration value')
    .argument('[key]', 'Config key (omit to show all)')
    .option('--format <format>', 'Output format')
    .action(async (key: string | undefined, options) => {
      const format = detectFormat(options.format);
      const allProfiles = await configManager.getAllProfiles();
      const configData = await configManager.load();
      const profileName = await configManager.getActiveProfileName();
      const profile = allProfiles[profileName];
      const settings = configData.settings;

      if (!profile) {
        printAndExit(
          { ok: false, message: `No profile configured. Run: dc3 config set gateway <url>` },
          format,
          1,
        );
      }

      if (!key) {
        // Show full config (without sensitive data)
        printAndExit(
          {
            profile: profileName,
            gateway: profile.gateway,
            tenant: profile.tenant,
            username: profile.username,
            credential_store: profile.credential_store,
            settings,
          },
          format,
        );
      }

      switch (key) {
        case 'gateway':
          printAndExit(profile.gateway, format);
          break;
        case 'tenant':
        case 'auth.tenant':
          printAndExit(profile.tenant, format);
          break;
        case 'username':
        case 'auth.username':
          printAndExit(profile.username, format);
          break;
        case 'credential_store':
        case 'auth.store':
          printAndExit(profile.credential_store, format);
          break;
        default: {
          // Try settings
          const ks = key! as keyof typeof settings;
          if (ks in settings) {
            printAndExit(settings[ks], format);
          } else {
            printAndExit({ ok: false, message: `Unknown config key: ${key}` }, format, 1);
          }
        }
      }
    });

  // dc3 config list
  config
    .command('list')
    .description('List all profiles')
    .option('--format <format>', 'Output format')
    .action(async (options) => {
      const format = detectFormat(options.format);
      const configData = await configManager.load();
      const profiles = await configManager.getAllProfiles();

      printAndExit(
        {
          current_profile: configData.current_profile,
          settings: configData.settings,
          profiles: Object.fromEntries(
            Object.entries(profiles).map(([name, p]) => [
              name,
              {
                gateway: p.gateway,
                tenant: p.tenant,
                username: p.username,
                credential_store: p.credential_store,
              },
            ]),
          ),
        },
        format,
      );
    });

  // dc3 config profile
  const profileCmd = config.command('profile').description('Manage configuration profiles');

  profileCmd
    .command('use <name>')
    .description('Switch to a profile')
    .option('--format <format>', 'Output format')
    .action(async (name: string, options) => {
      const format = detectFormat(options.format);
      await configManager.switchProfile(name);
      printAndExit({ ok: true, message: `Switched to profile "${name}"` }, format);
    });

  profileCmd
    .command('delete <name>')
    .description('Delete a profile')
    .option('--format <format>', 'Output format')
    .action(async (name: string, options) => {
      const format = detectFormat(options.format);
      await configManager.deleteProfile(name);
      printAndExit({ ok: true, message: `Deleted profile "${name}"` }, format);
    });

  // dc3 config reset
  config
    .command('reset')
    .description('Reset all configuration')
    .option('--format <format>', 'Output format')
    .action(async (options) => {
      const format = detectFormat(options.format);
      const { confirm } = await import('../utils/prompt.js');
      const ok = await confirm('This will delete all profiles and config. Continue?');
      if (!ok) {
        printAndExit({ ok: true, message: 'Cancelled' }, format);
      }
      await configManager.reset();
      printAndExit({ ok: true, message: 'Configuration reset' }, format);
    });
}
