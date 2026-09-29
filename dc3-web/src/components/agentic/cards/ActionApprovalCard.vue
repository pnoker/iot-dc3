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

<!-- Pending-action approval card. Risk tone follows the action kind (writes to
     the physical world read louder than paperwork) and the structured payload
     is shown as key/value rows instead of being buried in free text. -->
<template>
  <ChatCardShell :tone="riskTone" :title="action.title">
    <template #icon>
      <el-icon><component :is="actionIcon(action.actionType)"/></el-icon>
    </template>
    <template #status>
      <el-tag :type="riskTagType" size="small">{{ t('agentic.pending') }}</el-tag>
    </template>
    <div class="chat-action__content">
      <span class="chat-action__desc">{{ action.description }}</span>
      <dl v-if="payloadRows.length" class="chat-action__payload">
        <template v-for="row in payloadRows" :key="row.key">
          <dt>{{ row.key }}</dt>
          <dd>{{ row.value }}</dd>
        </template>
      </dl>
      <span v-if="action.expireTime" class="chat-action__expire">
        {{ t('agentic.actionExpireAt') }} {{ formatShortDateTime(action.expireTime) }}
      </span>
      <span v-if="error" class="chat-action__error" role="alert">{{ t('agentic.actionFailed') }}</span>
    </div>
    <template #footer>
      <div class="chat-action__buttons">
        <el-button
          :disabled="locked"
          :loading="loading"
          size="small"
          type="primary"
          @click="emit('confirm', action.actionId)"
        >
          <el-icon>
            <Check/>
          </el-icon>
          {{ t('agentic.confirm') }}
        </el-button>
        <el-button :disabled="locked" :loading="loading" size="small" @click="emit('reject', action.actionId)">
          <el-icon>
            <CircleClose/>
          </el-icon>
          {{ t('agentic.reject') }}
        </el-button>
      </div>
    </template>
  </ChatCardShell>
</template>

<script lang="ts" setup>
import {Check, CircleClose, DataAnalysis, Lightning, Setting, Tools} from '@element-plus/icons-vue';
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';
import type {AgenticAction} from '@/config/types';
import {formatShortDateTime} from '@/utils/timeUtil';
import ChatCardShell from './ChatCardShell.vue';

const props = defineProps<{
  action: AgenticAction;
  loading?: boolean;
  locked?: boolean;
  error?: boolean;
}>();
const emit = defineEmits<{confirm: [actionId: string]; reject: [actionId: string]}>();
const {t} = useI18n();

// Writes to the physical world read louder than paperwork.
const riskTone = computed(() => {
  const type = props.action.actionType || '';
  if (type.includes('WRITE')) return 'danger';
  if (type.includes('CONFIG') || type.includes('WORK_ORDER')) return 'warn';
  return 'info';
});
const riskTagType = computed(() =>
  riskTone.value === 'danger' ? 'danger' : riskTone.value === 'warn' ? 'warning' : 'info'
);

const actionIcon = (actionType: string) => {
  if (actionType.includes('WRITE')) return Lightning;
  if (actionType.includes('CONFIG')) return Setting;
  if (actionType.includes('WORK_ORDER')) return Tools;
  return DataAnalysis;
};

const payloadRows = computed(() =>
  Object.entries(props.action.payload || {})
    .filter(([, value]) => value !== null && value !== undefined && value !== '')
    .slice(0, 8)
    .map(([key, value]) => ({
      key,
      value: typeof value === 'object' ? JSON.stringify(value) : String(value),
    }))
);
</script>

<style lang="scss" scoped>
.chat-action__content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.chat-action__desc {
  color: var(--dc3-text-regular);
  overflow-wrap: anywhere;
}

.chat-action__payload {
  display: grid;
  grid-template-columns: minmax(64px, auto) minmax(0, 1fr);
  gap: 2px 8px;
  margin: 0;

  dt {
    color: var(--dc3-text-muted);
    font-size: 11px;
  }

  dd {
    margin: 0;
    color: var(--dc3-text-primary);
    font-size: 11px;
    overflow-wrap: anywhere;
  }
}

.chat-action__expire {
  color: var(--dc3-text-muted);
  font-size: 11px;
}

.chat-action__error {
  color: var(--el-color-danger-dark-2);
  font-size: 11px;
}

.chat-action__buttons {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}
</style>
