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

import {defineConfig, devices} from '@playwright/test';

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:8080';
const startServer = process.env.E2E_START_SERVER !== '0';
const workers = Number(process.env.E2E_WORKERS || 1);

export default defineConfig({
  testDir: './tests/e2e/specs',
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers,
  reporter: [
    ['list'],
    ['html', {open: 'never', outputFolder: 'playwright-report'}],
    ['json', {outputFile: 'test-results/e2e-results.json'}],
  ],
  use: {
    baseURL,
    headless: process.env.E2E_HEADLESS !== 'false',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
  },
  webServer: startServer
    ? {
      command: 'pnpm run serve:e2e',
      url: baseURL,
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    }
    : undefined,
  projects: [
    // Canonical desktop suite (existing).
    {
      name: 'chromium',
      use: {...devices['Desktop Chrome']},
    },
    // Three-terminal gate (docs/design/frontend-three-terminal-ux.md,
    // A2/A3): the same specs must hold on desktop, tablet, and mobile.
    // Gate specs live in tests/e2e/specs/responsive.spec.ts; the
    // overflow + shell assertions run under all three viewports.
    {
      name: 'chromium-desktop',
      use: {...devices['Desktop Chrome'], viewport: {width: 1440, height: 900}},
    },
    {
      name: 'chromium-tablet',
      // Explicitly chromium: the iPad device preset would switch to
      // webkit. Touch semantics (hasTouch) still apply to match a real
      // tablet primary pointer.
      use: {
        browserName: 'chromium',
        viewport: {width: 834, height: 1112},
        deviceScaleFactor: 2,
        hasTouch: true,
        isMobile: false,
      },
    },
    {
      name: 'chromium-mobile',
      use: {
        browserName: 'chromium',
        viewport: {width: 393, height: 851},
        deviceScaleFactor: 2.75,
        hasTouch: true,
        isMobile: true,
      },
    },
  ],
  outputDir: 'test-results/e2e-artifacts',
});
