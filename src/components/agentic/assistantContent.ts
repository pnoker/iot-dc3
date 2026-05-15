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

/**
 * Parse the assistant message content into renderable segments and tool-call
 * narration. Two transforms are layered on top of plain markdown:
 *
 *  1. Chart fences — ` ```chart:line\n{...}\n``` ` (also `chart:area`,
 *     `chart:column`). The fenced JSON is decoded into a {@link ChartSpec};
 *     each fence becomes its own segment so the renderer can mount an antv
 *     chart in place. JSON parse failures fall back to a markdown segment so
 *     users still see the original code block.
 *
 *  2. Tool-call narration — when the LLM emits a tool/function call as JSON
 *     in plain text instead of routing it through Spring AI's tool-callback
 *     pipeline, the JSON appears in the bubble (e.g. `{"tool":"searchDevice",
 *     "arguments":{...}}`). These objects are pulled out and surfaced in the
 *     details fold-out alongside backend traces.
 */

export interface ChartSeries {
  name?: string;
  data: Array<[number | string, number]>;
}

export interface ChartSpec {
  title?: string;
  unit?: string;
  xLabel?: string;
  yLabel?: string;
  xType?: 'time' | 'category' | 'linear';
  series: ChartSeries[];
}

export interface ChartSegment {
  type: 'chart';
  kind: 'line' | 'area' | 'column';
  spec: ChartSpec;
}

export interface MarkdownSegment {
  type: 'markdown';
  text: string;
}

export type AssistantSegment = MarkdownSegment | ChartSegment;

export interface ToolNarration {
  tool: string;
  arguments: unknown;
  raw: string;
}

export interface ParsedAssistantContent {
  segments: AssistantSegment[];
  toolNarrations: ToolNarration[];
}

const CHART_FENCE_RE = /```chart:(line|area|column)\s*\n([\s\S]*?)\n```/g;
const TOOL_KEYS = new Set(['tool', 'arguments', 'name', 'function', 'parameters', 'args']);

/**
 * Single entry-point for the assistant message renderer. Returns the visible
 * segments (markdown + chart) in source order and any tool-call narration that
 * was lifted out for the details fold-out. Empty/whitespace input yields an
 * empty result instead of throwing.
 */
export const parseAssistantContent = (content: string | undefined | null): ParsedAssistantContent => {
  if (!content) {
    return { segments: [], toolNarrations: [] };
  }

  const segments: AssistantSegment[] = [];
  const toolNarrations: ToolNarration[] = [];

  let cursor = 0;
  CHART_FENCE_RE.lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = CHART_FENCE_RE.exec(content)) !== null) {
    if (match.index > cursor) {
      pushMarkdown(segments, toolNarrations, content.slice(cursor, match.index));
    }
    const kind = match[1] as 'line' | 'area' | 'column';
    const payload = match[2] ?? '';
    let spec: ChartSpec | null = null;
    try {
      const parsed = JSON.parse(payload);
      if (isChartSpec(parsed)) {
        spec = parsed;
      }
    } catch {
      spec = null;
    }
    if (spec) {
      segments.push({ type: 'chart', kind, spec });
    } else {
      // Bad JSON — fall back to showing the fence as a normal code block
      segments.push({ type: 'markdown', text: match[0] });
    }
    cursor = match.index + match[0].length;
  }
  if (cursor < content.length) {
    pushMarkdown(segments, toolNarrations, content.slice(cursor));
  }

  return { segments, toolNarrations };
};

const pushMarkdown = (segments: AssistantSegment[], toolNarrations: ToolNarration[], text: string) => {
  const { cleaned, narrations } = extractToolNarrations(text);
  toolNarrations.push(...narrations);
  if (cleaned.trim().length > 0) {
    segments.push({ type: 'markdown', text: cleaned });
  }
};

