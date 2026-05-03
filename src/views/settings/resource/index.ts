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

import { addResource, deleteResource, getResourceTree, updateResource } from '@/api/resource';
import { getDriverByIds } from '@/api/driver';
import { getDeviceByIds } from '@/api/device';
import { getPointByIds } from '@/api/point';
import { getProfileByIds } from '@/api/profile';
import { timestampColumn } from '@/utils/DateUtil';
import { successMessage } from '@/utils/NotificationUtil';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import resourceTool from './tool/ResourceTool.vue';
import resourceEditForm from './edit/ResourceEditForm.vue';

export default defineComponent({
  name: 'SettingsResource',
  components: {
    BlankCard,
    resourceTool,
    resourceEditForm,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const editRef = ref<InstanceType<typeof resourceEditForm>>();

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

    const entityNameMap = reactive<Record<string, string>>({});

    const isGroupingNode = (row: any): boolean => {
      // Virtual grouping nodes registered by ResourceRegistrySync carry entity_id=0.
      return !row.entityId || String(row.entityId) === '0';
    };

    const formatEntityId = (row: any) => {
      if (isGroupingNode(row)) return '—';
      const id = String(row.entityId);
      return entityNameMap[id] || id;
    };

    const LINKABLE_TYPES = ['DRIVER', 'DEVICE', 'POINT', 'PROFILE', 'API', 'MENU'];

    const ENTITY_ROUTE_MAP: Record<string, string> = {
      DRIVER: 'driverDetail',
      DEVICE: 'deviceDetail',
      POINT: 'pointDetail',
      PROFILE: 'profileDetail',
      API: 'settingsApiDetail',
      MENU: 'settingsMenuDetail',
    };

    const isEntityLinkable = (row: any) => {
      if (isGroupingNode(row)) return false;
      return LINKABLE_TYPES.includes(row.resourceTypeFlag);
    };

    const goEntityDetail = (row: any) => {
      const routeName = ENTITY_ROUTE_MAP[row.resourceTypeFlag];
      if (!routeName) return;
      router.push({ name: routeName, query: { id: String(row.entityId) } }).catch(() => {
        // handled globally
      });
    };

    const openDetail = (row: any) => {
      router.push({ name: 'settingsResourceDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };

    // Flatten tree rows into a list for post-load entity-name resolution.
    const flatten = (nodes: any[], acc: any[] = []): any[] => {
      for (const n of nodes || []) {
        acc.push(n);
        if (n.children && n.children.length > 0) {
          flatten(n.children, acc);
        }
      }
      return acc;
    };

    const resolveEntityNames = (records: any[]) => {
      const driverIds: string[] = [];
      const deviceIds: string[] = [];
      const pointIds: string[] = [];
      const profileIds: string[] = [];
      for (const r of records) {
        if (isGroupingNode(r)) continue;
        const id = String(r.entityId);
        switch (r.resourceTypeFlag) {
          case 'DRIVER':
            driverIds.push(id);
            break;
          case 'DEVICE':
            deviceIds.push(id);
            break;
          case 'POINT':
            pointIds.push(id);
            break;
          case 'PROFILE':
            profileIds.push(id);
            break;
        }
      }

      const fill = (ids: string[], res: any, nameKey: string) => {
        const data = res.data || {};
        ids.forEach((id) => {
          const item = data[id];
          if (item) entityNameMap[id] = item[nameKey] || id;
        });
      };

      const promises: Promise<void>[] = [];
      if (driverIds.length)
        promises.push(
          getDriverByIds(driverIds)
            .then((r) => fill(driverIds, r, 'driverName'))
            .catch(() => {})
        );
      if (deviceIds.length)
        promises.push(
          getDeviceByIds(deviceIds)
            .then((r) => fill(deviceIds, r, 'deviceName'))
            .catch(() => {})
        );
      if (pointIds.length)
        promises.push(
          getPointByIds(pointIds)
            .then((r) => fill(pointIds, r, 'pointName'))
            .catch(() => {})
        );
      if (profileIds.length)
        promises.push(
          getProfileByIds(profileIds)
            .then((r) => fill(profileIds, r, 'profileName'))
            .catch(() => {})
        );
      return Promise.all(promises);
    };

    const load = () => {
      reactiveData.loading = true;
      getResourceTree(reactiveData.query)
        .then((res: any) => {
          const tree = res.data || [];
          reactiveData.listData = tree;
          reactiveData.page.total = tree.length;
          return resolveEntityNames(flatten(tree));
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

    const sort = () => {
      // Tree is assembled on the server side with a deterministic order; noop here.
      load();
    };

    const openAdd = () => editRef.value?.show();
    const openEdit = (row: any) => editRef.value?.showEdit(row);

    const onAdd = (form: any, done: () => void) => {
      addResource(form)
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
      updateResource(form)
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
      deleteResource(id)
        .then(() => {
          successMessage();
          load();
        })
        .catch(() => {
          // handled globally
        });
    };

    const sizeChange = () => load();
    const currentChange = () => load();

    load();

    return {
      t,
      editRef,
      reactiveData,
      isGroupingNode,
      formatEntityId,
      isEntityLinkable,
      goEntityDetail,
      openDetail,
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
