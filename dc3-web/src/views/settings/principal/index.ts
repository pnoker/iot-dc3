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

import {defineComponent, reactive} from 'vue';
import {useI18n} from 'vue-i18n';

import {disablePrincipal, enablePrincipal, listPrincipal} from '@/api/principal';
import {usePagedList} from '@/composables/usePagedList';
import {timestampColumn} from '@/utils/dateUtil';
import {successMessage} from '@/utils/notificationUtil';
import {cleanSearchParams} from '@/utils/searchParamUtil';
import {isEnabledFlag} from '@/utils/thingModelFormatUtil';

import type {PrincipalRecord} from '@/config/types';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';

const PRINCIPAL_TYPE_OPTIONS = [
  {label: 'USER', value: 'USER'},
  {label: 'SERVICE_ACCOUNT', value: 'SERVICE_ACCOUNT'},
  {label: 'SYSTEM', value: 'SYSTEM'},
];

export default defineComponent({
  name: 'SettingsPrincipal',
  components: {
    BlankCard,
    ToolCard,
    EnableFlagSegmented,
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
    } = usePagedList<PrincipalRecord, Record<string, unknown>>({
      request: (query) => listPrincipal(query),
    });

    const refresh = () => load();

    // Read-mostly roster: principals are created through user / service-account management, so
    // this page only lists and toggles enable. No add dialog.
    const filterForm = reactive<Record<string, any>>({principalType: '', principalName: '', enableFlag: ''});

    const toggleEnable = (row: PrincipalRecord) => {
      const disable = isEnabledFlag(row.enableFlag);
      (disable ? disablePrincipal : enablePrincipal)(row.id)
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
      filterForm.principalType = '';
      filterForm.principalName = '';
      filterForm.enableFlag = '';
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
      filterForm,
      principalTypeOptions: PRINCIPAL_TYPE_OPTIONS,
      toggleEnable,
      onSearch,
      onReset,
      timestampColumn,
      isEnabledFlag,
    };
  },
});
