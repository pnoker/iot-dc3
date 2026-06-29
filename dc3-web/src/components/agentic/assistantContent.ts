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
 * Parse assistant message content into renderable markdown and chart segments.
 * Tool traces are rendered only from backend `agentic.event` records, never from
 * model text.
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

export interface ParsedAssistantContent {
  segments: AssistantSegment[];
}

const CHART_FENCE_RE = /```chart:(line|area|column)\s*\n([\s\S]*?)\n```/g;

/**
 * Single entry-point for the assistant message renderer. Returns the visible
 * segments (markdown + chart) in source order. Empty/whitespace input yields
 * an empty result instead of throwing.
 */
export const parseAssistantContent = (content: string | undefined | null): ParsedAssistantContent => {
  if (!content) {
    return {segments: []};
  }

  const segments: AssistantSegment[] = [];

  let cursor = 0;
  CHART_FENCE_RE.lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = CHART_FENCE_RE.exec(content)) !== null) {
    if (match.index > cursor) {
      pushMarkdown(segments, content.slice(cursor, match.index));
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
      segments.push({type: 'chart', kind, spec});
    } else {
      // Bad JSON — fall back to showing the fence as a normal code block
      segments.push({type: 'markdown', text: match[0]});
    }
    cursor = match.index + match[0].length;
  }
  if (cursor < content.length) {
    pushMarkdown(segments, content.slice(cursor));
  }

  return {segments};
};

const pushMarkdown = (segments: AssistantSegment[], text: string) => {
  if (text.trim().length > 0) {
    segments.push({type: 'markdown', text});
  }
};

const isChartSpec = (value: unknown): value is ChartSpec => {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as ChartSpec;
  if (!Array.isArray(candidate.series) || candidate.series.length === 0) return false;
  return candidate.series.every((series) => Array.isArray(series.data));
};

/**
 * Strip the assistant message content down to a copy-friendly plain-text
 * representation: the markdown segments are concatenated in order, and chart
 * fences are replaced with a one-liner placeholder.
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
