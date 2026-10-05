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
  <el-dialog
    v-model="visible"
    :append-to-body="true"
    :title="$t('settings.agentic.checkResult')"
    class="things-dialog"
    destroy-on-close
    draggable
    width="560px"
  >
    <el-alert
      :closable="false"
      :title="result?.overall === 'PASS' ? $t('settings.agentic.checkOverallPass') : $t('settings.agentic.checkOverallFail')"
      :type="result?.overall === 'PASS' ? 'success' : 'error'"
      show-icon
    />
    <div v-if="result" class="check-levels">
      <div v-for="level in levelResults" :key="level.key" class="check-level">
        <div class="check-level-head">
          <span class="check-level-name">{{ level.title }}</span>
          <el-tag :type="statusTagType(level.result.status)" size="small">
            {{ $t(`settings.agentic.status${level.result.status}`) }}
          </el-tag>
          <span v-if="level.result.status !== 'SKIPPED'" class="check-level-latency">
            {{ $t('settings.agentic.checkLatency', {ms: level.result.latencyMs ?? 0}) }}
          </span>
        </div>
        <div v-if="level.result.status === 'FAIL'" class="check-level-error">
          <span class="check-error-type">{{ errorTypeLabel(level.result.errorType) }}</span>
          <span class="check-error-hint">{{ errorTypeHint(level.result.errorType) }}</span>
        </div>
        <el-collapse v-if="collapsibleContent(level.result)" class="check-detail">
          <el-collapse-item :title="$t('settings.agentic.rawError')" name="raw">
            <span v-if="level.result.message" class="check-raw">{{ level.result.message }}</span>
            <span v-if="level.result.upstreamStatus" class="check-raw-status">
              HTTP {{ level.result.upstreamStatus }}
            </span>
          </el-collapse-item>
          <el-collapse-item
            v-if="level.key === 'l1' && level.result.models?.length"
            :title="$t('settings.agentic.checkModels', {count: level.result.models.length})"
            name="models"
          >
            <el-tag v-for="model in level.result.models" :key="model" class="check-model-tag" size="small">
              {{ model }}
            </el-tag>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>
    <div v-if="result?.checkedAt" class="check-footer">
      {{ $t('settings.agentic.checkedAt') }}: {{ formatTime(result.checkedAt) }}
    </div>
    <template #footer>
      <div class="things-dialog-footer">
        <el-button type="primary" @click="visible = false">{{ $t('common.confirm') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue';
import {useI18n} from 'vue-i18n';

import type {AgenticCheckLevelResult, AgenticProviderCheckResult} from '@/config/types';

const {t} = useI18n();

const visible = ref(false);
const result = ref<AgenticProviderCheckResult | null>(null);

const levelResults = computed(() => {
  if (!result.value) return [];
  return [
    {key: 'l1', title: t('settings.agentic.checkL1'), result: result.value.l1},
    {key: 'l2', title: t('settings.agentic.checkL2'), result: result.value.l2},
  ];
});

const show = (checkResult: AgenticProviderCheckResult) => {
  result.value = checkResult;
  visible.value = true;
};

defineExpose({show});

const statusTagType = (status?: string) => {
  if (status === 'PASS') return 'success';
  if (status === 'FAIL') return 'danger';
  return 'info';
};

const errorTypeLabel = (errorType?: string) =>
  errorType ? t(`settings.agentic.errorType.${errorType}`, errorType) : '';

const errorTypeHint = (errorType?: string) =>
  errorType ? t(`settings.agentic.errorTypeHint.${errorType}`, '') : '';

const collapsibleContent = (level: AgenticCheckLevelResult) =>
  Boolean(level.message || level.upstreamStatus || level.models?.length);

const formatTime = (value: string) => String(value).replace('T', ' ').slice(0, 19);
</script>

<style scoped>
.check-levels {
  margin-top: 12px;
  display: grid;
  gap: 12px;
}

.check-level-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.check-level-name {
  font-weight: 600;
}

.check-level-latency {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.check-level-error {
  margin-top: 4px;
  display: grid;
  gap: 2px;
}

.check-error-type {
  color: var(--el-color-danger);
  font-weight: 600;
}

.check-error-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.check-detail {
  margin-top: 4px;
}

.check-raw {
  word-break: break-all;
}

.check-raw-status {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
}

.check-model-tag {
  margin: 0 4px 4px 0;
}

.check-footer {
  margin-top: 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
