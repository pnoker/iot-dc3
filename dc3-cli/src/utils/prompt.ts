import { createInterface } from 'node:readline';
import { stdin, stdout } from 'node:process';

/**
 * Interactively prompt the user for input.
 */
export async function prompt(question: string): Promise<string> {
  const rl = createInterface({ input: stdin, output: stdout });
  return new Promise((resolve) => {
    rl.question(question, (answer) => {
      rl.close();
      resolve(answer.trim());
    });
  });
}

/**
 * Interactively prompt for a password (input is hidden via stdout mutation).
 */
export async function passwordPrompt(question: string): Promise<string> {
  const rl = createInterface({ input: stdin, output: stdout });
  return new Promise((resolve) => {
    // Hide the typed characters by muting stdout
    const originalWrite = stdout.write.bind(stdout);
    (stdout as typeof stdout & { _write?: unknown })._write = originalWrite;
    stdout.write = ((_data: string | Uint8Array) => true) as typeof stdout.write;

    rl.question(question, (answer) => {
      stdout.write = originalWrite;
      rl.close();
      resolve(answer.trim());
    });
  });
}

/**
 * Confirm a yes/no action.
 */
export async function confirm(question: string): Promise<boolean> {
  const answer = await prompt(`${question} [y/N] `);
  return answer.toLowerCase() === 'y' || answer.toLowerCase() === 'yes';
}
