// Child fixture running the REAL CLI entry (src/index.ts) through Node type
// stripping, for process-level guards: real exit codes, real stderr/stdout
// channels, real state files under the redirected HOME.
import { buildProgram, handleProgramFailure } from '../../src/index.ts';

const program = buildProgram();
try {
  await program.parseAsync(['node', 'dc3', ...process.argv.slice(2)]);
} catch (error) {
  handleProgramFailure(error);
}
