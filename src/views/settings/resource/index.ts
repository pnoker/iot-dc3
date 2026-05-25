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

import { listApi } from '@/api/api';
import { addResource, deleteResource, listResourceTree, updateResource } from '@/api/resource';
import { listDriverByIds } from '@/api/driver';
import { listDeviceByIds } from '@/api/device';
import { getPointByIds } from '@/api/point';
import { listProfileByIds } from '@/api/profile';
import { useMenuStore } from '@/store';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';

import type {
  ApiRecord,
  DeviceRecord,
  DriverRecord,
  Order,
  PointRecord,
  ProfileRecord,
  ResourceForm,
  ResourceRecord,
} from '@/config/types';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import resourceTool from './tool/ResourceTool.vue';
import resourceEditForm from './edit/ResourceEditForm.vue';

type LinkableResourceType = 'DRIVER' | 'DEVICE' | 'POINT' | 'PROFILE' | 'API' | 'MENU';
type EntityRecord = DriverRecord | DeviceRecord | PointRecord | ProfileRecord | ApiRecord;

export default defineComponent({
  name: 'SettingsResource',
  components: {
    BlankCard,
    EnableTag,
    resourceTool,
    resourceEditForm,
  },
  setup() {
    const { t } = useI18n();
    const router = useRouter();
    const menuStore = useMenuStore();

    const editRef = ref<InstanceType<typeof resourceEditForm>>();

    const reactiveData = reactive({
      loading: false,
      listData: [] as ResourceRecord[],
      query: {} as Record<string, unknown>,
      page: {
        total: 0,
        size: 12,
        current: 1,
        orders: [] as Order[],
      },
    });

    const entityNameMap = reactive<Record<string, string>>({});

    const isGroupingNode = (row: ResourceRecord): boolean => {
      // Virtual grouping nodes registered by ResourceRegistrySync carry entity_id=0.
      return !row.entityId || String(row.entityId) === '0';
    };

    const formatEntityId = (row: ResourceRecord) => {
      if (isGroupingNode(row)) return '—';
      const id = String(row.entityId);
      return entityNameMap[id] || id;
    };

    const LINKABLE_TYPES: LinkableResourceType[] = ['DRIVER', 'DEVICE', 'POINT', 'PROFILE', 'API', 'MENU'];

    const ENTITY_ROUTE_MAP: Record<LinkableResourceType, string> = {
      DRIVER: 'driverDetail',
      DEVICE: 'deviceDetail',
      POINT: 'pointDetail',
      PROFILE: 'profileDetail',
      API: 'settingsApiDetail',
      MENU: 'settingsMenuDetail',
    };

    const resourceType = (row: ResourceRecord): LinkableResourceType | undefined => {
      const type = String(row.resourceTypeFlag || '') as LinkableResourceType;
      return LINKABLE_TYPES.includes(type) ? type : undefined;
    };

    const isEntityLinkable = (row: ResourceRecord) => {
      if (isGroupingNode(row)) return false;
      return Boolean(resourceType(row));
    };

    const goEntityDetail = (row: ResourceRecord) => {
      const type = resourceType(row);
      if (!type) return;
      const routeName = ENTITY_ROUTE_MAP[type];
      if (!routeName) return;
      router.push({ name: routeName, query: { id: String(row.entityId) } }).catch(() => {
        // handled globally
      });
    };

    const openDetail = (row: ResourceRecord) => {
      router.push({ name: 'settingsResourceDetail', query: { id: String(row.id) } }).catch(() => {
        // handled globally
      });
    };

    // Flatten tree rows into a list for post-load entity-name resolution.
    const flatten = (nodes: ResourceRecord[], acc: ResourceRecord[] = []): ResourceRecord[] => {
      for (const n of nodes || []) {
        acc.push(n);
        if (n.children && n.children.length > 0) {
          flatten(n.children, acc);
        }
      }
      return acc;
    };

    const resolveEntityNames = (records: ResourceRecord[]) => {
      const driverIds: string[] = [];
      const deviceIds: string[] = [];
      const pointIds: string[] = [];
      const profileIds: string[] = [];
      const apiIds: string[] = [];
      const menuIds: string[] = [];
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
          case 'API':
            apiIds.push(id);
            break;
          case 'MENU':
            menuIds.push(id);
            break;
        }
      }

      const fill = (ids: string[], res: R<Record<string, EntityRecord>>, nameKey: keyof EntityRecord) => {
        const data = res.data || {};
        ids.forEach((id) => {
          const item = data[id];
          const name = item?.[nameKey];
          if (name) entityNameMap[id] = String(name);
        });
      };

      const promises: Promise<void>[] = [];
      if (driverIds.length)
        promises.push(
          listDriverByIds(driverIds)
            .then((r) => fill(driverIds, r, 'driverName'))
            .catch(() => {})
        );
      if (deviceIds.length)
        promises.push(
          listDeviceByIds(deviceIds)
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
          listProfileByIds(profileIds)
            .then((r) => fill(profileIds, r, 'profileName'))
            .catch(() => {})
        );
      // APIs have no bulk-lookup endpoint; pull the whole list (capped at
      // 1000, which is already 10x the realistic API count on a single
      // tenant) and resolve from that map.
      if (apiIds.length)
        promises.push(
          listApi({ page: { size: 1000, current: 1 } })
            .then((r) => {
              const records = r.data?.records || [];
              const byId = new Map(records.map((a) => [String(a.id), a.apiName]));
              apiIds.forEach((id) => {
                const name = byId.get(id);
                if (name) entityNameMap[id] = name;
              });
            })
            .catch(() => {})
        );
      // Menus are already loaded into the pinia store for the top-nav, so
      // reuse the cached tree instead of hitting the network again.
      if (menuIds.length) {
        menuIds.forEach((id) => {
          const node = menuStore.findById(id);
          if (node) entityNameMap[id] = node.menuName;
        });
      }
      return Promise.all(promises);
    };

    const load = async () => {
      reactiveData.loading = true;
      try {
        // Ensure the menu tree is cached before resolving MENU-typed entity
        // names — fetchTree is idempotent and skips the network if already
        // loaded (Layout mounts it on startup, so this is usually a no-op).
        await menuStore.fetchTree();
        const res = await listResourceTree(reactiveData.query);
        const tree = res.data || [];
        reactiveData.listData = tree;
        reactiveData.page.total = tree.length;
        await resolveEntityNames(flatten(tree));
      } catch {
        // handled globally
      } finally {
        reactiveData.loading = false;
      }
    };

    const search = (params: Record<string, unknown>) => {
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
    const openEdit = (row: ResourceRecord) => editRef.value?.showEdit(row);

    const onAdd = (form: ResourceForm, done: () => void) => {
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

    const onUpdate = (form: ResourceForm, done: () => void) => {
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
