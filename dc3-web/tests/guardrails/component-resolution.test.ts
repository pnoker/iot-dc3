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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

// Guardrail: every custom element tag used in a <template> must resolve to a
// real component — the "deleted component still referenced" regression class
// (the ExternalLink incident: ExternalLink.vue was removed while templates
// still used <external-link>, and nothing failed until the app rendered).
//
// A custom tag resolves when ONE of the following holds:
//   (a) the file's <script> (inline, or the sibling TS file referenced by
//       <script src="...">, inlined by the harness below) imports it — either
//       the binding name or the .vue specifier basename matches the tag — or
//       defines it locally (const X = defineComponent/defineAsyncComponent);
//   (b) it is globally registered: the element-plus icon set installed by
//       src/config/plugins/element/element.ts (every capitalized export of
//       @element-plus/icons-vue — GLOBALS below mirrors that registration);
//   (c) it is auto-registered by unplugin-vue-components, which with this
//       repo's vite.config.ts (Components() without a dirs override) exposes
//       every PascalCase basename under src/components/** — the harness
//       derives that set from the walked tree;
//   (d) it is a Vue built-in (VUE_BUILTINS below).
// Element Plus (el-*), vue-router (router-*) and vant (van-*) tags are
// library-prefixed and out of scope. The scanner is a pure function so the
// <external-link> regression is unit-tested directly on a synthetic fixture.

import * as ElementIcons from '@element-plus/icons-vue';
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, posix, relative, resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const root = process.cwd();

// Concise HTML element set (tags actually legal inside a Vue template body,
// including the SVG elements used by icon markup). Anything else that is not
// library-prefixed counts as a custom component tag.
const HTML_ELEMENTS = new Set([
  'a', 'abbr', 'address', 'area', 'article', 'aside', 'audio', 'b', 'base', 'bdi', 'bdo',
  'blockquote', 'body', 'br', 'button', 'canvas', 'caption', 'cite', 'code', 'col', 'colgroup',
  'data', 'datalist', 'dd', 'del', 'details', 'dfn', 'dialog', 'div', 'dl', 'dt', 'em', 'embed',
  'fieldset', 'figcaption', 'figure', 'footer', 'form', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'head', 'header', 'hgroup', 'hr', 'html', 'i', 'iframe', 'img', 'input', 'ins', 'kbd',
  'label', 'legend', 'li', 'link', 'main', 'map', 'mark', 'menu', 'meta', 'meter', 'nav',
  'noscript', 'object', 'ol', 'optgroup', 'option', 'output', 'p', 'param', 'picture', 'pre',
  'progress', 'q', 'rp', 'rt', 'ruby', 's', 'samp', 'script', 'search', 'section', 'select',
  'slot', 'small', 'source', 'span', 'strong', 'style', 'sub', 'summary', 'sup', 'table',
  'tbody', 'td', 'template', 'textarea', 'tfoot', 'th', 'thead', 'time', 'title', 'tr',
  'track', 'u', 'ul', 'var', 'video', 'wbr',
  'svg', 'path', 'circle', 'rect', 'line', 'polyline', 'polygon', 'g', 'defs', 'use', 'text',
  'ellipse', 'stop', 'lineargradient', 'radialgradient', 'filter', 'mask', 'pattern',
  'clippath', 'symbol', 'tspan', 'foreignobject', 'animate', 'animatetransform',
]);

// Vue built-in template components (allowlist path (d)).
const VUE_BUILTINS = new Set([
  'component', 'transition', 'transition-group', 'keep-alive', 'teleport', 'suspense',
]);

const LIBRARY_PREFIXES = [/^el-/i, /^router-/i, /^van-/i];

