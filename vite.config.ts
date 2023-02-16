/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import legacy from '@vitejs/plugin-legacy'
import vue from '@vitejs/plugin-vue'
import * as dotenv from 'dotenv'
import * as fs from 'fs'
import { resolve } from 'path'
import { defineConfig } from 'vite'

export default ({ mode }) => {
    const NODE_ENV = (process.env.NODE_ENV = mode || 'dev')

    for (const file of [`./src/config/env/.env`, `./src/config/env/.env.${NODE_ENV}`]) {
        const envConfig = dotenv.parse(fs.readFileSync(file))
        for (const k in envConfig) {
            process.env[k] = envConfig[k]
        }
    }

    const alias = {
        '@': resolve(__dirname, './src'),
        vue$: 'vue/dist/vue.runtime.esm-bundler.js',
    }
    const proxy = {
        [process.env.APP_API_PREFIX as string]: {
            ws: true,
            changeOrigin: true,
            target: `${process.env.APP_API_PATH}:${process.env.APP_API_PORT}`,
            rewrite: (path) =>
                path.replace(new RegExp('^' + process.env.APP_API_PREFIX), process.env.APP_API_PREFIX as string),
        },
    }

    return defineConfig({
        base: './',
        root: './',
        envDir: './src/config/env',
        envPrefix: 'APP_',
        resolve: {
            alias,
        },
        server: {
            port: Number(process.env.APP_CLI_PORT),
            proxy,
        },
        plugins: [
            vue(),
            legacy({
                targets: [
                    'Android > 39',
                    'Chrome >= 60',
                    'Safari >= 10.1',
                    'iOS >= 10.3',
                    'Firefox >= 54',
                    'Edge >= 15',
                ],
            }),
        ],
    })
}
