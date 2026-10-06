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
import { Command, CommanderError } from 'commander';
import { realpathSync } from 'node:fs';
import { pathToFileURL } from 'node:url';
import { handleFatalError, UsageError } from './core/errors.js';
import { SilentExit } from './utils/format.js';
import { applyGlobalOptions } from './core/context.js';
import { registerConfigCommand } from './commands/config.js';
import { registerAuthCommand } from './commands/auth.js';
import { registerDeviceCommand } from './commands/device.js';
import { registerDriverCommand } from './commands/driver.js';
import { registerPointCommand } from './commands/point.js';
import { registerProfileCommand } from './commands/profile.js';
import { registerEventCommand } from './commands/event.js';
import { registerCommandCommand } from './commands/command.js';
import { registerAlertCommand } from './commands/alert.js';
import { registerDashboardCommand } from './commands/dashboard.js';
import { registerTopicCommand } from './commands/topic.js';
import { registerChatCommand } from './commands/chat.js';
import { registerToolCommand } from './commands/tool.js';
import { registerAnalyticsCommand } from './commands/analytics.js';
import { registerSessionCommand, registerActionCommand } from './commands/session.js';
import { registerGroupCommand } from './commands/group.js';
import { registerLabelCommand } from './commands/label.js';
import { registerProviderCommand } from './commands/provider.js';
import { registerModelCommand } from './commands/model.js';
import { registerAttachmentCommand } from './commands/attachment.js';

/** Alias invocations already warned about during one program run (one warning each). */
type AliasWarningLog = Set<string>;

/**
 * Full invocation path of a command, e.g. `dc3 device add`.
 * @param cmd - command to name
 * @returns space-joined path from the program root
 */
function commandPath(cmd: Command): string {
  const names: string[] = [];
  for (let current: Command | null = cmd; current; current = current.parent) {
    names.unshift(current.name());
  }
  return names.join(' ');
}

/**
 * Warn (once per invocation) when a command was reached through a compat
 * alias instead of its canonical name, e.g. `device create` instead of
 * `device add` (report F029). The dispatched segment is read from the parent
 * command's parsed operands: commander records on every parent the name it
 * used to dispatch one level down, which distinguishes `add` from `create`.
 * @param actionCommand - the command whose action is about to run
 * @param warned - per-invocation memory of already-emitted warnings
 */
function warnDeprecatedAliasInvocation(actionCommand: Command, warned: AliasWarningLog): void {
  const usedName = actionCommand.parent?.args?.[0];
  if (typeof usedName !== 'string') return;
  if (usedName === actionCommand.name() || !actionCommand.aliases().includes(usedName)) return;
  const canonical = commandPath(actionCommand);
  if (warned.has(canonical)) return;
  warned.add(canonical);
  process.stderr.write(
    `warning: '${usedName}' is a deprecated compat alias of '${actionCommand.name()}' and will be removed in an upcoming release; use '${canonical}'\n`,
  );
}

/**
 * Render a subcommand term for the help list, marking aliases as deprecated
 * compat aliases instead of presenting them as first-class alternates
 * (`add (deprecated alias: create)` rather than `add|create`).
 * @param cmd - subcommand being listed
 * @returns the term shown in the Commands section
 */
function deprecatedAliasSubcommandTerm(cmd: Command): string {
  const args = cmd.registeredArguments
    .map((arg) => `${arg.required ? '<' : '['}${arg.name()}${arg.variadic ? '...' : ''}${arg.required ? '>' : ']'}`)
    .join(' ');
  const alias = cmd.aliases()[0];
  const name = alias ? `${cmd.name()} (deprecated alias: ${alias})` : cmd.name();
  return `${name}${cmd.options.length ? ' [options]' : ''}${args ? ` ${args}` : ''}`;
}

/**
 * Register a path-aware `help [commands...]` command: every segment is
 * resolved against the target's children by name or alias, an unknown segment
 * is a single-line usage error (no help dump), and the resolved command's
 * help goes to STDOUT with exit 0 (reports F014 and F056).
 * @param target - command whose children the path resolves through
 */
function registerPathAwareHelpCommand(target: Command): void {
  target
    .command('help [commands...]')
    .description('Display help for a command path')
    .action((commands: string[]) => {
      let node: Command = target;
      for (const segment of commands ?? []) {
        const next = node.commands.find(
          (child) => child.name() === segment || child.aliases().includes(segment),
        );
        if (!next) {
          throw new UsageError(`unknown command '${segment}'`);
        }
        node = next;
      }
      process.exitCode = 0;
      node.outputHelp();
      throw new SilentExit();
    });
}

/**
 * Register the path-aware help command on the program and on every group that
 * has children, replacing commander's implicit single-segment help command.
 * @param cmd - command tree to cover
 */
function registerPathAwareHelpCommands(cmd: Command): void {
  if (cmd.commands.length > 0 && !cmd.commands.some((child) => child.name() === 'help')) {
    registerPathAwareHelpCommand(cmd);
  }
  for (const child of cmd.commands) {
    registerPathAwareHelpCommands(child);
  }
}

