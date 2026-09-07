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

import {computed, defineComponent, onBeforeUnmount, reactive, ref} from 'vue';
import {useI18n} from 'vue-i18n';

import {disablePrincipal, enablePrincipal, listPrincipal} from '@/api/principal';
import {usePagedList} from '@/composables/usePagedList';
import {successMessage} from '@/utils/notificationUtil';
import {cleanSearchParams} from '@/utils/searchParamUtil';
import {isEnabledFlag} from '@/utils/thingModelFormatUtil';

import {PRINCIPAL_TYPE_OPTIONS} from '@/config/constant/enums';
import type {PrincipalRecord, ResponsiveListColumn} from '@/config/types';

import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';

export default defineComponent({
  name: 'SettingsPrincipal',
  components: {
    ResponsiveRecordList,
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
    let disposed = false;

    // Read-mostly roster: principals are created through user / service-account management, so
    // this page only lists and toggles enable. No add dialog.
    const filterForm = reactive<Record<string, any>>({principalType: '', principalName: '', enableFlag: ''});
    const togglingIds = ref(new Set<string>());

    const columns = computed<ResponsiveListColumn<PrincipalRecord>[]>(() => [
      {
        key: 'principalName',
        label: t('settings.principal.principalName'),
        minWidth: 200,
        mobile: 'primary',
      },
      {key: 'displayName', label: t('settings.principal.displayName'), minWidth: 160},
      {key: 'principalType', label: t('settings.principal.principalType'), minWidth: 150, kind: 'tag'},
      {key: 'sourceType', label: t('settings.principal.sourceType'), minWidth: 130},
      {key: 'enableFlag', label: t('common.enable'), width: 90, kind: 'custom'},
      {key: 'lastLoginTime', label: t('settings.principal.lastLoginTime'), width: 165, kind: 'time'},
      {key: 'createTime', label: t('common.createTime'), width: 165, kind: 'time', mobile: 'hidden'},
    ]);

    const isToggling = (row: PrincipalRecord) => togglingIds.value.has(String(row.id));

    const toggleEnable = (row: PrincipalRecord) => {
      const id = String(row.id);
      if (togglingIds.value.has(id)) return;
      togglingIds.value.add(id);
      const disable = isEnabledFlag(row.enableFlag);
      (disable ? disablePrincipal : enablePrincipal)(row.id)
        .then(() => {
          if (disposed) return;
          successMessage();
          void load();
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          togglingIds.value.delete(id);
        });
    };

    onBeforeUnmount(() => {
      disposed = true;
      togglingIds.value.clear();
    });

    const onSearch = (data: Record<string, any>) => search(cleanSearchParams(data));
    const onReset = () => {
      filterForm.principalType = '';
      filterForm.principalName = '';
      filterForm.enableFlag = '';
      reset();
    };

    void load();

    return {
      t,
      reactiveData,
      refresh,
      sort,
      sizeChange,
      currentChange,
      filterForm,
      columns,
      principalTypeOptions: PRINCIPAL_TYPE_OPTIONS,
      toggleEnable,
      isToggling,
      onSearch,
      onReset,
      isEnabledFlag,
    };
  },
});
