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
  <div class="mcp-overview">
    <el-card shadow="never">
      <template #header>
        <div class="mcp-overview__header">
          <span class="mcp-overview__title">{{ t('settings.mcp.title') }}</span>
          <el-tooltip :content="t('common.refresh')" effect="dark" placement="top">
            <el-button
              :aria-label="t('common.refresh')"
              :icon="Refresh"
              :loading="loading"
              circle
              @click="loadMetadata"
            />
          </el-tooltip>
        </div>
      </template>
      <el-alert
        v-if="loadError"
        :closable="false"
        :title="t('common.loadFailed')"
        class="mcp-overview__error"
        show-icon
        type="error"
      >
        <el-button :loading="loading" link type="danger" @click="loadMetadata">
          {{ t('common.retry') }}
        </el-button>
      </el-alert>
      <el-descriptions v-loading="loading" :aria-busy="loading" :column="isMobile ? 1 : 2" border>
        <el-descriptions-item :label="t('settings.mcp.serverUrl')">
          <div class="mcp-overview__copy-line">
            <span>{{ mcpServerUrl }}</span>
            <el-button :icon="DocumentCopy" link type="primary" @click="copy(mcpServerUrl, t('settings.mcp.copied'))">
              {{ t('settings.mcp.copyUrl') }}
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.mcp.issuer')">{{ metadata.issuer || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('settings.mcp.authorizationEndpoint')">
          {{ metadata.authorization_endpoint || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.mcp.tokenEndpoint')">
          {{ metadata.token_endpoint || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.mcp.jwksUri')" :span="2">
          {{ metadata.jwks_uri || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <span class="mcp-overview__title">{{ t('settings.mcp.agentConfig') }}</span>
      </template>
      <div v-for="agent in agentSnippets" :key="agent.name" class="mcp-overview__snippet">
        <div class="mcp-overview__snippet-head">
          <span class="mcp-overview__snippet-name">{{ agent.name }}</span>
          <el-button :icon="DocumentCopy" link type="primary" @click="copy(agent.config, t('settings.mcp.copied'))">
            {{ t('settings.mcp.copyUrl') }}
          </el-button>
        </div>
        <pre class="mcp-overview__snippet-code">{{ agent.config }}</pre>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {DocumentCopy, Refresh} from '@element-plus/icons-vue';

import {getMcpMetadata} from '@/api/mcp';
import {useBreakpoint} from '@/composables/useBreakpoint';
import {MCP_SERVER_PATH} from '@/config/constant/api';
import type {McpMetadata} from '@/config/types';
import {copy} from '@/utils/commonUtil';

const {t} = useI18n();
const {isMobile} = useBreakpoint();
const loading = ref(false);
const loadError = ref(false);
const metadata = ref<McpMetadata>({});
let latestLoadId = 0;

const mcpServerUrl = computed(() => `${window.location.origin}${MCP_SERVER_PATH}`);

// Ready-to-paste MCP client config snippets for common AI agents. The agents discover OAuth
// via the protected-resource metadata, so the snippet only needs the server URL.
const agentSnippets = computed(() => {
  const server = {type: 'http', url: mcpServerUrl.value};
  return [
    {name: 'Claude Desktop', config: JSON.stringify({mcpServers: {dc3: server}}, null, 2)},
    {name: 'Cursor', config: JSON.stringify({mcpServers: {dc3: server}}, null, 2)},
    {name: 'VS Code', config: JSON.stringify({servers: {dc3: server}}, null, 2)},
  ];
});

const loadMetadata = async () => {
  const loadId = ++latestLoadId;
  loading.value = true;
  loadError.value = false;
  try {
    const res = await getMcpMetadata();
    if (loadId !== latestLoadId) return;
    metadata.value = res || {};
  } catch {
    if (loadId === latestLoadId) loadError.value = true;
  } finally {
    if (loadId === latestLoadId) loading.value = false;
  }
};

onBeforeUnmount(() => {
  latestLoadId += 1;
});

void loadMetadata();
</script>

<style lang="scss" scoped>
.mcp-overview {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-3);
}

.mcp-overview__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--dc3-space-3);
}

.mcp-overview__title {
  font-size: 16px;
  font-weight: 600;
}

.mcp-overview__error {
  margin-bottom: var(--dc3-space-3);

  :deep(.el-alert__content) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--dc3-space-2);
  }
}

.mcp-overview__copy-line {
  display: flex;
  align-items: center;
  gap: var(--dc3-space-2);
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  flex-wrap: wrap;
}

.mcp-overview__copy-line span {
  min-width: 0;
  max-width: 100%;
  flex: 1 1 180px;
  overflow-wrap: anywhere;
  word-break: break-word;
  white-space: normal;
}

.mcp-overview__copy-line :deep(.el-button) {
  flex: 0 0 auto;
  margin: 0;
}

:deep(.el-descriptions),
:deep(.el-descriptions__body),
:deep(.el-descriptions__table) {
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

:deep(.el-descriptions__table) {
  table-layout: fixed;
}

:deep(.el-descriptions__label),
:deep(.el-descriptions__content) {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.mcp-overview__snippet {
  margin-top: var(--dc3-space-3);

  &:first-child {
    margin-top: 0;
  }
}

.mcp-overview__snippet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  margin-bottom: var(--dc3-space-1);
}

.mcp-overview__snippet-name {
  font-weight: 600;
}

.mcp-overview__snippet-code {
  margin: 0;
  padding: var(--dc3-space-2) var(--dc3-space-3);
  max-height: 180px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border-radius: var(--dc3-radius-sm);
  font-size: 12px;
  line-height: 1.5;
}
</style>
