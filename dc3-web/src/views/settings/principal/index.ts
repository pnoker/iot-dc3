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
