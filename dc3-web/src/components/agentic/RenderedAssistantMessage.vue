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
      <div v-if="segment.type === 'markdown'" class="agentic-markdown" v-html="renderMarkdown(segment.text)"/>
      <ChartBlock v-else :kind="segment.kind" :spec="segment.spec"/>
    </template>
    <ChartBlock v-for="(chart, index) in charts" :key="chart.id || `${chart.type}-${index}`" :chart="chart"/>
  </div>
</template>

<script lang="ts" setup>
import {marked} from 'marked';
import DOMPurify from 'dompurify';
import {computed} from 'vue';
import ChartBlock from './ChartBlock.vue';
import {parseAssistantContent} from './assistantContent';
import type {AgenticVisualizationSpec} from '@/config/types';

const props = defineProps<{ content: string; charts?: AgenticVisualizationSpec[] }>();

const segments = computed(() => parseAssistantContent(props.content).segments);
const charts = computed(() => props.charts || []);

// Assistant content is model output rendered via v-html, so it must be sanitized with
// DOMPurify (script blocks, on* handlers, javascript:, data:/vbscript: schemes, mutation
// XSS). The previous hand-rolled regex sanitizer missed several of these (CodeQL
// js/bad-tag-filter, incomplete-*, url-scheme-check).
const renderMarkdown = (text: string) => {
  return DOMPurify.sanitize(String(marked.parse(text)), {USE_PROFILES: {html: true}});
};
</script>
