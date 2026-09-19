#!/usr/bin/env node
/**
 * Adds the project AGPL header to source files that lack it. Shared by
 * dc3-web and dc3-cli via lint-staged and the `headers` npm script;
 * the header text is generated from the root COPYRIGHT file so the
 * Java (spotless licenseHeader) and TypeScript surfaces never drift.
 *
 * Usage:
 *   node scripts/add-license-header.mjs <file>...     fix specific files
 *   node scripts/add-license-header.mjs --check <file>...  exit 1 if any lack the header
 */
import {readFileSync, writeFileSync} from 'node:fs';
import {resolve, dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const checkOnly = args[0] === '--check';
const files = checkOnly ? args.slice(1) : args;

if (!files.length) {
  console.error('usage: add-license-header.mjs [--check] <file>...');
  process.exit(2);
}

// Locate the repository root (two levels above dc3-web/scripts or dc3-cli/scripts).
// The script lives in dc3-web/scripts; the root holds COPYRIGHT.
const repoRoot = resolve(here, '..', '..');
const copyrightPath = join(repoRoot, 'COPYRIGHT');
const body = readFileSync(copyrightPath, 'utf8').trimEnd();

// Build the block comment the same way the maven antrun step does for Java:
// wrap lines with " * " and enclose in /* */.
const header = `/*
${body
  .split('\n')
  .map((line) => (line ? ` * ${line}` : ' *'))
  .join('\n')}
 */
`;

let missing = 0;
for (const file of files) {
  const src = readFileSync(file, 'utf8');
  const isVue = file.endsWith('.vue');
  const vueHeader = `<!--\n${header
    .split('\n')
    .slice(1, -1) // strip the /* and */ lines
    .map((line) => (line === ' *' ? '  -' : `  -${line.slice(1)}`))
    .join('\n')}\n-->\n`;
  const existing = isVue ? src.startsWith('<!--\n  - Copyright 2016-present') : src.startsWith('/*\n * Copyright 2016-present');
  if (existing) continue;
  const shebang = src.startsWith('#!') ? src.slice(0, src.indexOf('\n') + 1) : '';
  const rest = shebang ? src.slice(shebang.length) : src;
  if (checkOnly) {
    console.error(`missing license header: ${file}`);
    missing++;
  } else {
    writeFileSync(file, `${shebang}${isVue ? vueHeader : header}${rest.startsWith('\n') ? rest.slice(1) : rest}`);
    console.log(`header added: ${file}`);
  }
}
if (checkOnly && missing) process.exit(1);
