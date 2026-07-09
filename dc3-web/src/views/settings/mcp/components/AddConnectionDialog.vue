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
  <el-dialog v-model="visible" :title="t('settings.mcp.addConnection')" width="640px">
    <el-form :model="form" label-width="170px">
      <el-form-item :label="t('settings.mcp.connectionName')">
        <el-input v-model="form.connectionName"/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.clientId')">
        <el-select v-model="form.clientId" filterable>
          <el-option
            v-for="client in clients"
            :key="client.clientId"
            :label="`${client.clientName} / ${client.clientId}`"
            :value="client.clientId"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.grantType')">
        <el-segmented v-model="form.grantType" :options="MCP_GRANT_TYPE_OPTIONS"/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.principalType')">
        <el-segmented v-model="form.principalType" :options="MCP_PRINCIPAL_TYPE_OPTIONS"/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.principalId')">
        <el-select v-model="form.principalId" filterable style="width: 100%">
          <el-option v-for="opt in principalOptions" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.tenantId')">
        <el-input v-model="form.tenantId"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button :loading="submitting" type="primary" @click="submit">{{ t('common.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {ref} from 'vue';
import {useI18n} from 'vue-i18n';

import {addMcpConnection, listMcpClient} from '@/api/mcp';
import {listPrincipal} from '@/api/principal';
import {
  MCP_GRANT_TYPE_OPTIONS,
  MCP_GRANT_TYPES,
  MCP_PRINCIPAL_TYPE_OPTIONS,
  MCP_PRINCIPAL_TYPES,
} from '@/config/constant/enums';
import type {McpConnectionForm, OAuthClientRecord} from '@/config/types';
import {successMessage} from '@/utils/notificationUtil';

const {t} = useI18n();
const emit = defineEmits<{ (e: 'saved'): void }>();

const visible = ref(false);
const submitting = ref(false);
const clients = ref<OAuthClientRecord[]>([]);
const principalOptions = ref<Array<{ label: string; value: string }>>([]);
const form = ref<McpConnectionForm>({});

const loadOptions = async () => {
  const [clientRes, principalRes] = await Promise.all([
    listMcpClient(),
    listPrincipal({page: {current: 1, size: 1000}}),
  ]);
  clients.value = clientRes.data || [];
  principalOptions.value = (((principalRes as any)?.data?.records || []) as any[]).map((p) => ({
    label: p.displayName || p.principalName || String(p.id),
    value: String(p.id),
  }));
};

const open = async () => {
  form.value = {
    connectionName: '',
    clientId: '',
    principalId: '',
    principalType: MCP_PRINCIPAL_TYPES.USER,
    tenantId: '',
    grantType: MCP_GRANT_TYPES.AUTHORIZATION_CODE,
  };
  visible.value = true;
  await loadOptions();
  form.value.clientId = clients.value[0]?.clientId || '';
};

const submit = async () => {
  submitting.value = true;
  try {
    await addMcpConnection(form.value);
    successMessage(t('settings.mcp.saved'));
    visible.value = false;
    emit('saved');
  } finally {
    submitting.value = false;
  }
};

defineExpose({open});
</script>
