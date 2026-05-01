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

import { addMenu, deleteMenu, getMenuTree, updateMenu } from '@/api/menu';
import { successMessage } from '@/utils/NotificationUtil';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import menuTool from './tool/MenuTool.vue';
import menuEditForm from './edit/MenuEditForm.vue';

export default defineComponent({
  name: 'SettingsMenu',
  components: {
    BlankCard,
    menuTool,
    menuEditForm,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const editRef = ref<InstanceType<typeof menuEditForm>>();

    const reactiveData = reactive({
      loading: false,
      listData: [] as any[],
      query: {} as Record<string, any>,
      page: {
        total: 0,
        size: 12,
        current: 1,
        orders: [] as any[],
      },
    });

    const load = () => {
      reactiveData.loading = true;
      getMenuTree(reactiveData.query)
        .then((res: any) => {
          const tree = res.data || [];
          reactiveData.listData = tree;
          reactiveData.page.total = tree.length;
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          reactiveData.loading = false;
        });
    };

    const search = (params: any) => {
      reactiveData.query = params || {};
      load();
    };

    const reset = () => {
      reactiveData.query = {};
      load();
    };

    const refresh = () => load();

    const openAdd = () => editRef.value?.show();
    const openEdit = (row: any) => editRef.value?.showEdit(row);
    const openDetail = (row: any) => {
      router.push({ name: 'settingsMenuDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };

    const onAdd = (form: any, done: () => void) => {
      addMenu(form)
        .then(() => {
          successMessage();
          load();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const onUpdate = (form: any, done: () => void) => {
      updateMenu(form)
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
      deleteMenu(id)
        .then(() => {
          successMessage();
          load();
        })
        .catch(() => {
          // handled globally
        });
    };

    load();

    return {
      t,
      editRef,
      reactiveData,
      search,
      reset,
      refresh,
      openAdd,
      openEdit,
      openDetail,
      onAdd,
      onUpdate,
      remove,
    };
  },
});
