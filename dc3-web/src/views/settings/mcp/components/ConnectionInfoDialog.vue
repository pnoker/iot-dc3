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
    :aria-busy="loading"
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    :title="t('settings.mcp.connectionInfo')"
    class="things-dialog"
    destroy-on-close
    :width="isMobile ? 'calc(100% - 16px)' : '640px'"
    @closed="invalidateRequest"
  >
    <el-alert
      v-if="loadError"
      :closable="false"
      :title="t('common.loadFailed')"
      class="connection-info__alert"
      show-icon
      type="error"
    >
      <el-button :loading="loading" link type="danger" @click="loadMetadata">
        {{ t('common.retry') }}
      </el-button>
    </el-alert>
    <el-descriptions v-loading="loading" :column="1" border>
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
    <template #footer>
      <el-button :disabled="loading" @click="visible = false">{{ t('common.close') }}</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, ref} from 'vue';
import {useI18n} from 'vue-i18n';

import {getMcpMetadata} from '@/api/mcp';
import {useBreakpoint} from '@/composables/useBreakpoint';
import {MCP_SERVER_PATH} from '@/config/constant/api';
import type {McpConnectionRecord, McpMetadata} from '@/config/types';

const {t} = useI18n();
const {isMobile} = useBreakpoint();

const visible = ref(false);
const loading = ref(false);
const loadError = ref(false);
const clientId = ref('');
const metadata = ref<McpMetadata>({});
let requestSequence = 0;

const mcpServerUrl = computed(() => `${window.location.origin}${MCP_SERVER_PATH}`);

const invalidateRequest = () => {
  requestSequence += 1;
  loading.value = false;
};

const loadMetadata = async () => {
  const requestId = ++requestSequence;
  loading.value = true;
  loadError.value = false;
  try {
    const res = await getMcpMetadata();
    if (requestId !== requestSequence || !visible.value) return;
    metadata.value = res || {};
  } catch {
    if (requestId !== requestSequence || !visible.value) return;
    loadError.value = true;
  } finally {
    if (requestId === requestSequence) loading.value = false;
  }
};

const open = (row: McpConnectionRecord) => {
  invalidateRequest();
  clientId.value = row.clientId || '';
  metadata.value = {};
  loadError.value = false;
  visible.value = true;
  void loadMetadata();
};

onBeforeUnmount(() => {
  invalidateRequest();
});

defineExpose({open});
</script>

<style lang="scss" scoped>
.connection-info__alert {
  margin-bottom: var(--dc3-space-3);
}
</style>
