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

import { defineComponent } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import { listApi } from '@/api/api';
import { usePagedList } from '@/composables/usePagedList';
import { timestampColumn } from '@/utils/dateUtil';

import type { ApiRecord } from '@/config/types';

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

    const {
      state: reactiveData,
      load,
      search,
      reset,
      sort,
      sizeChange,
      currentChange,
    } = usePagedList<ApiRecord, Record<string, unknown>>({
      request: (query) => listApi(query),
    });

    const refresh = () => load();

    const openDetail = (row: ApiRecord) => {
      router.push({ name: 'settingsApiDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
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
