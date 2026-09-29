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

/**
 * Parse assistant message content into renderable markdown and chart segments.
 * Tool traces are rendered only from backend `agentic.event` records, never from
 * model text.
 */

/**
 * Chart series.
 */
export interface ChartSeries {
  name?: string;
  data: Array<[number | string, number]>;
}

/**
 * Chart specification.
 */
export interface ChartSpec {
  title?: string;
  unit?: string;
  xLabel?: string;
  yLabel?: string;
  xType?: 'time' | 'category' | 'linear';
  series: ChartSeries[];
}

/**
 * Chart segment.
 */
export interface ChartSegment {
  type: 'chart';
  kind: 'line' | 'area' | 'column';
  spec: ChartSpec;
}

/**
 * Markdown segment.
 */
export interface MarkdownSegment {
  type: 'markdown';
  text: string;
}

/**
 * assistant segment type alias.
 */
export type AssistantSegment = MarkdownSegment | ChartSegment;

/**
 * ParsedAssistantContent data contract.
 */
export interface ParsedAssistantContent {
  segments: AssistantSegment[];
}

const CHART_FENCE_RE = /```chart:(line|area|column)\s*\n([\s\S]*?)\n```/g;

/**
 * Single entry-point for the assistant message renderer. Returns the visible
 * segments (markdown + chart) in source order. Empty/whitespace input yields
 * an empty result instead of throwing.
 * @param content - text content to process
 * @returns the transformed value
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
 * @param content - text content to process
 * @returns the plain-text rendering
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

/**
 * Strips markdown markup so a quoted passage previews as plain text.
 *
 * @param content raw message content (markdown)
 * @returns the plain-text excerpt
 */
export const quoteExcerpt = (content: string) => {
  return toPlainText(content)
    .replace(/^\s{0,3}#{1,6}\s+/gm, '')
    .replace(/^\s*(?:[-*+] |\d+\.\s+)/gm, '')
    .replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
    .replace(/[*_`~]+/g, '')
    .replace(/\s+/g, ' ')
    .trim();
};

/**
 * Split pieces of a user message body: an optional quoted block
 * (`> **label**` header followed by `>` lines) and the free text after it.
 */
export interface UserMessageParts {
  body: string;
  label?: string;
  quote?: string;
}

/**
 * Parses a user message into its quote and body parts.
 *
 * @param content raw user message content
 * @returns the parsed parts
 */
export const userMessageParts = (content?: string | null): UserMessageParts => {
  // Legacy rows can carry a null/empty content — never let the render path
  // throw over one bad message (a render crash hides the whole panel).
  if (!content) {
    return {body: ''};
  }
  const lines = content.split('\n');
  const labelMatch = lines[0]?.match(/^> \*\*(.+)\*\*$/);
  if (!labelMatch) {
    return {body: content};
  }

  const quoteLines: string[] = [];
  let index = 1;
  while (index < lines.length) {
    const line = lines[index];
    if (!line?.startsWith('>')) break;
    quoteLines.push(line.replace(/^> ?/, ''));
    index += 1;
  }

  return {
    label: labelMatch[1],
    quote: quoteExcerpt(quoteLines.join('\n')),
    body: lines.slice(index).join('\n').trim(),
  };
};
