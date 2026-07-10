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

import vue from '@vitejs/plugin-vue';
import {resolve} from 'node:path';
import {defineConfig} from 'vitest/config';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
      vue$: 'vue/dist/vue.runtime.esm-bundler.js',
    },
  },
  test: {
    environment: 'happy-dom',
    globals: false,
    setupFiles: ['./tests/setup/vitest.setup.ts'],
    include: ['tests/**/*.{test,spec}.ts'],
    exclude: ['tests/e2e/**', 'node_modules/**', 'dist/**', 'src-tauri/**'],
    restoreMocks: true,
    clearMocks: true,
    mockReset: false,
    css: false,
    // List-page view tests mount the full route subtree with 5+ mocked APIs
    // and a fresh Pinia per test; under parallel execution against the
    // ~470-test suite, transform+import can stretch beyond the default
    // 5000ms cap. 30000ms covers the slowest mounts (PointValue) without
    // masking real hangs — anything legitimately stuck still fails fast
    // because the mount path itself doesn't poll.
    testTimeout: 30000,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json-summary', 'html'],
      reportsDirectory: './coverage',
      // Coverage now reflects the **whole testable surface** of src/, not just
      // the modules that already had tests. Views are still excluded — they
      // are covered by Playwright e2e specs and would otherwise drown the
      // unit-coverage signal.
      include: [
        'src/api/**/*.ts',
        'src/composables/**/*.ts',
        'src/config/axios/**/*.ts',
        'src/config/router/**/*.ts',
        'src/store/modules/**/*.ts',
        'src/utils/**/*.ts',
        'src/components/card/tool/ToolCard.vue',
        'src/components/segmented/RangeSegmented.vue',
      ],
      exclude: [
        'src/api/dashboard/index.ts',
        // Ambient/type-only modules contribute no executable coverage signal.
        'src/config/types/**',
        'src/config/ambient/**',
      ],
      // Thresholds sit a couple of points below measured coverage so the
      // gate fails on regression, not on noise. Bump again after the next
      // round of test additions. Currently measured (post-A1 fixtures):
      // branches 67% / functions 77% / lines 81% / statements 82%.
      thresholds: {
        branches: 65,
        functions: 75,
        lines: 78,
        statements: 78,
      },
    },
  },
});
