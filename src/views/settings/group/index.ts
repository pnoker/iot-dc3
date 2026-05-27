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
import { useRouter } from 'vue-router';

import { addGroup, deleteGroup, listGroup, updateGroup } from '@/api/group';
import { usePagedList } from '@/composables/usePagedList';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';

import type { GroupForm, GroupRecord } from '@/config/types/manager';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import groupTool from './tool/GroupTool.vue';
import groupEditForm from './edit/GroupEditForm.vue';

export default defineComponent({
  name: 'SettingsGroup',
  components: {
    BlankCard,
    EnableTag,
    groupTool,
    groupEditForm,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();
    const editRef = ref<InstanceType<typeof groupEditForm>>();

    const { state, load, search, reset, sort, sizeChange, currentChange } = usePagedList<
      GroupRecord,
      Record<string, unknown>
    >({
      request: (query) => listGroup(query),
    });

    const reactiveData = state as typeof state & { groupOptions: GroupRecord[] };
    reactiveData.groupOptions = [];

    const parentNameMap = reactive<Record<string, string>>({});

    const loadOptions = () => {
      listGroup({ page: { current: 1, size: 5000, orders: [{ column: 'group_index', asc: true }] } })
        .then((res) => {
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

    const parentName = (id: string | number | null | undefined) => {
      if (!id || String(id) === '0') return t('settings.group.rootGroup');
      return parentNameMap[String(id)] || String(id);
    };

    const refresh = () => {
      load();
      loadOptions();
    };

    const openAdd = () => editRef.value?.show();
    const openDetail = (row: GroupRecord) => {
      router.push({ name: 'settingsGroupDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };
    const openEdit = (row: GroupRecord) => editRef.value?.showEdit(row);

    const onAdd = (form: GroupForm, done: () => void) => {
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

    const onUpdate = (form: GroupForm, done: () => void) => {
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
      openDetail,
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
