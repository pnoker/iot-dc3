import { Command } from 'commander';
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
import { registerGroupCommand } from './commands/group.js';
import { registerLabelCommand } from './commands/label.js';

const program = new Command();

program
  .name('dc3')
  .description('IoT DC3 Platform CLI — AI-ready command-line interface')
  .version('0.1.0')
  .option('--profile <name>', 'Use a specific profile')
  .option('--format <format>', 'Output format: json, table, yaml')
  .option('--verbose', 'Verbose output (show request/response details)')
  .option('--ci', 'CI mode: no colors, json output, strict exit codes');

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

// Parse and handle errors
program.parseAsync(process.argv).catch((err: Error) => {
  process.stderr.write(`Error: ${err.message}\n`);
  process.exit(1);
});
