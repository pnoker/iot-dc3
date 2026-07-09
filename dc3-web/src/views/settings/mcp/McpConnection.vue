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
  <div>
    <entity-list-page ref="listRef" :config="config"/>
    <add-connection-dialog ref="addRef" @saved="reload"/>
    <connection-info-dialog ref="infoRef"/>
    <manage-tools-drawer ref="toolsRef"/>
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
