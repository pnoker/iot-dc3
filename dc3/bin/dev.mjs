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

import { createInterface } from 'node:readline';
import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { spawn } from 'node:child_process';
import { resolve } from 'node:path';

const repositoryRoot = resolve(import.meta.dirname, '..', '..');
const environmentFile = process.env.DC3_ENV_FILE ?? 'dc3/env/dev.env';

const services = {
  auth: { module: 'dc3-center/dc3-center-auth', artifact: 'dc3-center-auth' },
  gateway: { module: 'dc3-gateway', artifact: 'dc3-gateway' },
  data: { module: 'dc3-center/dc3-center-data', artifact: 'dc3-center-data' },
  manager: { module: 'dc3-center/dc3-center-manager', artifact: 'dc3-center-manager' },
  agentic: { module: 'dc3-center/dc3-center-agentic', artifact: 'dc3-center-agentic' },
};

const serviceOrder = ['auth', 'gateway', 'data', 'manager', 'agentic'];

function printUsage() {
  process.stderr.write(
    `Usage: make dev | make dev-<service>\n\nServices: ${serviceOrder.join(', ')}\nEnvironment: DC3_ENV_FILE=path/to/env make dev-auth\n`,
  );
}

function parseEnvironmentFile(filePath) {
  const absolutePath = resolve(repositoryRoot, filePath);
  if (!existsSync(absolutePath)) {
    throw new Error(`Environment file not found: ${filePath}`);
  }

  const environment = {};
  const lines = readFileSync(absolutePath, 'utf8').split(/\r?\n/);
  for (const [index, line] of lines.entries()) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }

    const assignment = trimmed.match(/^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)=(.*)$/);
    if (!assignment) {
      throw new Error(`Invalid environment assignment at ${filePath}:${index + 1}`);
    }

    let value = assignment[2].trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    environment[assignment[1]] = value;
  }
  return environment;
}

function prefixOutput(stream, service, childStream) {
  const reader = createInterface({ input: childStream });
  reader.on('line', (line) => {
    stream.write(`[${service}] ${line}\n`);
  });
}

function stopChildren(children, signal) {
  for (const child of children.values()) {
    if (child.exitCode === null && !child.killed) {
      child.kill(signal);
    }
  }
}

function findBootJar(service) {
  const targetDirectory = resolve(repositoryRoot, services[service].module, 'target');
  const artifact = services[service].artifact;
  const jar = readdirSync(targetDirectory)
    .filter(
      (file) =>
        (file === `${artifact}.jar` || file.startsWith(`${artifact}-`)) &&
        file.endsWith('.jar') &&
        !file.startsWith('original-'),
    )
    .sort((left, right) =>
      left === `${artifact}.jar` ? -1 : right === `${artifact}.jar` ? 1 : left.localeCompare(right),
    )[0];
  if (!jar) {
    throw new Error(`Spring Boot jar not found for ${service}: ${targetDirectory}`);
  }
  return resolve(targetDirectory, jar);
}

function buildServices(selectedServices, environment) {
  const maven = process.env.MAVEN_CMD ?? (process.platform === 'win32' ? 'mvn.cmd' : 'mvn');
  const modules = selectedServices.map((service) => services[service].module).join(',');
  return new Promise((resolveBuild, rejectBuild) => {
    const build = spawn(
      maven,
      [
        '-s',
        resolve(repositoryRoot, '.mvn/settings.xml'),
        '-pl',
        modules,
        '-am',
        'package',
        '-DskipTests',
      ],
      {
        cwd: repositoryRoot,
        env: environment,
        stdio: ['inherit', 'pipe', 'pipe'],
      },
    );
    prefixOutput(process.stdout, 'build', build.stdout);
    prefixOutput(process.stderr, 'build', build.stderr);
    build.on('error', rejectBuild);
    build.on('close', (code) => {
      if (code === 0) {
        resolveBuild();
      } else {
        rejectBuild(new Error(`Maven package failed with exit code ${code ?? 'unknown'}`));
      }
    });
  });
}

async function runServices(selectedServices) {
  const environment = {
    ...parseEnvironmentFile(environmentFile),
    ...process.env,
  };
  const children = new Map();
  const results = new Map();
  let stopping = false;
  let interrupted = false;
  let failureCode = null;

  const stop = (signal) => {
    if (stopping) {
      return;
    }
    stopping = true;
    stopChildren(children, signal);
  };

  await buildServices(selectedServices, environment);

  const completion = new Promise((resolveCompletion) => {
    for (const service of selectedServices) {
      const java = process.env.JAVA_CMD ?? (process.platform === 'win32' ? 'java.exe' : 'java');
      const child = spawn(java, ['-jar', findBootJar(service)], {
        cwd: repositoryRoot,
        env: environment,
        stdio: ['inherit', 'pipe', 'pipe'],
      });
      children.set(service, child);
      prefixOutput(process.stdout, service, child.stdout);
      prefixOutput(process.stderr, service, child.stderr);
      child.on('error', (error) => {
        process.stderr.write(`[${service}] ${error.message}\n`);
      });
      child.on('close', (code, signal) => {
        results.set(service, { code, signal });
        if (!stopping) {
          if (code !== 0) {
            failureCode = code !== null && code > 0 ? code : 1;
          }
          stop(signal ?? 'SIGTERM');
        }
        if (results.size === selectedServices.length) {
          resolveCompletion();
        }
      });
    }
  });

  const handleSignal = (signal) => {
    interrupted = true;
    stop(signal);
  };
  process.once('SIGINT', () => handleSignal('SIGINT'));
  process.once('SIGTERM', () => handleSignal('SIGTERM'));
  await completion;

  if (interrupted) {
    return 130;
  }
  return failureCode ?? 0;
}

async function main() {
  const argumentsList = process.argv.slice(2);
  const requestedService = argumentsList[0] ?? 'all';
  if (argumentsList.includes('--help') || argumentsList.includes('-h')) {
    printUsage();
    return 0;
  }

  const selectedServices = requestedService === 'all' ? serviceOrder : [requestedService];
  if (selectedServices.some((service) => !services[service])) {
    printUsage();
    return 1;
  }

  process.stdout.write(`Starting ${selectedServices.join(', ')} using ${environmentFile}\n`);
  return runServices(selectedServices);
}

main()
  .then((exitCode) => {
    process.exitCode = exitCode;
  })
  .catch((error) => {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  });
