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

import { addUser, deleteUser, getUserList, updateUser } from '@/api/user';
import { addRoleUserBind, deleteRoleUserBind } from '@/api/roleUserBind';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';

import type { Order } from '@/config/types';

import userTool from './tool/UserTool.vue';
import userEditForm from './edit/UserEditForm.vue';
import userAssignRoles from './assign/UserAssignRoles.vue';
import BlankCard from '@/components/card/blank/BlankCard.vue';

export default defineComponent({
  name: 'SettingsUser',
  components: {
    BlankCard,
    userTool,
    userEditForm,
    userAssignRoles,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const editRef = ref<InstanceType<typeof userEditForm>>();
    const assignRef = ref<InstanceType<typeof userAssignRoles>>();

    const reactiveData = reactive({
      loading: false,
      listData: [] as any[],
      query: {} as Record<string, any>,
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
      getUserList({ page: reactiveData.page, ...reactiveData.query })
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

    const refresh = () => load();

    const sort = () => {
      reactiveData.order = !reactiveData.order;
      reactiveData.page.orders = [{ column: 'create_time', asc: reactiveData.order }];
      load();
    };

    const openAdd = () => editRef.value?.show();
    const openEdit = (row: any) => editRef.value?.showEdit(row);
    const openAssignRoles = (row: any) => assignRef.value?.show(row);
    const openDetail = (row: any) => {
      router.push({ name: 'settingsUserDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };

    const onAdd = (form: any, done: () => void) => {
      addUser(form)
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
      updateUser(form)
        .then(() => {
          successMessage();
          load();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const onAssignRoles = async (userId: string, addIds: string[], removeBindIds: string[], done: () => void) => {
      try {
        await Promise.all([
          ...addIds.map((roleId) => addRoleUserBind({ userId, roleId })),
          ...removeBindIds.map((id) => deleteRoleUserBind(id)),
        ]);
        successMessage(t('settings.user.assignSaved'));
        done();
      } catch {
        // handled globally
      }
    };

    const remove = (id: string) => {
      deleteUser(id)
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
      assignRef,
      reactiveData,
      search,
      reset,
      refresh,
      sort,
      openAdd,
      openEdit,
      openDetail,
      openAssignRoles,
      onAdd,
      onUpdate,
      onAssignRoles,
      remove,
      sizeChange,
      currentChange,
      timestampColumn,
    };
  },
});
