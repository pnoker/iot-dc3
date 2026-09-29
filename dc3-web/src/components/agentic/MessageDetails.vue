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
  <details v-if="hasDetails" class="agentic-details">
    <summary>
      <el-icon>
        <Cpu/>
      </el-icon>
      <span>{{ summary }}</span>
    </summary>

    <div class="agentic-details__body">
      <div v-if="runOverview.length" class="agentic-stat-grid">
        <StatTile v-for="item in runOverview" :key="item.label" :label="item.label" :value="item.value"/>
      </div>

      <ChatCardShell v-if="thinkingItems.length" :title="t('agentic.detailThinking')">
        <div class="agentic-thinking-list">
          <div v-for="item in thinkingItems" :key="item.label" class="agentic-thinking">
            <span>{{ item.label }}</span>
            <small>{{ item.detail }}</small>
          </div>
        </div>
      </ChatCardShell>

      <ChatCardShell v-if="toolSteps.length" :title="t('agentic.detailToolChain')">
        <ol class="agentic-tool-list">
          <ToolStepCard v-for="step in toolSteps" :key="step.id" :step="step"/>
        </ol>
      </ChatCardShell>

      <ChatCardShell v-if="tokenItems.length" :title="t('agentic.detailTokenUsage')">
        <template #status>
          <strong class="agentic-token-total">{{ tokenTotalLabel }}</strong>
        </template>
        <div class="agentic-stat-grid">
          <StatTile v-for="item in tokenItems" :key="item.label" :label="item.label" :value="item.value" layout="row"/>
        </div>
      </ChatCardShell>

      <ChatCardShell v-if="contexts.length" :title="t('agentic.detailContexts')">
        <div class="agentic-contexts">
          <div v-for="(context, index) in contexts" :key="`${context.type}-${index}`" class="agentic-context">
            <el-tag size="small" type="info">{{ context.type }}</el-tag>
            <pre>{{ context.content }}</pre>
          </div>
        </div>
      </ChatCardShell>
    </div>
  </details>
</template>

<script lang="ts" setup>
import {Cpu} from '@element-plus/icons-vue';
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';
import type {AgenticMessage, AgenticTraceEvent} from '@/config/types';
import {createAssistantDetails} from './assistantDetails';
import ChatCardShell from './cards/ChatCardShell.vue';
import StatTile from './cards/StatTile.vue';
import ToolStepCard from './cards/ToolStepCard.vue';

const props = defineProps<{message: AgenticMessage; liveTraces?: AgenticTraceEvent[]}>();

const {t} = useI18n();
const {
  assistantContexts,
  assistantDetailSummary,
  assistantRunOverview,
  assistantThinkingItems,
  assistantTokenItems,
  assistantTokenTotalLabel,
  assistantToolSteps,
  hasAssistantDetails,
} = createAssistantDetails({t, liveTraces: () => props.liveTraces || []});

const hasDetails = computed(() => hasAssistantDetails(props.message));
const summary = computed(() => assistantDetailSummary(props.message));
const runOverview = computed(() => assistantRunOverview(props.message));
const thinkingItems = computed(() => assistantThinkingItems(props.message));
const toolSteps = computed(() => assistantToolSteps(props.message));
const tokenItems = computed(() => assistantTokenItems(props.message));
const tokenTotalLabel = computed(() => assistantTokenTotalLabel(props.message));
const contexts = computed(() => assistantContexts(props.message));
</script>

<style lang="scss" scoped>
.agentic-details {
  max-width: 100%;
  min-width: 0;
  margin-top: 8px;
  border-top: 1px solid var(--dc3-border-base);

  summary {
    display: flex;
    align-items: center;
    gap: 6px;
    min-height: 28px;
    color: var(--dc3-text-regular);
    font-size: 12px;
    cursor: pointer;
    list-style: none;

    &::-webkit-details-marker {
      display: none;
    }

    &::after {
      content: '';
      width: 6px;
      height: 6px;
      margin-left: auto;
      border-right: 1px solid var(--dc3-text-muted);
      border-bottom: 1px solid var(--dc3-text-muted);
      transform: rotate(45deg);
      transition: transform 0.16s ease;
    }
  }

  &[open] summary::after {
    transform: rotate(225deg);
  }
}

.agentic-details__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 4px;
}

.agentic-stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(92px, 1fr));
  gap: 6px;
}

.agentic-token-total {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
}

.agentic-thinking-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.agentic-thinking {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  padding-left: 10px;
  border-left: 2px solid var(--el-color-primary-light-5);

  span {
    color: var(--dc3-text-primary);
    font-size: 12px;
    font-weight: 600;
  }

  small {
    color: var(--dc3-text-regular);
    font-size: 11px;
    line-height: 1.45;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
  }
}

.agentic-tool-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.agentic-contexts {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.agentic-context {
  display: flex;
  flex-direction: column;
  gap: 5px;

  pre {
    max-height: var(--dc3-snippet-max-height);
    margin: 0;
    padding: 8px;
    overflow: auto;
    border: 1px solid var(--dc3-border-base);
    border-radius: var(--dc3-radius-md);
    color: var(--dc3-text-primary);
    white-space: pre-wrap;
    background: var(--dc3-bg-muted);
  }
}

@media (prefers-reduced-motion: reduce) {
  .agentic-details summary::after {
    transition: none;
  }
}
</style>
