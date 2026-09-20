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
    :title="t('settings.mcp.registerClient')"
    class="things-dialog"
    destroy-on-close
    width="680px"
    @closed="onClosed"
  >
    <el-alert
      v-if="saveError && !registeredSecret"
      :closable="false"
      :title="t('common.saveFailed')"
      class="things-dialog-form-alert"
      show-icon
      type="error"
    />
    <el-alert
      v-if="serviceAccountError && !registeredSecret"
      :closable="false"
      :title="t('common.loadFailed')"
      show-icon
      type="error"
    >
      <el-button :loading="loadingServiceAccounts" link type="danger" @click="loadServiceAccounts">
        {{ t('common.retry') }}
      </el-button>
    </el-alert>
    <el-form
      v-if="!registeredSecret"
      ref="formRef"
      v-loading="submitting"
      :aria-busy="submitting"
      :model="formModel"
      :rules="rules"
      label-position="top"
    >
      <el-form-item :label="t('settings.mcp.clientName')" prop="client_name">
        <el-input v-model="form.client_name" :disabled="submitting"/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.clientType')">
        <el-segmented v-model="form.client_type" :disabled="submitting" :options="MCP_CLIENT_TYPE_OPTIONS"/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.grantTypes')" prop="grantTypes">
        <el-select v-model="grantTypes" :disabled="submitting" multiple>
          <el-option v-for="opt in MCP_GRANT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.redirectUris')" prop="redirectUrisText">
        <el-input v-model="redirectUrisText" :disabled="submitting" type="textarea"/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.scopes')" prop="scopes">
        <el-select v-model="scopes" :disabled="submitting" multiple>
          <el-option v-for="opt in MCP_SCOPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.tenantId')">
        <el-input :model-value="currentTenant" disabled/>
      </el-form-item>
      <el-form-item :label="t('settings.mcp.serviceAccountPrincipalId')" prop="service_account_principal_id">
        <el-select
          v-model="form.service_account_principal_id"
          :disabled="submitting"
          :placeholder="t('settings.mcp.serviceAccountPrincipalId')"
          :loading="loadingServiceAccounts"
          filterable
        >
          <el-option
            v-for="sa in serviceAccounts"
            :key="sa.principalId ?? sa.id"
            :label="`${sa.serviceAccountName} / ${sa.principalId}`"
            :value="sa.principalId ?? ''"
          />
          <template #empty>
            <el-empty :description="t('settings.mcp.noServiceAccount')"/>
          </template>
        </el-select>
      </el-form-item>
    </el-form>
    <section v-else aria-live="polite" class="register-client-result">
      <el-alert
        :closable="false"
        :description="t('settings.mcp.secretOneTimeDescription')"
        :title="t('settings.mcp.secretOneTimeTitle')"
        show-icon
        type="warning"
      />
      <el-form label-position="top">
        <el-form-item v-if="registeredClientId" :label="t('settings.mcp.clientId')">
          <el-input :model-value="registeredClientId" readonly>
            <template #append>
              <el-button
                :aria-label="t('settings.mcp.copyClientId')"
                :icon="DocumentCopy"
                @click="copyCredential(registeredClientId, t('settings.mcp.clientId'))"
              >
                {{ t('settings.mcp.copy') }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('settings.mcp.clientSecret')">
          <el-input :model-value="registeredSecret" readonly>
            <template #append>
              <el-button
                :aria-label="t('settings.mcp.copyClientSecret')"
                :icon="DocumentCopy"
                @click="copyCredential(registeredSecret, t('settings.mcp.clientSecret'))"
              >
                {{ t('settings.mcp.copy') }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
    </section>
    <template #footer>
      <el-button v-if="registeredSecret" type="primary" @click="requestClose()">{{ t('common.close') }}</el-button>
      <template v-else>
        <el-button :disabled="submitting" @click="requestClose()">{{ t('common.cancel') }}</el-button>
        <el-button :loading="submitting" type="primary" @click="submit">{{ t('common.save') }}</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import type {FormInstance, FormRules} from 'element-plus';
import {ElMessageBox} from 'element-plus';
import {DocumentCopy} from '@element-plus/icons-vue';

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
import {setCopyContent} from '@/utils/clipboardUtil';
import {successMessage} from '@/utils/notificationUtil';
import {isEnabledFlag} from '@/utils/thingModelFormatUtil';
import {authNameRules, requiredSelectRule} from '@/utils/formRuleUtil';

const {t} = useI18n();
const authStore = useAuthStore();
const emit = defineEmits<{ (e: 'saved'): void }>();

const visible = ref(false);
const submitting = ref(false);
const saveError = ref(false);
const loadingServiceAccounts = ref(false);
const serviceAccountError = ref(false);
const formRef = ref<FormInstance>();
const serviceAccounts = ref<ServiceAccountRecord[]>([]);
const form = ref<Record<string, any>>({});
const grantTypes = ref<string[]>([]);
const scopes = ref<string[]>([]);
const redirectUrisText = ref('');
const registeredSecret = ref('');
const registeredClientId = ref('');
const initialForm = ref({
  form: {} as Record<string, any>,
  grantTypes: [] as string[],
  scopes: [] as string[],
  redirectUrisText: '',
});
let formSession = 0;
let serviceAccountRequest = 0;

const formModel = computed(() => ({
  ...form.value,
  grantTypes: grantTypes.value,
  scopes: scopes.value,
  redirectUrisText: redirectUrisText.value,
}));

const currentSnapshot = () =>
  JSON.stringify({
    form: form.value,
    grantTypes: grantTypes.value,
    scopes: scopes.value,
    redirectUrisText: redirectUrisText.value,
  });
const isDirty = computed(() => visible.value && currentSnapshot() !== JSON.stringify(initialForm.value));

const rules: FormRules = {
  client_name: authNameRules(t, t('settings.mcp.clientName')),
  grantTypes: requiredSelectRule(t('settings.mcp.grantsRequired')),
  redirectUrisText: [
    {
      validator: (_rule, value, callback) => {
        if (!grantTypes.value.includes(MCP_GRANT_TYPES.AUTHORIZATION_CODE) || splitText(String(value || '')).length > 0) {
          callback();
          return;
        }
        callback(new Error(t('settings.mcp.redirectUrisRequired')));
      },
      trigger: 'blur',
    },
  ],
  scopes: requiredSelectRule(t('settings.mcp.scopesRequired')),
  service_account_principal_id: [
    {
      validator: (_rule, value, callback) => {
        if (!grantTypes.value.includes(MCP_GRANT_TYPES.CLIENT_CREDENTIALS) || String(value || '').trim()) {
          callback();
          return;
        }
        callback(new Error(t('settings.mcp.serviceAccountRequired')));
      },
      trigger: 'change',
    },
  ],
};

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

const copyCredential = (value: string, label: string) => {
  void setCopyContent(value, true, label);
};

const loadServiceAccounts = async () => {
  const requestId = ++serviceAccountRequest;
  loadingServiceAccounts.value = true;
  serviceAccountError.value = false;
  try {
    const res = await listServiceAccount({offset: 0, limit: 200});
    if (requestId === serviceAccountRequest && visible.value) {
      serviceAccounts.value = (res?.items || []).filter((sa) => isEnabledFlag(sa.enableFlag));
    }
  } catch {
    if (requestId === serviceAccountRequest && visible.value) serviceAccountError.value = true;
  } finally {
    if (requestId === serviceAccountRequest) loadingServiceAccounts.value = false;
  }
};

const open = () => {
  formSession += 1;
  serviceAccountRequest += 1;
  form.value = {
    client_name: '',
    client_type: MCP_CLIENT_TYPES.PUBLIC,
    tenant_id: currentTenant.value,
    service_account_principal_id: '',
  };
  grantTypes.value = [MCP_GRANT_TYPES.AUTHORIZATION_CODE];
  scopes.value = [MCP_SCOPES.TOOLS_LIST, MCP_SCOPES.TOOLS_CALL];
  redirectUrisText.value = `${window.location.origin}/oauth/callback`;
  registeredSecret.value = '';
  registeredClientId.value = '';
  saveError.value = false;
  serviceAccountError.value = false;
  serviceAccounts.value = [];
  submitting.value = false;
  visible.value = true;
  initialForm.value = {
    form: {...form.value},
    grantTypes: [...grantTypes.value],
    scopes: [...scopes.value],
    redirectUrisText: redirectUrisText.value,
  };
  void loadServiceAccounts();
};

const reset = () => {
  form.value = {...initialForm.value.form};
  grantTypes.value = [...initialForm.value.grantTypes];
  scopes.value = [...initialForm.value.scopes];
  redirectUrisText.value = initialForm.value.redirectUrisText;
  saveError.value = false;
  formRef.value?.clearValidate();
};

const onClosed = () => {
  formSession += 1;
  serviceAccountRequest += 1;
  submitting.value = false;
  registeredSecret.value = '';
  registeredClientId.value = '';
  formRef.value?.clearValidate();
};

onBeforeUnmount(() => {
  formSession += 1;
  serviceAccountRequest += 1;
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
    const res = await registerMcpClient({
      ...form.value,
      grant_types: grantTypes.value,
      redirect_uris: splitText(redirectUrisText.value),
      scope: scopes.value.join(' '),
    });
    if (session !== formSession || !visible.value) return;
    registeredSecret.value = String(res?.client_secret || '');
    registeredClientId.value = String(res?.client_id || res?.clientId || '');
    initialForm.value = {
      form: {...form.value},
      grantTypes: [...grantTypes.value],
      scopes: [...scopes.value],
      redirectUrisText: redirectUrisText.value,
    };
    successMessage(t('settings.mcp.saved'));
    emit('saved');
    // Keep the dialog open when a confidential client returns a one-time secret
    // so the operator can copy it; otherwise close.
    if (!registeredSecret.value) visible.value = false;
  } catch {
    // Failure details are already reported by the axios response
    // interceptor; surface only the inline alert here.
    if (session === formSession && visible.value) saveError.value = true;
  } finally {
    if (session === formSession) submitting.value = false;
  }
};

defineExpose({open, reset});
</script>

<style lang="scss" scoped>
.register-client-result {
  display: grid;
  gap: var(--dc3-space-4);

  :deep(.el-form-item:last-child) {
    margin-bottom: 0;
  }

  :deep(.el-input-group__append .el-button) {
    min-height: var(--dc3-touch-target);
    margin: 0;
  }
}
</style>
