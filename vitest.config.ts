/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import vue from '@vitejs/plugin-vue';
import { resolve } from 'node:path';
import { defineConfig } from 'vitest/config';

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
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json-summary', 'html'],
      reportsDirectory: './coverage',
      include: [
        'src/api/**/*.ts',
        'src/config/axios/index.ts',
        'src/utils/**/*.ts',
        'src/components/card/tool/ToolCard.vue',
        'src/components/segmented/RangeSegmented.vue',
      ],
      exclude: ['src/api/dashboard/index.ts'],
      thresholds: {
        branches: 65,
        functions: 70,
        lines: 70,
        statements: 70,
      },
    },
  },
});
