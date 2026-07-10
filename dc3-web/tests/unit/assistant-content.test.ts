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

import {describe, expect, it} from 'vitest';

import {parseAssistantContent, toPlainText} from '@/components/agentic/assistantContent';

describe('assistant content parser', () => {
  it('keeps model-emitted tool-call JSON as visible markdown', () => {
    const content = [
      '准备查询设备数据。',
      '```json',
      '{"tool":"getDeviceList","arguments":{"name":"HZ-PACK-L3-VISION"}}',
      '```',
      '未获取到真实工具结果。',
    ].join('\n');

    const parsed = parseAssistantContent(content);
    const visibleText = parsed.segments.map((segment) => (segment.type === 'markdown' ? segment.text : '')).join('\n');

    expect(visibleText).toContain('getDeviceList');
    expect(toPlainText(content)).toContain('getDeviceList');
  });

  it('keeps chart fences as chart segments and summarizes them as plain text', () => {
    const content = [
      '历史趋势如下。',
      '```chart:line',
      '{"title":"HZ-PACK-L3-VISION / Conveyor Speed","unit":"m/min","series":[{"name":"value","data":[[0,31.2],[1,31.7]]}]}',
      '```',
    ].join('\n');

    const parsed = parseAssistantContent(content);

    expect(parsed.segments).toHaveLength(2);
    expect(parsed.segments[1]).toMatchObject({
      type: 'chart',
      kind: 'line',
      spec: {
        title: 'HZ-PACK-L3-VISION / Conveyor Speed',
        unit: 'm/min',
      },
    });
    expect(toPlainText(content)).toContain('[line chart — HZ-PACK-L3-VISION / Conveyor Speed, 2 points]');
  });
});
