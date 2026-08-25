/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Axiom A5 guardrail: every @media width breakpoint must reference the
// $breakpoint-* token from src/styles/tokens.scss — the single breakpoint
// contract of docs/design/frontend-three-terminal-ux.md. Hand-rolled px
// thresholds reintroduce desktop-first drift one patch at a time.

import {readdirSync, readFileSync, statSync} from 'node:fs';
import {join, relative} from 'node:path';

import {describe, expect, it} from 'vitest';

const root = process.cwd();
const srcDir = join(root, 'src');

// Media features that are width thresholds (allowed: prefers-*,
// prefers-reduced-motion, print, and any non-width query).
const WIDTH_MEDIA = /@media[^{]*\((?:max|min)-width\s*:\s*([^)]+)\)/g;

function walk(dir: string): string[] {
    return readdirSync(dir).flatMap((entry) => {
        const path = join(dir, entry);
        if (statSync(path).isDirectory()) return walk(path);
        return /\.(vue|scss)$/.test(path) ? [path] : [];
    });
}

function findViolations(): string[] {
    const violations: string[] = [];
    for (const path of walk(srcDir)) {
        const content = readFileSync(path, 'utf8');
        for (const match of content.matchAll(WIDTH_MEDIA)) {
            const value = match[1].trim();
            if (!value.startsWith('$breakpoint-')) {
                const line = content.slice(0, match.index).split('\n').length;
                violations.push(`${relative(root, path)}:${line} — @media width "${value}" is not a $breakpoint-* token`);
            }
        }
    }
    return violations;
}

describe('three-terminal breakpoint contract (A5)', () => {
    it('all @media width breakpoints use $breakpoint-* tokens', () => {
        const violations = findViolations();
        expect(violations, violations.join('\n')).toEqual([]);
    });
});
