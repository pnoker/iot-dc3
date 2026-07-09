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
            <el-button :icon="Refresh" :loading="loading" circle @click="loadMetadata"/>
          </el-tooltip>
        </div>
      </template>
      <el-descriptions :column="2" border>
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
import {computed, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {DocumentCopy, Refresh} from '@element-plus/icons-vue';

import {getMcpMetadata} from '@/api/mcp';
import {MCP_SERVER_PATH} from '@/config/constant/api';
import {copy} from '@/utils/commonUtil';

const {t} = useI18n();
const loading = ref(false);
const metadata = ref<Record<string, any>>({});

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
  loading.value = true;
  try {
    const res = await getMcpMetadata();
    metadata.value = res.data || {};
  } finally {
    loading.value = false;
  }
};

loadMetadata();
</script>

<style lang="scss" scoped>
.mcp-overview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mcp-overview__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.mcp-overview__title {
  font-size: 16px;
  font-weight: 600;
}

.mcp-overview__copy-line {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.mcp-overview__copy-line span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mcp-overview__snippet {
  margin-top: 12px;

  &:first-child {
    margin-top: 0;
  }
}

.mcp-overview__snippet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.mcp-overview__snippet-name {
  font-weight: 600;
}

.mcp-overview__snippet-code {
  margin: 0;
  padding: 10px 12px;
  max-height: 180px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
}
</style>
