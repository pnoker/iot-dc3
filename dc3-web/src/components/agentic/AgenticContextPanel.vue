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
  <aside class="agentic-context">
    <div class="agentic-context__header">
      <span>{{ t('agentic.contextPanel') }}</span>
      <el-button
        :aria-label="t('agentic.contextPanel')"
        circle
        class="agentic-tool"
        size="small"
        @click="emit('toggle')"
      >
        <el-icon>
          <Close/>
        </el-icon>
      </el-button>
    </div>

    <el-scrollbar class="agentic-context__body">
      <!-- Session card — the workbench's always-visible conversation meta. -->
      <section v-if="session" class="agentic-context__session">
        <div class="agentic-context__session-title">
          <el-icon class="agentic-sessions__item-icon">
            <component :is="sessionIcon(session.sessionExt?.icon)"/>
          </el-icon>
          <strong>{{ session.title }}</strong>
        </div>
        <small v-if="session.summary" class="agentic-context__session-summary">{{ session.summary }}</small>
        <div class="agentic-context__chips">
          <el-tag v-if="modelLabel" size="small">{{ modelLabel }}</el-tag>
          <el-tag v-if="session.sessionExt?.reasoningEnabled" size="small" type="success"
          >{{ t('agentic.reasoning') }}
          </el-tag>
          <el-tag v-if="typeof session.sessionExt?.temperature === 'number'" size="small" type="info"
          >{{ t('agentic.temperature') }} {{ session.sessionExt.temperature }}
          </el-tag>
          <el-tag v-if="session.sessionExt?.maxTokens" size="small" type="info"
          >{{ t('agentic.maxTokens') }} {{ session.sessionExt.maxTokens }}
          </el-tag>
        </div>
      </section>

      <!-- Focused message run — the detail footnote, seen large. -->
      <section v-if="message && hasDetails" class="agentic-context__run">
        <div v-if="runOverview.length" class="agentic-stat-grid">
          <StatTile v-for="item in runOverview" :key="item.label" :label="item.label" :value="item.value"/>
        </div>

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
            <div v-for="(context, index) in contexts" :key="`${context.type}-${index}`" class="agentic-context-item">
              <el-tag size="small" type="info">{{ context.type }}</el-tag>
              <pre>{{ context.content }}</pre>
            </div>
          </div>
        </ChatCardShell>
      </section>

      <div v-else class="agentic-context__empty">
        {{ t('agentic.contextPanelEmpty') }}
      </div>
    </el-scrollbar>
  </aside>
</template>

<script lang="ts" setup>
import {Close} from '@element-plus/icons-vue';
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';
import type {AgenticMessage, AgenticSession, AgenticTraceEvent} from '@/config/types';
import {createAssistantDetails} from './assistantDetails';
import ChatCardShell from './cards/ChatCardShell.vue';
import StatTile from './cards/StatTile.vue';
import ToolStepCard from './cards/ToolStepCard.vue';
import {sessionIcon} from './sessionIcons';

const props = defineProps<{
  session?: AgenticSession & {title: string};
  message?: AgenticMessage;
  modelLabel?: string;
  liveTraces: AgenticTraceEvent[];
}>();
const emit = defineEmits<{toggle: []}>();

const {t} = useI18n();
const {
  assistantContexts,
  assistantRunOverview,
  assistantTokenItems,
  assistantTokenTotalLabel,
  assistantToolSteps,
  hasAssistantDetails,
} = createAssistantDetails({t, liveTraces: () => props.liveTraces});

const hasDetails = computed(() => Boolean(props.message && hasAssistantDetails(props.message)));
const runOverview = computed(() => (props.message ? assistantRunOverview(props.message) : []));
const toolSteps = computed(() => (props.message ? assistantToolSteps(props.message) : []));
const tokenItems = computed(() => (props.message ? assistantTokenItems(props.message) : []));
const tokenTotalLabel = computed(() => (props.message ? assistantTokenTotalLabel(props.message) : ''));
const contexts = computed(() => (props.message ? assistantContexts(props.message) : []));
</script>

<style lang="scss" scoped>
.agentic-context {
  display: flex;
  flex-direction: column;
  flex: 0 0 var(--agentic-context-width, 300px);
  width: var(--agentic-context-width, 300px);
  min-width: 0;
  min-height: 0;
  border-left: 1px solid var(--dc3-border-base);
  background: var(--dc3-bg-canvas);
}

.agentic-context__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--dc3-space-3) var(--dc3-space-3) var(--dc3-space-2);
  color: var(--dc3-text-muted);
  font-size: 12px;
  font-weight: 600;
}

.agentic-context__body {
  flex: 1;
  min-height: 0;
  padding: 0 var(--dc3-space-3) var(--dc3-space-3);
}

.agentic-context__session {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-2);
  padding: var(--dc3-space-3);
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-md);
  background: var(--dc3-bg-elevated-strong);
}

.agentic-context__session-title {
  display: flex;
  align-items: center;
  gap: var(--dc3-space-2);
  min-width: 0;

  strong {
    overflow: hidden;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.agentic-context__session-summary {
  color: var(--dc3-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.agentic-context__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
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

.agentic-tool-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.agentic-context__run {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: var(--dc3-space-3);
}

.agentic-context__block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.agentic-context__empty {
  padding: var(--dc3-space-6) var(--dc3-space-3);
  color: var(--dc3-text-muted);
  font-size: 12px;
  text-align: center;
}











.agentic-contexts {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.agentic-context-item {
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

// Narrow workbench: the panel overlays the chat column instead of squeezing it.
@media (max-width: $breakpoint-sm-max) {
  .agentic-context {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 2;
    box-shadow: var(--dc3-shadow-md);
  }
}
</style>
