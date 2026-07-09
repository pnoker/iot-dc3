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
  <el-drawer v-model="visible" :title="t('settings.mcp.manageTools')" size="720px">
    <div class="manage-tools__toolbar">
      <span>{{ t('settings.mcp.selectedTools', {count: selectedToolIds.length}) }}</span>
      <el-button :loading="submitting" type="primary" @click="submit">{{ t('common.save') }}</el-button>
    </div>
    <el-table
      ref="toolTableRef"
      v-loading="loading"
      :data="tools"
      height="calc(100vh - 190px)"
      row-key="toolId"
      stripe
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="42"/>
      <el-table-column :label="t('settings.mcp.toolName')" min-width="220" prop="toolName" show-overflow-tooltip/>
      <el-table-column :label="t('settings.mcp.riskLevel')" width="110">
        <template #default="{row}">
          <el-tag :type="riskTag(row.riskLevel)">{{ row.riskLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('settings.mcp.permissionCode')"
        min-width="240"
        prop="permissionCode"
        show-overflow-tooltip
      />
    </el-table>
  </el-drawer>
</template>

<script lang="ts" setup>
import {nextTick, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import type {TableInstance} from 'element-plus';

import {listMcpConnectionTool, listMcpTool, replaceMcpConnectionTools} from '@/api/mcp';
import {MCP_RISK_LEVELS} from '@/config/constant/enums';
import type {McpConnectionRecord, McpToolRecord} from '@/config/types';
import {successMessage} from '@/utils/notificationUtil';

const {t} = useI18n();

const visible = ref(false);
const loading = ref(false);
const submitting = ref(false);
const tools = ref<McpToolRecord[]>([]);
const selectedToolIds = ref<string[]>([]);
const connection = ref<McpConnectionRecord | null>(null);
const toolTableRef = ref<TableInstance>();

const riskTag = (riskLevel?: string) => {
  if (riskLevel === MCP_RISK_LEVELS.HIGH) return 'danger';
  if (riskLevel === MCP_RISK_LEVELS.MEDIUM) return 'warning';
  return 'success';
};

const onSelectionChange = (rows: McpToolRecord[]) => {
  selectedToolIds.value = rows.map((row) => row.toolId);
};

const open = async (row: McpConnectionRecord) => {
  connection.value = row;
  selectedToolIds.value = [];
  visible.value = true;
  loading.value = true;
  try {
    const [toolRes, selectedRes] = await Promise.all([listMcpTool({limit: 500}), listMcpConnectionTool(row.id)]);
    tools.value = toolRes.data || [];
    selectedToolIds.value = selectedRes.data || [];
  } finally {
    loading.value = false;
  }
  await nextTick();
  toolTableRef.value?.clearSelection();
  const selected = new Set(selectedToolIds.value);
  for (const tool of tools.value) {
    if (selected.has(tool.toolId)) toolTableRef.value?.toggleRowSelection(tool, true);
  }
};

const submit = async () => {
  if (!connection.value) return;
  submitting.value = true;
  try {
    await replaceMcpConnectionTools(connection.value.id, selectedToolIds.value);
    successMessage(t('settings.mcp.saved'));
    visible.value = false;
  } finally {
    submitting.value = false;
  }
};

defineExpose({open});
</script>

<style lang="scss" scoped>
.manage-tools__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}
</style>
