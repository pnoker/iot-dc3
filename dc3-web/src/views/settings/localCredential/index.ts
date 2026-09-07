/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import {computed, defineComponent, onBeforeUnmount, reactive, ref, watch} from 'vue';
import {ElMessageBox} from 'element-plus';
import type {FormInstance, FormRules} from 'element-plus';
import {useI18n} from 'vue-i18n';
import {Plus} from '@element-plus/icons-vue';

import {
  addLocalCredential,
  deleteLocalCredential,
  listLocalCredential,
  resetLocalCredentialPassword,
} from '@/api/localCredential';
import {listPrincipal, listPrincipalByIds} from '@/api/principal';
import {usePagedList} from '@/composables/usePagedList';
import {successMessage} from '@/utils/notificationUtil';
import {cleanSearchParams} from '@/utils/searchParamUtil';
import {requiredSelectRule, requiredStringRule} from '@/utils/formRuleUtil';

import type {LocalCredentialForm, LocalCredentialRecord, ResponsiveListColumn} from '@/config/types';

import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';

export default defineComponent({
  name: 'SettingsLocalCredential',
  components: {
    ResponsiveRecordList,
    ToolCard,
  },
  setup() {
    const {t} = useI18n();
    const {
      state: reactiveData,
      load,
      search,
      reset,
      sort,
      sizeChange,
      currentChange,
    } = usePagedList<LocalCredentialRecord, Record<string, unknown>>({
      request: (query) => listLocalCredential(query),
    });

    const refresh = () => load();

    // Resolve principalId → principal name for the table column, reusing the
    // shared listPrincipalByIds endpoint (same source as the family relations).
    const principalNameMap = reactive<Record<string, string>>({});
    let principalNameRequestId = 0;
    const resolvePrincipalNames = async (rows: LocalCredentialRecord[]) => {
      const requestId = ++principalNameRequestId;
      const ids = Array.from(
        new Set(rows.map((r) => String(r.principalId ?? '')).filter((id) => id && id !== '0' && !principalNameMap[id]))
      );
      if (!ids.length) return;
      try {
        const res: any = await listPrincipalByIds(ids);
        if (requestId !== principalNameRequestId) return;
        (res || []).forEach((p: any) => {
          principalNameMap[String(p.id)] = p.displayName || p.principalName || String(p.id);
        });
      } catch {
        // handled globally
      }
    };
    watch(
      () => reactiveData.listData,
      (rows) => resolvePrincipalNames((rows as LocalCredentialRecord[]) || []),
      {
        immediate: true,
      }
    );
    const principalNameFor = (row: LocalCredentialRecord) =>
      principalNameMap[String(row.principalId)] || String(row.principalId ?? '-');
    const columns = computed<ResponsiveListColumn<LocalCredentialRecord>[]>(() => [
      {
        key: 'loginName',
        label: t('settings.localCredential.loginName'),
        minWidth: 160,
        mobile: 'primary',
      },
      {
        key: 'principalId',
        label: t('settings.localCredential.principalId'),
        minWidth: 140,
        formatter: principalNameFor,
      },
      {key: 'credentialType', label: t('settings.localCredential.credentialType'), minWidth: 130},
      {key: 'enableFlag', label: t('common.enable'), width: 90, kind: 'enable'},
      {
        key: 'passwordUpdatedTime',
        label: t('settings.localCredential.passwordUpdatedTime'),
        width: 165,
        kind: 'time',
      },
      {key: 'failedAttempts', label: t('settings.localCredential.failedAttempts'), minWidth: 120},
    ]);

    const principalOptions = ref<Array<{ label: string; value: string }>>([]);
    let optionRequestId = 0;
    let addSessionId = 0;
    let resetSessionId = 0;
    let disposed = false;

    const addFormRef = ref<FormInstance>();
    const resetFormRef = ref<FormInstance>();

    const addRules: FormRules = {
      loginName: requiredStringRule(t('settings.localCredential.loginNameRequired')),
      principalId: requiredSelectRule(t('settings.localCredential.principalRequired')),
      password: [
        ...requiredStringRule(t('settings.localCredential.passwordRequired')),
        {min: 6, message: t('settings.localCredential.passwordMin'), trigger: 'blur'},
      ],
    };
    const resetRules: FormRules = {
      password: [
        ...requiredStringRule(t('settings.localCredential.passwordRequired')),
        {min: 6, message: t('settings.localCredential.passwordMin'), trigger: 'blur'},
      ],
    };

    const loadPrincipalOptions = async (force = false) => {
      if (addDialog.submitting) return;
      if (principalOptions.value.length && !force) return;
      const requestId = ++optionRequestId;
      addDialog.optionsLoading = true;
      addDialog.optionsError = false;
      try {
        const res: any = await listPrincipal({offset: 0, limit: 200});
        if (requestId !== optionRequestId || !addDialog.visible) return;
        principalOptions.value = (res?.items || []).map((p: any) => ({
          label: p.displayName || p.principalName || String(p.id),
          value: String(p.id),
        }));
      } catch {
        if (requestId === optionRequestId && addDialog.visible) addDialog.optionsError = true;
      } finally {
        if (requestId === optionRequestId) addDialog.optionsLoading = false;
      }
    };

    const addDialog = reactive({
      visible: false,
      submitting: false,
      optionsLoading: false,
      optionsError: false,
      saveError: false,
      form: {loginName: '', principalId: '', password: ''} as LocalCredentialForm,
    });

    const resetDialog = reactive({
      visible: false,
      submitting: false,
      saveError: false,
      id: '',
      loginName: '',
      password: '',
    });

    const filterForm = reactive<Record<string, any>>({loginName: ''});

    const openAdd = () => {
      addSessionId += 1;
      addDialog.form = {loginName: '', principalId: '', password: ''};
      addDialog.saveError = false;
      addDialog.optionsError = false;
      addDialog.visible = true;
      addFormRef.value?.clearValidate();
      void loadPrincipalOptions();
    };

    const finishCloseAdd = (done?: () => void) => {
      addSessionId += 1;
      optionRequestId += 1;
      addDialog.saveError = false;
      addDialog.optionsLoading = false;
      if (done) done();
      else addDialog.visible = false;
    };

    const requestCloseAdd = async (done?: () => void) => {
      if (addDialog.submitting) return;
      const sessionId = addSessionId;
      const dirty = Boolean(addDialog.form.loginName || addDialog.form.principalId || addDialog.form.password);
      if (!dirty) {
        finishCloseAdd(done);
        return;
      }
      try {
        await ElMessageBox.confirm(t('common.discardConfirm'), {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'warning',
        });
        if (disposed || sessionId !== addSessionId || !addDialog.visible) return;
        finishCloseAdd(done);
      } catch {
        // Keep the draft open.
      }
    };

    const submitAdd = async () => {
      if (addDialog.submitting) return;
      const sessionId = addSessionId;
      if (!addDialog.visible) return;
      const valid = await addFormRef.value?.validate().catch(() => false);
      if (sessionId !== addSessionId || !addDialog.visible) return;
      if (!valid) return;
      addDialog.submitting = true;
      addDialog.saveError = false;
      try {
        await addLocalCredential({
          ...addDialog.form,
          loginName: addDialog.form.loginName?.trim(),
        });
        if (sessionId !== addSessionId) return;
        successMessage();
        addDialog.submitting = false;
        finishCloseAdd();
        await load();
      } catch {
        if (sessionId === addSessionId) addDialog.saveError = true;
      } finally {
        if (sessionId === addSessionId) addDialog.submitting = false;
      }
    };

    const openReset = (row: LocalCredentialRecord) => {
      resetSessionId += 1;
      resetDialog.id = row.id;
      resetDialog.loginName = row.loginName || '';
      resetDialog.password = '';
      resetDialog.saveError = false;
      resetDialog.visible = true;
      resetFormRef.value?.clearValidate();
    };

    const finishCloseReset = (done?: () => void) => {
      resetSessionId += 1;
      resetDialog.saveError = false;
      if (done) done();
      else resetDialog.visible = false;
    };

    const requestCloseReset = async (done?: () => void) => {
      if (resetDialog.submitting) return;
      const sessionId = resetSessionId;
      if (!resetDialog.password) {
        finishCloseReset(done);
        return;
      }
      try {
        await ElMessageBox.confirm(t('common.discardConfirm'), {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          type: 'warning',
        });
        if (disposed || sessionId !== resetSessionId || !resetDialog.visible) return;
        finishCloseReset(done);
      } catch {
        // Keep the draft open.
      }
    };

    const submitReset = async () => {
      if (resetDialog.submitting) return;
      const sessionId = resetSessionId;
      if (!resetDialog.visible) return;
      const valid = await resetFormRef.value?.validate().catch(() => false);
      if (sessionId !== resetSessionId || !resetDialog.visible) return;
      if (!valid) return;
      resetDialog.submitting = true;
      resetDialog.saveError = false;
      try {
        await resetLocalCredentialPassword(resetDialog.id, resetDialog.password);
        if (sessionId !== resetSessionId) return;
        successMessage();
        resetDialog.submitting = false;
        finishCloseReset();
      } catch {
        if (sessionId === resetSessionId) resetDialog.saveError = true;
      } finally {
        if (sessionId === resetSessionId) resetDialog.submitting = false;
      }
    };

    const deletingIds = ref(new Set<string>());
    const isDeleting = (row: LocalCredentialRecord) => deletingIds.value.has(String(row.id));
    const remove = (id: string) => {
      if (deletingIds.value.has(id)) return;
      deletingIds.value.add(id);
      deleteLocalCredential(id)
        .then(() => {
          if (disposed) return;
          successMessage();
          void load();
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          deletingIds.value.delete(id);
      });
    };

    onBeforeUnmount(() => {
      disposed = true;
      principalNameRequestId += 1;
      optionRequestId += 1;
      addSessionId += 1;
      resetSessionId += 1;
      deletingIds.value.clear();
    });

    const onSearch = (data: Record<string, any>) => search(cleanSearchParams(data));
    const onReset = () => {
      filterForm.loginName = '';
      reset();
    };

    void load();

    return {
      t,
      reactiveData,
      columns,
      refresh,
      sort,
      sizeChange,
      currentChange,
      addDialog,
      addFormRef,
      addRules,
      resetDialog,
      resetFormRef,
      resetRules,
      filterForm,
      openAdd,
      submitAdd,
      requestCloseAdd,
      loadPrincipalOptions,
      openReset,
      submitReset,
      requestCloseReset,
      remove,
      isDeleting,
      onSearch,
      onReset,
      principalNameFor,
      principalOptions,
      Plus,
    };
  },
});
