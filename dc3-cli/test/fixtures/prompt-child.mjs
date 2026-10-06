// Child fixture driving the REAL prompt module (src/utils/prompt.ts) through
// Node type stripping, so channel and termination contracts are verified at
// process level (stdout cleanliness, EOF exit codes, no hangs).
import { prompt, passwordPrompt, confirm } from '../../src/utils/prompt.ts';

const mode = process.argv[2] ?? 'password';
try {
  if (mode === 'password') {
    const pw = await passwordPrompt('Password: ');
    // The defect left writes buffered forever (writableLength stuck, bytes
    // never reaching the fd). Drainage + these very writes landing in the
    // parent prove stdout is intact. (An own `_write` is NOT a defect signal:
    // a piped stdout is a Socket that legitimately owns one.)
    const intact = process.stdout.writableLength === 0 && !process.stdout.destroyed;
    process.stdout.write(`password=${pw}\n`);
    process.stdout.write(`stdout-intact=${intact}\n`);
  } else if (mode === 'prompt') {
    const answer = await prompt('Tenant: ');
    process.stdout.write(`answer=${answer}\n`);
  } else if (mode === 'confirm') {
    const ok = await confirm('Proceed?');
    process.stdout.write(`confirm=${ok}\n`);
  }
  process.exitCode = 0;
} catch (error) {
  process.stdout.write(`error=${error?.name ?? 'Error'}:${error?.exitCode ?? ''}:${error?.message}\n`);
  process.exitCode = error?.exitCode ?? 1;
}
