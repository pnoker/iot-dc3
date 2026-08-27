import { Command } from 'commander';
import { configManager } from '../core/config-manager.js';
import { tokenManager } from '../core/token-manager.js';
import { dc3Client, AuthError } from '../core/client.js';
import {
  savePasswordToStore,
  deletePasswordFromStore,
} from '../core/credential-store.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import { prompt, passwordPrompt } from '../utils/prompt.js';

export function registerAuthCommand(program: Command): void {
  const auth = program
    .command('auth')
    .description('Authentication management');

  // dc3 auth login
  auth
    .command('login')
    .description('Log in to the DC3 platform')
    .option('-t, --tenant <tenant>', 'Tenant code')
    .option('-u, --username <username>', 'Login username')
    .option('-p, --password <password>', 'Password (not recommended — use interactive mode)')
    .option('--store <type>', 'Credential store type: keychain, encrypted, env, prompt', 'keychain')
    .option('--no-save', 'Do not save password (token expiry will require manual re-login)')
    .option('--format <format>', 'Output format: json, table, yaml')
    .action(async (options) => {
      try {
        const profileName = (await configManager.load()).current_profile;
        const tenant = options.tenant || (await prompt('Tenant: '));
        const username = options.username || (await prompt('Username: '));
        const password = options.password || (await passwordPrompt('Password: '));
        const format = detectFormat(options.format);

        const token = await dc3Client.login(
          tenant.trim(),
          username.trim(),
          password,
          profileName,
        );

        // Save profile config
        await configManager.setProfile(profileName, {
          tenant: tenant.trim(),
          username: username.trim(),
          credential_store: options.noSave ? 'prompt' : options.store,
        });

        // Save password (unless --no-save)
        if (!options.noSave) {
          await savePasswordToStore(
            `${username.trim()}@${tenant.trim()}`,
            password,
          );
        }

        // Get expiry info for display
        const state = await tokenManager.getState(profileName);
        const expiresAt = state
          ? new Date(state.expiresAt * 1000).toLocaleString()
          : 'unknown';

        // Clear password from memory
        (password as unknown as string).split('').fill('\0');

        printAndExit(
          {
            ok: true,
            tenant: tenant.trim(),
            username: username.trim(),
            token_prefix: token.substring(0, 20) + '...',
            expires_at: expiresAt,
            message: `Login successful. Token expires at ${expiresAt}`,
          },
          format,
        );
      } catch (err) {
        if (err instanceof AuthError) {
          printAndExit({ ok: false, message: err.message }, 'json', 1);
        }
        throw err;
      }
    });

  // dc3 auth logout
  auth
    .command('logout')
    .description('Log out and invalidate current token')
    .option('--format <format>', 'Output format')
    .action(async (options) => {
      const format = detectFormat(options.format);
      const profileName = (await configManager.load()).current_profile;
      const state = await tokenManager.getState(profileName);

      if (!state) {
        printAndExit(
          { ok: true, message: 'Already logged out' },
          format,
        );
      }

      await dc3Client.logout(profileName);
      await deletePasswordFromStore(`${state!.username}@${state!.tenant}`);

      printAndExit(
        { ok: true, message: 'Logged out successfully' },
        format,
      );
    });

  // dc3 auth status
  auth
    .command('status')
    .description('Check authentication status')
    .option('--format <format>', 'Output format')
    .option('-a, --all', 'Show all profiles')
    .action(async (options) => {
      const format = detectFormat(options.format);

      if (options.all) {
        const states = await tokenManager.getAllStates();
        const config = await configManager.load();
        const result: Record<string, unknown> = {};
        for (const [name, state] of Object.entries(states)) {
          const isExpired = state.expiresAt * 1000 < Date.now();
          result[name] = {
            tenant: state.tenant,
            username: state.username,
            authenticated: !isExpired,
            expires_at: new Date(state.expiresAt * 1000).toISOString(),
            remaining: isExpired ? 'expired' : `${Math.floor((state.expiresAt * 1000 - Date.now()) / 3600000)}h`,
          };
        }
        printAndExit(
          {
            current_profile: config.current_profile,
            profiles: result,
          },
          format,
        );
      }

      const profileName = (await configManager.load()).current_profile;
      const state = await tokenManager.getState(profileName);

      if (!state) {
        printAndExit(
          {
            authenticated: false,
            message: 'Not logged in. Run: dc3 auth login',
          },
          format,
        );
      }

      const isExpired = state!.expiresAt * 1000 < Date.now();
      const remainingMs = state!.expiresAt * 1000 - Date.now();
      printAndExit(
        {
          authenticated: !isExpired,
          tenant: state!.tenant,
          username: state!.username,
          expires_at: new Date(state!.expiresAt * 1000).toISOString(),
          remaining: isExpired
            ? 'expired'
            : `${Math.floor(remainingMs / 3600000)}h ${Math.floor((remainingMs % 3600000) / 60000)}m`,
        },
        format,
      );
    });

  // dc3 auth token
  auth
    .command('token')
    .description('Display current token (for scripting)')
    .option('--header', 'Output full X-Auth-* headers as JSON')
    .action(async (options) => {
      const profileName = (await configManager.load()).current_profile;
      const state = await tokenManager.getState(profileName);

      if (!state) {
        printAndExit(
          { error: 'Not logged in. Run: dc3 auth login' },
          'json',
          1,
        );
      }

      if (options.header) {
        const headers = tokenManager.buildHeaders(state!);
        process.stdout.write(JSON.stringify(headers, null, 2) + '\n');
      } else {
        process.stdout.write(state!.token + '\n');
      }
      process.exit(0);
    });
}
