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

import {afterAll, describe, expect, it} from 'vitest';

import {parseAssistantContent} from '@/components/agentic/assistantContent';

import {CORPUS} from './corpus';
import {runBlocking, runStreaming} from './helpers/chatHarness';
import {writeJsonReport} from './helpers/report';

/**
 * Contract tier: every corpus utterance must complete a clean dialogue turn
 * through the full chain — transport, SSE parsing, and content parsing — in
 * both streaming and blocking modes. This is the hard gate of the suite: no
 * input class may crash, truncate, or stall the reply pipeline.
 */

const streamLatencies: number[] = [];
let streamPass = 0;
let streamFail = 0;
let blockingPass = 0;
let blockingFail = 0;

const percentile = (values: number[], p: number): number => {
  if (values.length === 0) return 0;
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.min(sorted.length - 1, Math.floor((p / 100) * sorted.length))];
};

describe('dialogue contract · streaming', () => {
  it.each(CORPUS.map((item) => [item.id, item] as const))('%s completes a clean SSE turn', async (_id, item) => {
    const outcome = await runStreaming(item, item.turns[item.turns.length - 1]);
    streamLatencies.push(outcome.elapsedMs);

    const problems: string[] = [];
    if (outcome.error) problems.push(`error: ${outcome.error.message}`);
    if (!outcome.done) problems.push('onDone never fired');
    if (outcome.finishReason !== 'stop') problems.push(`finishReason=${outcome.finishReason}`);
    if (!outcome.content.trim()) problems.push('empty reply content');
    for (const event of outcome.events) {
      if (!event.type || !event.title) problems.push('malformed trace event');
    }
    for (const visualization of outcome.visualizations) {
      if (!Array.isArray(visualization.dataset)) problems.push('visualization without dataset');
    }
    if (outcome.content.trim()) {
      const parsed = parseAssistantContent(outcome.content);
      if (parsed.segments.length === 0) problems.push('content parsed to zero segments');
    }

    if (problems.length === 0) {
      streamPass += 1;
    } else {
      streamFail += 1;
    }
    expect(problems).toEqual([]);
  });
});

describe('dialogue contract · blocking', () => {
  it.each(CORPUS.map((item) => [item.id, item] as const))('%s completes a clean blocking turn', async (_id, item) => {
    const outcome = await runBlocking(item, item.turns[item.turns.length - 1]);

    const problems: string[] = [];
    if (outcome.error) problems.push(`error: ${outcome.error.message}`);
    if (!outcome.content.trim()) problems.push('empty reply content');
    if (outcome.finishReason !== 'stop') problems.push(`finishReason=${outcome.finishReason}`);
    for (const visualization of outcome.visualizations) {
      if (!Array.isArray(visualization.dataset)) problems.push('visualization without dataset');
    }

    if (problems.length === 0) {
      blockingPass += 1;
    } else {
      blockingFail += 1;
    }
    expect(problems).toEqual([]);
  });
});

afterAll(() => {
  writeJsonReport('contract-metrics', {
    generated: new Date().toISOString(),
    streamPass,
    streamFail,
    blockingPass,
    blockingFail,
    streamP50Ms: percentile(streamLatencies, 50),
    streamP95Ms: percentile(streamLatencies, 95),
  });
});
