/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { defineComponent, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { Plus } from '@element-plus/icons-vue';

import { addRolePrincipalBind, deleteRolePrincipalBind, listRolePrincipalBind } from '@/api/rolePrincipalBind';
import { listRole } from '@/api/role';
import { usePagedList } from '@/composables/usePagedList';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';
import { cleanSearchParams } from '@/utils/searchParamUtil';

import type { RolePrincipalBindForm, RolePrincipalBindRecord, RoleRecord } from '@/config/types';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';

const PRINCIPAL_TYPE_OPTIONS = [
  { label: 'USER', value: 'USER' },
  { label: 'SERVICE_ACCOUNT', value: 'SERVICE_ACCOUNT' },
  { label: 'SYSTEM', value: 'SYSTEM' },
];

export default defineComponent({
  name: 'SettingsRolePrincipalBind',
  components: {
    BlankCard,
    ToolCard,
  },
  setup() {
    const { t } = useI18n();
    const {
      state: reactiveData,
      load,
      search,
      reset,
      sort,
      sizeChange,
      currentChange,
    } = usePagedList<RolePrincipalBindRecord, Record<string, unknown>>({
      request: (query) => listRolePrincipalBind(query),
    });

    const refresh = () => load();

    // Roles feed the add-dialog selector. (No principal list endpoint exists yet, so the
    // principal id is entered manually — see the identity spec's Principal-controller gap.)
    const roles = ref<RoleRecord[]>([]);
    listRole({ page: { current: 1, size: 1000 } })
      .then((res) => {
        roles.value = res.data?.records || [];
      })
      .catch(() => {
        // handled globally
      });

    const dialog = reactive({
      visible: false,
      submitting: false,
      form: { roleId: '', principalId: '', principalType: 'USER' } as RolePrincipalBindForm,
    });

    const filterForm = reactive<Record<string, any>>({ roleId: '', principalId: '' });

    const openAdd = () => {
      dialog.form = { roleId: '', principalId: '', principalType: 'USER' };
      dialog.visible = true;
    };

    const submit = () => {
      dialog.submitting = true;
      addRolePrincipalBind(dialog.form)
        .then(() => {
          successMessage();
          dialog.visible = false;
          load();
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          dialog.submitting = false;
        });
    };

    const remove = (id: string) => {
      deleteRolePrincipalBind(id)
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
      filterForm.roleId = '';
      filterForm.principalId = '';
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
      roles,
      dialog,
      filterForm,
      principalTypeOptions: PRINCIPAL_TYPE_OPTIONS,
      openAdd,
      submit,
      remove,
      onSearch,
      onReset,
      timestampColumn,
      Plus,
    };
  },
});
