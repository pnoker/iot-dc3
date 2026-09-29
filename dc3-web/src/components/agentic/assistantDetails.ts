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

// Message-derived view models for the assistant detail surfaces: the
// in-message `details.agentic-details` footnote and the workbench context
// panel both consume these, so the extraction logic lives here once.
//
// The functions close over two injected dependencies instead of reaching
// into the store or i18n directly: `t` for labels and `liveTraces` for the
// streaming trace events of the active conversation (persisted traces come
// from the message itself). That keeps the module unit-testable.

import type {
  AgenticMessage,
  AgenticMessageContext,
  AgenticMessageTokens,
  AgenticTraceEvent,
} from '@/config/types';

/** A label/value pair of the run-overview grid. */
export interface AssistantRunStat {
  label: string;
  value: string;
}

/** A thinking timeline entry of the detail footnote. */
export interface AssistantThinkingItem {
  label: string;
  detail: string;
}

/** One numbered step of the tool chain. */
export interface AssistantChainStep {
  id: string;
  index: number;
  label: string;
  detail?: string;
  meta?: string;
  status?: AgenticTraceEvent['status'];
}

/** A label/value pair of the token-usage grid. */
export interface AssistantTokenItem {
  label: string;
  value: string;
}

/** Minimal i18n translate signature used for detail labels. */
export type AssistantTranslate = (key: string, params?: Record<string, unknown>) => string;

/** Injected dependencies for the detail derivations. */
export interface AssistantDetailsDeps {
  t: AssistantTranslate;
  /** Live streaming traces; pass an empty array when the message is done. */
  liveTraces: () => AgenticTraceEvent[];
}

const formatCount = (value: number) => {
  return new Intl.NumberFormat('en-US').format(value);
};

const uniqueStrings = (values: string[]) => {
  return Array.from(new Set(values.filter(Boolean)));
};

const traceKey = (event: AgenticTraceEvent) => {
  return [
    event.type,
    event.name || '',
    event.phase || '',
    event.status || '',
    event.code || '',
    event.title || '',
    event.detail || '',
  ].join('|');
};

