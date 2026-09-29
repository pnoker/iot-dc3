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

<!-- One step of the tool chain: numbered index, tool name, detail, meta —
     with the running/success/failed badge that used to be flattened into the
     meta text. -->
<template>
  <li :class="`chat-tool-step--${statusTone}`" class="chat-tool-step">
    <span class="chat-tool-step__index">{{ step.index }}</span>
    <div class="chat-tool-step__content">
      <div class="chat-tool-step__line">
        <strong>{{ step.label }}</strong>
        <el-tag v-if="step.status" :type="statusTagType" effect="plain" size="small">
          {{ statusLabel }}
        </el-tag>
      </div>
      <small v-if="step.detail">{{ step.detail }}</small>
      <em v-if="step.meta">{{ step.meta }}</em>
    </div>
  </li>
</template>

<script lang="ts" setup>
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';
import type {AssistantChainStep} from '../assistantDetails';

const props = defineProps<{step: AssistantChainStep}>();
const {t} = useI18n();

const statusTone = computed(() => {
  if (props.step.status === 'failed') return 'failed';
  if (props.step.status === 'running') return 'running';
  return 'success';
});
const statusTagType = computed(() =>
  props.step.status === 'failed' ? 'danger' : props.step.status === 'running' ? 'warning' : 'success'
);
const statusLabel = computed(() =>
  props.step.status === 'failed'
    ? t('agentic.traceFailed')
    : props.step.status === 'running'
      ? t('agentic.traceRunning')
      : t('agentic.traceSuccess')
);
</script>

<style lang="scss" scoped>
.chat-tool-step {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 8px;
  align-items: flex-start;
  min-width: 0;
}

.chat-tool-step__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: var(--dc3-radius-full);
  color: var(--el-color-primary-dark-2);
  font-size: 11px;
  font-weight: 700;
  background: var(--el-color-primary-light-9);
}

.chat-tool-step--failed .chat-tool-step__index {
  color: var(--el-color-danger-dark-2);
  background: var(--el-color-danger-light-9);
}

.chat-tool-step__content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;

  strong,
  small,
  em {
    overflow-wrap: anywhere;
  }

  strong {
    color: var(--dc3-text-primary);
    font-size: 12px;
  }

  small {
    color: var(--dc3-text-regular);
    font-size: 11px;
    line-height: 1.45;
  }

  em {
    color: var(--dc3-text-regular);
    font-size: 11px;
    font-style: normal;
  }
}

.chat-tool-step__line {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;

  strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
