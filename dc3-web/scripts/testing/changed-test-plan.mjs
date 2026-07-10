#!/usr/bin/env node

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

import {execFileSync} from 'node:child_process';

function git(args) {
  return execFileSync('git', args, {encoding: 'utf8'})
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean);
}

function changedFilesFromGit() {
  const base = process.env.TEST_IMPACT_BASE || process.argv.find((arg) => arg.startsWith('--base='))?.slice(7);
  const files = new Set();

  if (base) {
    try {
      for (const file of git(['diff', '--name-only', '--diff-filter=ACMR', `${base}...HEAD`])) {
        files.add(file);
      }
      return [...files].sort();
    } catch {
      console.warn(`Could not compare against ${base}; falling back to working tree changes.`);
    }
  }

  for (const args of [
    ['diff', '--name-only', '--diff-filter=ACMR', 'HEAD'],
    ['diff', '--cached', '--name-only', '--diff-filter=ACMR'],
    ['ls-files', '--others', '--exclude-standard'],
  ]) {
    for (const file of git(args)) {
      files.add(file);
    }
  }

  return [...files].sort();
}

const rules = [
  {
    name: 'API wrappers',
    source: [/^src\/api\//],
    tests: [/^tests\/api\//],
    commands: ['pnpm run test:api'],
    reason: 'API URL, params, body, upload, and transport contracts must stay explicit.',
  },
  {
    name: 'shared units',
    source: [/^src\/utils\//, /^src\/config\/axios\//, /^src\/store\//, /^src\/composables\//],
    tests: [/^tests\/unit\//],
    commands: ['pnpm run test:unit'],
    reason: 'Shared state and helpers need deterministic unit coverage.',
  },
  {
    name: 'components',
    source: [/^src\/components\//],
    tests: [/^tests\/component\//],
    commands: ['pnpm run test:component'],
    reason: 'Reusable UI contracts should cover props, emits, slots, and important states.',
  },
  {
    name: 'routes and pages',
    source: [/^src\/views\//, /^src\/config\/router\//],
    tests: [/^tests\/e2e\//, /^tests\/component\//],
    commands: ['pnpm run test:e2e'],
    reason: 'Route, menu, permission, and page workflow changes need browser-level smoke coverage.',
  },
  {
    name: 'test infrastructure',
    source: [
      /^package\.json$/,
      /^pnpm-lock\.yaml$/,
      /^vite\.config\.ts$/,
      /^vitest\.config\.ts$/,
      /^playwright\.config\.ts$/,
      /^eslint\.config\.mjs$/,
      /^\.github\/workflows\//,
    ],
    tests: [/^tests\/guardrails\//, /^tests\/README\.md$/, /^docs\/frontend-testing-guardrails\.md$/],
    commands: ['pnpm run lint:check', 'pnpm run check', 'pnpm run test:guard', 'pnpm run test:ci', 'pnpm run build'],
    reason: 'Tooling changes can break every layer and need the full local quality gate.',
  },
];

const changedFiles = changedFilesFromGit();

if (!changedFiles.length) {
  console.log('No changed files detected.');
  process.exit(0);
}

const required = new Map();
const missingTests = [];

for (const rule of rules) {
  const sourceChanged = changedFiles.some((file) => rule.source.some((pattern) => pattern.test(file)));
  if (!sourceChanged) continue;

  for (const command of rule.commands) {
    required.set(command, rule.reason);
  }

  const testChanged = changedFiles.some((file) => rule.tests.some((pattern) => pattern.test(file)));
  if (!testChanged) {
    missingTests.push(rule);
  }
}

console.log('Changed files:');
for (const file of changedFiles) {
  console.log(`- ${file}`);
}

if (required.size) {
  console.log('\nRecommended checks:');
  for (const [command, reason] of required) {
    console.log(`- ${command}`);
    console.log(`  ${reason}`);
  }
} else {
  console.log('\nNo source-impacting frontend test rule matched.');
}

if (missingTests.length) {
  console.log('\nPotential missing test updates:');
  for (const rule of missingTests) {
    console.log(`- ${rule.name}: ${rule.reason}`);
  }
}

if (process.env.TEST_IMPACT_ENFORCE === '1' && missingTests.length) {
  console.error('\nTEST_IMPACT_ENFORCE=1 blocks source changes without matching test/doc updates.');
  process.exit(1);
}
