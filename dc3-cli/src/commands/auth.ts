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
import { Command } from 'commander';
import { configManager } from '../core/config-manager.js';
import { tokenManager, credentialIdentifier } from '../core/token-manager.js';
import { dc3Client } from '../core/client.js';
import { savePasswordToStore, deletePasswordFromStore } from '../core/credential-store.js';
import { UsageError, AuthError } from '../core/errors.js';
import { detectFormat, printAndExit } from '../utils/format.js';
import { prompt, passwordPrompt } from '../utils/prompt.js';

/** Credential store types accepted by --store and auth.store. */
const STORE_TYPES = ['keychain', 'encrypted', 'env', 'prompt'] as const;

/**
 * Render an epoch-seconds timestamp as ISO-8601. Every machine-readable
 * timestamp (JSON/YAML fields) uses this one format so consumers can parse it
 * regardless of machine locale; locale formats are only for human-only prose
 * (report F049).
 * @param epochSeconds - epoch seconds to render
 * @returns the ISO-8601 UTC timestamp
 */
function isoTimestamp(epochSeconds: number): string {
  return new Date(epochSeconds * 1000).toISOString();
}

/**
 * Read an explicitly provided flag value: an explicit-but-empty value is a
 * usage error, never a silent fallback to prompting (report F053 philosophy).
 * @param value - raw flag value
 * @param hint - flag name used in the error message
 * @returns the trimmed value
 */
function flagValue(value: string, hint: string): string {
  const trimmed = value.trim();
  if (!trimmed) {
    throw new UsageError(`${hint} must not be empty`);
  }
  return trimmed;
}

/**
 * Collect one mandatory input from a flag or an interactive question. Callers
 * gate on `stdin.isTTY` (spec item 4): with a non-interactive stdin the
 * question could never be answered (it would hang on an open pipe or exit 0 on
 * a closed one — report F004), so the missing value fails fast as a usage
 * error naming the flag the script should pass instead.
 * @param hint - flag the value can be passed with, used in the error message
 * @param ask - interactive question asked when stdin can answer
 * @returns the collected value
 */
async function requiredInput(hint: string, ask: () => Promise<string>): Promise<string> {
  if (!process.stdin.isTTY) {
    throw new UsageError(
      `${hint} is required when stdin is not interactive; ` +
        'pass the flag (for secrets, set DC3_PASSWORD) or run dc3 in an interactive terminal',
    );
  }
  const answer = await ask();
  if (!answer.trim()) {
    throw new UsageError(`${hint} must not be empty`);
  }
  return answer.trim();
}

/**
 * Register the `auth` command tree on the CLI program.
 * @param program - commander program to attach the command to
 */
