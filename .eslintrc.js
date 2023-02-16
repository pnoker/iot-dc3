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

module.exports = {
    root: true,
    env: {
        node: true,
    },
    extends: [
        'plugin:vue/vue3-recommended',
        '@vue/typescript/recommended',
        'eslint:recommended',
        'plugin:prettier/recommended',
    ],
    plugins: ['vue', '@typescript-eslint'],
    parserOptions: {
        parser: '@typescript-eslint/parser',
        sourceType: 'module',
    },
    rules: {
        'vue/multi-word-component-names': 'off',
        '@typescript-eslint/no-explicit-any': 'off',
        '@typescript-eslint/no-empty-function': [
            'error',
            {
                allow: ['arrowFunctions'],
            },
        ],
        'brace-style': [2, '1tbs', { allowSingleLine: true }],
        'no-console': process.env.NODE_ENV === 'pro' ? 'warn' : 'off',
        'no-debugger': process.env.NODE_ENV === 'pro' ? 'warn' : 'off',
    },
}
