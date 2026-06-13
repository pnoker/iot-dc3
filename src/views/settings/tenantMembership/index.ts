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

import { defineComponent, reactive } from 'vue';
import { useI18n } from 'vue-i18n';
import { Plus } from '@element-plus/icons-vue';

import { addTenantMembership, deleteTenantMembership, listTenantMembership } from '@/api/tenantMembership';
import { usePagedList } from '@/composables/usePagedList';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';
import { cleanSearchParams } from '@/utils/searchParamUtil';

import type { TenantMembershipForm, TenantMembershipRecord } from '@/config/types';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';

const PRINCIPAL_TYPE_OPTIONS = [
  { label: 'USER', value: 'USER' },
  { label: 'SERVICE_ACCOUNT', value: 'SERVICE_ACCOUNT' },
  { label: 'SYSTEM', value: 'SYSTEM' },
];

const MEMBERSHIP_STATUS_OPTIONS = [
  { label: 'ACTIVE', value: 'ACTIVE' },
  { label: 'SUSPENDED', value: 'SUSPENDED' },
  { label: 'INVITED', value: 'INVITED' },
];

export default defineComponent({
  name: 'SettingsTenantMembership',
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
    } = usePagedList<TenantMembershipRecord, Record<string, unknown>>({
      request: (query) => listTenantMembership(query),
    });

    const refresh = () => load();

    const dialog = reactive({
      visible: false,
      submitting: false,
      form: { principalId: '', principalType: 'USER', membershipStatus: 'ACTIVE' } as TenantMembershipForm,
    });

    const filterForm = reactive<Record<string, any>>({ principalId: '', membershipStatus: '' });

    const openAdd = () => {
      dialog.form = { principalId: '', principalType: 'USER', membershipStatus: 'ACTIVE' };
      dialog.visible = true;
    };

    const submit = () => {
      dialog.submitting = true;
      addTenantMembership(dialog.form)
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
      deleteTenantMembership(id)
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
      filterForm.principalId = '';
      filterForm.membershipStatus = '';
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
      dialog,
      filterForm,
      principalTypeOptions: PRINCIPAL_TYPE_OPTIONS,
      membershipStatusOptions: MEMBERSHIP_STATUS_OPTIONS,
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