/**
 * Scan a chunk of markdown for tool-call JSON the model dropped into plain
 * prose. Two patterns are recognized:
 *
 *  - ` ```json {"tool":"X",...}``` ` fenced blocks
 *  - bare `{"tool":"X",...}` object literals on their own line(s) (best-effort
 *    brace-matching that bails on nesting/quote edge cases)
 *
 * A JSON object only counts as a tool call when it parses cleanly *and* its
 * top-level keys are a subset of {@link TOOL_KEYS} — otherwise it is kept in
 * the visible text. Each detected narration is removed from `cleaned` so it
 * doesn't render twice.
 */
const extractToolNarrations = (input: string): { cleaned: string; narrations: ToolNarration[] } => {
  const narrations: ToolNarration[] = [];
  let cleaned = input;

  cleaned = cleaned.replace(/```(?:json)?\s*\n([\s\S]*?)\n```/g, (full: string, body?: string) => {
    const narration = tryParseToolNarration(body ?? '');
    if (narration) {
      narrations.push({ ...narration, raw: full });
      return '';
    }
    return full;
  });

  const brace: { start: number; depth: number } = { start: -1, depth: 0 };
  let buffer = '';
  let inString = false;
  let escape = false;
  for (let i = 0; i < cleaned.length; i += 1) {
    const ch = cleaned[i];
    if (brace.start < 0) {
      if (ch === '{') {
        brace.start = i;
        brace.depth = 1;
      }
      continue;
    }
    if (escape) {
      escape = false;
      continue;
    }
    if (ch === '\\' && inString) {
      escape = true;
      continue;
    }
    if (ch === '"') {
      inString = !inString;
      continue;
    }
    if (inString) continue;
    if (ch === '{') brace.depth += 1;
    else if (ch === '}') {
      brace.depth -= 1;
      if (brace.depth === 0) {
        const candidate = cleaned.slice(brace.start, i + 1);
        const narration = tryParseToolNarration(candidate);
        if (narration) {
          narrations.push({ ...narration, raw: candidate });
          buffer += cleaned.slice(0, brace.start);
          cleaned = cleaned.slice(i + 1);
          i = -1;
        }
        brace.start = -1;
        brace.depth = 0;
        inString = false;
      }
    }
  }
  buffer += cleaned;
  return { cleaned: buffer, narrations };
};

const tryParseToolNarration = (raw: string): { tool: string; arguments: unknown } | null => {
  const trimmed = raw.trim();
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) {
    return null;
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(trimmed);
  } catch {
    return null;
  }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return null;
  }
  const keys = Object.keys(parsed as Record<string, unknown>);
  if (keys.length === 0) {
    return null;
  }
  if (!keys.every((key) => TOOL_KEYS.has(key))) {
    return null;
  }
  const obj = parsed as Record<string, unknown>;
  const tool = pickString(obj.tool, obj.name, (obj.function as Record<string, unknown> | undefined)?.name);
  if (!tool) {
    return null;
  }
  const args =
    obj.arguments ??
    obj.parameters ??
    obj.args ??
    (obj.function as Record<string, unknown> | undefined)?.arguments ??
    {};
  return { tool, arguments: args };
};

const pickString = (...values: unknown[]): string | null => {
  for (const value of values) {
    if (typeof value === 'string' && value.trim().length > 0) {
      return value.trim();
    }
  }
  return null;
};

const isChartSpec = (value: unknown): value is ChartSpec => {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as ChartSpec;
  if (!Array.isArray(candidate.series) || candidate.series.length === 0) return false;
  return candidate.series.every((series) => Array.isArray(series.data));
};

/**
 * Strip the assistant message content down to a copy-friendly plain-text
 * representation: the markdown segments are concatenated in order, chart
 * fences are replaced with a one-liner placeholder, and any tool-call JSON
 * already extracted by {@link parseAssistantContent} is left out.
 */
export const toPlainText = (content: string | undefined | null): string => {
  const parsed = parseAssistantContent(content);
  const parts = parsed.segments.map((segment) => {
    if (segment.type === 'markdown') {
      return segment.text;
    }
    const points = segment.spec.series.reduce((sum, series) => sum + series.data.length, 0);
    const titleHint = segment.spec.title ? ` — ${segment.spec.title}` : '';
    return `[${segment.kind} chart${titleHint}, ${points} points]`;
  });
  return parts.join('\n\n').trim();
};