/**
 * Reject excess positional arguments on every subcommand (report F053). The
 * program root stays lenient on purpose: with an action handler registered,
 * commander would otherwise report stray root operands as "too many
 * arguments" before the action can classify them as unknown commands.
 * @param cmd - command tree whose subcommands become strict
 */
function applyStrictExcessArguments(cmd: Command): void {
  for (const child of cmd.commands) {
    child.allowExcessArguments(false);
    applyStrictExcessArguments(child);
  }
}

/**
 * Build the fully configured CLI program: root options parse only before the
 * first subcommand (README contract, report F001), excess positionals are
 * rejected (report F053), commander's own error line is silenced in favour of
 * the single chokepoint line, and every escaped failure is bridged through the
 * error taxonomy (report F015).
 *
 * Exported as the test seam for entry-layer behavior; the bin entry runs it.
 * @returns the configured commander program
 */
export function buildProgram(): Command {
  const program = new Command();
  const deprecatedAliasWarnings: AliasWarningLog = new Set();

  program
    .name('dc3')
    .description('IoT DC3 Platform CLI — AI-ready command-line interface')
    .version('0.1.0')
    // Root options only parse before the first subcommand: leaf commands own
    // their options afterwards (e.g. `device update <id> --version <n>`).
    .enablePositionalOptions()
    // Bridge commander exits into thrown errors so every failure flows
    // through handleFatalError (single stderr line + stdout envelope).
    .exitOverride()
    // commander.error() writes its own "error: ..." line; the chokepoint
    // prints the single human line instead. Help output is unaffected.
    .configureOutput({ outputError: () => undefined })
    .configureHelp({ subcommandTerm: deprecatedAliasSubcommandTerm })
    .option('--profile <name>', 'Use a specific profile for this invocation (before the subcommand)')
    .option('--format <format>', 'Global output format: json, table, yaml (before the subcommand)');

  // Apply the global options once per invocation, before any command action
  // runs: --profile installs a validated per-invocation profile override,
  // --format seeds the format-resolution context (the command's own --format
  // still wins). Also surface one-time deprecation warnings for compat alias
  // invocations (create -> add).
  program.hook('preAction', async (_thisCommand, actionCommand) => {
    warnDeprecatedAliasInvocation(actionCommand, deprecatedAliasWarnings);
    await applyGlobalOptions(program.opts() as { profile?: string; format?: string });
  });

  // Register all commands
  registerConfigCommand(program);
  registerAuthCommand(program);
  registerDeviceCommand(program);
  registerDriverCommand(program);
  registerPointCommand(program);
  registerProfileCommand(program);
  registerGroupCommand(program);
  registerLabelCommand(program);
  registerEventCommand(program);
  registerCommandCommand(program);
  registerAlertCommand(program);
  registerDashboardCommand(program);
  registerTopicCommand(program);
  registerChatCommand(program);
  registerToolCommand(program);
  registerAnalyticsCommand(program);
  registerSessionCommand(program);
  registerActionCommand(program);
  registerProviderCommand(program);
  registerModelCommand(program);
  registerAttachmentCommand(program);

  registerPathAwareHelpCommands(program);
  applyStrictExcessArguments(program);

  // Bare `dc3` prints help to STDOUT and exits 0 (report F048). With an
  // action handler registered, commander no longer treats unknown operands as
  // "missing subcommand" — reject them explicitly to keep the single-line
  // unknown-command diagnostic.
  program.action((_opts: Record<string, unknown>, command: Command) => {
    const operands = command.args;
    if (operands.length > 0) {
      throw new UsageError(`unknown command '${operands[0]}'`);
    }
    program.outputHelp();
    process.exitCode = 0;
  });

  return program;
}

/**
 * Map a program failure to the reporting chokepoint. SilentExit is the
 * control-flow signal from printAndExit (exit code already set). Commander
 * errors that already emitted help/version output pass their exit code
 * through silently; everything else is reported through handleFatalError.
 * @param error - rejection reason of parseAsync
 */
export function handleProgramFailure(error: unknown): void {
  if (error instanceof SilentExit) return;
  if (error instanceof CommanderError) {
    // Help/version output was already written (stdout for --help/-V, stderr
    // for the bare-group help dump); do not report it a second time.
    if (error.code === 'commander.help' || error.exitCode === 0) {
      process.exitCode = error.exitCode;
      return;
    }
  }
  try {
    handleFatalError(error instanceof Error ? error : new Error(String(error)));
  } catch {
    // handleFatalError printed the message and set process.exitCode already;
    // swallow the re-thrown error so the process unwinds cleanly.
  }
}

/**
 * Parse the real argv and map fatal errors to the contract exit codes
 * (auth 3, network 2, business/usage 1).
 * @param program - the configured program to run
 */
export function runCli(program: Command): void {
  program.parseAsync(process.argv).catch((error: unknown) => {
    handleProgramFailure(error);
  });
}

/**
 * True when this module is the process entry point (bin: dc3).
 * @returns whether index.ts was executed directly
 */
function isMainModule(): boolean {
  try {
    return (
      process.argv[1] !== undefined &&
      import.meta.url === pathToFileURL(realpathSync(process.argv[1])).href
    );
  } catch {
    return false;
  }
}

if (isMainModule()) {
  runCli(buildProgram());
}
