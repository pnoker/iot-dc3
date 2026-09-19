import js from '@eslint/js';
import tsparser from '@typescript-eslint/parser';
import globals from 'globals';

import jsdoc from 'eslint-plugin-jsdoc';

export default [
  {
    plugins: { jsdoc },
    settings: { jsdoc: { mode: 'typescript' } },
    ignores: ['node_modules/**', 'dist/**'],
  },
  js.configs.recommended,
  {
    files: ['src/**/*.ts', 'test/**/*.ts'],
    languageOptions: {
      parser: tsparser,
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.node,
      },
    },
    rules: {
      '@typescript-eslint/no-unused-vars': 'off',
      'no-unused-vars': ['error', {argsIgnorePattern: '^_'}],
      // JSDoc gate - mirrors backend checkstyle: exported API must be documented.
      // `require.FunctionDeclaration: false` is required: the schema default
      // (true) fires on every function independently of `contexts`.
      'jsdoc/require-jsdoc': [
        'error',
        {
          require: {
            ArrowFunctionExpression: false,
            ClassDeclaration: false,
            ClassExpression: false,
            FunctionDeclaration: false,
            FunctionExpression: false,
            MethodDefinition: false
          },
          contexts: [
            'ExportNamedDeclaration > FunctionDeclaration',
            'ExportNamedDeclaration > VariableDeclaration > VariableDeclarator > ArrowFunctionExpression',
            'ExportNamedDeclaration > VariableDeclaration > VariableDeclarator > FunctionExpression',
            'ExportNamedDeclaration > TSInterfaceDeclaration',
            'ExportNamedDeclaration > TSTypeAliasDeclaration',
            'ExportNamedDeclaration > ClassDeclaration',
            'ExportDefaultDeclaration > FunctionDeclaration',
            'ExportDefaultDeclaration > ClassDeclaration'
          ],
          enableFixer: true,
          publicOnly: false
        }
      ],
      'jsdoc/require-param': 'error',
      'jsdoc/require-param-description': 'error',
      'jsdoc/require-returns': 'error',
      'jsdoc/require-returns-description': 'error',
      'jsdoc/check-param-names': 'error',
      'jsdoc/check-tag-names': 'error',
      'jsdoc/no-multi-asterisks': 'error',
      'jsdoc/empty-tags': 'error',
    },
  },
];
