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

import {afterEach, describe, expect, it, vi} from 'vitest';

import {streamAgenticChatCompletion} from '@/api/agentic';
import i18n from '@/config/i18n';
import type {AgenticStreamCallbacks} from '@/config/types';
import {pickScenario, trailingHint} from '@/mock/fetch';

import {ensureMockChatEngine} from './helpers/chatHarness';

/**
 * Transport tier: the SSE chain under hostile delivery conditions — frames
 * split across reads at arbitrary byte boundaries (including mid-UTF-8
 * rune), many frames per read, malformed payloads surfacing as errors
 * instead of silent corruption, mid-stream aborts, and the mock engine's
 * token-sized delta stream that makes every dialogue test exercise the
 * incremental pipeline.
 */

const encoder = new TextEncoder();
const sse = (obj: unknown) => encoder.encode(`data: ${JSON.stringify(obj)}\n\n`);
const DONE = encoder.encode('data: [DONE]\n\n');

const sseResponse = (chunks: Uint8Array[]): Response => {
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(chunk);
      controller.close();
    },
  });
  return new Response(body, {
    status: 200,
    headers: {'Content-Type': 'text/event-stream'},
  });
};

interface StreamOutcome {
  content: string;
  reasoning: string;
  finishReason?: string;
  done: boolean;
  error?: Error;
}

const collectStream = async (
  callbacks: Partial<AgenticStreamCallbacks> = {},
  prompt = 'transport probe'
): Promise<StreamOutcome> => {
  const outcome: StreamOutcome = {content: '', reasoning: '', done: false};
  const merged: AgenticStreamCallbacks = {
    ...callbacks,
    onDelta: (delta) => {
      outcome.content += delta;
      callbacks.onDelta?.(delta);
    },
    onReasoning: (reasoning) => {
      outcome.reasoning += reasoning;
      callbacks.onReasoning?.(reasoning);
    },
    onFinish: (reason) => {
      outcome.finishReason = reason;
      callbacks.onFinish?.(reason);
    },
    onDone: () => {
      outcome.done = true;
      callbacks.onDone?.();
    },
    onError: (error) => {
      outcome.error = error;
      callbacks.onError?.(error);
    },
  };
  try {
    await streamAgenticChatCompletion(
      {
        model: 'dc3-agentic',
        messages: [{role: 'user', content: prompt}],
        stream: true,
        conversationId: 'transport-resilience',
        attachments: [],
      },
      merged
    );
  } catch (error) {
    outcome.error = error instanceof Error ? error : new Error(String(error));
  }
  return outcome;
};

