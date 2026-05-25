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
import { useRouter } from 'vue-router';

import { listApi } from '@/api/api';
import { timestampColumn } from '@/utils/dateUtil';

import type { ApiRecord, Order } from '@/config/types';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import apiTool from './tool/ApiTool.vue';

// APIs are auto-registered by each service on startup — editing them from
// the UI would drift from the real routes in under a minute. This page is
// read-only by design; add/edit/delete intentionally stay off.
export default defineComponent({
  name: 'SettingsApi',
  components: {
    BlankCard,
    EnableTag,
    apiTool,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const reactiveData = reactive({
      loading: false,
      listData: [] as ApiRecord[],
      query: {} as Record<string, unknown>,
      order: false,
      page: {
        total: 0,
        size: 12,
        current: 1,
        orders: [] as Order[],
      },
    });

    const load = () => {
      reactiveData.loading = true;
      listApi({ page: reactiveData.page, ...reactiveData.query })
        .then((res) => {
          const data = res.data || {};
          reactiveData.listData = data.records || [];
          reactiveData.page.total = data.total || 0;
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          reactiveData.loading = false;
        });
    };

    const search = (params: Record<string, unknown>) => {
      reactiveData.query = params || {};
      reactiveData.page.current = 1;
      load();
    };

    const reset = () => {
      reactiveData.query = {};
      reactiveData.page.current = 1;
      load();
    };

    const refresh = () => load();

    const sort = () => {
      reactiveData.order = !reactiveData.order;
      reactiveData.page.orders = [{ column: 'create_time', asc: reactiveData.order }];
      load();
    };

    const openDetail = (row: ApiRecord) => {
      router.push({ name: 'settingsApiDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };

    const sizeChange = (size: number) => {
      reactiveData.page.size = size;
      load();
    };

    const currentChange = (current: number) => {
      reactiveData.page.current = current;
      load();
    };

    load();

    return {
      t,
      reactiveData,
      search,
      reset,
      refresh,
      sort,
      openDetail,
      sizeChange,
      currentChange,
      timestampColumn,
    };
  },
});
