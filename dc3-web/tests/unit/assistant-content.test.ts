/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
