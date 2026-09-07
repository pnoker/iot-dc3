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
    :before-close="requestClose"
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    :title="t('settings.mcp.addConnection')"
    class="things-dialog"
    destroy-on-close
    width="640px"
    @closed="onClosed"
  >
    <el-alert
      v-if="saveError"
      :closable="false"
      :title="t('common.saveFailed')"
      class="things-dialog-form-alert"
      show-icon
      type="error"
    />
    <el-alert
      v-if="optionsError"
      :closable="false"
      :title="t('common.loadFailed')"
      show-icon
      type="error"
    >
      <el-button :loading="loadingOptions" link type="danger" @click="loadOptions">
        {{ t('common.retry') }}
      </el-button>
    </el-alert>
    <el-form ref="formRef" v-loading="submitting" :aria-busy="submitting" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="t('settings.mcp.connectionName')" prop="connectionName">
        <el-input v-model="form.connectionName" :disabled="submitting" :placeholder="t('settings.mcp.connectionNameOptional')" clearable/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.clientId')" prop="clientId">
        <el-select v-model="form.clientId" :disabled="submitting" :loading="loadingOptions" filterable>
          <el-option
            v-for="client in clients"
            :key="client.clientId"
            :label="`${client.clientName} / ${client.clientId}`"
            :value="client.clientId"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.grantType')">
        <el-segmented v-model="form.grantType" :disabled="submitting" :options="MCP_GRANT_TYPE_OPTIONS"/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.principalType')">
        <el-segmented v-model="form.principalType" :disabled="submitting" :options="MCP_PRINCIPAL_TYPE_OPTIONS"/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.principalId')" prop="principalId">
        <el-select
          v-model="form.principalId"
          :disabled="submitting"
          :loading="loadingOptions"
          :placeholder="t('settings.mcp.principalOptional')"
          clearable
          filterable
          style="width: 100%"
        >
          <el-option v-for="opt in principalOptions" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.tenantId')">
        <el-input v-model="form.tenantId" :disabled="submitting"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="submitting" @click="requestClose()">{{ t('common.cancel') }}</el-button>
      <el-button :loading="submitting" type="primary" @click="submit">{{ t('common.save') }}</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import type {FormInstance, FormRules} from 'element-plus';
import {ElMessageBox} from 'element-plus';

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
import {optionalAuthNameRules, requiredSelectRule} from '@/utils/formRuleUtil';

const {t} = useI18n();
const emit = defineEmits<{ (e: 'saved'): void }>();

const visible = ref(false);
const submitting = ref(false);
const saveError = ref(false);
const loadingOptions = ref(false);
const optionsError = ref(false);
const formRef = ref<FormInstance>();
const clients = ref<OAuthClientRecord[]>([]);
const principalOptions = ref<Array<{ label: string; value: string }>>([]);
const form = ref<McpConnectionForm>({});
const initialForm = ref<McpConnectionForm>({});
let formSession = 0;
let optionsRequest = 0;

const isDirty = computed(() => visible.value && JSON.stringify(form.value) !== JSON.stringify(initialForm.value));

const rules: FormRules = {
  connectionName: optionalAuthNameRules(t),
  clientId: requiredSelectRule(t('settings.mcp.clientRequired')),
};

const loadOptions = async () => {
  const requestId = ++optionsRequest;
  loadingOptions.value = true;
  optionsError.value = false;
  try {
    const [clientRes, principalRes] = await Promise.all([
      listMcpClient(),
      listPrincipal({offset: 0, limit: 200}),
    ]);
    if (requestId !== optionsRequest || !visible.value) return;
    clients.value = clientRes || [];
    const principalPage = (principalRes as any)?.items || (principalRes as any)?.data?.items || [];
    principalOptions.value = (principalPage as any[]).map((p) => ({
      label: p.displayName || p.principalName || String(p.id),
      value: String(p.id),
    }));
  } catch {
    if (requestId === optionsRequest && visible.value) optionsError.value = true;
  } finally {
    if (requestId === optionsRequest) loadingOptions.value = false;
  }
};

const open = async () => {
  formSession += 1;
  optionsRequest += 1;
  form.value = {
    connectionName: '',
    clientId: '',
    principalId: '',
    principalType: MCP_PRINCIPAL_TYPES.USER,
    tenantId: '',
    grantType: MCP_GRANT_TYPES.AUTHORIZATION_CODE,
  };
  clients.value = [];
  principalOptions.value = [];
  optionsError.value = false;
  saveError.value = false;
  submitting.value = false;
  initialForm.value = {...form.value};
  visible.value = true;
  formRef.value?.clearValidate();
  const session = formSession;
  await loadOptions();
  if (session !== formSession || !visible.value) return;
  if (!optionsError.value) {
    form.value.clientId = clients.value[0]?.clientId || '';
    initialForm.value = {...form.value};
  }
};

const reset = () => {
  form.value = {...initialForm.value};
  saveError.value = false;
  formRef.value?.clearValidate();
};

const onClosed = () => {
  formSession += 1;
  optionsRequest += 1;
  submitting.value = false;
  formRef.value?.clearValidate();
};

onBeforeUnmount(() => {
  formSession += 1;
  optionsRequest += 1;
});

const requestClose = async (done?: () => void) => {
  if (submitting.value) return;
  const session = formSession;
  if (!isDirty.value) {
    if (done) done();
    else visible.value = false;
    return;
  }
  try {
    await ElMessageBox.confirm(t('common.discardConfirm'), t('common.confirm'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
    if (session !== formSession || !visible.value) return;
    if (done) done();
    else visible.value = false;
  } catch {
    // Keep the draft open when the user cancels the confirmation.
  }
};

const submit = async () => {
  if (submitting.value) return;
  const session = formSession;
  if (!visible.value) return;
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false);
    if (session !== formSession || !visible.value) return;
    if (!valid) return;
  }
  submitting.value = true;
  saveError.value = false;
  try {
    await addMcpConnection({
      ...form.value,
      connectionName: form.value.connectionName?.trim() || undefined,
      principalId: form.value.principalId || undefined,
      tenantId: form.value.tenantId?.trim() || undefined,
    });
    if (session !== formSession || !visible.value) return;
    successMessage(t('settings.mcp.saved'));
    initialForm.value = {...form.value};
    visible.value = false;
    emit('saved');
  } catch {
    if (session === formSession && visible.value) saveError.value = true;
  } finally {
    if (session === formSession) submitting.value = false;
  }
};

defineExpose({open, reset});
</script>
