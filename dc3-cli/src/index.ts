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
import { handleFatalError } from './core/errors.js';
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

const program = new Command();

program
  .name('dc3')
  .description('IoT DC3 Platform CLI — AI-ready command-line interface')
  .version('0.1.0')
  .option('--profile <name>', 'Use a specific profile for this invocation (before the subcommand)')
  .option('--format <format>', 'Global output format: json, table, yaml (before the subcommand)');

// Apply the global options once per invocation, before any command action runs:
// --profile installs a validated per-invocation profile override, --format seeds
// the format-resolution context (the command's own --format still wins).
program.hook('preAction', async () => {
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

// Parse and map fatal errors to the contract exit codes (auth 3, network 2, business 1).
// SilentExit is the control-flow signal from printAndExit — the exit code is already
// set on process.exitCode, so just terminate without printing an error.
program.parseAsync(process.argv).catch((error: unknown) => {
  if (error instanceof SilentExit) return;
  try {
    handleFatalError(error instanceof Error ? error : new Error(String(error)));
  } catch {
    // handleFatalError printed the message and set process.exitCode already;
    // swallow the re-thrown error so the process unwinds cleanly.
  }
});
