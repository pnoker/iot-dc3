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
import { Refresh, Search } from '@element-plus/icons-vue';

import { listIdentityAudit } from '@/api/identityAudit';
import { timestampLabel } from '@/utils/dateUtil';

import type { IdentityAuditRecord } from '@/config/types';

import BlankCard from '@/components/card/blank/BlankCard.vue';

const ACTION_OPTIONS = [
  { label: 'CREATE', value: 'CREATE' },
  { label: 'UPDATE', value: 'UPDATE' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'ENABLE', value: 'ENABLE' },
  { label: 'DISABLE', value: 'DISABLE' },
];
const STATUS_OPTIONS = [
  { label: 'SUCCESS', value: 'SUCCESS' },
  { label: 'FAILURE', value: 'FAILURE' },
];

export default defineComponent({
  name: 'SettingsIdentityAudit',
  components: {
    BlankCard,
  },
  setup() {
    const { t } = useI18n();
    const reactiveData = reactive({
      loading: false,
      list: [] as IdentityAuditRecord[],
      filter: { principalId: '', action: '', resourceType: '', status: '' },
    });

    const load = async () => {
      reactiveData.loading = true;
      try {
        const res = await listIdentityAudit({ ...reactiveData.filter, limit: 200 });
        reactiveData.list = res.data || [];
      } finally {
        reactiveData.loading = false;
      }
    };

    const reset = () => {
      reactiveData.filter = { principalId: '', action: '', resourceType: '', status: '' };
      load();
    };

    const statusTag = (status?: string) => {
      if (status === 'FAILURE') return 'danger';
      if (status === 'SUCCESS') return 'success';
      return 'info';
    };

    load();

    return {
      t,
      reactiveData,
      actionOptions: ACTION_OPTIONS,
      statusOptions: STATUS_OPTIONS,
      load,
      reset,
      timestampLabel,
      statusTag,
      Refresh,
      Search,
    };
  },
});
