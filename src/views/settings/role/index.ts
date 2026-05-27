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

import { defineComponent, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import { addRole, deleteRole, listRole, listRoleTree, updateRole } from '@/api/role';
import { addRoleResourceBind, deleteRoleResourceBind } from '@/api/roleResourceBind';
import { usePagedList } from '@/composables/usePagedList';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';

import type { RoleForm, RoleRecord } from '@/config/types/auth';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import roleTool from './tool/RoleTool.vue';
import roleEditForm from './edit/RoleEditForm.vue';
import roleAssignResources from './assign/RoleAssignResources.vue';

export default defineComponent({
  name: 'SettingsRole',
  components: {
    BlankCard,
    EnableTag,
    roleTool,
    roleEditForm,
    roleAssignResources,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const editRef = ref<InstanceType<typeof roleEditForm>>();
    const assignRef = ref<InstanceType<typeof roleAssignResources>>();

    const { state, load, search, reset, sort, sizeChange, currentChange } = usePagedList<
      RoleRecord,
      Record<string, unknown>
    >({
      request: (query) => listRole(query),
    });

    const reactiveData = state as typeof state & { roleTreeData: RoleRecord[] };
    reactiveData.roleTreeData = [];

    const loadTree = () => {
      listRoleTree()
        .then((res) => {
          reactiveData.roleTreeData = (res.data as RoleRecord[]) || [];
        })
        .catch(() => {
          // handled globally
        });
    };

    const refresh = () => load();

    const openAdd = () => editRef.value?.show();
    const openEdit = (row: RoleRecord) => editRef.value?.showEdit(row);
    const openAssignResources = (row: RoleRecord) => assignRef.value?.show(row);
    const openDetail = (row: RoleRecord) => {
      router.push({ name: 'settingsRoleDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };

    const onAdd = (form: RoleForm, done: () => void) => {
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

    const onUpdate = (form: RoleForm, done: () => void) => {
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
          ...addIds.map((resourceId) => addRoleResourceBind({ roleId, resourceId })),
          ...removeBindIds.map((id) => deleteRoleResourceBind(id)),
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
