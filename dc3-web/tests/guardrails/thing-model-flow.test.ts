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

import {readFileSync} from 'node:fs';
import {join} from 'node:path';

import {describe, expect, it} from 'vitest';

const root = process.cwd();

function readProjectFile(path: string) {
  return readFileSync(join(root, path), 'utf8');
}

// Match a named import in either flavor:
//   import { X } from '@/api/foo';
//   import { X, Y } from '@/api/foo';
// Word boundaries on both sides ensure `listPointByDeviceId` doesn't match
// `listPointInfoByDeviceId` (substring check would).
function importsName(source: string, name: string, fromPath: string): boolean {
  const pattern = new RegExp(
    String.raw`import\s+(?:type\s+)?\{[^}]*\b${name}\b[^}]*\}\s+from\s+['"]${fromPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"]`
  );
  return pattern.test(source);
}

// Match a `prop="X"` attribute on an `<el-form-item>`. This is what the
// runtime form-validation system actually keys off — far more meaningful
// than substring-matching a v-model path that any rename could dodge.
function declaresFormItemProp(source: string, prop: string): boolean {
  const pattern = new RegExp(String.raw`<el-form-item\b[^>]*\bprop=["']${prop}["']`);
  return pattern.test(source);
}

describe('thing model profile/device flow', () => {
  it('refreshes point/command/event definitions from the selected profile during device editing', () => {
    const source = readProjectFile('src/views/device/edit/index.ts');

    // Profile change must drive list refresh from the *profile* side.
    expect(importsName(source, 'listPointByProfileId', '@/api/point')).toBe(true);
    expect(importsName(source, 'listCommandByProfileId', '@/api/command')).toBe(true);
    expect(importsName(source, 'listEventByProfileId', '@/api/event')).toBe(true);

    // changeProfile() must fan out to all three siblings — losing any one
    // leaves stale matrix data after the user picks a different profile.
    expect(source).toMatch(/const changeProfile = \(\) => \{\s*pointInfo\(\);\s*commandInfo\(\);\s*eventInfo\(\);/);
  });

  it('keeps thing model entity codes server-generated rather than user-input form fields', () => {
    // *Code fields are derived from *Name on the backend. If a form ever
    // re-introduces a `prop="*Code"` el-form-item, two things break:
    //   (1) the user can collide with the generator
    //   (2) validation rules get stale because the source of truth moved
    // Asserting on the form-item declaration catches both, and survives
    // refactors that rename the underlying reactive state.
    const cases: Array<{ file: string; props: string[] }> = [
      {file: 'src/views/profile/add/ProfileAddForm.vue', props: ['profileCode']},
      {file: 'src/views/profile/edit/ProfileEdit.vue', props: ['profileCode']},
      {file: 'src/views/device/add/DeviceAddForm.vue', props: ['deviceCode']},
      {file: 'src/views/device/edit/DeviceEdit.vue', props: ['deviceCode']},
      {file: 'src/views/point/add/PointEditForm.vue', props: ['pointCode']},
      {file: 'src/views/point/value/edit/PointValueEditForm.vue', props: ['pointCode']},
      {file: 'src/views/settings/command/edit/CommandEditForm.vue', props: ['commandCode']},
      {file: 'src/views/settings/event/definition/edit/EventEditForm.vue', props: ['eventCode']},
    ];

    const offenders: string[] = [];
    for (const {file, props} of cases) {
      const source = readProjectFile(file);
      for (const prop of props) {
        if (declaresFormItemProp(source, prop)) {
          offenders.push(`${file} declares <el-form-item prop="${prop}">`);
        }
      }
    }

    expect(offenders).toEqual([]);
  });
});
