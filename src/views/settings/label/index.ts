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

import { addLabel, deleteLabel, listLabel, updateLabel } from '@/api/label';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';

import type { Order } from '@/config/types';
import type { LabelForm, LabelRecord } from '@/config/types/manager';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import labelTool from './tool/LabelTool.vue';
import labelEditForm from './edit/LabelEditForm.vue';

export default defineComponent({
  name: 'SettingsLabel',
  components: {
    BlankCard,
    EnableTag,
    labelTool,
    labelEditForm,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();
    const editRef = ref<InstanceType<typeof labelEditForm>>();

    const reactiveData = reactive({
      loading: false,
      listData: [] as LabelRecord[],
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
      listLabel({ page: reactiveData.page, ...reactiveData.query })
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

    const openAdd = () => editRef.value?.show();
    const openDetail = (row: LabelRecord) => {
      router.push({ name: 'settingsLabelDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };
    const openEdit = (row: LabelRecord) => editRef.value?.showEdit(row);

    const onAdd = (form: LabelForm, done: () => void) => {
      addLabel(form)
        .then(() => {
          successMessage();
          load();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const onUpdate = (form: LabelForm, done: () => void) => {
      updateLabel(form)
        .then(() => {
          successMessage();
          load();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const remove = (id: string) => {
      deleteLabel(id)
        .then(() => {
          successMessage();
          load();
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

    return {
      t,
      editRef,
      reactiveData,
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
