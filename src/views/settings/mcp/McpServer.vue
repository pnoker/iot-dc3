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
  <div class="mcp-page">
    <div class="mcp-toolbar">
      <div class="mcp-toolbar__title">{{ t('settings.mcp.title') }}</div>
      <div class="mcp-toolbar__actions">
        <el-button :icon="Refresh" :loading="reactiveData.loading" @click="loadAll" />
        <el-button :icon="Plus" type="primary" @click="openClientDialog">
          {{ t('settings.mcp.registerClient') }}
        </el-button>
        <el-button :icon="LinkIcon" type="primary" @click="openConnectionDialog">
          {{ t('settings.mcp.addConnection') }}
        </el-button>
        <el-button :icon="RefreshRight" :loading="reactiveData.refreshingCatalog" @click="refreshCatalog">
          {{ t('settings.mcp.refreshCatalog') }}
        </el-button>
      </div>
    </div>

    <blank-card>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('settings.mcp.serverUrl')">
          <div class="mcp-copy-line">
            <span>{{ mcpServerUrl }}</span>
            <el-button :icon="DocumentCopy" link type="primary" @click="copy(mcpServerUrl, t('settings.mcp.copied'))">
              {{ t('settings.mcp.copyUrl') }}
            </el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.mcp.issuer')">
          {{ reactiveData.metadata.issuer || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.mcp.authorizationEndpoint')">
          {{ reactiveData.metadata.authorization_endpoint || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.mcp.tokenEndpoint')">
          {{ reactiveData.metadata.token_endpoint || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.mcp.jwksUri')" :span="2">
          {{ reactiveData.metadata.jwks_uri || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </blank-card>

    <blank-card>
      <el-tabs v-model="reactiveData.active">
        <el-tab-pane :label="t('settings.mcp.connections')" name="connections">
          <el-table v-loading="reactiveData.loading" :data="reactiveData.connections" stripe>
            <el-table-column :label="t('settings.mcp.connectionName')" min-width="180" prop="connectionName" />
            <el-table-column
              :label="t('settings.mcp.clientId')"
              min-width="220"
              prop="clientId"
              show-overflow-tooltip
            />
            <el-table-column :label="t('settings.mcp.principalId')" min-width="150" prop="principalId" />
            <el-table-column :label="t('settings.mcp.principalType')" min-width="150" prop="principalType" />
            <el-table-column :label="t('settings.mcp.grantType')" min-width="180" prop="grantType" />
            <el-table-column :label="t('common.enable')" width="90">
              <template #default="{ row }">
                <enable-tag :value="row.enableFlag" />
              </template>
            </el-table-column>
            <el-table-column :label="t('settings.mcp.lastUsedTime')" min-width="170">
              <template #default="{ row }">{{ timestampLabel(row.lastUsedTime) }}</template>
            </el-table-column>
            <el-table-column :label="t('settings.mcp.revokeTime')" min-width="170">
              <template #default="{ row }">{{ timestampLabel(row.revokeTime) }}</template>
            </el-table-column>
            <el-table-column :label="t('common.operation')" fixed="right" width="190">
              <template #default="{ row }">
                <el-button link type="primary" @click="openToolsDrawer(row)">
                  {{ t('settings.mcp.manageTools') }}
                </el-button>
                <el-popconfirm
                  :cancel-button-text="t('common.cancel')"
                  :confirm-button-text="t('common.confirm')"
                  :title="t('settings.mcp.revoke')"
                  @confirm="revokeConnection(row)"
                >
                  <template #reference>
                    <el-button link type="danger">{{ t('settings.mcp.revoke') }}</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty :description="t('settings.mcp.empty')" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('settings.mcp.clients')" name="clients">
          <el-table v-loading="reactiveData.loading" :data="reactiveData.clients" stripe>
            <el-table-column :label="t('settings.mcp.clientName')" min-width="180" prop="clientName" />
            <el-table-column
              :label="t('settings.mcp.clientId')"
              min-width="240"
              prop="clientId"
              show-overflow-tooltip
            />
            <el-table-column :label="t('settings.mcp.clientType')" min-width="130" prop="clientType" />
            <el-table-column :label="t('settings.mcp.grantTypes')" min-width="220" prop="authorizationGrantTypes" />
            <el-table-column :label="t('settings.mcp.scopes')" min-width="240" prop="scopes" show-overflow-tooltip />
            <el-table-column :label="t('common.enable')" width="90">
              <template #default="{ row }">
                <enable-tag :value="row.enableFlag" />
              </template>
            </el-table-column>
            <template #empty>
              <el-empty :description="t('settings.mcp.empty')" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('settings.mcp.tools')" name="tools">
          <div class="mcp-tool-filter">
            <el-input
              v-model="reactiveData.toolKeyword"
              :placeholder="t('settings.mcp.keyword')"
              clearable
              @keyup.enter="loadTools"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="reactiveData.toolRisk" clearable>
              <el-option v-for="opt in MCP_RISK_LEVEL_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-button :icon="Search" @click="loadTools" />
          </div>
          <el-table v-loading="reactiveData.toolsLoading" :data="reactiveData.tools" stripe>
            <el-table-column
              :label="t('settings.mcp.toolName')"
              min-width="220"
              prop="toolName"
              show-overflow-tooltip
            />
            <el-table-column
              :label="t('settings.mcp.toolTitle')"
              min-width="180"
              prop="toolTitle"
              show-overflow-tooltip
            />
            <el-table-column :label="t('settings.mcp.serviceName')" min-width="150" prop="serviceName" />
            <el-table-column :label="t('settings.mcp.riskLevel')" width="110">
              <template #default="{ row }">
                <el-tag :type="riskTag(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              :label="t('settings.mcp.permissionCode')"
              min-width="260"
              prop="permissionCode"
              show-overflow-tooltip
            />
            <el-table-column :label="t('settings.mcp.httpMethod')" width="95" prop="httpMethod" />
            <el-table-column :label="t('settings.mcp.apiPath')" min-width="220" prop="apiPath" show-overflow-tooltip />
            <template #empty>
              <el-empty :description="t('settings.mcp.empty')" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </blank-card>

    <el-dialog v-model="reactiveData.clientDialogVisible" :title="t('settings.mcp.registerClient')" width="680px">
      <el-form :model="reactiveData.clientForm" label-width="190px">
        <el-form-item :label="t('settings.mcp.clientName')">
          <el-input v-model="reactiveData.clientForm.client_name" />
        </el-form-item>
        <el-form-item :label="t('settings.mcp.clientType')">
          <el-segmented v-model="reactiveData.clientForm.client_type" :options="MCP_CLIENT_TYPE_OPTIONS" />
        </el-form-item>
        <el-form-item :label="t('settings.mcp.grantTypes')">
          <el-select v-model="reactiveData.clientGrantTypes" multiple>
            <el-option v-for="opt in MCP_GRANT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.mcp.redirectUris')">
          <el-input v-model="reactiveData.redirectUrisText" type="textarea" />
        </el-form-item>
        <el-form-item :label="t('settings.mcp.scopes')">
          <el-select v-model="reactiveData.clientScopes" multiple>
            <el-option v-for="opt in MCP_SCOPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.mcp.tenantId')">
          <el-input v-model="reactiveData.clientForm.tenant_id" />
        </el-form-item>
        <el-form-item :label="t('settings.mcp.serviceAccountPrincipalId')">
          <el-input v-model="reactiveData.clientForm.service_account_principal_id" />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="reactiveData.registeredSecret"
        :closable="false"
        :title="`${t('settings.mcp.clientSecret')}: ${reactiveData.registeredSecret}`"
        show-icon
        type="success"
      />
      <template #footer>
        <el-button @click="reactiveData.clientDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="reactiveData.submitting" type="primary" @click="submitClient">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reactiveData.connectionDialogVisible" :title="t('settings.mcp.addConnection')" width="640px">
      <el-form :model="reactiveData.connectionForm" label-width="170px">
        <el-form-item :label="t('settings.mcp.connectionName')">
          <el-input v-model="reactiveData.connectionForm.connectionName" />
        </el-form-item>
        <el-form-item :label="t('settings.mcp.clientId')">
          <el-select v-model="reactiveData.connectionForm.clientId" filterable>
            <el-option
              v-for="client in reactiveData.clients"
              :key="client.clientId"
              :label="`${client.clientName} / ${client.clientId}`"
              :value="client.clientId"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.mcp.grantType')">
          <el-segmented v-model="reactiveData.connectionForm.grantType" :options="MCP_GRANT_TYPE_OPTIONS" />
        </el-form-item>
        <el-form-item :label="t('settings.mcp.principalType')">
          <el-segmented v-model="reactiveData.connectionForm.principalType" :options="MCP_PRINCIPAL_TYPE_OPTIONS" />
        </el-form-item>
        <el-form-item :label="t('settings.mcp.principalId')">
          <el-input v-model="reactiveData.connectionForm.principalId" />
        </el-form-item>
        <el-form-item :label="t('settings.mcp.tenantId')">
          <el-input v-model="reactiveData.connectionForm.tenantId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reactiveData.connectionDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button :loading="reactiveData.submitting" type="primary" @click="submitConnection">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="reactiveData.toolsDrawerVisible" :title="t('settings.mcp.manageTools')" size="720px">
      <div class="mcp-drawer-toolbar">
        <span>{{ t('settings.mcp.selectedTools', { count: reactiveData.selectedToolIds.length }) }}</span>
        <el-button :loading="reactiveData.submitting" type="primary" @click="submitConnectionTools">
          {{ t('common.save') }}
        </el-button>
      </div>
      <el-table
        ref="toolTableRef"
        v-loading="reactiveData.toolsLoading"
        :data="reactiveData.tools"
        height="calc(100vh - 190px)"
        row-key="toolId"
        stripe
        @selection-change="onToolSelectionChange"
      >
        <el-table-column type="selection" width="42" />
        <el-table-column :label="t('settings.mcp.toolName')" min-width="220" prop="toolName" show-overflow-tooltip />
        <el-table-column :label="t('settings.mcp.riskLevel')" width="110">
          <template #default="{ row }">
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
  </div>
</template>

<script lang="ts" setup>
  import { computed, nextTick, reactive, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { DocumentCopy, Link as LinkIcon, Plus, Refresh, RefreshRight, Search } from '@element-plus/icons-vue';
  import type { ElTable } from 'element-plus';

  import {
    addMcpConnection,
    getMcpMetadata,
    listMcpClient,
    listMcpConnection,
    listMcpConnectionTool,
    listMcpTool,
    refreshMcpToolCatalog,
    registerMcpClient,
    replaceMcpConnectionTools,
    revokeMcpConnection,
  } from '@/api/mcp';
  import type { McpConnectionForm, McpConnectionRecord, McpToolRecord, OAuthClientRecord } from '@/config/types';
  import { MCP_SERVER_PATH } from '@/config/constant/api';
  import {
    MCP_CLIENT_TYPE_OPTIONS,
    MCP_CLIENT_TYPES,
    MCP_GRANT_TYPE_OPTIONS,
    MCP_GRANT_TYPES,
    MCP_PRINCIPAL_TYPE_OPTIONS,
    MCP_PRINCIPAL_TYPES,
    MCP_RISK_LEVEL_OPTIONS,
    MCP_RISK_LEVELS,
    MCP_SCOPE_OPTIONS,
    MCP_SCOPES,
  } from '@/config/constant/enums';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import EnableTag from '@/components/tag/EnableTag.vue';
  import { copy } from '@/utils/commonUtil';
  import { successMessage } from '@/utils/notificationUtil';
  import { timestampLabel } from '@/utils/dateUtil';

  const { t } = useI18n();
  const toolTableRef = ref<InstanceType<typeof ElTable>>();

  const reactiveData = reactive({
    active: 'connections',
    loading: false,
    toolsLoading: false,
    refreshingCatalog: false,
    submitting: false,
    metadata: {} as Record<string, any>,
    clients: [] as OAuthClientRecord[],
    connections: [] as McpConnectionRecord[],
    tools: [] as McpToolRecord[],
    toolKeyword: '',
    toolRisk: '',
    clientDialogVisible: false,
    connectionDialogVisible: false,
    toolsDrawerVisible: false,
    clientForm: {
      client_name: '',
      client_type: MCP_CLIENT_TYPES.PUBLIC,
      tenant_id: '',
      service_account_principal_id: '',
    } as Record<string, any>,
    clientGrantTypes: [MCP_GRANT_TYPES.AUTHORIZATION_CODE],
    clientScopes: [MCP_SCOPES.TOOLS_LIST, MCP_SCOPES.TOOLS_CALL],
    redirectUrisText: '',
    registeredSecret: '',
    connectionForm: {
      connectionName: '',
      clientId: '',
      principalId: '',
      principalType: MCP_PRINCIPAL_TYPES.USER,
      tenantId: '',
      grantType: MCP_GRANT_TYPES.AUTHORIZATION_CODE,
    } as McpConnectionForm,
    selectedConnection: null as McpConnectionRecord | null,
    selectedToolIds: [] as string[],
  });

  const mcpServerUrl = computed(() => `${window.location.origin}${MCP_SERVER_PATH}`);

  const splitText = (value: string) =>
    value
      .split(/[\s,]+/)
      .map((item) => item.trim())
      .filter(Boolean);

  const riskTag = (riskLevel?: string) => {
    if (riskLevel === MCP_RISK_LEVELS.HIGH) return 'danger';
    if (riskLevel === MCP_RISK_LEVELS.MEDIUM) return 'warning';
    return 'success';
  };

  const loadTools = async () => {
    reactiveData.toolsLoading = true;
    try {
      const res = await listMcpTool({
        keyword: reactiveData.toolKeyword,
        risk_level: reactiveData.toolRisk,
        limit: 500,
      });
      reactiveData.tools = res.data || [];
    } finally {
      reactiveData.toolsLoading = false;
    }
  };

  const loadAll = async () => {
    reactiveData.loading = true;
    try {
      const [metadataRes, clientRes, connectionRes] = await Promise.all([
        getMcpMetadata(),
        listMcpClient(),
        listMcpConnection(),
      ]);
      reactiveData.metadata = metadataRes.data || {};
      reactiveData.clients = clientRes.data || [];
      reactiveData.connections = connectionRes.data || [];
      await loadTools();
    } finally {
      reactiveData.loading = false;
    }
  };

  const openClientDialog = () => {
    reactiveData.clientForm = {
      client_name: '',
      client_type: MCP_CLIENT_TYPES.PUBLIC,
      tenant_id: '',
      service_account_principal_id: '',
    };
    reactiveData.clientGrantTypes = [MCP_GRANT_TYPES.AUTHORIZATION_CODE];
    reactiveData.clientScopes = [MCP_SCOPES.TOOLS_LIST, MCP_SCOPES.TOOLS_CALL];
    reactiveData.redirectUrisText = `${window.location.origin}/oauth/callback`;
    reactiveData.registeredSecret = '';
    reactiveData.clientDialogVisible = true;
  };

  const submitClient = async () => {
    reactiveData.submitting = true;
    try {
      const res = await registerMcpClient({
        ...reactiveData.clientForm,
        grant_types: reactiveData.clientGrantTypes,
        redirect_uris: splitText(reactiveData.redirectUrisText),
        scope: reactiveData.clientScopes,
      });
      reactiveData.registeredSecret = String(res.data?.client_secret || '');
      successMessage(t('settings.mcp.saved'));
      await loadAll();
      if (!reactiveData.registeredSecret) {
        reactiveData.clientDialogVisible = false;
      }
    } finally {
      reactiveData.submitting = false;
    }
  };

  const openConnectionDialog = () => {
    reactiveData.connectionForm = {
      connectionName: '',
      clientId: reactiveData.clients[0]?.clientId || '',
      principalId: '',
      principalType: MCP_PRINCIPAL_TYPES.USER,
      tenantId: '',
      grantType: MCP_GRANT_TYPES.AUTHORIZATION_CODE,
    };
    reactiveData.connectionDialogVisible = true;
  };

  const submitConnection = async () => {
    reactiveData.submitting = true;
    try {
      await addMcpConnection(reactiveData.connectionForm);
      successMessage(t('settings.mcp.saved'));
      reactiveData.connectionDialogVisible = false;
      await loadAll();
    } finally {
      reactiveData.submitting = false;
    }
  };

  const revokeConnection = async (row: McpConnectionRecord) => {
    await revokeMcpConnection(row.id);
    successMessage(t('settings.mcp.saved'));
    await loadAll();
  };

  const openToolsDrawer = async (row: McpConnectionRecord) => {
    reactiveData.selectedConnection = row;
    reactiveData.selectedToolIds = [];
    reactiveData.toolsDrawerVisible = true;
    const res = await listMcpConnectionTool(row.id);
    reactiveData.selectedToolIds = res.data || [];
    await nextTick();
    toolTableRef.value?.clearSelection();
    const selected = new Set(reactiveData.selectedToolIds);
    for (const tool of reactiveData.tools) {
      if (selected.has(tool.toolId)) {
        toolTableRef.value?.toggleRowSelection(tool, true);
      }
    }
  };

  const onToolSelectionChange = (rows: McpToolRecord[]) => {
    reactiveData.selectedToolIds = rows.map((row) => row.toolId);
  };

  const submitConnectionTools = async () => {
    if (!reactiveData.selectedConnection) return;
    reactiveData.submitting = true;
    try {
      await replaceMcpConnectionTools(reactiveData.selectedConnection.id, reactiveData.selectedToolIds);
      successMessage(t('settings.mcp.saved'));
      reactiveData.toolsDrawerVisible = false;
    } finally {
      reactiveData.submitting = false;
    }
  };

  const refreshCatalog = async () => {
    reactiveData.refreshingCatalog = true;
    try {
      const res = await refreshMcpToolCatalog();
      successMessage(t('settings.mcp.refreshed', { count: res.data || 0 }));
      await loadTools();
    } finally {
      reactiveData.refreshingCatalog = false;
    }
  };

  loadAll();
</script>

<style lang="scss" scoped>
  .mcp-page {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .mcp-toolbar,
  .mcp-tool-filter,
  .mcp-drawer-toolbar,
  .mcp-copy-line {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .mcp-toolbar {
    justify-content: space-between;
  }

  .mcp-toolbar__title {
    font-size: 18px;
    font-weight: 600;
  }

  .mcp-toolbar__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .mcp-tool-filter {
    margin-bottom: 12px;
  }

  .mcp-tool-filter .el-input {
    max-width: 420px;
  }

  .mcp-tool-filter .el-select {
    width: 150px;
  }

  .mcp-drawer-toolbar {
    justify-content: space-between;
    margin-bottom: 12px;
  }

  .mcp-copy-line {
    min-width: 0;
  }

  .mcp-copy-line span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  @media (max-width: 768px) {
    .mcp-toolbar,
    .mcp-tool-filter {
      align-items: stretch;
      flex-direction: column;
    }

    .mcp-toolbar__actions {
      justify-content: flex-start;
    }

    .mcp-tool-filter .el-input,
    .mcp-tool-filter .el-select {
      max-width: none;
      width: 100%;
    }
  }
</style>
