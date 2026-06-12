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

import { addUser, deleteUser, listUser, updateUser } from '@/api/user';
import { addRolePrincipalBind, deleteRolePrincipalBind } from '@/api/rolePrincipalBind';
import { usePagedList } from '@/composables/usePagedList';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';

import type { UserForm, UserRecord } from '@/config/types/auth';

import userTool from './tool/UserTool.vue';
import userEditForm from './edit/UserEditForm.vue';
import userAssignRoles from './assign/UserAssignRoles.vue';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';

export default defineComponent({
  name: 'SettingsUser',
  components: {
    BlankCard,
    EnableTag,
    userTool,
    userEditForm,
    userAssignRoles,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const editRef = ref<InstanceType<typeof userEditForm>>();
    const assignRef = ref<InstanceType<typeof userAssignRoles>>();

    const {
      state: reactiveData,
      load,
      search,
      reset,
      sort,
      sizeChange,
      currentChange,
    } = usePagedList<UserRecord, Record<string, unknown>>({
      request: (query) => listUser(query),
    });

    const refresh = () => load();

    const openAdd = () => editRef.value?.show();
    const openEdit = (row: UserRecord) => editRef.value?.showEdit(row);
    const openAssignRoles = (row: UserRecord) => assignRef.value?.show(row);
    const openDetail = (row: UserRecord) => {
      router.push({ name: 'settingsUserDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };

    const onAdd = (form: UserForm, done: () => void) => {
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

    const onUpdate = (form: UserForm, done: () => void) => {
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

    const onAssignRoles = async (principalId: string, addIds: string[], removeBindIds: string[], done: () => void) => {
      try {
        await Promise.all([
          ...addIds.map((roleId) => addRolePrincipalBind({ principalId, principalType: 'USER', roleId })),
          ...removeBindIds.map((id) => deleteRolePrincipalBind(id)),
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
