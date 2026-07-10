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
  <el-dialog v-model="visible" :title="t('settings.mcp.connectionInfo')" width="640px">
    <el-descriptions :column="1" border>
      <el-descriptions-item :label="t('settings.mcp.serverUrl')">{{ mcpServerUrl }}</el-descriptions-item>
      <el-descriptions-item :label="t('settings.mcp.clientId')">{{ clientId || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('settings.mcp.issuer')">{{ metadata.issuer || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('settings.mcp.authorizationEndpoint')">
        {{ metadata.authorization_endpoint || '-' }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('settings.mcp.tokenEndpoint')">
        {{ metadata.token_endpoint || '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue';
import {useI18n} from 'vue-i18n';

import {getMcpMetadata} from '@/api/mcp';
import {MCP_SERVER_PATH} from '@/config/constant/api';
import type {McpConnectionRecord} from '@/config/types';

const {t} = useI18n();

const visible = ref(false);
const clientId = ref('');
const metadata = ref<Record<string, any>>({});

const mcpServerUrl = computed(() => `${window.location.origin}${MCP_SERVER_PATH}`);

const open = async (row: McpConnectionRecord) => {
  clientId.value = row.clientId || '';
  visible.value = true;
  const res = await getMcpMetadata();
  metadata.value = res.data || {};
};

defineExpose({open});
</script>
