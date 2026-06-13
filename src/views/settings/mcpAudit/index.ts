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

import { listMcpAudit } from '@/api/mcp';
import { timestampLabel } from '@/utils/dateUtil';

import type { McpAuditRecord } from '@/config/types';

import BlankCard from '@/components/card/blank/BlankCard.vue';

const STATUS_OPTIONS = [
  { label: 'SUCCESS', value: 'SUCCESS' },
  { label: 'ERROR', value: 'ERROR' },
  { label: 'DENIED', value: 'DENIED' },
];
const RISK_OPTIONS = [
  { label: 'LOW', value: 'LOW' },
  { label: 'MEDIUM', value: 'MEDIUM' },
  { label: 'HIGH', value: 'HIGH' },
];

export default defineComponent({
  name: 'SettingsMcpAudit',
  components: {
    BlankCard,
  },
  setup() {
    const { t } = useI18n();
    const reactiveData = reactive({
      loading: false,
      list: [] as McpAuditRecord[],
      filter: { principalId: '', toolId: '', status: '', riskLevel: '' },
    });

    // Read-only log viewer: the backend returns the latest N rows for the caller's tenant
    // (no pagination — dc3_mcp_audit_log is append-only).
    const load = async () => {
      reactiveData.loading = true;
      try {
        const res = await listMcpAudit({ ...reactiveData.filter, limit: 200 });
        reactiveData.list = res.data || [];
      } finally {
        reactiveData.loading = false;
      }
    };

    const reset = () => {
      reactiveData.filter = { principalId: '', toolId: '', status: '', riskLevel: '' };
      load();
    };

    const statusTag = (status?: string) => {
      if (status === 'ERROR' || status === 'DENIED') return 'danger';
      if (status === 'SUCCESS') return 'success';
      return 'info';
    };

    const riskTag = (risk?: string) => {
      if (risk === 'HIGH') return 'danger';
      if (risk === 'MEDIUM') return 'warning';
      return 'success';
    };

    load();

    return {
      t,
      reactiveData,
      statusOptions: STATUS_OPTIONS,
      riskOptions: RISK_OPTIONS,
      load,
      reset,
      timestampLabel,
      statusTag,
      riskTag,
      Refresh,
      Search,
    };
  },
});
