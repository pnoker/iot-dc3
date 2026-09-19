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
import { createInterface } from 'node:readline';
import { stdin, stdout } from 'node:process';

/**
 * Interactively prompt the user for input.
 * @param question - prompt text shown to the user
 * @returns the collected user input
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
 * @param question - prompt text shown to the user
 * @returns the collected user input
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
 * @param question - prompt text shown to the user
 * @returns the collected user input
 */
export async function confirm(question: string): Promise<boolean> {
  const answer = await prompt(`${question} [y/N] `);
  return answer.toLowerCase() === 'y' || answer.toLowerCase() === 'yes';
}