const IMPORT_RE = /import\s+([^'"]+?)\s+from\s*['"]([^'"]+)['"]/g;
const VUE_SPECIFIER_RE = /['"]([^'"]+\.vue)['"]/g;
const LOCAL_COMPONENT_RE = /const\s+([A-Za-z_$][\w$]*)\s*=\s*[^;\n]{0,80}(?:defineComponent|defineAsyncComponent)\s*\(/g;
const SCRIPT_SRC_RE = /<script\b[^>]*\ssrc=["']([^"']+)["'][^>]*>/g;

const normalizeTag = (tag: string): string => tag.toLowerCase().replace(/-/g, '');

function scriptOf(content: string): string {
  const parts: string[] = [];
  // The lookbehind skips self-closing <script ... src="..."/> tags so they
  // cannot swallow the rest of the file up to a later </script>.
  for (const match of content.matchAll(/<script\b[^>]*(?<!\/)>([\s\S]*?)<\/script>/g)) {
    parts.push(match[1]);
  }
  return parts.join('\n');
}

function templateText(content: string): string {
  // Depth-tracking walk: templates nest (<template #slot> inside the root
  // template), so a non-greedy regex would truncate the root block.
  const parts: string[] = [];
  let depth = 0;
  let start = -1;
  for (const match of content.matchAll(/<template\b[^>]*>|<\/template\s*>/g)) {
    if (match[0].startsWith('</')) {
      depth -= 1;
      if (depth === 0 && start >= 0) {
        parts.push(content.slice(start, match.index));
        start = -1;
      }
    } else if (depth === 0) {
      start = (match.index ?? 0) + match[0].length;
      depth = 1;
    } else {
      depth += 1;
    }
  }
  return parts.join('\n');
}

function importedNames(script: string): Set<string> {
  const names = new Set<string>();
  for (const match of script.matchAll(IMPORT_RE)) {
    const clause = match[1].replace(/[{}*]/g, '');
    for (const part of clause.split(',')) {
      const binding = part.trim().split(/\s+as\s+/).pop()?.trim();
      if (binding && /^[A-Za-z_$][\w$]*$/.test(binding)) names.add(normalizeTag(binding));
    }
  }
  return names;
}

function importedVueBasenames(script: string): Set<string> {
  const names = new Set<string>();
  for (const match of script.matchAll(VUE_SPECIFIER_RE)) {
    const segments = match[1].split('/');
    const base = segments[segments.length - 1].replace(/\.vue$/, '');
    if (base) names.add(normalizeTag(base));
  }
  return names;
}

function locallyDefinedNames(script: string): Set<string> {
  const names = new Set<string>();
  for (const match of script.matchAll(LOCAL_COMPONENT_RE)) {
    names.add(normalizeTag(match[1]));
  }
  return names;
}

function customTags(template: string): Set<string> {
  const tags = new Set<string>();
  for (const match of template.matchAll(/<([A-Za-z][A-Za-z0-9-]*)(?=[\s/>])/g)) {
    const tag = match[1];
    if (HTML_ELEMENTS.has(tag.toLowerCase())) continue;
    if (LIBRARY_PREFIXES.some((prefix) => prefix.test(tag))) continue;
    tags.add(tag);
  }
  return tags;
}

/**
 * Pure scanner: given a map of posix-relative .vue paths to file contents
 * (with external <script src> files already inlined), the globally registered
 * component names and the unplugin auto-registered names, return one entry
 * per custom tag that resolves through none of the paths (a)-(d).
 */
export function scanComponentResolution(
  files: Record<string, string>,
  globals: Set<string>,
  autoRegistered: Set<string>
): string[] {
  const violations: string[] = [];
  for (const [path, content] of Object.entries(files)) {
    const script = scriptOf(content);
    const resolvable = new Set<string>([
      ...importedNames(script),
      ...importedVueBasenames(script),
      ...locallyDefinedNames(script),
      ...autoRegistered,
    ]);
    for (const tag of customTags(templateText(content))) {
      const normalized = normalizeTag(tag);
      if (VUE_BUILTINS.has(tag.toLowerCase())) continue;
      if (resolvable.has(normalized)) continue;
      if (globals.has(tag.replace(/(^|-)([a-z0-9])/g, (_, __, char) => char.toUpperCase()))) continue;
      violations.push(`${path}: unresolved custom tag <${tag}>`);
    }
  }
  return violations;
}

// Harness: walk the real src/ tree.

function walk(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry);
    return statSync(path).isDirectory() ? walk(path) : path.endsWith('.vue') ? [path] : [];
  });
}

function inlineExternalScripts(content: string, absolutePath: string): string {
  let inlined = content;
  for (const match of content.matchAll(SCRIPT_SRC_RE)) {
    try {
      // Wrapped in a real <script> block so scriptOf() picks it up.
      inlined += `\n<script>\n${readFileSync(resolve(dirname(absolutePath), match[1]), 'utf8')}\n</script>`;
    } catch {
      // Unreadable sibling: leave the .vue content as is; the scanner then
      // surfaces unresolved tags instead of crashing the guardrail.
    }
  }
  return inlined;
}

const vueFiles = walk(join(root, 'src'));
const files: Record<string, string> = {};
for (const path of vueFiles) {
  const rel = relative(root, path).split('\\').join('/');
  files[rel] = inlineExternalScripts(readFileSync(path, 'utf8'), path);
}

// unplugin-vue-components default dirs (['src/components']) — not overridden
// in vite.config.ts — auto-register every .vue basename under src/components.
const autoRegistered = new Set(
  Object.keys(files)
    .filter((path) => path.startsWith('src/components/'))
    .map((path) => normalizeTag(posix.basename(path, '.vue')))
);

// Globals mirror src/config/plugins/element/element.ts: every capitalized
// export of @element-plus/icons-vue is registered via app.component().
const globals = new Set(
  Object.entries(ElementIcons)
    .filter(([name, comp]) => /^[A-Z]/.test(name) && comp && typeof comp === 'object')
    .map(([name]) => name)
);

describe('custom component resolution guardrail', () => {
  it('harness walks the real tree (guards against a vacuously green scan)', () => {
    expect(Object.keys(files).length).toBeGreaterThan(150);
    expect(autoRegistered.size).toBeGreaterThan(40);
    expect(globals.size).toBeGreaterThan(250);
  });

  it('every custom tag in src/**/*.vue resolves to a real component', () => {
    const violations = scanComponentResolution(files, globals, autoRegistered);
    expect(violations, violations.join('\n')).toEqual([]);
  });

  it('fails a fixture that references a deleted component (ExternalLink incident)', () => {
    const fixture: Record<string, string> = {
      'src/views/demo/Demo.vue': [
        '<template>',
        '  <external-link href="https://dc3.site"/>',
        '  <transition appear><span>ok</span></transition>',
        '</template>',
        '<script setup lang="ts">',
        '</script>',
      ].join('\n'),
    };
    const violations = scanComponentResolution(fixture, new Set(), new Set());
    expect(violations).toEqual(['src/views/demo/Demo.vue: unresolved custom tag <external-link>']);
  });

  it('resolves tags via import, global registration, auto-registration and built-ins', () => {
    const fixture: Record<string, string> = {
      'src/views/demo/Positive.vue': [
        '<template>',
        '  <tool-card/>',
        '  <device/>',
        '  <local-const/>',
        '  <Promotion/>',
        '  <keep-alive><span/></keep-alive>',
        '</template>',
        '<script setup lang="ts">',
        "import device from '@/views/device/Device.vue';",
        "import ToolCard from '@/components/card/tool/ToolCard.vue';",
        'const LocalConst = defineComponent({});',
        '</script>',
      ].join('\n'),
    };
    const violations = scanComponentResolution(fixture, new Set(['Promotion']), new Set());
    expect(violations).toEqual([]);
  });
});
