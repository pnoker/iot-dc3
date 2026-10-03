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

// The live suite drives real HTTP/SSE against a running stack; run it under
// Node so requests use the native fetch instead of happy-dom's CORS-restricted
// browser emulation. No DOM is needed here.
// @vitest-environment node
import {afterAll, beforeAll, describe, expect, it} from 'vitest';

import {login, type AuthSession} from './auth';
import {CORPUS, corpusStats} from '../corpus';
import type {DialogueCase} from '../corpus/types';
import {writeJsonReport, writeMarkdownReport} from '../helpers/report';
import {judgeCase, scoreCase, type CaseScore, type JudgeConfig} from './evaluator';
import {listMessages, listSessions} from './chain';
import {runLiveConversation, type LiveOutcome, type LiveRunConfig} from './sse';

/**
 * Live full-chain dialogue evaluation against a running DC3 stack (gateway →
 * auth/manager/data/agentic). Gated by `AGENTIC_EVAL_BASE_URL` — see
 * tests/agentic-dialogue/README.md for the environment contract. The suite
 * logs in through the real auth flow, replays every corpus case against the
 * real model, scores the replies, and verifies session/message persistence
 * through the gateway afterwards.
 */

const BASE_URL = (process.env.AGENTIC_EVAL_BASE_URL ?? '').replace(/\/$/, '');
const MODEL = process.env.AGENTIC_EVAL_MODEL ?? 'dc3-agentic';
const TIMEOUT_MS = Number(process.env.AGENTIC_EVAL_TIMEOUT_MS ?? 120_000);
const CONCURRENCY = Math.max(1, Number(process.env.AGENTIC_EVAL_CONCURRENCY ?? 4));
const JUDGE_URL = process.env.AGENTIC_EVAL_JUDGE_URL ?? '';
const FAMILY_FILTER = process.env.AGENTIC_EVAL_FAMILY_FILTER ?? '';
const LIMIT = Number(process.env.AGENTIC_EVAL_LIMIT ?? 0);

const selectedCorpus = ((): DialogueCase[] => {
  let cases = CORPUS;
  if (FAMILY_FILTER) {
    const pattern = new RegExp(FAMILY_FILTER);
    cases = cases.filter((item) => pattern.test(item.family));
  }
  return LIMIT > 0 ? cases.slice(0, LIMIT) : cases;
})();

const liveConfig: LiveRunConfig = {
  baseUrl: BASE_URL,
  tenant: process.env.AGENTIC_EVAL_TENANT ?? 'default',
  login: process.env.AGENTIC_EVAL_LOGIN ?? 'dc3',
  cookie: process.env.AGENTIC_EVAL_COOKIE,
  model: MODEL,
  timeoutMs: TIMEOUT_MS,
};

const judgeConfig: JudgeConfig | undefined = JUDGE_URL
  ? {
      baseUrl: JUDGE_URL,
      apiKey: process.env.AGENTIC_EVAL_JUDGE_KEY,
      model: process.env.AGENTIC_EVAL_JUDGE_MODEL ?? 'gpt-4o',
    }
  : undefined;

const scores: CaseScore[] = [];
const failures: string[] = [];
const conversationIds = new Map<string, string>();
const chainChecks: Array<{name: string; pass: boolean; detail?: string}> = [];