/** Cut a byte stream into random small pieces the way a network would. */
const chop = (bytes: Uint8Array, minSize = 1, maxSize = 11): Uint8Array[] => {
  const pieces: Uint8Array[] = [];
  let cursor = 0;
  while (cursor < bytes.length) {
    const size = minSize + Math.floor(Math.random() * (maxSize - minSize + 1));
    pieces.push(bytes.subarray(cursor, Math.min(cursor + size, bytes.length)));
    cursor += size;
  }
  return pieces;
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('SSE transport resilience', () => {
  it('reassembles frames split across reads at arbitrary byte boundaries (mid-rune included)', async () => {
    const fullText = '风机轴承温度 85.4℃，已超过阈值。Fan bearing temperature exceeds threshold. 🌡️';
    const frames: Uint8Array[] = [
      sse({object: 'agentic.event', type: 'tool', title: '查询位号实时值', phase: 'start', status: 'running'}),
      sse({choices: [{delta: {content: fullText}}]}),
      sse({object: 'agentic.visualization', visualization: {type: 'line', title: '温度', dataset: [{t: 1}], encode: {}}}),
      sse({choices: [{delta: {reasoning_content: '对照阈值'}}]}),
      sse({choices: [{finish_reason: 'stop'}]}),
      DONE,
    ];
    const whole = frames.reduce((acc, frame) => {
      const merged = new Uint8Array(acc.length + frame.length);
      merged.set(acc);
      merged.set(frame, acc.length);
      return merged;
    }, new Uint8Array(0));

    // Chop the entire byte stream into random 1-11 byte pieces — frames,
    // lines and UTF-8 runes all get cut wherever they land.
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(sseResponse(chop(whole)))
    );

    let events = 0;
    let visualizations = 0;
    const outcome = await collectStream({
      onEvent: () => {
        events += 1;
      },
      onVisualization: () => {
        visualizations += 1;
      },
    });

    expect(outcome.error).toBeUndefined();
    expect(outcome.done).toBe(true);
    expect(outcome.content).toBe(fullText);
    expect(outcome.reasoning).toBe('对照阈值');
    expect(outcome.finishReason).toBe('stop');
    expect(events).toBe(1);
    expect(visualizations).toBe(1);
  });

  it('parses several frames delivered in a single read', async () => {
    const frameA = sse({choices: [{delta: {content: 'first. '}}]});
    const frameB = sse({choices: [{delta: {content: 'second. '}}]});
    const frameC = sse({choices: [{delta: {content: 'third'}}]});
    const singleRead = new Uint8Array(frameA.length + frameB.length + frameC.length + DONE.length);
    singleRead.set(frameA, 0);
    singleRead.set(frameB, frameA.length);
    singleRead.set(frameC, frameA.length + frameB.length);
    singleRead.set(DONE, frameA.length + frameB.length + frameC.length);

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sseResponse([singleRead])));

    const outcome = await collectStream();
    expect(outcome.error).toBeUndefined();
    expect(outcome.content).toBe('first. second. third');
    expect(outcome.done).toBe(true);
  });

  it('parses the trailing frame even without a blank-line terminator', async () => {
    // Last data block arrives without the closing \n\n — the final flush
    // must still deliver it instead of dropping it on the floor.
    const unterminated = encoder.encode('data: [DONE]');
    const frames = [sse({choices: [{delta: {content: 'flushed'}}]}), unterminated];
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sseResponse(frames)));

    const outcome = await collectStream();
    expect(outcome.error).toBeUndefined();
    expect(outcome.content).toBe('flushed');
    expect(outcome.done).toBe(true);
  });

  it('surfaces a malformed frame as a stream error instead of silently corrupting the conversation', async () => {
    const frames = [
      sse({choices: [{delta: {content: 'good prefix, '}}]}),
      encoder.encode('data: {not valid json}\n\n'),
      sse({choices: [{finish_reason: 'stop'}]}),
    ];
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sseResponse(frames)));

    let reported: Error | undefined;
    const outcome = await collectStream({onError: (error) => (reported = error)});

    // Contract: the parser never guesses over broken payloads — the turn
    // fails loudly so the UI can offer regeneration (L60) rather than
    // showing a silently truncated answer as complete.
    expect(outcome.error).toBeDefined();
    expect(reported).toBe(outcome.error);
    expect(outcome.done).toBe(false);
    expect(outcome.content).toBe('good prefix, ');
  });

  it('delivers partial output and an AbortError when the stream is stopped mid-flight', async () => {
    const abortError =
      typeof DOMException !== 'undefined'
        ? new DOMException('The operation was aborted.', 'AbortError')
        : Object.assign(new Error('The operation was aborted.'), {name: 'AbortError'});

    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((_url: unknown, init?: RequestInit) => {
        const signal = init?.signal;
        const body = new ReadableStream<Uint8Array>({
          start(controller) {
            controller.enqueue(sse({choices: [{delta: {content: 'half of the answer'}}]}));
            // The stream hangs until the consumer aborts; a real aborted
            // body read rejects with an AbortError.
            signal?.addEventListener(
              'abort',
              () => {
                controller.error(abortError);
              },
              {once: true}
            );
          },
        });
        return Promise.resolve(
          new Response(body, {status: 200, headers: {'Content-Type': 'text/event-stream'}})
        );
      })
    );

    // The consumer stops the stream (store.stopStreaming) right after the
    // first delta lands.
    const abortController = new AbortController();
    const outcome = await collectStream({
      signal: abortController.signal,
      onDelta: () => {
        abortController.abort();
      },
    });

    expect(outcome.error?.name).toBe('AbortError');
    expect(outcome.content).toBe('half of the answer');
    expect(outcome.done).toBe(false);
  });
});

describe('mock engine token stream', () => {
  it('streams scenario text as many small token-sized deltas', async () => {
    ensureMockChatEngine();
    i18n.global.locale.value = 'zh';

    const prompt = '位号实时值如何';
    const scenario = pickScenario(prompt);
    const deltas: string[] = [];
    const outcome = await collectStream(
      {
        onDelta: (delta) => {
          deltas.push(delta);
        },
      },
      prompt
    );

    // The reply is no longer one mega-delta: every piece is at most 8 chars,
    // so the count is bounded below by totalChars/8 — guaranteed many small
    // tokens exercising the incremental pipeline on every run.
    const totalChars = scenario.text.length + trailingHint().length;
    expect(deltas.length).toBeGreaterThanOrEqual(Math.ceil(totalChars / 8));
    expect(deltas.length).toBeLessThanOrEqual(totalChars);
    for (const delta of deltas) {
      expect(delta.length).toBeGreaterThan(0);
      expect(delta.length).toBeLessThanOrEqual(8);
    }
    expect(outcome.content).toBe(`${scenario.text}${trailingHint()}`);
    expect(outcome.error).toBeUndefined();
    expect(outcome.finishReason).toBe('stop');
  });
});
