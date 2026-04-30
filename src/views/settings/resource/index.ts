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

import { addResource, deleteResource, getResourceList, updateResource } from '@/api/resource';
import { getDriverByIds } from '@/api/driver';
import { getDeviceByIds } from '@/api/device';
import { getPointByIds } from '@/api/point';
import { getProfileByIds } from '@/api/profile';
import { successMessage } from '@/utils/NotificationUtil';

import type { Order } from '@/config/entity';

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

    const editRef = ref<InstanceType<typeof resourceEditForm>>();

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

    const resourceNameMap: Record<string, string> = {};
    const entityNameMap: Record<string, string> = {};

    const formatParentResource = (id: any) => {
      if (id === undefined || id === null || String(id) === '0') {
        return 'Root';
      }
      return resourceNameMap[String(id)] || `ID: ${id}`;
    };

    const formatEntityId = (id: any) => {
      if (id === undefined || id === null || String(id) === '0') {
        return '—';
      }
      return entityNameMap[String(id)] || `${id}`;
    };

    const router = useRouter();

    const LINKABLE_TYPES = ['DRIVER', 'DEVICE', 'POINT', 'PROFILE', 'API'];

    const ENTITY_ROUTE_MAP: Record<string, string> = {
      DRIVER: 'driverDetail',
      DEVICE: 'deviceDetail',
      POINT: 'pointDetail',
      PROFILE: 'profileDetail',
      API: 'settingsApi',
    };

    const isEntityLinkable = (row: any) => {
      const id = row.entityId;
      if (!id || String(id) === '0') return false;
      return LINKABLE_TYPES.includes(row.resourceTypeFlag);
    };

    const goEntityDetail = (row: any) => {
      const routeName = ENTITY_ROUTE_MAP[row.resourceTypeFlag];
      if (!routeName) return;
      router.push({ name: routeName, query: { id: String(row.entityId), active: 'detail' } });
    };

    const resolveEntityNames = (records: any[]) => {
      const driverIds: string[] = [];
      const deviceIds: string[] = [];
      const pointIds: string[] = [];
      const profileIds: string[] = [];

      records.forEach((r) => {
        const id = r.entityId;
        if (!id || String(id) === '0') return;
        switch (r.resourceTypeFlag) {
          case 'DRIVER':
            driverIds.push(String(id));
            break;
          case 'DEVICE':
            deviceIds.push(String(id));
            break;
          case 'POINT':
            pointIds.push(String(id));
            break;
          case 'PROFILE':
            profileIds.push(String(id));
            break;
        }
      });

      const promises: Promise<void>[] = [];

      const mapDriverNames = (res: any) => {
        const data = res.data || {};
        driverIds.forEach((id) => {
          const item = data[id];
          if (item) entityNameMap[id] = item.driverName || id;
        });
      };
      const mapDeviceNames = (res: any) => {
        const data = res.data || {};
        deviceIds.forEach((id) => {
          const item = data[id];
          if (item) entityNameMap[id] = item.deviceName || id;
        });
      };
      const mapPointNames = (res: any) => {
        const data = res.data || {};
        pointIds.forEach((id) => {
          const item = data[id];
          if (item) entityNameMap[id] = item.pointName || id;
        });
      };
      const mapProfileNames = (res: any) => {
        const data = res.data || {};
        profileIds.forEach((id) => {
          const item = data[id];
          if (item) entityNameMap[id] = item.profileName || id;
        });
      };

      if (driverIds.length > 0) {
        promises.push(
          getDriverByIds(driverIds)
            .then(mapDriverNames)
            .catch(() => {})
        );
      }
      if (deviceIds.length > 0) {
        promises.push(
          getDeviceByIds(deviceIds)
            .then(mapDeviceNames)
            .catch(() => {})
        );
      }
      if (pointIds.length > 0) {
        promises.push(
          getPointByIds(pointIds)
            .then(mapPointNames)
            .catch(() => {})
        );
      }
      if (profileIds.length > 0) {
        promises.push(
          getProfileByIds(profileIds)
            .then(mapProfileNames)
            .catch(() => {})
        );
      }

      return Promise.all(promises);
    };

    const load = () => {
      reactiveData.loading = true;
      getResourceList({ page: reactiveData.page, ...reactiveData.query })
        .then((res: any) => {
          const data = res.data || {};
          const records = data.records || [];
          reactiveData.listData = records;
          reactiveData.page.total = data.total || 0;
          records.forEach((r: any) => {
            if (r.id) {
              resourceNameMap[String(r.id)] = r.resourceName || r.resourceCode || String(r.id);
            }
          });
          return resolveEntityNames(records);
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
      formatParentResource,
      formatEntityId,
      isEntityLinkable,
      goEntityDetail,
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
    };
  },
});
