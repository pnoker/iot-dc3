<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div class="agentic-rendered">
    <template v-for="(segment, index) in segments" :key="index">
      <div v-if="segment.type === 'markdown'" class="agentic-markdown" v-html="renderMarkdown(segment.text)" />
      <ChartBlock v-else :kind="segment.kind" :spec="segment.spec" />
    </template>
    <ChartBlock v-for="(chart, index) in charts" :key="chart.id || `${chart.type}-${index}`" :chart="chart" />
  </div>
</template>

<script lang="ts" setup>
  import {marked} from 'marked';
  import {computed} from 'vue';
  import ChartBlock from './ChartBlock.vue';
  import {parseAssistantContent} from './assistantContent';
  import type {AgenticVisualizationSpec} from '@/config/types';

  const props = defineProps<{content: string; charts?: AgenticVisualizationSpec[]}>();

  const segments = computed(() => parseAssistantContent(props.content).segments);
  const charts = computed(() => props.charts || []);

  const renderMarkdown = (text: string) => {
    return sanitizeHtml(String(marked.parse(text)));
  };

  const sanitizeHtml = (html: string) => {
    return html
      .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
      .replace(/\son\w+="[^"]*"/gi, '')
      .replace(/\son\w+='[^']*'/gi, '')
      .replace(/javascript:/gi, '');
  };
</script>
