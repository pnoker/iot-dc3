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

import { addRole, deleteRole, getRoleList, getRoleTree, updateRole } from '@/api/role';
import { bindRoleResource, unbindRoleResource } from '@/api/roleResourceBind';
import { timestampColumn } from '@/utils/DateUtil';
import { successMessage } from '@/utils/NotificationUtil';

import type { Order } from '@/config/entity';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import roleTool from './tool/RoleTool.vue';
import roleEditForm from './edit/RoleEditForm.vue';
import roleAssignResources from './assign/RoleAssignResources.vue';

export default defineComponent({
  name: 'SettingsRole',
  components: {
    BlankCard,
    roleTool,
    roleEditForm,
    roleAssignResources,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const editRef = ref<InstanceType<typeof roleEditForm>>();
    const assignRef = ref<InstanceType<typeof roleAssignResources>>();

    const reactiveData = reactive({
      loading: false,
      listData: [] as any[],
      roleTreeData: [] as any[],
      query: {} as Record<string, any>,
      order: false,
      page: {
        total: 0,
        size: 12,
        current: 1,
        orders: [] as Order[],
      },
    });

    const loadTree = () => {
      getRoleTree()
        .then((res: any) => {
          reactiveData.roleTreeData = (res.data as any[]) || [];
        })
        .catch(() => {
          // handled globally
        });
    };

    const load = () => {
      reactiveData.loading = true;
      getRoleList({ page: reactiveData.page, ...reactiveData.query })
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
    const openAssignResources = (row: any) => assignRef.value?.show(row);
    const openDetail = (row: any) => {
      router.push({ name: 'settingsRoleDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };

    const onAdd = (form: any, done: () => void) => {
      addRole(form)
        .then(() => {
          successMessage();
          load();
          loadTree();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const onUpdate = (form: any, done: () => void) => {
      updateRole(form)
        .then(() => {
          successMessage();
          load();
          loadTree();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const onAssignResources = async (roleId: string, addIds: string[], removeBindIds: string[], done: () => void) => {
      try {
        await Promise.all([
          ...addIds.map((resourceId) => bindRoleResource({ roleId, resourceId })),
          ...removeBindIds.map((id) => unbindRoleResource(id)),
        ]);
        successMessage(t('settings.role.assignSaved'));
        done();
      } catch {
        // handled globally
      }
    };

    const remove = (id: string) => {
      deleteRole(id)
        .then(() => {
          successMessage();
          load();
          loadTree();
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
    loadTree();

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
      openAssignResources,
      onAdd,
      onUpdate,
      onAssignResources,
      remove,
      sizeChange,
      currentChange,
      timestampColumn,
    };
  },
});