const runCase = async (item: DialogueCase): Promise<CaseScore> => {
  const attempt = async (): Promise<LiveOutcome> => {
    const conversationId = `agentic-eval-${item.id}-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
    conversationIds.set(item.id, conversationId);

    // Mirror the web client exactly: one user message per request, history
    // carried server-side through the shared conversation id.
    let outcome: LiveOutcome | undefined;
    for (const turn of item.turns) {
      outcome = await runLiveConversation(liveConfig, conversationId, [{role: 'user', content: turn}]);
      if (outcome.error) break;
    }
    return (
      outcome ?? {
        content: '',
        reasoning: '',
        events: [],
        visualizationCount: 0,
        done: false,
        error: 'no turn executed',
        elapsedMs: 0,
      }
    );
  };

  let finalOutcome = await attempt();
  let score = scoreCase(item, finalOutcome);

  // Parallel SSE streams occasionally drop mid-reply (empty content / missing
  // finish frame). That is a transport artifact, not a model-quality signal,
  // so retry once before recording; quality failures are never retried.
  const transportShaped =
    Boolean(finalOutcome.error) || !finalOutcome.content.trim() || !finalOutcome.finishReason;
  if (!score.hardPass && transportShaped) {
    finalOutcome = await attempt();
    score = scoreCase(item, finalOutcome);
  }

  if (judgeConfig) {
    const judged = await judgeCase(judgeConfig, item, finalOutcome.content);
    if (judged) {
      score.checks.push({
        name: `judge:${judged.relevance}/${judged.correctness}/${judged.safety}/${judged.tone}`,
        pass: judged.relevance >= 3 && judged.safety >= 3,
        hard: false,
        detail: judged.rationale,
      });
    }
  }
  if (!score.hardPass) {
    const failed = score.checks.filter((check) => check.hard && !check.pass).map((check) => check.name);
    const shape = `finish=${finalOutcome.finishReason ?? '-'} len=${finalOutcome.content.length} events=${finalOutcome.events.length} viz=${finalOutcome.visualizationCount}`;
    failures.push(
      `${item.id} [${item.family}] → ${failed.join(', ')} | ${shape}${finalOutcome.error ? ` | cause: ${finalOutcome.error.slice(0, 160)}` : ''}`
    );
  }
  return score;
};

describe.runIf(Boolean(BASE_URL))('live full-chain dialogue evaluation', () => {
  beforeAll(async () => {
    if (!liveConfig.cookie) {
      const session: AuthSession = await login(
        BASE_URL,
        liveConfig.tenant ?? 'default',
        liveConfig.login ?? 'dc3',
        process.env.AGENTIC_EVAL_PASSWORD ?? 'dc3dc3dc3'
      );
      liveConfig.cookie = session.cookie;
    }
  }, 60_000);

  it('authenticates and lists sessions through the gateway', async () => {
    const sessions = await listSessions(BASE_URL, liveConfig);
    chainChecks.push({name: 'auth+gateway session list', pass: Array.isArray(sessions), detail: `${sessions.length} sessions`});
    expect(Array.isArray(sessions)).toBe(true);
    expect(sessions.length).toBeGreaterThanOrEqual(0);
  });

  it('evaluates every corpus case against the real assistant', async () => {
    const queue = [...selectedCorpus];
    const workers = Array.from({length: Math.min(CONCURRENCY, queue.length)}, async () => {
      for (let item = queue.shift(); item; item = queue.shift()) {
        scores.push(await runCase(item));
      }
    });
    await Promise.all(workers);
    expect(scores.length).toBe(selectedCorpus.length);
    expect(failures).toEqual([]);
  }, 24 * 60 * 60 * 1000);

  it('persists conversation turns and replays them from the backend', async () => {
    // Sample across families: the streamed replies must be persisted as
    // user/assistant message pairs and readable back through the gateway.
    const sample = selectedCorpus.filter((_, index) => index % 97 === 0).slice(0, 20);
    const problems: string[] = [];
    for (const item of sample) {
      const conversationId = conversationIds.get(item.id);
      if (!conversationId) continue;
      const messages = await listMessages(BASE_URL, liveConfig, conversationId);
      const roles = messages.map((message) => message.role);
      if (messages.length < item.turns.length * 2) {
        problems.push(`${item.id}: expected ≥${item.turns.length * 2} persisted messages, got ${messages.length}`);
      }
      if (roles.filter((role) => role === 'assistant').length < item.turns.length) {
        problems.push(`${item.id}: assistant replies not persisted (${roles.join(',')})`);
      }
    }
    chainChecks.push({name: 'message persistence replay', pass: problems.length === 0, detail: problems.join('; ')});
    expect(problems).toEqual([]);
  }, 300_000);

  afterAll(() => {
    const byFamily = new Map<string, {cases: number; pass: number; score: number; elapsed: number}>();
    for (const score of scores) {
      const entry = byFamily.get(score.family) ?? {cases: 0, pass: 0, score: 0, elapsed: 0};
      entry.cases += 1;
      entry.pass += score.hardPass ? 1 : 0;
      entry.score += score.score;
      entry.elapsed += score.elapsedMs;
      byFamily.set(score.family, entry);
    }
    const rows = [...byFamily.entries()].map(([family, entry]) => ({
      family,
      cases: entry.cases,
      pass: entry.pass,
      passRate: `${((entry.pass / entry.cases) * 100).toFixed(1)}%`,
      avgScore: (entry.score / entry.cases).toFixed(2),
      avgLatencyMs: Math.round(entry.elapsed / entry.cases),
    }));

    writeJsonReport(`live-eval-${Date.now()}`, {
      generated: new Date().toISOString(),
      endpoint: BASE_URL,
      model: MODEL,
      concurrency: CONCURRENCY,
      chainChecks,
      failures,
      rows,
      scores,
    });
    writeMarkdownReport(
      'live-eval-report',
      [
        '# Agentic Assistant Dialogue Test Report (live full chain)',
        '',
        `Generated: ${new Date().toISOString()}`,
        `Endpoint: ${BASE_URL} · Model: ${MODEL} · Concurrency: ${CONCURRENCY} · Judge: ${judgeConfig ? judgeConfig.model : 'off'}`,
        '',
        `Corpus size: **${corpusStats(selectedCorpus).total}** (filter: ${FAMILY_FILTER || 'none'}${LIMIT ? `, limit ${LIMIT}` : ''}) · Hard-pass: **${scores.filter((s) => s.hardPass).length}/${scores.length}**`,
        '',
        '## Full-chain checks',
        '',
        ...chainChecks.map((check) => `- ${check.pass ? '✅' : '❌'} ${check.name}${check.detail ? ` — ${check.detail}` : ''}`),
        '',
        '| Family | Cases | Pass | Pass rate | Avg score | Avg latency |',
        '|---|---:|---:|---:|---:|---:|',
        ...rows.map(
          (row) => `| ${row.family} | ${row.cases} | ${row.pass} | ${row.passRate} | ${row.avgScore} | ${row.avgLatencyMs}ms |`
        ),
        '',
        '## Failures',
        '',
        ...(failures.length === 0 ? ['None — every case passed its hard checks.'] : failures.map((line) => `- ${line}`)),
      ].join('\n')
    );
  });
});

describe.runIf(!BASE_URL)('live full-chain dialogue evaluation (skipped)', () => {
  it('documents the required environment to enable live evaluation', () => {
    expect(BASE_URL).toBe('');
  });
});
