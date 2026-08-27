import { Command } from 'commander';
import { configManager } from '../core/config-manager.js';
import { detectFormat, printAndExit } from '../utils/format.js';

export function registerConfigCommand(program: Command): void {
  const config = program
    .command('config')
    .description('Configuration management');

  // dc3 config set <key> <value>
  config
    .command('set')
    .description('Set a configuration value')
    .argument('<key>', 'Config key (e.g., gateway, auth.tenant, auth.username)')
    .argument('<value>', 'Config value')
    .option('--format <format>', 'Output format')
    .action(async (key: string, value: string, options) => {
      const format = detectFormat(options.format);
      const profileName = (await configManager.load()).current_profile;

      switch (key) {
        case 'gateway':
          await configManager.setProfile(profileName, { gateway: value });
          printAndExit(
            { ok: true, key, value, message: `gateway set to ${value}` },
            format,
          );
          break;
        case 'auth.tenant':
        case 'tenant':
          await configManager.setProfile(profileName, { tenant: value });
          printAndExit(
            { ok: true, key, value, message: `tenant set to ${value}` },
            format,
          );
          break;
        case 'auth.username':
        case 'username':
          await configManager.setProfile(profileName, { username: value });
          printAndExit(
            { ok: true, key, value, message: `username set to ${value}` },
            format,
          );
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
                printAndExit(
                  { ok: false, message: `Invalid format: ${value}` },
                  format,
                  1,
                );
              }
              await configManager.setSetting('output_format', value as 'json' | 'table' | 'yaml');
            } else if (settingKey === 'color') {
              await configManager.setSetting('color', value === 'true');
            } else if (settingKey === 'renewal_threshold_hours') {
              await configManager.setSetting('renewal_threshold_hours', parseInt(value, 10));
            } else if (settingKey === 'retry_count') {
              await configManager.setSetting('retry_count', parseInt(value, 10));
            } else {
              printAndExit(
                { ok: false, message: `Unknown setting: ${settingKey}` },
                format,
                1,
              );
            }
          } else {
            printAndExit(
              { ok: false, message: `Unknown config key: ${key}` },
              format,
              1,
            );
          }
          printAndExit(
            { ok: true, key, value, message: `${key} set to ${value}` },
            format,
          );
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
      const profileName = configData.current_profile;
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
          console.log(profile.gateway);
          break;
        case 'tenant':
        case 'auth.tenant':
          console.log(profile.tenant);
          break;
        case 'username':
        case 'auth.username':
          console.log(profile.username);
          break;
        case 'credential_store':
        case 'auth.store':
          console.log(profile.credential_store);
          break;
        default: {
          // Try settings
          const ks = key! as keyof typeof settings;
          if (ks in settings) {
            console.log(String(settings[ks]));
          } else {
            printAndExit(
              { ok: false, message: `Unknown config key: ${key}` },
              format,
              1,
            );
          }
        }
      }
      process.exit(0);
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
  const profileCmd = config
    .command('profile')
    .description('Manage configuration profiles');

  profileCmd
    .command('use <name>')
    .description('Switch to a profile')
    .action(async (name: string) => {
      await configManager.switchProfile(name);
      console.log(`Switched to profile "${name}"`);
    });

  profileCmd
    .command('delete <name>')
    .description('Delete a profile')
    .action(async (name: string) => {
      await configManager.deleteProfile(name);
      console.log(`Deleted profile "${name}"`);
    });

  // dc3 config reset
  config
    .command('reset')
    .description('Reset all configuration')
    .action(async () => {
      const { confirm } = await import('../utils/prompt.js');
      const ok = await confirm('This will delete all profiles and config. Continue?');
      if (!ok) {
        console.log('Cancelled');
        process.exit(0);
      }
      await configManager.reset();
      console.log('Configuration reset');
    });
}
