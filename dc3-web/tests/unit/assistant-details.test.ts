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

import {createAssistantDetails} from '@/components/agentic/assistantDetails';
import type {AgenticMessage, AgenticTraceEvent} from '@/config/types';

// i18n stub: echo the key so summary composition stays assertable.
const t = (key: string, params?: Record<string, unknown>) =>
  params ? `${key}(${Object.values(params).join(',')})` : key;

const details = (liveTraces: AgenticTraceEvent[] = []) =>
  createAssistantDetails({t, liveTraces: () => liveTraces});

const base = (patch: Partial<AgenticMessage>): AgenticMessage =>
  ({id: 'm1', role: 'assistant', content: 'ok', ...patch}) as AgenticMessage;

describe('assistant detail derivations', () => {
  it('derives tool steps with running indices and prefers trace results over starts', () => {
    const message = base({
      contentExt: {
        traces: [
          {type: 'tool', name: 'searchDevices', phase: 'start', status: 'running'},
          {type: 'tool', name: 'searchDevices', phase: 'result', status: 'success'},
          {type: 'tool', name: 'writePoint', phase: 'error', status: 'failed'},
        ],
        tools: ['listDrivers'],
      },
    });
    const steps = details().assistantToolSteps(message);
    // Traced tools first, then a fallback step for tools that never traced.
    expect(steps.map((s) => s.label)).toEqual(['searchDevices', 'writePoint', 'listDrivers']);
    expect(steps.map((s) => s.index)).toEqual([1, 2, 3]);
    expect(steps[0].status).toBe('success'); // result outranks start
    expect(steps[1].status).toBe('failed');
    expect(steps[2].status).toBeUndefined(); // fallback carries no status
  });

  it('falls back to tool-name entries when no trace events exist', () => {
    const message = base({contentExt: {tools: ['alpha', 'beta']}});
    const steps = details().assistantToolSteps(message);
    expect(steps.map((s) => s.label)).toEqual(['alpha', 'beta']);
  });

  it('aggregates token totals and drops empty labels', () => {
    const message = base({contentExt: {tokens: {input: 10, output: 5, memory: 0}}});
    const d = details();
    expect(d.assistantTokenTotal(message)).toBe(15);
    expect(d.assistantTokenItems(message).map((i) => i.label)).toEqual([
      'agentic.tokenInput',
      'agentic.tokenOutput',
      'agentic.tokenMemory',
    ]);
    expect(d.assistantTokenTotal(base({contentExt: {}}))).toBeUndefined();
  });

  it('derives message status from status, finish reason and recovery flags', () => {
    const d = details();
    expect(d.assistantStatus(base({status: 'FAILED'}))).toBe('agentic.statusFailed');
    expect(d.assistantStatus(base({finishReason: 'error'}))).toBe('agentic.statusFailed');
    expect(d.assistantStatus(base({status: 'CANCELLED'}))).toBe('agentic.statusCancelled');
    expect(d.assistantStatus(base({contentExt: {recovered: true}}))).toBe('agentic.statusDone');
    expect(d.assistantStatus(base({status: 'COMPLETED'}))).toBe('agentic.statusDone');
  });

  it('folds reasoning, tools and tokens into the footnote summary', () => {
    const message = base({
      contentExt: {
        reasoning: true,
        tools: ['searchDevices'],
        tokens: {input: 3, output: 4},
      },
    });
    const summary = details().assistantDetailSummary(message);
    expect(summary).toContain('agentic.statusLabel');
    expect(summary).toContain('agentic.thinkingreasoningmode'); // lowercased by design
    expect(summary).toContain('agentic.detailSummaryTools(1)');
    expect(summary).toContain('agentic.detailSummaryTokens(7)');
  });

  it('gates the footnote on content, not on a bare failure status', () => {
    const d = details();
    // A failed message with nothing else carries no footnote at all —
    // the message badge is what surfaces its state in the UI.
    expect(d.hasAssistantDetails(base({status: 'FAILED'}))).toBe(false);
    expect(d.hasAssistantDetails(base({contentExt: {tokens: {input: 1}}}))).toBe(true);
  });

  it('merges live streaming traces with persisted ones and dedupes', () => {
    const live: AgenticTraceEvent[] = [{type: 'tool', name: 'liveTool', phase: 'start', status: 'running'}];
    const message = base({
      streaming: true,
      contentExt: {traces: [{type: 'tool', name: 'liveTool', phase: 'start', status: 'running'}]},
    });
    const steps = details(live).assistantToolSteps(message);
    expect(steps).toHaveLength(1); // same trace deduped
    expect(steps[0].label).toBe('liveTool');
    // non-streaming messages ignore live traces
    expect(details(live).assistantToolSteps(base({contentExt: {}}))).toHaveLength(0);
  });
});
