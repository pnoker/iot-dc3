// Child fixture running the REAL CLI entry through Node type stripping, for
// process-level guards: real exit codes, real stderr/stdout channels, real
// state files under the redirected HOME.
//
// DC3_ENTRY selects what "the CLI" is for this child: default is the src
// entry (type stripping + .js→.ts resolve hook), so tests run without a
// build. Point it at the built artifact ('../../dist/index.js') to re-run
// the same process-level guards against what actually ships.
/* global process */
const entry = process.env.DC3_ENTRY || '../../src/index.ts';
const { buildProgram, handleProgramFailure } = await import(entry);

const program = buildProgram();
try {
  await program.parseAsync(['node', 'dc3', ...process.argv.slice(2)]);
} catch (error) {
  handleProgramFailure(error);
}
