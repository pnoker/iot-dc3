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

import { defineComponent, reactive } from 'vue';
import { useI18n } from 'vue-i18n';

import { getRoleList } from '@/api/role';
import { getRoleUserList, listRoleByUserId } from '@/api/roleUserBind';

export default defineComponent({
  name: 'UserAssignRoles',
  emits: ['save'],
  setup(_, { emit }) {
    const { t } = useI18n();

    const reactiveData = reactive({
      visible: false,
      loading: false,
      submitting: false,
      user: {} as any,
      allRoles: [] as Array<{ id: string; label: string }>,
      // bindId(RoleUserBind 的 id) 作为某个 role 的查找键
      bindIdByRoleId: new Map<string, string>(),
      originalRoleIds: [] as string[],
      selectedIds: [] as string[],
    });

    const load = async () => {
      reactiveData.loading = true;
      try {
        const [allRes, ownRes, bindsRes] = await Promise.all([
          getRoleList({ page: { size: 1000, current: 1 } }) as Promise<any>,
          listRoleByUserId(reactiveData.user.id) as Promise<any>,
          getRoleUserList({ page: { size: 1000, current: 1 }, userId: reactiveData.user.id }) as Promise<any>,
        ]);

        reactiveData.allRoles = ((allRes.data?.records as any[]) || []).map((r) => ({
          id: String(r.id),
          label: `${r.roleName}${r.roleCode ? ` (${r.roleCode})` : ''}`,
        }));

        const ownIds = ((ownRes.data as any[]) || []).map((r) => String(r.id));
        reactiveData.originalRoleIds = ownIds;
        reactiveData.selectedIds = [...ownIds];

        const bindMap = new Map<string, string>();
        for (const bind of (bindsRes.data?.records as any[]) || []) {
          bindMap.set(String(bind.roleId), String(bind.id));
        }
        reactiveData.bindIdByRoleId = bindMap;
      } catch {
        // handled globally
      } finally {
        reactiveData.loading = false;
      }
    };

    const show = (user: any) => {
      reactiveData.user = user;
      reactiveData.selectedIds = [];
      reactiveData.originalRoleIds = [];
      reactiveData.bindIdByRoleId = new Map();
      reactiveData.allRoles = [];
      reactiveData.visible = true;
      load();
    };

    const submit = () => {
      const originalSet = new Set(reactiveData.originalRoleIds);
      const currentSet = new Set(reactiveData.selectedIds);

      const addIds: string[] = [];
      const removeBindIds: string[] = [];

      for (const id of currentSet) {
        if (!originalSet.has(id)) addIds.push(id);
      }
      for (const id of originalSet) {
        if (!currentSet.has(id)) {
          const bindId = reactiveData.bindIdByRoleId.get(id);
          if (bindId) removeBindIds.push(bindId);
        }
      }

      if (addIds.length === 0 && removeBindIds.length === 0) {
        reactiveData.visible = false;
        return;
      }

      reactiveData.submitting = true;
      emit('save', String(reactiveData.user.id), addIds, removeBindIds, () => {
        reactiveData.submitting = false;
        reactiveData.visible = false;
      });
    };

    return {
      t,
      reactiveData,
      show,
      submit,
    };
  },
});
