<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<template>
  <div class="agentic-rendered">
    <template v-for="(segment, index) in segments" :key="index">
      <div
        v-if="segment.type === 'markdown' && !isLiveTail(index)"
        class="agentic-markdown"
        v-html="renderMarkdown(segment.text)"
      />
      <!-- Live tail while streaming: the trailing markdown segment is still
           being written, so it renders as plain text instead of re-parsing an
           unclosed document on every delta flush. The completion pass renders
           the full formatted body. -->
      <div v-else-if="segment.type === 'markdown'" class="agentic-markdown agentic-markdown--raw">
        {{ segment.text }}
      </div>
      <ChartBlock v-else :kind="segment.kind" :spec="segment.spec"/>
    </template>
    <ChartBlock v-for="(chart, index) in charts" :key="chart.id || `${chart.type}-${index}`" :chart="chart"/>
  </div>
</template>

<script lang="ts" setup>
import {marked} from 'marked';
import DOMPurify from 'dompurify';
import {computed, watch} from 'vue';
import ChartBlock from './ChartBlock.vue';
import {parseAssistantContent} from './assistantContent';
import type {AgenticVisualizationSpec} from '@/config/types';

const props = defineProps<{
  content: string;
  charts?: AgenticVisualizationSpec[];
  /** True while the assistant message is still streaming in. */
  streaming?: boolean;
}>();

const segments = computed(() => parseAssistantContent(props.content).segments);
const charts = computed(() => props.charts || []);

// The trailing markdown segment is the one still being written while the
// message streams; everything before it is closed content.
const isLiveTail = (index: number) => {
  return Boolean(props.streaming) && index === segments.value.length - 1 && segments.value[index]?.type === 'markdown';
};

// Memoized markdown → sanitize pipeline. Streaming appends to the tail only,
// so closed segments keep their exact text and re-render from cache instead
// of re-running marked + DOMPurify on every delta flush. Cached values are
// the same deterministic output the uncached call produces — sanitization is
// skipped for a given text at most within one streaming message's lifetime,
// and the cache is dropped when the stream settles so the final frame always
// re-parses and re-sanitizes the whole message once.
const renderedHtmlCache = new Map<string, string>();
const renderMarkdown = (text: string) => {
  const cached = renderedHtmlCache.get(text);
  if (cached !== undefined) return cached;
  const html = sanitizeMarkdown(text);
  renderedHtmlCache.set(text, html);
  return html;
};

// Assistant content is model output rendered via v-html, so it must be sanitized with
// DOMPurify (script blocks, on* handlers, javascript:, data:/vbscript: schemes, mutation
// XSS). The previous hand-rolled regex sanitizer missed several of these (CodeQL
// js/bad-tag-filter, incomplete-*, url-scheme-check). Form controls are forbidden
// alongside the active-content vectors: model output must not be able to spoof
// login or input chrome inside the conversation.
const sanitizeMarkdown = (text: string) => {
  return DOMPurify.sanitize(String(marked.parse(text)), {
    USE_PROFILES: {html: true},
    FORBID_TAGS: ['form', 'input', 'button', 'select', 'textarea', 'option'],
  });
};

watch(
  () => props.streaming,
  (streaming, wasStreaming) => {
    // The stream settled: drop the cache so the completed message re-renders
    // through a full parse + sanitize pass (the security posture of the
    // non-streaming path is never relaxed by streaming).
    if (wasStreaming && !streaming) {
      renderedHtmlCache.clear();
    }
  }
);
</script>

<style lang="scss" scoped>
// Raw streaming tail: keep the model's line breaks visible before the final
// markdown pass renders them (matches how user text is displayed).
.agentic-markdown--raw {
  white-space: pre-wrap;
}
</style>
