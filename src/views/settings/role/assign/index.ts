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

import { getResourceList } from '@/api/resource';
import { getRoleResourceList, listResourceByRoleId } from '@/api/roleResourceBind';

export default defineComponent({
  name: 'RoleAssignResources',
  emits: ['save'],
  setup(_, { emit }) {
    const { t } = useI18n();

    const reactiveData = reactive({
      visible: false,
      loading: false,
      submitting: false,
      role: {} as any,
      allResources: [] as Array<{ id: string; label: string }>,
      bindIdByResourceId: new Map<string, string>(),
      originalResourceIds: [] as string[],
      selectedIds: [] as string[],
    });

    const load = async () => {
      reactiveData.loading = true;
      try {
        const [allRes, ownRes, bindsRes] = await Promise.all([
          getResourceList({ page: { size: 1000, current: 1 } }) as Promise<any>,
          listResourceByRoleId(reactiveData.role.id) as Promise<any>,
          getRoleResourceList({ page: { size: 1000, current: 1 }, roleId: reactiveData.role.id }) as Promise<any>,
        ]);

        reactiveData.allResources = ((allRes.data?.records as any[]) || []).map((r) => ({
          id: String(r.id),
          label: `${r.resourceName}${r.resourceCode ? ` (${r.resourceCode})` : ''}`,
        }));

        const ownIds = ((ownRes.data as any[]) || []).map((r) => String(r.id));
        reactiveData.originalResourceIds = ownIds;
        reactiveData.selectedIds = [...ownIds];

        const bindMap = new Map<string, string>();
        for (const bind of (bindsRes.data?.records as any[]) || []) {
          bindMap.set(String(bind.resourceId), String(bind.id));
        }
        reactiveData.bindIdByResourceId = bindMap;
      } catch {
        // handled globally
      } finally {
        reactiveData.loading = false;
      }
    };

    const show = (role: any) => {
      reactiveData.role = role;
      reactiveData.selectedIds = [];
      reactiveData.originalResourceIds = [];
      reactiveData.bindIdByResourceId = new Map();
      reactiveData.allResources = [];
      reactiveData.visible = true;
      load();
    };

    const submit = () => {
      const originalSet = new Set(reactiveData.originalResourceIds);
      const currentSet = new Set(reactiveData.selectedIds);

      const addIds: string[] = [];
      const removeBindIds: string[] = [];

      for (const id of currentSet) {
        if (!originalSet.has(id)) addIds.push(id);
      }
      for (const id of originalSet) {
        if (!currentSet.has(id)) {
          const bindId = reactiveData.bindIdByResourceId.get(id);
          if (bindId) removeBindIds.push(bindId);
        }
      }

      if (addIds.length === 0 && removeBindIds.length === 0) {
        reactiveData.visible = false;
        return;
      }

      reactiveData.submitting = true;
      emit('save', String(reactiveData.role.id), addIds, removeBindIds, () => {
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
