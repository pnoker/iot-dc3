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
import {resolve} from 'path';
import AutoImport from 'unplugin-auto-import/vite';
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers';
import Components from 'unplugin-vue-components/vite';
import {type ConfigEnv, defineConfig, loadEnv} from 'vite';

export default (configEnv: ConfigEnv) => {
  const env = loadEnv(configEnv.mode, './src/config/env', '');
  const envDir = './src/config/env';

  const alias: Record<string, string> = {
    '@': resolve(__dirname, './src'),
    vue$: 'vue/dist/vue.runtime.esm-bundler.js',
  };
  const apiPrefix = env.APP_API_PREFIX as string;
  const proxy = {
    [apiPrefix]: {
      ws: false,
      changeOrigin: true,
      target: `${env.APP_API_PATH}:${env.APP_API_PORT}`,
    },
  };
  const output = {
    entryFileNames: `assets/dc3.[name].[hash].js`,
    chunkFileNames: `assets/dc3.[name].[hash].js`,
    assetFileNames: `assets/dc3.[name].[hash].[ext]`,
    manualChunks: (id: string) => {
      if (id.includes('node_modules')) {
        // Merge vue and element-plus to avoid circular dependency
        if (id.includes('vue') || id.includes('pinia') || id.includes('element-plus') || id.includes('@element-plus')) {
          return 'vue-vendor';
        }
        if (id.includes('vue-router')) {
          return 'router-vendor';
        }
        if (id.includes('@antv+g2@') || id.includes('/node_modules/@antv/g2/')) {
          return 'g2-vendor';
        }
        if (id.includes('@antv+g6@') || id.includes('/node_modules/@antv/g6/')) {
          return 'g6-vendor';
        }
        if (id.includes('@antv+g@') || id.includes('/node_modules/@antv/g/')) {
          return 'antv-g';
        }
        if (id.includes('@antv+component@') || id.includes('/node_modules/@antv/component/')) {
          return 'antv-component';
        }
        if (id.includes('@antv+g-canvas@') || id.includes('/node_modules/@antv/g-canvas/')) {
          return 'antv-g-canvas';
        }
        if (id.includes('@antv+vendor@') || id.includes('/node_modules/@antv/vendor/')) {
          return 'antv-vendor-core';
        }
        if (id.includes('@antv+layout@') || id.includes('/node_modules/@antv/layout/')) {
          return 'antv-layout';
        }
        if (id.includes('@antv')) {
          return 'antv-vendor';
        }
        return 'vendor';
      }
      return undefined;
    },
  };

  return defineConfig({
    base: './',
    root: './',
    envDir,
    envPrefix: 'APP_',
    resolve: {
      alias,
    },
    clearScreen: false,
    server: {
      host: '0.0.0.0',
      port: Number(env.APP_CLI_PORT) || 8080,
      proxy,
      watch: {
        ignored: ['**/dist/**', '**/playwright-report/**', '**/test-results/**'],
      },
      open: false,
    },
    preview: {
      host: '0.0.0.0',
      port: Number(env.APP_CLI_PORT) || 8080,
      proxy,
    },
    build: {
      outDir: 'dist',
      chunkSizeWarningLimit: 1500,
      // minify defaults to rolldown in Vite 8
      sourcemap: configEnv.mode === 'production' ? false : true,
      reportCompressedSize: false,
      cssCodeSplit: true,
      rollupOptions: {output},
    },
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        imports: [
          'vue',
          'vue-router',
          'pinia',
          {
            'vue-router': ['RouteRecordRaw', 'NavigationGuardNext', 'RouteLocationNormalized', 'RouteMeta'],
            axios: ['AxiosInstance'],
            'element-plus': ['FormInstance', 'FormRules'],
          },
        ],
        dts: 'src/config/ambient/auto-imports.d.ts',
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/config/ambient/components.d.ts',
      }),
    ],
    optimizeDeps: {
      entries: ['index.html'],
      include: ['vue', 'vue-router', 'element-plus', '@element-plus/icons-vue', '@antv/g2'],
      exclude: ['@vitejs/plugin-vue'],
    },
    css: {
      preprocessorOptions: {
        scss: {
          charset: false,
          additionalData: (content, filename) => {
            // Avoid circular import by not adding additionalData to the element-variables.scss file itself
            if (filename.includes('element-variables.scss')) {
              return content;
            }
            return `@use "@/config/plugins/element/element-variables.scss" as *;${content}`;
          },
        },
      },
      modules: {
        localsConvention: 'camelCase',
      },
    },
  });
};