export function registerAuthCommand(program: Command): void {
  const auth = program.command('auth').description('Authentication management');

  // dc3 auth login
  auth
    .command('login')
    .description('Log in to the DC3 platform')
    .option('-t, --tenant <tenant>', 'Tenant code')
    .option('-u, --username <username>', 'Login username')
    .option(
      '-p, --password <password>',
      'Password (not recommended — use DC3_PASSWORD or interactive mode)',
    )
    .option('--store <type>', 'Credential store type: keychain, encrypted, env, prompt')
    .option('--no-save', 'Do not save password (token expiry will require manual re-login)')
    // Visible in help: the flag works, README documents it, and the sibling
    // --client-id/--client-secret/--scope descriptions reference it (report F051).
    .option('--oauth', 'OAuth client_credentials login (requires a registered MCP client)')
    .option('--client-id <id>', 'Registered OAuth client id (with --oauth)')
    .option('--client-secret <secret>', 'Registered OAuth client secret (with --oauth)')
    .option('--scope <scope>', 'Requested scopes, space-separated (with --oauth)')
    .option('--format <format>', 'Output format: json, table, yaml')
    .action(async (options) => {
      const format = detectFormat(options.format);
      if (options.oauth) {
        await loginOAuthAction(options, format);
        return;
      }
      if (
        options.store !== undefined &&
        !(STORE_TYPES as readonly string[]).includes(options.store)
      ) {
        throw new UsageError(
          `invalid --store '${options.store}' (expected keychain, encrypted, env, or prompt)`,
        );
      }
      const profileName = await configManager.getActiveProfileName();
      const tenant =
        options.tenant !== undefined
          ? flagValue(options.tenant, '--tenant')
          : await requiredInput('--tenant <tenant>', () => prompt('Tenant: '));
      const username =
        options.username !== undefined
          ? flagValue(options.username, '--username')
          : await requiredInput('--username <username>', () => prompt('Username: '));
      // Input sources in order: --password, then DC3_PASSWORD (headless/CI
      // logins never prompt — report F033), then the interactive prompt.
      const password =
        options.password ??
        (process.env.DC3_PASSWORD && process.env.DC3_PASSWORD.length > 0
          ? process.env.DC3_PASSWORD
          : await requiredInput('--password <password> (or set DC3_PASSWORD)', () =>
              passwordPrompt('Password: '),
            ));

      const token = await dc3Client.login(tenant, username, password, profileName);

      // --no-save: commander v12 exposes the negation as save === false, never
      // as a noSave property (report F008).
      const noSave = options.save === false;
      // Store selection: --no-save records 'prompt' (the profile reflects that
      // nothing is saved); otherwise an explicit --store wins, and without one
      // the profile keeps its configured store — a plain login must never
      // silently downgrade it back to the keychain default.
      const existingStore = (await configManager.getAllProfiles())[profileName]?.credential_store;
      const store = noSave ? 'prompt' : (options.store ?? existingStore ?? 'keychain');

      // Save profile config
      await configManager.setProfile(profileName, {
        tenant,
        username,
        credential_store: store,
      });

      // Save password (unless --no-save). Persistence is never silent: an
      // unavailable store is reported so the user knows silent renewal will
      // not work (report F005).
      let passwordSaved = false;
      let saveStore: string | undefined;
      if (!noSave) {
        const result = await savePasswordToStore(
          credentialIdentifier({ username, tenant }),
          password,
          profileName,
        );
        passwordSaved = result.persisted;
        saveStore = result.store;
        if (!result.persisted) {
          process.stderr.write(
            `Warning: the password was NOT saved — credential store "${result.store}" is not available ` +
              `on this machine. Silent token renewal is impossible until a working store is configured ` +
              `(dc3 config set auth.store encrypted, or pass --store explicitly).\n`,
          );
        }
      }

      // Get expiry info for display
      const state = await tokenManager.getState(profileName);
      const expiresAt = state ? isoTimestamp(state.expiresAt) : 'unknown';

      printAndExit(
        {
          ok: true,
          tenant,
          username,
          token_prefix: token.substring(0, 20) + '...',
          expires_at: expiresAt,
          password_saved: passwordSaved,
          credential_store: saveStore ?? store,
          message: noSave
            ? `Login successful (--no-save: password not stored). Token expires at ${expiresAt}`
            : `Login successful. Token expires at ${expiresAt}`,
        },
        format,
      );
    });

  /**
   * OAuth client_credentials flow (functional today; kept documented in the
   * README — gateway-side RS256 verification is a deployment concern, not a
   * reason to hide the flag).
   * @param options - command options
   * @param options.clientId - client id to scope the request
   * @param options.clientSecret - client secret issued with the registration
   * @param options.scope - space-separated scope list to request
   * @param format - output format for the rendered result
   */
  async function loginOAuthAction(
    options: { clientId?: string; clientSecret?: string; scope?: string },
    format: ReturnType<typeof detectFormat>,
  ): Promise<void> {
    const profileName = await configManager.getActiveProfileName();
    const clientId =
      options.clientId !== undefined
        ? flagValue(options.clientId, '--client-id')
        : await requiredInput('--client-id <id>', () => prompt('Client id: '));
    const clientSecret =
      options.clientSecret ??
      (await requiredInput('--client-secret <secret>', () => passwordPrompt('Client secret: ')));
    const result = await dc3Client.loginOAuth(clientId, clientSecret, options.scope, profileName);
    const expiresAt = isoTimestamp(result.expiresAt);
    printAndExit(
      {
        ok: true,
        auth_type: 'oauth',
        client_id: clientId,
        scope: result.scope ?? [],
        expires_at: expiresAt,
        message: `OAuth login successful. Token expires at ${expiresAt}`,
      },
      format,
    );
  }

  // dc3 auth logout
  auth
    .command('logout')
    .description('Log out and invalidate current token')
    .option('--format <format>', 'Output format')
    .action(async (options) => {
      const format = detectFormat(options.format);
      const profileName = await configManager.getActiveProfileName();
      const state = await tokenManager.getState(profileName);

      if (!state) {
        printAndExit({ ok: true, message: 'Already logged out' }, format);
      }

      // dc3Client.logout owns the ordering contract (spec item 5): local state
      // is cleared first, then the remote cancel is attempted, then the stored
      // password is deleted; a failed cancel warns about the surviving
      // gateway-side token before the original error propagates (exit 2/3).
      // The finally block guarantees the password is gone from the command
      // layer too, even when logout exits on an early error path.
      try {
        await dc3Client.logout(profileName);
      } finally {
        await deletePasswordFromStore(credentialIdentifier(state!));
      }

      printAndExit({ ok: true, message: 'Logged out successfully' }, format);
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
        const result: Record<string, unknown> = {};
        for (const [name, state] of Object.entries(states)) {
          const isExpired = state.expiresAt * 1000 < Date.now();
          result[name] = {
            tenant: state.tenant,
            username: state.username,
            authenticated: !isExpired,
            expires_at: isoTimestamp(state.expiresAt),
            remaining: isExpired
              ? 'expired'
              : `${Math.floor((state.expiresAt * 1000 - Date.now()) / 3600000)}h`,
          };
        }
        printAndExit(
          {
            current_profile: await configManager.getActiveProfileName(),
            profiles: result,
          },
          format,
        );
      }

      const profileName = await configManager.getActiveProfileName();
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
          expires_at: isoTimestamp(state!.expiresAt),
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
    .option('--format <format>', 'Output format: json, table, yaml')
    .action(async (options) => {
      const format = detectFormat(options.format);
      const profileName = await configManager.getActiveProfileName();
      const state = await tokenManager.getState(profileName);

      if (!state) {
        // Auth failure: propagate to the top-level handler (stderr + exit 3)
        throw new AuthError('Not logged in. Run: dc3 auth login');
      }

      if (options.header) {
        printAndExit(tokenManager.buildHeaders(state), format);
      }
      printAndExit(state.token, format);
    });
}
