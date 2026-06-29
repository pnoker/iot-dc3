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