const uniqueTraceEvents = (events: AgenticTraceEvent[]) => {
  const seen = new Set<string>();
  return events.filter((event) => {
    const key = traceKey(event);
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
};

const uniqueChainSteps = (steps: AssistantChainStep[]) => {
  const seen = new Set<string>();
  return steps.filter((step) => {
    const key = [step.label, step.status || '', step.detail || '', step.meta || ''].join('|');
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
};

const indexChainSteps = (steps: AssistantChainStep[]) => {
  return steps.map((step, index) => ({
    ...step,
    id: `${step.id}-${index}`,
    index: index + 1,
  }));
};

const uniqueThinkingItems = (items: AssistantThinkingItem[]) => {
  const seen = new Set<string>();
  return items.filter((item) => {
    const key = `${item.label}|${item.detail}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
};

/**
 * Builds the message-detail derivations bound to the given dependencies.
 *
 * @param deps translate function and live-trace provider
 * @returns the detail view-model functions for a message
 */
export const createAssistantDetails = (deps: AssistantDetailsDeps) => {
  const {t} = deps;

  const messageTraceEvents = (message: AgenticMessage) => {
    return message.streaming ? deps.liveTraces() : [];
  };

  const assistantTraceEvents = (message: AgenticMessage): AgenticTraceEvent[] => {
    return uniqueTraceEvents([...(message.contentExt?.traces || []), ...messageTraceEvents(message)]);
  };

  const assistantReasoningText = (message: AgenticMessage) => {
    const directReasoning = [message.reasoning, message.contentExt?.reasoningContent].filter((value): value is string =>
      Boolean(value)
    );
    const traceReasoning = assistantTraceEvents(message)
      .filter((event) => event.type === 'reasoning')
      .map((event) => event.detail || event.title)
      .filter((value): value is string => Boolean(value));
    return uniqueStrings([...directReasoning, ...traceReasoning])
      .join('\n')
      .trim();
  };

  const assistantReasoning = (message: AgenticMessage) => {
    return Boolean(
      assistantReasoningText(message) ||
        message.contentExt?.reasoning ||
        assistantTraceEvents(message).some((event) => event.type === 'reasoning')
    );
  };

  const assistantTools = (message: AgenticMessage) => {
    const persisted = message.contentExt?.tools || [];
    const streaming = messageTraceEvents(message)
      .filter((event) => event.type === 'tool')
      .map((event) => event.name || event.title)
      .filter(Boolean);
    return uniqueStrings([...persisted, ...streaming]);
  };

  const toolEventRank = (event: AgenticTraceEvent) => {
    if (event.phase === 'error' || event.status === 'failed') return 4;
    if (event.phase === 'result') return 3;
    if (event.phase === 'start') return 2;
    return 1;
  };

  const toolTraceMeta = (event: AgenticTraceEvent, detail?: string) => {
    const seen = new Set<string>();
    const parts = [event.status, event.code, event.detail].filter((part): part is string => {
      if (!part || part === detail || seen.has(part)) return false;
      seen.add(part);
      return true;
    });
    return parts.length ? parts.join(' · ') : undefined;
  };

  const groupedToolTraceEvents = (message: AgenticMessage): AgenticTraceEvent[] => {
    const grouped = new Map<string, AgenticTraceEvent>();
    assistantTraceEvents(message)
      .filter((event) => event.type === 'tool')
      .forEach((event) => {
        const key = event.name || event.title || traceKey(event);
        const current = grouped.get(key);
        if (!current || toolEventRank(event) >= toolEventRank(current)) {
          grouped.set(key, event);
        }
      });
    return Array.from(grouped.values());
  };

  const assistantToolSteps = (message: AgenticMessage): AssistantChainStep[] => {
    const traceSteps = groupedToolTraceEvents(message).map((event) => {
      const label = event.name || event.title || 'tool';
      const detail = event.title;
      const meta = toolTraceMeta(event, detail);
      return {
        id: traceKey(event),
        index: 0,
        label,
        detail,
        meta,
        status: event.status,
      };
    });
    const tracedToolLabels = new Set(traceSteps.map((step) => step.label));
    const fallbackSteps = assistantTools(message)
      .filter((tool) => !tracedToolLabels.has(tool))
      .map((tool) => ({
        id: `tool-${tool}`,
        index: 0,
        label: tool,
        detail: '',
      }));
    return indexChainSteps(uniqueChainSteps([...traceSteps, ...fallbackSteps]));
  };

  const assistantContexts = (message: AgenticMessage): AgenticMessageContext[] => {
    return message.contentExt?.contexts || [];
  };

  const assistantTokens = (message: AgenticMessage): AgenticMessageTokens | undefined => {
    return message.contentExt?.tokens;
  };

  const assistantTokenTotal = (message: AgenticMessage) => {
    const tokens = assistantTokens(message);
    if (!tokens) {
      return undefined;
    }
    const input = typeof tokens.input === 'number' ? tokens.input : 0;
    const output = typeof tokens.output === 'number' ? tokens.output : 0;
    return input + output > 0 ? input + output : undefined;
  };

  const assistantTokenTotalLabel = (message: AgenticMessage) => {
    const total = assistantTokenTotal(message);
    return typeof total === 'number' ? t('agentic.detailTotal', {n: formatCount(total)}) : t('agentic.detailPending');
  };

  const assistantTokenItems = (message: AgenticMessage): AssistantTokenItem[] => {
    const tokens = assistantTokens(message);
    if (!tokens) {
      return [];
    }
    const tokenOrder: Array<[keyof AgenticMessageTokens, string]> = [
      ['input', t('agentic.tokenInput')],
      ['output', t('agentic.tokenOutput')],
      ['text', t('agentic.tokenText')],
      ['context', t('agentic.tokenContext')],
      ['system', t('agentic.tokenSystem')],
      ['memory', t('agentic.tokenMemory')],
    ];
    return tokenOrder
      .filter(([key]) => typeof tokens[key] === 'number')
      .map(([key, label]) => ({label, value: formatCount(tokens[key] || 0)}));
  };

  const assistantStatus = (message: AgenticMessage) => {
    if (message.streaming) {
      return message.content ? t('agentic.statusStreaming') : t('agentic.statusThinking');
    }
    if (message.status === 'CANCELLED') {
      return t('agentic.statusCancelled');
    }
    if (message.status === 'FAILED') {
      return t('agentic.statusFailed');
    }
    const reason = message.finishReason?.toLowerCase();
    if (reason === 'cancelled' || reason === 'canceled') {
      return t('agentic.statusCancelled');
    }
    if (reason === 'error' || reason === 'failed') {
      return t('agentic.statusFailed');
    }
    if (message.contentExt?.recovered) {
      return t('agentic.statusDone');
    }
    if (assistantTraceEvents(message).some((event) => event.type === 'error' || event.status === 'failed')) {
      return t('agentic.statusFailed');
    }
    return t('agentic.statusDone');
  };

  const assistantRunOverview = (message: AgenticMessage): AssistantRunStat[] => {
    const stats: AssistantRunStat[] = [];
    const tools = assistantToolSteps(message).length;
    const tokenTotal = assistantTokenTotal(message);
    if (assistantReasoning(message)) {
      stats.push({label: t('agentic.thinkingReasoningMode'), value: t('agentic.statusEnabled')});
    }
    if (tools > 0) {
      stats.push({label: t('agentic.detailToolChain'), value: String(tools)});
    }
    if (typeof tokenTotal === 'number') {
      stats.push({label: t('agentic.detailTokenUsage'), value: formatCount(tokenTotal)});
    }
    return stats;
  };

  const assistantThinkingItems = (message: AgenticMessage): AssistantThinkingItem[] => {
    const items: AssistantThinkingItem[] = [];
    const traces = assistantTraceEvents(message);
    const tools = assistantToolSteps(message).length;
    if (message.streaming) {
      items.push({
        label: message.content ? t('agentic.thinkingGenerating') : t('agentic.thinkingPreparing'),
        detail: traces.length ? t('agentic.thinkingTraceCollecting') : t('agentic.thinkingWaitingChunk'),
      });
    }
    if (tools > 0) {
      items.push({
        label: t('agentic.thinkingToolExec'),
        detail: t('agentic.thinkingToolDetail', {n: tools}),
      });
    }
    return uniqueThinkingItems(items);
  };

  const hasAssistantDetails = (message: AgenticMessage) => {
    return (
      assistantRunOverview(message).length > 0 ||
      assistantThinkingItems(message).length > 0 ||
      assistantToolSteps(message).length > 0 ||
      assistantReasoning(message) ||
      assistantContexts(message).length > 0 ||
      assistantTokenItems(message).length > 0
    );
  };

  const assistantDetailSummary = (message: AgenticMessage) => {
    const parts: string[] = [];
    const tools = assistantToolSteps(message);
    const contexts = assistantContexts(message);
    const tokenTotal = assistantTokenTotal(message);
    parts.push(`${t('agentic.statusLabel')} ${assistantStatus(message)}`);
    if (assistantReasoning(message)) {
      parts.push(t('agentic.thinkingReasoningMode').toLowerCase());
    }
    if (tools.length) {
      parts.push(t('agentic.detailSummaryTools', {n: tools.length}));
    }
    if (contexts.length) {
      parts.push(t('agentic.detailContexts'));
    }
    if (typeof tokenTotal === 'number') {
      parts.push(t('agentic.detailSummaryTokens', {n: formatCount(tokenTotal)}));
    }
    return parts.join(' · ');
  };

  const hasReasoningPanel = (message: AgenticMessage) => {
    return Boolean(message.streaming || assistantReasoningText(message));
  };

  const isReasoningPanelOpen = (message: AgenticMessage) => {
    return Boolean(message.streaming);
  };

  const reasoningPanelStatus = (message: AgenticMessage) => {
    if (message.streaming) {
      return assistantReasoningText(message) ? t('agentic.statusStreaming') : t('agentic.statusThinking');
    }
    return t('agentic.statusDone');
  };

  const truncatedReason = (message: AgenticMessage): string => {
    const reason = message.finishReason?.toLowerCase();
    if (!reason || reason === 'stop') {
      return '';
    }
    if (reason === 'length') {
      return t('agentic.finishLength');
    }
    if (reason === 'content_filter') {
      return t('agentic.finishContentFilter');
    }
    if (reason === 'tool_calls') {
      return t('agentic.finishToolCalls');
    }
    return t('agentic.finishOther', {reason});
  };

  return {
    assistantContexts,
    assistantDetailSummary,
    assistantReasoning,
    assistantReasoningText,
    assistantRunOverview,
    assistantStatus,
    assistantThinkingItems,
    assistantTokenItems,
    assistantTokenTotal,
    assistantTokenTotalLabel,
    assistantTokens,
    assistantToolSteps,
    assistantTools,
    assistantTraceEvents,
    hasAssistantDetails,
    hasReasoningPanel,
    isReasoningPanelOpen,
    reasoningPanelStatus,
    truncatedReason,
  };
};
