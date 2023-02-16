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
    const nodeEnv = (process.env.NODE_ENV = mode || 'dev')
    const envFiles = [`./src/config/env/.env`, `./src/config/env/.env.${nodeEnv}`]

    for (const file of envFiles) {
        const envConfig = dotenv.parse(fs.readFileSync(file))
        for (const k in envConfig) {
            process.env[k] = envConfig[k]
        }
    }

    const alias = {
        '@': resolve(__dirname, './src'),
        vue$: 'vue/dist/vue.runtime.esm-bundler.js',
    }

    return defineConfig({
        base: './',
        root: './',
        resolve: {
            alias,
        },
        server: {
            port: process.env.APP_CLI_PORT,
            proxy: {
                [process.env.APP_API_PREFIX]: {
                    target: `${process.env.APP_API_PATH}:${process.env.APP_API_PORT}`,
                    changeOrigin: true,
                    ws: true,
                    rewrite: (path) =>
                        path.replace(new RegExp('^' + process.env.APP_API_PREFIX), process.env.APP_API_PREFIX),
                },
            },
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
