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

import {mount} from '@vue/test-utils';
import {describe, expect, it, vi} from 'vitest';

import RenderedAssistantMessage from '@/components/agentic/RenderedAssistantMessage.vue'; // Stub ChartBlock — its internals (G2, getContext, ResizeObserver) aren't

// Stub ChartBlock — its internals (G2, getContext, ResizeObserver) aren't
// the subject under test here. The contract we care about is "one
// ChartBlock per chart segment / per attached chart spec".
vi.mock('@/components/agentic/ChartBlock.vue', () => ({
  default: {
    name: 'ChartBlock',
    props: ['kind', 'spec', 'chart'],
    template: '<div class="chart-block-stub" :data-kind="kind || chart?.type" :data-id="chart?.id || spec?.title" />',
  },
}));

function mountAssistant(props: Record<string, unknown>) {
  return mount(RenderedAssistantMessage, {props});
}

describe('RenderedAssistantMessage', () => {
  it('renders plain text content as a single sanitized markdown segment', () => {
    const wrapper = mountAssistant({content: 'hello **world**'});

    const markdown = wrapper.findAll('.agentic-markdown');
    expect(markdown).toHaveLength(1);
    expect(markdown[0].html()).toContain('<strong>world</strong>');
    // No inline charts when only plain text was passed.
    expect(wrapper.findAll('.chart-block-stub')).toHaveLength(0);
  });

  it('strips <script> tags and inline event handlers from rendered markdown', () => {
    const wrapper = mountAssistant({
      content: 'before<script>alert(1)</script><img src=x onerror="bad()" />after',
    });

    const html = wrapper.find('.agentic-markdown').html();
    expect(html).not.toContain('<script');
    expect(html).not.toContain('onerror=');
    expect(html).not.toContain('javascript:');
  });

  it('renders one ChartBlock per attached visualization spec', () => {
    const wrapper = mountAssistant({
      content: 'see chart',
      charts: [
        {id: 'c1', type: 'line', title: 'Trend', dataset: [], encode: {x: 'i', y: 'v'}},
        {id: 'c2', type: 'column', title: 'Counts', dataset: [], encode: {x: 'i', y: 'v'}},
      ],
    });

    const charts = wrapper.findAll('.chart-block-stub');
    expect(charts).toHaveLength(2);
    expect(charts[0].attributes('data-id')).toBe('c1');
    expect(charts[1].attributes('data-kind')).toBe('column');
  });
});
