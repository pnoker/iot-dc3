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
  <el-dialog v-model="visible" :title="t('settings.mcp.registerClient')" width="680px">
    <el-form :model="form" label-width="190px">
      <el-form-item :label="t('settings.mcp.clientName')">
        <el-input v-model="form.client_name" />
      </el-form-item>
      <el-form-item :label="t('settings.mcp.clientType')">
        <el-segmented v-model="form.client_type" :options="MCP_CLIENT_TYPE_OPTIONS" />
      </el-form-item>
      <el-form-item :label="t('settings.mcp.grantTypes')">
        <el-select v-model="grantTypes" multiple>
          <el-option v-for="opt in MCP_GRANT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.redirectUris')">
        <el-input v-model="redirectUrisText" type="textarea" />
      </el-form-item>
      <el-form-item :label="t('settings.mcp.scopes')">
        <el-select v-model="scopes" multiple>
          <el-option v-for="opt in MCP_SCOPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.tenantId')">
        <el-input :model-value="currentTenant" disabled />
      </el-form-item>
      <el-form-item :label="t('settings.mcp.serviceAccountPrincipalId')">
        <el-select
          v-model="form.service_account_principal_id"
          :placeholder="t('settings.mcp.serviceAccountPrincipalId')"
          filterable
        >
          <el-option
            v-for="sa in serviceAccounts"
            :key="sa.principalId ?? sa.id"
            :label="`${sa.serviceAccountName} / ${sa.principalId}`"
            :value="sa.principalId ?? ''"
          />
          <template #empty>
            <el-empty :description="t('settings.mcp.noServiceAccount')" />
          </template>
        </el-select>
      </el-form-item>
    </el-form>
    <el-alert
      v-if="registeredSecret"
      :closable="false"
      :title="`${t('settings.mcp.clientSecret')}: ${registeredSecret}`"
      show-icon
      type="success"
    />
    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button :loading="submitting" type="primary" @click="submit">{{ t('common.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
  import {computed, ref} from 'vue';
  import {useI18n} from 'vue-i18n';

  import {registerMcpClient} from '@/api/mcp';
  import {listServiceAccount} from '@/api/serviceAccount';
  import {
    MCP_CLIENT_TYPE_OPTIONS,
    MCP_CLIENT_TYPES,
    MCP_GRANT_TYPE_OPTIONS,
    MCP_GRANT_TYPES,
    MCP_SCOPE_OPTIONS,
    MCP_SCOPES,
  } from '@/config/constant/enums';
  import type {ServiceAccountRecord} from '@/config/types';
  import {useAuthStore} from '@/store/modules/auth';
  import {successMessage} from '@/utils/notificationUtil';
  import {isEnabledFlag} from '@/utils/thingModelFormatUtil';

  const {t} = useI18n();
  const authStore = useAuthStore();
  const emit = defineEmits<{(e: 'saved'): void}>();

  const visible = ref(false);
  const submitting = ref(false);
  const serviceAccounts = ref<ServiceAccountRecord[]>([]);
  const form = ref<Record<string, any>>({});
  const grantTypes = ref<string[]>([]);
  const scopes = ref<string[]>([]);
  const redirectUrisText = ref('');
  const registeredSecret = ref('');

  // The registered client is bound to the logged-in tenant; surface it read-only.
  const currentTenant = computed(() => {
    const tenant = authStore.getTenant;
    return typeof tenant === 'string' && tenant ? tenant : 'default';
  });

  const splitText = (value: string) =>
    value
      .split(/[\s,]+/)
      .map((item) => item.trim())
      .filter(Boolean);

  const loadServiceAccounts = async () => {
    const res = await listServiceAccount({page: {current: 1, size: 1000}});
    serviceAccounts.value = (res.data?.records || []).filter((sa) => isEnabledFlag(sa.enableFlag));
  };

  const open = () => {
    form.value = {
      client_name: '',
      client_type: MCP_CLIENT_TYPES.PUBLIC,
      service_account_principal_id: '',
    };
    grantTypes.value = [MCP_GRANT_TYPES.AUTHORIZATION_CODE];
    scopes.value = [MCP_SCOPES.TOOLS_LIST, MCP_SCOPES.TOOLS_CALL];
    redirectUrisText.value = `${window.location.origin}/oauth/callback`;
    registeredSecret.value = '';
    visible.value = true;
    void loadServiceAccounts();
  };

  const submit = async () => {
    submitting.value = true;
    try {
      const res = await registerMcpClient({
        ...form.value,
        grant_types: grantTypes.value,
        redirect_uris: splitText(redirectUrisText.value),
        scope: scopes.value,
      });
      registeredSecret.value = String(res.data?.client_secret || '');
      successMessage(t('settings.mcp.saved'));
      emit('saved');
      // Keep the dialog open when a confidential client returns a one-time secret
      // so the operator can copy it; otherwise close.
      if (!registeredSecret.value) visible.value = false;
    } finally {
      submitting.value = false;
    }
  };

  defineExpose({open});
</script>
