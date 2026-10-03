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

// DOMPurify relies on a full DOM implementation; happy-dom does not strip
// <script>/on* here (verified in rendered-assistant-message.test.ts), so run
// this file under jsdom like the existing sanitizer tests.
// @vitest-environment jsdom
import {mount} from '@vue/test-utils';
import {describe, expect, it, vi} from 'vitest';

import RenderedAssistantMessage from '@/components/agentic/RenderedAssistantMessage.vue';
import {parseAssistantContent, quoteExcerpt, toPlainText} from '@/components/agentic/assistantContent';

import {CORPUS} from './corpus';

/**
 * Rendering hardening: hostile payloads from the corpus must stay inert through
 * the assistant rendering chain (parse → marked → DOMPurify → v-html) and the
 * user-side quoting helpers. Model output is untrusted input, so this tier
 * covers malicious content in both roles.
 */

vi.mock('@/components/agentic/ChartBlock.vue', () => ({
  default: {
    name: 'ChartBlock',
    props: ['kind', 'spec', 'chart'],
    template: '<div class="chart-block-stub" />',
  },
}));

const HOSTILE_FAMILIES = ['edge-markup', 'edge-code-injection', 'edge-injection', 'edge-jailbreak', 'edge-secrets'];
const hostileCases = CORPUS.filter((item) => HOSTILE_FAMILIES.includes(item.family));

const FORBIDDEN_TAGS = ['script', 'iframe', 'object', 'embed', 'applet', 'form', 'meta', 'link', 'frameset'];
const URL_ATTRS = ['href', 'src', 'xlink:href', 'action', 'formaction', 'data'];

/**
 * Inspect the rendered DOM (not the raw HTML string — escaped payload text is
 * inert and must not trip the check) for executable structures: forbidden
 * elements, `on*` handler attributes, and active-content URL schemes.
 * @param root - rendered root element
 * @returns descriptions of every executable structure found
 */
const executableStructures = (root: Element): string[] => {
  const findings: string[] = [];
  for (const node of Array.from(root.querySelectorAll('*'))) {
    const tag = node.tagName.toLowerCase();
    if (FORBIDDEN_TAGS.includes(tag)) findings.push(`element <${tag}>`);
    for (const attr of Array.from(node.attributes)) {
      const name = attr.name.toLowerCase();
      if (name.startsWith('on')) findings.push(`handler ${tag}[${name}]`);
      if (URL_ATTRS.includes(name) && /^\s*(javascript|vbscript|data:text\/html)/i.test(attr.value)) {
        findings.push(`scheme ${tag}[${name}]=${attr.value.slice(0, 40)}`);
      }
    }
  }
  return findings;
};

describe('dialogue rendering hardening', () => {
  it('has a broad hostile-payload sample', () => {
    expect(hostileCases.length).toBeGreaterThanOrEqual(150);
  });

  it.each(hostileCases.map((item) => [item.id, item] as const))(
    '%s renders hostile model output inert',
    (_id, item) => {
      const payload = item.turns[item.turns.length - 1];
      const wrapper = mount(RenderedAssistantMessage, {props: {content: `分析结果：${payload}`}});

      expect(executableStructures(wrapper.element)).toEqual([]);
      expect(document.querySelector('script')).toBeNull();
    }
  );

  it.each(hostileCases.map((item) => [item.id, item] as const))(
    '%s survives the plain-text and quoting helpers',
    (_id, item) => {
      const payload = item.turns[item.turns.length - 1];
      const plain = toPlainText(payload);
      expect(typeof plain).toBe('string');
      const excerpt = quoteExcerpt(payload);
      expect(typeof excerpt).toBe('string');
      // Parsing must never throw and must always yield a renderable shape.
      const parsed = parseAssistantContent(payload);
      expect(Array.isArray(parsed.segments)).toBe(true);
    }
  );
});
