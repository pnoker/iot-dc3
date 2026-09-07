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
  <el-drawer
    v-model="visible"
    :before-close="requestClose"
    :close-on-click-modal="!submitting"
    :close-on-press-escape="!submitting"
    :size="isMobile ? '100%' : '720px'"
    :title="t('settings.mcp.manageTools')"
    class="manage-tools"
    destroy-on-close
    @closed="reset"
  >
    <div class="manage-tools__toolbar">
      <span>{{ t('settings.mcp.selectedTools', {count: selectedToolIds.length}) }}</span>
    </div>
    <el-alert
      v-if="loadError"
      :closable="false"
      :title="t('common.loadFailed')"
      class="manage-tools__alert"
      show-icon
      type="error"
    >
      <el-button :disabled="submitting" :loading="loading" link type="danger" @click="loadTools">
        {{ t('common.retry') }}
      </el-button>
    </el-alert>
    <el-alert
      v-if="submitError"
      :closable="false"
      :title="t('settings.mcp.saveFailed')"
      class="manage-tools__alert"
      show-icon
      type="error"
    />

    <div v-loading="loading" :aria-busy="loading || submitting" class="manage-tools__content" aria-live="polite">
      <template v-if="!loadError">
        <el-checkbox-group
          v-if="isMobile"
          v-model="selectedToolIds"
          :disabled="submitting"
          class="manage-tools__cards"
        >
          <label v-for="tool in tools" :key="tool.toolId" class="manage-tools__card">
            <el-checkbox :value="tool.toolId"/>
            <span class="manage-tools__card-content">
              <strong>{{ tool.toolName }}</strong>
              <span v-if="tool.permissionCode">{{ tool.permissionCode }}</span>
            </span>
            <el-tag :type="riskTag(tool.riskLevel)" size="small">{{ tool.riskLevel || '-' }}</el-tag>
          </label>
        </el-checkbox-group>
        <el-table
          v-else
          ref="toolTableRef"
          :data="tools"
          height="100%"
          row-key="toolId"
          stripe
          @selection-change="onSelectionChange"
        >
          <el-table-column :selectable="() => !loading && !submitting" type="selection" width="48"/>
          <el-table-column
            :label="t('settings.mcp.toolName')"
            min-width="220"
            prop="toolName"
            show-overflow-tooltip
          />
          <el-table-column :label="t('settings.mcp.riskLevel')" width="110">
            <template #default="{row}">
              <el-tag :type="riskTag(row.riskLevel)">{{ row.riskLevel || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            :label="t('settings.mcp.permissionCode')"
            min-width="240"
            prop="permissionCode"
            show-overflow-tooltip
          />
        </el-table>
        <el-empty v-if="!loading && tools.length === 0" :description="t('settings.mcp.empty')"/>
      </template>
    </div>

    <template #footer>
      <div class="manage-tools__footer">
        <el-button :disabled="submitting" @click="requestClose()">{{ t('common.cancel') }}</el-button>
        <el-button
          :disabled="loading || loadError"
          :loading="submitting"
          type="primary"
          @click="submit"
        >
          {{ t('common.save') }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script lang="ts" setup>
import {computed, nextTick, onBeforeUnmount, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {ElMessageBox, type TableInstance} from 'element-plus';

import {listMcpConnectionTool, listMcpTool, replaceMcpConnectionTools} from '@/api/mcp';
import {useBreakpoint} from '@/composables/useBreakpoint';
import {MCP_RISK_LEVELS} from '@/config/constant/enums';
import type {McpConnectionRecord, McpToolRecord} from '@/config/types';
import {successMessage} from '@/utils/notificationUtil';

const {t} = useI18n();
const {isMobile} = useBreakpoint();

const visible = ref(false);
const loading = ref(false);
const loadError = ref(false);
const submitError = ref(false);
const submitting = ref(false);
const tools = ref<McpToolRecord[]>([]);
const selectedToolIds = ref<string[]>([]);
const initialSelectedToolIds = ref<string[]>([]);
const connection = ref<McpConnectionRecord | null>(null);
const toolTableRef = ref<TableInstance>();
let requestSequence = 0;

const normalizedIds = (ids: string[]) => [...new Set(ids)].sort();
const isDirty = computed(() => {
  const current = normalizedIds(selectedToolIds.value);
  const initial = normalizedIds(initialSelectedToolIds.value);
  return current.length !== initial.length || current.some((id, index) => id !== initial[index]);
});

const riskTag = (riskLevel?: string) => {
  if (riskLevel === MCP_RISK_LEVELS.HIGH) return 'danger';
  if (riskLevel === MCP_RISK_LEVELS.MEDIUM) return 'warning';
  return 'success';
};

const onSelectionChange = (rows: McpToolRecord[]) => {
  if (loading.value || submitting.value) return;
  selectedToolIds.value = rows.map((row) => row.toolId);
};

const syncTableSelection = async () => {
  if (isMobile.value) return;
  const selected = new Set(selectedToolIds.value);
  await nextTick();
  toolTableRef.value?.clearSelection();
  for (const tool of tools.value) {
    if (selected.has(tool.toolId)) toolTableRef.value?.toggleRowSelection(tool, true);
  }
};

const loadTools = async () => {
  const activeConnection = connection.value;
  if (!activeConnection || submitting.value) return;
  const requestId = ++requestSequence;
  loadError.value = false;
  submitError.value = false;
  tools.value = [];
  selectedToolIds.value = [];
  initialSelectedToolIds.value = [];
  loading.value = true;
  try {
    const [toolRes, selectedRes] = await Promise.all([
      listMcpTool({offset: 0, limit: 200}),
      listMcpConnectionTool(activeConnection.id),
    ]);
    if (requestId !== requestSequence || connection.value?.id !== activeConnection.id || !visible.value) return;
    tools.value = toolRes?.items || [];
    selectedToolIds.value = selectedRes || [];
    initialSelectedToolIds.value = [...selectedToolIds.value];
    await syncTableSelection();
  } catch {
    if (requestId !== requestSequence || connection.value?.id !== activeConnection.id || !visible.value) return;
    loadError.value = true;
  } finally {
    if (requestId === requestSequence) loading.value = false;
  }
};

const open = (row: McpConnectionRecord) => {
  requestSequence += 1;
  connection.value = row;
  tools.value = [];
  selectedToolIds.value = [];
  initialSelectedToolIds.value = [];
  loadError.value = false;
  submitError.value = false;
  visible.value = true;
  void loadTools();
};

const submit = async () => {
  const activeConnection = connection.value;
  if (!activeConnection || submitting.value) return;
  const requestId = requestSequence;
  submitError.value = false;
  submitting.value = true;
  try {
    await replaceMcpConnectionTools(activeConnection.id, selectedToolIds.value);
    if (requestId !== requestSequence || connection.value?.id !== activeConnection.id || !visible.value) return;
    initialSelectedToolIds.value = [...selectedToolIds.value];
    successMessage(t('settings.mcp.saved'));
    visible.value = false;
  } catch {
    if (requestId === requestSequence && connection.value?.id === activeConnection.id && visible.value) {
      submitError.value = true;
    }
  } finally {
    if (requestId === requestSequence) submitting.value = false;
  }
};

const requestClose = async (done?: () => void) => {
  if (submitting.value) return;
  const requestId = requestSequence;
  if (!isDirty.value) {
    if (done) done();
    else visible.value = false;
    return;
  }
  try {
    await ElMessageBox.confirm(t('common.discardConfirm'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    });
    if (requestId !== requestSequence || !visible.value) return;
    if (done) done();
    else visible.value = false;
  } catch {
    // Keep the current selection when the user cancels the confirmation.
  }
};

const reset = () => {
  requestSequence += 1;
  loading.value = false;
  submitting.value = false;
  loadError.value = false;
  submitError.value = false;
  tools.value = [];
  selectedToolIds.value = [];
  initialSelectedToolIds.value = [];
  connection.value = null;
};

onBeforeUnmount(() => {
  requestSequence += 1;
  submitting.value = false;
});

watch(isMobile, (mobile) => {
  if (!mobile && visible.value && !loading.value && !loadError.value) void syncTableSelection();
});

defineExpose({open});
</script>

<style lang="scss" scoped>
.manage-tools {
  :deep(.el-drawer__body) {
    display: flex;
    min-height: 0;
    flex-direction: column;
    padding-bottom: var(--dc3-space-3);
  }
}

.manage-tools__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--dc3-space-2);
  margin-bottom: var(--dc3-space-3);
  color: var(--dc3-text-regular);
}

.manage-tools__alert {
  flex: 0 0 auto;
  margin-bottom: var(--dc3-space-3);
}

.manage-tools__content {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.manage-tools__cards {
  display: flex;
  height: 100%;
  flex-direction: column;
  gap: var(--dc3-space-2);
  overflow-y: auto;
}

.manage-tools__card {
  display: flex;
  align-items: center;
  gap: var(--dc3-space-3);
  min-height: var(--dc3-touch-target);
  padding: var(--dc3-space-3);
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-lg);
  background: var(--dc3-bg-elevated);
  cursor: pointer;
}

.manage-tools__card-content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: var(--dc3-space-1);

  strong,
  span {
    overflow-wrap: anywhere;
  }

  span {
    color: var(--dc3-text-muted);
    font-size: var(--el-font-size-small);
  }
}

.manage-tools__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--dc3-space-2);
}
</style>
