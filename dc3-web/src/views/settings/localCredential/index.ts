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

import {defineComponent, reactive, ref, watch} from 'vue';
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
import {timestampColumn} from '@/utils/dateUtil';
import {successMessage} from '@/utils/notificationUtil';
import {cleanSearchParams} from '@/utils/searchParamUtil';

import type {LocalCredentialForm, LocalCredentialRecord} from '@/config/types';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';

export default defineComponent({
  name: 'SettingsLocalCredential',
  components: {
    BlankCard,
    ToolCard,
    EnableTag,
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
    const resolvePrincipalNames = async (rows: LocalCredentialRecord[]) => {
      const ids = Array.from(
        new Set(rows.map((r) => String(r.principalId ?? '')).filter((id) => id && id !== '0' && !principalNameMap[id]))
      );
      if (!ids.length) return;
      try {
        const res: any = await listPrincipalByIds(ids);
        (res?.data || []).forEach((p: any) => {
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

    // Principal options for the add-dialog dropdown (choose by name, not raw id).
    const principalOptions = ref<Array<{ label: string; value: string }>>([]);
    const loadPrincipalOptions = async () => {
      if (principalOptions.value.length) return;
      try {
        const res: any = await listPrincipal({page: {current: 1, size: 1000}});
        principalOptions.value = (res?.data?.records || []).map((p: any) => ({
          label: p.displayName || p.principalName || String(p.id),
          value: String(p.id),
        }));
      } catch {
        // handled globally
      }
    };

    const addDialog = reactive({
      visible: false,
      submitting: false,
      form: {loginName: '', principalId: '', password: ''} as LocalCredentialForm,
    });

    const resetDialog = reactive({visible: false, submitting: false, id: '', loginName: '', password: ''});

    const filterForm = reactive<Record<string, any>>({loginName: ''});

    const openAdd = () => {
      loadPrincipalOptions();
      addDialog.form = {loginName: '', principalId: '', password: ''};
      addDialog.visible = true;
    };

    const submitAdd = () => {
      addDialog.submitting = true;
      addLocalCredential(addDialog.form)
        .then(() => {
          successMessage();
          addDialog.visible = false;
          load();
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          addDialog.submitting = false;
        });
    };

    const openReset = (row: LocalCredentialRecord) => {
      resetDialog.id = row.id;
      resetDialog.loginName = row.loginName || '';
      resetDialog.password = '';
      resetDialog.visible = true;
    };

    const submitReset = () => {
      resetDialog.submitting = true;
      resetLocalCredentialPassword(resetDialog.id, resetDialog.password)
        .then(() => {
          successMessage();
          resetDialog.visible = false;
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          resetDialog.submitting = false;
        });
    };

    const remove = (id: string) => {
      deleteLocalCredential(id)
        .then(() => {
          successMessage();
          load();
        })
        .catch(() => {
          // handled globally
        });
    };

    const onSearch = (data: Record<string, any>) => search(cleanSearchParams(data));
    const onReset = () => {
      filterForm.loginName = '';
      reset();
    };

    load();

    return {
      t,
      reactiveData,
      refresh,
      sort,
      sizeChange,
      currentChange,
      addDialog,
      resetDialog,
      filterForm,
      openAdd,
      submitAdd,
      openReset,
      submitReset,
      remove,
      onSearch,
      onReset,
      timestampColumn,
      principalNameFor,
      principalOptions,
      Plus,
    };
  },
});
