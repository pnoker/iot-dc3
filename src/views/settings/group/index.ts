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

import { addGroup, deleteGroup, getGroupList, updateGroup } from '@/api/group';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';

import type { Order } from '@/config/types';
import type { GroupRecord } from '@/config/types/manager';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import groupTool from './tool/GroupTool.vue';
import groupEditForm from './edit/GroupEditForm.vue';

export default defineComponent({
  name: 'SettingsGroup',
  components: {
    BlankCard,
    groupTool,
    groupEditForm,
  },
  setup() {
    const { t } = useI18n();
    const editRef = ref<InstanceType<typeof groupEditForm>>();

    const reactiveData = reactive({
      loading: false,
      listData: [] as GroupRecord[],
      groupOptions: [] as GroupRecord[],
      query: {} as Record<string, any>,
      order: false,
      page: {
        total: 0,
        size: 12,
        current: 1,
        orders: [] as Order[],
      },
    });

    const parentNameMap = reactive<Record<string, string>>({});

    const loadOptions = () => {
      getGroupList({ page: { current: 1, size: 5000, orders: [{ column: 'group_index', asc: true }] } })
        .then((res: any) => {
          const records = (res.data?.records || []) as GroupRecord[];
          reactiveData.groupOptions = records;
          Object.keys(parentNameMap).forEach((key) => delete parentNameMap[key]);
          records.forEach((row) => {
            parentNameMap[String(row.id)] = row.groupName || String(row.id);
          });
        })
        .catch(() => {
          // handled globally
        });
    };

    const load = () => {
      reactiveData.loading = true;
      getGroupList({ page: reactiveData.page, ...reactiveData.query })
        .then((res: any) => {
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

    const parentName = (id: string | number | null | undefined) => {
      if (!id || String(id) === '0') return t('settings.group.rootGroup');
      return parentNameMap[String(id)] || String(id);
    };

    const search = (params: any) => {
      reactiveData.query = params || {};
      reactiveData.page.current = 1;
      load();
    };

    const reset = () => {
      reactiveData.query = {};
      reactiveData.page.current = 1;
      load();
    };

    const refresh = () => {
      load();
      loadOptions();
    };

    const sort = () => {
      reactiveData.order = !reactiveData.order;
      reactiveData.page.orders = [{ column: 'create_time', asc: reactiveData.order }];
      load();
    };

    const openAdd = () => editRef.value?.show();
    const openEdit = (row: GroupRecord) => editRef.value?.showEdit(row);

    const onAdd = (form: any, done: () => void) => {
      addGroup(form)
        .then(() => {
          successMessage();
          refresh();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const onUpdate = (form: any, done: () => void) => {
      updateGroup(form)
        .then(() => {
          successMessage();
          refresh();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const remove = (id: string) => {
      deleteGroup(id)
        .then(() => {
          successMessage();
          refresh();
        })
        .catch(() => {
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
    loadOptions();

    return {
      t,
      editRef,
      reactiveData,
      parentName,
      search,
      reset,
      refresh,
      sort,
      openAdd,
      openEdit,
      onAdd,
      onUpdate,
      remove,
      sizeChange,
      currentChange,
      timestampColumn,
    };
  },
});
