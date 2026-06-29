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
  <div>
    <entity-list-page ref="listRef" :config="config" />
    <add-connection-dialog ref="addRef" @saved="reload" />
    <connection-info-dialog ref="infoRef" />
    <manage-tools-drawer ref="toolsRef" />
  </div>
</template>

<script lang="ts" setup>
  import {ref} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {ElMessageBox} from 'element-plus';

  import {revokeMcpConnection} from '@/api/mcp';
  import EntityListPage from '@/components/entity/EntityListPage.vue';
  import type {McpConnectionRecord} from '@/config/types';
  import {successMessage} from '@/utils/notificationUtil';

  import AddConnectionDialog from './components/AddConnectionDialog.vue';
  import ConnectionInfoDialog from './components/ConnectionInfoDialog.vue';
  import ManageToolsDrawer from './components/ManageToolsDrawer.vue';
  import {createMcpConnectionConfig} from './mcpConnectionConfig';

  const {t} = useI18n();
  const listRef = ref<InstanceType<typeof EntityListPage>>();
  const addRef = ref<InstanceType<typeof AddConnectionDialog>>();
  const infoRef = ref<InstanceType<typeof ConnectionInfoDialog>>();
  const toolsRef = ref<InstanceType<typeof ManageToolsDrawer>>();

  const reload = () => listRef.value?.reload();

  const onRevoke = async (row: Record<string, any>) => {
    try {
      await ElMessageBox.confirm(t('settings.mcp.revoke'), t('settings.mcp.connectionInfo'), {
        type: 'warning',
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
      });
    } catch {
      return; // cancelled
    }
    await revokeMcpConnection((row as McpConnectionRecord).id);
    successMessage(t('settings.mcp.saved'));
    reload();
  };

  const config = createMcpConnectionConfig(t, {
    onAddConnection: () => addRef.value?.open(),
    onConnectionInfo: (row) => infoRef.value?.open(row as McpConnectionRecord),
    onManageTools: (row) => toolsRef.value?.open(row as McpConnectionRecord),
    onRevoke,
  });
</script>
